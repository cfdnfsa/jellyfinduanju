package com.wjp.jellyfinduanju.ui.components.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.media3.exoplayer.ExoPlayer
import com.wjp.jellyfinduanju.data.model.EpisodeLocalData
import com.wjp.jellyfinduanju.ui.components.media.VideoPlayer
import com.wjp.jellyfinduanju.ui.screens.player.PlayerUiState

/**
 * VideoPlayerCallbacks - 视频播放器回调封装
 * 
 * 将所有视频播放相关的回调函数封装到一个数据类中，
 * 减少 VideoContentArea 的参数数量
 * 
 * @param onProgressChanged 播放进度变化回调
 * @param onPlaybackStateChanged 播放状态变化回调
 * @param onSeekComplete 跳转完成回调
 * @param onBufferingChanged 缓冲状态变化回调
 * @param onVideoEnded 视频播放完成回调
 * @param onRetry 重试回调（加载失败时）
 */
data class VideoPlayerCallbacks(
    val onProgressChanged: (Long, Long) -> Unit = { _, _ -> },
    val onPlaybackStateChanged: (Boolean) -> Unit = {},
    val onSeekComplete: () -> Unit = {},
    val onBufferingChanged: (Boolean) -> Unit = {},
    val onVideoEnded: () -> Unit = {},
    val onRetry: () -> Unit = {}
)

/**
 * VideoContentArea - 视频内容区域
 * 
 * 统一管理视频播放器的不同状态显示，
 * 封装横竖屏中重复的视频内容渲染逻辑
 * 
 * 根据状态自动路由到对应的视图：
 * - 当前页 + 有视频 → VideoPlayer + BufferingIndicator
 * - 当前页 + 加载中 → VideoLoadingView
 * - 当前页 + 错误 → VideoErrorView
 * - 非当前页 → EpisodePlaceholder
 * 
 * @param isCurrentPage 是否为当前显示的页面
 * @param episode 剧集数据
 * @param uiState 播放器 UI 状态（包含所有视频相关状态）
 * @param exoPlayer ExoPlayer 实例
 * @param callbacks 视频播放器回调封装
 * @param modifier 布局修饰符
 * 
 * 使用场景：
 * - PlayerContent 中的横屏模式
 * - PlayerContent 中的竖屏模式
 * - 任何需要视频播放的地方
 * 
 * 使用示例：
 * ```
 * val callbacks = VideoPlayerCallbacks(
 *     onProgressChanged = { current, duration -> ... },
 *     onPlaybackStateChanged = { isPlaying -> ... },
 *     onSeekComplete = { ... },
 *     onBufferingChanged = { buffering -> ... },
 *     onVideoEnded = { ... },
 *     onRetry = { ... }
 * )
 * 
 * VideoContentArea(
 *     isCurrentPage = page == pagerState.currentPage,
 *     episode = episode,
 *     uiState = uiState,
 *     exoPlayer = exoPlayer,
 *     callbacks = callbacks
 * )
 * ```
 */
@Composable
fun VideoContentArea(
    isCurrentPage: Boolean,
    episode: EpisodeLocalData,
    uiState: PlayerUiState,
    exoPlayer: ExoPlayer,
    callbacks: VideoPlayerCallbacks,
    modifier: Modifier = Modifier
) {
    // 只有当前页面才渲染视频播放器
    if (isCurrentPage) {
        if (uiState.currentVideoUri != null) {
            // 视频播放器
            // 使用 episode.id 作为 key，确保切换集数时重新创建 VideoPlayer
            key(episode.id) {
                VideoPlayer(
                    exoPlayer = exoPlayer,
                    videoUri = uiState.currentVideoUri,
                    isPlaying = uiState.isPlaying,
                    playbackSpeed = uiState.actualSpeed,
                    seekToPosition = uiState.seekToPosition,
                    isFullscreen = uiState.isFullscreen,
                    onProgressChanged = callbacks.onProgressChanged,
                    onPlaybackStateChanged = callbacks.onPlaybackStateChanged,
                    onSeekComplete = callbacks.onSeekComplete,
                    onBufferingChanged = callbacks.onBufferingChanged,
                    onVideoEnded = callbacks.onVideoEnded,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // 缓冲指示器（视频加载中显示）
            BufferingIndicator(visible = uiState.isBuffering)
        } else if (uiState.isVideoLoading) {
            // 视频源加载中
            VideoLoadingView()
        } else if (uiState.videoError != null) {
            // 视频加载错误 + 重试按钮
            VideoErrorView(
                errorMessage = uiState.videoError,
                onRetry = callbacks.onRetry
            )
        }
    } else {
        // 非当前页面：显示剧集信息占位（预览效果）
        EpisodePlaceholder(
            episodeNumber = episode.episodeNumber,
            episodeName = episode.name
        )
    }
}

