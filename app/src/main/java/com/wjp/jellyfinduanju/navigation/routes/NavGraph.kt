package com.wjp.jellyfinduanju.navigation.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.wjp.jellyfinduanju.data.local.SeriesDataStore
import com.wjp.jellyfinduanju.di.entrypoint.getSeriesDataStore
import com.wjp.jellyfinduanju.navigation.BottomNavItem
import com.wjp.jellyfinduanju.ui.screens.detail.DetailScreen
import com.wjp.jellyfinduanju.ui.screens.detail.DetailViewModel
import com.wjp.jellyfinduanju.ui.screens.download.DownloadScreen
import com.wjp.jellyfinduanju.ui.screens.download.DownloadViewModel
import com.wjp.jellyfinduanju.ui.screens.list.ListScreen
import com.wjp.jellyfinduanju.ui.screens.list.ListViewModel
import com.wjp.jellyfinduanju.ui.screens.player.PlayerScreen
import com.wjp.jellyfinduanju.ui.screens.player.PlayerViewModel
import com.wjp.jellyfinduanju.ui.screens.settings.SettingsScreen
import com.wjp.jellyfinduanju.ui.screens.settings.SettingsViewModel
import com.wjp.jellyfinduanju.ui.screens.settings.cache.CacheSettingsScreen
import com.wjp.jellyfinduanju.ui.screens.settings.folder.LocalFolderManageScreen
import com.wjp.jellyfinduanju.ui.screens.settings.tag.TagManageScreen

/**
 * AppNavGraph - 应用导航图
 * 
 * @param navController 导航控制器，负责页面跳转和返回操作
 * @param modifier 可选的 Modifier，用于调整布局
 * 
 * 说明：
 * - NavHost 是导航容器，管理所有页面的显示和切换
 * - startDestination 指定应用启动时显示的第一个页面
 * - 每个页面通过 NavGraphBuilder 扩展函数独立配置
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,  // 导航控制器，管理页面跳转
    modifier: Modifier = Modifier  // 布局修饰符，用于调整导航容器的样式
) {
    // 获取剧集数据存储，用于在详情页和播放页获取数据（已内置缓存）
    val seriesDataStore = getSeriesDataStore()
    
    // NavHost：导航容器，管理页面的显示和切换
    NavHost(
        navController = navController,  // 导航控制器
        startDestination = BottomNavItem.List.route,  // 启动时显示列表页
        modifier = modifier  // 布局修饰符
    ) {
        // ==================== 底部导航页面（主要功能） ====================
        listScreen(navController)
        downloadScreen(navController)
        settingsScreen(navController)
        
        // ==================== 设置子页面 ====================
        cacheSettingsScreen(navController)
        tagManageScreen(navController)
        localFolderManageScreen(navController)
        
        // ==================== 详情页和播放页 ====================
        seriesDetailScreen(navController, seriesDataStore)
        playerScreen(navController, seriesDataStore)
    }
}

// ==================== 底部导航页面配置 ====================

/**
 * 列表页路由配置
 * 
 * @param navController 导航控制器
 * 
 * 说明：
 * - hiltViewModel() 通过依赖注入自动创建 ViewModel
 * - onSeriesClick 点击剧集时触发，导航到详情页并传递 seriesId
 */
private fun NavGraphBuilder.listScreen(navController: NavHostController) {
    composable(BottomNavItem.List.route) {  // 列表页路由
        val listViewModel = hiltViewModel<ListViewModel>()  // 自动注入 ViewModel
        ListScreen(
            viewModel = listViewModel,  // 传递 ViewModel
            onSeriesClick = { series ->  // 点击剧集的回调
                navController.navigate(AppRoutes.seriesDetail(series.id))  // 跳转到详情页
            }
        )
    }
}

/**
 * 下载页路由配置
 * 
 * @param navController 导航控制器
 * 
 * 说明：
 * - onNavigateToDetail 点击下载项时触发，跳转到对应的剧集详情页
 */
private fun NavGraphBuilder.downloadScreen(navController: NavHostController) {
    composable(BottomNavItem.Download.route) {  // 下载页路由
        val downloadViewModel = hiltViewModel<DownloadViewModel>()  // 自动注入 ViewModel
        DownloadScreen(
            viewModel = downloadViewModel,  // 传递 ViewModel
            onNavigateToDetail = { seriesId ->  // 点击下载项的回调
                navController.navigate(AppRoutes.seriesDetail(seriesId))  // 跳转到详情页
            }
        )
    }
}

/**
 * 设置页路由配置
 * 
 * @param navController 导航控制器
 * 
 * 说明：
 * - onNavigateToCacheSettings 跳转到缓存设置页
 * - onNavigateToTagManage 跳转到标签管理页
 * - onNavigateToLocalFolderManage 跳转到本地文件夹管理页
 */
private fun NavGraphBuilder.settingsScreen(navController: NavHostController) {
    composable(BottomNavItem.Settings.route) {  // 设置页路由
        val settingsViewModel = hiltViewModel<SettingsViewModel>()  // 自动注入 ViewModel
        SettingsScreen(
            viewModel = settingsViewModel,  // 传递 ViewModel
            onNavigateToCacheSettings = {  // 跳转到缓存设置页
                navController.navigate(AppRoutes.CACHE_SETTINGS)
            },
            onNavigateToTagManage = {  // 跳转到标签管理页
                navController.navigate(AppRoutes.TAG_MANAGE)
            },
            onNavigateToLocalFolderManage = {  // 跳转到本地文件夹管理页
                navController.navigate(AppRoutes.LOCAL_FOLDER_MANAGE)
            }
        )
    }
}

// ==================== 设置子页面配置 ====================

/**
 * 缓存设置页路由配置
 * 
 * @param navController 导航控制器
 * 
 * 说明：
 * - onBack 返回上一页，调用 popBackStack() 弹出当前页面
 */
private fun NavGraphBuilder.cacheSettingsScreen(navController: NavHostController) {
    composable(AppRoutes.CACHE_SETTINGS) {  // 缓存设置页路由
        CacheSettingsScreen(
            onBack = { navController.popBackStack() }  // 返回上一页
        )
    }
}

/**
 * 标签管理页路由配置
 * 
 * @param navController 导航控制器
 * 
 * 说明：
 * - onBack 返回上一页
 * - onDeleteTag 删除标签时的回调（可选，Screen 内部有默认处理）
 */
private fun NavGraphBuilder.tagManageScreen(navController: NavHostController) {
    composable(AppRoutes.TAG_MANAGE) {  // 标签管理页路由
        TagManageScreen(
            onBack = { navController.popBackStack() },  // 返回上一页
        )
    }
}

/**
 * 本地文件夹管理页路由配置
 * 
 * @param navController 导航控制器
 * 
 * 说明：
 * - onBack 返回上一页
 */
private fun NavGraphBuilder.localFolderManageScreen(navController: NavHostController) {
    composable(AppRoutes.LOCAL_FOLDER_MANAGE) {  // 本地文件夹管理页路由
        LocalFolderManageScreen(
            onBack = { navController.popBackStack() }  // 返回上一页
        )
    }
}

// ==================== 详情页和播放页配置 ====================

/**
 * 剧集详情页路由配置
 * 
 * @param navController 导航控制器
 * @param seriesDataStore 剧集数据存储，用于获取剧集信息
 * 
 * 说明：
 * - route 带路径参数 {seriesId}，通过 URL 传递剧集 ID
 * - arguments 声明参数类型为 String
 * - backStackEntry.arguments 用于获取路由传递的参数
 * - onEpisodeClick 点击集数时跳转到播放页，传递 seriesId 和 episodeId
 * - LaunchedEffect 在数据不存在时自动返回上一页
 */
private fun NavGraphBuilder.seriesDetailScreen(
    navController: NavHostController,
    seriesDataStore: SeriesDataStore
) {
    composable(
        route = AppRoutes.SERIES_DETAIL,  // 路由模板，带 {seriesId} 参数
        arguments = listOf(  // 声明路由参数
            navArgument("seriesId") { type = NavType.StringType }  // seriesId 参数，类型为 String
        )
    ) { backStackEntry ->  // backStackEntry 包含导航参数和状态
        // 从导航参数中获取 seriesId
        val seriesId = backStackEntry.arguments?.getString("seriesId") ?: ""
        
        // 从本地数据存储获取剧集信息
        val series = seriesDataStore.getSeries(seriesId)
        
        // 数据不存在时自动返回（避免显示空白页）
        if (series == null) {
            // LaunchedEffect：副作用 API，在协程中执行操作
            // - Unit 作为 key：表示这个副作用只在首次进入时执行一次
            // - 如果 key 变化，会取消旧协程并重新执行
            // - 组件销毁时会自动取消协程
            LaunchedEffect(Unit) {
                navController.popBackStack()  // 自动返回上一页
            }
            return@composable  // 提前返回，不渲染界面
        }
        
        // 正常渲染详情页
        val detailViewModel = hiltViewModel<DetailViewModel>()  // 自动注入 ViewModel
        DetailScreen(
            viewModel = detailViewModel,  // 传递 ViewModel
            onBack = { navController.popBackStack() },  // 返回上一页
            onEpisodeClick = { episode ->  // 点击集数的回调
                navController.navigate(AppRoutes.player(series.id, episode.id))  // 跳转到播放页
            }
        )
    }
}

/**
 * 播放器页面路由配置
 * 全屏视频播放器
 * 
 * @param navController 导航控制器
 * @param seriesDataStore 剧集数据存储，用于验证剧集是否存在
 * 
 * 说明：
 * - route 带两个路径参数 {seriesId} 和 {episodeId}
 * - arguments 声明两个 String 类型参数
 * - onBack 点击返回时退出播放器
 * - 数据不存在时自动返回，防止显示空白页
 */
private fun NavGraphBuilder.playerScreen(
    navController: NavHostController,
    seriesDataStore: SeriesDataStore
) {
    composable(
        route = AppRoutes.PLAYER,  // 路由模板，带 {seriesId} 和 {episodeId} 参数
        arguments = listOf(  // 声明路由参数
            navArgument("seriesId") { type = NavType.StringType },  // seriesId 参数，类型为 String
            navArgument("episodeId") { type = NavType.StringType }  // episodeId 参数，类型为 String
        )
    ) { backStackEntry ->  // backStackEntry 包含导航参数和状态
        val seriesId = backStackEntry.arguments?.getString("seriesId") ?: ""  // 获取 seriesId
        val series = seriesDataStore.getSeries(seriesId)  // 获取剧集信息
        
        // 数据不存在时自动返回（避免显示空白页）
        if (series == null) {
            // LaunchedEffect：副作用 API，在协程中执行操作
            // - Unit 作为 key：表示这个副作用只在首次进入时执行一次
            // - 如果 key 变化，会取消旧协程并重新执行
            // - 组件销毁时会自动取消协程
            LaunchedEffect(Unit) {
                navController.popBackStack()  // 自动返回上一页
            }
            return@composable  // 提前返回，不渲染界面
        }
        
        // 正常渲染播放器页面
        val playerViewModel = hiltViewModel<PlayerViewModel>()  // 自动注入 ViewModel
        PlayerScreen(
            viewModel = playerViewModel,  // 传递 ViewModel
            onBack = { navController.popBackStack() }  // 返回上一页（退出播放器）
        )
    }
}
