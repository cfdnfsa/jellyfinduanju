package com.wjp.jellyfinduanju.data.settings

import android.content.Context
import android.os.Environment
import com.tencent.mmkv.MMKV
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * LocalFolderReference - 本地文件夹引用信息
 * 
 * 记录用户添加的本地文件夹，用于刷新和管理
 * 
 * @property uri 文件夹 URI（持久化权限）
 * @property displayName 显示名称（从路径提取）
 * @property addTime 添加时间
 * @property lastScanTime 最后扫描时间
 * @property seriesCount 该文件夹下的剧集数量
 * @property permissionStatus 权限状态：0=正常，-1=权限失效
 */
@Serializable
data class LocalFolderReference(
    val uri: String,
    val displayName: String,
    val addTime: Long,
    val lastScanTime: Long = 0,
    val seriesCount: Int = 0,
    val permissionStatus: Int = 0
)

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
    
    // JSON 序列化器
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
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
    
    // ==================== 列表排序设置 ====================
    
    /**
     * 列表排序类型
     * 默认：按更新时间降序
     */
    var listSortType: String
        get() = mmkv.decodeString(Keys.LIST_SORT_TYPE) ?: "UPDATE_TIME"
        set(value) {
            mmkv.encode(Keys.LIST_SORT_TYPE, value)
        }
    
    /**
     * 列表排序方向
     * true = 升序, false = 降序
     * 默认：降序（最新在前）
     */
    var listSortAscending: Boolean
        get() = mmkv.decodeBool(Keys.LIST_SORT_ASCENDING, false)
        set(value) {
            mmkv.encode(Keys.LIST_SORT_ASCENDING, value)
        }
    
    /**
     * 列表查看模式（云端/本地）
     * 
     * 默认：CLOUD（云端模式）
     */
    var viewMode: String
        get() = mmkv.decodeString(Keys.VIEW_MODE) ?: "CLOUD"
        set(value) {
            mmkv.encode(Keys.VIEW_MODE, value)
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
    
    // ==================== 本地文件夹管理 ====================
    
    /**
     * 本地文件夹引用列表
     * 存储为 JSON 数组
     */
    var localFolderReferences: List<LocalFolderReference>
        get() {
            val jsonString = mmkv.decodeString(Keys.LOCAL_FOLDER_REFERENCES) ?: return emptyList()
            return if (jsonString.isEmpty()) {
                emptyList()
            } else {
                try {
                    json.decodeFromString(jsonString)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
        }
        set(value) {
            val jsonString = json.encodeToString(value)
            mmkv.encode(Keys.LOCAL_FOLDER_REFERENCES, jsonString)
        }
    
    /**
     * 添加本地文件夹引用
     * 
     * @param uri 文件夹 URI
     * @param displayName 显示名称
     * @return 是否添加成功（如果已存在则返回 false）
     */
    fun addLocalFolderReference(uri: String, displayName: String): Boolean {
        val refs = localFolderReferences.toMutableList()
        
        // 检查是否已存在
        if (refs.any { it.uri == uri }) {
            return false
        }
        
        // 添加新引用
        refs.add(
            LocalFolderReference(
                uri = uri,
                displayName = displayName,
                addTime = System.currentTimeMillis(),
                lastScanTime = 0,
                seriesCount = 0,
                permissionStatus = 0
            )
        )
        
        localFolderReferences = refs
        return true
    }
    
    /**
     * 删除本地文件夹引用
     * 
     * @param uri 文件夹 URI
     */
    fun removeLocalFolderReference(uri: String) {
        localFolderReferences = localFolderReferences.filter { it.uri != uri }
    }
    
    /**
     * 更新文件夹引用信息
     * 
     * @param uri 文件夹 URI
     * @param update 更新函数
     */
    fun updateLocalFolderReference(uri: String, update: (LocalFolderReference) -> LocalFolderReference) {
        val refs = localFolderReferences.toMutableList()
        val index = refs.indexOfFirst { it.uri == uri }
        
        if (index != -1) {
            refs[index] = update(refs[index])
            localFolderReferences = refs
        }
    }
    
    /**
     * 获取单个文件夹引用
     * 
     * @param uri 文件夹 URI
     * @return 文件夹引用，如果不存在则返回 null
     */
    fun getLocalFolderReference(uri: String): LocalFolderReference? {
        return localFolderReferences.find { it.uri == uri }
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
        const val LIST_SORT_TYPE = "list_sort_type"
        const val LIST_SORT_ASCENDING = "list_sort_ascending"
        const val LOCAL_FOLDER_REFERENCES = "local_folder_references"
        const val VIEW_MODE = "view_mode"
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

