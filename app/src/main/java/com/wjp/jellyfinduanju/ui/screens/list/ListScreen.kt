package com.wjp.jellyfinduanju.ui.screens.list

import androidx.compose.foundation.ExperimentalFoundationApi
import com.wjp.jellyfinduanju.ui.components.SearchableTopBar
import com.wjp.jellyfinduanju.ui.components.SortMenuItem
import com.wjp.jellyfinduanju.ui.components.SortOption
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.wjp.jellyfinduanju.data.model.SeriesLocalData
import com.wjp.jellyfinduanju.data.settings.AppSettings
import kotlinx.coroutines.launch

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
 * 5. 标签筛选（全部、正在看、已缓存等）
 * 6. 支持云端/本地模式切换
 * 7. 长按显示菜单（标记已看、缓存、删除等）
 * 
 * @param viewModel 列表页面的 ViewModel，管理所有业务逻辑和状态
 * @param onSeriesClick 剧集点击回调，点击剧集卡片时触发（跳转到详情页）
 */
@Composable
fun ListScreen(
    viewModel: ListViewModel,
    onSeriesClick: (SeriesLocalData) -> Unit = {}
) {
    // ===== 1. 状态收集 =====

    // UI 状态  必须用 copy() 创建新对象才能触发   直接修改 uiState.xxx = xxx 不会触发
    val uiState by viewModel.uiState.collectAsState()


    // ===== 2. 初始化监听和状态 =====

    // 设置生命周期监听：当页面从后台返回前台时，刷新观看状态
    SetupLifecycleObserver(viewModel)

    // 底部的弹窗
    val snackbarHostState = remember { SnackbarHostState() }
    // 处理错误消息显示：当有错误产生时，通过 Snackbar 显示
    HandleErrorMessages(uiState.error, snackbarHostState, viewModel)


    // ===== 3. Pager 状态初始化 =====

    // 获取所有标签
    val allTags = viewModel.getAllFilterTags()
    
    // 当前选中标签的索引位置
    val selectedIndex = allTags.indexOf(uiState.selectedFilterTag).coerceAtLeast(0)
    
    // Pager

    // ListScreen
    //    ├─ 标签栏（TagFilterBar）         ← 这个不是 Pager
    //    │   [全部] [正在看] [已缓存]
    //    └─ 内容区（HorizontalPager）       ← 这个才是 Pager！
    //    └─ 第0页：显示"全部"的剧集
    val pagerState = rememberPagerState(
        initialPage = selectedIndex,          // 初始显示的页面
        pageCount = { allTags.size }          // 总页数 = 标签数量
    )
    // 同步Pager滑动状态到ViewModel    监听pagerState  pagerState不带回调
    SyncPagerWithViewModel(
        pagerState = pagerState,
        allTags = allTags,
        selectedFilterTag = uiState.selectedFilterTag,
        onFilterTagChange = { viewModel.updateFilterTag(it) }
    )
    
    // ===== 4. 主界面内容 =====
    // 渲染主界面（包括顶部栏、标签筛选栏、剧集网格）
    MainScreenContent(
        viewModel = viewModel,
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        allTags = allTags,
        pagerState = pagerState,
        onSeriesClick = onSeriesClick
    )
    
    // ===== 5. 对话框 =====
    // 渲染对话框（缓存全部、删除缓存、删除剧集、设置标签）
    // 根据 uiState.dialogType 决定显示哪个对话框
    DialogContent(
        viewModel = viewModel,
        uiState = uiState
    )
}

/**
 * MainScreenContent - 主界面内容
 * 
 * 包含顶部栏、标签筛选栏、内容分页器
 *
 * Scaffold(
 *     topBar = {                    // 顶栏插槽
 *         TopAppBar(title = { Text("列表") })
 *     },
 *     bottomBar = {                 // 底部导航栏插槽
 *         NavigationBar { ... }
 *     },
 *     snackbarHost = {              // Snackbar 提示插槽
 *         SnackbarHost(snackbarHostState)
 *     },
 *     floatingActionButton = {      // 浮动按钮插槽（可选）
 *         FloatingActionButton(onClick = {}) {
 *             Icon(Icons.Default.Add, null)
 *         }
 *     }
 * ) { paddingValues ->             // 内容区域（lambda 参数）
 *     // paddingValues 自动计算了顶栏和底栏占用的空间
 *     LazyColumn(
 *         modifier = Modifier.padding(paddingValues)  // 避免被遮挡
 *     ) {
 *         // 内容
 *     }
 * }
 */
@Composable
/**
 * MainScreenContent - 列表页主内容区域
 * 
 * @param viewModel ListViewModel，管理页面状态和业务逻辑
 * @param uiState 当前 UI 状态，包含剧集列表、加载状态等
 * @param snackbarHostState Snackbar 状态管理器，用于显示错误提示
 * @param allTags 所有可用的标签列表，用于标签筛选栏
 * @param pagerState Pager 状态，管理标签页切换
 * @param onSeriesClick 点击剧集的回调函数，传递被点击的剧集数据
 */
private fun MainScreenContent(
    viewModel: ListViewModel,
    uiState: ListUiState,
    snackbarHostState: SnackbarHostState,
    allTags: List<String>,
    pagerState: PagerState,
    onSeriesClick: (SeriesLocalData) -> Unit
) {
    // 获取协程作用域（用于执行异步操作，如 Pager 滚动动画）
    val coroutineScope = rememberCoroutineScope()
    
    // Scaffold 提供了标准的 Material Design 布局结构
    Scaffold(
        // 顶部栏：搜索框 + 右侧操作按钮
        topBar = {
            ListSearchTopBar(
                viewModel = viewModel,
                uiState = uiState
            )
        },
        // 底部消息提示（用于显示错误信息）
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues -> // paddingValues: 自动计算padding要不然内容区会被挡住
        // 主内容区域（垂直布局）
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // 标签筛选栏（全部、正在看、已缓存等）
            TagFilterBar(
                allTags = allTags,  // 所有标签
                currentPage = pagerState.currentPage,  // 当前选中的标签页
                onTagSelected = { tag ->
                    // 点击标签时，滑动 Pager 到对应页面
                    val index = allTags.indexOf(tag)
                    if (index >= 0) {
                        coroutineScope.launch {
                            // 带动画地滚动到指定页面
                            pagerState.animateScrollToPage(index)
                        }
                    }
                }
            )
            
            // 横向分页器（可以左右滑动切换标签）
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                // 每个页面的内容
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        // 场景1：正在加载，且列表为空 → 显示加载动画
                        uiState.isLoading && uiState.seriesList.isEmpty() -> {
                            LoadingContent()
                        }
                        // 场景2：列表为空，且不在加载中 → 显示空状态提示
                        uiState.isEmpty && !uiState.isLoading -> {
                            EmptyContent(onRefreshClick = { viewModel.refreshSeriesList() })
                        }
                        // 场景3：正常显示剧集网格列表
                        else -> {
                            SeriesGrid(
                                viewModel = viewModel,
                                uiState = uiState,
                                onSeriesClick = onSeriesClick
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * SetupLifecycleObserver - 设置生命周期观察者
 * 
 * 监听页面恢复事件，刷新观看状态
 */
@Composable
private fun SetupLifecycleObserver(viewModel: ListViewModel) {
    // 获取当前的生命周期所有者（通常是 Activity 或 Fragment）
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // DisposableEffect 会在组件销毁时自动清理资源
    DisposableEffect(lifecycleOwner) {
        // 创建生命周期观察者，监听生命周期事件
        val observer = LifecycleEventObserver { _, event ->
            // 当页面恢复到前台时（比如从详情页返回列表页）
            if (event == Lifecycle.Event.ON_RESUME) {
                // 刷新观看状态（同步最新的播放进度、已看/未看标记等）
                viewModel.refreshWatchStatus()
            }
        }
        
        // 将观察者注册到生命周期
        lifecycleOwner.lifecycle.addObserver(observer)
        
        // 返回清理函数，在组件销毁时执行
        onDispose {
            // 移除观察者，避免内存泄漏
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * HandleErrorMessages - 处理错误消息显示
 */
@Composable
private fun HandleErrorMessages(
    error: String?,
    snackbarHostState: SnackbarHostState,
    viewModel: ListViewModel
) {
    // LaunchedEffect 会在 error 变化时重新执行
    // 这样每次有新错误产生时，都会触发显示逻辑
    LaunchedEffect(error) {
        // 如果错误不为空（有错误产生）
        error?.let {
            // 显示 Snackbar 提示错误信息（底部弹出的消息条）
            snackbarHostState.showSnackbar(it)
            
            // 清除 ViewModel 中的错误状态
            // 避免重复显示相同的错误，或者页面重组时再次显示
            viewModel.clearError()
        }
    }
}

/**
 * SyncPagerWithViewModel - 同步 Pager 滑动状态到 ViewModel
 * 
 * 监听用户滑动内容区，自动更新 ViewModel 中的选中标签
 * 
 * @param pagerState Pager 状态对象，用于读取当前滑动到的页面索引
 * @param allTags 所有标签列表，用于根据页面索引查找对应的标签名称
 * @param selectedFilterTag ViewModel 中当前选中的标签，用于判断是否需要更新（避免重复）
 * @param onFilterTagChange 标签变化回调，通知 ViewModel 更新选中的标签
 */
@Composable
private fun SyncPagerWithViewModel(
    pagerState: PagerState,
    allTags: List<String>,
    selectedFilterTag: String,
    onFilterTagChange: (String) -> Unit
) {
    // LaunchedEffect 监听 pagerState.settledPage 的变化
    // settledPage 表示滑动停止后的最终页面（区别于滑动中的 currentPage）
    LaunchedEffect(pagerState.settledPage) {
        // 边界检查：确保页面索引在有效范围内
        if (pagerState.settledPage < allTags.size) {
            // 根据当前页面索引，获取对应的标签
            val newTag = allTags[pagerState.settledPage]
            
            // 如果新标签和当前选中的标签不同
            // 避免重复触发更新（比如用户点击标签导致的滑动）
            if (newTag != selectedFilterTag) {
                // 通知 ViewModel 更新选中的筛选标签
                // 这样标签筛选栏和内容列表会同步更新
                onFilterTagChange(newTag)
            }
        }
    }
}

/**
 * ListSearchTopBar - 列表页的搜索顶栏
 */
@Composable
private fun ListSearchTopBar(
    viewModel: ListViewModel,
    uiState: ListUiState
) {
    // 使用通用的 SearchableTopBar 组件
    SearchableTopBar(
        searchQuery = uiState.searchQuery,  // 当前搜索关键词
        onSearchQueryChange = { viewModel.updateSearchQuery(it) },  // 搜索变化
        onClearSearch = { viewModel.clearSearch() },  // 清除搜索
        actions = {  // 右侧操作按钮（通过插槽自定义）
            // 排序按钮 + 排序菜单
            SortButton(viewModel = viewModel, uiState = uiState)
            // 视图模式切换按钮（云端 ↔ 本地）
            ViewModeToggleButton(viewModel = viewModel, uiState = uiState)
            // 刷新按钮
            RefreshButton(viewModel = viewModel, uiState = uiState)
        }
    )
}

/**
 * SortButton - 排序按钮 + 排序菜单
 * 
 * @param viewModel ViewModel
 * @param uiState UI 状态
 */
@Composable
private fun SortButton(
    viewModel: ListViewModel,
    uiState: ListUiState
) {
    // 排序菜单显示状态（组件内部管理）
    var showSortMenu by remember { mutableStateOf(false) }
    
    // 排序选项配置列表（数据驱动）
    val sortOptions = remember {
        listOf(
            SortOption(SortType.NAME, "按名称", Icons.Default.SortByAlpha),
            SortOption(SortType.UPDATE_TIME, "按更新时间", Icons.Default.Update),
            SortOption(SortType.PLAY_TIME, "按播放时间", Icons.Default.PlayArrow)
        )
    }
    
    Box {
        // 排序按钮
        IconButton(onClick = { showSortMenu = true }) {  // 点击打开菜单
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "排序"
            )
        }

        // 排序菜单（数据驱动 + forEach 循环）
        DropdownMenu(
            expanded = showSortMenu,  // 根据状态显示/隐藏
            onDismissRequest = { showSortMenu = false }  // 点击外部关闭
        ) {
            // 遍历排序选项列表，生成菜单项
            sortOptions.forEach { option ->
                SortMenuItem(
                    label = option.label,  // 显示文本
                    icon = option.icon,  // 图标
                    isSelected = uiState.sortType == option.type,  // 是否选中
                    sortAscending = uiState.sortAscending,  // 升序/降序
                    onClick = {
                        viewModel.toggleSortType(option.type)  // 切换排序类型
                        showSortMenu = false  // 关闭菜单
                    }
                )
            }
        }
    }
}

/**
 * ViewModeToggleButton - 视图模式切换按钮（云端 ↔ 本地）
 * 
 * @param viewModel ViewModel
 * @param uiState UI 状态
 */
@Composable
private fun ViewModeToggleButton(
    viewModel: ListViewModel,
    uiState: ListUiState
) {
    IconButton(onClick = { viewModel.toggleViewMode() }) {
        Icon(
            imageVector = if (uiState.viewMode == ViewMode.CLOUD) {
                Icons.Default.CloudQueue  // 云端图标
            } else {
                Icons.Default.Folder  // 本地文件夹图标
            },
            contentDescription = if (uiState.viewMode == ViewMode.CLOUD) {
                "切换到本地"
            } else {
                "切换到云端"
            },
            tint = if (uiState.viewMode == ViewMode.CLOUD) {
                MaterialTheme.colorScheme.primary  // 云端：主题色
            } else {
                MaterialTheme.colorScheme.secondary  // 本地：次要色
            }
        )
    }
}

/**
 * RefreshButton - 刷新按钮
 * 
 * @param viewModel ViewModel
 * @param uiState UI 状态
 */
@Composable
private fun RefreshButton(
    viewModel: ListViewModel,
    uiState: ListUiState
) {
    IconButton(
        onClick = { viewModel.refreshSeriesList() },
        enabled = !uiState.isLoading  // 加载中时禁用
    ) {
        if (uiState.isLoading) {
            // 加载中：显示进度指示器
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            // 空闲：显示刷新图标
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "刷新"
            )
        }
    }
}

/**
 * DialogContent - 所有对话框
 */
@Composable
private fun DialogContent(
    viewModel: ListViewModel,
    uiState: ListUiState
) {
    uiState.dialogType?.let { dialogType ->
        val series = uiState.selectedSeries ?: return@let
        
        when (dialogType) {
            DialogType.CACHE_ALL, DialogType.DELETE_ALL_CACHE, DialogType.DELETE_SERIES -> {
                AlertDialog(
                    onDismissRequest = { viewModel.hideDialog() },
                    title = {
                        Text(
                            text = when (dialogType) {
                                DialogType.CACHE_ALL -> "缓存全部"
                                DialogType.DELETE_ALL_CACHE -> "删除全部缓存"
                                DialogType.DELETE_SERIES -> "删除剧集"
                                else -> ""
                            }
                        )
                    },
                    text = {
                        Text(
                            text = when (dialogType) {
                                DialogType.CACHE_ALL -> "确定要下载「${series.name}」的全部剧集吗？"
                                DialogType.DELETE_ALL_CACHE -> "确定要删除「${series.name}」的全部本地缓存吗？"
                                DialogType.DELETE_SERIES -> "确定要删除「${series.name}」吗？\n\n此操作将删除：\n• 列表数据\n• 剧集详情\n• 所有下载的视频\n\n此操作不可撤销！"
                                else -> ""
                            }
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                when (dialogType) {
                                    DialogType.CACHE_ALL -> viewModel.confirmCacheAll()
                                    DialogType.DELETE_ALL_CACHE -> viewModel.confirmDeleteAllCache()
                                    DialogType.DELETE_SERIES -> viewModel.confirmDeleteSeries()
                                    else -> {}
                                }
                            }
                        ) {
                            Text(
                                text = "确定",
                                color = if (dialogType == DialogType.DELETE_SERIES) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.hideDialog() }) {
                            Text("取消")
                        }
                    }
                )
            }
            
            DialogType.SET_TAG -> {
                // 标签选择对话框（多选）
                val customTags = AppSettings.customTags
                AlertDialog(
                    onDismissRequest = { viewModel.hideDialog() },
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
                                            .clickable { viewModel.toggleSeriesTag(tag, !isSelected) }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { viewModel.toggleSeriesTag(tag, it) }
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
                        TextButton(onClick = { viewModel.hideDialog() }) {
                            Text("完成")
                        }
                    },
                    dismissButton = {
                        if (series.customTags.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearSeriesTags() }) {
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
 * TagFilterBar - 标签筛选栏（紧凑胶囊式）
 *
 * 使用 FilterChip 实现，更紧凑、更现代
 */
@Composable
private fun TagFilterBar(
    allTags: List<String>,
    currentPage: Int,
    onTagSelected: (String) -> Unit
) {
    val customTags = AppSettings.customTags
    val lazyListState = rememberLazyListState()
    rememberCoroutineScope()
    
    // 自动滚动到选中项
    LaunchedEffect(currentPage) {
        if (currentPage in allTags.indices) {
            lazyListState.animateScrollToItem(currentPage)
        }
    }

    LazyRow(
        state = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(allTags) { index, tag ->
            val isSelected = index == currentPage
            
            // 根据标签类型获取图标
            val icon = when {
                tag == "全部" -> null
                tag == "正在看" -> Icons.Default.PlayCircle
                tag == "已缓存" -> Icons.Default.Download
                tag == "未看" -> Icons.Default.VisibilityOff
                tag == "已看" -> Icons.Default.CheckCircle
                tag in customTags -> Icons.AutoMirrored.Filled.Label
                else -> Icons.Default.Folder  // 文件夹标签
            }
            
            FilterChip(
                selected = isSelected,
                onClick = { onTagSelected(tag) },
                label = {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,  // 更小的字体
                        fontSize = 12.sp  // 明确设置字号
                    )
                },
                leadingIcon = if (icon != null) {
                    {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)  // 更小的图标
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp
                ),
                modifier = Modifier.height(28.dp)  // 紧凑高度
            )
        }
    }
}

/**
 * SeriesGrid - 剧集网格列表
 *
 * 一行三个的网格布局，支持长按显示菜单
 * 
 * @param viewModel 列表 ViewModel（用于处理业务逻辑）
 * @param uiState UI 状态（包含剧集列表、排序等信息）
 * @param onSeriesClick 剧集点击回调（点击卡片时触发）
 */
@Composable
private fun SeriesGrid(
    viewModel: ListViewModel,  // 列表 ViewModel
    uiState: ListUiState,  // UI 状态
    onSeriesClick: (SeriesLocalData) -> Unit  // 剧集点击回调
) {
    // 网格状态（用于控制滚动位置）
    val gridState = rememberLazyGridState()

    // 监听排序变化，滚动到顶部
    // 当排序类型或升降序发生变化时，自动滚动到列表顶部
    LaunchedEffect(uiState.sortType, uiState.sortAscending) {
        if (uiState.seriesList.isNotEmpty()) {  // 仅当列表非空时滚动
            gridState.scrollToItem(0)  // 滚动到第一项
        }
    }

    // 垂直滚动的网格布局
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),  // 固定一行三个（3列）
        state = gridState,  // 网格滚动状态
        contentPadding = PaddingValues(8.dp),  // 内容边距（四周 8dp）
        horizontalArrangement = Arrangement.spacedBy(8.dp),  // 列之间的间距
        verticalArrangement = Arrangement.spacedBy(8.dp),  // 行之间的间距
        modifier = Modifier.fillMaxSize()  // 填充整个可用空间
    ) {
        // 遍历剧集列表，生成网格项
        items(
            items = uiState.seriesList,  // 数据源（剧集列表）
            key = { it.id },  // 唯一标识（用于优化重组性能）
            contentType = { "series_card" }  // 内容类型（用于优化回收）
        ) { series ->  // 每个剧集数据
            // 单个剧集卡片
            SeriesGridItem(
                series = series,  // 剧集数据
                viewModel = viewModel,  // ViewModel
                uiState = uiState,  // UI 状态
                onSeriesClick = onSeriesClick,  // 点击回调
                modifier = Modifier.animateItem()  // 添加动画效果（增删时）
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
    viewModel: ListViewModel,
    uiState: ListUiState,
    onSeriesClick: (SeriesLocalData) -> Unit,
    modifier: Modifier = Modifier
) {
    // 图片 URL 状态
    var imageUrl by remember { mutableStateOf<String?>(null) }

    // 获取图片 URL
    LaunchedEffect(series.id) {
        viewModel.getSeriesImageUrl(series.id) { url ->
            imageUrl = url
        }
    }

    val isMenuExpanded = uiState.selectedSeries?.id == series.id

    Box(
        modifier = modifier  // 应用从外部传入的 modifier（包含 animateItem）
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onSeriesClick(series) },
                    onLongClick = { viewModel.showSeriesMenu(series) }
                ),
            shape = RoundedCornerShape(16.dp),  // 大圆角，更现代
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface  // 使用 surface 而不是 surfaceVariant，更统一
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp  // 扁平化，无阴影
            )
        ) {
        Column {
            // 图片区域
                Box(
                    modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.67f)  // 2:3 的宽高比（海报比例）
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))  // 大圆角
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (imageUrl != null) {
                    // 显示图片（优化：加载缩略图）
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(300)  // 300ms 淡入动画
                            .size(400, 600)  // 缩略图尺寸（宽400px，高600px，足够清晰）
                            .memoryCachePolicy(CachePolicy.ENABLED)  // 启用内存缓存
                            .diskCachePolicy(CachePolicy.ENABLED)    // 启用磁盘缓存
                            .build(),
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
            onDismissRequest = { viewModel.hideSeriesMenu() }
        ) {
            // 标记已看/未看
            if (series.played) {
                DropdownMenuItem(
                    text = { Text("标记为未看") },
                    onClick = { viewModel.markSeriesAsWatched(false) },
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
                    onClick = { viewModel.markSeriesAsWatched(true) },
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
                onClick = { viewModel.showCacheAllDialog() },
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
                onClick = { viewModel.showDeleteAllCacheDialog() },
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
                onClick = { viewModel.showTagDialog() },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = null
                    )
                }
            )

            // 分割线
            HorizontalDivider()

            // 删除剧集（危险操作，红色显示）
            DropdownMenuItem(
                text = {
                    Text(
                        text = "删除剧集",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = { viewModel.showDeleteSeriesDialog() },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
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
