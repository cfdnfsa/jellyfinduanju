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
 * 提供在 Composable 中获取 Hilt 依赖的方法
 * 
 * ========== 设计理念 ==========
 * 
 * 这些函数不包含 remember 逻辑，原因：
 * 1. 让调用方明确看到 remember 的使用
 * 2. 更透明，符合 Compose 的显式原则
 * 3. 调用方可以根据需要决定是否缓存
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
 *     val repo = remember { getAuthRepository() }  // 正确
 * }
 * 
 * // ❌ 错误：在普通函数中调用
 * fun loadData() {
 *     val repo = getAuthRepository()  // 编译错误！
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
 *     // 显式使用 remember，让缓存逻辑清晰可见
 *     val authRepository = remember { getAuthRepository() }
 *     
 *     // 使用 authRepository
 *     val userSession by authRepository.userSession.collectAsState(initial = null)
 * }
 * ```
 * 
 * 优点：
 * 1. 透明：调用方明确看到 remember 缓存
 * 2. 灵活：可以根据需要传递 keys 给 remember
 * 3. 统一：所有 EntryPoint 访问逻辑集中管理
 * 4. 类型安全：编译时检查，避免运行时错误
 */

/**
 * 在 Composable 中获取 AuthRepository
 * 
 * 作用：提供用户认证、会话管理等功能
 * 
 * 注意：
 * - 这个函数每次调用都会从 EntryPoint 获取依赖
 * - 建议在调用处使用 remember { } 缓存结果
 * - 这样可以让调用方明确看到缓存逻辑
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
 *     // 显式使用 remember 缓存，让代码更清晰
 *     val authRepository = remember { getAuthRepository() }
 *     val userSession by authRepository.userSession.collectAsState(initial = null)
 * }
 * ```
 * 
 * @return AuthRepository 单例实例
 */
@Composable
fun getAuthRepository(): AuthRepository {
    // LocalContext.current - 只能在 @Composable 中访问
    val context = LocalContext.current
    
    // 通过 EntryPoint 从 Hilt 容器获取依赖
    // 类似于 Spring 的 applicationContext.getBean(AuthRepository.class)
    return EntryPointAccessors.fromApplication(
        context.applicationContext,
        GlobalEntryPoint::class.java
    ).authRepository()
}

/**
 * 在 Composable 中获取 SeriesDataStore
 * 
 * 作用：提供本地剧集数据的读写功能
 * 
 * 使用场景：
 * - MainNavHost：根据 seriesId 获取剧集信息
 * - DetailScreen：显示剧集详情
 * - PlayerScreen：获取播放信息
 * 
 * 使用示例：
 * ```
 * @Composable
 * fun MyScreen() {
 *     val seriesDataStore = remember { getSeriesDataStore() }
 *     // 使用 seriesDataStore
 * }
 * ```
 * 
 * @return SeriesDataStore 单例实例
 */
@Composable
fun getSeriesDataStore(): SeriesDataStore {
    val context = LocalContext.current
    return EntryPointAccessors.fromApplication(
        context.applicationContext,
        GlobalEntryPoint::class.java
    ).seriesDataStore()
}

// ========== 扩展指南 ==========
//
// 未来如果需要添加新的依赖，只需：
// 1. 在 AppEntryPoints.kt 的 GlobalEntryPoint 中声明方法
// 2. 在这里添加对应的 get* Composable 函数
//
// 例如：
// @Composable  // ← 必须添加此注解
// fun getDownloadManager(): DownloadManager {
//     val context = LocalContext.current  // 需要 @Composable
//     return EntryPointAccessors.fromApplication(
//         context.applicationContext,
//         GlobalEntryPoint::class.java
//     ).downloadManager()
// }
//
// 使用时：
// @Composable
// fun MyScreen() {
//     val downloadManager = remember { getDownloadManager() }
//     // 使用 downloadManager
// }
