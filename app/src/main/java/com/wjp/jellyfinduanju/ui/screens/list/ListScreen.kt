package com.wjp.jellyfinduanju.ui.screens.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wjp.jellyfinduanju.data.model.SeriesLocalData

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
    
    // 监听生命周期，每次页面恢复（Resume）时刷新观看状态
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshWatchStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ListScreenContent(
        uiState = uiState,
        allFilterTags = viewModel.getAllFilterTags(),
        onRefreshClick = { viewModel.refreshSeriesList() },
        onSeriesClick = onSeriesClick,
        onSeriesLongClick = { series -> viewModel.showSeriesMenu(series) },
        onDismissMenu = { viewModel.hideSeriesMenu() },
        onMarkAsWatched = { viewModel.markSeriesAsWatched(true) },
        onMarkAsUnwatched = { viewModel.markSeriesAsWatched(false) },
        onCacheAll = { viewModel.showCacheAllDialog() },
        onDeleteAllCache = { viewModel.showDeleteAllCacheDialog() },
        onSetTag = { viewModel.showTagDialog() },
        onConfirmCacheAll = { viewModel.confirmCacheAll() },
        onConfirmDeleteAllCache = { viewModel.confirmDeleteAllCache() },
        onToggleTag = { tag, selected -> viewModel.toggleSeriesTag(tag, selected) },
        onClearTags = { viewModel.clearSeriesTags() },
        onFilterTagChange = { viewModel.updateFilterTag(it) },
        onDismissDialog = { viewModel.hideDialog() },
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListScreenContent(
    uiState: ListUiState,
    allFilterTags: List<String>,
    onRefreshClick: () -> Unit,
    onSeriesClick: (SeriesLocalData) -> Unit,
    onSeriesLongClick: (SeriesLocalData) -> Unit,
    onDismissMenu: () -> Unit,
    onMarkAsWatched: () -> Unit,
    onMarkAsUnwatched: () -> Unit,
    onCacheAll: () -> Unit,
    onDeleteAllCache: () -> Unit,
    onSetTag: () -> Unit,
    onConfirmCacheAll: () -> Unit,
    onConfirmDeleteAllCache: () -> Unit,
    onToggleTag: (String, Boolean) -> Unit,
    onClearTags: () -> Unit,
    onFilterTagChange: (String) -> Unit,
    onDismissDialog: () -> Unit,
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
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 标签筛选栏
                        TagFilterBar(
                            allTags = allFilterTags,
                            selectedTag = uiState.selectedFilterTag,
                            onTagSelected = onFilterTagChange
                        )
                        
                        // 剧集网格
                        SeriesGrid(
                            seriesList = uiState.seriesList,
                            selectedSeries = uiState.selectedSeries,
                            onSeriesClick = onSeriesClick,
                            onSeriesLongClick = onSeriesLongClick,
                            onDismissMenu = onDismissMenu,
                            onMarkAsWatched = onMarkAsWatched,
                            onMarkAsUnwatched = onMarkAsUnwatched,
                            onCacheAll = onCacheAll,
                            onDeleteAllCache = onDeleteAllCache,
                            onSetTag = onSetTag,
                            getImageUrl = getImageUrl
                        )
                    }
                }
            }
        }
    }
    
    // 确认对话框
    uiState.dialogType?.let { dialogType ->
        val series = uiState.selectedSeries ?: return@let
        
        when (dialogType) {
            DialogType.CACHE_ALL, DialogType.DELETE_ALL_CACHE -> {
                AlertDialog(
                    onDismissRequest = onDismissDialog,
                    title = {
                        Text(
                            text = when (dialogType) {
                                DialogType.CACHE_ALL -> "缓存全部"
                                DialogType.DELETE_ALL_CACHE -> "删除全部缓存"
                                else -> ""
                            }
                        )
                    },
                    text = {
                        Text(
                            text = when (dialogType) {
                                DialogType.CACHE_ALL -> "确定要下载「${series.name}」的全部剧集吗？"
                                DialogType.DELETE_ALL_CACHE -> "确定要删除「${series.name}」的全部本地缓存吗？"
                                else -> ""
                            }
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                when (dialogType) {
                                    DialogType.CACHE_ALL -> onConfirmCacheAll()
                                    DialogType.DELETE_ALL_CACHE -> onConfirmDeleteAllCache()
                                    else -> {}
                                }
                            }
                        ) {
                            Text("确定")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismissDialog) {
                            Text("取消")
                        }
                    }
                )
            }
            
            DialogType.SET_TAG -> {
                // 标签选择对话框（多选）
                val customTags = com.wjp.jellyfinduanju.data.settings.AppSettings.customTags
                AlertDialog(
                    onDismissRequest = onDismissDialog,
                    title = { Text("设置标签") },
                    text = {
                        Column {
                            // ===== 系统标签（只读） =====
                            Text(
                                text = "系统标签",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // 缓存状态标签
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (series.isVideoCached) Icons.Default.Download else Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = if (series.isVideoCached) 
                                        MaterialTheme.colorScheme.tertiary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (series.isVideoCached) "已缓存" else "未缓存",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (series.isVideoCached) 
                                        MaterialTheme.colorScheme.tertiary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // 云端文件夹标签
                            series.cloudFolderTag?.let { cloudTag ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = cloudTag,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            
                            // ===== 自定义标签（可编辑） =====
                            Text(
                                text = "自定义标签",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            if (customTags.isNotEmpty()) {
                                customTags.forEach { tag ->
                                    val isSelected = tag in series.customTags
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onToggleTag(tag, !isSelected) }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { onToggleTag(tag, it) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(tag)
                                    }
                                }
                            } else {
                                Text(
                                    text = "暂无自定义标签，请到设置中添加",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = onDismissDialog) {
                            Text("完成")
                        }
                    },
                    dismissButton = {
                        if (series.customTags.isNotEmpty()) {
                            TextButton(onClick = onClearTags) {
                                Text("清除全部")
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * TagFilterBar - 标签筛选栏
 * 
 * 横向滚动显示所有可用标签
 */
@Composable
private fun TagFilterBar(
    allTags: List<String>,
    selectedTag: String,
    onTagSelected: (String) -> Unit
) {
    // 系统标签列表（用于判断标签类型）
    listOf("全部", "已缓存", "未缓存")
    val customTags = com.wjp.jellyfinduanju.data.settings.AppSettings.customTags
    
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(allTags.size) { index ->
            val tag = allTags[index]
            val isSelected = tag == selectedTag
            
            // 根据标签类型设置不同颜色
            val (containerColor, labelColor, icon) = when {
                tag == "全部" -> Triple(
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    null
                )
                tag == "已缓存" -> Triple(
                    if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                    if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    Icons.Default.Download
                )
                tag == "未缓存" -> Triple(
                    if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                    if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    Icons.Default.CloudQueue
                )
                tag in customTags -> Triple(
                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    Icons.Default.Label
                )
                // 其他的都是云端文件夹标签
                else -> Triple(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    Icons.Default.Folder
                )
            }
            
            FilterChip(
                selected = isSelected,
                onClick = { onTagSelected(tag) },
                label = { 
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelMedium
                    ) 
                },
                leadingIcon = icon?.let { iconVector ->
                    {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = containerColor,
                    selectedLabelColor = labelColor,
                    selectedLeadingIconColor = labelColor
                )
            )
        }
    }
}

/**
 * SeriesGrid - 剧集网格列表
 * 
 * 一行三个的网格布局，支持长按显示菜单
 */
@Composable
private fun SeriesGrid(
    seriesList: List<SeriesLocalData>,
    selectedSeries: SeriesLocalData?,
    onSeriesClick: (SeriesLocalData) -> Unit,
    onSeriesLongClick: (SeriesLocalData) -> Unit,
    onDismissMenu: () -> Unit,
    onMarkAsWatched: () -> Unit,
    onMarkAsUnwatched: () -> Unit,
    onCacheAll: () -> Unit,
    onDeleteAllCache: () -> Unit,
    onSetTag: () -> Unit,
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
                isMenuExpanded = selectedSeries?.id == series.id,
                onClick = { onSeriesClick(series) },
                onLongClick = { onSeriesLongClick(series) },
                onDismissMenu = onDismissMenu,
                onMarkAsWatched = onMarkAsWatched,
                onMarkAsUnwatched = onMarkAsUnwatched,
                onCacheAll = onCacheAll,
                onDeleteAllCache = onDeleteAllCache,
                onSetTag = onSetTag,
                getImageUrl = getImageUrl
            )
        }
    }
}

/**
 * SeriesGridItem - 剧集网格项
 * 
 * 显示单个剧集的封面图和标题，支持长按显示菜单
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeriesGridItem(
    series: SeriesLocalData,
    isMenuExpanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onMarkAsWatched: () -> Unit,
    onMarkAsUnwatched: () -> Unit,
    onCacheAll: () -> Unit,
    onDeleteAllCache: () -> Unit,
    onSetTag: () -> Unit,
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
    
    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
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
                
                // 左上角：已删除标识或观看进度状态
                if (series.isDeletedFromServer) {
                    // 优先显示：已从服务器删除（红色）
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
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "已从服务器删除",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFFF44336)  // 红色
                        )
                    }
                } else {
                    // 观看进度状态判断：
                    // 1. played = true → 已看完（绿色）
                    // 2. played = false && lastWatchTime > 0 → 正在看（橙色）
                    // 3. played = false && lastWatchTime = 0 → 没看过（不显示图标）
                    val hasProgress = series.played || series.lastWatchTime > 0
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
        }  // Card 闭合
        
        // 长按菜单
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = onDismissMenu
        ) {
            // 标记已看/未看
            if (series.played) {
                DropdownMenuItem(
                    text = { Text("标记为未看") },
                    onClick = onMarkAsUnwatched,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                )
            } else {
                DropdownMenuItem(
                    text = { Text("标记为已看") },
                    onClick = onMarkAsWatched,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                )
            }
            
            // 缓存全部
            DropdownMenuItem(
                text = { Text("缓存全部") },
                onClick = onCacheAll,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null
                    )
                }
            )
            
            // 删除全部缓存
            DropdownMenuItem(
                text = { Text("删除全部缓存") },
                onClick = onDeleteAllCache,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
            
            // 设置标签
            DropdownMenuItem(
                text = { Text("设置标签") },
                onClick = onSetTag,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null
                    )
                }
            )
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
