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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.wjp.jellyfinduanju.di.entrypoint.getAuthRepository
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wjp.jellyfinduanju.navigation.BottomNavItem
import com.wjp.jellyfinduanju.navigation.routes.AppNavGraph
import com.wjp.jellyfinduanju.navigation.routes.AppRoutes
import com.wjp.jellyfinduanju.ui.screens.login.LoginScreen
import com.wjp.jellyfinduanju.ui.screens.login.LoginViewModel

// ==================== 主入口 ====================

/**
 * MainApp - 应用的主 Composable 函数
 * 
 * 负责：
 * 1. 检查登录状态
 * 2. 未登录 -> 显示登录页
 * 3. 已登录 -> 显示主页（带底部导航）
 * 
 * 说明：
 * - 使用响应式状态管理，监听 userSession 的变化
 * - 登录成功后 userSession 自动更新，界面自动切换
 * - 不需要手动处理页面跳转，完全基于状态驱动
 */
@Composable
fun MainApp() {
    // 通过 EntryPoint 获取 AuthRepository（已内置缓存）
    val authRepository = getAuthRepository()
    
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
        // 已登录，显示主页面（带底部导航和内容区域）
        MainContent()
    } else {
        // 未登录，显示登录页面
        LoginContent()
    }
}

// ==================== 登录页面 ====================

/**
 * LoginContent - 登录页面容器
 * 
 * 说明：
 * - 用户未登录时显示此页面
 * - 登录成功后 userSession 会自动更新，触发 MainApp 重组切换到 MainContent
 * - 不需要手动处理登录回调，依靠响应式状态自动切换
 */
@Composable
private fun LoginContent() {
    val viewModel = hiltViewModel<LoginViewModel>()  // 自动注入 ViewModel
    
    LoginScreen(viewModel)  // 显示登录界面
}

// ==================== 主页面 ====================

/**
 * MainContent - 主页面（带底部导航）
 * 
 * 职责：
 * - 管理 Scaffold 布局（底部导航栏）
 * - 根据当前路由决定是否显示底部栏
 * - 控制页面内边距（播放器需要全屏）
 * 
 * 说明：
 * - rememberNavController 创建导航控制器，在重组时保持实例不变
 * - currentBackStackEntryAsState 监听导航栈变化，返回 State 对象
 * - Scaffold 提供标准的 Material Design 页面布局
 */
@Composable
private fun MainContent() {
    // 创建导航控制器，管理页面跳转和返回栈
    val navController = rememberNavController()
    
    // 监听当前路由，用于动态控制 UI（如是否显示底部栏）
    val navBackStackEntry by navController.currentBackStackEntryAsState()  // 当前导航栈顶条目
    val currentRoute = navBackStackEntry?.destination?.route  // 当前路由路径
    
    // 判断是否显示底部导航栏
    // 规则：详情页、播放页、设置子页面需要隐藏底部栏，获得更大显示空间
    val shouldShowBottomBar = when {
        currentRoute == null -> true  // 默认显示
        currentRoute == AppRoutes.CACHE_SETTINGS -> false  // 缓存设置页隐藏
        currentRoute == AppRoutes.TAG_MANAGE -> false  // 标签管理页隐藏
        currentRoute == AppRoutes.LOCAL_FOLDER_MANAGE -> false  // 文件夹管理页隐藏
        currentRoute.startsWith("series_detail/") -> false  // 剧集详情页隐藏
        currentRoute.startsWith("player/") -> false  // 播放器页面隐藏
        else -> true  // 其他页面显示
    }
    
    // 判断是否应用内边距
    // 规则：播放器页面需要全屏显示，不应用 Scaffold 的 padding
    val shouldApplyPadding = !currentRoute.orEmpty().startsWith("player/")

    // Scaffold：Material Design 的标准页面布局容器
    Scaffold(
        bottomBar = {  // 底部栏插槽
            if (shouldShowBottomBar) {  // 根据路由决定是否显示
                MainBottomNavigationBar(navController = navController)
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)  // 禁用自动内边距，配合边到边模式
    ) { innerPadding ->  // innerPadding 是 Scaffold 自动计算的内边距
        // 根据页面类型决定是否应用内边距
        // 播放器页面不使用 padding，实现全屏效果
        AppNavGraph(
            navController = navController,  // 传递导航控制器
            // true -> 应用内边距
            modifier = if (shouldApplyPadding) Modifier.padding(innerPadding) else Modifier  // 动态内边距
        )
    }
}

// ==================== 底部导航栏 ====================

/**
 * MainBottomNavigationBar - 底部导航栏
 * 
 * @param navController 导航控制器，用于处理页面跳转
 * 
 * 说明：
 * - 使用 Material Design 3 的 NavigationBar 组件
 * - 管理三个主要功能：列表、下载、设置
 * - 支持页面状态保存和恢复
 */
@Composable
private fun MainBottomNavigationBar(
    navController: NavHostController
) {
    // 定义底部导航项
    val bottomNavItems = listOf(
        BottomNavItem.List,      // 剧集列表
        BottomNavItem.Download,  // 下载管理
        BottomNavItem.Settings   // 设置
    )
    
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,  // 背景色
        tonalElevation = 0.dp  // 扁平化设计，无阴影
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()  // 监听导航状态
        val currentDestination = navBackStackEntry?.destination  // 当前目的地

        bottomNavItems.forEach { item ->  // 遍历所有导航项
            // 判断当前项是否被选中
            // hierarchy.any：检查当前目的地是否在导航层次结构中，支持嵌套导航图
            val isSelected = currentDestination?.hierarchy?.any { 
                it.route == item.route 
            } == true
            
            NavigationBarItem(
                icon = {  // 图标插槽
                    Icon(
                        imageVector = item.icon,  // 图标资源
                        contentDescription = item.title  // 无障碍描述
                    ) 
                },
                label = { Text(item.title) },  // 文字标签
                selected = isSelected,  // 选中状态
                onClick = {  // 点击事件
                    navController.navigate(item.route) {  // 导航到目标路由
                        // popUpTo：清除返回栈到起始位置，避免栈过深
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true  // 保存页面状态（滚动位置、表单数据等）
                        }
                        launchSingleTop = true  // 避免重复创建同一页面
                        restoreState = true  // 恢复之前保存的状态
                    }
                },
                colors = NavigationBarItemDefaults.colors(  // 颜色配置
                    selectedIconColor = MaterialTheme.colorScheme.primary,  // 选中图标颜色
                    selectedTextColor = MaterialTheme.colorScheme.primary,  // 选中文字颜色
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,  // 选中指示器背景色
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,  // 未选中图标颜色
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant  // 未选中文字颜色
                )
            )
        }
    }
    
}

