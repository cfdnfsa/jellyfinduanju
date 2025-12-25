package com.wjp.jellyfinduanju.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * VideoErrorView - 视频错误视图
 * 
 * 显示错误信息和重试按钮，用于视频加载失败时
 * 
 * @param errorMessage 错误信息
 * @param onRetry 重试回调
 * @param modifier 布局修饰符
 * 
 * 使用场景：
 * - 视频加载失败
 * - 网络错误
 * - 视频源无效
 * 
 * 使用示例：
 * ```
 * VideoErrorView(
 *     errorMessage = "网络连接失败",
 *     onRetry = { viewModel.retryLoad() },
 *     modifier = Modifier.fillMaxSize()
 * )
 * ```
 */
@Composable
fun VideoErrorView(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 错误标题
            Text(
                text = "视频加载失败",
                color = Color.White,
                fontSize = 18.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 错误详情
            Text(
                text = errorMessage,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 重试按钮
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "重试",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "点击重试",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

