package com.wjp.jellyfinduanju.ui.components.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * IconBadge - 图标徽章（角标）
 * 
 * 通用的图标标签组件，常用于显示状态、提示等
 * 
 * @param icon 图标
 * @param tint 图标颜色
 * @param backgroundColor 背景颜色
 * @param modifier 布局修饰符
 * 
 * 使用场景：
 * - 视频观看状态（已看完、正在看）
 * - 删除状态标识
 * - 下载状态提示
 * - 任何需要角标的地方
 * 
 * 使用示例：
 * ```
 * IconBadge(
 *     icon = Icons.Default.CheckCircle,
 *     tint = Color.Green,
 *     modifier = Modifier.align(Alignment.TopStart)
 * )
 * ```
 */
@Composable
fun IconBadge(
    icon: ImageVector,  // 要显示的图标
    tint: Color,  // 图标颜色
    backgroundColor: Color = Color.Black.copy(alpha = 0.6f),  // 背景颜色（默认半透明黑色）
    modifier: Modifier = Modifier  // 外部修饰符
) {
    Box(
        modifier = modifier
            .size(24.dp)  // 固定大小 24dp
            .background(
                color = backgroundColor,  // 背景颜色
                shape = RoundedCornerShape(12.dp)  // 圆角（半圆）
            ),
        contentAlignment = Alignment.Center  // 内容居中
    ) {
        Icon(
            imageVector = icon,  // 图标内容
            contentDescription = null,  // 装饰性图标，无需描述
            modifier = Modifier.size(20.dp),  // 图标大小 20dp
            tint = tint  // 图标颜色
        )
    }
}

/**
 * TextBadge - 文本徽章（角标）
 * 
 * 通用的文本标签组件，常用于显示数量、类型等信息
 * 
 * @param text 显示的文本
 * @param backgroundColor 背景颜色
 * @param textColor 文本颜色
 * @param modifier 布局修饰符
 * 
 * 使用场景：
 * - 剧集数量（"12集2季"）
 * - 新内容提示（"NEW"）
 * - 类型标签（"HD"、"4K"）
 * - 任何需要文字角标的地方
 * 
 * 使用示例：
 * ```
 * TextBadge(
 *     text = "12集",
 *     modifier = Modifier.align(Alignment.TopEnd)
 * )
 * ```
 */
@Composable
fun TextBadge(
    text: String,  // 显示的文本内容
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),  // 背景颜色（默认主题色）
    textColor: Color = MaterialTheme.colorScheme.onPrimary,  // 文本颜色（默认与背景对应）
    modifier: Modifier = Modifier  // 外部修饰符
) {
    Box(
        modifier = modifier
            .background(
                color = backgroundColor,  // 背景颜色
                shape = RoundedCornerShape(4.dp)  // 小圆角
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)  // 内边距
    ) {
        Text(
            text = text,  // 文本内容
            style = MaterialTheme.typography.labelSmall,  // 小号字体
            color = textColor  // 文本颜色
        )
    }
}
