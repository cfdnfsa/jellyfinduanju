package com.wjp.jellyfinduanju.ui.screens.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.wjp.jellyfinduanju.data.model.SeriesLocalData
import com.wjp.jellyfinduanju.ui.theme.JellyfinDuanjuTheme

/**
 * ListScreen - 剧集列表页面
 * 
 * 显示 Jellyfin 服务器上的剧集列表
 * 
 * 功能：
 * 1. 一行三个的网格布局
 * 2. 显示剧集封面图和标题
 * 3. 顶部搜索框和刷新按钮
 * 4. 支持模糊搜索筛选
 * 
 * @param viewModel 列表页面的 ViewModel
 * @param onSeriesClick 剧集点击回调（后续实现）
 */
@Composable
fun ListScreen(
    viewModel: ListViewModel,
    onSeriesClick: (SeriesLocalData) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    ListScreenContent(
        uiState = uiState,
        onRefreshClick = { viewModel.refreshSeriesList() },
        onSeriesClick = onSeriesClick,
        onErrorShown = { viewModel.clearError() },
        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
        onClearSearch = { viewModel.clearSearch() },
        getImageUrl = { seriesId, callback -> 
            viewModel.getSeriesImageUrl(seriesId, callback) 
        }
    )
}

/**
 * ListScreenContent - 列表页面内容（无状态组件）
 * 
 * @param uiState UI 状态
 * @param onRefreshClick 刷新按钮点击回调
 * @param onSeriesClick 剧集点击回调
 * @param onErrorShown 错误显示后的回调
 * @param onSearchQueryChange 搜索关键词变化回调
 * @param onClearSearch 清除搜索回调
 * @param getImageUrl 获取图片 URL 的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListScreenContent(
    uiState: ListUiState,
    onRefreshClick: () -> Unit,
    onSeriesClick: (SeriesLocalData) -> Unit,
    onErrorShown: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    getImageUrl: (String, (String?) -> Unit) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    
    // 显示错误信息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            onErrorShown()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // 搜索框
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        placeholder = { 
                            Text(
                                "搜索剧集...",
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = onClearSearch) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "清除",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                            }
                        )
                    )
                },
                actions = {
                    // 刷新按钮
                    IconButton(
                        onClick = onRefreshClick,
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // 加载中且列表为空
                uiState.isLoading && uiState.seriesList.isEmpty() -> {
                    LoadingContent()
                }
                // 列表为空
                uiState.isEmpty && !uiState.isLoading -> {
                    EmptyContent(onRefreshClick = onRefreshClick)
                }
                // 显示列表
                else -> {
                    SeriesGrid(
                        seriesList = uiState.seriesList,
                        onSeriesClick = onSeriesClick,
                        getImageUrl = getImageUrl
                    )
                }
            }
        }
    }
}

/**
 * SeriesGrid - 剧集网格列表
 * 
 * 一行三个的网格布局
 * 
 * @param seriesList 剧集列表
 * @param onSeriesClick 剧集点击回调
 * @param getImageUrl 获取图片 URL 的回调
 */
@Composable
private fun SeriesGrid(
    seriesList: List<SeriesLocalData>,
    onSeriesClick: (SeriesLocalData) -> Unit,
    getImageUrl: (String, (String?) -> Unit) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),  // 固定一行三个
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = seriesList,
            key = { it.id }
        ) { series ->
            SeriesGridItem(
                series = series,
                onClick = { onSeriesClick(series) },
                getImageUrl = getImageUrl
            )
        }
    }
}

/**
 * SeriesGridItem - 剧集网格项
 * 
 * 显示单个剧集的封面图和标题
 * 
 * @param series 剧集数据
 * @param onClick 点击回调
 * @param getImageUrl 获取图片 URL 的回调
 */
@Composable
private fun SeriesGridItem(
    series: SeriesLocalData,
    onClick: () -> Unit,
    getImageUrl: (String, (String?) -> Unit) -> Unit
) {
    // 图片 URL 状态
    var imageUrl by remember { mutableStateOf<String?>(null) }
    
    // 获取图片 URL
    LaunchedEffect(series.id) {
        getImageUrl(series.id) { url ->
            imageUrl = url
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column {
            // 图片区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.67f)  // 2:3 的宽高比（海报比例）
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (imageUrl != null) {
                    // 显示图片
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = series.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // 占位图
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                
                // 左上角：观看进度状态
                val hasProgress = series.lastWatchPositionTicks > 0 || series.played
                if (hasProgress) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .size(24.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (series.played) {
                            // 已看完：绿色 ✓
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "已看完",
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF4CAF50)  // 绿色
                            )
                        } else {
                            // 正在看：橙色播放图标
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "正在看",
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFFFF9800)  // 橙色
                            )
                        }
                    }
                }
                
                // 右上角：集数和季数标签
                val episodeCount = series.episodeCount
                val seasonCount = series.seasonCount
                if (episodeCount != null && episodeCount > 0 || seasonCount != null && seasonCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        // 格式：12集2季 或 12集 或 2季
                        val text = buildString {
                            episodeCount?.let { if (it > 0) append("${it}集") }
                            seasonCount?.let { if (it > 0) append("${it}季") }
                        }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            // 标题区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = series.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * LoadingContent - 加载中内容
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "加载中...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * EmptyContent - 空列表内容
 * 
 * @param onRefreshClick 刷新按钮点击回调
 */
@Composable
private fun EmptyContent(
    onRefreshClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无剧集",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右上角刷新按钮获取剧集列表",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            androidx.compose.material3.Button(
                onClick = onRefreshClick
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("刷新")
            }
        }
    }
}

// ==================== 预览 ====================

@Preview(
    name = "剧集列表页面",
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun PreviewListScreen() {
    JellyfinDuanjuTheme(darkTheme = true) {
        ListScreenContent(
            uiState = ListUiState(
                seriesList = listOf(
                    SeriesLocalData(
                        id = "1",
                        name = "这是一个很长的剧集名称测试",
                        episodeCount = 24,
                        seasonCount = 2,
                        played = true  // 已看完
                    ),
                    SeriesLocalData(
                        id = "2",
                        name = "短名称",
                        episodeCount = 12,
                        seasonCount = 1,
                        lastWatchPositionTicks = 100000  // 正在看
                    ),
                    SeriesLocalData(
                        id = "3",
                        name = "测试剧集",
                        episodeCount = 8,
                        seasonCount = 1
                        // 未开始
                    ),
                    SeriesLocalData(
                        id = "4",
                        name = "另一个剧集",
                        episodeCount = 16,
                        seasonCount = 2,
                        played = true  // 已看完
                    )
                ),
                isEmpty = false,
                searchQuery = ""
            ),
            onRefreshClick = {},
            onSeriesClick = {},
            onErrorShown = {},
            onSearchQueryChange = {},
            onClearSearch = {},
            getImageUrl = { _, _ -> }
        )
    }
}

@Preview(
    name = "空列表",
    showBackground = true
)
@Composable
private fun PreviewEmptyList() {
    JellyfinDuanjuTheme {
        EmptyContent(onRefreshClick = {})
    }
}

