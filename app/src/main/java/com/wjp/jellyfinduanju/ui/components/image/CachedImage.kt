package com.wjp.jellyfinduanju.ui.components.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * CachedAsyncImage - 带缓存的异步图片组件
 * 
 * 自动处理图片加载、缓存、占位图的通用图片组件
 * 
 * @param imageUrl 图片 URL（可为 null）
 * @param contentDescription 无障碍描述
 * @param modifier 布局修饰符
 * @param contentScale 图片缩放方式
 * @param placeholderIcon 占位图图标（当 imageUrl 为 null 时显示）
 * 
 * 特点：
 * - 启用内存缓存（快速访问）
 * - 禁用磁盘缓存（避免与 Repository 的本地缓存重复）
 * - 300ms 淡入动画
 * - 缩略图加载（400x600px）
 * - imageUrl 为 null 时显示渐变占位图
 * 
 * 使用场景：
 * - 剧集封面
 * - 用户头像
 * - 任何需要加载远程图片的地方
 * 
 * 使用示例：
 * ```
 * CachedAsyncImage(
 *     imageUrl = series.coverUrl,
 *     contentDescription = series.name,
 *     modifier = Modifier.fillMaxSize()
 * )
 * ```
 */
@Composable
fun CachedAsyncImage(
    imageUrl: String?,  // 图片 URL（可为 null）
    contentDescription: String?,  // 无障碍描述
    modifier: Modifier = Modifier,  // 外部修饰符
    contentScale: ContentScale = ContentScale.Crop,  // 图片缩放方式（默认裁剪）
    placeholderIcon: ImageVector = Icons.Default.Movie  // 占位图图标
) {
    if (imageUrl != null) {
        // 有 URL：显示异步图片
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)  // 图片 URL
                .crossfade(300)  // 300ms 淡入动画
                .size(400, 600)  // 缩略图尺寸（宽 400px，高 600px，足够清晰）
                .memoryCachePolicy(CachePolicy.ENABLED)  // 启用内存缓存
                .diskCachePolicy(CachePolicy.DISABLED)  // 禁用磁盘缓存（已有本地缓存）
                .build(),
            contentDescription = contentDescription,  // 无障碍描述
            contentScale = contentScale,  // 缩放方式
            modifier = modifier  // 应用修饰符
        )
    } else {
        // 无 URL：显示占位图
        GradientPlaceholder(
            icon = placeholderIcon,  // 占位图图标
            modifier = modifier  // 应用修饰符
        )
    }
}

/**
 * GradientPlaceholder - 渐变占位图
 * 
 * 当图片 URL 为空时显示的占位图，使用渐变背景 + 图标
 * 
 * @param icon 占位图图标
 * @param modifier 布局修饰符
 */
@Composable
private fun GradientPlaceholder(
    icon: ImageVector,  // 占位图图标
    modifier: Modifier = Modifier  // 外部修饰符
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(  // 垂直渐变
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,  // 顶部颜色
                        MaterialTheme.colorScheme.surfaceVariant  // 底部颜色
                    )
                )
            ),
        contentAlignment = Alignment.Center  // 内容居中
    ) {
        Icon(
            imageVector = icon,  // 图标内容
            contentDescription = null,  // 装饰性图标，无需描述
            modifier = Modifier.size(32.dp),  // 图标大小 32dp
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)  // 半透明
        )
    }
}

@Preview
@Composable
private fun CachedAsyncImagePreview() {
    CachedAsyncImage(
        imageUrl = null,  // 显示占位图
        contentDescription = "预览",
        modifier = Modifier.size(200.dp, 300.dp)
    )
}
