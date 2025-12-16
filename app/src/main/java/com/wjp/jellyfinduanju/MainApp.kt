package com.wjp.jellyfinduanju

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.wjp.jellyfinduanju.di.entrypoint.getAuthRepository
import com.wjp.jellyfinduanju.di.entrypoint.getSeriesDataStore
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wjp.jellyfinduanju.navigation.BottomNavItem
import com.wjp.jellyfinduanju.ui.screens.download.DownloadScreen
import com.wjp.jellyfinduanju.ui.screens.list.ListScreen
import com.wjp.jellyfinduanju.data.local.SeriesDataStore
import com.wjp.jellyfinduanju.data.repository.AuthRepository
import com.wjp.jellyfinduanju.ui.screens.login.LoginScreen
import com.wjp.jellyfinduanju.ui.screens.detail.DetailScreen
import com.wjp.jellyfinduanju.ui.screens.settings.SettingsScreen
import com.wjp.jellyfinduanju.ui.screens.settings.cache.CacheSettingsScreen
import com.wjp.jellyfinduanju.ui.screens.settings.tag.TagManageScreen
import com.wjp.jellyfinduanju.ui.screens.settings.folder.LocalFolderManageScreen
import com.wjp.jellyfinduanju.ui.screens.player.PlayerScreen

// ==================== 导航路由定义 ====================

/**
 * Routes - 导航路由常量
 * 
 * 集中管理所有非底部导航的路由路径
 * 底部导航的路由在 BottomNavItem 中定义
 */
private object Routes {
    // 设置子页面路由
    const val CACHE_SETTINGS = "cache_settings"
    const val TAG_MANAGE = "tag_manage"
    const val LOCAL_FOLDER_MANAGE = "local_folder_manage"
    
    // 带参数的路由（模板格式，{} 内为参数占位符）
    const val SERIES_DETAIL = "series_detail/{seriesId}"
    const val PLAYER = "player/{seriesId}/{episodeId}"
    
    /**
     * 构建剧集详情页路由（填充参数）
     * @param seriesId 剧集ID
     */
    fun seriesDetail(seriesId: String) = "series_detail/$seriesId"
    
    /**
     * 构建播放页路由（填充参数）
     * @param seriesId 剧集ID
     * @param episodeId 集数ID
     */
    fun player(seriesId: String, episodeId: String) = "player/$seriesId/$episodeId"
}

// ==================== 主入口 ====================

/**
 * MainApp - 应用的主 Composable 函数
 * 
 * 负责：
 * 1. 检查登录状态
 * 2. 未登录 -> 显示登录页
 * 3. 已登录 -> 显示主页（带底部导航）
 */
@Composable
fun MainApp() {
    // 通过 EntryPoint 获取 AuthRepository，使用 remember 缓存
    // remember 确保在重组时不会重复创建实例
    val authRepository = remember { getAuthRepository() }
    
    // collectAsState：将 Flow 转换为 Compose State，自动触发重组
    // initial = null：指定初始值，在 Flow 发出第一个值之前使用
    // 
    // 工作流程：
    // 1. 首次渲染时 userSession = null（初始值）
    // 2. Flow 异步从数据库读取用户会话
    // 3. 读取完成后更新 userSession，触发重组
    // 
    // userSession 为 null = 未登录，有值 = 已登录
    val userSession by authRepository.userSession.collectAsState(initial = null)
    
    // 根据登录状态显示不同页面
    val isLoggedIn = userSession != null
    
    if (isLoggedIn) {
        // 已登录，显示主页面
        MainContent()
    } else {
        // 未登录，显示登录页面
        LoginContent(
            onLoginSuccess = {
                // 登录成功后，userSession 会自动更新
                // 由于我们在收集 userSession，界面会自动切换到 MainContent
            }
        )
    }
}

// ==================== 登录页面 ====================

/**
 * LoginContent - 登录页面容器
 * 
 * 用户未登录时显示此页面
 * 登录成功后会自动切换到 MainContent
 */
@Composable
private fun LoginContent(
    onLoginSuccess: () -> Unit
) {
    val viewModel = hiltViewModel<com.wjp.jellyfinduanju.ui.screens.login.LoginViewModel>()
    
    LoginScreen(
        viewModel = viewModel,
        onLoginSuccess = onLoginSuccess
    )
}

// ==================== 主页面 ====================

/**
 * MainContent - 主页面（带底部导航）
 * 
 * 职责：
 * - 管理 Scaffold 布局（底部导航栏）
 * - 根据当前路由决定是否显示底部栏
 * - 控制页面内边距（播放器需要全屏）
 */
@Composable
private fun MainContent() {
    // rememberNavController：创建导航控制器，在重组时保持实例
    val navController = rememberNavController()
    
    // 监听当前导航状态，用于控制 UI 显示
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // 判断是否显示底部导航栏
    // 详情页、播放页、设置子页面需要隐藏底部栏，获得更大显示空间
    val shouldShowBottomBar = when {
        currentRoute == null -> true
        currentRoute == Routes.CACHE_SETTINGS -> false
        currentRoute == Routes.TAG_MANAGE -> false
        currentRoute == Routes.LOCAL_FOLDER_MANAGE -> false
        currentRoute.startsWith("series_detail/") -> false
        currentRoute.startsWith("player/") -> false
        else -> true
    }
    
    // 判断是否应用内边距
    // 播放器页面需要全屏显示，不应用 Scaffold 的 padding
    val shouldApplyPadding = !currentRoute.orEmpty().startsWith("player/")

    // Scaffold：Material Design 的标准页面布局容器
    // 提供顶栏、底栏、浮动按钮等标准位置
    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                MainBottomNavigationBar(navController = navController)
            }
        },
        // 禁用 Scaffold 的自动系统栏内边距，配合 MainActivity 的边到边模式
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        // innerPadding：Scaffold 自动计算的内边距（避开底栏）
        // 播放器页面不使用此 padding，实现全屏效果
        MainNavHost(
            navController = navController,
            modifier = if (shouldApplyPadding) Modifier.padding(innerPadding) else Modifier
        )
    }
}

// ==================== 底部导航栏 ====================

/**
 * MainBottomNavigationBar - 底部导航栏
 * 
 * Material Design 3 的底部导航组件
 * 用于在主要功能之间快速切换（列表、下载、设置）
 */
@Composable
private fun MainBottomNavigationBar(
    navController: NavHostController
) {
    val bottomNavItems = listOf(
        BottomNavItem.List,      // 剧集列表
        BottomNavItem.Download,  // 下载管理
        BottomNavItem.Settings   // 设置
    )
    
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp  // 扁平化设计，无阴影
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        bottomNavItems.forEach { item ->
            // hierarchy：检查当前目的地是否在导航层次结构中
            // 支持嵌套导航图的选中状态
            val isSelected = currentDestination?.hierarchy?.any { 
                it.route == item.route 
            } == true
            
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = item.icon, 
                        contentDescription = item.title
                    ) 
                },
                label = { Text(item.title) },
                selected = isSelected,
                onClick = {
                    navController.navigate(item.route) {
                        // popUpTo：清除返回栈到起始位置，避免堆栈过深
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true  // 保存页面状态
                        }
                        launchSingleTop = true  // 避免重复创建同一页面
                        restoreState = true     // 恢复之前保存的状态
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

// ==================== 导航 Host ====================

/**
 * MainNavHost - 主导航 Host
 * 
 * 职责：
 * - 配置所有页面的路由
 * - 管理页面跳转和参数传递
 * - 处理 ViewModel 注入
 */
@Composable
private fun MainNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // 获取剧集数据存储，用于在详情页和播放页获取数据
    // 使用 remember 缓存，避免每次重组都重新获取
    val seriesDataStore = remember { getSeriesDataStore() }
    
    // NavHost：导航容器，管理页面的显示和切换
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.List.route,  // 启动时显示列表页
        modifier = modifier
    ) {
        // ==================== 底部导航页面（主要功能） ====================
        
        // 列表页：显示所有剧集
        composable(BottomNavItem.List.route) {
            // hiltViewModel：自动注入 ViewModel 及其依赖
            val listViewModel = hiltViewModel<com.wjp.jellyfinduanju.ui.screens.list.ListViewModel>()
            ListScreen(
                viewModel = listViewModel,
                onSeriesClick = { series ->
                    navController.navigate(Routes.seriesDetail(series.id))
                }
            )
        }
        
        // 下载页：管理下载任务
        composable(BottomNavItem.Download.route) {
            val downloadViewModel = hiltViewModel<com.wjp.jellyfinduanju.ui.screens.download.DownloadViewModel>()
            DownloadScreen(
                viewModel = downloadViewModel,
                onNavigateToDetail = { seriesId ->
                    navController.navigate(Routes.seriesDetail(seriesId))
                }
            )
        }
        
        // 设置页：应用配置入口
        composable(BottomNavItem.Settings.route) {
            val settingsViewModel = hiltViewModel<com.wjp.jellyfinduanju.ui.screens.settings.SettingsViewModel>()
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateToCacheSettings = {
                    navController.navigate(Routes.CACHE_SETTINGS)
                },
                onNavigateToTagManage = {
                    navController.navigate(Routes.TAG_MANAGE)
                },
                onNavigateToLocalFolderManage = {
                    navController.navigate(Routes.LOCAL_FOLDER_MANAGE)
                }
            )
        }
        
        // ==================== 子页面（从设置页进入） ====================
        
        // 缓存设置：管理视频缓存
        composable(Routes.CACHE_SETTINGS) {
            CacheSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // 标签管理：管理剧集分类标签
        composable(Routes.TAG_MANAGE) {
            TagManageScreen(
                onBack = { navController.popBackStack() },
                onDeleteTag = { tag ->
                    // 删除标签的回调（可选，Screen 内部有默认处理）
                }
            )
        }
        
        // 本地文件夹管理：管理本地媒体文件路径
        composable(Routes.LOCAL_FOLDER_MANAGE) {
            LocalFolderManageScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // 剧集详情页：显示剧集信息和集数列表
        // 路由参数：seriesId（剧集ID）
        composable(
            route = Routes.SERIES_DETAIL,  // "series_detail/{seriesId}"
            arguments = listOf(
                navArgument("seriesId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // 从导航参数中获取 seriesId
            val seriesId = backStackEntry.arguments?.getString("seriesId") ?: ""
            
            // 从本地数据存储获取剧集信息
            val series = seriesDataStore.getSeries(seriesId)
            
            if (series != null) {
                val detailViewModel = hiltViewModel<com.wjp.jellyfinduanju.ui.screens.detail.DetailViewModel>()
                DetailScreen(
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() },
                    onEpisodeClick = { episode ->
                        navController.navigate(Routes.player(series.id, episode.id))
                    }
                )
            } else {
                // 数据不存在时自动返回（避免显示空白页）
                // LaunchedEffect：在 Composable 生命周期中执行副作用
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
        
        // 播放页面：全屏视频播放器
        // 路由参数：seriesId（剧集ID）、episodeId（集数ID）
        composable(
            route = Routes.PLAYER,  // "player/{seriesId}/{episodeId}"
            arguments = listOf(
                navArgument("seriesId") { type = NavType.StringType },
                navArgument("episodeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId") ?: ""
            val series = seriesDataStore.getSeries(seriesId)
            
            if (series != null) {
                val playerViewModel = hiltViewModel<com.wjp.jellyfinduanju.ui.screens.player.PlayerViewModel>()
                PlayerScreen(
                    viewModel = playerViewModel,
                    onBack = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
    }
}
