package com.wjp.jellyfinduanju.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wjp.jellyfinduanju.data.model.EpisodeLocalData
import com.wjp.jellyfinduanju.data.model.SeasonLocalData
import com.wjp.jellyfinduanju.data.model.SeriesLocalData
import com.wjp.jellyfinduanju.data.repository.DetailRepository
import com.wjp.jellyfinduanju.download.DownloadManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PlayerViewModel - 播放页面的 ViewModel
 * 
 * 职责：
 * 1. 管理当前播放的剧集信息
 * 2. 管理所有可播放的剧集列表（用于滑动切换）
 * 3. 获取视频源（本地缓存优先，否则网络流）
 * 4. 管理播放状态（播放/暂停、进度、倍速等）
 * 5. 管理选集弹窗状态
 * 
 * @param detailRepository 详情仓库
 * @param series 剧集信息
 * @param initialEpisodeId 初始播放的集 ID
 */
class PlayerViewModel(
    private val detailRepository: DetailRepository,
    private val series: SeriesLocalData,
    private val initialEpisodeId: String
) : ViewModel() {
    
    // ==================== UI 状态 ====================
    
    private val _uiState = MutableStateFlow(PlayerUiState(series = series))
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    // 进度保存任务
    private var progressSaveJob: Job? = null
    
    // 上次保存进度的时间
    private var lastProgressSaveTime = 0L
    
    // 进度保存间隔（10秒）
    private val progressSaveInterval = 10_000L
    
    // ==================== 初始化 ====================
    
    init {
        loadEpisodes()
        startProgressSaveLoop()
    }
    
    /**
     * 启动进度保存循环
     * 每 10 秒保存一次进度
     */
    private fun startProgressSaveLoop() {
        progressSaveJob = viewModelScope.launch {
            while (true) {
                delay(progressSaveInterval)
                saveCurrentProgress()
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // ViewModel 销毁时保存进度
        viewModelScope.launch {
            saveCurrentProgress(force = true)
        }
        progressSaveJob?.cancel()
    }
    
    /**
     * 加载所有季和剧集
     */
    private fun loadEpisodes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // 加载季列表
            val seasonsResult = detailRepository.getSeasons(series.id)
            
            seasonsResult.fold(
                onSuccess = { seasons ->
                    _uiState.update { it.copy(seasons = seasons) }
                    
                    // 加载每个季的剧集
                    val allEpisodes = mutableListOf<EpisodeLocalData>()
                    seasons.forEach { season ->
                        val episodesResult = detailRepository.getEpisodes(series.id, season.id)
                        episodesResult.onSuccess { episodes ->
                            allEpisodes.addAll(episodes)
                            _uiState.update { state ->
                                val map = state.episodesBySeasonId.toMutableMap()
                                map[season.id] = episodes
                                state.copy(episodesBySeasonId = map)
                            }
                        }
                    }
                    
                    // 设置所有剧集列表（按季和集数排序）
                    val sortedEpisodes = allEpisodes.sortedWith(
                        compareBy({ it.seasonNumber }, { it.episodeNumber })
                    )
                    
                    // 找到初始播放位置
                    val initialIndex = sortedEpisodes.indexOfFirst { it.id == initialEpisodeId }
                        .takeIf { it >= 0 } ?: 0
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            allEpisodes = sortedEpisodes,
                            currentEpisodeIndex = initialIndex
                        )
                    }
                    
                    // 加载初始视频源
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
    
    // ==================== 播放控制 ====================
    
    /**
     * 切换到指定集
     */
    fun switchToEpisode(index: Int) {
        if (index in 0 until _uiState.value.allEpisodes.size && index != _uiState.value.currentEpisodeIndex) {
            _uiState.update { it.copy(currentEpisodeIndex = index) }
            onEpisodeChanged()
        }
    }
    
    /**
     * 切换到指定集（通过 episodeId）
     */
    fun switchToEpisode(episodeId: String) {
        val index = _uiState.value.allEpisodes.indexOfFirst { it.id == episodeId }
        if (index >= 0) {
            switchToEpisode(index)
        }
    }
    
    /**
     * 切换播放/暂停状态
     */
    fun togglePlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }
    
    /**
     * 设置播放状态
     */
    fun setPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }
    
    /**
     * 设置缓冲状态
     */
    fun setBuffering(buffering: Boolean) {
        _uiState.update { it.copy(isBuffering = buffering) }
    }
    
    /**
     * 更新播放进度
     * 
     * 从 VideoPlayer 回调，更新 UI 状态
     */
    fun updateProgress(currentPosition: Long, duration: Long) {
        val previousPosition = _uiState.value.currentPosition
        
        _uiState.update { 
            it.copy(
                currentPosition = currentPosition,
                duration = duration
            )
        }
        
        // 检测视频播放完成（进度 >= 95%）
        if (duration > 0 && currentPosition > 0) {
            val progress = currentPosition.toFloat() / duration
            if (progress >= 0.95f && !_uiState.value.hasReportedComplete) {
                onVideoCompleted()
            }
        }
    }
    
    /**
     * 视频播放完成处理（由进度检测触发）
     */
    private fun onVideoCompleted() {
        val episode = _uiState.value.currentEpisode ?: return
        
        _uiState.update { it.copy(hasReportedComplete = true) }
        
        viewModelScope.launch {
            // 标记为已看完
            detailRepository.markAsPlayed(episode.id, episode.seasonId)
            
            // 上报播放停止
            detailRepository.reportPlaybackStopped(
                episodeId = episode.id,
                positionTicks = _uiState.value.duration * 10_000  // 转换为 ticks
            )
        }
    }
    
    /**
     * 视频播放结束（由 VideoPlayer 的 onVideoEnded 触发）
     * 自动播放下一集，如果是最后一集则停止
     */
    fun onVideoEnded() {
        val currentIndex = _uiState.value.currentEpisodeIndex
        val totalEpisodes = _uiState.value.allEpisodes.size
        
        // 如果不是最后一集，自动播放下一集
        if (currentIndex < totalEpisodes - 1) {
            switchToEpisode(currentIndex + 1)
        } else {
            // 最后一集，停止播放
            _uiState.update { it.copy(isPlaying = false) }
        }
    }
    
    /**
     * 重试加载视频
     */
    fun retryLoadVideo() {
        _uiState.update { it.copy(videoError = null) }
        loadVideoSource()
    }
    
    /**
     * 是否有下一集
     */
    fun hasNextEpisode(): Boolean {
        return _uiState.value.currentEpisodeIndex < _uiState.value.allEpisodes.size - 1
    }
    
    /**
     * 是否有上一集
     */
    fun hasPreviousEpisode(): Boolean {
        return _uiState.value.currentEpisodeIndex > 0
    }
    
    /**
     * 播放下一集
     */
    fun playNextEpisode() {
        if (hasNextEpisode()) {
            switchToEpisode(_uiState.value.currentEpisodeIndex + 1)
        }
    }
    
    /**
     * 播放上一集
     */
    fun playPreviousEpisode() {
        if (hasPreviousEpisode()) {
            switchToEpisode(_uiState.value.currentEpisodeIndex - 1)
        }
    }
    
    /**
     * 保存当前播放进度（静默处理，失败不影响）
     * 
     * @param force 是否强制保存（忽略时间间隔检查）
     */
    private suspend fun saveCurrentProgress(force: Boolean = false) {
        try {
            val state = _uiState.value
            val episode = state.currentEpisode ?: return
            
            // 如果没有有效进度，不保存
            if (state.currentPosition <= 0) return
            
            val now = System.currentTimeMillis()
            if (!force && now - lastProgressSaveTime < progressSaveInterval / 2) {
                return
            }
            lastProgressSaveTime = now
            
            // 转换为 ticks（1秒 = 10,000,000 ticks）
            val positionTicks = state.currentPosition * 10_000
            
            // 保存到本地
            detailRepository.savePlaybackPositionLocal(
                seasonId = episode.seasonId,
                episodeId = episode.id,
                positionTicks = positionTicks
            )
            
            // 上报到服务器（静默，失败不处理）
            try {
                detailRepository.reportPlaybackProgress(
                    episodeId = episode.id,
                    positionTicks = positionTicks,
                    isPaused = !state.isPlaying
                )
            } catch (_: Exception) {
                // 静默处理，汇报失败不影响使用
            }
        } catch (_: Exception) {
            // 静默处理
        }
    }
    
    /**
     * 立即汇报当前进度到服务器（静默处理）
     * 用于 seek 结束后立即汇报
     */
    private fun reportProgressToServer() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val episode = state.currentEpisode ?: return@launch
                if (state.currentPosition <= 0) return@launch
                
                val positionTicks = state.currentPosition * 10_000
                
                // 保存到本地
                detailRepository.savePlaybackPositionLocal(
                    seasonId = episode.seasonId,
                    episodeId = episode.id,
                    positionTicks = positionTicks
                )
                
                // 汇报到服务器（静默）
                detailRepository.reportPlaybackProgress(
                    episodeId = episode.id,
                    positionTicks = positionTicks,
                    isPaused = false
                )
            } catch (_: Exception) {
                // 静默处理
            }
        }
    }
    
    /**
     * 跳转到指定位置
     * 同时更新 currentPosition，让进度条立即显示目标位置
     */
    fun seekTo(position: Long) {
        _uiState.update { 
            it.copy(
                seekToPosition = position,
                currentPosition = position  // 立即更新显示位置
            ) 
        }
    }
    
    /**
     * 清除跳转位置（seek 完成后调用）
     */
    fun clearSeekPosition() {
        _isSeeking = false  // seek 完成，可以响应播放状态回调了
        _isChangingEpisode = false  // 切换集数也完成了
        _uiState.update { it.copy(seekToPosition = null) }
    }
    
    // 是否正在 seeking（用于忽略 seek 期间的播放状态回调）
    private var _isSeeking = false
    
    // 是否正在切换集数（用于忽略切换期间旧视频的播放状态回调）
    private var _isChangingEpisode = false
    
    /**
     * 是否正在 seeking
     */
    fun isSeeking(): Boolean = _isSeeking
    
    /**
     * 是否正在切换集数
     */
    fun isChangingEpisode(): Boolean = _isChangingEpisode
    
    /**
     * 开始拖动进度条（暂停播放）
     */
    fun onSeekStart() {
        if (!_isSeeking) {
            _isSeeking = true
            _uiState.update { it.copy(isPlaying = false) }
        }
    }
    
    /**
     * 结束拖动进度条（恢复播放）
     * 注意：_isSeeking 在 clearSeekPosition 中设为 false（seek 完成后）
     * 
     * @param targetPosition 如果提供，则跳转到该位置并恢复播放
     */
    fun onSeekEnd(targetPosition: Long? = null) {
        // 不在这里设置 _isSeeking = false，等 seek 完成后再设置
        if (targetPosition != null) {
            _uiState.update { 
                it.copy(
                    seekToPosition = targetPosition,
                    currentPosition = targetPosition,
                    isPlaying = true  // 恢复播放
                ) 
            }
            // 立即汇报新进度到服务器（静默）
            reportProgressToServer()
        } else {
            _isSeeking = false  // 没有目标位置，直接结束
            _uiState.update { it.copy(isPlaying = true) }
        }
    }
    
    // ==================== 倍速控制 ====================
    
    /**
     * 设置播放倍速
     */
    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
    }
    
    /**
     * 开始长按倍速
     */
    fun startLongPressSpeed() {
        _uiState.update { it.copy(isLongPressSpeed = true) }
    }
    
    /**
     * 结束长按倍速
     */
    fun endLongPressSpeed() {
        _uiState.update { it.copy(isLongPressSpeed = false) }
    }
    
    // ==================== 选集弹窗 ====================
    
    /**
     * 显示选集弹窗
     */
    fun showEpisodeSelector() {
        _uiState.update { it.copy(showEpisodeSelector = true) }
    }
    
    /**
     * 隐藏选集弹窗
     */
    fun hideEpisodeSelector() {
        _uiState.update { it.copy(showEpisodeSelector = false) }
    }
    
    // ==================== 倍速选择弹窗 ====================
    
    /**
     * 显示倍速选择弹窗
     */
    fun showSpeedSelector() {
        _uiState.update { it.copy(showSpeedSelector = true) }
    }
    
    /**
     * 隐藏倍速选择弹窗
     */
    fun hideSpeedSelector() {
        _uiState.update { it.copy(showSpeedSelector = false) }
    }
    
    // ==================== 横竖屏控制 ====================
    
    /**
     * 设置全屏状态
     */
    fun setFullscreen(fullscreen: Boolean) {
        _uiState.update { it.copy(isFullscreen = fullscreen) }
    }
    
    /**
     * 切换全屏状态
     */
    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }
    
    // ==================== 视频源 ====================
    
    /**
     * 加载当前剧集的视频源
     * 优先使用本地缓存，否则从网络获取
     * 同时恢复上次播放位置（历史进度）
     */
    fun loadVideoSource() {
        val episode = _uiState.value.currentEpisode ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isVideoLoading = true, videoError = null, hasReportedComplete = false) }
            
            // 获取保存的播放位置（转换 ticks 为毫秒）- 恢复历史进度
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
                        isPlaying = true  // 自动播放
                    )
                }
                
                // 如果没有历史进度，直接清除切换标志（不需要等 seek 完成）
                if (savedPosition <= 0) {
                    _isChangingEpisode = false
                }
                
                // 上报播放开始（静默）
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
                        isPlaying = true  // 自动播放
                    )
                }
                
                // 如果没有历史进度，直接清除切换标志
                if (savedPosition <= 0) {
                    _isChangingEpisode = false
                }
                
                // 上报播放开始（静默）
                try {
                    detailRepository.reportPlaybackStart(episode.id, episode.playbackPositionTicks)
                } catch (_: Exception) { }
            } else {
                _isChangingEpisode = false  // 加载失败也要清除
                _uiState.update { 
                    it.copy(
                        isVideoLoading = false,
                        videoError = "获取视频源失败"
                    )
                }
            }
        }
    }
    
    /**
     * 当切换剧集后重新加载视频源
     * 1. 保存当前进度到服务器（静默）
     * 2. 重置状态
     * 3. 加载新视频（自动播放，恢复历史进度）
     */
    private fun onEpisodeChanged() {
        // 标记正在切换集数，忽略旧视频的播放状态回调
        _isChangingEpisode = true
        
        // 保存当前进度（静默处理）
        viewModelScope.launch {
            saveCurrentProgress(force = true)
        }
        
        // 重置播放状态
        _uiState.update { 
            it.copy(
                currentPosition = 0,
                duration = 0,
                currentVideoUri = null,
                hasReportedComplete = false
            )
        }
        // 加载新视频（会自动播放，并恢复历史进度）
        loadVideoSource()
    }
    
    // ==================== 错误处理 ====================
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * 播放页 UI 状态
 */
data class PlayerUiState(
    val series: SeriesLocalData,
    val seasons: List<SeasonLocalData> = emptyList(),
    val episodesBySeasonId: Map<String, List<EpisodeLocalData>> = emptyMap(),
    val allEpisodes: List<EpisodeLocalData> = emptyList(),  // 所有剧集（排序后）
    val currentEpisodeIndex: Int = 0,                        // 当前播放索引
    val isLoading: Boolean = true,
    val error: String? = null,
    
    // 视频源
    val currentVideoUri: String? = null,  // 当前视频 URI（本地路径或网络 URL）
    val isVideoLoading: Boolean = false,  // 视频加载中
    val isLocalVideo: Boolean = false,    // 是否为本地视频
    val videoError: String? = null,       // 视频加载错误
    
    // 播放状态
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,    // 缓冲中
    val currentPosition: Long = 0,       // 当前位置（毫秒）
    val duration: Long = 0,              // 总时长（毫秒）
    val seekToPosition: Long? = null,    // 需要跳转的位置
    
    // 倍速
    val playbackSpeed: Float = 1.0f,     // 当前倍速
    val isLongPressSpeed: Boolean = false, // 是否长按倍速中
    
    // 弹窗状态
    val showEpisodeSelector: Boolean = false,  // 选集弹窗
    val showSpeedSelector: Boolean = false,    // 倍速选择弹窗
    
    // 横竖屏
    val isFullscreen: Boolean = false,
    
    // 播放完成标记（防止重复上报）
    val hasReportedComplete: Boolean = false
) {
    /**
     * 当前播放的剧集
     */
    val currentEpisode: EpisodeLocalData?
        get() = allEpisodes.getOrNull(currentEpisodeIndex)
    
    /**
     * 播放进度百分比
     */
    val progressPercent: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration else 0f
    
    /**
     * 格式化当前位置
     */
    val currentPositionText: String
        get() = formatDuration(currentPosition)
    
    /**
     * 格式化总时长
     */
    val durationText: String
        get() = formatDuration(duration)
    
    /**
     * 实际播放倍速（长按时使用 3x，否则使用设置的倍速）
     */
    val actualSpeed: Float
        get() = if (isLongPressSpeed) 3.0f else playbackSpeed
    
    companion object {
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
 * PlayerViewModel 工厂类
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

