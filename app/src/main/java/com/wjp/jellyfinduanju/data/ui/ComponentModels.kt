package com.wjp.jellyfinduanju.data.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * ComponentModels.kt - UI 组件通用数据模型
 * 
 * 包含可复用的 UI 组件数据类，如菜单项、排序选项等
 */

/**
 * MenuItem - 菜单项数据类
 * 
 * @param text 显示文本
 * @param icon 图标
 * @param onClick 点击回调
 * @param textColor 文本颜色（可选，危险操作可用红色）
 * @param iconTint 图标颜色（可选）
 * @param isDivider 是否是分割线（特殊标记，true 时其他字段无效）
 * 
 * 使用示例：
 * ```
 * val menuItems = listOf(
 *     MenuItem("删除", Icons.Default.Delete, textColor = errorColor, iconTint = errorColor) { delete() },
 *     MenuItem.divider(),
 *     MenuItem("取消", Icons.Default.Close) { dismiss() }
 * )
 * 
 * DataDrivenMenu(
 *     expanded = showMenu,
 *     onDismissRequest = { showMenu = false },
 *     items = menuItems
 * )
 * ```
 */
data class MenuItem(
    val text: String = "",  // 显示文本
    val icon: ImageVector? = null,  // 图标（可为空）
    val onClick: () -> Unit = {},  // 点击回调
    val textColor: Color? = null,  // 文本颜色（可选）
    val iconTint: Color? = null,  // 图标颜色（可选）
    val isDivider: Boolean = false  // 是否是分割线
) {
    companion object {
        /**
         * 创建分割线菜单项
         */
        fun divider() = MenuItem(isDivider = true)
    }
}

/**
 * SortOption - 排序选项配置数据类
 * 
 * @param type 排序类型枚举值
 * @param label 显示文本
 * @param icon 图标（可选）
 */
data class SortOption<T>(
    val type: T,          // 排序类型（泛型，支持任意枚举）
    val label: String,    // 显示文本，如 "按名称"
    val icon: ImageVector? = null  // 可选图标
)

