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
 * 深色主题配色方案（极简黑白风格）
 * 
 * 灵感：Apple Music, Spotify
 * 特点：深灰背景 + 翡翠绿强调色 + 极简设计
 */
private val DarkColorScheme = darkColorScheme(
    // 主色调：翡翠绿（唯一的亮色点缀）
    primary = MinimalGreen,
    primaryContainer = MinimalGreen.copy(alpha = 0.15f),
    onPrimary = MinimalBlack,
    onPrimaryContainer = MinimalGreenLight,
    
    // 次要色调：天空蓝
    secondary = MinimalBlue,
    secondaryContainer = MinimalBlue.copy(alpha = 0.15f),
    onSecondary = MinimalBlack,
    onSecondaryContainer = Color(0xFF7DD3FC),
    
    // 第三色调：琥珀色
    tertiary = MinimalAmber,
    tertiaryContainer = MinimalAmber.copy(alpha = 0.15f),
    onTertiary = MinimalBlack,
    onTertiaryContainer = Color(0xFFFDE68A),
    
    // 背景色：深灰（不用纯黑，更柔和）
    background = MinimalDarkBackground,
    onBackground = MinimalTextPrimary,
    
    // 表面色：卡片、对话框等（略浅于背景）
    surface = MinimalDarkSurface,
    surfaceVariant = MinimalDarkSurfaceVariant,
    onSurface = MinimalTextPrimary,
    onSurfaceVariant = MinimalTextSecondary,
    
    // 轮廓线颜色
    outline = MinimalTextTertiary,
    outlineVariant = Color(0xFF3A3A3A),
    
    // 错误色
    error = MinimalRed,
    errorContainer = MinimalRed.copy(alpha = 0.15f),
    onError = MinimalWhite,
    onErrorContainer = Color(0xFFFECDD3)
)

/**
 * 浅色主题配色方案（极简黑白风格）
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
    onSecondaryContainer = Color(0xFF075985),
    
    // 第三色调：琥珀色
    tertiary = MinimalAmber,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiary = MinimalBlack,
    onTertiaryContainer = Color(0xFF78350F),
    
    // 背景色：柔和白
    background = MinimalLightBackground,
    onBackground = MinimalTextPrimaryLight,
    
    // 表面色：纯白
    surface = MinimalLightSurface,
    surfaceVariant = MinimalLightSurfaceVariant,
    onSurface = MinimalTextPrimaryLight,
    onSurfaceVariant = MinimalTextSecondaryLight,
    
    // 轮廓线颜色
    outline = MinimalTextTertiaryLight,
    outlineVariant = Color(0xFFE5E5E5),
    
    // 错误色
    error = MinimalRed,
    errorContainer = Color(0xFFFEE2E2),
    onError = MinimalWhite,
    onErrorContainer = Color(0xFF7F1D1D)
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
            // 设置状态栏背景色为主题背景色
            window.statusBarColor = colorScheme.background.toArgb()
            // 设置状态栏图标/文字的颜色模式
            // 深色主题时使用浅色图标，浅色主题时使用深色图标
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
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
