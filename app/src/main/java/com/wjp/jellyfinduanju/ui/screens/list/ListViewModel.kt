package com.wjp.jellyfinduanju.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wjp.jellyfinduanju.data.model.SeriesLocalData
import com.wjp.jellyfinduanju.data.repository.DetailRepository
import com.wjp.jellyfinduanju.data.repository.SeriesRepository
import com.wjp.jellyfinduanju.data.settings.AppSettings
import com.wjp.jellyfinduanju.data.ui.DialogType
import com.wjp.jellyfinduanju.data.ui.ListUiState
import com.wjp.jellyfinduanju.data.ui.SortType
import com.wjp.jellyfinduanju.data.ui.ViewMode
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
    
    // === UI 状态 ===================
    
    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()
    
    // 完整的剧集列表（用于搜索筛选）
    private var allSeriesList: List<SeriesLocalData> = emptyList()
    
    // === 初始化 ===================
    
    init {
        // 从 AppSettings 加载排序设置
        val savedSortType = try {
            SortType.valueOf(AppSettings.listSortType)
        } catch (e: Exception) {
            // 解析失败时使用默认值
            SortType.UPDATE_TIME
        }
        val savedSortAscending = AppSettings.listSortAscending
        
        // 从 AppSettings 加载查看模式
        val savedViewMode = try {
            ViewMode.valueOf(AppSettings.viewMode)
        } catch (e: Exception) {
            // 解析失败时使用默认值（云端模式）
            ViewMode.CLOUD
        }
        
        // 将加载的设置应用到 UI 状态
        _uiState.update { 
            it.copy(
                sortType = savedSortType,
                sortAscending = savedSortAscending,
                viewMode = savedViewMode
            )
        }
        
        // 加载本地缓存的剧集列表
        loadLocalSeries()
        
        // 订阅剧集列表变化（实时响应数据更新）
        viewModelScope.launch {
            seriesRepository.seriesListFlow.collect { list ->
                allSeriesList = list
                // 数据更新后重新应用筛选和排序
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
    
    // === 视图模式 ===================
    
    /**
     * 切换视图模式（云端 ↔ 本地）
     */
    fun toggleViewMode() {
        // 在云端和本地模式之间切换
        val newMode = if (_uiState.value.viewMode == ViewMode.CLOUD) {
            ViewMode.LOCAL
        } else {
            ViewMode.CLOUD
        }
        
        // 持久化保存到本地
        AppSettings.viewMode = newMode.name
        
        _uiState.update { 
            it.copy(
                viewMode = newMode,
                selectedFilterTag = "全部" // 切换模式时重置筛选标签
            )
        }
        
        // 重新应用筛选（因为不同模式显示不同的剧集）
        applySearchFilter()
    }
    
    // === 搜索功能 ===================
    
    /**
     * 更新搜索关键词
     * 
     * @param query 搜索关键词
     */
    fun updateSearchQuery(query: String) {
        // 更新搜索关键词
        _uiState.update { it.copy(searchQuery = query) }
        // 立即应用新的搜索条件
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
        // 更新选中的标签
        _uiState.update { it.copy(selectedFilterTag = tag) }
        // 应用新的标签筛选
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
        // 通用的固定标签（所有模式都有）
        val fixedSystemTags = listOf("全部", "正在看")
        val watchStatusTags = listOf("未看", "已看")
        val customTags = AppSettings.customTags
        
        // 一次性获取所有剧集列表（避免重复调用）
        val allSeries = seriesRepository.getLocalSeriesList()
        
        // 根据模式获取特定标签
        val modeSpecificTags = when (_uiState.value.viewMode) {
            ViewMode.CLOUD -> getCloudSpecificTags(allSeries)
            ViewMode.LOCAL -> getLocalSpecificTags(allSeries)
        }
        
        // 拼接：固定标签 + 模式特定标签 + 自定义标签 + 观看状态标签
        return fixedSystemTags + modeSpecificTags + customTags + watchStatusTags
    }
    
    /**
     * 获取云端模式特定的标签
     * 
     * @param allSeries 所有剧集列表
     * @return 云端模式特有的标签列表（已缓存 + 云端文件夹）
     */
    private fun getCloudSpecificTags(allSeries: List<SeriesLocalData>): List<String> {
        // 提取云端文件夹标签
        val cloudFolderTags = allSeries
            .filter { !it.isLocalImport }  // 只要云端剧集
            .mapNotNull { it.cloudFolderTag }  // 提取文件夹标签
            .distinct()  // 去重
            .sorted()  // 按字母排序
        
        // 云端特有：已缓存标签 + 云端文件夹标签
        return listOf("已缓存") + cloudFolderTags
    }
    
    /**
     * 获取本地模式特定的标签
     * 
     * @param allSeries 所有剧集列表
     * @return 本地模式特有的标签列表（本地文件夹标签）
     */
    private fun getLocalSpecificTags(allSeries: List<SeriesLocalData>): List<String> {
        // 筛选本地剧集
        val localSeries = allSeries.filter { it.isLocalImport }
        
        // 1. 根文件夹标签（从哪个根目录导入）
        val localFolderTags = localSeries
            .mapNotNull { it.localFolderTag }
            .distinct()
        
        // 2. 自动生成的标签（父文件夹名称）
        val autoTags = localSeries
            .flatMap { it.customTags }
            .distinct()
        
        // 合并所有本地特有标签并排序
        return (localFolderTags + autoTags).distinct().sorted()
    }
    
    /**
     * 应用搜索筛选和排序
     * 
     * 多层级过滤流程：模式过滤 → 标签筛选 → 搜索筛选 → 排序
     * 每次搜索/筛选/排序条件变化时都会调用此方法
     * 
     * 过滤顺序很重要：
     * 1. 先按模式筛选（减少数据量）
     * 2. 再按标签筛选（进一步缩小范围）
     * 3. 最后按搜索关键词筛选（精确查找）
     * 4. 对结果进行排序
     */
    private fun applySearchFilter() {
        // === 获取当前筛选条件 ===================
        
        // 搜索关键词（已去除首尾空格）
        val query = _uiState.value.searchQuery.trim()
        
        // 当前选中的筛选标签（如"全部"、"正在看"、"已缓存"等）
        val selectedTag = _uiState.value.selectedFilterTag
        
        // 视图模式（云端/本地）
        val viewMode = _uiState.value.viewMode
        
        // === 第一步：模式过滤（云端 / 本地） ===================
        // 根据当前视图模式，只显示对应类型的剧集
        val modeFilteredList = when (viewMode) {
            ViewMode.CLOUD -> allSeriesList.filter { !it.isLocalImport }  // 云端：非本地导入的剧集
            ViewMode.LOCAL -> allSeriesList.filter { it.isLocalImport }   // 本地：本地导入的剧集
        }
        
        // === 第二步：标签筛选 ===================
        // 根据选中的标签进一步筛选剧集
        val tagFilteredList = filterByTag(modeFilteredList, selectedTag)
        
        // === 第三步：搜索筛选 ===================
        // 如果有搜索关键词，按名称进行模糊匹配
        val searchFilteredList = if (query.isEmpty()) {
            // 无搜索关键词，使用上一步的结果
            tagFilteredList
        } else {
            // 有搜索关键词，按剧集名称过滤（不区分大小写）
            tagFilteredList.filter { series ->
                series.name.contains(query, ignoreCase = true)
            }
        }
        
        // === 第四步：排序 ===================
        // 根据当前排序设置对结果进行排序
        val sortedList = applySorting(searchFilteredList)
        
        // === 更新 UI 状态 ===================
        _uiState.update { 
            it.copy(
                seriesList = sortedList,  // 更新显示的列表
                isEmpty = sortedList.isEmpty() && !it.isLoading  // 更新空状态（加载中不算空）
            ) 
        }
    }
    
    /**
     * 根据标签筛选剧集
     * 
     * @param list 待筛选的剧集列表
     * @param tag 筛选标签（如"全部"、"正在看"、"已缓存"等）
     * @return 筛选后的剧集列表
     */
    private fun filterByTag(list: List<SeriesLocalData>, tag: String): List<SeriesLocalData> {
        return when (tag) {
            "全部" -> list  // 不过滤，显示所有
            
            "已缓存" -> list.filter { it.isVideoCached }  // 只显示已下载缓存的剧集
            
            "正在看" -> list.filter { series ->
                // 正在看：有观看记录但未看完
                series.lastWatchTime > 0 && !series.played
            }
            
            "未看" -> list.filter { series ->
                // 未看：从未观看过，且未标记为已看
                series.lastWatchTime == 0L && !series.played
            }
            
            "已看" -> list.filter { series ->
                // 已看：已标记为播放完成
                series.played
            }
            
            else -> {
                // 其他标签：检查是否匹配云端文件夹、本地文件夹或自定义标签
                list.filter { series ->
                    series.cloudFolderTag == tag ||      // 匹配云端文件夹标签
                    series.localFolderTag == tag ||      // 匹配本地文件夹标签
                    tag in series.customTags             // 匹配自定义标签
                }
            }
        }
    }
    
    // === 排序功能 ===================
    
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
        
        // 使用解构声明同时获取排序类型和方向
        // "to" 创建 Pair(类型, 方向)，然后通过 val (a, b) 解构赋值给两个变量
        val (finalSortType, finalAscending) = if (newSortType == currentSortType) {
            // 相同类型：切换方向（升序 ↔ 降序）
            newSortType to !currentAscending
        } else {
            // 不同类型：使用默认方向
            val defaultAscending = when (newSortType) {
                SortType.NAME -> true           // 按名称默认升序（A-Z）
                SortType.UPDATE_TIME -> false   // 按更新时间默认降序（最新在前）
                SortType.PLAY_TIME -> false     // 按播放时间默认降序（最近在前）
            }
            newSortType to defaultAscending
        }
        
        // 持久化保存排序设置
        AppSettings.listSortType = finalSortType.name
        AppSettings.listSortAscending = finalAscending
        
        // 更新 UI 状态并重新排序
        _uiState.update { 
            it.copy(
                sortType = finalSortType,
                sortAscending = finalAscending
            )
        }
        applySearchFilter()
    }
    
    /**
     * 应用排序
     * 
     * @param list 待排序的列表
     * @return 排序后的列表
     */
    private fun applySorting(list: List<SeriesLocalData>): List<SeriesLocalData> {
        // 排序类型
        val sortType = _uiState.value.sortType
        // 排序方向
        val ascending = _uiState.value.sortAscending
        
        // 根据排序类型进行排序（默认升序）
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
        
        // 如果需要降序，翻转结果
        return if (ascending) sorted else sorted.reversed()
    }
    
    // === 刷新操作 ===================
    
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
        // 避免重复刷新（防抖）
        if (_uiState.value.isLoading) return
        
        // 根据当前模式调用不同的刷新方法
        when (_uiState.value.viewMode) {
            ViewMode.CLOUD -> refreshCloudSeries()  // 云端：从服务器拉取
            ViewMode.LOCAL -> refreshLocalFolders()  // 本地：扫描文件夹
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
                    // 更新全量列表
                    allSeriesList = list
                    // 清除加载状态
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    // 重新应用筛选和排序
                    applySearchFilter()
                },
                onFailure = { exception ->
                    // 显示错误信息
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
                
                var successCount = 0  // 成功扫描的文件夹数
                var totalSeries = 0   // 扫描到的总剧集数
                
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
    
    // === 图片 URL ===================
    
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
    
    // === 对话框 & 菜单 ===================
    
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
     * 隐藏对话框
     */
    fun hideDialog() {
        _uiState.update { it.copy(dialogType = null, selectedSeries = null) }
    }
    
    // === 标签管理 ===================
    
    /**
     * 切换剧集标签
     * 
     * @param tag 标签名称
     * @param selected 是否选中
     */
    fun toggleSeriesTag(tag: String, selected: Boolean) {
        val series = _uiState.value.selectedSeries ?: return
        
        // 添加或移除标签
        if (selected) {
            seriesRepository.addSeriesTag(series.id, tag)
        } else {
            seriesRepository.removeSeriesTag(series.id, tag)
        }
        
        // 重新获取剧集数据以更新 UI（包含新的标签列表）
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
        // 清空该剧集的所有自定义标签
        seriesRepository.updateSeriesTags(series.id, emptyList())
        // 关闭对话框
        hideDialog()
    }
    
    // === 批量操作 ===================
    
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
                    // 本地剧集：只更新本地状态（无需同步服务器）
                    seriesRepository.updateSeriesPlayedStatus(series.id, watched)
                    
                    // 更新内存中的列表（避免重新加载）
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
                    // 重新应用筛选（已看/未看状态可能影响筛选结果）
                    applySearchFilter()
                    
                    // 显示成功提示
                    _uiState.update { 
                        it.copy(error = if (watched) "已标记为看完" else "已标记为未看")
                    }
                } else {
                    // 云端剧集：需要上报服务器
                    val result = detailRepository.markSeriesAsWatched(series.id, watched)
                    
                    result.fold(
                        onSuccess = {
                            // 同步更新本地状态
                            seriesRepository.updateSeriesPlayedStatus(series.id, watched)
                            
                            // 显示成功提示
                            _uiState.update { 
                                it.copy(error = if (watched) "已标记为看完" else "已标记为未看")
                            }
                        },
                        onFailure = { e ->
                            // 显示错误提示
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
                        
                        // 2. 遍历每个季，获取剧集列表
                        seasons.forEach { season ->
                            val episodesResult = detailRepository.refreshEpisodes(series.id, season.id)
                            episodesResult.fold(
                                onSuccess = { episodes ->
                                    // 筛选出未缓存的剧集
                                    episodes.filter { !it.isCached }.forEach { episode ->
                                        allEpisodesToDownload.add(episode)
                                    }
                                },
                                onFailure = { 
                                    // 某个季失败时跳过，继续处理其他季
                                }
                            )
                        }
                        
                        if (allEpisodesToDownload.isNotEmpty()) {
                            // 3. 批量获取视频流 URL
                            val episodeIds = allEpisodesToDownload.map { it.id }
                            val videoUrls = detailRepository.getVideoStreamUrls(episodeIds)
                            
                            // 4. 添加到下载队列
                            DownloadManager.addDownloads(
                                episodes = allEpisodesToDownload,
                                seriesName = series.name,
                                videoUrls = videoUrls
                            )
                        }
                        
                        // 显示结果提示
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
                // 1. 删除所有视频缓存文件
                DownloadManager.deleteAllCacheForSeries(series.id)
                
                // 2. 删除剧集数据和详情缓存（包括列表、季、集数据）
                seriesRepository.deleteSeriesAndCleanup(series.id)
                
                // 显示成功提示
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
    
    // === 错误处理 ===================
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
