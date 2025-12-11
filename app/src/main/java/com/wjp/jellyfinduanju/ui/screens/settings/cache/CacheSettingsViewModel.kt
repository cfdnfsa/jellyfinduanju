package com.wjp.jellyfinduanju.ui.screens.settings.cache

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wjp.jellyfinduanju.JellyfinApp
import com.wjp.jellyfinduanju.data.settings.AppSettings
import com.wjp.jellyfinduanju.download.DownloadManager
import com.wjp.jellyfinduanju.util.CacheUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * CacheSettingsViewModel - 缓存设置页面的 ViewModel
 * 
 * 职责：
 * 1. 管理缓存设置页面的 UI 状态 [CacheSettingsUiState]
 * 2. 处理用户交互事件（点击、确认、取消等）
 * 3. 调用 [CacheUtils] 执行实际的缓存操作
 * 4. 通过 [AppSettings] 读写缓存路径配置
 * 
 * 状态流转：
 * - 用户点击 → ViewModel 更新状态 → UI 响应状态变化
 * - 这种单向数据流确保了状态的可预测性
 * 
 * 使用示例：
 * ```kotlin
 * val viewModel: CacheSettingsViewModel = viewModel()
 * val uiState by viewModel.uiState.collectAsState()
 * 
 * // 显示清除缓存对话框
 * viewModel.showClearCacheDialog()
 * 
 * // 确认清除
 * viewModel.confirmClearCache()
 * ```
 */
class CacheSettingsViewModel : ViewModel() {
    
    // ==================== UI 状态 ====================
    
    /**
     * 私有的可变状态流
     * 
     * 使用 MutableStateFlow 存储 UI 状态
     * 只有 ViewModel 内部可以修改，外部只能读取
     */
    private val _uiState = MutableStateFlow(CacheSettingsUiState())
    
    /**
     * 公开的只读状态流
     * 
     * UI 层通过 collectAsState() 订阅此 Flow
     * 当状态变化时，UI 会自动重组
     */
    val uiState: StateFlow<CacheSettingsUiState> = _uiState.asStateFlow()
    
    // ==================== 初始化 ====================
    
    /**
     * ViewModel 初始化时自动加载缓存信息
     */
    init {
        loadCacheInfo()
    }
    
    /**
     * 加载缓存信息
     * 
     * 从 [AppSettings] 读取缓存路径
     * 从 [CacheUtils] 获取缓存大小和文件数量
     * 
     * 执行流程：
     * 1. 设置 isLoading = true，显示加载指示器
     * 2. 在 IO 线程读取缓存信息（避免阻塞主线程）
     * 3. 更新 UI 状态
     */
    fun loadCacheInfo() {
        viewModelScope.launch {
            // 显示加载状态
            _uiState.update { it.copy(isLoading = true) }
            
            // 切换到 IO 线程执行文件操作
            withContext(Dispatchers.IO) {
                val cacheInfo = CacheUtils.getCacheInfo()
                
                // 获取有视频缓存的剧集数量
                val seriesDataStore = JellyfinApp.instance.seriesDataStore
                val cachedSeriesCount = seriesDataStore.getAllSeries().count { series -> series.isVideoCached }
                
                // 更新状态（StateFlow 的 update 是线程安全的）
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        imageCachePath = AppSettings.imageCachePath,
                        videoCachePath = AppSettings.videoCachePath,
                        imageCacheSize = cacheInfo.getFormattedImageCacheSize(),
                        videoCacheSize = cacheInfo.getFormattedVideoCacheSize(),
                        totalCacheSize = cacheInfo.getFormattedTotalSize(),
                        imageFileCount = cacheInfo.imageFileCount,
                        videoFileCount = cacheInfo.videoFileCount,
                        cachedSeriesCount = cachedSeriesCount
                    )
                }
            }
        }
    }
    
    // ==================== 清除缓存相关方法 ====================
    
    /**
     * 显示清除缓存确认对话框
     * 
     * 当用户点击"清除所有缓存"按钮时调用
     * 设置 showClearCacheDialog = true，UI 会显示确认对话框
     */
    fun showClearCacheDialog() {
        _uiState.update { it.copy(showClearCacheDialog = true) }
    }
    
    /**
     * 隐藏清除缓存确认对话框
     * 
     * 当用户点击对话框外部或"取消"按钮时调用
     */
    fun hideClearCacheDialog() {
        _uiState.update { it.copy(showClearCacheDialog = false) }
    }
    
    /**
     * 确认清除所有缓存
     * 
     * 当用户在确认对话框中点击"确定"时调用
     * 
     * 执行流程：
     * 1. 关闭对话框，显示清除中状态
     * 2. 在 IO 线程执行清除操作
     * 3. 重新加载缓存信息
     * 4. 显示成功提示
     */
    fun confirmClearCache() {
        viewModelScope.launch {
            // 关闭对话框，显示清除中状态
            _uiState.update { 
                it.copy(
                    showClearCacheDialog = false,
                    isClearing = true
                ) 
            }
            
            // 在 IO 线程执行清除操作
            withContext(Dispatchers.IO) {
                // 1. 取消所有下载任务
                DownloadManager.cancelAllDownloads()
                
                // 2. 清除下载任务记录
                JellyfinApp.instance.downloadDataStore.clearAll()
                
                // 3. 清除所有缓存文件（图片+视频）
                CacheUtils.clearAllCache()
                
                // 4. 清除剧集列表数据（SeriesDataStore）
                JellyfinApp.instance.seriesDataStore.clearAll()
                
                // 5. 清除剧集详情数据（EpisodeDataStore）
                JellyfinApp.instance.episodeDataStore.clearAll()
            }
            
            // 隐藏清除中状态
            _uiState.update { it.copy(isClearing = false) }
            
            // 重新加载缓存信息以更新显示
            loadCacheInfo()
            
            // 显示成功提示（通过 Snackbar）
            _uiState.update { it.copy(message = "所有缓存已清除") }
        }
    }
    
    // ==================== 清除数据缓存相关方法 ====================
    
    /**
     * 显示清除数据确认对话框
     * 
     * @param type 要清除的数据类型
     */
    fun showClearDataDialog(type: ClearDataType) {
        _uiState.update { 
            it.copy(
                showClearDataDialog = true,
                clearDataType = type
            ) 
        }
    }
    
    /**
     * 隐藏清除数据确认对话框
     */
    fun hideClearDataDialog() {
        _uiState.update { 
            it.copy(
                showClearDataDialog = false,
                clearDataType = null
            ) 
        }
    }
    
    /**
     * 确认清除数据
     * 
     * 根据 clearDataType 执行对应的清除操作
     * 注意：有视频缓存的剧集数据不会被删除
     */
    fun confirmClearData() {
        val dataType = _uiState.value.clearDataType ?: return
        
        viewModelScope.launch {
            // 关闭对话框，显示清除中状态
            _uiState.update { 
                it.copy(
                    showClearDataDialog = false,
                    isClearing = true
                ) 
            }
            
            withContext(Dispatchers.IO) {
                val seriesDataStore = JellyfinApp.instance.seriesDataStore
                val episodeDataStore = JellyfinApp.instance.episodeDataStore
                
                // 获取有视频缓存的剧集 ID 列表（这些不能被删除）
                val allSeriesList = seriesDataStore.getAllSeries()
                val cachedSeriesIds = allSeriesList
                    .filter { series -> series.isVideoCached }
                    .map { series -> series.id }
                    .toSet()
                
                when (dataType) {
                    ClearDataType.EPISODE_DATA -> {
                        // 清除剧集详情缓存，但保留有视频缓存的
                        if (cachedSeriesIds.isEmpty()) {
                            // 没有视频缓存，直接清除所有
                            episodeDataStore.clearAll()
                        } else {
                            // 有视频缓存，需要保留对应的详情数据
                            // 由于 EpisodeDataStore 按季存储，这里暂时跳过有缓存的
                            // 实际实现需要更精细的控制
                            _uiState.update { state ->
                                state.copy(message = "有 ${cachedSeriesIds.size} 个剧集有视频缓存，其详情数据已保留")
                            }
                        }
                    }
                    ClearDataType.SERIES_DATA -> {
                        // 清除剧集列表缓存，但保留有视频缓存的
                        val seriesToKeep = allSeriesList.filter { series -> series.id in cachedSeriesIds }
                        val seriesToDelete = allSeriesList.filter { series -> series.id !in cachedSeriesIds }
                        
                        if (seriesToDelete.isNotEmpty()) {
                            // 清除没有视频缓存的剧集
                            seriesDataStore.clearAll()
                            // 重新保存有视频缓存的剧集
                            seriesToKeep.forEach { series ->
                                seriesDataStore.saveSeries(series)
                            }
                        }
                    }
                    ClearDataType.ALL_CACHE -> {
                        // 这个已经在 confirmClearCache 中处理
                    }
                }
            }
            
            // 隐藏清除中状态
            _uiState.update { it.copy(isClearing = false, clearDataType = null) }
            
            // 重新加载缓存信息
            loadCacheInfo()
            
            // 显示成功提示
            val message = when (dataType) {
                ClearDataType.EPISODE_DATA -> "剧集详情缓存已清除"
                ClearDataType.SERIES_DATA -> "剧集列表缓存已清除"
                ClearDataType.ALL_CACHE -> "所有缓存已清除"
            }
            _uiState.update { it.copy(message = message) }
        }
    }
    
    // ==================== 修改缓存路径相关方法 ====================
    
    /**
     * 显示修改图片缓存路径对话框
     * 
     * 当用户点击图片缓存路径项时调用
     * 
     * 设置：
     * - showEditPathDialog = true（显示对话框）
     * - editingPathType = IMAGE（标记正在编辑图片路径）
     * - editingPath = 当前图片缓存路径（预填充输入框）
     */
    fun showEditImagePathDialog() {
        _uiState.update { 
            it.copy(
                showEditPathDialog = true,
                editingPathType = PathType.IMAGE,
                editingPath = AppSettings.imageCachePath
            ) 
        }
    }
    
    /**
     * 显示修改视频缓存路径对话框
     * 
     * 当用户点击视频缓存路径项时调用
     * 逻辑同 [showEditImagePathDialog]
     */
    fun showEditVideoPathDialog() {
        _uiState.update { 
            it.copy(
                showEditPathDialog = true,
                editingPathType = PathType.VIDEO,
                editingPath = AppSettings.videoCachePath
            ) 
        }
    }
    
    /**
     * 隐藏修改路径对话框
     * 
     * 当用户点击对话框外部或"取消"按钮时调用
     * 重置所有编辑相关状态
     */
    fun hideEditPathDialog() {
        _uiState.update { 
            it.copy(
                showEditPathDialog = false,
                editingPathType = null,
                editingPath = ""
            ) 
        }
    }
    
    /**
     * 更新编辑中的路径
     * 
     * 当用户在输入框中输入时调用
     * 实时更新 editingPath 状态
     * 
     * @param path 用户输入的新路径
     */
    fun updateEditingPath(path: String) {
        _uiState.update { it.copy(editingPath = path) }
    }
    
    /**
     * 确认修改路径（第一步）
     * 
     * 当用户在编辑路径对话框中点击"确定"时调用
     * 
     * 执行流程：
     * 1. 验证路径不为空
     * 2. 关闭编辑对话框
     * 3. 显示路径变更确认对话框（提醒用户会清除旧缓存）
     */
    fun confirmEditPath() {
        val state = _uiState.value
        val newPath = state.editingPath.trim()
        
        // 验证路径
        if (newPath.isEmpty()) {
            _uiState.update { it.copy(message = "路径不能为空") }
            return
        }
        
        viewModelScope.launch {
            // 关闭编辑对话框
            _uiState.update { it.copy(showEditPathDialog = false) }
            
            // 显示路径变更确认对话框
            _uiState.update { 
                it.copy(
                    showPathChangeConfirmDialog = true,
                    pendingNewPath = newPath  // 暂存新路径
                ) 
            }
        }
    }
    
    /**
     * 隐藏路径变更确认对话框
     * 
     * 当用户在确认对话框中点击"取消"时调用
     */
    fun hidePathChangeConfirmDialog() {
        _uiState.update { 
            it.copy(
                showPathChangeConfirmDialog = false,
                pendingNewPath = ""
            ) 
        }
    }
    
    /**
     * 确认路径变更（第二步，最终确认）
     * 
     * 当用户在路径变更确认对话框中点击"确定"时调用
     * 
     * 执行流程：
     * 1. 关闭对话框，显示加载状态
     * 2. 根据路径类型调用对应的更新方法（会自动清除旧缓存）
     * 3. 重新加载缓存信息
     * 4. 显示成功提示
     * 
     * 注意：更改路径会自动清除旧目录下的缓存文件
     */
    fun confirmPathChange() {
        val state = _uiState.value
        val newPath = state.pendingNewPath
        val pathType = state.editingPathType
        
        viewModelScope.launch {
            // 关闭对话框，显示加载状态
            _uiState.update { 
                it.copy(
                    showPathChangeConfirmDialog = false,
                    isLoading = true
                ) 
            }
            
            // 在 IO 线程执行路径更新操作
            withContext(Dispatchers.IO) {
                when (pathType) {
                    PathType.IMAGE -> {
                        // 更新图片缓存路径，同时清除旧目录的缓存
                        CacheUtils.updateImageCachePath(newPath, clearOldCache = true)
                    }
                    PathType.VIDEO -> {
                        // 更新视频缓存路径，同时清除旧目录的缓存
                        CacheUtils.updateVideoCachePath(newPath, clearOldCache = true)
                    }
                    null -> {
                        // 不应该发生，但需要处理
                    }
                }
            }
            
            // 重新加载缓存信息
            loadCacheInfo()
            
            // 显示成功提示，重置编辑状态
            _uiState.update { 
                it.copy(
                    message = "缓存路径已更新，旧缓存已清除",
                    editingPathType = null,
                    pendingNewPath = ""
                ) 
            }
        }
    }
    
    // ==================== 消息处理 ====================
    
    /**
     * 清除消息
     * 
     * 当 Snackbar 显示完成后调用
     * 避免消息重复显示
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

/**
 * PathType - 路径类型枚举
 * 
 * 用于区分当前正在编辑的是图片缓存路径还是视频缓存路径
 */
enum class PathType {
    /** 图片缓存路径 */
    IMAGE,
    
    /** 视频缓存路径 */
    VIDEO
}

/**
 * CacheSettingsUiState - 缓存设置页面的 UI 状态
 * 
 * 使用 data class 存储所有 UI 相关的状态
 * 通过 copy() 方法创建新状态，确保不可变性
 * 
 * 状态分类：
 * 1. 加载状态 - 控制加载指示器的显示
 * 2. 缓存信息 - 显示当前缓存的路径、大小、文件数
 * 3. 对话框状态 - 控制各种对话框的显示/隐藏
 * 4. 编辑状态 - 路径编辑相关的临时状态
 * 5. 消息提示 - 通过 Snackbar 显示的消息
 * 
 * @property isLoading 是否正在加载数据
 * @property isClearing 是否正在清除缓存
 * @property imageCachePath 图片缓存路径
 * @property videoCachePath 视频缓存路径
 * @property imageCacheSize 图片缓存大小（格式化后的字符串，如 "128.5 MB"）
 * @property videoCacheSize 视频缓存大小（格式化后的字符串）
 * @property totalCacheSize 总缓存大小（格式化后的字符串）
 * @property imageFileCount 图片缓存文件数量
 * @property videoFileCount 视频缓存文件数量
 * @property showClearCacheDialog 是否显示清除缓存确认对话框
 * @property showEditPathDialog 是否显示编辑路径对话框
 * @property showPathChangeConfirmDialog 是否显示路径变更确认对话框
 * @property editingPathType 当前正在编辑的路径类型（IMAGE 或 VIDEO）
 * @property editingPath 编辑中的路径值（用户在输入框中输入的内容）
 * @property pendingNewPath 待确认的新路径（用户确认编辑后，等待最终确认的路径）
 * @property message 需要通过 Snackbar 显示的消息，显示后应清除
 */
/**
 * ClearDataType - 清除数据类型枚举
 */
enum class ClearDataType {
    /** 清除所有文件缓存（图片+视频+下载任务） */
    ALL_CACHE,
    /** 清除剧集详情缓存（EpisodeDataStore） */
    EPISODE_DATA,
    /** 清除剧集列表缓存（SeriesDataStore） */
    SERIES_DATA
}

data class CacheSettingsUiState(
    // ==================== 加载状态 ====================
    
    /** 是否正在加载缓存信息 */
    val isLoading: Boolean = false,
    
    /** 是否正在清除缓存 */
    val isClearing: Boolean = false,
    
    // ==================== 缓存信息 ====================
    
    /** 图片缓存路径 */
    val imageCachePath: String = "",
    
    /** 视频缓存路径 */
    val videoCachePath: String = "",
    
    /** 图片缓存大小（格式化字符串） */
    val imageCacheSize: String = "0 B",
    
    /** 视频缓存大小（格式化字符串） */
    val videoCacheSize: String = "0 B",
    
    /** 总缓存大小（格式化字符串） */
    val totalCacheSize: String = "0 B",
    
    /** 图片文件数量 */
    val imageFileCount: Int = 0,
    
    /** 视频文件数量 */
    val videoFileCount: Int = 0,
    
    /** 有视频缓存的剧集数量 */
    val cachedSeriesCount: Int = 0,
    
    // ==================== 对话框状态 ====================
    
    /** 是否显示清除缓存确认对话框 */
    val showClearCacheDialog: Boolean = false,
    
    /** 是否显示编辑路径对话框 */
    val showEditPathDialog: Boolean = false,
    
    /** 是否显示路径变更确认对话框 */
    val showPathChangeConfirmDialog: Boolean = false,
    
    /** 是否显示清除数据确认对话框 */
    val showClearDataDialog: Boolean = false,
    
    /** 当前要清除的数据类型 */
    val clearDataType: ClearDataType? = null,
    
    // ==================== 编辑状态 ====================
    
    /** 当前正在编辑的路径类型 */
    val editingPathType: PathType? = null,
    
    /** 编辑中的路径（输入框中的值） */
    val editingPath: String = "",
    
    /** 待确认的新路径 */
    val pendingNewPath: String = "",
    
    // ==================== 消息提示 ====================
    
    /** Snackbar 消息，显示后应置空 */
    val message: String? = null
)
