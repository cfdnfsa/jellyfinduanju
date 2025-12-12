package com.wjp.jellyfinduanju.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wjp.jellyfinduanju.data.model.SeriesLocalData
import com.wjp.jellyfinduanju.data.repository.DetailRepository
import com.wjp.jellyfinduanju.data.repository.SeriesRepository
import com.wjp.jellyfinduanju.download.DownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ListViewModel - 剧集列表页面的 ViewModel
 * 
 * 职责：
 * 1. 管理剧集列表的 UI 状态
 * 2. 处理刷新操作
 * 3. 处理搜索筛选
 * 4. 提供图片 URL
 * 5. 批量操作（标记已看/缓存/删除）
 */
class ListViewModel(
    private val seriesRepository: SeriesRepository,
    private val detailRepository: DetailRepository
) : ViewModel() {
    
    // ==================== UI 状态 ====================
    
    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()
    
    // 完整的剧集列表（用于搜索筛选）
    private var allSeriesList: List<SeriesLocalData> = emptyList()
    
    // ==================== 初始化 ====================
    
    init {
        // 加载本地缓存的剧集列表
        loadLocalSeries()
        
        // 订阅剧集列表变化
        viewModelScope.launch {
            seriesRepository.seriesListFlow.collect { list ->
                allSeriesList = list
                // 应用当前搜索条件
                applySearchFilter()
            }
        }
    }
    
    /**
     * 加载本地缓存的剧集列表
     * 
     * 同时从本地 Episode 缓存更新观看状态
     */
    private fun loadLocalSeries() {
        viewModelScope.launch {
            // 先从本地 Episode 缓存更新所有 Series 的观看状态
            seriesRepository.updateSeriesStatusFromLocalEpisodes()
            
            // 然后加载列表
            allSeriesList = seriesRepository.getLocalSeriesList()
            applySearchFilter()
        }
    }
    
    /**
     * 刷新观看状态
     * 
     * 从详情页返回列表页时调用，更新观看状态
     */
    fun refreshWatchStatus() {
        viewModelScope.launch {
            // 从本地 Episode 缓存更新观看状态
            seriesRepository.updateSeriesStatusFromLocalEpisodes()
            
            // 重新加载列表以触发 UI 更新
            allSeriesList = seriesRepository.getLocalSeriesList()
            applySearchFilter()
        }
    }
    
    // ==================== 搜索功能 ====================
    
    /**
     * 更新搜索关键词
     * 
     * @param query 搜索关键词
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applySearchFilter()
    }
    
    /**
     * 清除搜索
     */
    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "") }
        applySearchFilter()
    }
    
    /**
     * 更新筛选标签
     */
    fun updateFilterTag(tag: String) {
        _uiState.update { it.copy(selectedFilterTag = tag) }
        applySearchFilter()
    }
    
    /**
     * 获取所有可用的筛选标签
     */
    fun getAllFilterTags(): List<String> {
        val systemTags = listOf("全部", "已缓存", "未缓存")
        
        // 从剧集列表中提取云端文件夹标签（去重）
        val allSeries = seriesRepository.getLocalSeriesList()
        val cloudFolderTags = allSeries
            .mapNotNull { it.cloudFolderTag }
            .distinct()
            .sorted()
        
        // 获取自定义标签
        val customTags = com.wjp.jellyfinduanju.data.settings.AppSettings.customTags
        
        return systemTags + cloudFolderTags + customTags
    }
    
    /**
     * 应用搜索筛选
     * 
     * 根据搜索关键词筛选剧集列表
     * 支持模糊搜索：匹配剧集名称
     */
    private fun applySearchFilter() {
        val query = _uiState.value.searchQuery.trim()
        val selectedTag = _uiState.value.selectedFilterTag
        
        // 先应用标签筛选
        val tagFilteredList = when (selectedTag) {
            "全部" -> allSeriesList
            "已缓存" -> allSeriesList.filter { it.isVideoCached }
            "未缓存" -> allSeriesList.filter { !it.isVideoCached }
            else -> {
                // 检查是否匹配云端文件夹标签或自定义标签
                allSeriesList.filter { series ->
                    series.cloudFolderTag == selectedTag || selectedTag in series.customTags
                }
            }
        }
        
        // 再应用搜索筛选
        val filteredList = if (query.isEmpty()) {
            tagFilteredList
        } else {
            tagFilteredList.filter { series ->
                // 模糊匹配剧集名称（不区分大小写）
                series.name.contains(query, ignoreCase = true)
            }
        }
        
        _uiState.update { 
            it.copy(
                seriesList = filteredList,
                isEmpty = filteredList.isEmpty() && !it.isLoading
            ) 
        }
    }
    
    // ==================== 刷新操作 ====================
    
    /**
     * 刷新剧集列表
     * 
     * 从服务器获取最新的剧集列表
     * 用户点击刷新按钮时调用
     */
    fun refreshSeriesList() {
        // 避免重复刷新
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = seriesRepository.refreshSeriesList()
            
            result.fold(
                onSuccess = { list ->
                    allSeriesList = list
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    // 应用搜索筛选
                    applySearchFilter()
                },
                onFailure = { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "刷新失败"
                        ) 
                    }
                }
            )
        }
    }
    
    // ==================== 图片 URL ====================
    
    /**
     * 获取剧集图片 URL
     * 
     * 优先使用本地缓存，没有则返回远程 URL
     * 
     * @param seriesId 剧集 ID
     * @param callback 回调，返回图片 URL
     */
    fun getSeriesImageUrl(seriesId: String, callback: (String?) -> Unit) {
        viewModelScope.launch {
            val url = seriesRepository.getSeriesImageUrl(seriesId)
            callback(url)
        }
    }
    
    // ==================== 错误处理 ====================
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    // ==================== 长按菜单操作 ====================
    
    /**
     * 显示剧集菜单
     */
    fun showSeriesMenu(series: SeriesLocalData) {
        _uiState.update { it.copy(selectedSeries = series) }
    }
    
    /**
     * 隐藏剧集菜单
     */
    fun hideSeriesMenu() {
        _uiState.update { it.copy(selectedSeries = null) }
    }
    
    /**
     * 显示缓存全部对话框
     */
    fun showCacheAllDialog() {
        _uiState.update { it.copy(dialogType = DialogType.CACHE_ALL) }
    }
    
    /**
     * 显示删除全部缓存对话框
     */
    fun showDeleteAllCacheDialog() {
        _uiState.update { it.copy(dialogType = DialogType.DELETE_ALL_CACHE) }
    }
    
    /**
     * 隐藏对话框
     */
    fun hideDialog() {
        _uiState.update { it.copy(dialogType = null, selectedSeries = null) }
    }
    
    /**
     * 显示标签设置对话框
     */
    fun showTagDialog() {
        _uiState.update { it.copy(dialogType = DialogType.SET_TAG) }
    }
    
    /**
     * 切换剧集标签
     * 
     * @param tag 标签名称
     * @param selected 是否选中
     */
    fun toggleSeriesTag(tag: String, selected: Boolean) {
        val series = _uiState.value.selectedSeries ?: return
        
        if (selected) {
            seriesRepository.addSeriesTag(series.id, tag)
        } else {
            seriesRepository.removeSeriesTag(series.id, tag)
        }
        
        // 更新 selectedSeries 以刷新 UI
        val updatedSeries = seriesRepository.getSeries(series.id)
        if (updatedSeries != null) {
            _uiState.update { it.copy(selectedSeries = updatedSeries) }
        }
    }
    
    /**
     * 清除剧集所有自定义标签
     */
    fun clearSeriesTags() {
        val series = _uiState.value.selectedSeries ?: return
        seriesRepository.updateSeriesTags(series.id, emptyList())
        hideDialog()
    }
    
    // ==================== 批量操作 ====================
    
    /**
     * 标记整个剧集为已看/未看
     * 
     * 直接调用 Jellyfin API 标记整个 Series，不影响各集的播放进度
     * 本地更新状态，不重新请求服务器列表
     * 
     * @param watched true 标记为已看，false 标记为未看
     */
    fun markSeriesAsWatched(watched: Boolean) {
        val series = _uiState.value.selectedSeries ?: return
        hideSeriesMenu()
        
        viewModelScope.launch {
            try {
                // 1. 上报服务器
                val result = detailRepository.markSeriesAsWatched(series.id, watched)
                
                result.fold(
                    onSuccess = {
                        // 2. 本地更新状态（不请求服务器）
                        seriesRepository.updateSeriesPlayedStatus(series.id, watched)
                        
                        _uiState.update { 
                            it.copy(error = if (watched) "已标记为看完" else "已标记为未看")
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { 
                            it.copy(error = "操作失败: ${e.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "操作失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 确认缓存全部
     */
    fun confirmCacheAll() {
        val series = _uiState.value.selectedSeries ?: return
        hideDialog()
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // 1. 获取该剧集的所有季
                val seasonsResult = detailRepository.refreshSeasons(series.id)
                seasonsResult.fold(
                    onSuccess = { seasons ->
                        // 收集所有未缓存的剧集
                        val allEpisodesToDownload = mutableListOf<com.wjp.jellyfinduanju.data.model.EpisodeLocalData>()
                        
                        // 2. 获取每个季的剧集
                        seasons.forEach { season ->
                            val episodesResult = detailRepository.refreshEpisodes(series.id, season.id)
                            episodesResult.fold(
                                onSuccess = { episodes ->
                                    // 收集未缓存的剧集
                                    episodes.filter { !it.isCached }.forEach { episode ->
                                        allEpisodesToDownload.add(episode)
                                    }
                                },
                                onFailure = { }
                            )
                        }
                        
                        if (allEpisodesToDownload.isNotEmpty()) {
                            // 3. 获取视频 URL
                            val episodeIds = allEpisodesToDownload.map { it.id }
                            val videoUrls = detailRepository.getVideoStreamUrls(episodeIds)
                            
                            // 4. 添加到下载队列
                            DownloadManager.addDownloads(
                                episodes = allEpisodesToDownload,
                                seriesName = series.name,
                                videoUrls = videoUrls
                            )
                        }
                        
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = if (allEpisodesToDownload.isNotEmpty()) 
                                    "已添加 ${allEpisodesToDownload.size} 集到下载队列" 
                                else 
                                    "所有剧集已缓存"
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { 
                            it.copy(isLoading = false, error = "获取剧集失败: ${e.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, error = "操作失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 确认删除全部缓存
     */
    fun confirmDeleteAllCache() {
        val series = _uiState.value.selectedSeries ?: return
        hideDialog()
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // 删除该剧集的所有视频缓存
                val deletedCount = DownloadManager.deleteAllCacheForSeries(series.id)
                
                // 更新剧集的缓存状态
                seriesRepository.updateVideoCacheStatus(series.id, false)
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = if (deletedCount > 0) "已删除 $deletedCount 个缓存文件" else "没有找到缓存文件"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, error = "删除失败: ${e.message}")
                }
            }
        }
    }
}

/**
 * 对话框类型
 */
enum class DialogType {
    CACHE_ALL,
    DELETE_ALL_CACHE,
    SET_TAG
}

/**
 * ListUiState - 剧集列表页面的 UI 状态
 */
data class ListUiState(
    val isLoading: Boolean = false,
    val seriesList: List<SeriesLocalData> = emptyList(),
    val isEmpty: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    /** 当前选中的剧集（用于显示菜单） */
    val selectedSeries: SeriesLocalData? = null,
    /** 当前显示的对话框类型 */
    val dialogType: DialogType? = null,
    /** 当前选中的筛选标签 */
    val selectedFilterTag: String = "全部"
)

/**
 * ListViewModelFactory - ListViewModel 的工厂类
 * 
 * 用于创建带参数的 ViewModel
 */
class ListViewModelFactory(
    private val seriesRepository: SeriesRepository,
    private val detailRepository: DetailRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListViewModel::class.java)) {
            return ListViewModel(seriesRepository, detailRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
