package com.wjp.jellyfinduanju.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Theme.kt - 应用主题定义
 * 
 * Material 3 使用 ColorScheme 来定义一套完整的颜色方案
 * 包括 primary、secondary、background、surface 等角色颜色
 */

/**
 * 深色主题配色方案（极简克制风格）
 * 
 * 特点：接近纯黑背景 + 翡翠绿/天空蓝强调色 + 克制用色
 */
private val DarkColorScheme = darkColorScheme(
    // 主色调：翡翠绿（仅在必要时使用）
    primary = MinimalGreen,
    primaryContainer = MinimalGreen.copy(alpha = 0.15f),
    onPrimary = MinimalWhite,
    onPrimaryContainer = MinimalGreenLight,
    
    // 次要色调：天空蓝
    secondary = MinimalBlue,
    secondaryContainer = MinimalBlue.copy(alpha = 0.15f),
    onSecondary = MinimalWhite,
    onSecondaryContainer = MinimalBlueLight,
    
    // 第三色调：灰琥珀（警告时使用）
    tertiary = MinimalAmber,
    tertiaryContainer = MinimalAmber.copy(alpha = 0.12f),
    onTertiary = MinimalBlack,
    onTertiaryContainer = Color(0xFFE8D9C3),
    
    // 背景色：接近纯黑（极简）
    background = MinimalDarkBackground,
    onBackground = MinimalTextPrimary,
    
    // 表面色：卡片、对话框等（略浅于背景，保持极简）
    surface = MinimalDarkSurface,
    surfaceVariant = MinimalDarkSurfaceVariant,
    onSurface = MinimalTextPrimary,
    onSurfaceVariant = MinimalTextSecondary,
    
    // 轮廓线颜色（深灰，低调）
    outline = MinimalBorderDark,
    outlineVariant = Color(0xFF1F1F1F),
    
    // 错误色（柔和红，不刺眼）
    error = MinimalRed,
    errorContainer = MinimalRed.copy(alpha = 0.15f),
    onError = MinimalBlack,
    onErrorContainer = Color(0xFFEDD3D3)
)

/**
 * 浅色主题配色方案（极简克制风格）
 * 
 * 特点：温暖白色背景 + 黑色文字/图标为主 + 翡翠绿/天空蓝点缀
 */
private val LightColorScheme = lightColorScheme(
    // 主色调：翡翠绿
    primary = MinimalGreen,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimary = MinimalWhite,
    onPrimaryContainer = MinimalGreenDark,
    
    // 次要色调：天空蓝
    secondary = MinimalBlue,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondary = MinimalWhite,
    onSecondaryContainer = MinimalBlueDark,
    
    // 第三色调：灰琥珀
    tertiary = MinimalAmber,
    tertiaryContainer = Color(0xFFEEE5D6),
    onTertiary = MinimalBlack,
    onTertiaryContainer = Color(0xFF6B5D48),
    
    // 背景色：温暖的浅灰（不用纯白，更柔和）
    background = MinimalLightBackground,
    onBackground = MinimalTextPrimaryLight,  // 纯黑文字
    
    // 表面色：纯白卡片
    surface = MinimalLightSurface,
    surfaceVariant = MinimalLightSurfaceVariant,
    onSurface = MinimalTextPrimaryLight,  // 纯黑文字
    onSurfaceVariant = MinimalTextSecondaryLight,  // 深灰文字
    
    // 轮廓线颜色（浅灰，低调）
    outline = MinimalBorderLight,
    outlineVariant = Color(0xFFF0F0F0),
    
    // 错误色（柔和红）
    error = MinimalRed,
    errorContainer = Color(0xFFF5E5E5),
    onError = MinimalBlack,
    onErrorContainer = Color(0xFF8B6666)
)

/**
 * JellyfinDuanjuTheme - 应用的主题 Composable
 * 
 * @param darkTheme 是否使用深色主题，默认跟随系统设置
 * @param content 被主题包裹的内容
 * 
 * 使用方式：
 * ```
 * JellyfinDuanjuTheme {
 *     // 你的 Composable 内容
 * }
 * ```
 */
@Composable
fun JellyfinDuanjuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // 检测系统是否开启深色模式
    content: @Composable () -> Unit
) {
    // 根据深浅色模式选择对应的配色方案
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // LocalView.current 获取当前的 Android View
    val view = LocalView.current
    
    // isInEditMode 判断是否在 Android Studio 预览模式
    // 预览模式下不执行系统 UI 修改
    if (!view.isInEditMode) {
        // SideEffect 用于执行不影响 Compose 状态的副作用操作
        // 这里用于修改系统状态栏的颜色和样式
        SideEffect {
            // 获取当前 Activity 的 Window
            val window = (view.context as Activity).window
            // 保持状态栏透明（支持边到边显示）
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            // 设置状态栏图标/文字的颜色模式
            // 深色主题时使用浅色图标，浅色主题时使用深色图标（黑色线条风格）
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            
            // 确保内容延伸到系统栏（边到边显示）
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    // MaterialTheme 是 Material 3 的主题提供者
    // 它会向下传递 colorScheme 和 typography，让子组件可以使用
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // 使用 Type.kt 中定义的字体样式
        content = content
    )
}
