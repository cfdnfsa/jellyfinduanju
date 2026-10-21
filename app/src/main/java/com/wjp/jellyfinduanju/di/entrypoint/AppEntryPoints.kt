package com.wjp.jellyfinduanju.di.entrypoint

import com.wjp.jellyfinduanju.data.local.SeriesDataStore
import com.wjp.jellyfinduanju.data.repository.AuthRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * AppEntryPoints - 统一管理所有的 Hilt EntryPoint
 * 
 * 作用：集中定义在 Composable 中需要手动获取的依赖
 * 类比 Spring：相当于统一的 ApplicationContext 接口定义
 * 
 * 为什么需要 EntryPoint？
 * - Composable 函数不支持构造函数注入
 * - 通过 EntryPoint 可以手动从 Hilt 容器获取依赖
 * - 类似于 Spring 的 ApplicationContext.getBean()
 */

/**
 * GlobalEntryPoint - 全局依赖入口
 * 
 * 包含应用中所有需要在 Composable 中手动获取的依赖
 * 
 * ========== 注解说明 ==========
 * 
 * @EntryPoint 注解：
 * - 作用：告诉 Hilt "这是一个入口点接口"
 * - 标记后，Hilt 会为这个接口生成访问依赖的实现代码
 * - 类似于定义一个"获取依赖的窗口"
 * - 编译时生成：GlobalEntryPoint_HiltInternal 等辅助类
 * 
 * @InstallIn(SingletonComponent::class) 注解：
 * - 作用：指定依赖的生命周期作用域
 * - SingletonComponent：应用级别的单例（整个 App 生命周期内只有一个实例）
 * - 其他可选组件：
 *   · ActivityComponent：Activity 级别（Activity 销毁时依赖也销毁）
 *   · ViewModelComponent：ViewModel 级别
 *   · FragmentComponent：Fragment 级别
 * 
 * 工作原理（类比 Spring）：
 * 1. @EntryPoint = 定义获取 Bean 的接口
 * 2. @InstallIn = 指定 Bean 的作用域（@ApplicationScope / @RequestScope）
 * 3. Hilt 编译时生成实现代码
 * 4. 运行时通过 EntryPointAccessors 获取实例
 * 
 * 示例对比：
 * 
 * Spring 方式：
 * ```java
 * @Component
 * public class AuthRepository { }
 * 
 * // 获取 Bean
 * AuthRepository repo = applicationContext.getBean(AuthRepository.class);
 * ```
 * 
 * Hilt 方式（本文件）：
 * ```kotlin
 * @EntryPoint
 * @InstallIn(SingletonComponent::class)
 * interface GlobalEntryPoint {
 *     fun authRepository(): AuthRepository
 * }
 * 
 * // 获取依赖
 * val repo = EntryPointAccessors
 *     .fromApplication(context, GlobalEntryPoint::class.java)
 *     .authRepository()
 * ```
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface GlobalEntryPoint {
    
    /**
     * 获取认证仓库
     * 用于管理用户登录状态、会话等
     */
    fun authRepository(): AuthRepository
    
    /**
     * 获取剧集数据存储
     * 用于管理本地剧集数据
     */
    fun seriesDataStore(): SeriesDataStore
    
    // 未来如果需要在 Composable 中获取其他依赖，在这里添加即可
    // 例如：
    // fun downloadManager(): DownloadManager
    // fun settingsRepository(): SettingsRepository
}
