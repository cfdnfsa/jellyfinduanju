package com.wjp.jellyfinduanju.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * EpisodePlaceholder - 剧集占位视图
 * 
 * 用于 VerticalPager 中非当前页面的占位显示，
 * 显示集数和名称，提示用户可以滑动切换
 * 
 * @param episodeNumber 集数编号
 * @param episodeName 集名称
 * @param modifier 布局修饰符
 * 
 * 使用场景：
 * - VerticalPager 的非当前页面
 * - 预览下一集/上一集
 * 
 * 使用示例：
 * ```
 * EpisodePlaceholder(
 *     episodeNumber = 2,
 *     episodeName = "第二集",
 *     modifier = Modifier.fillMaxSize()
 * )
 * ```
 */
@Composable
fun EpisodePlaceholder(
    episodeNumber: Int,
    episodeName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 集数（大字号，半透明）
            Text(
                text = "第${episodeNumber}集",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 集名称（小字号，更淡）
            Text(
                text = episodeName,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }
    }
}

