package com.wjp.jellyfinduanju.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * HistoryScreen - 历史页面
 * 
 * 功能：显示用户的观看历史记录
 * 
 * 后续实现：
 * - 从 Jellyfin 同步观看历史
 * - 显示观看进度（已看到第几集、观看时间点）
 * - 继续观看功能
 * - 清除历史记录
 */
@Composable
fun HistoryScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("观看历史")
    }
}
