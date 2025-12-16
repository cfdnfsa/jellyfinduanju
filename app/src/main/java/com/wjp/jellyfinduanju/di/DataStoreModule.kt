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
 * @InstallIn(SingletonComponent::class) 
 * - 表示这些依赖的生命周期与应用相同（单例）
 * 
 * @Provides 标记提供依赖的方法
 * @Singleton 确保整个应用只有一个实例
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    
    /**
     * 提供 AppDataStore
     * 用于存储应用配置、登录信息等
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

