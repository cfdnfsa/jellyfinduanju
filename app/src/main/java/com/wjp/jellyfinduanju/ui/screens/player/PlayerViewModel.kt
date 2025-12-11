package com.wjp.jellyfinduanju.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wjp.jellyfinduanju.data.model.EpisodeLocalData
import com.wjp.jellyfinduanju.data.model.SeasonLocalData
import com.wjp.jellyfinduanju.data.model.SeriesLocalData
import com.wjp.jellyfinduanju.data.repository.DetailRepository
import com.wjp.jellyfinduanju.data.settings.AppSettings
import com.wjp.jellyfinduanju.download.DownloadManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ============================================================================
 * PlayerViewModel - 视频播放页面的 ViewModel
 * ============================================================================
 * 
 * 【核心职责】
 * 1. 剧集数据管理：加载所有季和剧集，维护当前播放索引
 * 2. 视频源获取：优先本地缓存，无缓存则使用网络流
 * 3. 播放状态管理：播放/暂停、进度、倍速、缓冲状态
 * 4. 进度同步：定时保存进度到本地和服务器
 * 5. UI 状态管理：选集弹窗、倍速弹窗、全屏状态
 * 
 * 【状态流转】
 * 1. 初始化 → loadEpisodes() → 加载所有季和剧集
 * 2. 加载完成 → loadVideoSource() → 获取视频源并自动播放
 * 3. 切换集数 → onEpisodeChanged() → 保存进度 + 重新加载
 * 4. seek 操作 → onSeekStart() → 用户拖动中 → onSeekEnd() → 恢复播放
 * 
 * 【关键状态标记】
 * - isSeeking: 用户正在拖动进度条/滑动调整进度
 * - isChangingEpisode: 正在切换集数（忽略旧视频的播放状态回调）
 * 这两个标记用于防止 ExoPlayer 的状态回调覆盖用户意图
 * 
 * @param detailRepository 详情仓库，用于获取剧集数据和上报进度
 * @param series 当前剧集的基本信息
 * @param initialEpisodeId 初始播放的集 ID（从详情页传入）
 */
class PlayerViewModel(
    private val detailRepository: DetailRepository,
    private val series: SeriesLocalData,
    private val initialEpisodeId: String
) : ViewModel() {
    
    // ============================================================================
    // UI 状态
    // ============================================================================
    
    /**
     * UI 状态流
     * 
     * 使用 StateFlow 实现单向数据流：
     * - _uiState: 内部可修改的 MutableStateFlow
     * - uiState: 外部只读的 StateFlow
     * 
     * 所有 UI 相关状态都放在 PlayerUiState 中统一管理
     */
    private val _uiState = MutableStateFlow(PlayerUiState(series = series))
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    /** 进度保存的协程任务，用于取消 */
    private var progressSaveJob: Job? = null
    
    /** 上次保存进度的时间戳，用于防抖 */
    private var lastProgressSaveTime = 0L
    
    /** 进度保存间隔：10秒 */
    private val progressSaveInterval = 10_000L
    
    // ============================================================================
    // 初始化
    // ============================================================================
    
    init {
        // ViewModel 创建时自动加载剧集数据
        loadEpisodes()
        // 启动定时保存进度的循环
        startProgressSaveLoop()
    }
    
    /**
     * 启动进度保存循环
     * 
     * 每隔 10 秒自动保存一次播放进度到本地和服务器
     * 这是一个无限循环，在 ViewModel 销毁时会被取消
     */
    private fun startProgressSaveLoop() {
        progressSaveJob = viewModelScope.launch {
            while (true) {
                delay(progressSaveInterval)
                saveCurrentProgress()
            }
        }
    }
    
    /**
     * ViewModel 销毁时的清理工作
     * 
     * 1. 强制保存当前进度（确保不丢失）
     * 2. 取消进度保存的协程任务
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            saveCurrentProgress(force = true)
        }
        progressSaveJob?.cancel()
    }
    
    /**
     * 加载所有季和剧集数据
     * 
     * 【流程】
     * 1. 设置 isLoading = true
     * 2. 从 Repository 加载季列表
     * 3. 遍历每个季，加载该季的所有剧集
     * 4. 将所有剧集按 季号→集号 排序
     * 5. 找到初始播放的集的索引
     * 6. 加载视频源
     */
    private fun loadEpisodes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val seasonsResult = detailRepository.getSeasons(series.id)
            
            seasonsResult.fold(
                onSuccess = { seasons ->
                    _uiState.update { it.copy(seasons = seasons) }
                    
                    // 收集所有剧集
                    val allEpisodes = mutableListOf<EpisodeLocalData>()
                    seasons.forEach { season ->
                        val episodesResult = detailRepository.getEpisodes(series.id, season.id)
                        episodesResult.onSuccess { episodes ->
                            allEpisodes.addAll(episodes)
                            // 同时维护按季分组的 Map（用于选集弹窗）
                            _uiState.update { state ->
                                val map = state.episodesBySeasonId.toMutableMap()
                                map[season.id] = episodes
                                state.copy(episodesBySeasonId = map)
                            }
                        }
                    }
                    
                    // 排序：先按季号，再按集号
                    val sortedEpisodes = allEpisodes.sortedWith(
                        compareBy({ it.seasonNumber }, { it.episodeNumber })
                    )
                    
                    // 找到初始播放位置（如果找不到默认第一集）
                    val initialIndex = sortedEpisodes.indexOfFirst { it.id == initialEpisodeId }
                        .takeIf { it >= 0 } ?: 0
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            allEpisodes = sortedEpisodes,
                            currentEpisodeIndex = initialIndex
                        )
                    }
                    
                    // 加载视频源
                    loadVideoSource()
                },
                onFailure = { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message
                        )
                    }
                }
            )
        }
    }
    
    // ============================================================================
    // 播放控制
    // ============================================================================
    
    /**
     * 切换到指定集（通过索引）
     * 
     * @param index 目标集在 allEpisodes 中的索引
     * 
     * 会触发 onEpisodeChanged() 进行视频切换
     */
    fun switchToEpisode(index: Int) {
        if (index in 0 until uiState.value.allEpisodes.size && index != uiState.value.currentEpisodeIndex) {
            _uiState.update { it.copy(currentEpisodeIndex = index) }
            onEpisodeChanged()
        }
    }
    
    /**
     * 切换到指定集（通过 episodeId）
     * 
     * @param episodeId 目标集的 ID
     * 
     * 内部转换为索引后调用 switchToEpisode(index)
     */
    fun switchToEpisode(episodeId: String) {
        val index = uiState.value.allEpisodes.indexOfFirst { it.id == episodeId }
        if (index >= 0) {
            switchToEpisode(index)
        }
    }
    
    /** 切换播放/暂停状态 */
    fun togglePlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }
    
    /** 设置播放状态（由 VideoPlayer 回调） */
    fun setPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }
    
    /** 设置缓冲状态（由 VideoPlayer 回调） */
    fun setBuffering(buffering: Boolean) {
        _uiState.update { it.copy(isBuffering = buffering) }
    }
    
    /**
     * 更新播放进度
     * 
     * @param currentPosition 当前播放位置（毫秒）
     * @param duration 视频总时长（毫秒）
     * 
     * 由 VideoPlayer 定期回调，同时检测是否播放完成（>= 95%）
     */
    fun updateProgress(currentPosition: Long, duration: Long) {
        _uiState.update { 
            it.copy(
                currentPosition = currentPosition,
                duration = duration
            )
        }
        
        // 检测播放完成（进度 >= 95% 视为看完）
        if (duration > 0 && currentPosition > 0) {
            val progress = currentPosition.toFloat() / duration
            if (progress >= 0.95f && !uiState.value.hasReportedComplete) {
                onVideoCompleted()
            }
        }
    }
    
    /**
     * 视频播放完成处理
     * 
     * 1. 标记为已看完（上报服务器）
     * 2. 上报播放停止
     * 
     * hasReportedComplete 防止重复上报
     */
    private fun onVideoCompleted() {
        val episode = uiState.value.currentEpisode ?: return
        
        _uiState.update { it.copy(hasReportedComplete = true) }
        
        viewModelScope.launch {
            detailRepository.markAsPlayed(episode.id, episode.seasonId)
            detailRepository.reportPlaybackStopped(
                episodeId = episode.id,
                positionTicks = uiState.value.duration * 10_000  // 毫秒转 ticks
            )
        }
    }
    
    /**
     * 视频播放结束回调（由 ExoPlayer 的 STATE_ENDED 触发）
     * 
     * 根据设置决定是否自动播放下一集
     */
    fun onVideoEnded() {
        val currentIndex = uiState.value.currentEpisodeIndex
        val totalEpisodes = uiState.value.allEpisodes.size
        
        // 检查是否开启自动播放下一集
        if (AppSettings.autoPlayNext && currentIndex < totalEpisodes - 1) {
            switchToEpisode(currentIndex + 1)
        } else {
            _uiState.update { it.copy(isPlaying = false) }
        }
    }
    
    /** 重试加载视频（用户点击重试按钮） */
    fun retryLoadVideo() {
        _uiState.update { it.copy(videoError = null) }
        loadVideoSource()
    }
    
    /** 是否有下一集 */
    fun hasNextEpisode(): Boolean {
        return uiState.value.currentEpisodeIndex < uiState.value.allEpisodes.size - 1
    }
    
    /** 是否有上一集 */
    fun hasPreviousEpisode(): Boolean {
        return uiState.value.currentEpisodeIndex > 0
    }
    
    /** 播放下一集 */
    fun playNextEpisode() {
        if (hasNextEpisode()) {
            switchToEpisode(uiState.value.currentEpisodeIndex + 1)
        }
    }
    
    /** 播放上一集 */
    fun playPreviousEpisode() {
        if (hasPreviousEpisode()) {
            switchToEpisode(uiState.value.currentEpisodeIndex - 1)
        }
    }
    
    /**
     * 保存当前播放进度
     * 
     * @param force 是否强制保存（忽略时间间隔检查）
     * 
     * 【流程】
     * 1. 转换 毫秒 → ticks（Jellyfin 格式）
     * 2. 保存到本地 MMKV
     * 3. 上报到服务器（静默，失败不影响）
     * 
     * 注意：这是一个 suspend 函数，需要在协程中调用
     */
    private suspend fun saveCurrentProgress(force: Boolean = false) {
        try {
            val state = uiState.value
            val episode = state.currentEpisode ?: return
            
            if (state.currentPosition <= 0) return
            
            // 防抖：距离上次保存不足 5 秒则跳过
            val now = System.currentTimeMillis()
            if (!force && now - lastProgressSaveTime < progressSaveInterval / 2) {
                return
            }
            lastProgressSaveTime = now
            
            // 毫秒转 ticks：1毫秒 = 10,000 ticks
            val positionTicks = state.currentPosition * 10_000
            
            // 保存到本地
            detailRepository.savePlaybackPositionLocal(
                seasonId = episode.seasonId,
                episodeId = episode.id,
                positionTicks = positionTicks
            )
            
            // 上报到服务器（静默处理）
            try {
                detailRepository.reportPlaybackProgress(
                    episodeId = episode.id,
                    positionTicks = positionTicks,
                    isPaused = !state.isPlaying
                )
            } catch (_: Exception) { }
        } catch (_: Exception) { }
    }
    
    /**
     * 立即汇报进度到服务器
     * 
     * 用于 seek 结束后立即同步进度，不等待定时器
     */
    private fun reportProgressToServer() {
        viewModelScope.launch {
            try {
                val state = uiState.value
                val episode = state.currentEpisode ?: return@launch
                if (state.currentPosition <= 0) return@launch
                
                val positionTicks = state.currentPosition * 10_000
                
                detailRepository.savePlaybackPositionLocal(
                    seasonId = episode.seasonId,
                    episodeId = episode.id,
                    positionTicks = positionTicks
                )
                
                detailRepository.reportPlaybackProgress(
                    episodeId = episode.id,
                    positionTicks = positionTicks,
                    isPaused = false
                )
            } catch (_: Exception) { }
        }
    }
    
    // ============================================================================
    // Seek 相关（进度拖动/滑动）
    // ============================================================================
    
    /**
     * 跳转到指定位置
     * 
     * @param position 目标位置（毫秒）
     * 
     * 同时更新 currentPosition，让进度条立即显示目标位置
     */
    fun seekTo(position: Long) {
        _uiState.update { 
            it.copy(
                seekToPosition = position,
                currentPosition = position
            ) 
        }
    }
    
    /**
     * 清除跳转位置（ExoPlayer seek 完成后调用）
     * 
     * 同时清除 isSeeking 和 isChangingEpisode 标记
     */
    fun clearSeekPosition() {
        _uiState.update { it.copy(seekToPosition = null, isSeeking = false, isChangingEpisode = false) }
    }
    
    /**
     * 开始拖动/滑动
     * 
     * 设置 isSeeking = true，暂停播放
     * isSeeking 用于忽略 ExoPlayer 的播放状态回调
     */
    fun onSeekStart() {
        _uiState.update { it.copy(isSeeking = true, isPlaying = false) }
    }
    
    /**
     * 结束拖动/滑动
     * 
     * @param targetPosition 跳转目标位置，null 则不跳转
     * 
     * 【流程】
     * 1. 如果有目标位置：设置 seekToPosition + 恢复播放 + 汇报进度
     * 2. 如果无目标位置：只恢复播放
     * 
     * 注意：isSeeking 在 clearSeekPosition() 中清除（seek 完成后）
     */
    fun onSeekEnd(targetPosition: Long? = null) {
        if (targetPosition != null) {
            _uiState.update { 
                it.copy(
                    seekToPosition = targetPosition,
                    currentPosition = targetPosition,
                    isPlaying = true
                ) 
            }
            reportProgressToServer()
        } else {
            _uiState.update { it.copy(isSeeking = false, isPlaying = true) }
        }
    }
    
    // ============================================================================
    // 倍速控制
    // ============================================================================
    
    /** 设置播放倍速 */
    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
    }
    
    /** 开始长按倍速（临时 3x） */
    fun startLongPressSpeed() {
        _uiState.update { it.copy(isLongPressSpeed = true) }
    }
    
    /** 结束长按倍速 */
    fun endLongPressSpeed() {
        _uiState.update { it.copy(isLongPressSpeed = false) }
    }
    
    // ============================================================================
    // 弹窗控制
    // ============================================================================
    
    /** 显示选集弹窗 */
    fun showEpisodeSelector() {
        _uiState.update { it.copy(showEpisodeSelector = true) }
    }
    
    /** 隐藏选集弹窗 */
    fun hideEpisodeSelector() {
        _uiState.update { it.copy(showEpisodeSelector = false) }
    }
    
    /** 显示倍速选择弹窗 */
    fun showSpeedSelector() {
        _uiState.update { it.copy(showSpeedSelector = true) }
    }
    
    /** 隐藏倍速选择弹窗 */
    fun hideSpeedSelector() {
        _uiState.update { it.copy(showSpeedSelector = false) }
    }
    
    // ============================================================================
    // 横竖屏控制
    // ============================================================================
    
    /** 设置全屏状态 */
    fun setFullscreen(fullscreen: Boolean) {
        _uiState.update { it.copy(isFullscreen = fullscreen) }
    }
    
    /** 切换全屏状态 */
    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }
    
    // ============================================================================
    // 视频源加载
    // ============================================================================
    
    /**
     * 加载当前剧集的视频源
     * 
     * 【优先级】
     * 1. 本地缓存（已下载的视频文件）
     * 2. 网络流（直接播放服务器视频）
     * 
     * 【流程】
     * 1. 获取历史播放进度（用于恢复）
     * 2. 检查本地缓存文件
     * 3. 无缓存则获取网络流 URL
     * 4. 设置 isPlaying = true 自动播放
     * 5. 上报播放开始到服务器
     */
    fun loadVideoSource() {
        val episode = uiState.value.currentEpisode ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isVideoLoading = true, videoError = null, hasReportedComplete = false) }
            
            // 获取历史进度（ticks 转毫秒）
            val savedPosition = episode.playbackPositionTicks / 10_000
            if (savedPosition > 0) {
                _uiState.update { it.copy(seekToPosition = savedPosition) }
            }
            
            // 优先使用本地缓存
            val cachedFile = DownloadManager.getCachedVideoFile(episode.seriesId, episode.id)
            if (cachedFile != null && cachedFile.exists()) {
                _uiState.update { 
                    it.copy(
                        currentVideoUri = cachedFile.absolutePath,
                        isVideoLoading = false,
                        isLocalVideo = true,
                        isPlaying = true
                    )
                }
                
                if (savedPosition <= 0) {
                    _uiState.update { it.copy(isChangingEpisode = false) }
                }
                
                try {
                    detailRepository.reportPlaybackStart(episode.id, episode.playbackPositionTicks)
                } catch (_: Exception) { }
                return@launch
            }
            
            // 无缓存，使用网络流
            val videoUrl = detailRepository.getVideoStreamUrl(episode.id)
            if (videoUrl != null) {
                _uiState.update { 
                    it.copy(
                        currentVideoUri = videoUrl,
                        isVideoLoading = false,
                        isLocalVideo = false,
                        isPlaying = true
                    )
                }
                
                if (savedPosition <= 0) {
                    _uiState.update { it.copy(isChangingEpisode = false) }
                }
                
                try {
                    detailRepository.reportPlaybackStart(episode.id, episode.playbackPositionTicks)
                } catch (_: Exception) { }
            } else {
                _uiState.update { 
                    it.copy(
                        isVideoLoading = false,
                        videoError = "获取视频源失败",
                        isChangingEpisode = false
                    )
                }
            }
        }
    }
    
    /**
     * 切换剧集后的处理
     * 
     * 【流程】
     * 1. 设置 isChangingEpisode = true（忽略旧视频的回调）
     * 2. 保存当前进度
     * 3. 重置播放状态
     * 4. 加载新视频
     */
    private fun onEpisodeChanged() {
        viewModelScope.launch {
            saveCurrentProgress(force = true)
        }
        
        _uiState.update { 
            it.copy(
                currentPosition = 0,
                duration = 0,
                currentVideoUri = null,
                hasReportedComplete = false,
                isChangingEpisode = true
            )
        }
        
        loadVideoSource()
    }
    
    // ============================================================================
    // 错误处理
    // ============================================================================
    
    /** 清除错误信息 */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * ============================================================================
 * PlayerUiState - 播放页 UI 状态
 * ============================================================================
 * 
 * 包含播放页面所有 UI 相关的状态，使用 data class 实现不可变性
 * 通过 copy() 方法创建新实例来更新状态
 */
data class PlayerUiState(
    // ==================== 剧集数据 ====================
    /** 当前剧集的基本信息 */
    val series: SeriesLocalData,
    /** 所有季列表 */
    val seasons: List<SeasonLocalData> = emptyList(),
    /** 按季 ID 分组的剧集（用于选集弹窗） */
    val episodesBySeasonId: Map<String, List<EpisodeLocalData>> = emptyMap(),
    /** 所有剧集列表（按季号→集号排序，用于 VerticalPager） */
    val allEpisodes: List<EpisodeLocalData> = emptyList(),
    /** 当前播放的集在 allEpisodes 中的索引 */
    val currentEpisodeIndex: Int = 0,
    /** 数据加载中 */
    val isLoading: Boolean = true,
    /** 错误信息 */
    val error: String? = null,
    
    // ==================== 视频源 ====================
    /** 当前视频 URI（本地路径如 /data/.../xxx.mp4 或网络 URL） */
    val currentVideoUri: String? = null,
    /** 视频源加载中 */
    val isVideoLoading: Boolean = false,
    /** 是否为本地视频（区分缓存和网络流） */
    val isLocalVideo: Boolean = false,
    /** 视频加载错误信息 */
    val videoError: String? = null,
    
    // ==================== 播放状态 ====================
    /** 是否正在播放 */
    val isPlaying: Boolean = false,
    /** 是否正在缓冲 */
    val isBuffering: Boolean = false,
    /** 当前播放位置（毫秒） */
    val currentPosition: Long = 0,
    /** 视频总时长（毫秒） */
    val duration: Long = 0,
    /** 需要跳转的位置（毫秒），ExoPlayer 会读取并执行 seek */
    val seekToPosition: Long? = null,
    
    // ==================== 倍速 ====================
    /** 当前设置的倍速（默认使用 AppSettings 中的设置） */
    val playbackSpeed: Float = AppSettings.defaultPlaybackSpeed,
    /** 是否处于长按倍速状态 */
    val isLongPressSpeed: Boolean = false,
    /** 长按倍速值（从 AppSettings 读取） */
    val longPressSpeedValue: Float = AppSettings.longPressSpeed,
    
    // ==================== 弹窗状态 ====================
    /** 选集弹窗是否显示 */
    val showEpisodeSelector: Boolean = false,
    /** 倍速选择弹窗是否显示 */
    val showSpeedSelector: Boolean = false,
    
    // ==================== 其他状态 ====================
    /** 是否全屏（横屏）模式 */
    val isFullscreen: Boolean = false,
    /** 是否已上报播放完成（防止重复上报） */
    val hasReportedComplete: Boolean = false,
    /** 是否正在 seek（拖动进度条/滑动），用于忽略 ExoPlayer 回调 */
    val isSeeking: Boolean = false,
    /** 是否正在切换集数，用于忽略旧视频的 ExoPlayer 回调 */
    val isChangingEpisode: Boolean = false
) {
    // ==================== 计算属性 ====================
    
    /** 当前播放的剧集（可能为 null） */
    val currentEpisode: EpisodeLocalData?
        get() = allEpisodes.getOrNull(currentEpisodeIndex)
    
    /** 播放进度百分比 (0.0 ~ 1.0) */
    val progressPercent: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration else 0f
    
    /** 格式化的当前位置（如 "12:34" 或 "1:23:45"） */
    val currentPositionText: String
        get() = formatDuration(currentPosition)
    
    /** 格式化的总时长 */
    val durationText: String
        get() = formatDuration(duration)
    
    /** 实际播放倍速（长按时使用 longPressSpeedValue，否则使用 playbackSpeed） */
    val actualSpeed: Float
        get() = if (isLongPressSpeed) longPressSpeedValue else playbackSpeed
    
    companion object {
        /**
         * 格式化时长
         * 
         * @param ms 毫秒数
         * @return 格式化字符串，如 "12:34" 或 "1:23:45"
         */
        fun formatDuration(ms: Long): String {
            val totalSeconds = ms / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }
}

/**
 * ============================================================================
 * PlayerViewModelFactory - ViewModel 工厂类
 * ============================================================================
 * 
 * 由于 PlayerViewModel 需要构造参数，不能直接使用默认的 ViewModelProvider
 * 需要自定义工厂类来创建 ViewModel 实例
 * 
 * 使用方式：
 * ```
 * val viewModel: PlayerViewModel = viewModel(
 *     factory = PlayerViewModelFactory(repository, series, episodeId)
 * )
 * ```
 */
class PlayerViewModelFactory(
    private val detailRepository: DetailRepository,
    private val series: SeriesLocalData,
    private val initialEpisodeId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            return PlayerViewModel(detailRepository, series, initialEpisodeId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
