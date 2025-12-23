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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wjp.jellyfinduanju.data.model.SeriesLocalData
import com.wjp.jellyfinduanju.data.settings.AppSettings
import com.wjp.jellyfinduanju.ui.components.badge.IconBadge
import com.wjp.jellyfinduanju.ui.components.badge.OverlayBadges
import com.wjp.jellyfinduanju.ui.components.badge.TextBadge
import com.wjp.jellyfinduanju.ui.components.dialog.ConfirmDialog
import com.wjp.jellyfinduanju.ui.components.dialog.DialogConfig
import com.wjp.jellyfinduanju.ui.components.dialog.TagSelectionDialog
import com.wjp.jellyfinduanju.ui.components.image.CachedAsyncImage
import com.wjp.jellyfinduanju.ui.components.menu.DataDrivenMenu
import com.wjp.jellyfinduanju.ui.components.menu.MenuItem
import com.wjp.jellyfinduanju.ui.components.menu.SortMenuItem
import com.wjp.jellyfinduanju.ui.components.menu.SortOption
import com.wjp.jellyfinduanju.ui.components.topbar.SearchableTopBar
import kotlinx.coroutines.launch

// ==================== 主入口 ====================

/**
 * ListScreen - 剧集列表页面
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

// ==================== 生命周期与副作用 ====================

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

// ==================== 顶部栏组件 ====================

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

// ==================== 对话框 ====================

/**
 * DialogContent - 对话框统一入口
 * 
 * 根据 dialogType 路由到不同的对话框
 */
@Composable
private fun DialogContent(
    viewModel: ListViewModel,
    uiState: ListUiState
) {
    val dialogType = uiState.dialogType ?: return
    val series = uiState.selectedSeries ?: return
    
    // 根据类型显示不同的对话框
    when (dialogType) {
        DialogType.SET_TAG -> {
            ShowTagSelectionDialog(series, viewModel)
        }
        else -> {
            ShowConfirmationDialog(dialogType, series, viewModel)
        }
    }
}

/**
 * ShowTagSelectionDialog - 显示标签选择对话框
 * 
 * 用于为剧集设置自定义标签
 */
@Composable
private fun ShowTagSelectionDialog(
    series: SeriesLocalData,
    viewModel: ListViewModel
) {
    TagSelectionDialog(
        series = series,  // 剧集数据
        onTagToggle = { tag, selected -> viewModel.toggleSeriesTag(tag, selected) },  // 标签切换回调
        onClearAllTags = { viewModel.clearSeriesTags() },  // 清除所有标签回调
        onDismiss = { viewModel.hideDialog() }  // 关闭对话框回调
    )
}

/**
 * ShowConfirmationDialog - 显示确认类对话框
 * 
 * 用于缓存、删除等需要二次确认的操作
 */
@Composable
private fun ShowConfirmationDialog(
    dialogType: DialogType,
    series: SeriesLocalData,
    viewModel: ListViewModel
) {
    val errorColor = MaterialTheme.colorScheme.error
    val config = getConfirmDialogConfig(dialogType, series, viewModel, errorColor) ?: return
    
    ConfirmDialog(
        config = config,
        onDismiss = { viewModel.hideDialog() }
    )
}

/**
 * getConfirmDialogConfig - 获取确认对话框配置
 * 
 * 根据 dialogType 返回对应的对话框配置
 * 
 * @return DialogConfig 配置对象，如果类型不匹配返回 null
 */
private fun getConfirmDialogConfig(
    dialogType: DialogType,
    series: SeriesLocalData,
    viewModel: ListViewModel,
    errorColor: Color
): DialogConfig? = when (dialogType) {
    DialogType.CACHE_ALL -> DialogConfig(
        title = "缓存全部",
        message = "确定要下载「${series.name}」的全部剧集吗？",
        onConfirm = { viewModel.confirmCacheAll() }
    )
    DialogType.DELETE_ALL_CACHE -> DialogConfig(
        title = "删除全部缓存",
        message = "确定要删除「${series.name}」的全部本地缓存吗？",
        onConfirm = { viewModel.confirmDeleteAllCache() }
    )
    DialogType.DELETE_SERIES -> DialogConfig(
        title = "删除剧集",
        message = "确定要删除「${series.name}」吗？\n\n此操作将删除：\n• 列表数据\n• 剧集详情\n• 所有下载的视频\n\n此操作不可撤销！",
        confirmColor = errorColor,  // 危险操作，红色按钮
        onConfirm = { viewModel.confirmDeleteSeries() }
    )
    else -> null  // 非确认类对话框，返回 null
}

// ==================== 标签筛选栏 ====================

/**
 * TagFilterBar - 标签筛选栏（紧凑胶囊式）
 *
 * 水平滚动的标签筛选条，使用 FilterChip 实现紧凑布局
 * 
 * @param allTags 所有标签列表
 * @param currentPage 当前选中的标签索引
 * @param onTagSelected 标签选择回调
 */
@Composable
private fun TagFilterBar(
    allTags: List<String>,  // 标签列表
    currentPage: Int,  // 当前选中索引
    onTagSelected: (String) -> Unit  // 选择回调
) {
    // LazyRow 滚动状态（用于自动滚动到选中项）
    val lazyListState = rememberLazyListState()
    
    // 监听 currentPage 变化，自动滚动到选中的标签
    LaunchedEffect(currentPage) {
        if (currentPage in allTags.indices) {  // 确保索引合法
            lazyListState.animateScrollToItem(currentPage)  // 动画滚动
        }
    }

    // 水平滚动的标签列表
    LazyRow(
        state = lazyListState,  // 滚动状态
        modifier = Modifier
            .fillMaxWidth()  // 填充宽度
            .background(MaterialTheme.colorScheme.surface),  // 背景色
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),  // 内边距
        horizontalArrangement = Arrangement.spacedBy(6.dp)  // 标签间距
    ) {
        // 遍历所有标签，生成 FilterChip
        itemsIndexed(allTags) { index, tag ->
            val isSelected = index == currentPage  // 判断是否选中
            
            // 单个标签 Chip
            TagFilterChip(
                tag = tag,  // 标签名称
                isSelected = isSelected,  // 是否选中
                onClick = { onTagSelected(tag) }  // 点击回调
            )
        }
    }
}

/**
 * TagFilterChip - 单个标签过滤 Chip
 * 
 * 显示单个标签的 FilterChip，包含图标和文本
 * 
 * @param tag 标签名称
 * @param isSelected 是否选中
 * @param onClick 点击回调
 */
@Composable
private fun TagFilterChip(
    tag: String,  // 标签名称
    isSelected: Boolean,  // 是否选中
    onClick: () -> Unit  // 点击回调
) {
    // 根据标签类型获取对应图标
    val icon = getTagIcon(tag)
    
    FilterChip(
        selected = isSelected,  // 选中状态
        onClick = onClick,  // 点击回调
        label = {
            // 标签文本
            Text(
                text = tag,  // 标签名称
                style = MaterialTheme.typography.labelSmall,  // 小号标签字体
                fontSize = 12.sp  // 明确字号（紧凑）
            )
        },
        leadingIcon = if (icon != null) {
            // 如果有图标，显示图标
            {
                Icon(
                    imageVector = icon,  // 图标内容
                    contentDescription = null,  // 装饰性图标
                    modifier = Modifier.size(14.dp)  // 小尺寸图标
                )
            }
        } else null,  // 无图标时不显示
        colors = FilterChipDefaults.filterChipColors(
            // 选中状态颜色
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,  // 选中背景
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,  // 选中文本
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer  // 选中图标
        ),
        border = FilterChipDefaults.filterChipBorder( // 外边框样式
            enabled = true,  // 启用状态
            selected = isSelected,  // 选中状态
            borderColor = if (isSelected) {  // 边框颜色
                MaterialTheme.colorScheme.primary  // 选中：主题色
            } else {
                MaterialTheme.colorScheme.outline  // 未选中：轮廓色
            },
            selectedBorderColor = MaterialTheme.colorScheme.primary,  // 选中边框颜色
            borderWidth = 1.dp,  // 边框宽度
            selectedBorderWidth = 1.dp  // 选中边框宽度
        ),
        modifier = Modifier.height(28.dp)  // 紧凑高度
    )
}

/**
 * getTagIcon - 获取标签对应的图标
 * 
 * 根据标签类型返回对应的图标，用于标签过滤栏显示
 * 
 * @param tag 标签名称
 * @return 对应的图标，如果没有图标则返回 null
 */
private fun getTagIcon(tag: String): ImageVector? {
    val customTags = AppSettings.customTags  // 获取自定义标签列表
    
    return when {
        tag == "全部" -> null  // 全部标签：无图标
        tag == "正在看" -> Icons.Default.PlayCircle  // 正在看：播放图标
        tag == "已缓存" -> Icons.Default.Download  // 已缓存：下载图标
        tag == "未看" -> Icons.Default.VisibilityOff  // 未看：不可见图标
        tag == "已看" -> Icons.Default.CheckCircle  // 已看：完成图标
        tag in customTags -> Icons.AutoMirrored.Filled.Label  // 自定义标签：标签图标
        else -> Icons.Default.Folder  // 其他（文件夹标签）：文件夹图标
    }
}

// ==================== 剧集网格 ====================

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
 * 显示单个剧集的卡片，包含封面图、状态角标、集数信息和标题
 * 支持点击进入详情、长按显示操作菜单
 * 
 * @param series 剧集数据（包含名称、封面、观看状态等信息）
 * @param viewModel 列表 ViewModel（用于处理业务逻辑）
 * @param uiState UI 状态（用于判断菜单是否展开）
 * @param onSeriesClick 剧集点击回调（点击卡片时触发）
 * @param modifier 布局修饰符（包含 animateItem 动画效果）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeriesGridItem(
    series: SeriesLocalData,  // 剧集数据
    viewModel: ListViewModel,  // 列表 ViewModel
    uiState: ListUiState,  // UI 状态
    onSeriesClick: (SeriesLocalData) -> Unit,  // 点击回调
    modifier: Modifier = Modifier  // 布局修饰符
) {
    // 图片 URL 状态（异步加载）
    var imageUrl by remember { mutableStateOf<String?>(null) }

    // 异步获取图片 URL
    LaunchedEffect(series.id) {
        viewModel.getSeriesImageUrl(series.id) { url ->
            imageUrl = url  // 获取成功后更新状态
        }
    }
    
    // 判断当前剧集的菜单是否展开（对话框弹出时菜单自动隐藏）
    val isMenuExpanded = uiState.selectedSeries?.id == series.id && uiState.dialogType == null

    Box(
        modifier = modifier  // 应用从外部传入的 modifier（包含 animateItem 动画）
    ) {
        // 剧集卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()  // 填充宽度
                .combinedClickable(  // 支持点击和长按
                    onClick = { onSeriesClick(series) },  // 点击：进入详情
                    onLongClick = { viewModel.showSeriesMenu(series) }  // 长按：显示菜单
                ),
            shape = RoundedCornerShape(16.dp),  // 大圆角
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface  // 卡片背景色
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp  // 扁平化设计，无阴影
            )
        ) {
            Column {
                // 图片区域（使用 OverlayBadges 统一管理角标位置）
                OverlayBadges(
                    modifier = Modifier
                        .fillMaxWidth()  // 填充宽度
                        .aspectRatio(0.67f)  // 2:3 的宽高比（标准海报比例） 2/3 = 0.66
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))  // 顶部圆角
                        .background(MaterialTheme.colorScheme.surfaceVariant),  // 占位背景色
                    backgroundContent = {
                        // 背景：剧集封面图
                        CachedAsyncImage(
                            imageUrl = imageUrl,  // 图片 URL
                            contentDescription = series.name,  // 无障碍描述
                            modifier = Modifier.fillMaxSize()  // 填充整个空间
                        )
                    },
                    topStart = { StatusBadge(series) },  // 左上角：状态角标（已看完/正在看/已删除）
                    topEnd = { EpisodeSeasonBadge(series) }  // 右上角：集数季数角标
                )

                // 标题区域（剧集名称）
                SeriesTitle(series.name)
            }
        }  // Card 闭合

        // 长按菜单（数据驱动方式生成菜单项）
        DataDrivenMenu(
            expanded = isMenuExpanded,  // 菜单是否展开
            onDismissRequest = { viewModel.hideSeriesMenu() },  // 关闭菜单回调
            items = buildSeriesMenuItems(  // 构建菜单项列表
                series = series,  // 剧集数据
                errorColor = MaterialTheme.colorScheme.error,  // 危险操作颜色（红色）
                viewModel = viewModel  // ViewModel
            )
        )
    }  // Box 闭合
}

/**
 * StatusBadge - 状态角标（左上角）
 * 
 * 根据剧集状态显示不同的角标：
 * - 已删除（红色云端图标）
 * - 已看完（绿色✓图标）
 * - 正在看（橙色播放图标）
 * - 未看过（不显示）
 */
@Composable
private fun StatusBadge(series: SeriesLocalData) {
    // 提前处理：已从服务器删除（红色，优先级最高）
    if (series.isDeletedFromServer) {
        IconBadge(
            icon = Icons.Default.CloudOff,  // 云端关闭图标
            tint = Color(0xFFF44336)  // 红色
        )
        return  // 提前返回，减少嵌套
    }
    
    // 判断观看进度：
    // 1. played = true → 已看完（绿色）
    // 2. played = false && lastWatchTime > 0 → 正在看（橙色）
    // 3. played = false && lastWatchTime = 0 → 没看过（不显示图标）
    val hasProgress = series.played || series.lastWatchTime > 0
    if (!hasProgress) return  // 无进度，不显示，提前返回
    
    // 显示观看进度图标（扁平化）
    IconBadge(
        icon = if (series.played) 
            Icons.Default.CheckCircle  // 已看完：✓
        else 
            Icons.Default.PlayCircle,  // 正在看：播放图标
        tint = if (series.played) 
            Color(0xFF4CAF50)  // 绿色
        else 
            Color(0xFFFF9800)  // 橙色
    )
}

/**
 * EpisodeSeasonBadge - 集数季数角标（右上角）
 * 
 * 显示剧集和季数信息，格式：12集2季 或 12集 或 2季
 */
@Composable
private fun EpisodeSeasonBadge(series: SeriesLocalData) {
    val episodeCount = series.episodeCount  // 集数
    val seasonCount = series.seasonCount  // 季数

    if (episodeCount == null && seasonCount == null){
        return
    }

    // 格式：12集2季 或 12集 或 2季
    val text = buildString {
        episodeCount?.let { if (it > 0) append("${it}集") }
        seasonCount?.let { if (it > 0) append("${it}季") }
    }
    TextBadge(text = text)  // 显示文本
}

/**
 * SeriesTitle - 剧集标题显示组件
 *
 * @param seriesName 剧集名称（完整标题）
 */
@Composable
private fun SeriesTitle(seriesName: String) {
    // 外层容器：固定高度，内容居中
    Box(
        modifier = Modifier
            .fillMaxWidth()  // 填充卡片宽度
            .height(40.dp)  // 固定高度（保证所有卡片一致）
            .padding(horizontal = 6.dp, vertical = 4.dp),  // 内边距（留白）
        contentAlignment = Alignment.Center  // 内容垂直和水平居中
    ) {
        // 剧集标题文本
        Text(
            text = seriesName,  // 剧集名称
            style = MaterialTheme.typography.bodySmall,  // 小号正文字体
            fontWeight = FontWeight.Medium,  // 中等字重（比常规粗一点，增强可读性）
            maxLines = 2,  // 最多显示2行
            overflow = TextOverflow.Ellipsis,  // 超出部分显示省略号（...）
            textAlign = TextAlign.Center,  // 文本居中对齐
            color = MaterialTheme.colorScheme.onSurface  // 文本颜色
        )
    }
}

/**
 * buildSeriesMenuItems - 构建剧集长按菜单项（数据驱动）
 * 
 * 直接使用 MenuItem 配置菜单项，配置数据与 UI 分离
 */
private fun buildSeriesMenuItems(series: SeriesLocalData, errorColor: Color, viewModel: ListViewModel) = listOf(
    // 标记已看/未看（根据状态动态切换）
    if (series.played) {
        MenuItem(text = "标记为未看", icon = Icons.Default.VisibilityOff, onClick = { 
            viewModel.markSeriesAsWatched(false)  // 标记为未看
            viewModel.hideSeriesMenu()  // 关闭菜单
        })
    } else {
        MenuItem(text = "标记为已看", icon = Icons.Default.Visibility, onClick = { 
            viewModel.markSeriesAsWatched(true)  // 标记为已看
            viewModel.hideSeriesMenu()  // 关闭菜单
        })
    },
    // 缓存全部（弹对话框，菜单会在对话框弹出时自然消失）
    MenuItem(text = "缓存全部", icon = Icons.Default.Download, onClick = { viewModel.showCacheAllDialog() }),
    // 删除全部缓存（弹对话框，菜单会在对话框弹出时自然消失）
    MenuItem(text = "删除全部缓存", icon = Icons.Default.Delete, onClick = { viewModel.showDeleteAllCacheDialog() }),
    // 设置标签（弹对话框，菜单会在对话框弹出时自然消失）
    MenuItem(text = "设置标签", icon = Icons.AutoMirrored.Filled.Label, onClick = { viewModel.showTagDialog() }),
    // 分割线
    MenuItem.divider(),
    // 删除剧集（弹对话框，菜单会在对话框弹出时自然消失）
    MenuItem(text = "删除剧集", icon = Icons.Default.Delete, textColor = errorColor, iconTint = errorColor, onClick = { viewModel.showDeleteSeriesDialog() })
)

// ==================== 状态占位组件 ====================

/**
 * LoadingContent - 加载中占位内容
 */
@Composable
private fun LoadingContent() {
    // 外层容器：填充整个屏幕，内容居中
    Box(
        modifier = Modifier.fillMaxSize(),  // 填充父容器
        contentAlignment = Alignment.Center  // 内容居中对齐
    ) {
        // 垂直排列：进度指示器 + 提示文本
        Column(
            horizontalAlignment = Alignment.CenterHorizontally  // 子元素水平居中
        ) {
            // 圆形进度指示器（Material Design 风格）
            CircularProgressIndicator()
            
            // 间距
            Spacer(modifier = Modifier.height(16.dp))
            
            // 提示文本
            Text(
                text = "加载中...",  // 加载提示
                style = MaterialTheme.typography.bodyMedium,  // 中等字体
                color = MaterialTheme.colorScheme.onSurfaceVariant  // 文本颜色
            )
        }
    }
}

/**
 * EmptyContent - 空列表占位内容
 *
 * @param onRefreshClick 刷新按钮点击回调，通常调用 ViewModel 的刷新方法
 */
@Composable
private fun EmptyContent(
    onRefreshClick: () -> Unit  // 刷新按钮回调
) {
    // 外层容器：填充整个屏幕，内容居中
    Box(
        modifier = Modifier.fillMaxSize(),  // 填充父容器
        contentAlignment = Alignment.Center  // 内容居中对齐
    ) {
        // 垂直排列：图标 + 文本 + 按钮
        Column(
            horizontalAlignment = Alignment.CenterHorizontally  // 子元素水平居中
        ) {
            // 电影图标（视觉焦点）
            Icon(
                imageVector = Icons.Default.Movie,  // 电影图标
                contentDescription = null,  // 装饰性图标，无需描述
                modifier = Modifier.size(64.dp),  // 大尺寸图标（64x64dp）
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)  // 半透明灰色
            )
            
            // 间距
            Spacer(modifier = Modifier.height(16.dp))
            
            // 主标题：空状态提示
            Text(
                text = "暂无剧集",  // 简洁明了的状态描述
                style = MaterialTheme.typography.titleMedium,  // 中等标题字体
                color = MaterialTheme.colorScheme.onSurfaceVariant  // 文本颜色
            )
            
            // 间距
            Spacer(modifier = Modifier.height(8.dp))
            
            // 副标题：操作引导
            Text(
                text = "点击右上角刷新按钮获取剧集列表",  // 引导用户操作
                style = MaterialTheme.typography.bodyMedium,  // 正文字体
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)  // 更淡的灰色
            )
            
            // 间距
            Spacer(modifier = Modifier.height(24.dp))
            
            // 刷新按钮（行动号召，Call To Action）
            Button(
                onClick = onRefreshClick  // 点击触发刷新
            ) {
                // 按钮内容：图标 + 文本
                Icon(
                    imageVector = Icons.Default.Refresh,  // 刷新图标
                    contentDescription = null  // 按钮已有文本，图标为装饰
                )
                Spacer(modifier = Modifier.width(8.dp))  // 图标和文本间距
                Text("刷新")  // 按钮文本
            }
        }
    }
}
