package com.wjp.jellyfinduanju

import android.app.Application
import android.content.Context
import com.tencent.mmkv.MMKV
import com.wjp.jellyfinduanju.data.local.DownloadDataStore
import com.wjp.jellyfinduanju.data.local.EpisodeDataStore
import com.wjp.jellyfinduanju.data.local.SeriesDataStore
import com.wjp.jellyfinduanju.data.settings.AppSettings
import com.wjp.jellyfinduanju.download.DownloadManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * JellyfinApp.kt - 自定义 Application 类
 * 
 * Application 是整个应用的全局入口，生命周期与应用进程相同
 * 
 * @HiltAndroidApp 注解：
 * - 让 Hilt 为应用生成依赖注入代码
 * - 这是使用 Hilt 的第一步
 * 
 * 用途：
 * 1. 初始化全局库（如 MMKV、AppSettings）
 * 2. 依赖注入由 Hilt 自动管理
 * 
 * 注意：
 * - 使用 Hilt 后，不再需要手动创建 Repository 和 DataStore
 * - Hilt 会根据模块定义自动注入依赖
 */
@HiltAndroidApp
class JellyfinApp : Application() {
    
    /**
     * 使用 @Inject 注解让 Hilt 自动注入这些依赖
     * 用于初始化 DownloadManager
     */
    @Inject
    lateinit var downloadDataStore: DownloadDataStore
    
    @Inject
    lateinit var episodeDataStore: EpisodeDataStore
    
    @Inject
    lateinit var seriesDataStore: SeriesDataStore
    
    /**
     * attachBaseContext 在 onCreate 之前调用，也在 Hilt 注入之前调用
     * 这是初始化 MMKV 的最佳时机
     */
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // 初始化 MMKV（必须在 Hilt 注入之前）
        MMKV.initialize(this)
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化 AppSettings（必须在 MMKV 之后）
        AppSettings.init(this)
        
        // 初始化下载管理器
        // 注意：此时 Hilt 已经注入了所需的 DataStore
        DownloadManager.init(
            dataStore = downloadDataStore,
            episodeStore = episodeDataStore,
            seriesStore = seriesDataStore
        )
    }
}

