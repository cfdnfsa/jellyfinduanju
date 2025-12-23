package com.wjp.jellyfinduanju.ui.components.menu

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

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
 * DataDrivenMenu - 数据驱动的下拉菜单
 * 
 * 通过菜单项数据列表自动生成菜单，避免重复代码
 * 
 * @param expanded 是否展开
 * @param onDismissRequest 关闭回调
 * @param items 菜单项列表
 * 
 * 使用场景：
 * - 长按卡片弹出的操作菜单
 * - 任何需要下拉菜单的地方
 * 
 * 使用示例：
 * ```
 * val menuItems = listOf(
 *     MenuItem(
 *         text = "标记为已看",
 *         icon = Icons.Default.Visibility,
 *         onClick = { viewModel.markAsWatched(true) }
 *     ),
 *     MenuItem.divider(),  // 分割线
 *     MenuItem(
 *         text = "删除剧集",
 *         icon = Icons.Default.Delete,
 *         textColor = MaterialTheme.colorScheme.error,  // 红色
 *         iconTint = MaterialTheme.colorScheme.error,
 *         onClick = { viewModel.showDeleteDialog() }
 *     )
 * )
 * 
 * DataDrivenMenu(
 *     expanded = isExpanded,
 *     onDismissRequest = { isExpanded = false },
 *     items = menuItems
 * )
 * ```
 * 
 * 优势：
 * - 菜单项用数据配置，更简洁
 * - 容易维护和修改
 * - 类似于 SortMenuItem 的设计
 */
@Composable
fun DataDrivenMenu(
    expanded: Boolean,  // 是否展开
    onDismissRequest: () -> Unit,  // 关闭回调
    items: List<MenuItem>  // 菜单项列表
) {
    DropdownMenu(
        expanded = expanded,  // 根据状态显示/隐藏
        onDismissRequest = onDismissRequest  // 点击外部关闭
    ) {
        // 遍历菜单项列表
        items.forEach { item ->
            // 提前处理分割线（减少嵌套）
            if (item.isDivider) {
                HorizontalDivider()
                return@forEach  // 跳过本次循环
            }
            
            // 普通菜单项
            DropdownMenuItem(
                text = {
                    Text(
                        text = item.text,  // 文本内容
                        color = item.textColor ?: Color.Unspecified  // 文本颜色（未指定则用默认）
                    )
                },
                onClick = item.onClick,  // 执行点击回调（菜单会在对话框弹出或点击外部时自然关闭）
                leadingIcon = item.icon?.let { icon ->
                    // 如果有图标，显示图标
                    {
                        Icon(
                            imageVector = icon,  // 图标内容
                            contentDescription = null,  // 装饰性图标
                            tint = item.iconTint ?: Color.Unspecified  // 图标颜色（未指定则用默认）
                        )
                    }
                }
            )
        }
    }
}
