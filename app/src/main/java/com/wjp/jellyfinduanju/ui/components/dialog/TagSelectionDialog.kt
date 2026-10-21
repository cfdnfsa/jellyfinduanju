package com.wjp.jellyfinduanju.ui.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wjp.jellyfinduanju.data.model.SeriesLocalData
import com.wjp.jellyfinduanju.data.settings.AppSettings

/**
 * TagSelectionDialog - 标签选择对话框
 * 
 * 显示系统标签（只读）和自定义标签（可编辑）
 * 
 * @param series 剧集数据
 * @param onTagToggle 标签切换回调 (tag: String, selected: Boolean) -> Unit
 * @param onClearAllTags 清除所有自定义标签回调
 * @param onDismiss 关闭对话框回调
 * 
 * 使用场景：
 * - 为剧集设置自定义标签
 * - 查看剧集的系统标签状态
 * 
 * 使用示例：
 * ```kotlin
 * TagSelectionDialog(
 *     series = series,
 *     onTagToggle = { tag, selected -> viewModel.toggleTag(tag, selected) },
 *     onClearAllTags = { viewModel.clearAllTags() },
 *     onDismiss = { viewModel.hideDialog() }
 * )
 * ```
 */
@Composable
fun TagSelectionDialog(
    series: SeriesLocalData,  // 剧集数据
    onTagToggle: (String, Boolean) -> Unit,  // 标签切换回调
    onClearAllTags: () -> Unit,  // 清除所有标签回调
    onDismiss: () -> Unit  // 关闭对话框回调
) {
    AlertDialog(
        onDismissRequest = onDismiss,  // 点击外部或返回键关闭
        title = { Text("设置标签") },  // 对话框标题
        text = {
            // 对话框内容：系统标签 + 自定义标签
            TagDialogContent(
                series = series,
                onTagToggle = onTagToggle
            )
        },
        confirmButton = {
            // 完成按钮（主按钮）
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        },
        dismissButton = {
            // 清除全部按钮（仅当有自定义标签时显示）
            if (series.customTags.isNotEmpty()) {
                TextButton(onClick = onClearAllTags) {
                    Text("清除全部")
                }
            }
        }
    )
}

/**
 * TagDialogContent - 标签对话框内容
 * 
 * 包含系统标签区域和自定义标签区域
 */
@Composable
private fun TagDialogContent(
    series: SeriesLocalData,  // 剧集数据
    onTagToggle: (String, Boolean) -> Unit  // 标签切换回调
) {
    Column {
        // 系统标签区域（只读）
        SystemTagsSection(series)
        
        // 分割线
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // 自定义标签区域（可编辑）
        CustomTagsSection(
            series = series,
            onTagToggle = onTagToggle
        )
    }
}

/**
 * SystemTagsSection - 系统标签区域
 * 
 * 显示只读的系统标签（缓存状态、云端文件夹）
 */
@Composable
private fun SystemTagsSection(series: SeriesLocalData) {
    // 区域标题
    Text(
        text = "系统标签",  // 标题文本
        style = MaterialTheme.typography.labelMedium,  // 中等标签字体
        color = MaterialTheme.colorScheme.primary,  // 主题色
        modifier = Modifier.padding(bottom = 8.dp)  // 底部间距
    )
    
    // 缓存状态标签
    SystemTagRow(
        icon = if (series.isVideoCached) Icons.Default.Download else Icons.Default.CloudQueue,  // 图标：已缓存 or 未缓存
        text = if (series.isVideoCached) "已缓存" else "未缓存",  // 文本
        color = if (series.isVideoCached)  // 颜色：绿色 or 灰色
            MaterialTheme.colorScheme.tertiary
        else
            MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    // 云端文件夹标签（可选）
    series.cloudFolderTag?.let { cloudTag ->
        SystemTagRow(
            icon = Icons.Default.Folder,  // 文件夹图标
            text = cloudTag,  // 云端文件夹名称
            color = MaterialTheme.colorScheme.secondary  // 次要主题色
        )
    }
}

/**
 * CustomTagsSection - 自定义标签区域
 * 
 * 显示可编辑的自定义标签列表（直接从 AppSettings 读取）
 */
@Composable
private fun CustomTagsSection(
    series: SeriesLocalData,  // 剧集数据
    onTagToggle: (String, Boolean) -> Unit  // 标签切换回调
) {
    // 区域标题
    Text(
        text = "自定义标签",  // 标题文本
        style = MaterialTheme.typography.labelMedium,  // 中等标签字体
        color = MaterialTheme.colorScheme.primary,  // 主题色
        modifier = Modifier.padding(bottom = 8.dp)  // 底部间距
    )
    
    // 从 AppSettings 直接读取自定义标签列表
    val customTags = AppSettings.customTags
    
    // 判断是否有自定义标签
    if (customTags.isNotEmpty()) {
        // 有标签：显示标签列表
        customTags.forEach { tag ->
            CustomTagItem(
                tag = tag,
                isSelected = tag in series.customTags,  // 判断是否选中
                onToggle = onTagToggle
            )
        }
    } else {
        // 无标签：显示空状态提示
        EmptyTagsHint()
    }
}

/**
 * CustomTagItem - 单个自定义标签项
 * 
 * 显示可选中的标签项（Checkbox + 文本）
 */
@Composable
private fun CustomTagItem(
    tag: String,  // 标签名称
    isSelected: Boolean,  // 是否选中
    onToggle: (String, Boolean) -> Unit  // 切换回调
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()  // 填充宽度
            .clickable { onToggle(tag, !isSelected) }  // 点击切换选中状态
            .padding(vertical = 4.dp),  // 垂直间距
        verticalAlignment = Alignment.CenterVertically  // 垂直居中对齐
    ) {
        // 复选框
        Checkbox(
            checked = isSelected,  // 选中状态
            onCheckedChange = { onToggle(tag, it) }  // 状态变化回调
        )
        
        // 间距
        Spacer(modifier = Modifier.width(8.dp))
        
        // 标签名称
        Text(tag)
    }
}

/**
 * EmptyTagsHint - 空标签提示
 * 
 * 当没有自定义标签时显示的提示文本
 */
@Composable
private fun EmptyTagsHint() {
    Text(
        text = "暂无自定义标签，请到设置中添加",  // 提示文本
        style = MaterialTheme.typography.bodySmall,  // 小号字体
        color = MaterialTheme.colorScheme.onSurfaceVariant  // 次要文本颜色
    )
}

/**
 * SystemTagRow - 系统标签行组件
 * 
 * 用于显示只读的系统标签（如缓存状态、云端文件夹）
 * 
 * @param icon 图标
 * @param text 文本
 * @param color 颜色
 */
@Composable
private fun SystemTagRow(
    icon: ImageVector,  // 图标
    text: String,  // 文本
    color: Color  // 颜色
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Preview
@Composable
private fun SystemTagRowPreview() {
    SystemTagRow(
        icon = Icons.Default.Download,
        text = "已缓存",
        color = Color.Green
    )
}
