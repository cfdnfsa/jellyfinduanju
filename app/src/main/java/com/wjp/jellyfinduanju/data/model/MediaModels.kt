package com.wjp.jellyfinduanju.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * MediaModels.kt - 媒体相关的数据模型
 * 
 * 定义 Jellyfin 媒体内容（剧集、视频等）的数据结构
 */

// ==================== API 响应模型 ====================

/**
 * 媒体项列表响应
 * 
 * 对应 Jellyfin API: GET /Users/{userId}/Items
 * 
 * @property items 媒体项列表
 * @property totalRecordCount 总记录数
 * @property startIndex 起始索引
 */
@Serializable
data class ItemsResponse(
    @SerialName("Items")
    val items: List<MediaItem> = emptyList(),
    
    @SerialName("TotalRecordCount")
    val totalRecordCount: Int = 0,
    
    @SerialName("StartIndex")
    val startIndex: Int = 0
)

/**
 * 媒体项（剧集/电影/视频等）
 * 
 * Jellyfin 中的基本媒体单位
 * 
 * @property id 唯一标识符
 * @property name 名称/标题
 * @property serverId 所属服务器 ID
 * @property type 类型（Series, Movie, Episode 等）
 * @property overview 简介/描述
 * @property productionYear 制作年份
 * @property imageTags 图片标签（用于构建图片 URL）
 * @property backdropImageTags 背景图片标签
 * @property primaryImageAspectRatio 主图片宽高比
 * @property userData 用户相关数据（播放进度等）
 * @property runTimeTicks 运行时长（单位：ticks，1秒 = 10,000,000 ticks）
 * @property childCount 子项数量（如剧集数）
 */
@Serializable
data class MediaItem(
    @SerialName("Id")
    val id: String,
    
    @SerialName("Name")
    val name: String,
    
    @SerialName("ServerId")
    val serverId: String? = null,
    
    @SerialName("Type")
    val type: String? = null,
    
    @SerialName("Overview")
    val overview: String? = null,
    
    @SerialName("ProductionYear")
    val productionYear: Int? = null,
    
    @SerialName("ImageTags")
    val imageTags: ImageTags? = null,
    
    @SerialName("BackdropImageTags")
    val backdropImageTags: List<String>? = null,
    
    @SerialName("PrimaryImageAspectRatio")
    val primaryImageAspectRatio: Double? = null,
    
    @SerialName("UserData")
    val userData: UserItemData? = null,
    
    @SerialName("RunTimeTicks")
    val runTimeTicks: Long? = null,
    
    @SerialName("ChildCount")
    val childCount: Int? = null,
    
    @SerialName("RecursiveItemCount")
    val recursiveItemCount: Int? = null,
    
    @SerialName("SeriesName")
    val seriesName: String? = null,
    
    @SerialName("SeriesId")
    val seriesId: String? = null,
    
    @SerialName("IndexNumber")
    val indexNumber: Int? = null,
    
    @SerialName("ParentIndexNumber")
    val parentIndexNumber: Int? = null
)

/**
 * 图片标签
 * 
 * 用于构建图片 URL
 * 格式：/Items/{itemId}/Images/{imageType}?tag={tag}
 */
@Serializable
data class ImageTags(
    @SerialName("Primary")
    val primary: String? = null,
    
    @SerialName("Thumb")
    val thumb: String? = null,
    
    @SerialName("Banner")
    val banner: String? = null,
    
    @SerialName("Logo")
    val logo: String? = null
)

/**
 * 用户项数据
 * 
 * 包含用户对该媒体项的个人数据
 * 
 * @property playbackPositionTicks 播放位置（ticks）
 * @property playCount 播放次数
 * @property isFavorite 是否收藏
 * @property played 是否已播放完成
 * @property lastPlayedDate 最后播放时间
 */
@Serializable
data class UserItemData(
    @SerialName("PlaybackPositionTicks")
    val playbackPositionTicks: Long = 0,
    
    @SerialName("PlayCount")
    val playCount: Int = 0,
    
    @SerialName("IsFavorite")
    val isFavorite: Boolean = false,
    
    @SerialName("Played")
    val played: Boolean = false,
    
    @SerialName("LastPlayedDate")
    val lastPlayedDate: String? = null
)

// ==================== 本地存储模型 ====================

/**
 * 剧集本地缓存数据
 * 
 * 存储在 MMKV 中的剧集信息
 * 后续可扩展用于观看记录、下载状态等
 * 
 * @property id 剧集唯一标识（来自 Jellyfin）
 * @property name 剧集名称
 * @property overview 剧集简介
 * @property type 类型
 * @property productionYear 制作年份
 * @property primaryImageTag 主图片标签（用于构建本地图片路径）
 * @property localImagePath 本地缓存的图片路径
 * @property seasonCount 季数（ChildCount）
 * @property episodeCount 总集数（RecursiveItemCount）
 * @property lastSyncTime 最后同步时间（毫秒时间戳）
 * @property isVideoCached 是否已缓存视频
 * @property isDeletedFromServer 是否已从服务器删除（本地有缓存但服务器已删除）
 * @property played 是否已看完（来自云端）
 * @property lastWatchTime 最后观看时间（毫秒时间戳，来自云端）
 * @property lastWatchPositionTicks 最后观看位置（ticks，来自云端）
 */
@Serializable
data class SeriesLocalData(
    val id: String,
    val name: String,
    val overview: String? = null,
    val type: String? = null,
    val productionYear: Int? = null,
    val primaryImageTag: String? = null,
    val localImagePath: String? = null,
    val seasonCount: Int? = null,
    val episodeCount: Int? = null,
    val lastSyncTime: Long = 0,
    val isVideoCached: Boolean = false,
    val isDeletedFromServer: Boolean = false,
    val played: Boolean = false,
    val lastWatchTime: Long = 0,
    val lastWatchPositionTicks: Long = 0
) {
    companion object {
        /**
         * 从 API 响应的 MediaItem 创建本地数据
         */
        fun fromMediaItem(item: MediaItem): SeriesLocalData {
            return SeriesLocalData(
                id = item.id,
                name = item.name,
                overview = item.overview,
                type = item.type,
                productionYear = item.productionYear,
                primaryImageTag = item.imageTags?.primary,
                seasonCount = item.childCount,
                episodeCount = item.recursiveItemCount,
                lastSyncTime = System.currentTimeMillis()
            )
        }
    }
}

/**
 * 季本地数据
 * 
 * 存储单个季的信息
 * 
 * @property id 季唯一标识
 * @property seriesId 所属剧集 ID
 * @property name 季名称（如 "第1季"）
 * @property seasonNumber 季编号（1, 2, 3...）
 * @property episodeCount 该季的集数
 * @property primaryImageTag 封面图片标签
 * @property overview 季简介
 */
@Serializable
data class SeasonLocalData(
    val id: String,
    val seriesId: String,
    val name: String,
    val seasonNumber: Int = 0,
    val episodeCount: Int = 0,
    val primaryImageTag: String? = null,
    val overview: String? = null
) {
    companion object {
        /**
         * 从 API 响应的 MediaItem 创建季数据
         */
        fun fromMediaItem(item: MediaItem, seriesId: String): SeasonLocalData {
            return SeasonLocalData(
                id = item.id,
                seriesId = seriesId,
                name = item.name,
                seasonNumber = item.indexNumber ?: 0,
                episodeCount = item.childCount ?: 0,
                primaryImageTag = item.imageTags?.primary,
                overview = item.overview
            )
        }
    }
}

/**
 * 单集本地数据
 * 
 * 存储单个剧集的详细信息
 * 
 * @property id 剧集唯一标识
 * @property seriesId 所属剧集 ID
 * @property seasonId 所属季 ID
 * @property name 剧集名称
 * @property episodeNumber 集编号（1, 2, 3...）
 * @property seasonNumber 所属季编号
 * @property overview 剧集简介
 * @property runTimeTicks 时长（ticks）
 * @property primaryImageTag 封面图片标签
 * @property played 是否已看完
 * @property playbackPositionTicks 播放进度（ticks）
 * @property playCount 播放次数
 * @property lastPlayedDate 最后播放时间（毫秒时间戳）
 * @property isCached 是否已缓存视频
 * @property localVideoPath 本地视频路径
 */
@Serializable
data class EpisodeLocalData(
    val id: String,
    val seriesId: String,
    val seasonId: String,
    val name: String,
    val episodeNumber: Int = 0,
    val seasonNumber: Int = 0,
    val overview: String? = null,
    val runTimeTicks: Long = 0,
    val primaryImageTag: String? = null,
    val played: Boolean = false,
    val playbackPositionTicks: Long = 0,
    val playCount: Int = 0,
    val lastPlayedDate: Long = 0,
    val isCached: Boolean = false,
    val localVideoPath: String? = null
) {
    /**
     * 计算播放进度百分比
     * @return 0-100 的进度值，如果没有时长信息则返回 0
     */
    fun getProgressPercent(): Int {
        if (runTimeTicks <= 0) return 0
        return ((playbackPositionTicks.toDouble() / runTimeTicks) * 100).toInt().coerceIn(0, 100)
    }
    
    /**
     * 格式化进度显示
     * @return 进度文本，如 "80%" 或 "--"
     */
    fun getProgressText(): String {
        return if (playbackPositionTicks > 0) {
            "${getProgressPercent()}%"
        } else {
            "--"
        }
    }
    
    companion object {
        /**
         * 从 API 响应的 MediaItem 创建剧集数据
         */
        fun fromMediaItem(item: MediaItem, seriesId: String): EpisodeLocalData {
            return EpisodeLocalData(
                id = item.id,
                seriesId = seriesId,
                seasonId = item.seriesId ?: "", // Jellyfin 返回的 SeasonId 在其他字段
                name = item.name,
                episodeNumber = item.indexNumber ?: 0,
                seasonNumber = item.parentIndexNumber ?: 0,
                overview = item.overview,
                runTimeTicks = item.runTimeTicks ?: 0,
                primaryImageTag = item.imageTags?.primary,
                played = item.userData?.played ?: false,
                playbackPositionTicks = item.userData?.playbackPositionTicks ?: 0,
                playCount = item.userData?.playCount ?: 0,
                lastPlayedDate = 0 // 需要解析 item.userData?.lastPlayedDate
            )
        }
    }
}

