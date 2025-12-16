package com.wjp.jellyfinduanju

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.wjp.jellyfinduanju.ui.theme.JellyfinDuanjuTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity - 应用的主入口 Activity
 * 
 * 继承自 ComponentActivity，这是 Jetpack Compose 推荐使用的 Activity 基类
 * ComponentActivity 提供了与 Compose 集成所需的功能
 * 
 * @AndroidEntryPoint 注解：
 * - 让 Hilt 为这个 Activity 生成依赖注入代码
 * - Activity 中可以使用 @Inject 注入依赖
 * - Activity 中创建的 ViewModel 可以自动注入依赖
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示（Edge-to-Edge）
        // 这会让应用内容延伸到状态栏和导航栏区域，实现沉浸式体验
        enableEdgeToEdge()
        
        // 全局隐藏导航栏（底部系统栏）
        window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            // 隐藏导航栏
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            // 设置手势模式（从边缘滑动可以临时显示）
            controller.systemBarsBehavior = 
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        // setContent 是 Compose 的入口点
        // 在这里定义整个应用的 UI 内容
        setContent {
            // JellyfinDuanjuTheme 是自定义的主题
            // 它会为整个应用提供统一的颜色、字体等样式
            JellyfinDuanjuTheme {
                // Surface 是 Material Design 的基础容器组件
                // 它提供背景色、形状、阴影等基础样式
                Surface(
                    modifier = Modifier.fillMaxSize(), // 填满整个屏幕
                    color = MaterialTheme.colorScheme.background // 使用主题中定义的背景色
                ) {
                    // MainApp 是应用的主要内容，包含导航和各个页面
                    MainApp()
                }
            }
        }
    }
}
