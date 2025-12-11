package com.wjp.jellyfinduanju.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type.kt - 字体样式定义
 * 
 * Material 3 的 Typography 定义了一套完整的文字样式系统
 * 包括 display、headline、title、body、label 等级别
 * 
 * 每个级别有 Large、Medium、Small 三种尺寸
 * 例如：titleLarge、titleMedium、titleSmall
 * 
 * 使用方式：
 * ```
 * Text(
 *     text = "Hello",
 *     style = MaterialTheme.typography.titleLarge
 * )
 * ```
 */
val Typography = Typography(
    // ============ Body 正文样式 ============
    
    // bodyLarge：用于主要正文内容
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default, // 默认字体
        fontWeight = FontWeight.Normal,  // 正常粗细
        fontSize = 16.sp,                // 字号 16sp
        lineHeight = 24.sp,              // 行高 24sp
        letterSpacing = 0.5.sp           // 字间距 0.5sp
    ),
    
    // ============ Title 标题样式 ============
    
    // titleLarge：用于页面标题、重要标题
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,    // 粗体
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    
    // ============ Label 标签样式 ============
    
    // labelSmall：用于按钮、标签等小文字
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,  // 中等粗细
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    
    // 提示：可以根据需要添加更多样式
    // 例如：displayLarge, headlineMedium, bodySmall 等
)
