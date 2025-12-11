package com.wjp.jellyfinduanju.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * VideoPlayer - ExoPlayer 视频播放器组件（使用外部提供的 ExoPlayer）
 * 
 * 功能：
 * 1. 支持本地视频和网络视频
 * 2. 支持 HLS 流
 * 3. 暴露播放状态给外部
 * 4. 支持倍速播放
 * 5. 支持进度控制
 * 6. 缓冲状态回调
 * 7. 播放完成回调
 * 
 * @param exoPlayer 外部提供的 ExoPlayer 实例
 * @param videoUri 视频 URI（本地路径或网络 URL）
 * @param isPlaying 是否播放
 * @param playbackSpeed 播放倍速
 * @param seekToPosition 需要跳转的位置（毫秒）
 * @param onPlaybackStateChanged 播放状态变化回调
 * @param onProgressChanged 播放进度变化回调
 * @param onPlayerReady 播放器准备就绪回调
 * @param onSeekComplete 跳转完成回调
 * @param onBufferingChanged 缓冲状态变化回调
 * @param onVideoEnded 视频播放完成回调
 * @param modifier Modifier
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    exoPlayer: ExoPlayer,
    videoUri: String,
    isPlaying: Boolean,
    playbackSpeed: Float = 1.0f,
    seekToPosition: Long? = null,
    onPlaybackStateChanged: (Boolean) -> Unit = {},
    onProgressChanged: (currentPosition: Long, duration: Long) -> Unit = { _, _ -> },
    onPlayerReady: () -> Unit = {},
    onSeekComplete: () -> Unit = {},
    onBufferingChanged: (Boolean) -> Unit = {},
    onVideoEnded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 记录当前播放的 URI
    var currentUri by remember { mutableStateOf("") }
    
    // 当 videoUri 变化时，切换视频
    LaunchedEffect(videoUri) {
        if (videoUri.isNotEmpty() && videoUri != currentUri) {
            currentUri = videoUri
            
            // 判断是本地文件还是网络 URL
            val uri = if (videoUri.startsWith("/") || videoUri.startsWith("file://")) {
                Uri.parse("file://$videoUri")
            } else {
                Uri.parse(videoUri)
            }
            
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }
    
    // 控制播放/暂停
    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }
    
    // 控制倍速
    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }
    
    // 处理跳转
    LaunchedEffect(seekToPosition) {
        seekToPosition?.let { position ->
            exoPlayer.seekTo(position)
            onSeekComplete()
        }
    }
    
    // 播放器事件监听
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        onBufferingChanged(false)
                        onPlayerReady()
                    }
                    Player.STATE_BUFFERING -> {
                        onBufferingChanged(true)
                    }
                    Player.STATE_ENDED -> {
                        onPlaybackStateChanged(false)
                        onVideoEnded()
                    }
                    Player.STATE_IDLE -> {
                        onBufferingChanged(false)
                    }
                }
            }
            
            override fun onIsPlayingChanged(playing: Boolean) {
                onPlaybackStateChanged(playing)
            }
        }
        
        exoPlayer.addListener(listener)
        
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }
    
    // 定时更新进度
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                onProgressChanged(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0))
            }
            kotlinx.coroutines.delay(500)  // 每 500ms 更新一次
        }
    }
    
    // 使用 AndroidView 嵌入 PlayerView
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false  // 不使用默认控制器，我们自定义
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // 设置视频填充模式
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        update = { playerView ->
            // 确保 PlayerView 使用正确的 ExoPlayer 实例
            if (playerView.player != exoPlayer) {
                playerView.player = exoPlayer
            }
        },
        modifier = modifier
    )
}

/**
 * 创建并记住 ExoPlayer 实例
 * 
 * 在 PlayerScreen 层级调用，确保整个播放页面只有一个 ExoPlayer 实例
 */
@OptIn(UnstableApi::class)
@Composable
fun rememberExoPlayer(): ExoPlayer {
    val context = LocalContext.current
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    
    // 在组件销毁时释放播放器
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    return exoPlayer
}

