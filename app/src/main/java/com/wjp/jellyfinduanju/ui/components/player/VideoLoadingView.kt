package com.wjp.jellyfinduanju.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * VideoLoadingView - 视频加载中视图
 * 
 * 显示加载动画和提示文本，用于视频源加载阶段
 * 
 * @param message 加载提示文本
 * @param modifier 布局修饰符
 * 
 * 使用场景：
 * - 视频源加载中
 * - 切换集数时的过渡状态
 * 
 * 使用示例：
 * ```
 * VideoLoadingView(
 *     message = "正在加载视频...",
 *     modifier = Modifier.fillMaxSize()
 * )
 * ```
 */
@Composable
fun VideoLoadingView(
    message: String = "正在加载视频...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 加载动画
            CircularProgressIndicator(color = Color.White)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 提示文本
            Text(
                text = message,
                color = Color.White
            )
        }
    }
}

