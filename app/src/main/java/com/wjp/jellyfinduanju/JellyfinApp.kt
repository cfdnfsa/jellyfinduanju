package com.wjp.jellyfinduanju

import android.app.Application
import com.tencent.mmkv.MMKV
import com.wjp.jellyfinduanju.data.local.AppDataStore
import com.wjp.jellyfinduanju.data.local.DownloadDataStore
import com.wjp.jellyfinduanju.data.local.EpisodeDataStore
import com.wjp.jellyfinduanju.data.local.SeriesDataStore
import com.wjp.jellyfinduanju.data.repository.AuthRepository
import com.wjp.jellyfinduanju.data.repository.DetailRepository
import com.wjp.jellyfinduanju.data.repository.SeriesRepository
import com.wjp.jellyfinduanju.data.settings.AppSettings
import com.wjp.jellyfinduanju.download.DownloadManager

/**
 * JellyfinApp.kt - 自定义 Application 类
 * 
 * Application 是整个应用的全局入口，生命周期与应用进程相同
 * 
 * 用途：
 * 1. 初始化全局依赖（如 MMKV、AppSettings、Repository）
 * 2. 提供全局访问点（类似简单的依赖注入）
 * 
 * 注意：
 * - 这里使用简单的手动依赖注入
 * - 大型项目建议使用 Hilt/Koin 等依赖注入框架
 */
class JellyfinApp : Application() {
    
    // 使用 lazy 延迟初始化，首次访问时才创建
    // 这样可以避免 Application 启动时的性能开销
    
    /**
     * DataStore 实例
     * 用于持久化存储用户配置和登录状态
     */
    val dataStore: AppDataStore by lazy {
        AppDataStore(this)
    }
    
    /**
     * 剧集数据存储
     * 用于持久化存储剧集信息
     */
    val seriesDataStore: SeriesDataStore by lazy {
        SeriesDataStore()
    }
    
    /**
     * 剧集详情数据存储
     * 用于持久化存储季和剧集信息
     */
    val episodeDataStore: EpisodeDataStore by lazy {
        EpisodeDataStore()
    }
    
    /**
     * 下载任务数据存储
     * 用于持久化存储下载任务状态
     */
    val downloadDataStore: DownloadDataStore by lazy {
        DownloadDataStore()
    }
    
    /**
     * 认证仓库
     * 封装登录、登出等认证相关操作
     */
    val authRepository: AuthRepository by lazy {
        AuthRepository(dataStore)
    }
    
    /**
     * 剧集仓库
     * 封装剧集获取、缓存等操作
     */
    val seriesRepository: SeriesRepository by lazy {
        SeriesRepository(seriesDataStore, episodeDataStore, authRepository)
    }
    
    /**
     * 详情仓库
     * 封装剧集详情（季、集）的获取和操作
     */
    val detailRepository: DetailRepository by lazy {
        DetailRepository(authRepository, episodeDataStore)
    }
    
    companion object {
        /**
         * 全局 Application 实例
         * 
         * 使用方式：
         * val app = JellyfinApp.instance
         * val repo = app.authRepository
         */
        lateinit var instance: JellyfinApp
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 初始化 MMKV
        MMKV.initialize(this)
        
        // 初始化 AppSettings（必须在 MMKV 之后）
        AppSettings.init(this)
        
        // 初始化下载管理器
        DownloadManager.init(
            dataStore = downloadDataStore,
            episodeStore = episodeDataStore,
            seriesStore = seriesDataStore
        )
    }
}

