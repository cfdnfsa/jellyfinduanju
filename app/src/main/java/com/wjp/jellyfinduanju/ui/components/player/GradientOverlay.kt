package com.wjp.jellyfinduanju.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * GradientPosition - 渐变方向枚举
 */
enum class GradientPosition {
    TOP,    // 从上到下（黑色到透明）
    BOTTOM  // 从下到上（透明到黑色）
}

/**
 * GradientOverlay - 渐变遮罩组件
 * 
 * 用于在视频播放器顶部或底部创建渐变遮罩，
 * 让控制栏的文字和按钮在视频内容上更清晰可见
 * 
 * @param position 渐变位置（顶部或底部）
 * @param height 遮罩高度
 * @param startAlpha 起始透明度（黑色部分的透明度，0.0 ~ 1.0）
 * @param modifier 布局修饰符
 * 
 * 使用场景：
 * - 视频播放器顶部栏背景
 * - 视频播放器底部栏背景
 * - 任何需要渐变遮罩的场景
 * 
 * 使用示例：
 * ```
 * // 顶部遮罩（从黑到透明）
 * GradientOverlay(
 *     position = GradientPosition.TOP,
 *     height = 100.dp,
 *     modifier = Modifier
 *         .fillMaxWidth()
 *         .align(Alignment.TopStart)
 * )
 * 
 * // 底部遮罩（从透明到黑）
 * GradientOverlay(
 *     position = GradientPosition.BOTTOM,
 *     height = 120.dp,
 *     modifier = Modifier
 *         .fillMaxWidth()
 *         .align(Alignment.BottomCenter)
 * )
 * ```
 */
@Composable
fun GradientOverlay(
    position: GradientPosition,
    height: Dp = 100.dp,
    startAlpha: Float = 0.7f,
    modifier: Modifier = Modifier
) {
    // 根据位置决定渐变方向
    val gradientColors = when (position) {
        GradientPosition.TOP -> listOf(
            Color.Black.copy(alpha = startAlpha),  // 顶部深色
            Color.Transparent                       // 底部透明
        )
        GradientPosition.BOTTOM -> listOf(
            Color.Transparent,                      // 顶部透明
            Color.Black.copy(alpha = startAlpha)   // 底部深色
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                brush = Brush.verticalGradient(colors = gradientColors)
            )
    )
}

