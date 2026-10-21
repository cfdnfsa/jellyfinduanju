package com.wjp.jellyfinduanju.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * DragProgressIndicator - 拖动进度指示器
 * 
 * 用于在用户水平拖动调节播放进度时，显示目标时间和总时长
 * 居中显示，半透明黑色背景
 * 
 * @param targetPosition 目标时间位置（毫秒）
 * @param duration 视频总时长（毫秒）
 * @param visible 是否显示
 * @param modifier 布局修饰符
 * @param formatDuration 时间格式化函数
 * 
 * 使用场景：
 * - 视频播放器拖动进度时的预览
 * - 任何需要显示时间预览的场景
 * 
 * 使用示例：
 * ```
 * DragProgressIndicator(
 *     targetPosition = dragPosition,
 *     duration = videoDuration,
 *     visible = isDragging,
 *     modifier = Modifier.align(Alignment.Center)
 * )
 * ```
 */
@Composable
fun DragProgressIndicator(
    targetPosition: Long,
    duration: Long,
    visible: Boolean,
    modifier: Modifier = Modifier,
    formatDuration: (Long) -> String = { formatTime(it) }
) {
    // 仅当可见且时长有效时显示
    if (!visible || duration <= 0) return
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 目标时间（大字号，高亮）
            Text(
                text = formatDuration(targetPosition),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 总时长（小字号，半透明）
            Text(
                text = "/ ${formatDuration(duration)}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

/**
 * formatTime - 时间格式化工具函数
 * 
 * 将毫秒转换为 mm:ss 或 hh:mm:ss 格式
 */
private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

