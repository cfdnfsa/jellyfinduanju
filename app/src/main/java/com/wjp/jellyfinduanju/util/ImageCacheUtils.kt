package com.wjp.jellyfinduanju.util

import com.wjp.jellyfinduanju.data.network.NetworkModule
import com.wjp.jellyfinduanju.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * ImageCacheUtils - 图片缓存工具类
 * 
 * 提供图片下载和缓存相关的操作
 * 
 * 图片存储结构：
 * {imageCachePath}/
 *   ├── {itemId}_primary.jpg
 *   ├── {itemId}_thumb.jpg
 *   └── ...
 */
object ImageCacheUtils {
    
    // 图片类型常量
    const val IMAGE_TYPE_PRIMARY = "Primary"
    const val IMAGE_TYPE_THUMB = "Thumb"
    const val IMAGE_TYPE_BACKDROP = "Backdrop"
    
    // 默认图片尺寸（用于缩放，减少存储空间）
    private const val DEFAULT_MAX_WIDTH = 300
    private const val DEFAULT_MAX_HEIGHT = 450
    
    /**
     * 获取本地图片路径
     * 
     * @param itemId 媒体项 ID
     * @param imageType 图片类型
     * @return 本地图片路径
     */
    fun getLocalImagePath(itemId: String, imageType: String = IMAGE_TYPE_PRIMARY): String {
        val fileName = "${itemId}_${imageType.lowercase()}.jpg"
        return File(AppSettings.imageCachePath, fileName).absolutePath
    }
    
    /**
     * 检查本地是否有缓存图片
     * 
     * @param itemId 媒体项 ID
     * @param imageType 图片类型
     * @return 是否存在
     */
    fun hasLocalImage(itemId: String, imageType: String = IMAGE_TYPE_PRIMARY): Boolean {
        val path = getLocalImagePath(itemId, imageType)
        return File(path).exists()
    }
    
    /**
     * 获取本地图片 File 对象
     * 
     * @param itemId 媒体项 ID
     * @param imageType 图片类型
     * @return File 对象，如果不存在返回 null
     */
    fun getLocalImageFile(itemId: String, imageType: String = IMAGE_TYPE_PRIMARY): File? {
        val path = getLocalImagePath(itemId, imageType)
        val file = File(path)
        return if (file.exists()) file else null
    }
    
    /**
     * 下载并缓存图片
     * 
     * @param serverUrl 服务器地址
     * @param itemId 媒体项 ID
     * @param imageType 图片类型
     * @param imageTag 图片标签（可选，用于验证）
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @return 成功返回本地路径，失败返回 null
     */
    suspend fun downloadAndCacheImage(
        serverUrl: String,
        itemId: String,
        imageType: String = IMAGE_TYPE_PRIMARY,
        imageTag: String? = null,
        maxWidth: Int = DEFAULT_MAX_WIDTH,
        maxHeight: Int = DEFAULT_MAX_HEIGHT
    ): String? = withContext(Dispatchers.IO) {
        try {
            // 确保缓存目录存在
            FileUtils.ensureDirectoryExists(AppSettings.imageCachePath)
            
            // 创建 API 客户端
            val api = NetworkModule.createJellyfinApi(serverUrl)
            
            // 下载图片
            val response = api.getItemImage(
                itemId = itemId,
                imageType = imageType,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                tag = imageTag
            )
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // 保存到本地
                    val localPath = getLocalImagePath(itemId, imageType)
                    val file = File(localPath)
                    
                    FileOutputStream(file).use { output ->
                        body.byteStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    
                    return@withContext localPath
                }
            }
            
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 删除本地缓存图片
     * 
     * @param itemId 媒体项 ID
     * @param imageType 图片类型，为 null 则删除该 item 的所有图片
     * @return 是否成功
     */
    fun deleteLocalImage(itemId: String, imageType: String? = null): Boolean {
        return try {
            if (imageType != null) {
                // 删除指定类型的图片
                val path = getLocalImagePath(itemId, imageType)
                File(path).delete()
            } else {
                // 删除该 item 的所有图片
                val cacheDir = File(AppSettings.imageCachePath)
                if (cacheDir.exists() && cacheDir.isDirectory) {
                    cacheDir.listFiles()?.filter { 
                        it.name.startsWith("${itemId}_") 
                    }?.forEach { 
                        it.delete() 
                    }
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 批量删除本地缓存图片
     * 
     * @param itemIds 媒体项 ID 列表
     * @return 删除的图片数量
     */
    fun deleteLocalImages(itemIds: List<String>): Int {
        var deletedCount = 0
        itemIds.forEach { itemId ->
            if (deleteLocalImage(itemId)) {
                deletedCount++
            }
        }
        return deletedCount
    }
    
    /**
     * 获取图片的远程 URL
     * 
     * @param serverUrl 服务器地址
     * @param itemId 媒体项 ID
     * @param imageType 图片类型
     * @param imageTag 图片标签
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @return 图片 URL
     */
    fun getRemoteImageUrl(
        serverUrl: String,
        itemId: String,
        imageType: String = IMAGE_TYPE_PRIMARY,
        imageTag: String? = null,
        maxWidth: Int? = DEFAULT_MAX_WIDTH,
        maxHeight: Int? = DEFAULT_MAX_HEIGHT
    ): String {
        val baseUrl = serverUrl.trimEnd('/')
        val params = mutableListOf<String>()
        
        maxWidth?.let { params.add("maxWidth=$it") }
        maxHeight?.let { params.add("maxHeight=$it") }
        imageTag?.let { params.add("tag=$it") }
        
        val queryString = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
        
        return "$baseUrl/Items/$itemId/Images/$imageType$queryString"
    }
    
    /**
     * 清除所有图片缓存
     * 
     * @return 是否成功
     */
    fun clearAllImageCache(): Boolean {
        return FileUtils.clearDirectory(AppSettings.imageCachePath)
    }
    
    /**
     * 获取图片缓存大小
     * 
     * @return 字节数
     */
    fun getImageCacheSize(): Long {
        return FileUtils.getDirectorySize(AppSettings.imageCachePath)
    }
    
    /**
     * 获取格式化的图片缓存大小
     * 
     * @return 格式化字符串
     */
    fun getFormattedImageCacheSize(): String {
        return FileUtils.formatFileSize(getImageCacheSize())
    }
}

