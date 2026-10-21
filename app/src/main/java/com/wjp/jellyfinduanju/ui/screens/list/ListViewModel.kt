package com.wjp.jellyfinduanju.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wjp.jellyfinduanju.data.model.EpisodeLocalData
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    
    companion object {
        // 中文排序器（缓存实例，避免每次排序都创建，提升性能）
        private val CHINESE_COLLATOR = Collator.getInstance(Locale.CHINESE)
    }
    
    // === UI 状态 ===================
    
    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()
    
    // 完整的剧集列表（用于搜索筛选）
    private var allSeriesList: List<SeriesLocalData> = emptyList()
    
    // 搜索防抖任务（避免输入时频繁触发搜索）
    private var searchJob: Job? = null
    
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
     * 使用防抖机制：等待用户停止输入 300ms 后才执行搜索
     * 避免用户快速输入时频繁触发搜索，提升性能和体验
     * 
     * @param query 搜索关键词
     */
    fun updateSearchQuery(query: String) {
        // 更新搜索关键词
        _uiState.update { it.copy(searchQuery = query) }
        
        // 取消之前的搜索任务（用户还在输入中）
        searchJob?.cancel()
        
        // 延迟 300ms 后执行搜索（用户停止输入后才搜索）
        searchJob = viewModelScope.launch {
            delay(300)  // 等待 300ms
            applySearchFilter()  // 执行搜索
        }
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
        // 排序类型
        val currentSortType = _uiState.value.sortType
        // 排序方向
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
        AppSettings.listSortType = finalSortType.name  // 列表排序类型
        AppSettings.listSortAscending = finalAscending  // 列表排序方向
        
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
                // 按名称排序（使用中文拼音排序，复用缓存的 Collator 实例）
                list.sortedWith(compareBy(CHINESE_COLLATOR) { it.name })
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
                    markLocalSeriesAsWatched(series, watched)
                } else {
                    // 云端剧集：需要上报服务器
                    markCloudSeriesAsWatched(series, watched)
                }
            } catch (e: Exception) {
                showError("操作失败: ${e.message}")
            }
        }
    }
    
    /**
     * 标记本地剧集为已看/未看
     * 
     * 本地剧集无需同步服务器，只更新本地数据库和内存状态
     * 
     * @param series 要标记的剧集
     * @param watched true 标记为已看，false 标记为未看
     */
    private suspend fun markLocalSeriesAsWatched(series: SeriesLocalData, watched: Boolean) {
        // 更新数据库中的播放状态
        seriesRepository.updateSeriesPlayedStatus(series.id, watched)
        
        // 更新内存中的列表（避免重新加载整个列表，提升性能）
        updateSeriesInMemory(series.id, watched)
        
        // 重新应用筛选（已看/未看状态可能影响筛选结果）
        applySearchFilter()
        
        // 显示成功提示
        showWatchedStatusMessage(watched)
    }
    
    /**
     * 标记云端剧集为已看/未看
     * 
     * 云端剧集需要先上报服务器，成功后再更新本地状态
     * 
     * @param series 要标记的剧集
     * @param watched true 标记为已看，false 标记为未看
     */
    private suspend fun markCloudSeriesAsWatched(series: SeriesLocalData, watched: Boolean) {
        // 调用 API 上报服务器
        val result = detailRepository.markSeriesAsWatched(series.id, watched)
        
        result.fold(
            onSuccess = {
                // 服务器更新成功，同步更新本地数据库
                seriesRepository.updateSeriesPlayedStatus(series.id, watched)
                
                // 显示成功提示
                showWatchedStatusMessage(watched)
            },
            onFailure = { e ->
                // 服务器更新失败，显示错误提示
                showError("操作失败: ${e.message}")
            }
        )
    }
    
    /**
     * 更新内存中剧集的播放状态
     * 
     * 通过 map 操作更新指定剧集的状态，避免重新从数据库加载全部数据
     * 
     * @param seriesId 要更新的剧集 ID
     * @param watched true 标记为已看，false 标记为未看
     */
    private fun updateSeriesInMemory(seriesId: String, watched: Boolean) {
        allSeriesList = allSeriesList.map { series ->
            if (series.id == seriesId) {
                // 找到目标剧集，更新播放状态
                series.copy(
                    played = watched,
                    // 如果标记为已看，更新最后观看时间；否则保持原时间
                    lastWatchTime = if (watched) System.currentTimeMillis() else series.lastWatchTime
                )
            } else {
                // 其他剧集保持不变
                series
            }
        }
    }
    
    /**
     * 显示已看/未看状态的成功提示消息
     * 
     * @param watched true 显示"已标记为看完"，false 显示"已标记为未看"
     */
    private fun showWatchedStatusMessage(watched: Boolean) {
        val message = if (watched) "已标记为看完" else "已标记为未看"
        _uiState.update { it.copy(error = message) }
    }
    
    /**
     * 显示错误提示消息
     * 
     * @param message 错误消息内容
     */
    private fun showError(message: String) {
        _uiState.update { it.copy(error = message) }
    }
    
    /**
     * 确认缓存全部
     * 
     * 将剧集的所有未缓存剧集添加到下载队列
     */
    fun confirmCacheAll() {
        val series = _uiState.value.selectedSeries ?: return
        hideDialog()
        
        // 检查是否为本地剧集
        if (series.isLocalImport) {
            showError("本地剧集无需缓存")
            return
        }
        
        viewModelScope.launch {
            setLoading(true)
            
            try {
                // 1. 收集所有未缓存的剧集
                val episodesToDownload = collectUncachedEpisodes(series.id)
                
                // 2. 添加到下载队列
                if (episodesToDownload.isNotEmpty()) {
                    addEpisodesToDownloadQueue(episodesToDownload, series.name)
                }
                
                // 3. 显示结果
                showCacheAllSuccess(episodesToDownload.size)
            } catch (e: Exception) {
                showErrorWithLoading("操作失败: ${e.message}")
            }
        }
    }
    
    /**
     * 收集剧集中所有未缓存的剧集
     * 
     * 遍历剧集的所有季，收集未缓存的剧集列表
     * 
     * @param seriesId 剧集 ID
     * @return 未缓存的剧集列表
     * @throws Exception 获取剧集失败时抛出异常
     */
    private suspend fun collectUncachedEpisodes(seriesId: String): List<EpisodeLocalData> {
        // 1. 获取该剧集的所有季
        val seasonsResult = detailRepository.refreshSeasons(seriesId)
        val seasons = seasonsResult.getOrElse { error ->
            throw Exception("获取剧集失败: ${error.message}")
        }
        
        // 2. 遍历每个季，收集未缓存的剧集
        val uncachedEpisodes = mutableListOf<EpisodeLocalData>()
        seasons.forEach { season ->
            val episodes = getSeasonEpisodes(seriesId, season.id)
            // 筛选出未缓存的剧集并添加到列表
            uncachedEpisodes.addAll(episodes.filter { !it.isCached })
        }
        
        return uncachedEpisodes
    }
    
    /**
     * 获取某一季的剧集列表
     * 
     * @param seriesId 剧集 ID
     * @param seasonId 季 ID
     * @return 该季的剧集列表，失败时返回空列表（不中断整体流程）
     */
    private suspend fun getSeasonEpisodes(seriesId: String, seasonId: String): List<EpisodeLocalData> {
        val episodesResult = detailRepository.refreshEpisodes(seriesId, seasonId)
        // 某个季失败时返回空列表，继续处理其他季
        return episodesResult.getOrElse { emptyList() }
    }
    
    /**
     * 添加剧集到下载队列
     * 
     * 批量获取视频流 URL，并将剧集添加到 DownloadManager
     * 
     * @param episodes 要下载的剧集列表
     * @param seriesName 剧集名称（用于显示）
     */
    private suspend fun addEpisodesToDownloadQueue(
        episodes: List<EpisodeLocalData>,
        seriesName: String
    ) {
        // 1. 批量获取视频流 URL
        val episodeIds = episodes.map { it.id }
        val videoUrls = detailRepository.getVideoStreamUrls(episodeIds)
        
        // 2. 添加到下载队列
        DownloadManager.addDownloads(
            episodes = episodes,
            seriesName = seriesName,
            videoUrls = videoUrls
        )
    }
    
    /**
     * 显示缓存全部的成功结果
     * 
     * @param count 添加到下载队列的剧集数量，0 表示所有剧集已缓存
     */
    private fun showCacheAllSuccess(count: Int) {
        val message = if (count > 0) {
            "已添加 $count 集到下载队列"
        } else {
            "所有剧集已缓存"
        }
        _uiState.update { 
            it.copy(isLoading = false, error = message)
        }
    }
    
    /**
     * 设置加载状态
     * 
     * @param loading true 显示加载中，false 隐藏加载
     */
    private fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
    }
    
    /**
     * 显示错误消息并关闭加载状态
     * 
     * @param message 错误消息内容
     */
    private fun showErrorWithLoading(message: String) {
        _uiState.update { 
            it.copy(isLoading = false, error = message)
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
