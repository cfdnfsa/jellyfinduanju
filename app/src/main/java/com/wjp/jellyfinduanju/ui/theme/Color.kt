package com.wjp.jellyfinduanju.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color.kt - 应用颜色定义
 * 
 * 这里定义了应用中使用的所有颜色常量
 * 使用 Color(0xAARRGGBB) 格式，其中：
 * - AA: Alpha 透明度（FF = 完全不透明）
 * - RR: 红色分量
 * - GG: 绿色分量
 * - BB: 蓝色分量
 */

// ============ Material 3 默认调色板 ============

// 浅色主题的紫色系
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

// 深色主题的紫色系
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ============ Jellyfin 品牌色 ============

// Jellyfin 的标志性蓝色
val JellyfinPurple = Color(0xFF00A4DC)

// 深色背景色
val JellyfinDark = Color(0xFF101010)

// 深色表面色（略浅于背景，用于卡片等）
val JellyfinSurface = Color(0xFF1A1A1A)
