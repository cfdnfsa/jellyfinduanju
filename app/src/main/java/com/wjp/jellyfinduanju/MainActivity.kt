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
 * 职责：
 * 1. 作为 Compose UI 的宿主容器
 * 2. 配置系统 UI（状态栏、导航栏的沉浸式显示）
 * 3. 作为 Hilt 依赖注入的入口点
 * 
 * 技术选型：
 * - 继承 ComponentActivity：Compose 推荐的轻量级基类，提供生命周期、ViewModel 支持
 * - @AndroidEntryPoint：Hilt 注解，支持依赖注入和 ViewModel 自动注入
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    /**
     * onCreate - Activity 创建时的回调
     * @param savedInstanceState 用于恢复状态（首次创建为 null，屏幕旋转等情况会有值）
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ========== 第一步：启用边到边显示（Edge-to-Edge） ==========
        // 
        // enableEdgeToEdge() 是 Android 15 (API 35+) 引入的新 API
        // 作用：让应用内容可以绘制在状态栏和导航栏的下方，实现沉浸式体验
        // 
        // 具体效果：
        // - 状态栏（顶部）：显示时间、电量等系统信息的区域
        // - 导航栏（底部）：显示返回、主页、多任务按钮的区域
        // - 应用内容可以延伸到这些区域下方，创造无边界的视觉效果
        // 
        // 注意事项：
        // - 启用后需要自己处理系统栏的内边距（padding），避免内容被遮挡
        // - Compose 中可以使用 WindowInsets 来处理这些间距
        enableEdgeToEdge()
        
        // 配置系统栏显示行为
        window?.let { window ->
            // 设置窗口不自动适配系统栏（false = 允许内容延伸到系统栏下方）
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // --- 2.2 获取窗口插入控制器 ---
            // 
            // WindowInsetsController 用于控制系统栏（状态栏、导航栏）的行为
            // getInsetsController 参数：
            // - window: 窗口对象
            // - window.decorView: 窗口的根视图，系统栏就附加在这个视图上
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            
            // --- 2.3 隐藏导航栏 ---
            // 
            // controller.hide() 用于隐藏指定类型的系统栏
            // WindowInsetsCompat.Type.navigationBars() 指定要隐藏的是导航栏
            // 
            // 系统栏类型：
            // - statusBars(): 状态栏（顶部）
            // - navigationBars(): 导航栏（底部）
            // - systemBars(): 状态栏 + 导航栏
            // - ime(): 输入法（软键盘）
            // 
            // 为什么只隐藏导航栏而不隐藏状态栏？
            // - 状态栏显示时间、电量等重要信息，完全隐藏影响用户体验
            // - 导航栏隐藏后可以增加显示区域，尤其适合视频播放等场景
            // - 现代 Android 设备多使用手势导航，不需要一直显示导航栏
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            
            // --- 2.4 设置系统栏的行为模式 ---
            // 
            // systemBarsBehavior 控制系统栏隐藏后如何重新显示
            // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE 的含义：
            // 
            // - TRANSIENT（暂时的）：系统栏显示后会自动隐藏
            // - BY_SWIPE（通过滑动）：用户从屏幕边缘滑动可以临时显示系统栏
            // - 显示后几秒钟会自动隐藏，不会一直停留
            // 
            // 其他可选行为：
            // - BEHAVIOR_DEFAULT: 默认行为，触摸屏幕任意位置都会显示系统栏
            // - BEHAVIOR_SHOW_BARS_BY_TOUCH: 触摸屏幕显示系统栏，但不会自动隐藏
            // - BEHAVIOR_SHOW_BARS_BY_SWIPE: 滑动显示，但不会自动隐藏
            // 
            // 为什么选择 TRANSIENT 模式？
            // - 保持沉浸式体验，系统栏不会长时间占用屏幕空间
            // - 用户需要时可以通过手势召唤出来
            // - 适合视频播放、阅读等需要最大化内容区域的应用
            controller.systemBarsBehavior = 
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        // ========== 第三步：设置 Compose UI 内容 ==========
        // 
        // setContent 是 ComponentActivity 的扩展函数
        // 作用：将 Compose UI 树附加到这个 Activity 的窗口上
        // 
        // 工作原理：
        // 1. 创建一个 ComposeView 作为 Activity 的 content view
        // 2. 在 ComposeView 中设置 Composition（Compose UI 树）
        // 3. Composition 会监听状态变化并自动重组（Recomposition）
        // 
        // 注意：
        // - setContent 只能调用一次
        // - 在 setContent 的 lambda 中定义的是整个应用的 UI 树
        // - 这个 lambda 在初始化时执行一次，之后通过重组机制更新 UI
        setContent {
            // --- 3.1 应用主题包装 ---
            // 
            // JellyfinDuanjuTheme 是自定义的主题 Composable 函数
            // 主题的作用：
            // - 定义颜色方案（Color Scheme）：主色、次要色、背景色等
            // - 定义字体排版（Typography）：标题、正文的字体大小和样式
            // - 定义形状（Shapes）：按钮、卡片等组件的圆角大小
            // - 为整个应用提供统一的视觉风格
            // 
            // Material Design 3 主题系统：
            // - 支持动态颜色（Dynamic Color）：从壁纸提取颜色
            // - 支持深色模式自动切换
            // - 主题中定义的样式会自动应用到所有 Material 组件
            // 
            // 子组件访问主题：
            // - MaterialTheme.colorScheme.primary 获取主色
            // - MaterialTheme.typography.bodyLarge 获取正文字体样式
            // - MaterialTheme.shapes.medium 获取中等圆角形状
            JellyfinDuanjuTheme {
                // --- 3.2 使用 Surface 作为根容器 ---
                // 
                // Surface 是 Material Design 的基础容器组件
                // 功能：
                // - 提供背景色（基于 Material Design 的色彩系统）
                // - 提供阴影（elevation）支持
                // - 提供形状（shape）支持
                // - 处理点击涟漪效果
                // 
                // 为什么使用 Surface：
                // - Material Design 规范建议使用 Surface 作为内容的容器
                // - 它会根据 elevation 自动调整背景色（深色模式下尤为明显）
                // - 为子组件提供正确的颜色对比度
                Surface(
                    // Modifier.fillMaxSize() 修饰符
                    // 作用：让 Surface 填满整个父容器（即整个屏幕）
                    // 
                    // Modifier 是 Compose 的核心概念：
                    // - 用于修改组件的外观、行为、布局
                    // - 可以链式调用：Modifier.fillMaxSize().padding(16.dp)
                    // - 顺序很重要：padding(16.dp).background(Color.Red) 
                    //   和 background(Color.Red).padding(16.dp) 效果不同
                    modifier = Modifier.fillMaxSize(),
                    
                    // color 参数设置 Surface 的背景色
                    // MaterialTheme.colorScheme.background：
                    // - 从当前主题中获取背景色
                    // - 深色模式和浅色模式会自动使用不同的颜色
                    // - 这是 Material Design 3 推荐的做法，确保视觉一致性
                    color = MaterialTheme.colorScheme.background
                ) {
                    // --- 3.3 加载主应用内容 ---
                    // 
                    // MainApp() 是应用的主 Composable 函数
                    // 它的职责通常包括：
                    // - 设置导航系统（如 NavHost）
                    // - 管理全局状态（如登录状态）
                    // - 协调不同页面之间的切换
                    // - 提供全局的 Scaffold（包含 TopBar、BottomBar 等）
                    // 
                    // MainApp 放在 Surface 内部的原因：
                    // - Surface 提供了正确的背景色和主题上下文
                    // - MainApp 中的所有组件都能正确继承主题样式
                    // - 符合 Material Design 的设计规范
                    MainApp()
                }
            }
        }

    }

}
