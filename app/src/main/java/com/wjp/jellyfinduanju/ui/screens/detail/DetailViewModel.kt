package com.wjp.jellyfinduanju.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wjp.jellyfinduanju.data.model.DownloadStatus
import com.wjp.jellyfinduanju.data.model.EpisodeLocalData
import com.wjp.jellyfinduanju.data.model.SeasonLocalData
import com.wjp.jellyfinduanju.data.model.SeriesLocalData
import com.wjp.jellyfinduanju.data.repository.DetailRepository
import com.wjp.jellyfinduanju.data.repository.SeriesRepository
import com.wjp.jellyfinduanju.download.DownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * DetailViewModel - 剧集详情页的 ViewModel
 * 
 * 职责：
 * 1. 管理详情页的 UI 状态
 * 2. 加载季和剧集数据（优先使用本地缓存）
 * 3. 刷新数据（从服务器同步）
 * 4. 处理标记已看完操作
 * 5. 管理缓存选择模式
 * 6. 删除视频后检查并清理（如果已从服务器删除且无视频缓存）
 * 
 * 缓存策略：
 * - 进入页面时：有缓存用缓存，无缓存请求网络
 * - 点击刷新时：强制从服务器获取最新数据
 * - 一切以服务器数据为准
 * 
 * @property detailRepository 详情数据仓库
 * @property seriesRepository 剧集仓库（用于删除后清理）
 * @property series 当前剧集信息
 */
class DetailViewModel(
    private val detailRepository: DetailRepository,
    private val seriesRepository: SeriesRepository,
    private val series: SeriesLocalData
) : ViewModel() {
    
    // ==================== UI 状态 ====================
    
    private val _uiState = MutableStateFlow(DetailUiState(series = series))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    
    // ==================== 初始化 ====================
    
    init {
        // 初始加载季列表（会优先使用缓存）
        loadSeasons()
        
        // 监听下载状态变化，当本剧集的下载完成时更新 UI
        observeDownloadChanges()
    }
    
    /**
     * 从本地缓存刷新数据
     * 
     * 用于从播放页返回时，重新加载本地最新的进度数据
     * 不会请求网络，只从 MMKV 读取
     */
    fun refreshFromLocalCache() {
        loadSeasons()
    }
    
    /**
     * 监听下载状态变化
     * 
     * 追踪本剧集中每个剧集的下载状态：
     * - 正在下载中
     * - 排队等待中
     * - 已完成
     */
    private fun observeDownloadChanges() {
        viewModelScope.launch {
            DownloadManager.getTasksFlow().collectLatest { tasks ->
                // 筛选属于本剧集的任务
                val seriesTasks = tasks.filter { it.seriesId == series.id }
                
                // 分类任务
                val downloadingIds = seriesTasks
                    .filter { it.status == DownloadStatus.DOWNLOADING }
                    .map { it.episodeId }
                    .toSet()
                
                val pendingIds = seriesTasks
                    .filter { it.status == DownloadStatus.PENDING }
                    .map { it.episodeId }
                    .toSet()
                
                val completedTasks = seriesTasks
                    .filter { it.status == DownloadStatus.COMPLETED }
                
                // 更新 UI 状态
                _uiState.update { state ->
                    var updatedEpisodes = state.episodesBySeasonId
                    
                    // 更新已完成任务的缓存标记
                    if (completedTasks.isNotEmpty()) {
                        updatedEpisodes = updatedEpisodes.toMutableMap().apply {
                            completedTasks.forEach { task ->
                                this[task.seasonId]?.let { episodes ->
                                    this[task.seasonId] = episodes.map { episode ->
                                        if (episode.id == task.episodeId && !episode.isCached) {
                                            episode.copy(isCached = true, localVideoPath = task.localPath)
                                        } else {
                                            episode
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    state.copy(
                        episodesBySeasonId = updatedEpisodes,
                        downloadingEpisodeIds = downloadingIds,
                        pendingEpisodeIds = pendingIds
                    )
                }
            }
        }
    }
    
    // ==================== 加载数据（使用缓存） ====================
    
    /**
     * 加载季列表
     * 
     * 优化策略：
     * - 先让界面渲染，再异步加载数据
     * - 有缓存时不显示加载状态，直接更新数据
     * - 仅在需要网络请求时才显示加载状态
     */
    private fun loadSeasons() {
        viewModelScope.launch(Dispatchers.Default) {
            // 先检查是否有缓存，有缓存时不显示 loading
            val hasCache = detailRepository.hasCache(series.id)
            
            if (!hasCache) {
                // 无缓存，显示加载状态
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                }
            }
            
            val result = detailRepository.getSeasons(series.id)
            
            result.fold(
                onSuccess = { seasons ->
                    withContext(Dispatchers.Main) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                seasons = seasons,
                                error = null
                            )
                        }
                    }
                    
                    // 并行加载每个季的剧集
                    seasons.forEach { season ->
                        loadEpisodes(season.id)
                    }
                },
                onFailure = { exception ->
                    withContext(Dispatchers.Main) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "加载失败"
                            )
                        }
                    }
                }
            )
        }
    }
    
    /**
     * 加载某季的剧集列表
     * 
     * 异步加载，不阻塞 UI
     * 
     * @param seasonId 季 ID
     */
    private fun loadEpisodes(seasonId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val result = detailRepository.getEpisodes(series.id, seasonId)
            
            result.fold(
                onSuccess = { episodes ->
                    withContext(Dispatchers.Main) {
                        _uiState.update { state ->
                            val updatedEpisodes = state.episodesBySeasonId.toMutableMap()
                            updatedEpisodes[seasonId] = episodes
                            state.copy(episodesBySeasonId = updatedEpisodes)
                        }
                        
                        // 检查是否所有季都加载完成，如果是则初始化默认展开的分组
                        val allSeasonsLoaded = _uiState.value.seasons.all { season ->
                            _uiState.value.episodesBySeasonId.containsKey(season.id)
                        }
                        if (allSeasonsLoaded && _uiState.value.expandedGroupKeys.isEmpty()) {
                            initializeDefaultExpandedGroups()
                        }
                    }
                },
                onFailure = { exception ->
                    // 单个季加载失败不影响整体，只记录日志
                    android.util.Log.e("DetailViewModel", "加载剧集失败: ${exception.message}")
                }
            )
        }
    }
    
    // ==================== 刷新数据（强制网络） ====================
    
    /**
     * 同步数据（从服务器拉取）
     * 
     * 从服务器获取最新数据（播放进度、是否看完等），保留本地缓存状态
     * 用户点击同步按钮时调用
     */
    fun syncFromServer() {
        if (_uiState.value.isRefreshing) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            
            android.util.Log.d("DetailViewModel", "syncFromServer: 开始同步 seriesId=${series.id}")
            
            // 强制刷新季列表
            val seasonsResult = detailRepository.refreshSeasons(series.id)
            
            seasonsResult.fold(
                onSuccess = { seasons ->
                    _uiState.update { 
                        it.copy(seasons = seasons)
                    }
                    
                    // 刷新每个季的剧集（会保留本地缓存状态）
                    seasons.forEach { season ->
                        val episodesResult = detailRepository.refreshEpisodes(series.id, season.id)
                        episodesResult.fold(
                            onSuccess = { episodes ->
                                android.util.Log.d("DetailViewModel", "syncFromServer: 季${season.seasonNumber} 获取到 ${episodes.size} 集")
                                episodes.take(3).forEach { ep ->
                                    android.util.Log.d("DetailViewModel", "  第${ep.episodeNumber}集: played=${ep.played}, positionTicks=${ep.playbackPositionTicks}, runTimeTicks=${ep.runTimeTicks}, percent=${ep.getProgressPercent()}%")
                                }
                                _uiState.update { state ->
                                    val updatedEpisodes = state.episodesBySeasonId.toMutableMap()
                                    updatedEpisodes[season.id] = episodes
                                    state.copy(episodesBySeasonId = updatedEpisodes)
                                }
                            },
                            onFailure = { e ->
                                android.util.Log.e("DetailViewModel", "syncFromServer: 季${season.seasonNumber} 失败: ${e.message}")
                            }
                        )
                    }
                    
                    _uiState.update { it.copy(isRefreshing = false) }
                },
                onFailure = { exception ->
                    android.util.Log.e("DetailViewModel", "syncFromServer: 失败 ${exception.message}")
                    _uiState.update { 
                        it.copy(
                            isRefreshing = false,
                            error = exception.message ?: "同步失败"
                        )
                    }
                }
            )
        }
    }
    
    /**
     * 上报进度到服务器
     * 
     * 将本地保存的播放进度上报到服务器
     * 用户点击上报按钮时调用
     */
    fun uploadProgressToServer() {
        if (_uiState.value.isUploading) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            
            val seasons = _uiState.value.seasons
            val count = detailRepository.uploadLocalProgressToServer(series.id, seasons)
            
            _uiState.update { 
                it.copy(
                    isUploading = false,
                    uploadResult = if (count > 0) "已上报 $count 条进度" else "无进度需要上报"
                )
            }
            
            // 3秒后清除提示
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(uploadResult = null) }
        }
    }
    
    // ==================== 标记已看完 ====================
    
    /**
     * 显示标记已看完对话框
     * 
     * @param episode 要标记的剧集
     */
    fun showMarkPlayedDialog(episode: EpisodeLocalData) {
        _uiState.update { it.copy(episodeToMark = episode) }
    }
    
    /**
     * 隐藏标记已看完对话框
     */
    fun hideMarkPlayedDialog() {
        _uiState.update { it.copy(episodeToMark = null) }
    }
    
    /**
     * 确认标记剧集为已看完
     */
    fun confirmMarkAsPlayed() {
        val episode = _uiState.value.episodeToMark ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isMarkingPlayed = true) }
            
            val result = if (episode.played) {
                // 如果已看完，则取消标记
                detailRepository.unmarkAsPlayed(episode.id, episode.seasonId)
            } else {
                // 如果未看完，则标记为已看完
                detailRepository.markAsPlayed(episode.id, episode.seasonId)
            }
            
            result.fold(
                onSuccess = {
                    // 更新本地状态
                    _uiState.update { state ->
                        val updatedEpisodes = state.episodesBySeasonId.toMutableMap()
                        val seasonEpisodes = updatedEpisodes[episode.seasonId]?.toMutableList()
                        
                        seasonEpisodes?.let { list ->
                            val index = list.indexOfFirst { it.id == episode.id }
                            if (index != -1) {
                                list[index] = episode.copy(played = !episode.played)
                                updatedEpisodes[episode.seasonId] = list
                            }
                        }
                        
                        state.copy(
                            episodesBySeasonId = updatedEpisodes,
                            isMarkingPlayed = false,
                            episodeToMark = null
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update { 
                        it.copy(
                            isMarkingPlayed = false,
                            error = exception.message ?: "标记失败"
                        )
                    }
                }
            )
        }
    }
    
    // ==================== 缓存选择模式 ====================
    
    /**
     * 进入缓存选择模式
     */
    fun enterCacheSelectMode() {
        _uiState.update { 
            it.copy(
                isCacheSelectMode = true,
                isDeleteSelectMode = false,
                selectedEpisodeIds = emptySet()
            )
        }
    }
    
    /**
     * 进入删除缓存选择模式
     */
    fun enterDeleteSelectMode() {
        _uiState.update { 
            it.copy(
                isCacheSelectMode = false,
                isDeleteSelectMode = true,
                selectedEpisodeIds = emptySet()
            )
        }
    }
    
    /**
     * 退出选择模式
     */
    fun exitSelectMode() {
        _uiState.update { 
            it.copy(
                isCacheSelectMode = false,
                isDeleteSelectMode = false,
                selectedEpisodeIds = emptySet()
            )
        }
    }
    
    /**
     * 切换剧集的选中状态
     * 
     * @param episodeId 剧集 ID
     */
    fun toggleEpisodeSelection(episodeId: String) {
        _uiState.update { state ->
            val newSet = state.selectedEpisodeIds.toMutableSet()
            if (newSet.contains(episodeId)) {
                newSet.remove(episodeId)
            } else {
                newSet.add(episodeId)
            }
            state.copy(selectedEpisodeIds = newSet)
        }
    }
    
    /**
     * 全选/取消全选
     */
    fun toggleSelectAll() {
        _uiState.update { state ->
            // 获取所有剧集 ID
            val allEpisodeIds = state.episodesBySeasonId.values.flatten().map { it.id }.toSet()
            
            // 判断当前模式
            val validEpisodeIds = if (state.isDeleteSelectMode) {
                // 删除模式：只选择已缓存的
                state.episodesBySeasonId.values.flatten()
                    .filter { it.isCached }
                    .map { it.id }
                    .toSet()
            } else {
                // 缓存模式：选择所有
                allEpisodeIds
            }
            
            val newSet = if (state.selectedEpisodeIds.containsAll(validEpisodeIds)) {
                // 如果全选了，则取消全选
                emptySet()
            } else {
                // 否则全选
                validEpisodeIds
            }
            
            state.copy(selectedEpisodeIds = newSet)
        }
    }
    
    // ==================== 分组展开/折叠 ====================
    
    /**
     * 切换分组的展开/折叠状态
     * 
     * @param groupKey 分组 key（格式：seasonId-start-end）
     */
    fun toggleGroupExpansion(groupKey: String) {
        _uiState.update { state ->
            val newSet = state.expandedGroupKeys.toMutableSet()
            if (newSet.contains(groupKey)) {
                newSet.remove(groupKey)
            } else {
                newSet.add(groupKey)
            }
            state.copy(expandedGroupKeys = newSet)
        }
    }
    
    /**
     * 批量选择/取消选择一个分组的所有剧集
     * 
     * @param seasonId 季 ID
     * @param startEpisode 起始集数
     * @param endEpisode 结束集数
     */
    fun toggleGroupSelection(seasonId: String, startEpisode: Int, endEpisode: Int) {
        _uiState.update { state ->
            // 获取该分组的所有剧集
            val episodes = state.episodesBySeasonId[seasonId]?.filter { episode ->
                episode.episodeNumber in startEpisode..endEpisode
            } ?: return@update state
            
            // 过滤掉不可选的剧集（删除模式下只能选已缓存的）
            val selectableEpisodes = if (state.isDeleteSelectMode) {
                episodes.filter { it.isCached }
            } else {
                episodes
            }
            
            val episodeIds = selectableEpisodes.map { it.id }.toSet()
            
            // 判断是否全选了
            val allSelected = episodeIds.all { state.selectedEpisodeIds.contains(it) }
            
            val newSet = state.selectedEpisodeIds.toMutableSet()
            if (allSelected) {
                // 取消选择
                newSet.removeAll(episodeIds)
            } else {
                // 选择
                newSet.addAll(episodeIds)
            }
            
            state.copy(selectedEpisodeIds = newSet)
        }
    }
    
    /**
     * 初始化默认展开的分组
     * 
     * 策略：展开所有包含"正在看"剧集的分组（有进度但未看完）
     */
    private fun initializeDefaultExpandedGroups() {
        viewModelScope.launch(Dispatchers.Default) {
            val seasons = _uiState.value.seasons
            val episodesBySeasonId = _uiState.value.episodesBySeasonId
            
            if (seasons.isEmpty() || episodesBySeasonId.isEmpty()) return@launch
            
            val defaultExpandedKeys = mutableSetOf<String>()
            
            // 遍历每个季
            for (season in seasons) {
                val episodes = episodesBySeasonId[season.id] ?: continue
                if (episodes.isEmpty()) continue
                
                // 按集数排序
                val sortedEpisodes = episodes.sortedBy { it.episodeNumber }
                
                val groupSize = 10
                var hasAnyWatching = false
                
                // 遍历所有分组
                for (startEpisode in 1..sortedEpisodes.size step groupSize) {
                    val endEpisode = minOf(startEpisode + groupSize - 1, sortedEpisodes.size)
                    val groupEpisodes = sortedEpisodes.filter { 
                        it.episodeNumber in startEpisode..endEpisode 
                    }
                    
                    // 检查这个分组是否有"正在看"的剧集（有进度但未看完）
                    val hasWatching = groupEpisodes.any { 
                        !it.played && it.playbackPositionTicks > 0
                    }
                    
                    if (hasWatching) {
                        val groupKey = "${season.id}-$startEpisode-$endEpisode"
                        defaultExpandedKeys.add(groupKey)
                        hasAnyWatching = true
                    }
                }
                
                // 如果这个季没有"正在看"的，就展开第一个分组
                if (!hasAnyWatching) {
                    val groupKey = "${season.id}-1-${minOf(groupSize, sortedEpisodes.size)}"
                    defaultExpandedKeys.add(groupKey)
                }
            }
            
            // 更新状态
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(expandedGroupKeys = defaultExpandedKeys) }
            }
        }
    }
    
    /**
     * 缓存选中的剧集
     * 
     * 将选中的剧集添加到下载队列
     * 下载任务由 DownloadManager 统一管理
     */
    fun cacheSelectedEpisodes() {
        val selectedIds = _uiState.value.selectedEpisodeIds
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            // 获取选中的剧集
            val selectedEpisodes = _uiState.value.episodesBySeasonId.values
                .flatten()
                .filter { selectedIds.contains(it.id) }
            
            // 获取视频 URL
            val videoUrls = detailRepository.getVideoStreamUrls(selectedIds.toList())
            
            // 添加到下载队列
            DownloadManager.addDownloads(
                episodes = selectedEpisodes,
                seriesName = series.name,
                videoUrls = videoUrls
            )
            
            // 退出选择模式
            _uiState.update { 
                it.copy(
                    isCacheSelectMode = false,
                    selectedEpisodeIds = emptySet()
                )
            }
        }
    }
    
    /**
     * 删除选中剧集的缓存
     * 
     * 删除后会检查：
     * 1. 是否还有剩余的视频缓存
     * 2. 如果没有了，更新 isVideoCached = false
     * 3. 如果该剧集已从服务器删除且没有视频缓存了，触发清理回调
     */
    fun deleteSelectedCache() {
        val selectedIds = _uiState.value.selectedEpisodeIds
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            // 删除本地视频文件和下载记录
            selectedIds.forEach { episodeId ->
                DownloadManager.deleteCompletedDownload(episodeId)
            }
            
            // 更新 UI 状态
            _uiState.update { state ->
                val updatedEpisodes = state.episodesBySeasonId.toMutableMap()
                
                updatedEpisodes.forEach { (seasonId, episodes) ->
                    val newList = episodes.map { episode ->
                        if (selectedIds.contains(episode.id)) {
                            episode.copy(isCached = false, localVideoPath = null)
                        } else {
                            episode
                        }
                    }
                    updatedEpisodes[seasonId] = newList
                }
                
                // 检查是否还有剩余的视频缓存
                val hasRemainingCache = updatedEpisodes.values.flatten().any { it.isCached }
                
                // 更新 Series 的视频缓存状态
                if (!hasRemainingCache) {
                    seriesRepository.updateVideoCacheStatus(series.id, false)
                    
                    // 如果已从服务器删除且没有视频缓存了，需要清理
                    if (series.isDeletedFromServer) {
                        // 删除所有本地数据
                        seriesRepository.deleteSeriesAndCleanup(series.id)
                        return@update state.copy(
                            episodesBySeasonId = updatedEpisodes,
                            isDeleteSelectMode = false,
                            selectedEpisodeIds = emptySet(),
                            shouldNavigateBack = true // 触发返回
                        )
                    }
                }
                
                state.copy(
                    episodesBySeasonId = updatedEpisodes,
                    isDeleteSelectMode = false,
                    selectedEpisodeIds = emptySet()
                )
            }
        }
    }
    
    // ==================== 图片 URL ====================
    
    /**
     * 获取图片 URL
     * 
     * @param itemId 项目 ID
     * @param imageTag 图片标签
     * @param callback 回调
     */
    fun getImageUrl(itemId: String, imageTag: String?, callback: (String?) -> Unit) {
        viewModelScope.launch {
            val url = detailRepository.getImageUrl(itemId, imageTag)
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
}

/**
 * DetailUiState - 详情页 UI 状态
 * 
 * @property series 剧集信息
 * @property seasons 季列表
 * @property episodesBySeasonId 按季 ID 分组的剧集列表
 * @property isLoading 是否正在加载（首次进入）
 * @property isRefreshing 是否正在刷新（点击刷新按钮）
 * @property error 错误信息
 * @property isCacheSelectMode 是否处于缓存选择模式
 * @property isDeleteSelectMode 是否处于删除选择模式
 * @property selectedEpisodeIds 已选中的剧集 ID 集合
 * @property episodeToMark 待标记的剧集（用于显示对话框）
 * @property isMarkingPlayed 是否正在标记已看完
 * @property shouldNavigateBack 是否需要返回上一页（删除已从服务器删除的剧集后触发）
 * @property expandedGroupKeys 已展开的分组 key 集合（格式：seasonId-start-end，如 "s1-1-10"）
 */
data class DetailUiState(
    val series: SeriesLocalData,
    val seasons: List<SeasonLocalData> = emptyList(),
    val episodesBySeasonId: Map<String, List<EpisodeLocalData>> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isCacheSelectMode: Boolean = false,
    val isDeleteSelectMode: Boolean = false,
    val selectedEpisodeIds: Set<String> = emptySet(),
    val episodeToMark: EpisodeLocalData? = null,
    val isMarkingPlayed: Boolean = false,
    val shouldNavigateBack: Boolean = false,
    // 下载状态追踪
    val downloadingEpisodeIds: Set<String> = emptySet(),  // 正在下载中
    val pendingEpisodeIds: Set<String> = emptySet(),      // 排队等待中
    // 上报进度状态
    val isUploading: Boolean = false,
    val uploadResult: String? = null,
    // 分组展开状态
    val expandedGroupKeys: Set<String> = emptySet()       // 已展开的分组
) {
    /**
     * 是否处于任意选择模式
     */
    val isInSelectMode: Boolean
        get() = isCacheSelectMode || isDeleteSelectMode
    
    /**
     * 获取所有剧集数量
     */
    val totalEpisodeCount: Int
        get() = episodesBySeasonId.values.sumOf { it.size }
    
    /**
     * 获取已缓存的剧集数量
     */
    val cachedEpisodeCount: Int
        get() = episodesBySeasonId.values.flatten().count { it.isCached }
}

/**
 * DetailViewModelFactory - DetailViewModel 的工厂类
 * 
 * 用于创建带参数的 ViewModel
 */
class DetailViewModelFactory(
    private val detailRepository: DetailRepository,
    private val seriesRepository: SeriesRepository,
    private val series: SeriesLocalData
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            return DetailViewModel(detailRepository, seriesRepository, series) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

