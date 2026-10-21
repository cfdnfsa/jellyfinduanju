package com.wjp.jellyfinduanju.ui.components.player

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * SpeedIndicator - 倍速指示器
 * 
 * 用于在长按加速播放时，显示当前播放倍速
 * 通常显示在右上角，简洁明了
 * 
 * @param speed 当前播放倍速（如 3.0f）
 * @param visible 是否显示
 * @param modifier 布局修饰符（通常包含位置信息）
 * 
 * 使用场景：
 * - 长按 3x 倍速播放时的提示
 * - 任何需要显示倍速的场景
 * 
 * 使用示例：
 * ```
 * SpeedIndicator(
 *     speed = 3.0f,
 *     visible = isLongPressing,
 *     modifier = Modifier
 *         .align(Alignment.TopEnd)
 *         .padding(top = 60.dp, end = 16.dp)
 * )
 * ```
 */
@Composable
fun SpeedIndicator(
    speed: Float,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    // 仅当可见时显示
    if (!visible) return
    
    Text(
        text = "${speed}x",
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}

