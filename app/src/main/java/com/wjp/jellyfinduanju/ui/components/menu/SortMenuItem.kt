package com.wjp.jellyfinduanju.ui.components.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wjp.jellyfinduanju.data.ui.SortOption

/**
 * SortMenuItem - 排序菜单项组件
 * 
 * @param label 显示文本
 * @param icon 前导图标（可选）
 * @param isSelected 是否选中当前项
 * @param sortAscending 升序还是降序（仅选中时显示）
 * @param onClick 点击回调
 * 
 * 特点：
 * - 前导图标：区分不同排序类型
 * - RadioButton：明确显示选中状态
 * - 升降序指示：箭头图标
 * - 颜色突出：选中项使用主题色
 * 
 * 使用示例：
 * ```
 * SortMenuItem(
 *     label = "按名称",
 *     icon = Icons.Default.SortByAlpha,
 *     isSelected = true,
 *     sortAscending = true,
 *     onClick = { ... }
 * )
 * ```
 */
@Composable
fun SortMenuItem(
    label: String,  // 显示文本（如 "按名称"）
    icon: ImageVector?,  // 前导图标（可选）
    isSelected: Boolean,  // 是否选中当前项
    sortAscending: Boolean,  // 是否升序（true=升序，false=降序）
    onClick: () -> Unit  // 点击回调
) {
    DropdownMenuItem(
        leadingIcon = if (icon != null) {  // 如果提供了图标，显示前导图标
            { MenuLeadingIcon(icon = icon, isSelected = isSelected) }
        } else null,  // 否则不显示
        text = { MenuItemText(label = label, isSelected = isSelected, sortAscending = sortAscending) },  // 中间文本区域
        trailingIcon = { MenuTrailingRadioButton(isSelected = isSelected) },  // 尾部单选按钮
        onClick = onClick  // 点击事件
    )
}

/**
 * MenuLeadingIcon - 菜单项的前导图标
 * 
 * @param icon 图标
 * @param isSelected 是否选中
 */
@Composable
private fun MenuLeadingIcon(
    icon: ImageVector,  // 要显示的图标
    isSelected: Boolean  // 是否选中
) {
    Icon(
        imageVector = icon,  // 图标内容
        contentDescription = null,  // 无障碍描述（装饰性图标可为 null）
        tint = if (isSelected)  // 根据选中状态设置颜色
            MaterialTheme.colorScheme.primary  // 选中：主题色（高亮）
        else 
            MaterialTheme.colorScheme.onSurfaceVariant  // 未选中：灰色
    )
}

/**
 * MenuItemText - 菜单项的文本内容（包含排序箭头）
 * 
 * @param label 显示文本
 * @param isSelected 是否选中
 * @param sortAscending 升序/降序
 */
@Composable
private fun MenuItemText(
    label: String,  // 显示文本
    isSelected: Boolean,  // 是否选中
    sortAscending: Boolean  // 是否升序
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,  // 垂直居中对齐
        horizontalArrangement = Arrangement.SpaceBetween,  // 水平两端对齐（文本在左，箭头在右）
        modifier = Modifier.fillMaxWidth()  // 填充整个宽度
    ) {
        // 排序名称（如 "按名称"）
        Text(
            text = label,  // 显示的文本内容
            color = if (isSelected)  // 根据选中状态设置颜色
                MaterialTheme.colorScheme.primary  // 选中：主题色（高亮）
            else 
                Color.Unspecified  // 未选中：默认色
        )
        // 升降序箭头（仅选中时显示）
        if (isSelected) {
            SortDirectionIcon(sortAscending = sortAscending)  // 显示 ↑ 或 ↓
        }
    }
}

/**
 * SortDirectionIcon - 排序方向箭头图标
 * 
 * @param sortAscending 是否升序
 */
@Composable
private fun SortDirectionIcon(sortAscending: Boolean) {  // 是否升序
    Icon(
        imageVector = if (sortAscending)  // 根据升降序选择箭头方向
            Icons.Default.ArrowUpward  // 升序 ↑（向上箭头）
        else 
            Icons.Default.ArrowDownward,  // 降序 ↓（向下箭头）
        contentDescription = if (sortAscending) "升序" else "降序",  // 无障碍描述
        modifier = Modifier.size(16.dp),  // 图标大小 16dp（较小）
        tint = MaterialTheme.colorScheme.primary  // 使用主题色（高亮）
    )
}

/**
 * MenuTrailingRadioButton - 菜单项的尾部单选按钮
 * 
 * @param isSelected 是否选中
 */
@Composable
private fun MenuTrailingRadioButton(isSelected: Boolean) {  // 是否选中
    RadioButton(
        selected = isSelected,  // 单选按钮的选中状态
        onClick = null,  // 不处理点击（由外层 DropdownMenuItem 统一处理）
        colors = RadioButtonDefaults.colors(
            selectedColor = MaterialTheme.colorScheme.primary  // 选中时的颜色（主题色）
        )
    )
}

@Preview
@Composable
private fun SortMenuItemPreview() {
    SortMenuItem(
        label = "按名称",
        icon = Icons.Default.SortByAlpha,
        isSelected = true,
        sortAscending = true,
        onClick = {}
    )
}
