package com.wjp.jellyfinduanju

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.wjp.jellyfinduanju.ui.screens.HistoryScreen
import com.wjp.jellyfinduanju.ui.screens.download.DownloadScreen
import com.wjp.jellyfinduanju.ui.screens.download.DownloadViewModel
import com.wjp.jellyfinduanju.ui.screens.download.DownloadViewModelFactory
import com.wjp.jellyfinduanju.ui.screens.list.ListScreen
import com.wjp.jellyfinduanju.ui.screens.list.ListViewModel
import com.wjp.jellyfinduanju.ui.screens.list.ListViewModelFactory
import com.wjp.jellyfinduanju.ui.screens.login.LoginScreen
import com.wjp.jellyfinduanju.ui.screens.login.LoginViewModel
import com.wjp.jellyfinduanju.ui.screens.login.LoginViewModelFactory
import com.wjp.jellyfinduanju.ui.screens.detail.DetailScreen
import com.wjp.jellyfinduanju.ui.screens.detail.DetailViewModel
import com.wjp.jellyfinduanju.ui.screens.detail.DetailViewModelFactory
import com.wjp.jellyfinduanju.ui.screens.settings.SettingsScreen
import com.wjp.jellyfinduanju.ui.screens.settings.SettingsViewModel
import com.wjp.jellyfinduanju.ui.screens.settings.SettingsViewModelFactory
import com.wjp.jellyfinduanju.ui.screens.settings.cache.CacheSettingsScreen
import com.wjp.jellyfinduanju.ui.screens.player.PlayerScreen
import com.wjp.jellyfinduanju.ui.screens.player.PlayerViewModel
import com.wjp.jellyfinduanju.ui.screens.player.PlayerViewModelFactory

// ==================== 导航路由定义 ====================

/**
 * Routes - 导航路由常量
 * 
 * 集中定义所有非底部导航的路由
 */
private object Routes {
    const val CACHE_SETTINGS = "cache_settings"
    const val SERIES_DETAIL = "series_detail/{seriesId}"
    const val PLAYER = "player/{seriesId}/{episodeId}"
    
    /**
     * 构建剧集详情页路由
     */
    fun seriesDetail(seriesId: String) = "series_detail/$seriesId"
    
    /**
     * 构建播放页路由
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
    // 获取 Application 实例中的 Repository
    val authRepository = JellyfinApp.instance.authRepository
    
    // 收集用户会话状态
    // 返回 null 表示未登录，返回 UserSession 表示已登录
    val userSession by authRepository.userSession.collectAsState(initial = null)
    
    // 使用一个简单的标志判断是否需要检查登录状态
    // 由于 collectAsState 的初始值是 null，我们需要区分"正在加载"和"确实未登录"
    @Suppress("UNUSED_VARIABLE")
    val serverConfig by authRepository.serverConfig.collectAsState(initial = null)
    
    // 判断是否已登录
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
 */
@Composable
private fun LoginContent(
    onLoginSuccess: () -> Unit
) {
    // 获取 Repository
    val authRepository = JellyfinApp.instance.authRepository
    
    // 使用 Factory 创建 ViewModel
    val viewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(authRepository)
    )
    
    LoginScreen(
        viewModel = viewModel,
        onLoginSuccess = onLoginSuccess
    )
}

// ==================== 主页面 ====================

/**
 * MainContent - 主页面（带底部导航）
 * 
 * 只有登录后才会显示
 */
@Composable
private fun MainContent() {
    // 创建导航控制器
    val navController = rememberNavController()
    
    // 获取当前导航状态，用于判断是否显示底部导航栏
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // 需要隐藏底部导航栏的路由列表
    // 注意：对于带参数的路由，需要检查路由模板
    val shouldShowBottomBar = when {
        currentRoute == null -> true
        currentRoute == Routes.CACHE_SETTINGS -> false
        currentRoute.startsWith("series_detail/") -> false
        currentRoute.startsWith("player/") -> false  // 播放页面也隐藏底部栏
        else -> true
    }

    Scaffold(
        bottomBar = {
            // 只在主页面显示底部导航栏
            if (shouldShowBottomBar) {
                MainBottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        MainNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

// ==================== 底部导航栏 ====================

/**
 * MainBottomNavigationBar - 底部导航栏
 */
@Composable
private fun MainBottomNavigationBar(
    navController: NavHostController
) {
    // 底部导航项列表
    val bottomNavItems = listOf(
        BottomNavItem.List,
        BottomNavItem.History,
        BottomNavItem.Download,
        BottomNavItem.Settings
    )
    
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = item.icon, 
                        contentDescription = item.title
                    ) 
                },
                label = { Text(item.title) },
                selected = currentDestination?.hierarchy?.any { 
                    it.route == item.route 
                } == true,
                onClick = {
                    navController.navigate(item.route) {
                        // 弹出到导航图的起始目的地，避免在底部导航之间创建大量堆栈
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // 避免在顶部创建同一目的地的多个副本
                        launchSingleTop = true
                        // 重新选择以前选择的项目时恢复其状态
                        restoreState = true
                    }
                }
            )
        }
    }
}

// ==================== 导航 Host ====================

/**
 * MainNavHost - 主导航 Host
 * 
 * 管理所有页面的导航
 */
@Composable
private fun MainNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // 获取 Repository
    val authRepository = JellyfinApp.instance.authRepository
    val seriesRepository = JellyfinApp.instance.seriesRepository
    
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.List.route,
        modifier = modifier
    ) {
        // ==================== 底部导航页面 ====================
        
        // 列表页
        composable(BottomNavItem.List.route) {
            val listViewModel: ListViewModel = viewModel(
                factory = ListViewModelFactory(seriesRepository)
            )
            ListScreen(
                viewModel = listViewModel,
                onSeriesClick = { series ->
                    // 导航到剧集详情页
                    navController.navigate(Routes.seriesDetail(series.id))
                }
            )
        }
        
        // 历史页
        composable(BottomNavItem.History.route) {
            HistoryScreen()
        }
        
        // 下载页
        composable(BottomNavItem.Download.route) {
            val downloadViewModel: DownloadViewModel = viewModel(
                factory = DownloadViewModelFactory(
                    JellyfinApp.instance.downloadDataStore,
                    JellyfinApp.instance.seriesDataStore,
                    JellyfinApp.instance.episodeDataStore
                )
            )
            DownloadScreen(viewModel = downloadViewModel)
        }
        
        // 设置页
        composable(BottomNavItem.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(authRepository)
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateToCacheSettings = {
                    navController.navigate(Routes.CACHE_SETTINGS)
                }
            )
        }
        
        // ==================== 子页面 ====================
        
        // 缓存设置页面
        composable(Routes.CACHE_SETTINGS) {
            CacheSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // 剧集详情页
        composable(
            route = Routes.SERIES_DETAIL,
            arguments = listOf(
                navArgument("seriesId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId") ?: ""
            
            // 从本地数据获取剧集信息
            val series = JellyfinApp.instance.seriesDataStore.getSeries(seriesId)
            
            if (series != null) {
                val detailViewModel: DetailViewModel = viewModel(
                    factory = DetailViewModelFactory(
                        detailRepository = JellyfinApp.instance.detailRepository,
                        seriesRepository = seriesRepository,
                        series = series
                    )
                )
                DetailScreen(
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() },
                    onEpisodeClick = { episode ->
                        // 导航到播放页面
                        navController.navigate(Routes.player(series.id, episode.id))
                    }
                )
            } else {
                // 剧集不存在，返回上一页
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
        
        // 播放页面
        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument("seriesId") { type = NavType.StringType },
                navArgument("episodeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId") ?: ""
            val episodeId = backStackEntry.arguments?.getString("episodeId") ?: ""
            
            // 从本地数据获取剧集信息
            val series = JellyfinApp.instance.seriesDataStore.getSeries(seriesId)
            
            if (series != null) {
                val playerViewModel: PlayerViewModel = viewModel(
                    factory = PlayerViewModelFactory(
                        detailRepository = JellyfinApp.instance.detailRepository,
                        series = series,
                        initialEpisodeId = episodeId
                    )
                )
                PlayerScreen(
                    viewModel = playerViewModel,
                    onBack = { navController.popBackStack() }
                )
            } else {
                // 剧集不存在，返回上一页
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
    }
}
