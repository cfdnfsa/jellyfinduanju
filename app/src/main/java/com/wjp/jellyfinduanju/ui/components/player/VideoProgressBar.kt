package com.wjp.jellyfinduanju.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * VideoProgressBar - 视频进度条组件
 * 
 * 封装复杂的进度条拖动逻辑，包括：
 * - 拖动状态管理
 * - 预览时间显示
 * - 拖动时的高亮效果
 * - 自定义轨道样式
 * 
 * 【拖动逻辑（参考抖音）】
 * 1. 开始拖动：暂停播放，显示预览时间
 * 2. 拖动中：进度条实时跟随，时间文本高亮显示
 * 3. 松开：跳转到目标位置，自动恢复播放
 * 
 * @param currentPosition 当前播放位置（毫秒）
 * @param duration 视频总时长（毫秒）
 * @param currentPositionText 当前时间文本（格式化后）
 * @param durationText 总时长文本（格式化后）
 * @param onSeekStart 拖动开始回调
 * @param onSeekEnd 拖动结束回调（跳转并恢复播放）
 * @param modifier 布局修饰符
 * 
 * 使用场景：
 * - 播放器底部控制栏
 * - 任何需要视频进度条的地方
 * 
 * 使用示例：
 * ```
 * VideoProgressBar(
 *     currentPosition = uiState.currentPosition,
 *     duration = uiState.duration,
 *     currentPositionText = uiState.currentPositionText,
 *     durationText = uiState.durationText,
 *     onSeekStart = { viewModel.onSeekStart() },
 *     onSeekEnd = { targetPos -> viewModel.onSeekEnd(targetPos) }
 * )
 * ```
 */
@Composable
fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    currentPositionText: String,
    durationText: String,
    onSeekStart: () -> Unit,
    onSeekEnd: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    // ==================== 进度条拖动状态 ====================
    
    /** 是否正在拖动进度条 */
    var isSliderDragging by remember { mutableStateOf(false) }
    
    /** 拖动位置（0.0 ~ 1.0） */
    var sliderDragPosition by remember { mutableStateOf(0f) }
    
    // 显示的进度：拖动时显示拖动位置，否则显示实际位置
    val displayProgress = if (isSliderDragging) {
        sliderDragPosition
    } else {
        if (duration > 0) currentPosition.toFloat() / duration else 0f
    }
    
    // 显示的时间文本：拖动时显示预览时间
    val displayPositionText = if (isSliderDragging && duration > 0) {
        formatDuration((sliderDragPosition * duration).toLong())
    } else {
        currentPositionText
    }
    
    // ==================== 进度条行 ====================
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),  // 固定进度条行高度
        verticalAlignment = Alignment.CenterVertically  // 居中对齐
    ) {
        // 当前时间（拖动时高亮）
        Text(
            text = displayPositionText,
            color = if (isSliderDragging) Color(0xFFFF6B00) else Color.White,
            fontSize = if (isSliderDragging) 14.sp else 12.sp,
            fontWeight = if (isSliderDragging) FontWeight.Bold else FontWeight.Normal
        )
        
        // 进度条 Slider（抖音/快手风格）
        Slider(
            value = displayProgress,
            onValueChange = { progress ->
                // 开始拖动时通知暂停
                if (!isSliderDragging) {
                    isSliderDragging = true
                    onSeekStart()
                }
                sliderDragPosition = progress
            },
            onValueChangeFinished = {
                // 松开时：跳转 + 恢复播放
                if (isSliderDragging && duration > 0) {
                    val targetPos = (sliderDragPosition * duration).toLong()
                    onSeekEnd(targetPos)
                }
                isSliderDragging = false
            },
            modifier = Modifier
                .weight(1f)
                .height(24.dp)  // 限制 Slider 高度
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            track = { sliderState ->
                // 自定义轨道，确保已完成和未完成部分高度一致，且垂直居中
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)  // 统一高度2dp
                    ) {
                        // 未完成部分（背景）
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.3f))
                        )
                        // 已完成部分（前景）
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(sliderState.value)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                }
            },
            thumb = {
                // 不显示滑块，使用透明占位
                Box(
                    modifier = Modifier.size(0.dp)
                )
            }
        )
        
        // 总时长
        Text(
            text = durationText,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

/**
 * formatDuration - 时间格式化工具函数
 * 
 * 将毫秒转换为 mm:ss 或 hh:mm:ss 格式
 */
private fun formatDuration(milliseconds: Long): String {
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

