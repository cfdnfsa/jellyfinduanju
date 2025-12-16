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
 * 为什么 remember 在函数内部而不是调用处？
 * 
 * 技术原因：
 * - remember { } 的 lambda 不是 @Composable 上下文
 * - LocalContext.current 只能在 @Composable 中调用
 * - 因此无法写成：remember { getAuthRepository() }
 * 
 * 正确方式：
 * ```
 * @Composable
 * fun getAuthRepository(): AuthRepository {
 *     val context = LocalContext.current  // ← 必须在 @Composable 中
 *     return remember {                    // ← remember 包裹获取逻辑
 *         EntryPointAccessors.fromApplication(...)
 *     }
 * }
 * ```
 * 
 * 错误方式：
 * ```
 * val repo = remember { getAuthRepository() }  // ❌ 编译错误！
 * // 因为 getAuthRepository() 内部有 LocalContext.current
 * // 但 remember { } 不是 @Composable 上下文
 * ```
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
 *     val repo = getAuthRepository()  // 正确
 * }
 * 
 * // ❌ 错误：在普通函数中调用
 * fun loadData() {
 *     val repo = getAuthRepository()  // 编译错误！
 * }
 * 
 * // ❌ 错误：在 remember 中调用
 * @Composable
 * fun MyScreen() {
 *     val repo = remember { getAuthRepository() }  // 编译错误！
 *     // 因为 remember { } 不是 @Composable 上下文
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
 *     // 直接调用即可，内部已缓存
 *     val authRepository = getAuthRepository()
 *     
 *     // 使用 authRepository
 *     val userSession by authRepository.userSession.collectAsState(initial = null)
 * }
 * ```
 * 
 * 优点：
 * 1. 简洁：一行代码获取依赖
 * 2. 性能：内部已使用 remember 缓存
 * 3. 统一：所有 EntryPoint 访问逻辑集中管理
 * 4. 类型安全：编译时检查，避免运行时错误
 */

/**
 * 在 Composable 中获取 AuthRepository（带缓存）
 * 
 * 作用：提供用户认证、会话管理等功能
 * 
 * 设计说明：
 * - 内部已使用 remember 缓存，调用方不需要再包装
 * - 这样既能看到是获取依赖的操作，又有缓存优化
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
 *     // 直接调用，已内置 remember 缓存
 *     val authRepository = getAuthRepository()
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
    
    // 使用 remember 缓存，避免每次重组都重新获取
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
 * 在 Composable 中获取 SeriesDataStore（带缓存）
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
 *     // 直接调用，已内置 remember 缓存
 *     val seriesDataStore = getSeriesDataStore()
 *     // 使用 seriesDataStore
 * }
 * ```
 * 
 * @return SeriesDataStore 单例实例
 */
@Composable
fun getSeriesDataStore(): SeriesDataStore {
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
// 2. 在这里添加对应的 get* Composable 函数
//
// 例如：
// @Composable  // ← 必须添加此注解
// fun getDownloadManager(): DownloadManager {
//     val context = LocalContext.current  // 需要 @Composable
//     return remember {                    // ← remember 必须在函数内部
//         EntryPointAccessors.fromApplication(
//             context.applicationContext,
//             GlobalEntryPoint::class.java
//         ).downloadManager()
//     }
// }
//
// 使用时：
// @Composable
// fun MyScreen() {
//     val downloadManager = getDownloadManager()  // 直接调用即可
//     // 使用 downloadManager
// }
