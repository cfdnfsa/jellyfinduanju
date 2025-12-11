package com.wjp.jellyfinduanju.ui.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.wjp.jellyfinduanju.ui.components.VideoPlayer
import com.wjp.jellyfinduanju.ui.components.rememberExoPlayer

/**
 * PlayerScreen - 视频播放页面
 * 
 * 功能：
 * 1. 全屏视频播放（支持横竖屏）
 * 2. 上下滑动切换集数（抖音风格）
 * 3. 透明顶部栏（返回、集数、倍速）
 * 4. 底部选集栏
 * 5. 长按倍速播放
 * 
 * @param viewModel 播放页 ViewModel
 * @param onBack 返回回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    
    // 创建单一的 ExoPlayer 实例（整个播放页面共享）
    val exoPlayer = rememberExoPlayer()
    
    // 选集弹窗状态
    val episodeSelectorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // 控制栏显示状态
    var showControls by remember { mutableStateOf(true) }
    
    // VerticalPager 状态
    val pagerState = rememberPagerState(
        initialPage = uiState.currentEpisodeIndex,
        pageCount = { uiState.allEpisodes.size }
    )
    
    // 隐藏系统导航栏（进入时隐藏，退出时恢复）
    DisposableEffect(activity) {
        activity?.window?.let { window ->
            // 让内容扩展到系统栏区域
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // 获取 InsetsController
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            
            // 隐藏导航栏
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            
            // 设置行为：从边缘滑动可临时显示
            controller.systemBarsBehavior = 
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        onDispose {
            // 退出时恢复导航栏
            activity?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.navigationBars())
                // 恢复默认行为
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }
    }
    
    // 监听 Pager 滑动变化
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page != uiState.currentEpisodeIndex) {
                viewModel.switchToEpisode(page)
            }
        }
    }
    
    // 当 ViewModel 中的 currentEpisodeIndex 变化时（从选集弹窗切换），同步到 pager
    LaunchedEffect(uiState.currentEpisodeIndex) {
        if (pagerState.currentPage != uiState.currentEpisodeIndex) {
            pagerState.animateScrollToPage(uiState.currentEpisodeIndex)
        }
    }
    
    // 处理返回键
    BackHandler {
        if (uiState.isFullscreen) {
            // 横屏时返回竖屏
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            viewModel.setFullscreen(false)
        } else if (uiState.showEpisodeSelector) {
            viewModel.hideEpisodeSelector()
        } else {
            onBack()
        }
    }
    
    // 切换全屏的辅助函数
    val toggleFullscreen: () -> Unit = {
        if (uiState.isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            viewModel.setFullscreen(false)
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            viewModel.setFullscreen(true)
        }
    }
    
    // 监听横竖屏状态，退出时恢复
    DisposableEffect(Unit) {
        onDispose {
            // 退出时恢复竖屏
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (uiState.isLoading) {
            // 加载中
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else if (uiState.allEpisodes.isNotEmpty()) {
            // VerticalPager 实现抖音式滑动
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 0  // 不预加载其他页面，只显示当前页
            ) { page ->
                val isCurrentPage = page == pagerState.currentPage
                
                // 每页都是一个 PlayerContent
                PlayerContent(
                    exoPlayer = exoPlayer,
                    uiState = uiState,
                    episode = uiState.allEpisodes[page],
                    isCurrentPage = isCurrentPage,
                    showControls = showControls && isCurrentPage,
                    onToggleControls = { showControls = !showControls },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSeek = { viewModel.seekTo(it) },
                    onSeekStart = { viewModel.onSeekStart() },
                    onSeekEnd = { targetPos -> viewModel.onSeekEnd(targetPos) },
                    onBack = {
                        if (uiState.isFullscreen) {
                            // 横屏时先退出全屏
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            viewModel.setFullscreen(false)
                        } else {
                            onBack()
                        }
                    },
                    onShowSpeedSelector = { viewModel.showSpeedSelector() },
                    onShowEpisodeSelector = { viewModel.showEpisodeSelector() },
                    onToggleFullscreen = toggleFullscreen,
                    onProgressChanged = { current, duration ->
                        if (isCurrentPage) {
                            viewModel.updateProgress(current, duration)
                        }
                    },
                    onPlaybackStateChanged = { isPlaying ->
                        // 只在非 seeking/切换集数 状态下同步播放器状态
                        val state = viewModel.uiState.value
                        if (isCurrentPage && !state.isSeeking && !state.isChangingEpisode) {
                            viewModel.setPlaying(isPlaying)
                        }
                    },
                    onSeekComplete = {
                        viewModel.clearSeekPosition()
                    },
                    onBufferingChanged = { buffering ->
                        if (isCurrentPage) {
                            viewModel.setBuffering(buffering)
                        }
                    },
                    onVideoEnded = {
                        if (isCurrentPage) {
                            viewModel.onVideoEnded()
                        }
                    },
                    onRetry = {
                        viewModel.retryLoadVideo()
                    },
                    onLongPressStart = {
                        viewModel.startLongPressSpeed()
                    },
                    onLongPressEnd = {
                        viewModel.endLongPressSpeed()
                    }
                )
            }
        } else {
            // 无内容
            Text(
                text = "无可播放内容",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
    
    // 选集弹窗
    if (uiState.showEpisodeSelector) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideEpisodeSelector() },
            sheetState = episodeSelectorSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            EpisodeSelectorContent(
                seasons = uiState.seasons,
                episodesBySeasonId = uiState.episodesBySeasonId,
                currentEpisodeId = uiState.currentEpisode?.id,
                onEpisodeClick = { episode ->
                    viewModel.switchToEpisode(episode.id)
                    viewModel.hideEpisodeSelector()
                }
            )
        }
    }
    
    // 倍速选择弹窗
    if (uiState.showSpeedSelector) {
        SpeedSelectorDialog(
            currentSpeed = uiState.playbackSpeed,
            onSpeedSelected = { speed ->
                viewModel.setPlaybackSpeed(speed)
                viewModel.hideSpeedSelector()
            },
            onDismiss = { viewModel.hideSpeedSelector() }
        )
    }
}

/**
 * 播放内容区域
 * 
 * @param exoPlayer 共享的 ExoPlayer 实例
 * @param uiState 播放器 UI 状态
 * @param episode 当前页面对应的剧集
 * @param isCurrentPage 是否为当前显示的页面
 * @param showControls 是否显示控制栏
 * @param onToggleControls 切换控制栏显示
 * @param onTogglePlayPause 切换播放/暂停
 * @param onSeek 跳转到指定位置
 * @param onSeekStart 开始拖动进度条
 * @param onSeekEnd 结束拖动进度条（可选传入目标位置）
 * @param onBack 返回
 * @param onShowSpeedSelector 显示倍速选择
 * @param onShowEpisodeSelector 显示选集弹窗
 * @param onToggleFullscreen 切换全屏
 * @param onProgressChanged 播放进度变化
 * @param onPlaybackStateChanged 播放状态变化
 * @param onSeekComplete 跳转完成
 * @param onBufferingChanged 缓冲状态变化
 * @param onVideoEnded 视频播放完成
 * @param onRetry 重试加载
 * @param onLongPressStart 长按开始回调
 * @param onLongPressEnd 长按结束回调
 */
@Composable
private fun PlayerContent(
    exoPlayer: ExoPlayer,
    uiState: PlayerUiState,
    episode: com.wjp.jellyfinduanju.data.model.EpisodeLocalData,
    isCurrentPage: Boolean,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: (Long?) -> Unit,
    onBack: () -> Unit,
    onShowSpeedSelector: () -> Unit,
    onShowEpisodeSelector: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onProgressChanged: (Long, Long) -> Unit,
    onPlaybackStateChanged: (Boolean) -> Unit,
    onSeekComplete: () -> Unit,
    onBufferingChanged: (Boolean) -> Unit = {},
    onVideoEnded: () -> Unit = {},
    onRetry: () -> Unit = {},
    onLongPressStart: () -> Unit = {},
    onLongPressEnd: () -> Unit = {}
) {
    // 长按状态
    var isLongPressing by remember { mutableStateOf(false) }
    
    // 水平滑动进度调节状态
    var isDragging by remember { mutableStateOf(false) }
    var dragTargetPosition by remember { mutableStateOf(0L) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onLongPress = {
                        isLongPressing = true
                        onLongPressStart()
                    },
                    onPress = {
                        tryAwaitRelease()
                        if (isLongPressing) {
                            isLongPressing = false
                            onLongPressEnd()
                        }
                    }
                )
            }
            .pointerInput(uiState.duration, uiState.currentPosition) {
                // 水平滑动调节进度（参考抖音逻辑）
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragTargetPosition = uiState.currentPosition
                        onSeekStart()
                    },
                    onDragEnd = {
                        if (isDragging) {
                            // 保存目标位置，然后结束拖动状态
                            val targetPos = dragTargetPosition
                            isDragging = false
                            // 一次性完成：跳转 + 恢复播放
                            onSeekEnd(targetPos)
                        }
                    },
                    onDragCancel = {
                        if (isDragging) {
                            isDragging = false
                            // 取消时恢复播放（不跳转）
                            onSeekEnd(null)
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        if (uiState.duration > 0) {
                            // 每滑动 1px 调整 100ms
                            val seekAmount = (dragAmount * 100).toLong()
                            dragTargetPosition = (dragTargetPosition + seekAmount)
                                .coerceIn(0, uiState.duration)
                        }
                    }
                )
            }
    ) {
        // 只有当前页面才加载和播放视频
        if (isCurrentPage) {
            // 视频播放器（使用共享的 ExoPlayer 实例）
            if (uiState.currentVideoUri != null) {
                VideoPlayer(
                    exoPlayer = exoPlayer,
                    videoUri = uiState.currentVideoUri,
                    isPlaying = uiState.isPlaying,
                    playbackSpeed = uiState.actualSpeed,
                    seekToPosition = uiState.seekToPosition,
                    onProgressChanged = onProgressChanged,
                    onPlaybackStateChanged = onPlaybackStateChanged,
                    onSeekComplete = onSeekComplete,
                    onBufferingChanged = onBufferingChanged,
                    onVideoEnded = onVideoEnded,
                    modifier = Modifier.fillMaxSize()
                )
                
                // 缓冲指示器
                if (uiState.isBuffering) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            } else if (uiState.isVideoLoading) {
                // 视频加载中
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "正在加载视频...",
                            color = Color.White
                        )
                    }
                }
            } else if (uiState.videoError != null) {
                // 视频加载错误 + 重试按钮
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "视频加载失败",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.videoError,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        // 重试按钮
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable { onRetry() }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "重试",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "点击重试",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        } else {
            // 非当前页面：显示剧集信息占位
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "第${episode.episodeNumber}集",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = episode.name,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // 滑动进度指示器
        if (isDragging && uiState.duration > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = PlayerUiState.formatDuration(dragTargetPosition),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "/ ${PlayerUiState.formatDuration(uiState.duration)}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // 控制层
        if (showControls) {
            // 顶部渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            // 顶部栏
            PlayerTopBar(
                seriesName = uiState.series.name,
                episodeTitle = "第${episode.episodeNumber}集 ${episode.name}",
                playbackSpeed = uiState.playbackSpeed,
                isLongPressSpeed = uiState.isLongPressSpeed,
                isFullscreen = uiState.isFullscreen,
                onBack = onBack,
                onSpeedClick = onShowSpeedSelector,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp)  // 顶部留一点间距
                    .then(
                        if (uiState.isFullscreen) Modifier.navigationBarsPadding() 
                        else Modifier
                    )
            )
            
            // 中央播放/暂停按钮
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            // 底部渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
            
            // 底部控制栏
            PlayerBottomBar(
                currentPosition = uiState.currentPosition,
                duration = uiState.duration,
                currentPositionText = uiState.currentPositionText,
                durationText = uiState.durationText,
                episodeCount = uiState.allEpisodes.size,
                isFullscreen = uiState.isFullscreen,
                onSeek = onSeek,
                onSeekStart = onSeekStart,
                onSeekEnd = onSeekEnd,
                onShowEpisodeSelector = onShowEpisodeSelector,
                onToggleFullscreen = onToggleFullscreen,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .then(
                        if (uiState.isFullscreen) Modifier.navigationBarsPadding() 
                        else Modifier
                    )
            )
        }
    }
}

/**
 * 顶部栏
 * 
 * 包含：返回按钮、剧名、集数标题、倍速按钮
 * 横屏时增加水平内边距
 */
@Composable
private fun PlayerTopBar(
    seriesName: String,
    episodeTitle: String,
    playbackSpeed: Float,
    isLongPressSpeed: Boolean,
    isFullscreen: Boolean,
    onBack: () -> Unit,
    onSpeedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalPadding = if (isFullscreen) 24.dp else 8.dp
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 返回按钮
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White
            )
        }
        
        // 标题区域
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 剧名
            Text(
                text = seriesName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            // 集数
            Text(
                text = episodeTitle,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 长按倍速提示
        if (isLongPressSpeed) {
            Text(
                text = "3x 快进中",
                color = Color(0xFFFF6B00),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        // 倍速按钮
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .clickable(onClick = onSpeedClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (playbackSpeed == 1.0f) "倍速" else "${playbackSpeed}x",
                color = Color.White,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * 底部控制栏
 * 
 * 横屏时增加水平内边距
 * 进度条拖动逻辑（参考抖音）：
 * 1. 开始拖动：暂停播放
 * 2. 拖动中：进度条实时跟随，显示预览时间
 * 3. 松开：跳转到目标位置，自动恢复播放
 */
@Composable
private fun PlayerBottomBar(
    currentPosition: Long,
    duration: Long,
    currentPositionText: String,
    durationText: String,
    episodeCount: Int,
    isFullscreen: Boolean,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: (Long?) -> Unit,
    onShowEpisodeSelector: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalPadding = if (isFullscreen) 32.dp else 16.dp
    
    // 进度条拖动状态
    var isSliderDragging by remember { mutableStateOf(false) }
    var sliderDragPosition by remember { mutableStateOf(0f) }
    
    // 显示的进度：拖动时显示拖动位置，否则显示实际位置
    val displayProgress = if (isSliderDragging) {
        sliderDragPosition
    } else {
        if (duration > 0) currentPosition.toFloat() / duration else 0f
    }
    
    // 显示的时间文本：拖动时显示预览时间
    val displayPositionText = if (isSliderDragging && duration > 0) {
        PlayerUiState.formatDuration((sliderDragPosition * duration).toLong())
    } else {
        currentPositionText
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 8.dp)
    ) {
        // 进度条
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayPositionText,
                color = if (isSliderDragging) Color(0xFFFF6B00) else Color.White,
                fontSize = if (isSliderDragging) 14.sp else 12.sp,
                fontWeight = if (isSliderDragging) FontWeight.Bold else FontWeight.Normal
            )
            
            Slider(
                value = displayProgress,
                onValueChange = { progress ->
                    // 拖动开始时通知暂停
                    if (!isSliderDragging) {
                        isSliderDragging = true
                        onSeekStart()
                    }
                    sliderDragPosition = progress
                },
                onValueChangeFinished = {
                    // 松开时：一次性完成跳转 + 恢复播放
                    if (isSliderDragging && duration > 0) {
                        val targetPos = (sliderDragPosition * duration).toLong()
                        onSeekEnd(targetPos)
                    }
                    isSliderDragging = false
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = if (isSliderDragging) Color(0xFFFF6B00) else Color.White,
                    activeTrackColor = Color(0xFFFF6B00),
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            
            Text(
                text = durationText,
                color = Color.White,
                fontSize = 12.sp
            )
        }
        
        // 选集栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 选集按钮
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onShowEpisodeSelector)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选集·共${episodeCount}集",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "展开",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // 全屏切换按钮
            IconButton(onClick = onToggleFullscreen) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullscreen) "退出全屏" else "全屏",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * 选集弹窗内容
 * 
 * 垂直滚动显示所有季和剧集
 * 
 * @param seasons 季列表
 * @param episodesBySeasonId 按季 ID 分组的剧集列表
 * @param currentEpisodeId 当前播放的剧集 ID
 * @param onEpisodeClick 点击剧集回调
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpisodeSelectorContent(
    seasons: List<com.wjp.jellyfinduanju.data.model.SeasonLocalData>,
    episodesBySeasonId: Map<String, List<com.wjp.jellyfinduanju.data.model.EpisodeLocalData>>,
    currentEpisodeId: String?,
    onEpisodeClick: (com.wjp.jellyfinduanju.data.model.EpisodeLocalData) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)  // 固定高度，防止占满全屏
            .padding(horizontal = 16.dp)
    ) {
        // 标题
        item {
            Text(
                text = "选集",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        
        // 按季显示剧集
        seasons.forEach { season ->
            val episodes = episodesBySeasonId[season.id] ?: emptyList()
            
            // 季标题
            item {
                Text(
                    text = season.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // 剧集网格
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    episodes.forEach { episode ->
                        EpisodeChip(
                            episode = episode,
                            isCurrentPlaying = episode.id == currentEpisodeId,
                            onClick = { onEpisodeClick(episode) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // 底部间距
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 剧集选择芯片
 * 
 * 显示剧集编号、缓存状态、播放状态
 */
@Composable
private fun EpisodeChip(
    episode: com.wjp.jellyfinduanju.data.model.EpisodeLocalData,
    isCurrentPlaying: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isCurrentPlaying -> MaterialTheme.colorScheme.primary
        episode.played -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    
    val textColor = when {
        isCurrentPlaying -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    val borderColor = when {
        isCurrentPlaying -> MaterialTheme.colorScheme.primary
        episode.isCached -> Color(0xFF4CAF50)  // 绿色边框表示已缓存
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (episode.isCached) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 集数
            Text(
                text = "${episode.episodeNumber}",
                color = textColor,
                fontSize = 16.sp,
                fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.Medium
            )
            
            // 进度指示（如果有）
            if (episode.playbackPositionTicks > 0 && !episode.played) {
                Text(
                    text = "${episode.getProgressPercent()}%",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            } else if (episode.played) {
                Text(
                    text = "已看",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
        
        // 正在播放指示
        if (isCurrentPlaying) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(8.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

/**
 * 倍速选择对话框
 */
@Composable
private fun SpeedSelectorDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
    
    // 简单的弹窗实现
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}  // 阻止点击穿透
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "播放倍速",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            speeds.forEach { speed ->
                val isSelected = speed == currentSpeed
                Text(
                    text = if (speed == 1.0f) "正常" else "${speed}x",
                    color = if (isSelected) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSpeedSelected(speed) }
                        .padding(vertical = 12.dp, horizontal = 24.dp)
                )
            }
        }
    }
}

