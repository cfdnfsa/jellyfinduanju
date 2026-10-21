package com.wjp.jellyfinduanju.ui.components.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * BufferingIndicator - 缓冲指示器
 * 
 * 显示在视频播放器中央的加载动画，
 * 用于视频缓冲时的提示
 * 
 * @param visible 是否显示
 * @param modifier 布局修饰符
 * 
 * 使用场景：
 * - 视频缓冲中
 * - 网络较慢时的播放提示
 * 
 * 使用示例：
 * ```
 * Box(modifier = Modifier.fillMaxSize()) {
 *     VideoPlayer(...)
 *     
 *     BufferingIndicator(
 *         visible = isBuffering,
 *         modifier = Modifier.align(Alignment.Center)
 *     )
 * }
 * ```
 */
@Composable
fun BufferingIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    // 仅当可见时显示
    if (!visible) return
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = Color.White,
            modifier = Modifier.size(48.dp)
        )
    }
}

