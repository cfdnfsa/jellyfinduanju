package com.wjp.jellyfinduanju.data.model

import kotlinx.serialization.Serializable

/**
 * DownloadModels.kt - 下载相关的数据模型
 */

// ==================== 下载任务状态 ====================

/**
 * 下载状态枚举
 */
enum class DownloadStatus {
    PENDING,      // 等待中（队列中）
    DOWNLOADING,  // 下载中
    PAUSED,       // 已暂停
    COMPLETED,    // 已完成
    FAILED        // 失败
}

/**
 * 下载任务
 * 
 * 表示一个视频下载任务
 * 
 * @property id 任务唯一标识（使用 episodeId）
 * @property seriesId 所属剧集 ID
 * @property seriesName 剧集名称
 * @property seasonId 所属季 ID
 * @property seasonNumber 季编号
 * @property episodeId 剧集 ID
 * @property episodeNumber 集编号
 * @property episodeName 集名称
 * @property totalBytes 文件总大小（字节）
 * @property downloadedBytes 已下载大小（字节）
 * @property status 下载状态
 * @property errorMessage 错误信息（失败时）
 * @property localPath 本地文件路径
 * @property videoUrl 视频流 URL
 * @property createTime 创建时间（毫秒时间戳）
 * @property updateTime 更新时间（毫秒时间戳）
 */
@Serializable
data class DownloadTask(
    val id: String,  // 使用 episodeId 作为唯一标识
    val seriesId: String,
    val seriesName: String,
    val seasonId: String,
    val seasonNumber: Int,
    val episodeId: String,
    val episodeNumber: Int,
    val episodeName: String,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val errorMessage: String? = null,
    val localPath: String? = null,
    val videoUrl: String? = null,
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
) {
    /**
     * 计算下载进度百分比
     * @return 0-100 的进度值
     */
    fun getProgressPercent(): Int {
        if (totalBytes <= 0) return 0
        return ((downloadedBytes.toDouble() / totalBytes) * 100).toInt().coerceIn(0, 100)
    }
    
    /**
     * 格式化已下载大小
     */
    fun getDownloadedSizeText(): String {
        return formatFileSize(downloadedBytes)
    }
    
    /**
     * 格式化总大小
     */
    fun getTotalSizeText(): String {
        return formatFileSize(totalBytes)
    }
    
    /**
     * 格式化进度文本
     */
    fun getProgressText(): String {
        return "${getDownloadedSizeText()} / ${getTotalSizeText()}"
    }
    
    companion object {
        /**
         * 从 EpisodeLocalData 创建下载任务
         */
        fun fromEpisode(
            episode: EpisodeLocalData,
            seriesName: String,
            localPath: String
        ): DownloadTask {
            return DownloadTask(
                id = episode.id,
                seriesId = episode.seriesId,
                seriesName = seriesName,
                seasonId = episode.seasonId,
                seasonNumber = episode.seasonNumber,
                episodeId = episode.id,
                episodeNumber = episode.episodeNumber,
                episodeName = episode.name,
                localPath = localPath
            )
        }
        
        /**
         * 格式化文件大小
         */
        fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
                else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
            }
        }
    }
}

/**
 * 已缓存的剧集信息
 * 
 * 用于下载页面展示已完成的剧集
 * 
 * @property seriesId 剧集 ID
 * @property seriesName 剧集名称
 * @property primaryImageTag 封面图片标签
 * @property cachedEpisodeCount 已缓存的集数
 * @property totalEpisodeCount 总集数
 * @property totalCacheSize 缓存总大小（字节）
 * @property isDeletedFromServer 是否已从服务器删除
 */
@Serializable
data class CachedSeriesInfo(
    val seriesId: String,
    val seriesName: String,
    val primaryImageTag: String? = null,
    val cachedEpisodeCount: Int = 0,
    val totalEpisodeCount: Int = 0,
    val totalCacheSize: Long = 0,
    val isDeletedFromServer: Boolean = false
) {
    /**
     * 格式化缓存大小
     */
    fun getCacheSizeText(): String {
        return DownloadTask.formatFileSize(totalCacheSize)
    }
    
    /**
     * 获取缓存进度文本
     */
    fun getCacheProgressText(): String {
        return "$cachedEpisodeCount/$totalEpisodeCount 集"
    }
}

