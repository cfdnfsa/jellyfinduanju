package com.wjp.jellyfinduanju.ui.components.badge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * OverlayBadges - 叠加徽章容器
 * 
 * 统一管理图片上的角标位置（四个角），避免重复写对齐和内边距代码
 * 
 * @param backgroundContent 背景内容（通常是图片）
 * @param topStart 左上角内容（可选）
 * @param topEnd 右上角内容（可选）
 * @param bottomStart 左下角内容（可选）
 * @param bottomEnd 右下角内容（可选）
 * @param modifier 布局修饰符
 * @param badgePadding 角标内边距（默认 4.dp）
 * 
 * 使用场景：
 * - 剧集卡片上的状态标识和信息标签
 * - 任何需要在图片四个角显示角标的地方
 * 
 * 使用示例：
 * ```
 * OverlayBadges(
 *     backgroundContent = {
 *         CachedAsyncImage(imageUrl = url, ...)
 *     },
 *     topStart = {
 *         IconBadge(icon = Icons.Default.CheckCircle, ...)
 *     },
 *     topEnd = {
 *         TextBadge(text = "12集")
 *     }
 * )
 * ```
 * 
 * 优势：
 * - 不用每次都写 .align(Alignment.TopStart).padding(4.dp)
 * - 角标位置统一，不会乱
 * - 代码更简洁、更易维护
 */
@Composable
fun OverlayBadges(
    backgroundContent: @Composable BoxScope.() -> Unit,  // 背景内容（图片）
    modifier: Modifier = Modifier,  // 外部修饰符
    badgePadding: androidx.compose.ui.unit.Dp = 4.dp,  // 角标统一内边距
    topStart: (@Composable BoxScope.() -> Unit)? = null,  // 左上角插槽
    topEnd: (@Composable BoxScope.() -> Unit)? = null,  // 右上角插槽
    bottomStart: (@Composable BoxScope.() -> Unit)? = null,  // 左下角插槽
    bottomEnd: (@Composable BoxScope.() -> Unit)? = null  // 右下角插槽
) {
    Box(modifier = modifier) {
        // 背景内容（通常是图片）
        backgroundContent()
        
        // 左上角角标
        topStart?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)  // 左上角对齐
                    .padding(badgePadding)  // 统一内边距
            ) {
                it()  // 执行插槽内容
            }
        }
        
        // 右上角角标
        topEnd?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)  // 右上角对齐
                    .padding(badgePadding)  // 统一内边距
            ) {
                it()  // 执行插槽内容
            }
        }
        
        // 左下角角标
        bottomStart?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)  // 左下角对齐
                    .padding(badgePadding)  // 统一内边距
            ) {
                it()  // 执行插槽内容
            }
        }
        
        // 右下角角标
        bottomEnd?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)  // 右下角对齐
                    .padding(badgePadding)  // 统一内边距
            ) {
                it()  // 执行插槽内容
            }
        }
    }
}
