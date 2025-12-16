package com.wjp.jellyfinduanju.di

import android.content.Context
import com.wjp.jellyfinduanju.data.local.AppDataStore
import com.wjp.jellyfinduanju.data.local.DownloadDataStore
import com.wjp.jellyfinduanju.data.local.EpisodeDataStore
import com.wjp.jellyfinduanju.data.local.SeriesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DataStoreModule - Hilt 模块：提供数据存储相关的依赖
 * 
 * 职责：创建和提供底层数据存储对象（内存缓存、本地存储等）
 * 
 * ========== 注解说明 ==========
 * 
 * @Module
 * - 作用：标记这是 Hilt 的依赖提供模块
 * - 类比 Spring：相当于 @Configuration
 * - Hilt 会扫描这个类，找到所有 @Provides 方法
 * 
 * @InstallIn(SingletonComponent::class)
 * - 作用：指定依赖的作用域/生命周期
 * - SingletonComponent：应用级单例，整个 App 生命周期内只有一个实例
 * - 类比 Spring：相当于 @ApplicationScope
 * - 其他选项：ActivityComponent（Activity 级）、ViewModelComponent（ViewModel 级）
 * 
 * @Provides
 * - 作用：标记"这个方法可以提供依赖对象"
 * - 类比 Spring：相当于 @Bean
 * - Hilt 会调用这些方法来创建依赖实例
 * 
 * @Singleton
 * - 作用：确保全局只创建一次，后续都复用同一个实例
 * - 类比 Spring：相当于 @Singleton
 * - 配合 @InstallIn(SingletonComponent::class) 使用
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    
    /**
     * 提供 AppDataStore
     * 用于存储应用配置、登录信息等
     * 
     * @ApplicationContext 注解：
     * - 告诉 Hilt 注入的是 Application 级别的 Context
     * - 而不是 Activity 的 Context，避免内存泄漏
     * - Application Context 的生命周期和 App 一致
     */
    @Provides
    @Singleton
    fun provideAppDataStore(@ApplicationContext context: Context): AppDataStore {
        return AppDataStore(context)
    }
    
    /**
     * 提供 SeriesDataStore
     * 用于缓存剧集列表数据
     */
    @Provides
    @Singleton
    fun provideSeriesDataStore(): SeriesDataStore {
        return SeriesDataStore()
    }
    
    /**
     * 提供 EpisodeDataStore
     * 用于缓存季和剧集详情数据
     */
    @Provides
    @Singleton
    fun provideEpisodeDataStore(): EpisodeDataStore {
        return EpisodeDataStore()
    }
    
    /**
     * 提供 DownloadDataStore
     * 用于存储下载任务状态
     */
    @Provides
    @Singleton
    fun provideDownloadDataStore(): DownloadDataStore {
        return DownloadDataStore()
    }
}

