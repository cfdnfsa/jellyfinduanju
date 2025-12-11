package com.wjp.jellyfinduanju.data.settings

import android.content.Context
import android.os.Environment
import com.tencent.mmkv.MMKV
import java.io.File

/**
 * AppSettings - 应用设置数据类
 * 
 * 存储所有应用设置项，通过 MMKV 持久化到本地
 * 
 * 使用方式：
 * - 读取：AppSettings.imageCachePath
 * - 修改：AppSettings.imageCachePath = "/new/path"（会自动保存）
 * 
 * 初始化：
 * - 在 Application.onCreate() 中调用 AppSettings.init(context)
 */
object AppSettings {
    
    // MMKV 实例，用于持久化存储
    private lateinit var mmkv: MMKV
    
    // 默认缓存目录名
    private const val DEFAULT_IMAGE_CACHE_DIR = "image_cache"
    private const val DEFAULT_VIDEO_CACHE_DIR = "video_cache"
    
    // ==================== 默认值 ====================
    
    private lateinit var defaultImageCachePath: String
    private lateinit var defaultVideoCachePath: String
    
    // ==================== 初始化 ====================
    
    /**
     * 初始化 AppSettings
     * 
     * 必须在 Application.onCreate() 中调用
     * 
     * @param context 应用上下文
     */
    fun init(context: Context) {
        // 初始化 MMKV（使用单独的实例存储设置）
        mmkv = MMKV.mmkvWithID("app_settings")
        
        // 设置默认缓存路径（应用私有目录）
        val cacheDir = context.getExternalFilesDir(null) 
            ?: context.filesDir
        
        defaultImageCachePath = File(cacheDir, DEFAULT_IMAGE_CACHE_DIR).absolutePath
        defaultVideoCachePath = File(cacheDir, DEFAULT_VIDEO_CACHE_DIR).absolutePath
        
        // 确保默认目录存在
        ensureDirectoryExists(imageCachePath)
        ensureDirectoryExists(videoCachePath)
    }
    
    // ==================== 缓存设置 ====================
    
    /**
     * 图片缓存路径
     * 
     * 读取时：返回保存的路径，如果没有则返回默认路径
     * 写入时：自动保存到 MMKV
     */
    var imageCachePath: String
        get() = mmkv.decodeString(Keys.IMAGE_CACHE_PATH) ?: defaultImageCachePath
        set(value) {
            mmkv.encode(Keys.IMAGE_CACHE_PATH, value)
        }
    
    /**
     * 视频缓存路径
     */
    var videoCachePath: String
        get() = mmkv.decodeString(Keys.VIDEO_CACHE_PATH) ?: defaultVideoCachePath
        set(value) {
            mmkv.encode(Keys.VIDEO_CACHE_PATH, value)
        }
    
    /**
     * 最大并发下载数量
     * 默认为 1，表示同时只下载一个任务
     */
    var maxConcurrentDownloads: Int
        get() = mmkv.decodeInt(Keys.MAX_CONCURRENT_DOWNLOADS, 1)
        set(value) {
            mmkv.encode(Keys.MAX_CONCURRENT_DOWNLOADS, value.coerceIn(1, 5))
        }
    
    // ==================== 播放设置 ====================
    
    /**
     * 长按倍速
     * 默认 3.0x，可选 2.0/3.0/4.0/5.0
     */
    var longPressSpeed: Float
        get() = mmkv.decodeFloat(Keys.LONG_PRESS_SPEED, 3.0f)
        set(value) {
            mmkv.encode(Keys.LONG_PRESS_SPEED, value)
        }
    
    /**
     * 默认播放倍速
     * 默认 1.0x，可选 0.5/0.75/1.0/1.25/1.5/2.0
     */
    var defaultPlaybackSpeed: Float
        get() = mmkv.decodeFloat(Keys.DEFAULT_PLAYBACK_SPEED, 1.0f)
        set(value) {
            mmkv.encode(Keys.DEFAULT_PLAYBACK_SPEED, value)
        }
    
    /**
     * 自动播放下一集
     * 默认开启
     */
    var autoPlayNext: Boolean
        get() = mmkv.decodeBool(Keys.AUTO_PLAY_NEXT, true)
        set(value) {
            mmkv.encode(Keys.AUTO_PLAY_NEXT, value)
        }
    
    /**
     * 滑动切换阈值
     * 滑动超过页面多少百分比切换到下一页
     * 默认 0.2f（20%），可选 0.1/0.2/0.3/0.4/0.5
     */
    var swipeThreshold: Float
        get() = mmkv.decodeFloat(Keys.SWIPE_THRESHOLD, 0.2f)
        set(value) {
            mmkv.encode(Keys.SWIPE_THRESHOLD, value.coerceIn(0.1f, 0.5f))
        }
    
    /**
     * 进度上报间隔（秒）
     * 0 表示不上报，最小 10 秒
     * 默认 0（不上报）
     */
    var reportInterval: Int
        get() = mmkv.decodeInt(Keys.REPORT_INTERVAL, 0)
        set(value) {
            // 0 表示不上报，否则最小 10 秒
            val validValue = if (value == 0) 0 else value.coerceAtLeast(10)
            mmkv.encode(Keys.REPORT_INTERVAL, validValue)
        }
    
    // ==================== 自定义标签 ====================
    
    /**
     * 自定义标签列表
     * 用逗号分隔存储
     */
    var customTags: List<String>
        get() {
            val tagsString = mmkv.decodeString(Keys.CUSTOM_TAGS) ?: return emptyList()
            return if (tagsString.isEmpty()) emptyList() else tagsString.split(",")
        }
        set(value) {
            mmkv.encode(Keys.CUSTOM_TAGS, value.joinToString(","))
        }
    
    /**
     * 添加自定义标签
     */
    fun addCustomTag(tag: String) {
        if (tag.isNotBlank() && tag !in customTags) {
            customTags = customTags + tag.trim()
        }
    }
    
    /**
     * 删除自定义标签
     */
    fun removeCustomTag(tag: String) {
        customTags = customTags.filter { it != tag }
    }
    
    // ==================== MMKV 键名 ====================
    private object Keys {
        const val IMAGE_CACHE_PATH = "image_cache_path"
        const val VIDEO_CACHE_PATH = "video_cache_path"
        const val MAX_CONCURRENT_DOWNLOADS = "max_concurrent_downloads"
        const val LONG_PRESS_SPEED = "long_press_speed"
        const val DEFAULT_PLAYBACK_SPEED = "default_playback_speed"
        const val AUTO_PLAY_NEXT = "auto_play_next"
        const val SWIPE_THRESHOLD = "swipe_threshold"
        const val REPORT_INTERVAL = "report_interval"
        const val CUSTOM_TAGS = "custom_tags"
    }
    
    /**
     * 获取默认图片缓存路径
     */
    fun getDefaultImageCachePath(): String = defaultImageCachePath
    
    /**
     * 获取默认视频缓存路径
     */
    fun getDefaultVideoCachePath(): String = defaultVideoCachePath
    
    /**
     * 重置缓存路径为默认值
     */
    fun resetCachePathsToDefault() {
        mmkv.removeValueForKey(Keys.IMAGE_CACHE_PATH)
        mmkv.removeValueForKey(Keys.VIDEO_CACHE_PATH)
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 确保目录存在
     */
    private fun ensureDirectoryExists(path: String) {
        val dir = File(path)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }
    
    /**
     * 检查路径是否为默认路径
     */
    fun isDefaultImageCachePath(): Boolean {
        return imageCachePath == defaultImageCachePath
    }
    
    fun isDefaultVideoCachePath(): Boolean {
        return videoCachePath == defaultVideoCachePath
    }
}

