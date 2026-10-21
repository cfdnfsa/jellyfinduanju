package com.wjp.jellyfinduanju.ui.screens.settings.folder

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wjp.jellyfinduanju.JellyfinApp
import com.wjp.jellyfinduanju.data.repository.SeriesRepository
import com.wjp.jellyfinduanju.data.settings.AppSettings
import com.wjp.jellyfinduanju.data.settings.LocalFolderReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * LocalFolderManageViewModel - 本地文件夹管理页面的 ViewModel
 */
class LocalFolderManageViewModel(
    private val application: Application,
    private val seriesRepository: SeriesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocalFolderManageUiState())
    val uiState: StateFlow<LocalFolderManageUiState> = _uiState.asStateFlow()

    init {
        // 加载已添加的文件夹列表
        loadFolders()
    }

    /**
     * 加载文件夹列表
     */
    private fun loadFolders() {
        viewModelScope.launch {
            val folders = AppSettings.localFolderReferences
            _uiState.update { it.copy(folders = folders) }
        }
    }

    /**
     * 添加文件夹
     *
     * @param uri 文件夹 URI
     */
    fun addFolder(uri: Uri) {
        viewModelScope.launch {
            try {
                // 获取持久化权限
                application.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                // 提取文件夹显示名称
                val displayName = extractFolderName(uri)

                // 保存文件夹引用
                val success = AppSettings.addLocalFolderReference(uri.toString(), displayName)

                if (success) {
                    // 触发扫描
                    scanFolder(uri.toString(), displayName)
                    // 重新加载列表
                    loadFolders()
                } else {
                    _uiState.update { it.copy(error = "该文件夹已添加") }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(error = "添加失败: ${e.message}") }
            }
        }
    }

    /**
     * 扫描指定文件夹
     *
     * @param folderUri 文件夹 URI
     * @param displayName 文件夹显示名称
     */
    fun scanFolder(folderUri: String, displayName: String) {
        // 如果正在扫描，则不重复执行
        if (_uiState.value.scanningFolderUri != null) return

        _uiState.update {
            it.copy(
                scanningFolderUri = folderUri,
                scanProgress = 0f,
                error = null
            )
        }

        viewModelScope.launch {
            val currentRef = AppSettings.localFolderReferences.find { it.uri == folderUri }
            if (currentRef == null) {
                _uiState.update { it.copy(scanningFolderUri = null, error = "文件夹引用不存在") }
                return@launch
            }

            try {
                // 检查权限是否有效
                if (!hasUriPermission(Uri.parse(folderUri))) {
                    _uiState.update {
                        it.copy(
                            scanningFolderUri = null,
                            error = "权限已失效，请重新授权"
                        )
                    }
                    // 更新权限状态
                    AppSettings.updateLocalFolderReference(folderUri) { it.copy(permissionStatus = -1) }
                    loadFolders()
                    return@launch
                }

                // 调用 Repository 进行扫描
                val result = seriesRepository.scanLocalFolder(
                    folderUri = folderUri,
                    folderName = displayName
                )

                result.fold(
                    onSuccess = { seriesList ->
                        val seriesCount = seriesList.size
                        _uiState.update {
                            it.copy(
                                scanningFolderUri = null,
                                scanProgress = 1f,
                                error = "扫描完成，发现 ${seriesCount} 个剧集"
                            )
                        }
                        // 更新文件夹引用信息
                        AppSettings.updateLocalFolderReference(folderUri) {
                            it.copy(
                                lastScanTime = System.currentTimeMillis(),
                                seriesCount = seriesCount,
                                permissionStatus = 0
                            )
                        }
                        loadFolders()
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                scanningFolderUri = null,
                                error = "扫描失败: ${e.message}"
                            )
                        }
                        AppSettings.updateLocalFolderReference(folderUri) { it.copy(permissionStatus = 0) }
                        loadFolders()
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        scanningFolderUri = null,
                        error = "扫描异常: ${e.message}"
                    )
                }
                AppSettings.updateLocalFolderReference(folderUri) { it.copy(permissionStatus = 0) }
                loadFolders()
            }
        }
    }

    /**
     * 删除文件夹引用及其所有剧集数据
     */
    fun deleteFolder(folderUri: String) {
        viewModelScope.launch {
            try {
                // 找到该文件夹下的所有剧集
                val seriesInFolder = seriesRepository.getLocalSeriesByFolder(folderUri)
                
                // 删除每个剧集
                seriesInFolder.forEach { series ->
                    seriesRepository.deleteLocalSeriesAndCleanup(series.id)
                }
                
                // 删除文件夹引用
                AppSettings.removeLocalFolderReference(folderUri)
                loadFolders()
                _uiState.update { it.copy(error = "已删除文件夹引用") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "删除失败: ${e.message}") }
            }
        }
    }

    /**
     * 检查 URI 权限是否有效
     */
    private fun hasUriPermission(uri: Uri): Boolean {
        return try {
            application.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission && it.isWritePermission
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 从 URI 中提取文件夹名称
     */
    private fun extractFolderName(uri: Uri): String {
        return try {
            val path = uri.lastPathSegment ?: uri.toString()
            path.split(":").lastOrNull()?.split("/")?.lastOrNull() ?: path
        } catch (e: Exception) {
            uri.toString()
        }
    }
}

data class LocalFolderManageUiState(
    val folders: List<LocalFolderReference> = emptyList(),
    val scanningFolderUri: String? = null, // 正在扫描的文件夹 URI
    val scanProgress: Float = 0f,          // 扫描进度 (0f - 1f)
    val error: String? = null
)

class LocalFolderManageViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocalFolderManageViewModel::class.java)) {
            val seriesRepository = JellyfinApp.instance.seriesRepository
            return LocalFolderManageViewModel(application, seriesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

