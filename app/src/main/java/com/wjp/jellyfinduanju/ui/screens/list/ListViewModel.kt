package com.wjp.jellyfinduanju.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wjp.jellyfinduanju.data.model.SeriesLocalData
import com.wjp.jellyfinduanju.data.repository.DetailRepository
import com.wjp.jellyfinduanju.data.repository.SeriesRepository
import com.wjp.jellyfinduanju.data.settings.AppSettings
import com.wjp.jellyfinduanju.download.DownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

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
@HiltViewModel
class ListViewModel @Inject constructor(
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
        // 从 AppSettings 加载排序设置
        val savedSortType = try {
            SortType.valueOf(AppSettings.listSortType)
        } catch (e: Exception) {
            SortType.UPDATE_TIME
        }
        val savedSortAscending = AppSettings.listSortAscending
        
        // 从 AppSettings 加载查看模式
        val savedViewMode = try {
            ViewMode.valueOf(AppSettings.viewMode)
        } catch (e: Exception) {
            ViewMode.CLOUD
        }
        
        _uiState.update { 
            it.copy(
                sortType = savedSortType,
                sortAscending = savedSortAscending,
                viewMode = savedViewMode
            )
        }
        
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
    
    // ==================== 排序功能 ====================
    
    /**
     * 切换排序类型
     * 
     * 如果点击的是当前排序类型，则切换排序方向（升序/降序）
     * 如果点击的是新的排序类型，则使用该类型的默认方向
     * 
     * @param newSortType 新的排序类型
     */
    fun toggleSortType(newSortType: SortType) {
        val currentSortType = _uiState.value.sortType
        val currentAscending = _uiState.value.sortAscending
        
        val (finalSortType, finalAscending) = if (newSortType == currentSortType) {
            // 相同类型，切换方向
            newSortType to !currentAscending
        } else {
            // 不同类型，使用新类型的默认方向
            val defaultAscending = when (newSortType) {
                SortType.NAME -> true           // 按名称默认升序（A-Z）
                SortType.UPDATE_TIME -> false   // 按更新时间默认降序（最新在前）
                SortType.PLAY_TIME -> false     // 按播放时间默认降序（最近在前）
            }
            newSortType to defaultAscending
        }
        
        // 保存到本地
        AppSettings.listSortType = finalSortType.name
        AppSettings.listSortAscending = finalAscending
        
        // 更新状态并重新排序
        _uiState.update { 
            it.copy(
                sortType = finalSortType,
                sortAscending = finalAscending
            )
        }
        applySearchFilter()
    }
    
    /**
     * 获取所有可用的筛选标签
     * 
     * 根据当前模式返回不同的标签：
     * - 云端模式：全部、正在看、已缓存、自定义标签、云端文件夹、未看、已看
     * - 本地模式：全部、正在看、自定义标签、本地文件夹、未看、已看
     */
    fun getAllFilterTags(): List<String> {
        val viewMode = _uiState.value.viewMode
        
        // 固定系统标签
        val fixedSystemTags = listOf("全部", "正在看")
        val watchStatusTags = listOf("未看", "已看")
        
        // 获取所有剧集
        val allSeries = seriesRepository.getLocalSeriesList()
        
        // 获取自定义标签
        val customTags = AppSettings.customTags
        
        return when (viewMode) {
            ViewMode.CLOUD -> {
                // 云端模式：包含"已缓存"标签和云端文件夹标签
                val cloudFolderTags = allSeries
                    .filter { !it.isLocalImport }
                    .mapNotNull { it.cloudFolderTag }
                    .distinct()
                    .sorted()
                
                fixedSystemTags + listOf("已缓存") + customTags + cloudFolderTags + watchStatusTags
            }
            ViewMode.LOCAL -> {
                // 本地模式：收集所有本地剧集的标签
                val localSeries = allSeries.filter { it.isLocalImport }
                
                // 1. 根文件夹标签
                val localFolderTags = localSeries
                    .mapNotNull { it.localFolderTag }
                    .distinct()
                
                // 2. 自动生成的标签（父文件夹名称）
                val autoTags = localSeries
                    .flatMap { it.customTags }
                    .distinct()
                
                // 合并所有标签并排序
                val allLocalTags = (localFolderTags + autoTags).distinct().sorted()
                
                fixedSystemTags + customTags + allLocalTags + watchStatusTags
            }
        }
    }
    
    /**
     * 应用搜索筛选和排序
     * 
     * 根据搜索关键词筛选剧集列表，然后应用排序
     * 支持模糊搜索：匹配剧集名称
     */
    private fun applySearchFilter() {
        val query = _uiState.value.searchQuery.trim()
        val selectedTag = _uiState.value.selectedFilterTag
        val viewMode = _uiState.value.viewMode
        
        // 第一步：根据模式过滤（云端 / 本地）
        val modeFilteredList = when (viewMode) {
            ViewMode.CLOUD -> allSeriesList.filter { !it.isLocalImport }
            ViewMode.LOCAL -> allSeriesList.filter { it.isLocalImport }
        }
        
        // 第二步：应用标签筛选
        val tagFilteredList = when (selectedTag) {
            "全部" -> modeFilteredList
            "已缓存" -> modeFilteredList.filter { it.isVideoCached }
            "正在看" -> modeFilteredList.filter { series ->
                // 有观看记录但未标记为已播放完成
                series.lastWatchTime > 0 && !series.played
            }
            "未看" -> modeFilteredList.filter { series ->
                // 没有观看记录且未标记为已播放
                series.lastWatchTime == 0L && !series.played
            }
            "已看" -> modeFilteredList.filter { series ->
                // 已标记为播放完成
                series.played
            }
            else -> {
                // 检查是否匹配云端文件夹标签、本地文件夹标签或自定义标签
                modeFilteredList.filter { series ->
                    series.cloudFolderTag == selectedTag || 
                    series.localFolderTag == selectedTag || 
                    selectedTag in series.customTags
                }
            }
        }
        
        // 第三步：应用搜索筛选
        val searchFilteredList = if (query.isEmpty()) {
            tagFilteredList
        } else {
            tagFilteredList.filter { series ->
                // 模糊匹配剧集名称（不区分大小写）
                series.name.contains(query, ignoreCase = true)
            }
        }
        
        // 第四步：应用排序
        val sortedList = applySorting(searchFilteredList)
        
        _uiState.update { 
            it.copy(
                seriesList = sortedList,
                isEmpty = sortedList.isEmpty() && !it.isLoading
            ) 
        }
    }
    
    /**
     * 应用排序
     * 
     * @param list 待排序的列表
     * @return 排序后的列表
     */
    private fun applySorting(list: List<SeriesLocalData>): List<SeriesLocalData> {
        val sortType = _uiState.value.sortType
        val ascending = _uiState.value.sortAscending
        
        val sorted = when (sortType) {
            SortType.NAME -> {
                // 按名称排序（使用中文拼音排序）
                val collator = Collator.getInstance(Locale.CHINESE)
                list.sortedWith(compareBy(collator) { it.name })
            }
            SortType.UPDATE_TIME -> {
                // 按更新时间排序（使用 lastSyncTime 作为更新时间）
                list.sortedBy { it.lastSyncTime }
            }
            SortType.PLAY_TIME -> {
                // 按播放时间排序
                list.sortedBy { it.lastWatchTime }
            }
        }
        
        // 根据排序方向返回
        return if (ascending) sorted else sorted.reversed()
    }
    
    // ==================== 模式切换 ====================
    
    /**
     * 切换视图模式（云端 ↔ 本地）
     */
    fun toggleViewMode() {
        val newMode = if (_uiState.value.viewMode == ViewMode.CLOUD) {
            ViewMode.LOCAL
        } else {
            ViewMode.CLOUD
        }
        
        // 保存到 AppSettings
        AppSettings.viewMode = newMode.name
        
        _uiState.update { 
            it.copy(
                viewMode = newMode,
                selectedFilterTag = "全部" // 切换模式时重置筛选标签
            )
        }
        
        // 重新应用筛选
        applySearchFilter()
    }
    
    // ==================== 刷新操作 ====================
    
    /**
     * 刷新剧集列表
     * 
     * 根据当前模式刷新：
     * - 云端模式：从服务器获取最新的剧集列表
     * - 本地模式：重新扫描所有本地文件夹
     * 
     * 用户点击刷新按钮时调用
     */
    fun refreshSeriesList() {
        // 避免重复刷新
        if (_uiState.value.isLoading) return
        
        when (_uiState.value.viewMode) {
            ViewMode.CLOUD -> refreshCloudSeries()
            ViewMode.LOCAL -> refreshLocalFolders()
        }
    }
    
    /**
     * 刷新云端剧集列表
     */
    private fun refreshCloudSeries() {
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
    
    /**
     * 刷新本地文件夹
     * 
     * 重新扫描所有已添加的本地文件夹
     */
    fun refreshLocalFolders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // 获取所有本地文件夹引用
                val localFolders = AppSettings.localFolderReferences
                
                if (localFolders.isEmpty()) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "还没有添加本地文件夹"
                        ) 
                    }
                    return@launch
                }
                
                var successCount = 0
                var totalSeries = 0
                
                // 逐个扫描文件夹
                localFolders.forEach { folderRef ->
                    val result = seriesRepository.scanLocalFolder(
                        folderUri = folderRef.uri,
                        folderName = folderRef.displayName
                    )
                    
                    result.fold(
                        onSuccess = { seriesList ->
                            successCount++
                            totalSeries += seriesList.size
                        },
                        onFailure = { 
                            // 跳过失败的文件夹，继续扫描下一个
                        }
                    )
                }
                
                // 重新加载列表
                allSeriesList = seriesRepository.getLocalSeriesList()
                applySearchFilter()
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = if (successCount > 0) {
                            "已刷新 $successCount 个文件夹，共 $totalSeries 个剧集"
                        } else {
                            "刷新失败，请检查文件夹权限"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "刷新失败: ${e.message}"
                    )
                }
            }
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
     * 显示删除剧集对话框
     */
    fun showDeleteSeriesDialog() {
        _uiState.update { it.copy(dialogType = DialogType.DELETE_SERIES) }
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
                if (series.isLocalImport) {
                    // 本地剧集：只更新本地状态
                    seriesRepository.updateSeriesPlayedStatus(series.id, watched)
                    
                    // 更新内存中的列表
                    allSeriesList = allSeriesList.map { 
                        if (it.id == series.id) {
                            it.copy(
                                played = watched,
                                lastWatchTime = if (watched) System.currentTimeMillis() else it.lastWatchTime
                            )
                        } else {
                            it
                        }
                    }
                    applySearchFilter()
                    
                    _uiState.update { 
                        it.copy(error = if (watched) "已标记为看完" else "已标记为未看")
                    }
                } else {
                    // 云端剧集：上报服务器
                    val result = detailRepository.markSeriesAsWatched(series.id, watched)
                    
                    result.fold(
                        onSuccess = {
                            // 本地更新状态
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
                }
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
        
        // 本地剧集不需要缓存
        if (series.isLocalImport) {
            _uiState.update { 
                it.copy(error = "本地剧集无需缓存")
            }
            return
        }
        
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
        
        // 本地剧集没有缓存
        if (series.isLocalImport) {
            _uiState.update { 
                it.copy(error = "本地剧集没有缓存")
            }
            return
        }
        
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
    
    /**
     * 确认删除剧集
     * 
     * 删除包括：
     * 1. 列表数据
     * 2. 剧集详情（季和集）
     * 3. 所有下载的视频文件
     * 4. 图片缓存
     */
    fun confirmDeleteSeries() {
        val series = _uiState.value.selectedSeries ?: return
        hideDialog()
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // 1. 删除所有视频缓存
                DownloadManager.deleteAllCacheForSeries(series.id)
                
                // 2. 删除剧集数据和详情缓存
                seriesRepository.deleteSeriesAndCleanup(series.id)
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "已删除「${series.name}」"
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
 * 视图模式
 */
enum class ViewMode {
    CLOUD,  // 云端剧集
    LOCAL   // 本地导入
}

/**
 * 对话框类型
 */
enum class DialogType {
    CACHE_ALL,
    DELETE_ALL_CACHE,
    SET_TAG,
    DELETE_SERIES  // 删除剧集（包括列表、详情和所有缓存）
}

/**
 * 排序类型
 */
enum class SortType {
    NAME,           // 按名称
    UPDATE_TIME,    // 按更新时间
    PLAY_TIME       // 按播放时间
}

/**
 * ListUiState - 剧集列表页面的 UI 状态
 */
data class ListUiState(
    /** 是否正在加载数据（显示加载指示器） */
    val isLoading: Boolean = false,
    /** 剧集列表数据（经过筛选和排序后的列表） */
    val seriesList: List<SeriesLocalData> = emptyList(),
    /** 列表是否为空（用于显示空状态界面） */
    val isEmpty: Boolean = true,
    /** 错误信息（如果有错误则显示，通过 Snackbar 提示） */
    val error: String? = null,
    /** 搜索关键词（用户输入的搜索文本） */
    val searchQuery: String = "",
    /** 当前选中的剧集（用于显示菜单） */
    val selectedSeries: SeriesLocalData? = null,
    /** 当前显示的对话框类型 */
    val dialogType: DialogType? = null,
    /** 当前选中的筛选标签 */
    val selectedFilterTag: String = "全部",
    /** 排序类型 */
    val sortType: SortType = SortType.UPDATE_TIME,
    /** 排序方向：true = 升序, false = 降序 */
    val sortAscending: Boolean = false,
    /** 视图模式：云端 / 本地 */
    val viewMode: ViewMode = ViewMode.CLOUD
)
