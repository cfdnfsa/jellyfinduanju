package com.wjp.jellyfinduanju.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * BottomNavItem - 底部导航项的密封类
 * 
 * 使用 sealed class（密封类）的好处：
 * 1. 类型安全：编译器知道所有可能的子类，when 表达式可以完全覆盖
 * 2. 不可扩展：外部代码无法创建新的子类，保证导航项是固定的
 * 3. 代码清晰：所有导航项定义在一个地方，便于维护
 * 
 * @property route 导航路由，唯一标识每个页面
 * @property title 显示的标题文字
 * @property icon 显示的图标
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    /**
     * 列表页 - 显示从网络获取的短剧列表
     * 
     * data object 是 Kotlin 1.9+ 的特性
     * 相比普通 object，它自动实现了 equals、hashCode 和 toString
     */
    data object List : BottomNavItem(
        route = "list",
        title = "列表",
        icon = Icons.Default.List // Material Icons 中的列表图标
    )

    /**
     * 下载页 - 显示已下载的短剧
     */
    data object Download : BottomNavItem(
        route = "download",
        title = "下载",
        icon = Icons.Default.Download // 下载图标
    )

    /**
     * 设置页 - 应用设置（服务器配置、缓存管理等）
     */
    data object Settings : BottomNavItem(
        route = "settings",
        title = "设置",
        icon = Icons.Default.Settings // 齿轮图标
    )
}
