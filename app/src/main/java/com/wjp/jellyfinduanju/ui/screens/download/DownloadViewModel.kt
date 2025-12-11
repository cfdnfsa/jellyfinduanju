package com.wjp.jellyfinduanju.ui.screens.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wjp.jellyfinduanju.data.local.DownloadDataStore
import com.wjp.jellyfinduanju.data.local.EpisodeDataStore
import com.wjp.jellyfinduanju.data.local.SeriesDataStore
import com.wjp.jellyfinduanju.data.model.DownloadStatus
import com.wjp.jellyfinduanju.data.model.DownloadTask
import com.wjp.jellyfinduanju.data.model.SeriesLocalData
import com.wjp.jellyfinduanju.download.DownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * DownloadViewModel - 下载页面的 ViewModel
 * 
 * 职责：
 * 1. 管理下载页面的 UI 状态
 * 2. 展示正在下载、等待中、失败的任务
 * 3. 展示已完成的下载（按剧集分组）
 * 4. 处理暂停、恢复、重试、取消等操作
 */
class DownloadViewModel(
    private val downloadDataStore: DownloadDataStore,
    private val seriesDataStore: SeriesDataStore,
    private val episodeDataStore: EpisodeDataStore
) : ViewModel() {
    
    // ==================== UI 状态 ====================
    
    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()
    
    // ==================== 初始化 ====================
    
    init {
        // 订阅下载任务变化
        viewModelScope.launch {
            DownloadManager.getTasksFlow().collectLatest { tasks ->
                updateUiState(tasks)
            }
        }
    }
    
    /**
     * 更新 UI 状态
     */
    private fun updateUiState(tasks: List<DownloadTask>) {
        val downloading = tasks.filter { it.status == DownloadStatus.DOWNLOADING }
        val pending = tasks.filter { it.status == DownloadStatus.PENDING }
        val failed = tasks.filter { it.status == DownloadStatus.FAILED }
        val completed = tasks.filter { it.status == DownloadStatus.COMPLETED }
        
        // 按剧集分组已完成的任务
        val completedBySeries = completed.groupBy { it.seriesId }
            .map { (seriesId, episodes) ->
                val series = seriesDataStore.getSeries(seriesId)
                CompletedSeriesGroup(
                    seriesId = seriesId,
                    seriesName = series?.name ?: episodes.firstOrNull()?.seriesName ?: "未知剧集",
                    primaryImageTag = series?.primaryImageTag,
                    isDeletedFromServer = series?.isDeletedFromServer ?: false,
                    episodes = episodes.sortedBy { it.episodeNumber }
                )
            }
            .sortedByDescending { it.episodes.maxOfOrNull { e -> e.updateTime } ?: 0 }
        
        _uiState.update {
            it.copy(
                downloadingTasks = downloading,
                pendingTasks = pending,
                failedTasks = failed,
                completedGroups = completedBySeries,
                isLoading = false
            )
        }
    }
    
    // ==================== 下载操作 ====================
    
    /**
     * 暂停下载
     */
    fun pauseDownload(taskId: String) {
        DownloadManager.pauseDownload(taskId)
    }
    
    /**
     * 恢复下载
     */
    fun resumeDownload(taskId: String) {
        DownloadManager.resumeDownload(taskId)
    }
    
    /**
     * 重试下载
     */
    fun retryDownload(taskId: String) {
        DownloadManager.retryDownload(taskId)
    }
    
    /**
     * 取消下载
     */
    fun cancelDownload(taskId: String) {
        DownloadManager.cancelDownload(taskId)
    }
    
    /**
     * 删除已完成的下载
     */
    fun deleteCompletedDownload(taskId: String) {
        viewModelScope.launch {
            val task = downloadDataStore.getTask(taskId) ?: return@launch
            
            // 删除下载记录和文件
            DownloadManager.deleteCompletedDownload(taskId)
            
            // 更新 EpisodeDataStore 中的缓存状态
            episodeDataStore.updateEpisodeCacheStatus(
                seasonId = task.seasonId,
                episodeId = task.episodeId,
                isCached = false,
                localVideoPath = null
            )
            
            // 检查该剧集是否还有其他视频缓存
            val hasRemainingCache = episodeDataStore.hasAnyVideoCache(task.seriesId)
            if (!hasRemainingCache) {
                // 更新 SeriesLocalData.isVideoCached
                seriesDataStore.getSeries(task.seriesId)?.let { series ->
                    seriesDataStore.saveSeries(series.copy(isVideoCached = false))
                    
                    // 如果已从服务器删除且没有缓存了，清理所有数据
                    if (series.isDeletedFromServer) {
                        seriesDataStore.deleteSeries(task.seriesId)
                        episodeDataStore.deleteSeriesDetailCache(task.seriesId)
                    }
                }
            }
        }
    }
    
    /**
     * 删除整个剧集的所有已完成下载
     */
    fun deleteSeriesDownloads(seriesId: String) {
        viewModelScope.launch {
            val tasks = downloadDataStore.getTasksBySeries(seriesId)
                .filter { it.status == DownloadStatus.COMPLETED }
            
            tasks.forEach { task ->
                DownloadManager.deleteCompletedDownload(task.id)
                episodeDataStore.updateEpisodeCacheStatus(
                    seasonId = task.seasonId,
                    episodeId = task.episodeId,
                    isCached = false,
                    localVideoPath = null
                )
            }
            
            // 更新 SeriesLocalData
            seriesDataStore.getSeries(seriesId)?.let { series ->
                seriesDataStore.saveSeries(series.copy(isVideoCached = false))
                
                if (series.isDeletedFromServer) {
                    seriesDataStore.deleteSeries(seriesId)
                    episodeDataStore.deleteSeriesDetailCache(seriesId)
                }
            }
        }
    }
    
    /**
     * 暂停所有下载
     */
    fun pauseAll() {
        DownloadManager.pauseAll()
    }
}

/**
 * 下载页面 UI 状态
 */
data class DownloadUiState(
    val isLoading: Boolean = true,
    val downloadingTasks: List<DownloadTask> = emptyList(),
    val pendingTasks: List<DownloadTask> = emptyList(),
    val failedTasks: List<DownloadTask> = emptyList(),
    val completedGroups: List<CompletedSeriesGroup> = emptyList()
) {
    /**
     * 是否有活跃的下载任务
     */
    val hasActiveTasks: Boolean
        get() = downloadingTasks.isNotEmpty() || pendingTasks.isNotEmpty()
    
    /**
     * 总的活跃任务数
     */
    val activeTaskCount: Int
        get() = downloadingTasks.size + pendingTasks.size
}

/**
 * 已完成的剧集分组
 */
data class CompletedSeriesGroup(
    val seriesId: String,
    val seriesName: String,
    val primaryImageTag: String?,
    val isDeletedFromServer: Boolean,
    val episodes: List<DownloadTask>
) {
    val episodeCount: Int get() = episodes.size
    val totalSize: Long get() = episodes.sumOf { it.totalBytes }
    
    fun getTotalSizeText(): String {
        return when {
            totalSize < 1024 -> "$totalSize B"
            totalSize < 1024 * 1024 -> String.format("%.1f KB", totalSize / 1024.0)
            totalSize < 1024 * 1024 * 1024 -> String.format("%.1f MB", totalSize / (1024.0 * 1024))
            else -> String.format("%.2f GB", totalSize / (1024.0 * 1024 * 1024))
        }
    }
}

/**
 * DownloadViewModel 工厂类
 */
class DownloadViewModelFactory(
    private val downloadDataStore: DownloadDataStore,
    private val seriesDataStore: SeriesDataStore,
    private val episodeDataStore: EpisodeDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DownloadViewModel::class.java)) {
            return DownloadViewModel(downloadDataStore, seriesDataStore, episodeDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

