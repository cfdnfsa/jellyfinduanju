package com.wjp.jellyfinduanju.ui.screens.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wjp.jellyfinduanju.data.model.EpisodeLocalData
import com.wjp.jellyfinduanju.data.model.SeasonLocalData
import com.wjp.jellyfinduanju.data.model.SeriesLocalData
import com.wjp.jellyfinduanju.ui.theme.JellyfinDuanjuTheme

/**
 * DetailScreen - 剧集详情页
 * 
 * 显示剧集的详细信息，包括：
 * 1. 头部：封面图片 + 标题 + 简介
 * 2. 季列表：每季下显示剧集方框
 * 3. 操作按钮：缓存、删除等
 * 
 * @param viewModel 详情页 ViewModel
 * @param onBack 返回回调
 * @param onEpisodeClick 剧集点击回调（用于播放）
 */
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onEpisodeClick: (EpisodeLocalData) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 监听生命周期，从播放页返回时刷新本地数据
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshFromLocalCache()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // 监听返回信号（删除已从服务器删除的剧集后触发）
    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            onBack()
        }
    }
    
    DetailScreenContent(
        uiState = uiState,
        onBack = onBack,
        onSync = { viewModel.syncFromServer() },
        onUpload = { viewModel.uploadProgressToServer() },
        onEpisodeClick = onEpisodeClick,
        onEpisodeLongClick = { episode -> viewModel.showMarkPlayedDialog(episode) },
        onToggleEpisodeSelection = { episodeId -> viewModel.toggleEpisodeSelection(episodeId) },
        onEnterCacheSelectMode = { viewModel.enterCacheSelectMode() },
        onEnterDeleteSelectMode = { viewModel.enterDeleteSelectMode() },
        onExitSelectMode = { viewModel.exitSelectMode() },
        onToggleSelectAll = { viewModel.toggleSelectAll() },
        onCacheSelected = { viewModel.cacheSelectedEpisodes() },
        onDeleteSelected = { viewModel.deleteSelectedCache() },
        onConfirmMarkPlayed = { viewModel.confirmMarkAsPlayed() },
        onDismissMarkDialog = { viewModel.hideMarkPlayedDialog() },
        onErrorShown = { viewModel.clearError() },
        getImageUrl = { itemId, imageTag, callback ->
            viewModel.getImageUrl(itemId, imageTag, callback)
        }
    )
}

/**
 * DetailScreenContent - 详情页内容（无状态组件）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreenContent(
    uiState: DetailUiState,
    onBack: () -> Unit,
    onSync: () -> Unit,
    onUpload: () -> Unit,
    onEpisodeClick: (EpisodeLocalData) -> Unit,
    onEpisodeLongClick: (EpisodeLocalData) -> Unit,
    onToggleEpisodeSelection: (String) -> Unit,
    onEnterCacheSelectMode: () -> Unit,
    onEnterDeleteSelectMode: () -> Unit,
    onExitSelectMode: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onCacheSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onConfirmMarkPlayed: () -> Unit,
    onDismissMarkDialog: () -> Unit,
    onErrorShown: () -> Unit,
    getImageUrl: (String, String?, (String?) -> Unit) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 显示错误信息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            onErrorShown()
        }
    }
    
    // 显示上报结果
    LaunchedEffect(uiState.uploadResult) {
        uiState.uploadResult?.let { result ->
            snackbarHostState.showSnackbar(result)
        }
    }
    
    Scaffold(
        topBar = {
            DetailTopBar(
                isInSelectMode = uiState.isInSelectMode,
                isCacheMode = uiState.isCacheSelectMode,
                isRefreshing = uiState.isRefreshing,
                isUploading = uiState.isUploading,
                selectedCount = uiState.selectedEpisodeIds.size,
                onBack = onBack,
                onSync = onSync,
                onUpload = onUpload,
                onExitSelectMode = onExitSelectMode,
                onToggleSelectAll = onToggleSelectAll,
                onConfirmAction = if (uiState.isCacheSelectMode) onCacheSelected else onDeleteSelected
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.seasons.isEmpty()) {
            // 首次加载
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // 头部：封面 + 标题 + 简介
                DetailHeader(
                    series = uiState.series,
                    getImageUrl = getImageUrl
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 操作按钮（非选择模式时显示）
                if (!uiState.isInSelectMode) {
                    ActionButtons(
                        cachedCount = uiState.cachedEpisodeCount,
                        totalCount = uiState.totalEpisodeCount,
                        onCacheClick = onEnterCacheSelectMode,
                        onDeleteClick = onEnterDeleteSelectMode
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 季和剧集列表
                uiState.seasons.forEach { season ->
                    val episodes = uiState.episodesBySeasonId[season.id] ?: emptyList()
                    
                    SeasonSection(
                        season = season,
                        episodes = episodes,
                        isInSelectMode = uiState.isInSelectMode,
                        isDeleteMode = uiState.isDeleteSelectMode,
                        selectedEpisodeIds = uiState.selectedEpisodeIds,
                        downloadingEpisodeIds = uiState.downloadingEpisodeIds,
                        pendingEpisodeIds = uiState.pendingEpisodeIds,
                        onEpisodeClick = { episode ->
                            if (uiState.isInSelectMode) {
                                onToggleEpisodeSelection(episode.id)
                            } else {
                                onEpisodeClick(episode)
                            }
                        },
                        onEpisodeLongClick = onEpisodeLongClick,
                        getImageUrl = getImageUrl
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 底部留白
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // 标记已看完对话框
    uiState.episodeToMark?.let { episode ->
        MarkPlayedDialog(
            episode = episode,
            isLoading = uiState.isMarkingPlayed,
            onConfirm = onConfirmMarkPlayed,
            onDismiss = onDismissMarkDialog
        )
    }
}

// ==================== TopBar ====================

/**
 * DetailTopBar - 详情页顶部栏
 * 
 * 正常模式：[←返回]                    [↑上报] [↓同步]
 * 选择模式：[✕取消]  已选择 X 集  [全选] [✓确认]
 * 
 * 同步：从服务器拉取最新数据（播放进度、是否看完）
 * 上报：将本地进度上报到服务器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    isInSelectMode: Boolean,
    isCacheMode: Boolean,
    isRefreshing: Boolean,
    isUploading: Boolean,
    selectedCount: Int,
    onBack: () -> Unit,
    onSync: () -> Unit,
    onUpload: () -> Unit,
    onExitSelectMode: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onConfirmAction: () -> Unit
) {
    TopAppBar(
        title = {
            // 只在选择模式显示标题
            if (isInSelectMode) {
                Text("已选择 $selectedCount 集")
            }
            // 正常模式不显示标题
        },
        navigationIcon = {
            IconButton(onClick = if (isInSelectMode) onExitSelectMode else onBack) {
                Icon(
                    imageVector = if (isInSelectMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isInSelectMode) "取消" else "返回"
                )
            }
        },
        actions = {
            if (isInSelectMode) {
                // 选择模式：全选 + 确认
                IconButton(onClick = onToggleSelectAll) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = "全选"
                    )
                }
                
                IconButton(
                    onClick = onConfirmAction,
                    enabled = selectedCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = if (isCacheMode) "缓存" else "删除",
                        tint = if (selectedCount > 0) {
                            if (isCacheMode) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
            } else {
                // 正常模式：上报按钮
                IconButton(
                    onClick = onUpload,
                    enabled = !isUploading && !isRefreshing
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "上报进度到服务器"
                        )
                    }
                }
                
                // 同步按钮
                IconButton(
                    onClick = onSync,
                    enabled = !isRefreshing && !isUploading
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "从服务器同步数据"
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (isInSelectMode) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    )
}

// ==================== 头部区域 ====================

/**
 * DetailHeader - 详情页头部
 * 
 * 布局：图片 | 标题 + 简介
 */
@Composable
private fun DetailHeader(
    series: SeriesLocalData,
    getImageUrl: (String, String?, (String?) -> Unit) -> Unit
) {
    var imageUrl by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(series.id) {
        getImageUrl(series.id, series.primaryImageTag) { url ->
            imageUrl = url
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 封面图片
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(0.67f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = series.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 标题和简介
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 标题
            Text(
                text = series.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // 年份和集数
            val metaInfo = buildString {
                series.productionYear?.let { append("$it · ") }
                series.episodeCount?.let { append("${it}集") }
                series.seasonCount?.let { 
                    if (it > 1) append(" · ${it}季")
                }
            }
            if (metaInfo.isNotEmpty()) {
                Text(
                    text = metaInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // 简介
            series.overview?.let { overview ->
                if (overview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==================== 操作按钮 ====================

/**
 * ActionButtons - 操作按钮区域
 */
@Composable
private fun ActionButtons(
    cachedCount: Int,
    totalCount: Int,
    onCacheClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 缓存按钮
        OutlinedButton(
            onClick = onCacheClick,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("选择缓存")
        }
        
        // 删除缓存按钮
        OutlinedButton(
            onClick = onDeleteClick,
            modifier = Modifier.weight(1f),
            enabled = cachedCount > 0,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("删除缓存($cachedCount)")
        }
    }
}

// ==================== 季区域 ====================

/**
 * SeasonSection - 单季区域
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeasonSection(
    season: SeasonLocalData,
    episodes: List<EpisodeLocalData>,
    isInSelectMode: Boolean,
    isDeleteMode: Boolean,
    selectedEpisodeIds: Set<String>,
    downloadingEpisodeIds: Set<String>,
    pendingEpisodeIds: Set<String>,
    onEpisodeClick: (EpisodeLocalData) -> Unit,
    onEpisodeLongClick: (EpisodeLocalData) -> Unit,
    getImageUrl: (String, String?, (String?) -> Unit) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 季标题
        Text(
            text = season.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // 剧集网格（使用 FlowRow 实现自动换行）
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            episodes.forEach { episode ->
                val isSelected = selectedEpisodeIds.contains(episode.id)
                val isSelectable = !isDeleteMode || episode.isCached
                val isDownloading = downloadingEpisodeIds.contains(episode.id)
                val isPending = pendingEpisodeIds.contains(episode.id)
                
                EpisodeCard(
                    episode = episode,
                    isSelected = isSelected,
                    isInSelectMode = isInSelectMode,
                    isSelectable = isSelectable,
                    isDownloading = isDownloading,
                    isPending = isPending,
                    onClick = { if (isSelectable || !isInSelectMode) onEpisodeClick(episode) },
                    onLongClick = { if (!isInSelectMode) onEpisodeLongClick(episode) }
                )
            }
        }
    }
}

// ==================== 剧集方框组件 ====================

/**
 * EpisodeCard - 单个剧集方框
 * 
 * 布局：
 * ┌─────────────────┐
 * │ [缓存标记]      │
 * │      集数       │
 * │     进度%      │
 * └─────────────────┘
 * 
 * 颜色：
 * - 绿色边框/背景：已看完
 * - 灰色边框/背景：未看
 * - 选中时有特殊高亮
 * 
 * 缓存图标（右上角）：
 * - 绿色实心圆 + 下载图标：已缓存
 * - 蓝色实心圆 + 旋转图标：正在下载
 * - 橙色实心圆 + 时钟图标：排队等待
 * 
 * @param episode 剧集数据
 * @param isSelected 是否被选中
 * @param isInSelectMode 是否处于选择模式
 * @param isSelectable 是否可选（删除模式下只有已缓存的可选）
 * @param isDownloading 是否正在下载
 * @param isPending 是否在排队中
 * @param onClick 点击回调
 * @param onLongClick 长按回调
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeCard(
    episode: EpisodeLocalData,
    isSelected: Boolean,
    isInSelectMode: Boolean,
    isSelectable: Boolean,
    isDownloading: Boolean = false,
    isPending: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // 根据观看状态确定颜色
    val watchedColor = Color(0xFF4CAF50)  // 绿色 - 已看完
    val unwatchedColor = MaterialTheme.colorScheme.outline  // 灰色 - 未看
    val watchingColor = Color(0xFFFF9800)  // 橙色 - 正在看（有进度但未完成）
    
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        episode.played -> watchedColor
        episode.playbackPositionTicks > 0 -> watchingColor
        else -> unwatchedColor
    }
    
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        episode.played -> watchedColor.copy(alpha = 0.1f)
        episode.playbackPositionTicks > 0 -> watchingColor.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }
    
    Box(
        modifier = Modifier
            .size(width = 72.dp, height = 64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                enabled = isSelectable || !isInSelectMode,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(
                if (!isSelectable && isInSelectMode) {
                    Modifier.background(Color.Black.copy(alpha = 0.3f))
                } else {
                    Modifier
                }
            )
    ) {
        // 缓存/下载状态标记（右上角）
        // 优先级：已缓存 > 正在下载 > 排队中
        val showStatusIcon = episode.isCached || isDownloading || isPending
        
        if (showStatusIcon) {
            val statusColor = when {
                episode.isCached -> Color(0xFF4CAF50)  // 绿色 - 已缓存
                isDownloading -> Color(0xFF2196F3)     // 蓝色 - 下载中
                else -> Color(0xFFFF9800)               // 橙色 - 排队中
            }
            
            // 所有状态都使用下载图标，通过颜色区分
            val statusIcon = Icons.Default.Download
            
            val statusDesc = when {
                episode.isCached -> "已缓存"
                isDownloading -> "下载中"
                else -> "排队中"
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
                    .background(
                        color = statusColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = statusDesc,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        
        // 选中标记（左上角）
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选中",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        
        // 内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 集数
            Text(
                text = "${episode.episodeNumber}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (episode.played) watchedColor else MaterialTheme.colorScheme.onSurface
            )
            
            // 进度
            Text(
                text = episode.getProgressText(),
                style = MaterialTheme.typography.labelSmall,
                color = if (episode.played) {
                    watchedColor.copy(alpha = 0.7f)
                } else if (episode.playbackPositionTicks > 0) {
                    watchingColor
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

// ==================== 对话框 ====================

/**
 * MarkPlayedDialog - 标记已看完对话框
 */
@Composable
private fun MarkPlayedDialog(
    episode: EpisodeLocalData,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isPlayed = episode.played
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isPlayed) "取消标记" else "标记已看完")
        },
        text = {
            Text(
                if (isPlayed) {
                    "确定要取消第 ${episode.episodeNumber} 集的已看完标记吗？"
                } else {
                    "确定要将第 ${episode.episodeNumber} 集标记为已看完吗？"
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("确定")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("取消")
            }
        }
    )
}

// ==================== 预览 ====================

@Preview(
    name = "详情页",
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun PreviewDetailScreen() {
    val series = SeriesLocalData(
        id = "1",
        name = "测试剧集",
        overview = "这是一段很长的剧情简介，用于测试文本显示效果。这是一段很长的剧情简介，用于测试文本显示效果。",
        productionYear = 2024,
        episodeCount = 24,
        seasonCount = 2
    )
    
    val seasons = listOf(
        SeasonLocalData("s1", "1", "第 1 季", 1, 12),
        SeasonLocalData("s2", "1", "第 2 季", 2, 12)
    )
    
    val episodes = (1..12).map { i ->
        EpisodeLocalData(
            id = "e$i",
            seriesId = "1",
            seasonId = "s1",
            name = "第 $i 集",
            episodeNumber = i,
            seasonNumber = 1,
            runTimeTicks = 30L * 60 * 10_000_000, // 30分钟
            played = i <= 3,
            playbackPositionTicks = if (i == 4) 15L * 60 * 10_000_000 else 0,
            isCached = i <= 2
        )
    }
    
    JellyfinDuanjuTheme(darkTheme = true) {
        DetailScreenContent(
            uiState = DetailUiState(
                series = series,
                seasons = seasons,
                episodesBySeasonId = mapOf("s1" to episodes)
            ),
            onBack = {},
            onSync = {},
            onUpload = {},
            onEpisodeClick = {},
            onEpisodeLongClick = {},
            onToggleEpisodeSelection = {},
            onEnterCacheSelectMode = {},
            onEnterDeleteSelectMode = {},
            onExitSelectMode = {},
            onToggleSelectAll = {},
            onCacheSelected = {},
            onDeleteSelected = {},
            onConfirmMarkPlayed = {},
            onDismissMarkDialog = {},
            onErrorShown = {},
            getImageUrl = { _, _, _ -> }
        )
    }
}

@Preview(
    name = "选择模式",
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun PreviewSelectMode() {
    val series = SeriesLocalData(
        id = "1",
        name = "测试剧集",
        episodeCount = 12,
        seasonCount = 1
    )
    
    val seasons = listOf(
        SeasonLocalData("s1", "1", "第 1 季", 1, 12)
    )
    
    val episodes = (1..12).map { i ->
        EpisodeLocalData(
            id = "e$i",
            seriesId = "1",
            seasonId = "s1",
            name = "第 $i 集",
            episodeNumber = i,
            seasonNumber = 1,
            isCached = i <= 4
        )
    }
    
    JellyfinDuanjuTheme(darkTheme = true) {
        DetailScreenContent(
            uiState = DetailUiState(
                series = series,
                seasons = seasons,
                episodesBySeasonId = mapOf("s1" to episodes),
                isCacheSelectMode = true,
                selectedEpisodeIds = setOf("e5", "e6", "e7")
            ),
            onBack = {},
            onSync = {},
            onUpload = {},
            onEpisodeClick = {},
            onEpisodeLongClick = {},
            onToggleEpisodeSelection = {},
            onEnterCacheSelectMode = {},
            onEnterDeleteSelectMode = {},
            onExitSelectMode = {},
            onToggleSelectAll = {},
            onCacheSelected = {},
            onDeleteSelected = {},
            onConfirmMarkPlayed = {},
            onDismissMarkDialog = {},
            onErrorShown = {},
            getImageUrl = { _, _, _ -> }
        )
    }
}

