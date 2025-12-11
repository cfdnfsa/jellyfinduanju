package com.wjp.jellyfinduanju

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.wjp.jellyfinduanju.ui.theme.JellyfinDuanjuTheme

/**
 * MainActivity - 应用的主入口 Activity
 * 
 * 继承自 ComponentActivity，这是 Jetpack Compose 推荐使用的 Activity 基类
 * ComponentActivity 提供了与 Compose 集成所需的功能
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示（Edge-to-Edge）
        // 这会让应用内容延伸到状态栏和导航栏区域，实现沉浸式体验
        enableEdgeToEdge()
        
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
