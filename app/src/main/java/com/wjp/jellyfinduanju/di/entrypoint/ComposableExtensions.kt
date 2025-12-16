package com.wjp.jellyfinduanju.di.entrypoint

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wjp.jellyfinduanju.data.local.SeriesDataStore
import com.wjp.jellyfinduanju.data.repository.AuthRepository
import dagger.hilt.android.EntryPointAccessors

/**
 * ComposableExtensions - Composable 依赖注入扩展函数
 * 
 * 提供优雅的方式在 Composable 中获取依赖，类似于 hiltViewModel()
 * 
 * ========== @Composable 注解说明 ==========
 * 
 * @Composable 注解的作用：
 * 
 * 1. 标识 UI 函数
 *    - 告诉 Compose 编译器："这个函数可以创建/管理 UI 或状态"
 *    - 只有标记了 @Composable 的函数才能调用其他 @Composable 函数
 * 
 * 2. 启用重组机制（Recomposition）
 *    - 当状态改变时，Compose 会自动重新执行这个函数
 *    - 实现响应式 UI：数据变化 → 自动更新界面
 * 
 * 3. 访问 Compose 特性
 *    - 可以使用 remember、LaunchedEffect、DisposableEffect 等
 *    - 可以读取 CompositionLocal（如 LocalContext、LocalDensity）
 * 
 * 使用规则：
 * 
 * ✅ 可以调用 @Composable 函数的地方：
 * - 其他 @Composable 函数内
 * - @Composable lambda 中（如 setContent { }）
 * 
 * ❌ 不能调用 @Composable 函数的地方：
 * - 普通函数中
 * - ViewModel 中
 * - 协程中（除非在 LaunchedEffect 内）
 * - 回调函数中（如 onClick = { }）
 * 
 * 示例：
 * ```
 * // ✅ 正确：在 @Composable 函数中调用
 * @Composable
 * fun MyScreen() {
 *     val repo = rememberAuthRepository()  // 正确
 * }
 * 
 * // ❌ 错误：在普通函数中调用
 * fun loadData() {
 *     val repo = rememberAuthRepository()  // 编译错误！
 * }
 * ```
 * 
 * 对比理解：
 * - @Composable ≠ 扩展函数（扩展函数是 fun Type.method()）
 * - @Composable = React 的函数组件 / Vue 的 setup()
 * - 函数被导入后，任何 @Composable 上下文都可以调用
 * 
 * ========================================
 * 
 * 使用示例：
 * ```
 * @Composable
 * fun MyScreen() {
 *     val authRepository = rememberAuthRepository()
 *     // 使用 authRepository
 * }
 * ```
 * 
 * 优点：
 * 1. 简洁：一行代码即可获取依赖
 * 2. 统一：所有 EntryPoint 访问逻辑集中管理
 * 3. 符合 Compose 规范：使用 remember* 命名规范
 * 4. 类型安全：编译时检查，避免运行时错误
 */

/**
 * 在 Composable 中获取 AuthRepository
 * 
 * 作用：提供用户认证、会话管理等功能
 * 
 * @Composable 注解的作用（本函数）：
 * 1. 允许访问 LocalContext（Compose 的上下文系统）
 * 2. 允许使用 remember（缓存结果，避免重组时重复创建）
 * 3. 只能在其他 @Composable 函数中调用此函数
 * 
 * 使用场景：
 * - MainApp：检查登录状态
 * - LoginScreen：执行登录操作
 * - SettingsScreen：退出登录
 * 
 * 使用示例：
 * ```
 * @Composable
 * fun MyScreen() {
 *     val authRepository = rememberAuthRepository()  // 只需一行
 *     val userSession by authRepository.userSession.collectAsState(initial = null)
 * }
 * ```
 * 
 * @return AuthRepository 单例实例
 */
@Composable
fun rememberAuthRepository(): AuthRepository {
    // LocalContext.current - 只能在 @Composable 中访问
    val context = LocalContext.current
    
    // remember { } - 只能在 @Composable 中使用
    // 作用：缓存计算结果，重组时不会重新创建
    return remember {
        // 通过 EntryPoint 从 Hilt 容器获取依赖
        // 类似于 Spring 的 applicationContext.getBean(AuthRepository.class)
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            GlobalEntryPoint::class.java
        ).authRepository()
    }
}

/**
 * 在 Composable 中获取 SeriesDataStore
 * 
 * 作用：提供本地剧集数据的读写功能
 * 
 * @Composable 注解的作用（本函数）：
 * 1. 允许访问 LocalContext
 * 2. 允许使用 remember 缓存结果
 * 3. 确保函数在 Compose UI 上下文中调用
 * 
 * 使用场景：
 * - MainNavHost：根据 seriesId 获取剧集信息
 * - DetailScreen：显示剧集详情
 * - PlayerScreen：获取播放信息
 * 
 * @return SeriesDataStore 单例实例
 */
@Composable
fun rememberSeriesDataStore(): SeriesDataStore {
    val context = LocalContext.current
    return remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            GlobalEntryPoint::class.java
        ).seriesDataStore()
    }
}

// ========== 扩展指南 ==========
//
// 未来如果需要添加新的依赖，只需：
// 1. 在 AppEntryPoints.kt 的 GlobalEntryPoint 中声明方法
// 2. 在这里添加对应的 remember* Composable 函数
//
// 例如：
// @Composable  // ← 必须添加此注解
// fun rememberDownloadManager(): DownloadManager {
//     val context = LocalContext.current  // 需要 @Composable
//     return remember {                    // 需要 @Composable
//         EntryPointAccessors.fromApplication(
//             context.applicationContext,
//             GlobalEntryPoint::class.java
//         ).downloadManager()
//     }
// }
//
// ========== 常见问题 Q&A ==========
//
// Q1: 为什么这些函数需要 @Composable 注解？
// A1: 因为函数内部使用了 LocalContext.current 和 remember
//     这两个特性只能在 @Composable 上下文中使用
//
// Q2: 可以在 ViewModel 中调用这些函数吗？
// A2: 不可以！ViewModel 不是 @Composable 上下文
//     ViewModel 应该使用构造函数注入：
//     class MyViewModel @Inject constructor(
//         private val authRepository: AuthRepository
//     ) : ViewModel()
//
// Q3: @Composable 函数和扩展函数有什么区别？
// A3: - @Composable：标记可以管理 UI/状态的函数
//     - 扩展函数：为类添加新方法（fun Type.method()）
//     - 两者是独立概念，可以同时使用
//
// Q4: 为什么文件名叫 ComposableExtensions？
// A4: 表示"为 Composable 开发提供的扩展工具函数集合"
//     不是指 Kotlin 的扩展函数，而是功能扩展
//
// Q5: 这些函数为什么用 remember 开头命名？
// A5: 符合 Compose 规范：
//     - remember*：缓存对象，避免重组时重复创建
//     - 类似的命名：rememberCoroutineScope、rememberUpdatedState
