@file:OptIn(ExperimentalMaterial3Api::class)

package com.wjp.jellyfinduanju.ui.screens.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.wjp.jellyfinduanju.ui.components.media.rememberExoPlayer
import com.wjp.jellyfinduanju.ui.components.player.DragProgressIndicator
import com.wjp.jellyfinduanju.ui.components.player.GradientOverlay
import com.wjp.jellyfinduanju.ui.components.player.GradientPosition
import com.wjp.jellyfinduanju.ui.components.player.PlayerGestureCallbacks
import com.wjp.jellyfinduanju.ui.components.player.PlayerGestureState
import com.wjp.jellyfinduanju.ui.components.player.SpeedIndicator
import com.wjp.jellyfinduanju.ui.components.player.VideoContentArea
import com.wjp.jellyfinduanju.ui.components.player.VideoPlayerCallbacks
import com.wjp.jellyfinduanju.ui.components.player.VideoProgressBar
import com.wjp.jellyfinduanju.ui.components.player.playerGestures

/**
 * PlayerScreen - 视频播放页面
 *
 * @param viewModel 播放页 ViewModel，管理所有状态和业务逻辑
 * @param onBack 返回回调，退出播放页面
 */
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {

    // 收集播放器 UI 状态
    val uiState by viewModel.uiState.collectAsState()

    // 创建并记住 ExoPlayer 实例（整个页面共享，切集数时复用）
    val exoPlayer = rememberExoPlayer()
    // 控制栏显示状态（横屏模式的控制栏显隐）
    val showControls = remember { mutableStateOf(true) }
    
    // 副作用：系统UI设置、生命周期管理、返回键处理、控制栏自动隐藏
    SetupSystemUI(viewModel)
    SetupPlayerLifecycle(viewModel)
    HandlePlayerBackPress(viewModel, onBack)
    AutoHideControls(showControls, viewModel)

    // UI 布局：全屏黑色背景容器
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 根据加载状态和数据状态显示不同内容
        when {
            // 加载中：显示加载指示器
            uiState.isLoading -> LoadingContent()
            // 有数据：显示 VerticalPager（抖音式上下滑动切换集数）
            uiState.allEpisodes.isNotEmpty() -> PlayerPagerContent(
                exoPlayer = exoPlayer,  // 共享的播放器实例
                uiState = uiState,  // 播放器 UI 状态
                showControls = showControls,  // 控制栏显示状态（横屏模式使用）
                viewModel = viewModel,  // 播放器 ViewModel
                onBack = onBack  // 返回回调
            )
        }
    }

    // 弹窗层：选集弹窗、倍速弹窗（不影响主内容，独立渲染）
    PlayerDialogs(
        uiState = uiState,
        viewModel = viewModel
    )
}

// ==================== UI 组件（拆分后的独立方法）====================

/**
 * PlayerDialogs - 播放器弹窗集合
 * 
 * 统一管理播放器相关的所有弹窗：
 * - 倍速选择器弹窗
 * - 选集弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerDialogs(
    uiState: PlayerUiState,
    viewModel: PlayerViewModel
) {
    // 倍速选择器弹窗
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

    // 选集弹窗
    if (uiState.showEpisodeSelector) {
        // 选集弹窗状态（跳过半展开态）
        val episodeSelectorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
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
                    viewModel.forceSaveProgress(mustSave = true)
                    viewModel.switchToEpisode(episode.id)
                    viewModel.hideEpisodeSelector()
                }
            )
        }
    }
}

/**
 * LoadingContent - 加载状态
 */
@Composable
private fun BoxScope.LoadingContent() {
    CircularProgressIndicator(
        modifier = Modifier.align(Alignment.Center),
        color = Color.White
    )
}

/**
 * PlayerPagerContent - 播放器主内容（VerticalPager）
 */
@SuppressLint("SourceLockedOrientationActivity")
@Composable
private fun PlayerPagerContent(
    exoPlayer: ExoPlayer,
    uiState: PlayerUiState,
    showControls: MutableState<Boolean>,
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val activity = LocalContext.current as? Activity
    
    // VerticalPager 状态（抖音式上下滑动切换集数）
    val pagerState = rememberPagerState(
        initialPage = uiState.currentEpisodeIndex,
        pageCount = { uiState.allEpisodes.size }
    )
    
    // 同步 Pager 与 ViewModel 状态
    var isProgrammaticScroll by remember { mutableStateOf(false) }
    
    // Pager → ViewModel：监听滑动变化
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (!isProgrammaticScroll && page != uiState.currentEpisodeIndex) {
                viewModel.forceSaveProgress()
                viewModel.switchToEpisode(page)
            }
        }
    }
    
    // ViewModel → Pager：监听集数切换
    LaunchedEffect(uiState.currentEpisodeIndex) {
        if (pagerState.currentPage != uiState.currentEpisodeIndex) {
            isProgrammaticScroll = true
            pagerState.animateScrollToPage(uiState.currentEpisodeIndex)
            isProgrammaticScroll = false
        }
    }
    
    // 创建全屏切换回调（内部逻辑）
    val toggleFullscreen: () -> Unit = {
        if (uiState.isFullscreen) {
            // 横屏 → 竖屏
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            viewModel.setFullscreen(false)
        } else {
            // 竖屏 → 横屏
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            viewModel.setFullscreen(true)
        }
    }
    
    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 0,
        flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            snapPositionalThreshold = com.wjp.jellyfinduanju.data.settings.AppSettings.swipeThreshold
        )
    ) { page ->
        val isCurrentPage = page == pagerState.currentPage

        PlayerContent(
            exoPlayer = exoPlayer,
            uiState = uiState,
            episode = uiState.allEpisodes[page],
            isCurrentPage = isCurrentPage,
            showControls = showControls.value && isCurrentPage,
            showPlayButton = false,
            onToggleControls = {
                if (uiState.isFullscreen) {
                    showControls.value = !showControls.value
                } else {
                    viewModel.togglePlayPause()
                }
            },
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSeek = { viewModel.seekTo(it) },
            onSeekStart = { viewModel.onSeekStart() },
            onSeekEnd = { targetPos -> viewModel.onSeekEnd(targetPos) },
            onBack = {
                if (uiState.isFullscreen) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    viewModel.setFullscreen(false)
                } else {
                    viewModel.forceSaveProgress(mustSave = true)
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
                val state = viewModel.uiState.value
                if (isCurrentPage) {
                    if (state.isChangingEpisode) {
                        if (isPlaying) {
                            viewModel.onVideoActuallyStarted()
                        }
                    } else if (!state.isSeeking) {
                        viewModel.setPlaying(isPlaying)
                    }
                }
            },
            onSeekComplete = { viewModel.clearSeekPosition() },
            onBufferingChanged = { buffering ->
                if (isCurrentPage) {
                    viewModel.setBuffering(buffering)
                }
            },
            onVideoEnded = {
                if (page == pagerState.currentPage) {
                    viewModel.onVideoEnded()
                }
            },
            onRetry = { viewModel.retryLoadVideo() },
            onLongPressStart = { viewModel.startLongPressSpeed() },
            onLongPressEnd = { viewModel.endLongPressSpeed() }
        )
    }
}

/**
 * ============================================================================
 * PlayerContent - 播放内容区域
 * ============================================================================
 *
 * VerticalPager 的每一页内容，包含：
 * - VideoPlayer（视频播放器）
 * - 控制层（顶部栏、底部栏、中央播放按钮）
 * - 手势处理（点击、长按、水平滑动）
 *
 * 【手势处理】
 * - detectTapGestures: 处理点击和长按
 * - detectHorizontalDragGestures: 处理水平滑动调节进度
 *
 * @param exoPlayer 共享的 ExoPlayer 实例
 * @param uiState 播放器 UI 状态
 * @param episode 当前页面对应的剧集数据
 * @param isCurrentPage 是否为当前显示的页面（用于决定是否播放）
 * @param showControls 是否显示控制栏（横屏模式使用）
 * @param showPlayButton 是否显示播放按钮（竖屏模式只控制播放按钮）
 * @param onToggleControls 切换控制栏/播放按钮显示
 * @param onTogglePlayPause 切换播放/暂停
 * @param onSeek 跳转到指定位置
 * @param onSeekStart 开始拖动（暂停播放）
 * @param onSeekEnd 结束拖动（恢复播放，可选跳转）
 * @param onBack 返回
 * @param onShowSpeedSelector 显示倍速选择弹窗
 * @param onShowEpisodeSelector 显示选集弹窗
 * @param onToggleFullscreen 切换全屏
 * @param onProgressChanged 播放进度变化回调
 * @param onPlaybackStateChanged 播放状态变化回调
 * @param onSeekComplete seek 完成回调
 * @param onBufferingChanged 缓冲状态变化回调
 * @param onVideoEnded 视频播放完成回调
 * @param onRetry 重试加载回调
 * @param onLongPressStart 长按开始回调（开始 3x 倍速）
 * @param onLongPressEnd 长按结束回调（恢复正常倍速）
 */
@Composable
private fun PlayerContent(
    exoPlayer: ExoPlayer,
    uiState: PlayerUiState,
    episode: com.wjp.jellyfinduanju.data.model.EpisodeLocalData,
    isCurrentPage: Boolean,
    showControls: Boolean,
    showPlayButton: Boolean,
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
    // ==================== 本地状态 ====================

    /** 手势状态（拖动状态和目标位置） */
    var gestureState by remember { mutableStateOf(PlayerGestureState(false, 0L)) }

    // ==================== 手势处理 + 布局（竖屏/横屏不同布局） ====================

    if (uiState.isFullscreen) {
        // ==================== 横屏模式：悬浮控制栏（沉浸式） ====================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .playerGestures(
                    currentPosition = uiState.currentPosition,
                    duration = uiState.duration,
                    callbacks = PlayerGestureCallbacks(
                        onTap = onToggleControls,
                        onLongPressStart = onLongPressStart,
                        onLongPressEnd = onLongPressEnd,
                        onSeekStart = onSeekStart,
                        onSeekEnd = onSeekEnd
                    ),
                    onGestureStateChanged = { gestureState = it }
                )
        ) {
            // ==================== 视频内容 ====================
            
            VideoContentArea(
                isCurrentPage = isCurrentPage,
                episode = episode,
                uiState = uiState,
                exoPlayer = exoPlayer,
                callbacks = VideoPlayerCallbacks(
                    onProgressChanged = onProgressChanged,
                    onPlaybackStateChanged = onPlaybackStateChanged,
                    onSeekComplete = onSeekComplete,
                    onBufferingChanged = onBufferingChanged,
                    onVideoEnded = onVideoEnded,
                    onRetry = onRetry
                )
            )

            // ==================== 滑动进度指示器 ====================

            DragProgressIndicator(
                targetPosition = gestureState.dragTargetPosition,
                duration = uiState.duration,
                visible = gestureState.isDragging,
                formatDuration = { PlayerUiState.formatDuration(it) },
                modifier = Modifier.align(Alignment.Center)
            )

            // ==================== 控制层 ====================

            // 顶部和底部控制栏的显示逻辑：
            // - 竖屏模式：始终显示
            // - 横屏模式：受 showControls 控制
            val showBars = !uiState.isFullscreen || showControls

            if (showBars) {
                // 顶部渐变遮罩
                GradientOverlay(
                    position = GradientPosition.TOP,
                    height = 100.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                )

                // 顶部控制栏
                PlayerTopBar(
                    seriesName = uiState.series.name,
                    episodeTitle = "第${episode.episodeNumber}集",
                    playbackSpeed = uiState.playbackSpeed,
                    isFullscreen = uiState.isFullscreen,
                    onBack = onBack,
                    onSpeedClick = onShowSpeedSelector,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                )

                // 底部渐变遮罩
                GradientOverlay(
                    position = GradientPosition.BOTTOM,
                    height = 120.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
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
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // 中央播放/暂停按钮：横屏模式，受 showControls 控制
            if (uiState.isFullscreen && showControls) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "暂停" else "播放",
                    tint = Color.White.copy(alpha = 0.9f),  // 稍微透明
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)  // 圆润的大图标
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,  // 去掉点击波纹效果
                            onClick = onTogglePlayPause
                        )
                )
            }

            // ==================== 长按倍速提示（始终显示，不受控制栏影响） ====================
            SpeedIndicator(
                speed = uiState.longPressSpeedValue,
                visible = uiState.isLongPressSpeed,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 16.dp)
            )
        }
    } else {
        // ==================== 竖屏模式：固定底部控制区（抖音风格） ====================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .playerGestures(
                    currentPosition = uiState.currentPosition,
                    duration = uiState.duration,
                    callbacks = PlayerGestureCallbacks(
                        onTap = onToggleControls,
                        onLongPressStart = onLongPressStart,
                        onLongPressEnd = onLongPressEnd,
                        onSeekStart = onSeekStart,
                        onSeekEnd = onSeekEnd
                    ),
                    onGestureStateChanged = { gestureState = it }
                )
        ) {
            // ==================== 视频区域（带顶部控制栏） ====================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)  // 占据剩余空间
            ) {
                // 视频内容
                VideoContentArea(
                    isCurrentPage = isCurrentPage,
                    episode = episode,
                    uiState = uiState,
                    exoPlayer = exoPlayer,
                    callbacks = VideoPlayerCallbacks(
                        onProgressChanged = onProgressChanged,
                        onPlaybackStateChanged = onPlaybackStateChanged,
                        onSeekComplete = onSeekComplete,
                        onBufferingChanged = onBufferingChanged,
                        onVideoEnded = onVideoEnded,
                        onRetry = onRetry
                    )
                )

                // 水平滑动进度指示器
                DragProgressIndicator(
                    targetPosition = gestureState.dragTargetPosition,
                    duration = uiState.duration,
                    visible = gestureState.isDragging,
                    formatDuration = { PlayerUiState.formatDuration(it) },
                    modifier = Modifier.align(Alignment.Center)
                )

                // 顶部渐变遮罩
                GradientOverlay(
                    position = GradientPosition.TOP,
                    height = 100.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                )

                // 顶部控制栏
                PlayerTopBar(
                    seriesName = uiState.series.name,
                    episodeTitle = "第${episode.episodeNumber}集",
                    playbackSpeed = uiState.playbackSpeed,
                    isFullscreen = uiState.isFullscreen,
                    onBack = onBack,
                    onSpeedClick = onShowSpeedSelector,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                )

                // 长按倍速提示
                SpeedIndicator(
                    speed = uiState.longPressSpeedValue,
                    visible = uiState.isLongPressSpeed,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 60.dp, end = 16.dp)
                )

                // 中央播放按钮：竖屏模式，仅在暂停时显示
                if (!uiState.isPlaying && isCurrentPage) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "播放",
                        tint = Color.White.copy(alpha = 0.9f),  // 稍微透明
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp)  // 圆润的大图标
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,  // 去掉点击波纹效果
                                onClick = onTogglePlayPause
                            )
                    )
                }
            }

            // ==================== 底部固定控制区（不透明背景） ====================
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
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * ============================================================================
 * PlayerTopBar - 顶部控制栏
 * ============================================================================
 *
 * 包含：返回按钮、剧名、集数标题、倍速按钮
 * 横屏时增加水平内边距以避开刘海/圆角
 */
@Composable
private fun PlayerTopBar(
    seriesName: String,
    episodeTitle: String,
    playbackSpeed: Float,
    isFullscreen: Boolean,
    onBack: () -> Unit,
    onSpeedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.6f),  // 顶部深色
                        Color.Transparent  // 底部透明
                    )
                )
            )
            .padding(horizontal = 8.dp)
            .padding(top = 28.dp, bottom = 16.dp),  // 再往下移8dp
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

        // 集数标题（只显示集数，不显示剧名）
        Text(
            text = episodeTitle,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 倍速按钮（纯文字，无背景）
        Text(
            text = if (playbackSpeed == 1.0f) "倍速" else "${playbackSpeed}x",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .clickable(onClick = onSpeedClick)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

/**
 * ============================================================================
 * PlayerBottomBar - 底部控制栏
 * ============================================================================
 *
 * 包含：进度条（Slider）、当前时间、总时长、选集按钮、全屏按钮
 *
 * 【进度条拖动逻辑（参考抖音）】
 * 1. 开始拖动：暂停播放，显示预览时间
 * 2. 拖动中：进度条实时跟随，时间文本高亮显示
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
    val horizontalPadding = 16.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isFullscreen) {
                    // 横屏：透明背景（会有渐变遮罩）
                    Color.Transparent
                } else {
                    // 竖屏：纯黑色背景（固定控制区）
                    Color.Black
                }
            )
            .padding(horizontal = horizontalPadding)
            .padding(
                top = if (isFullscreen) 80.dp else 1.dp,  // 横屏需要更大的 top padding，竖屏只留1dp缝隙
                bottom = if (isFullscreen) 0.dp else 2.dp  // 竖屏底部 padding，非常紧凑
            )
    ) {
        // ==================== 进度条行 ====================
        VideoProgressBar(
            currentPosition = currentPosition,
            duration = duration,
            currentPositionText = currentPositionText,
            durationText = durationText,
            onSeekStart = onSeekStart,
            onSeekEnd = onSeekEnd
        )

        // ==================== 选集栏 ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)  // 固定高度，保持紧凑
                .padding(top = 2.dp),  // 与进度条之间的间距
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 选集按钮
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onShowEpisodeSelector)
                    .padding(horizontal = 8.dp),  // 只保留水平 padding，去掉垂直 padding
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选集·共${episodeCount}集",
                    color = Color.White,
                    fontSize = 14.sp  // 增大字体，更清晰
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "展开",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)  // 增大图标
                )
            }

            // 全屏切换按钮（紧凑版本）
            Icon(
                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (isFullscreen) "退出全屏" else "全屏",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)  // 增大按钮，更容易点击
                    .clickable(onClick = onToggleFullscreen)
            )
        }
    }
}

/**
 * ============================================================================
 * EpisodeSelectorContent - 选集弹窗内容
 * ============================================================================
 *
 * 在 ModalBottomSheet 中显示，垂直滚动列表
 * 按季分组显示剧集，使用 FlowRow 实现网格布局
 *
 * @param seasons 季列表
 * @param episodesBySeasonId 按季 ID 分组的剧集
 * @param currentEpisodeId 当前播放的剧集 ID（高亮显示）
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

            // 剧集网格（FlowRow 自动换行）
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    episodes.forEachIndexed { index, episode ->
                        key(episode.id) {
                            EpisodeChip(
                                episode = episode,
                                isCurrentPlaying = episode.id == currentEpisodeId,
                                onClick = { onEpisodeClick(episode) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 底部间距（避免被系统导航栏遮挡）
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * ============================================================================
 * EpisodeChip - 剧集选择芯片
 * ============================================================================
 *
 * 显示单个剧集的选择按钮，包含：
 * - 集数编号
 * - 播放进度（如果有）
 * - 已看状态
 * - 缓存状态（绿色边框）
 * - 正在播放状态（高亮 + 小圆点）
 *
 * @param episode 剧集数据
 * @param isCurrentPlaying 是否为当前播放的集
 * @param onClick 点击回调
 */
@Composable
private fun EpisodeChip(
    episode: com.wjp.jellyfinduanju.data.model.EpisodeLocalData,
    isCurrentPlaying: Boolean,
    onClick: () -> Unit
) {
    // 背景色
    val backgroundColor = when {
        isCurrentPlaying -> MaterialTheme.colorScheme.primary
        episode.played -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    // 文字颜色
    val textColor = when {
        isCurrentPlaying -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    // 边框颜色（已缓存显示绿色）
    val borderColor = when {
        isCurrentPlaying -> MaterialTheme.colorScheme.primary
        episode.isCached -> Color(0xFF4CAF50)  // 绿色表示已缓存
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
            // 集数编号
            Text(
                text = "${episode.episodeNumber}",
                color = textColor,
                fontSize = 16.sp,
                fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.Medium
            )

            // 进度指示（有进度但未看完）
            if (episode.playbackPositionTicks > 0 && !episode.played) {
                Text(
                    text = "${episode.getProgressPercent()}%",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            } else if (episode.played) {
                // 已看标记
                Text(
                    text = "已看",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }

        // 正在播放指示（右上角小圆点）
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
 * ============================================================================
 * SpeedSelectorDialog - 倍速选择对话框
 * ============================================================================
 *
 * 简单的弹窗实现，显示倍速选项列表
 * 点击选项后回调并关闭弹窗
 *
 * @param currentSpeed 当前倍速
 * @param onSpeedSelected 选择倍速回调
 * @param onDismiss 关闭弹窗回调
 */
@Composable
private fun SpeedSelectorDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    // 可选倍速列表
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)

    // 半透明遮罩 + 居中弹窗
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),  // 点击遮罩关闭
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
                    onClick = {}  // 阻止点击穿透到遮罩
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Text(
                text = "播放倍速",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 倍速选项列表
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

// ==================== 副作用函数（参考 ListScreen 风格）====================

/**
 * SetupSystemUI - 设置系统 UI（组合函数）
 * 
 * 封装所有系统 UI 相关的副作用：状态栏、系统栏、屏幕常亮
 * 
 * @param viewModel 播放器 ViewModel
 */
@Composable
private fun SetupSystemUI(viewModel: PlayerViewModel) {
    // 获取 Activity 实例（只获取一次，传递给子函数）
    val activity = LocalContext.current as? Activity
    
    SetupStatusBarStyle(activity)
    SetupSystemBarsVisibility(activity, viewModel)
    SetupKeepScreenOn(activity, viewModel)
    CleanupSystemUI(activity)
}

/**
 * SetupStatusBarStyle - 设置状态栏样式
 * 
 * 状态栏背景透明，文字为白色
 * 
 * @param activity Activity 实例
 */
@Composable
private fun SetupStatusBarStyle(activity: Activity?) {
    // 每次重组时设置状态栏样式
    SideEffect {
        activity?.window?.let { window ->
            // 状态栏背景透明
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            // 获取系统栏控制器
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            // 状态栏文字为白色（适配暗色背景）
            controller.isAppearanceLightStatusBars = false
        }
    }
}

/**
 * SetupSystemBarsVisibility - 控制系统栏显示/隐藏
 * 
 * 根据全屏状态切换系统栏显隐逻辑
 * 
 * @param activity Activity 实例
 * @param viewModel 播放器 ViewModel
 */
@Composable
private fun SetupSystemBarsVisibility(activity: Activity?, viewModel: PlayerViewModel) {
    // 收集全屏状态
    val uiState by viewModel.uiState.collectAsState()
    
    // 监听全屏状态变化
    LaunchedEffect(uiState.isFullscreen) {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            
            if (uiState.isFullscreen) {
                // 横屏模式：隐藏所有系统栏（状态栏+导航栏）
                WindowCompat.setDecorFitsSystemWindows(window, false)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                // 从边缘滑动可临时唤出系统栏
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                // 竖屏模式：显示透明状态栏，隐藏导航栏
                WindowCompat.setDecorFitsSystemWindows(window, false)
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.hide(WindowInsetsCompat.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}

/**
 * SetupKeepScreenOn - 控制屏幕常亮
 * 
 * 播放时保持屏幕常亮，暂停时允许自动熄灭
 * 
 * @param activity Activity 实例
 * @param viewModel 播放器 ViewModel
 */
@Composable
private fun SetupKeepScreenOn(activity: Activity?, viewModel: PlayerViewModel) {
    // 收集播放状态
    val uiState by viewModel.uiState.collectAsState()
    
    // 监听播放状态变化
    LaunchedEffect(uiState.isPlaying) {
        activity?.window?.let { window ->
            if (uiState.isPlaying) {
                // 播放中：保持屏幕常亮
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                // 暂停：允许屏幕自动熄灭
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

/**
 * CleanupSystemUI - 清理系统 UI 设置
 * 
 * 组件销毁时恢复系统 UI 默认状态
 * 
 * @param activity Activity 实例
 */
@Composable
private fun CleanupSystemUI(activity: Activity?) {
    // 组件销毁时执行清理
    DisposableEffect(activity) {
        onDispose {
            activity?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                // 显示所有系统栏
                controller.show(WindowInsetsCompat.Type.systemBars())
                // 恢复默认布局行为
                WindowCompat.setDecorFitsSystemWindows(window, true)
                // 恢复状态栏透明
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                // 状态栏文字恢复为黑色
                controller.isAppearanceLightStatusBars = true
                // 取消屏幕常亮
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

/**
 * SetupPlayerLifecycle - 设置播放器生命周期
 * 
 * 处理播放器的生命周期事件：退出时保存进度、恢复屏幕方向
 * 
 * @param viewModel 播放器 ViewModel
 */
@SuppressLint("SourceLockedOrientationActivity")
@Composable
private fun SetupPlayerLifecycle(
    viewModel: PlayerViewModel
) {
    // 获取 Activity 实例（用于控制屏幕方向）
    val activity = LocalContext.current as? Activity
    
    // 组件销毁时强制保存播放进度
    DisposableEffect(activity) {
        onDispose {
            // 强制立即保存进度，不延迟
            viewModel.forceSaveProgress(mustSave = true)
        }
    }
    
    // 组件销毁时恢复屏幕方向为系统默认
    DisposableEffect(Unit) {
        onDispose {
            // 取消方向锁定，恢复为系统自动旋转
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

/**
 * HandlePlayerBackPress - 处理返回键
 * 
 * 优先级：横屏→退出全屏、选集弹窗→关闭弹窗、其他→保存并退出
 * 
 * @param viewModel 播放器 ViewModel
 * @param onBack 返回回调
 */
@SuppressLint("SourceLockedOrientationActivity")
@Composable
private fun HandlePlayerBackPress(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    // 获取 Activity 实例（用于控制屏幕方向）
    val activity = LocalContext.current as? Activity
    // 收集 UI 状态（全屏状态、弹窗状态）
    val uiState by viewModel.uiState.collectAsState()
    
    // 拦截返回键，根据当前状态执行不同操作
    BackHandler {
        when {
            // 情况1：横屏模式下返回 → 退出全屏（切回竖屏）
            uiState.isFullscreen -> {
                // 设置屏幕方向为竖屏
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                // 更新 ViewModel 状态
                viewModel.setFullscreen(false)
            }
            // 情况2：选集弹窗打开 → 关闭弹窗
            uiState.showEpisodeSelector -> {
                viewModel.hideEpisodeSelector()
            }
            // 情况3：其他情况 → 保存进度并退出播放页面
            else -> {
                // 强制保存当前播放进度
                viewModel.forceSaveProgress(mustSave = true)
                // 执行返回操作
                onBack()
            }
        }
    }
}

/**
 * AutoHideControls - 自动隐藏控制栏
 * 
 * 横屏模式下，3 秒无操作自动隐藏控制栏
 * 
 * @param showControls 控制栏显示状态
 * @param viewModel 播放器 ViewModel
 */
@Composable
private fun AutoHideControls(
    showControls: MutableState<Boolean>,
    viewModel: PlayerViewModel
) {
    // 收集 UI 状态（全屏状态）
    val uiState by viewModel.uiState.collectAsState()
    
    // 监听控制栏显示状态和全屏状态的变化
    LaunchedEffect(showControls.value, uiState.isFullscreen) {
        // 仅在横屏模式 且 控制栏显示时执行自动隐藏逻辑
        if (uiState.isFullscreen && showControls.value) {
            // 延迟 3 秒
            delay(3000)
            // 自动隐藏控制栏
            showControls.value = false
        }
    }
}

