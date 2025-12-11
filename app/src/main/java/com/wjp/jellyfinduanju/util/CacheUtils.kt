package com.wjp.jellyfinduanju.util

import com.wjp.jellyfinduanju.data.settings.AppSettings

/**
 * CacheUtils - 缓存管理工具类
 * 
 * 提供缓存相关的便捷操作
 */
object CacheUtils {
    
    /**
     * 缓存信息数据类
     */
    data class CacheInfo(
        val imageCacheSize: Long,      // 图片缓存大小（字节）
        val videoCacheSize: Long,      // 视频缓存大小（字节）
        val imageFileCount: Int,       // 图片文件数量
        val videoFileCount: Int,       // 视频文件数量
        val totalSize: Long,           // 总大小
        val totalFileCount: Int        // 总文件数
    ) {
        /**
         * 获取格式化的图片缓存大小
         */
        fun getFormattedImageCacheSize(): String = FileUtils.formatFileSize(imageCacheSize)
        
        /**
         * 获取格式化的视频缓存大小
         */
        fun getFormattedVideoCacheSize(): String = FileUtils.formatFileSize(videoCacheSize)
        
        /**
         * 获取格式化的总缓存大小
         */
        fun getFormattedTotalSize(): String = FileUtils.formatFileSize(totalSize)
    }
    
    /**
     * 获取缓存信息
     * 
     * @return 缓存信息
     */
    fun getCacheInfo(): CacheInfo {
        val imageCacheSize = FileUtils.getDirectorySize(AppSettings.imageCachePath)
        val videoCacheSize = FileUtils.getDirectorySize(AppSettings.videoCachePath)
        val imageFileCount = FileUtils.getFileCount(AppSettings.imageCachePath)
        val videoFileCount = FileUtils.getFileCount(AppSettings.videoCachePath)
        
        return CacheInfo(
            imageCacheSize = imageCacheSize,
            videoCacheSize = videoCacheSize,
            imageFileCount = imageFileCount,
            videoFileCount = videoFileCount,
            totalSize = imageCacheSize + videoCacheSize,
            totalFileCount = imageFileCount + videoFileCount
        )
    }
    
    /**
     * 清除所有缓存
     * 
     * @return 是否成功
     */
    fun clearAllCache(): Boolean {
        val imageCleared = clearImageCache()
        val videoCleared = clearVideoCache()
        return imageCleared && videoCleared
    }
    
    /**
     * 清除图片缓存
     * 
     * @return 是否成功
     */
    fun clearImageCache(): Boolean {
        return FileUtils.clearDirectory(AppSettings.imageCachePath)
    }
    
    /**
     * 清除视频缓存
     * 
     * @return 是否成功
     */
    fun clearVideoCache(): Boolean {
        return FileUtils.clearDirectory(AppSettings.videoCachePath)
    }
    
    /**
     * 更新图片缓存路径
     * 
     * 如果路径发生变化，会清除旧路径的缓存
     * 
     * @param newPath 新路径
     * @param clearOldCache 是否清除旧缓存
     * @return 是否成功
     */
    fun updateImageCachePath(newPath: String, clearOldCache: Boolean = true): Boolean {
        val oldPath = AppSettings.imageCachePath
        
        // 路径没有变化
        if (oldPath == newPath) return true
        
        // 清除旧缓存
        if (clearOldCache) {
            FileUtils.clearDirectory(oldPath)
        }
        
        // 确保新目录存在
        FileUtils.ensureDirectoryExists(newPath)
        
        // 更新设置
        AppSettings.imageCachePath = newPath
        
        return true
    }
    
    /**
     * 更新视频缓存路径
     * 
     * @param newPath 新路径
     * @param clearOldCache 是否清除旧缓存
     * @return 是否成功
     */
    fun updateVideoCachePath(newPath: String, clearOldCache: Boolean = true): Boolean {
        val oldPath = AppSettings.videoCachePath
        
        // 路径没有变化
        if (oldPath == newPath) return true
        
        // 清除旧缓存
        if (clearOldCache) {
            FileUtils.clearDirectory(oldPath)
        }
        
        // 确保新目录存在
        FileUtils.ensureDirectoryExists(newPath)
        
        // 更新设置
        AppSettings.videoCachePath = newPath
        
        return true
    }
    
    /**
     * 确保缓存目录存在
     */
    fun ensureCacheDirectoriesExist() {
        FileUtils.ensureDirectoryExists(AppSettings.imageCachePath)
        FileUtils.ensureDirectoryExists(AppSettings.videoCachePath)
    }
}

