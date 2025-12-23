package com.wjp.jellyfinduanju.ui.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * DialogConfig - 确认对话框配置数据类
 * 
 * 用于数据驱动地配置确认类对话框，支持通过 Map 管理多个对话框
 * 
 * @param title 对话框标题
 * @param message 提示信息
 * @param confirmText 确认按钮文本（默认"确定"）
 * @param confirmColor 确认按钮颜色（null = 默认，非 null = 危险操作）
 * @param onConfirm 确认回调
 * 
 * 使用示例：
 * ```kotlin
 * val config = DialogConfig(
 *     title = "删除剧集",
 *     message = "确定要删除吗？",
 *     confirmColor = MaterialTheme.colorScheme.error,
 *     onConfirm = { viewModel.delete() }
 * )
 * ```
 */
data class DialogConfig(
    val title: String,  // 对话框标题
    val message: String,  // 提示信息
    val confirmText: String = "确定",  // 确认按钮文本
    val confirmColor: Color? = null,  // 确认按钮颜色（null = 默认，非 null = 危险操作）
    val onConfirm: () -> Unit  // 确认回调
)

/**
 * ConfirmDialog - 通用确认对话框
 * 
 * 用于需要二次确认的操作（删除、清空、重置等）
 * 
 * @param config 对话框配置
 * @param onDismiss 取消/关闭回调
 * 
 * 特点：
 * - 数据驱动（通过 DialogConfig 配置）
 * - 支持危险操作提示（红色按钮）
 * - 统一样式
 * 
 * 使用场景：
 * - 删除操作
 * - 清空操作
 * - 批量操作
 * - 任何需要二次确认的操作
 * 
 * 使用示例：
 * ```kotlin
 * ConfirmDialog(
 *     config = DialogConfig(
 *         title = "删除剧集",
 *         message = "确定要删除吗？",
 *         confirmColor = MaterialTheme.colorScheme.error,
 *         onConfirm = { viewModel.delete() }
 *     ),
 *     onDismiss = { viewModel.hideDialog() }
 * )
 * ```
 */
@Composable
fun ConfirmDialog(
    config: DialogConfig,  // 对话框配置
    onDismiss: () -> Unit  // 取消/关闭回调
) {
    AlertDialog(
        onDismissRequest = onDismiss,  // 点击外部或返回键关闭
        title = { Text(config.title) },  // 标题
        text = { Text(config.message) },  // 提示信息
        confirmButton = {
            TextButton(onClick = config.onConfirm) {  // 确认按钮
                Text(
                    text = config.confirmText,  // 按钮文本
                    color = config.confirmColor ?: MaterialTheme.colorScheme.primary  // 颜色
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {  // 取消按钮
                Text("取消")
            }
        }
    )
}
