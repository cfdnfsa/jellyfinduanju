package com.wjp.jellyfinduanju.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * BottomNavItem - 底部导航项的密封类
 * 
 * 使用 Outlined 风格图标（简约黑色线条风格）
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
     */
    data object List : BottomNavItem(
        route = "list",
        title = "列表",
        icon = Icons.AutoMirrored.Outlined.List // 简约线条风格
    )

    /**
     * 下载页 - 显示已下载的短剧
     */
    data object Download : BottomNavItem(
        route = "download",
        title = "下载",
        icon = Icons.Outlined.Download // 简约线条风格
    )

    /**
     * 设置页 - 应用设置（服务器配置、缓存管理等）
     */
    data object Settings : BottomNavItem(
        route = "settings",
        title = "设置",
        icon = Icons.Outlined.Settings // 简约线条风格
    )
}
