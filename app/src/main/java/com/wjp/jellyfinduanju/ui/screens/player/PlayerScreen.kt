@file:OptIn(ExperimentalMaterial3Api::class)

package com.wjp.jellyfinduanju.ui.screens.player

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.SideEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.wjp.jellyfinduanju.ui.components.VideoPlayer
import com.wjp.jellyfinduanju.ui.components.rememberExoPlayer

/**
 * ============================================================================
 * PlayerScreen - 视频播放页面
 * ============================================================================
 * 
 * 【核心功能】
 * 1. 全屏视频播放（支持横竖屏切换）
 * 2. 上下滑动切换集数（抖音风格 VerticalPager）
 * 3. 透明顶部栏（返回、剧名、集数、倍速按钮）
 * 4. 底部控制栏（进度条、选集按钮、全屏按钮）
 * 5. 长按 3x 倍速播放
 * 6. 水平滑动调节进度
 * 7. 3 秒无操作自动隐藏控制栏
 * 
 * 【组件结构】
 * PlayerScreen
 *   ├── VerticalPager（上下滑动切换集数）
 *   │     └── PlayerContent（每页的播放内容）
 *   │           ├── VideoPlayer（ExoPlayer 封装）
 *   │           ├── PlayerTopBar（顶部控制栏）
 *   │           └── PlayerBottomBar（底部控制栏）
 *   ├── EpisodeSelectorContent（选集弹窗）
 *   └── SpeedSelectorDialog（倍速选择弹窗）
 * 
 * 【手势操作】
 * - 单击：显示/隐藏控制栏
 * - 长按：3x 倍速播放
 * - 上下滑动：切换集数
 * - 左右滑动：调节播放进度
 * - 拖动进度条：精确跳转
 * 
 * @param viewModel 播放页 ViewModel，管理所有状态和业务逻辑
 * @param onBack 返回回调，退出播放页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    // ============================================================================
    // 状态收集与初始化
    // ============================================================================
    
    /** 收集 ViewModel 的 UI 状态 */
    val uiState by viewModel.uiState.collectAsState()
    
    /** 获取 Activity 用于控制屏幕方向和系统 UI */
    val context = LocalContext.current
    val activity = context as? Activity

    /**
     * 创建单一的 ExoPlayer 实例
     * 
     * 整个播放页面共享一个 ExoPlayer，切换集数时只更换 MediaItem
     * 这样可以避免频繁创建/销毁播放器实例
     */
    val exoPlayer = rememberExoPlayer()
    
    /** 选集弹窗状态（Material3 BottomSheet） */
    val episodeSelectorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    /** 控制栏显示状态（横屏模式使用） */
    var showControls by remember { mutableStateOf(true) }
    
    /** 播放按钮显示状态（横屏模式使用，竖屏根据播放状态自动显示） */
    var showPlayButton by remember { mutableStateOf(false) }  // 横屏模式初始隐藏
    
    // ============================================================================
    // 副作用：自动隐藏控制栏（仅横屏模式）
    // ============================================================================
    
    /**
     * 自动隐藏控制栏（仅横屏模式）
     * 
     * 竖屏模式：播放按钮根据播放状态显示，不需要自动隐藏
     * 横屏模式：3 秒无操作隐藏所有控制栏（顶部、底部、播放按钮）
     */
    LaunchedEffect(showControls, uiState.isFullscreen) {
        if (uiState.isFullscreen && showControls) {
            // 横屏模式：3 秒后隐藏控制栏
            delay(3000)
            showControls = false
        }
    }
    
    // ============================================================================
    // VerticalPager 状态
    // ============================================================================
    
    /**
     * VerticalPager 状态
     * 
     * initialPage: 初始页面索引（从 ViewModel 获取）
     * pageCount: 总页数（剧集总数）
     */
    val pagerState = rememberPagerState(
        initialPage = uiState.currentEpisodeIndex,
        pageCount = { uiState.allEpisodes.size }
    )
    
    // ============================================================================
    // 副作用：系统 UI 控制
    // ============================================================================
    
    /**
     * 设置状态栏为透明，文字为白色
     * 
     * 使用 SideEffect 确保每次重组时都应用颜色
     */
    SideEffect {
        activity?.window?.let { window ->
            // 设置状态栏背景为透明
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            // 设置状态栏图标为浅色（白色）
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = false
        }
    }
    
    /**
     * 系统 UI 控制（状态栏和导航栏）
     * 
     * 竖屏模式：
     * - 内容延伸到状态栏下方（视频铺满全屏）
     * - 显示透明状态栏（文字白色）
     * - 隐藏底部导航栏
     * 
     * 横屏模式：
     * - 内容延伸到系统栏区域
     * - 隐藏状态栏和导航栏（真正全屏）
     */
    LaunchedEffect(uiState.isFullscreen) {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            
            if (uiState.isFullscreen) {
                // 横屏模式：让内容扩展到系统栏区域，隐藏所有系统栏
                WindowCompat.setDecorFitsSystemWindows(window, false)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = 
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                // 竖屏模式：内容延伸到状态栏，显示透明状态栏，隐藏导航栏
                WindowCompat.setDecorFitsSystemWindows(window, false)
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.hide(WindowInsetsCompat.Type.navigationBars())
                controller.systemBarsBehavior = 
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
    
    /**
     * 播放时保持屏幕常亮，暂停时允许锁屏
     * 
     * 使用 FLAG_KEEP_SCREEN_ON 标志：
     * - 播放时：添加标志，屏幕保持常亮
     * - 暂停时：移除标志，允许系统进入锁屏
     */
    LaunchedEffect(uiState.isPlaying) {
        activity?.window?.let { window ->
            if (uiState.isPlaying) {
                // 播放时保持屏幕常亮
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                // 暂停时清除标志，允许锁屏
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    
    /**
     * 退出时清理资源
     * 
     * 1. 恢复系统栏显示
     * 2. 恢复状态栏颜色
     * 3. 清除屏幕常亮标志
     */
    DisposableEffect(activity) {
        onDispose {
            activity?.window?.let { window ->
                // 恢复所有系统栏
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
                
                // 恢复状态栏颜色为透明（让主题决定）
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                
                // 清除屏幕常亮标志
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    
    // ============================================================================
    // 副作用：Pager 与 ViewModel 状态同步
    // ============================================================================
    
    /**
     * 监听 Pager 滑动变化 → 同步到 ViewModel
     * 
     * 用户上下滑动时，pagerState.currentPage 会变化
     * 通过 snapshotFlow 转换为 Flow 并收集
     * 当 page 变化时，通知 ViewModel 切换集数
     */
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page != uiState.currentEpisodeIndex) {
                viewModel.switchToEpisode(page)
            }
        }
    }
    
    /**
     * 监听 ViewModel 状态变化 → 同步到 Pager
     * 
     * 当用户从选集弹窗切换时，ViewModel 的 currentEpisodeIndex 会变化
     * 需要同步滚动 Pager 到对应页面
     */
    LaunchedEffect(uiState.currentEpisodeIndex) {
        if (pagerState.currentPage != uiState.currentEpisodeIndex) {
            pagerState.animateScrollToPage(uiState.currentEpisodeIndex)
        }
    }
    
    // ============================================================================
    // 返回键处理
    // ============================================================================
    
    /**
     * 处理系统返回键
     * 
     * 优先级：
     * 1. 横屏模式 → 先退出全屏（变回竖屏）
     * 2. 选集弹窗打开 → 关闭弹窗
     * 3. 其他情况 → 退出播放页面
     */
    BackHandler {
        if (uiState.isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            viewModel.setFullscreen(false)
        } else if (uiState.showEpisodeSelector) {
            viewModel.hideEpisodeSelector()
        } else {
            onBack()
        }
    }
    
    // ============================================================================
    // 辅助函数
    // ============================================================================
    
    /**
     * 切换全屏（横竖屏）
     * 
     * 全屏 → 退出全屏（竖屏）
     * 非全屏 → 进入全屏（横屏）
     */
    val toggleFullscreen: () -> Unit = {
        if (uiState.isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            viewModel.setFullscreen(false)
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            viewModel.setFullscreen(true)
        }
    }
    
    /**
     * 退出播放页面时恢复屏幕方向
     * 
     * 设置为 UNSPECIFIED 让系统根据传感器自动决定方向
     */
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    
    // ============================================================================
    // UI 布局
    // ============================================================================
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (uiState.isLoading) {
            // ==================== 加载中 ====================
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else if (uiState.allEpisodes.isNotEmpty()) {
            // ==================== VerticalPager（抖音式滑动切换） ====================
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // 不预加载其他页面，只渲染当前页
                // 这样可以避免同时创建多个 VideoPlayer
                beyondViewportPageCount = 0,
                // 确保滑动后页面完全对齐（snap），使用设置中的阈值
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapPositionalThreshold = com.wjp.jellyfinduanju.data.settings.AppSettings.swipeThreshold
                )
            ) { page ->
                // 判断是否为当前显示的页面
                val isCurrentPage = page == pagerState.currentPage
                
                // 每页都是一个 PlayerContent
                PlayerContent(
                    exoPlayer = exoPlayer,
                    uiState = uiState,
                    episode = uiState.allEpisodes[page],
                    isCurrentPage = isCurrentPage,
                    showControls = showControls && isCurrentPage,
                    showPlayButton = showPlayButton && isCurrentPage,
                    onToggleControls = {
                        // 横屏模式：切换所有控制栏和播放按钮
                        // 竖屏模式：直接播放/暂停视频
                        if (uiState.isFullscreen) {
                            showControls = !showControls
                        } else {
                            // 竖屏：直接播放/暂停
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
                                // 切换集数期间：只响应 isPlaying = true（表示真正开始播放了）
                                if (isPlaying) {
                                    viewModel.onVideoActuallyStarted()
                                }
                                // 忽略 isPlaying = false（buffering 状态）
                            } else if (!state.isSeeking) {
                                // 正常状态：同步播放状态
                                viewModel.setPlaying(isPlaying)
                            }
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
            // ==================== 无内容 ====================
            Text(
                text = "无可播放内容",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
    
    // ==================== 选集弹窗 ====================
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
    
    // ==================== 倍速选择弹窗 ====================
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
    
    /** 是否正在长按（用于 3x 倍速） */
    var isLongPressing by remember { mutableStateOf(false) }
    
    /** 是否正在水平拖动（调节进度） */
    var isDragging by remember { mutableStateOf(false) }
    
    /** 水平拖动的目标位置 */
    var dragTargetPosition by remember { mutableStateOf(0L) }
    
    // ==================== 手势处理 + 布局（竖屏/横屏不同布局） ====================
    
    if (uiState.isFullscreen) {
        // ==================== 横屏模式：悬浮控制栏（沉浸式） ====================
        Box(
        modifier = Modifier
            .fillMaxSize()
            // 手势1：点击和长按
            .pointerInput(Unit) {
                detectTapGestures(
                    // 单击：切换控制栏显示
                    onTap = { onToggleControls() },
                    // 长按：开始 3x 倍速
                    onLongPress = {
                        isLongPressing = true
                        onLongPressStart()
                    },
                    // 按压：用于检测长按结束
                    onPress = {
                        tryAwaitRelease()  // 等待手指抬起
                        if (isLongPressing) {
                            isLongPressing = false
                            onLongPressEnd()
                        }
                    }
                )
            }
            // 手势2：水平滑动调节进度
            .pointerInput(uiState.duration, uiState.currentPosition) {
                detectHorizontalDragGestures(
                    // 开始拖动
                    onDragStart = {
                        isDragging = true
                        dragTargetPosition = uiState.currentPosition
                        onSeekStart()  // 通知暂停播放
                    },
                    // 结束拖动
                    onDragEnd = {
                        if (isDragging) {
                            val targetPos = dragTargetPosition
                            isDragging = false
                            onSeekEnd(targetPos)  // 跳转并恢复播放
                        }
                    },
                    // 取消拖动
                    onDragCancel = {
                        if (isDragging) {
                            isDragging = false
                            onSeekEnd(null)  // 不跳转，只恢复播放
                        }
                    },
                    // 拖动中
                    onHorizontalDrag = { _, dragAmount ->
                        if (uiState.duration > 0) {
                            // 每滑动 1px 调整 100ms 进度
                            val seekAmount = (dragAmount * 100).toLong()
                            dragTargetPosition = (dragTargetPosition + seekAmount)
                                .coerceIn(0, uiState.duration)
                        }
                    }
                )
            }
    ) {
        // ==================== 视频内容 ====================
        
        // 只有当前页面才渲染视频播放器
        if (isCurrentPage) {
            if (uiState.currentVideoUri != null) {
                // 视频播放器
                VideoPlayer(
                    exoPlayer = exoPlayer,
                    videoUri = uiState.currentVideoUri,
                    isPlaying = uiState.isPlaying,
                    playbackSpeed = uiState.actualSpeed,
                    seekToPosition = uiState.seekToPosition,
                    isFullscreen = uiState.isFullscreen,
                    onProgressChanged = onProgressChanged,
                    onPlaybackStateChanged = onPlaybackStateChanged,
                    onSeekComplete = onSeekComplete,
                    onBufferingChanged = onBufferingChanged,
                    onVideoEnded = onVideoEnded,
                    modifier = Modifier.fillMaxSize()
                )
                
                // 缓冲指示器（视频加载中显示）
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
                // 视频源加载中
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
            // 非当前页面：显示剧集信息占位（预览效果）
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
        
        // ==================== 滑动进度指示器 ====================
        
        // 水平滑动时显示目标时间
        if (isDragging && uiState.duration > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 目标时间（大字）
                    Text(
                        text = PlayerUiState.formatDuration(dragTargetPosition),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // 总时长
                    Text(
                        text = "/ ${PlayerUiState.formatDuration(uiState.duration)}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // ==================== 控制层 ====================
        
        // 顶部和底部控制栏的显示逻辑：
        // - 竖屏模式：始终显示
        // - 横屏模式：受 showControls 控制
        val showBars = !uiState.isFullscreen || showControls
        
        if (showBars) {
            // 顶部渐变遮罩（从黑到透明）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.TopStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
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
            
            // 底部渐变遮罩（从透明到黑）
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
            if (uiState.isLongPressSpeed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 60.dp, end = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${uiState.longPressSpeedValue}x",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    } else {
        // ==================== 竖屏模式：固定底部控制区（抖音风格） ====================
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 手势1：点击和长按
                .pointerInput(Unit) {
                    detectTapGestures(
                        // 单击：直接暂停/播放
                        onTap = { onToggleControls() },
                        // 长按：开始 3x 倍速
                        onLongPress = {
                            isLongPressing = true
                            onLongPressStart()
                        },
                        // 按压：用于检测长按结束
                        onPress = {
                            tryAwaitRelease()  // 等待手指抬起
                            if (isLongPressing) {
                                isLongPressing = false
                                onLongPressEnd()
                            }
                        }
                    )
                }
                // 手势2：水平滑动调节进度
                .pointerInput(uiState.duration, uiState.currentPosition) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                            dragTargetPosition = uiState.currentPosition
                            onSeekStart()
                        },
                        onDragEnd = {
                            if (isDragging) {
                                val targetPos = dragTargetPosition
                                isDragging = false
                                onSeekEnd(targetPos)
                            }
                        },
                        onDragCancel = {
                            if (isDragging) {
                                isDragging = false
                                onSeekEnd(null)
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (uiState.duration > 0) {
                                val seekAmount = (dragAmount * 100).toLong()
                                dragTargetPosition = (dragTargetPosition + seekAmount)
                                    .coerceIn(0, uiState.duration)
                            }
                        }
                    )
                }
        ) {
            // ==================== 视频区域（带顶部控制栏） ====================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)  // 占据剩余空间
            ) {
                // 视频内容
                if (isCurrentPage) {
                    if (uiState.currentVideoUri != null) {
                        // 视频播放器
                        VideoPlayer(
                            exoPlayer = exoPlayer,
                            videoUri = uiState.currentVideoUri,
                            isPlaying = uiState.isPlaying,
                            playbackSpeed = uiState.actualSpeed,
                            seekToPosition = uiState.seekToPosition,
                            isFullscreen = uiState.isFullscreen,
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
                        // 视频源加载中
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
                
                // 水平滑动进度指示器
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
                
                // 顶部渐变遮罩
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.TopStart)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Transparent
                                )
                            )
                        )
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
                if (uiState.isLongPressSpeed) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 60.dp, end = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${uiState.longPressSpeedValue}x",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
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
    
    // ==================== 进度条拖动状态 ====================
    
    /** 是否正在拖动进度条 */
    var isSliderDragging by remember { mutableStateOf(false) }
    
    /** 拖动位置（0.0 ~ 1.0） */
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),  // 固定进度条行高度
            verticalAlignment = Alignment.CenterVertically  // 居中对齐
        ) {
            // 当前时间（拖动时高亮）
            Text(
                text = displayPositionText,
                color = if (isSliderDragging) Color(0xFFFF6B00) else Color.White,
                fontSize = if (isSliderDragging) 14.sp else 12.sp,
                fontWeight = if (isSliderDragging) FontWeight.Bold else FontWeight.Normal
            )
            
            // 进度条 Slider（抖音/快手风格）
            Slider(
                value = displayProgress,
                onValueChange = { progress ->
                    // 开始拖动时通知暂停
                    if (!isSliderDragging) {
                        isSliderDragging = true
                        onSeekStart()
                    }
                    sliderDragPosition = progress
                },
                onValueChangeFinished = {
                    // 松开时：跳转 + 恢复播放
                    if (isSliderDragging && duration > 0) {
                        val targetPos = (sliderDragPosition * duration).toLong()
                        onSeekEnd(targetPos)
                    }
                    isSliderDragging = false
                },
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)  // 限制 Slider 高度
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                ),
                track = { sliderState ->
                    // 自定义轨道，确保已完成和未完成部分高度一致，且垂直居中
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)  // 统一高度2dp
                    ) {
                        // 未完成部分（背景）
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.3f))
                        )
                        // 已完成部分（前景）
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(sliderState.value)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                        }
                    }
                },
                thumb = {
                    // 不显示滑块，使用透明占位
                    Box(
                        modifier = Modifier.size(0.dp)
                    )
                }
            )
            
            // 总时长
            Text(
                text = durationText,
                color = Color.White,
                fontSize = 12.sp
            )
        }
        
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

