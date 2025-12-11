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
 * 深色主题配色方案
 * 
 * 使用 Jellyfin 风格的深色背景 + 蓝色强调色
 */
private val DarkColorScheme = darkColorScheme(
    // 主色调：用于按钮、链接、高亮等
    primary = JellyfinPurple,
    // 次要色调
    secondary = PurpleGrey80,
    // 第三色调
    tertiary = Pink80,
    // 背景色：整个应用的背景
    background = JellyfinDark,
    // 表面色：卡片、对话框等组件的背景
    surface = JellyfinSurface,
    // 以下是对应的「上层」颜色（放在 primary/surface 等上面的文字/图标颜色）
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

/**
 * 浅色主题配色方案
 */
private val LightColorScheme = lightColorScheme(
    primary = JellyfinPurple,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color.White,
    surface = Color(0xFFF5F5F5),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
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
