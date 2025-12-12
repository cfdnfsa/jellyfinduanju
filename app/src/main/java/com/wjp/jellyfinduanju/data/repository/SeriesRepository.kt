package com.wjp.jellyfinduanju.data.repository

import android.os.Build
import com.wjp.jellyfinduanju.data.local.EpisodeDataStore
import com.wjp.jellyfinduanju.data.local.SeriesDataStore
import com.wjp.jellyfinduanju.data.model.MediaItem
import com.wjp.jellyfinduanju.data.model.SeriesLocalData
import com.wjp.jellyfinduanju.data.network.JellyfinAuthHeader
import com.wjp.jellyfinduanju.data.network.NetworkModule
import com.wjp.jellyfinduanju.util.ImageCacheUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * SeriesRepository - 剧集数据仓库
 * 
 * 职责：
 * 1. 从 Jellyfin 服务器获取剧集列表
 * 2. 管理本地剧集数据缓存
 * 3. 下载和管理剧集图片
 * 4. 处理刷新逻辑（清理不存在的剧集）
 * 
 * @property seriesDataStore 剧集本地存储
 * @property episodeDataStore 剧集详情本地存储（季/集数据）
 * @property authRepository 认证仓库（获取服务器信息和用户信息）
 */
class SeriesRepository(
    private val seriesDataStore: SeriesDataStore,
    private val episodeDataStore: EpisodeDataStore,
    private val authRepository: AuthRepository
) {
    
    // ==================== 数据流 ====================
    
    /**
     * 剧集列表 Flow
     */
    val seriesListFlow: Flow<List<SeriesLocalData>> = seriesDataStore.seriesListFlow
    
    /**
     * 最后刷新时间 Flow
     */
    val lastRefreshTimeFlow: Flow<Long> = seriesDataStore.lastRefreshTimeFlow
    
    // ==================== 获取数据 ====================
    
    /**
     * 从服务器刷新剧集列表
     * 
     * 流程：
     * 1. 获取服务器配置和用户会话
     * 2. 调用 API 获取剧集列表
     * 3. 对比本地数据，删除不存在的剧集（如果没有视频缓存）
     * 4. 保存新数据到本地
     * 5. 下载剧集图片
     * 
     * @return 成功返回剧集列表，失败返回异常
     */
    suspend fun refreshSeriesList(): Result<List<SeriesLocalData>> = withContext(Dispatchers.IO) {
        try {
            // 获取服务器配置
            val serverConfig = authRepository.serverConfig.first()
                ?: return@withContext Result.failure(Exception("未配置服务器"))
            
            // 获取用户会话
            val userSession = authRepository.userSession.first()
                ?: return@withContext Result.failure(Exception("未登录"))
            
            // 获取设备信息
            val deviceId = authRepository.getDeviceId()
            val deviceName = getDeviceName()
            
            // 创建 API 客户端（使用当前激活的服务器地址）
            val api = authRepository.createActiveApi(serverConfig.serverUrl)
            
            // 构建认证头
            val authHeader = JellyfinAuthHeader.buildAuthHeader(
                deviceId = deviceId,
                deviceName = deviceName,
                token = userSession.accessToken
            )
            
            // 先获取媒体库列表
            val viewsResponse = api.getUserViews(
                authorization = authHeader,
                userId = userSession.userId
            )
            
            // 按媒体库分组获取 Series，这样可以准确知道每个 Series 属于哪个媒体库
            val allServerItems = mutableListOf<Pair<MediaItem, String?>>() // (item, 媒体库名称)
            
            if (viewsResponse.isSuccessful) {
                val views = viewsResponse.body()?.items ?: emptyList()
                
                // 为每个媒体库获取其中的 Series
                for (view in views) {
                    // 获取该媒体库下的所有 Series
                    val libraryResponse = api.getUserItems(
                        authorization = authHeader,
                        userId = userSession.userId,
                        includeItemTypes = "Series",
                        recursive = true,
                        sortBy = "SortName",
                        sortOrder = "Ascending",
                        fields = "Overview,PrimaryImageAspectRatio,ChildCount,RecursiveItemCount,UserData",
                        parentId = view.id
                    )
                    
                    if (libraryResponse.isSuccessful) {
                        val items = libraryResponse.body()?.items ?: emptyList()
                        items.forEach { item ->
                            allServerItems.add(item to view.name)
                        }
                    }
                }
            } else {
                // 降级：直接获取所有 Series（不带媒体库信息）
                val fallbackResponse = api.getUserItems(
                    authorization = authHeader,
                    userId = userSession.userId,
                    includeItemTypes = "Series",
                    recursive = true,
                    sortBy = "SortName",
                    sortOrder = "Ascending",
                    fields = "Overview,PrimaryImageAspectRatio,ChildCount,RecursiveItemCount,UserData"
                )
                if (fallbackResponse.isSuccessful) {
                    fallbackResponse.body()?.items?.forEach { item ->
                        allServerItems.add(item to null)
                    }
                }
            }
            
            if (allServerItems.isNotEmpty()) {
                val serverItemIds = allServerItems.map { it.first.id }.toSet()
                
                // 获取本地所有剧集 ID
                val localItemIds = seriesDataStore.getAllSeriesIds()
                
                // 找出服务器上不存在的剧集
                val itemsNotOnServer = localItemIds.filter { localId ->
                    localId !in serverItemIds
                }
                
                // ==================== 额外请求：获取所有 Episode 的观看状态 ====================
                
                // 1. 获取所有有播放进度的 Episode（正在看）
                val resumableEpisodesResponse = api.getUserItems(
                    authorization = authHeader,
                    userId = userSession.userId,
                    includeItemTypes = "Episode",
                    recursive = true,
                    fields = "SeriesId,UserData",
                    filters = "IsResumable"  // 有播放进度但未看完
                )
                
                // 2. 获取所有已看完的 Episode
                val playedEpisodesResponse = api.getUserItems(
                    authorization = authHeader,
                    userId = userSession.userId,
                    includeItemTypes = "Episode",
                    recursive = true,
                    fields = "SeriesId,UserData",
                    filters = "IsPlayed"  // 已看完
                )
                
                // 统计每个 Series 的观看状态
                val seriesWatchStatus = mutableMapOf<String, WatchStatus>()
                
                // 处理正在看的 Episode
                if (resumableEpisodesResponse.isSuccessful) {
                    val resumableEpisodes = resumableEpisodesResponse.body()?.items ?: emptyList()
                    resumableEpisodes.forEach { episode ->
                        episode.seriesId?.let { seriesId ->
                            seriesWatchStatus[seriesId] = WatchStatus.WATCHING
                        }
                    }
                }
                
                // 处理已看完的 Episode
                if (playedEpisodesResponse.isSuccessful) {
                    val playedEpisodes = playedEpisodesResponse.body()?.items ?: emptyList()
                    playedEpisodes.forEach { episode ->
                        episode.seriesId?.let { seriesId ->
                            // 如果已经标记为"正在看"，保持"正在看"状态
                            // 如果还没标记，则标记为"有看完的集"（也算正在看）
                            if (seriesId !in seriesWatchStatus) {
                                seriesWatchStatus[seriesId] = WatchStatus.WATCHING
                            }
                        }
                    }
                }
                // ==================== 额外请求结束 ===================="
                
                // 处理服务器上不存在的剧集
                itemsNotOnServer.forEach { itemId ->
                    val localSeries = seriesDataStore.getSeries(itemId)
                    if (localSeries != null) {
                        if (localSeries.isVideoCached) {
                            // 有视频缓存 → 标记为"已从服务器删除"
                            seriesDataStore.saveSeries(
                                localSeries.copy(isDeletedFromServer = true)
                            )
                        } else {
                            // 无视频缓存 → 删除本地数据 + 清理详情缓存
                            deleteSeriesAndCleanup(itemId)
                        }
                    }
                }
                
                // 转换并保存新数据
                val newSeriesList = allServerItems.map { (item, libraryName) ->
                    // 判断观看状态
                    val watchStatus = seriesWatchStatus[item.id] ?: WatchStatus.UNWATCHED
                    val hasWatchProgress = watchStatus == WatchStatus.WATCHING
                    
                    // 检查是否已有本地数据
                    val existingData = seriesDataStore.getSeries(item.id)
                    
                    // 计算 lastWatchTime：如果有观看进度，使用当前时间；否则为 0
                    val finalLastWatchTime = if (item.userData?.played == true) {
                        // 已看完：尝试解析时间，失败则用当前时间
                        parseIsoDateTime(item.userData?.lastPlayedDate).takeIf { it > 0 }
                            ?: System.currentTimeMillis()
                    } else if (hasWatchProgress) {
                        // 正在看：使用当前时间作为标记
                        System.currentTimeMillis()
                    } else {
                        // 没看过
                        0L
                    }
                    
                    if (existingData != null) {
                        // 更新现有数据，使用云端观看进度
                        // 如果之前标记为已删除，现在恢复（服务器重新上传了）
                        // 重要：保留本地的 customTags，更新 cloudFolderTag
                        existingData.copy(
                            name = item.name,
                            overview = item.overview,
                            type = item.type,
                            productionYear = item.productionYear,
                            primaryImageTag = item.imageTags?.primary,
                            seasonCount = item.childCount,
                            episodeCount = item.recursiveItemCount,
                            lastSyncTime = System.currentTimeMillis(),
                            isDeletedFromServer = false, // 恢复标记
                            // 云端观看进度
                            lastWatchPositionTicks = item.userData?.playbackPositionTicks ?: 0,
                            lastWatchTime = finalLastWatchTime,  // 使用计算后的时间
                            played = item.userData?.played ?: false,
                            // 更新云端文件夹标签
                            cloudFolderTag = libraryName ?: existingData.cloudFolderTag
                            // 注意：customTags 不在这里更新，保持原有值
                        )
                    } else {
                        // 创建新数据
                        SeriesLocalData.fromMediaItem(item, libraryName).copy(
                            lastWatchTime = finalLastWatchTime  // 使用计算后的时间
                        )
                    }
                }
                
                // 保存到本地
                seriesDataStore.saveSeriesList(newSeriesList, clearOld = false)
                seriesDataStore.saveLastRefreshTime()
                
                // 下载图片（异步进行，不阻塞返回）
                val itemsForImageDownload = allServerItems.map { it.first }
                // 使用当前激活的服务器地址（支持主备切换）
                val activeServerUrl = authRepository.getActiveServerUrl() ?: serverConfig.serverUrl
                downloadSeriesImages(activeServerUrl, itemsForImageDownload)
                
                // 获取已从服务器删除但本地有缓存的剧集
                val deletedButCachedSeries = itemsNotOnServer.mapNotNull { itemId ->
                    val localSeries = seriesDataStore.getSeries(itemId)
                    if (localSeries?.isVideoCached == true) {
                        localSeries
                    } else {
                        null
                    }
                }
                
                // 返回列表 = 已删除但有缓存的剧集 + 服务器剧集（已删除的显示在最前面）
                val fullList = deletedButCachedSeries + newSeriesList
                
                return@withContext Result.success(fullList)
            }
            
            Result.failure(Exception("获取剧集列表失败"))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("刷新失败: ${e.message}"))
        }
    }
    
    /**
     * 获取本地缓存的剧集列表
     */
    fun getLocalSeriesList(): List<SeriesLocalData> {
        return seriesDataStore.getAllSeries()
    }
    
    /**
     * 获取单个剧集
     */
    fun getSeries(seriesId: String): SeriesLocalData? {
        return seriesDataStore.getSeries(seriesId)
    }
    
    /**
     * 更新剧集的播放状态（本地）
     * 
     * @param seriesId 剧集 ID
     * @param played 是否已看完
     */
    fun updateSeriesPlayedStatus(seriesId: String, played: Boolean) {
        val series = seriesDataStore.getSeries(seriesId) ?: return
        val updatedSeries = series.copy(
            played = played,
            lastWatchTime = if (played) System.currentTimeMillis() else series.lastWatchTime
        )
        seriesDataStore.saveSeries(updatedSeries)
    }
    
    /**
     * 从本地 Episode 缓存更新所有 Series 的观看状态
     * 
     * 在列表页加载时调用，根据本地 Episode 的播放进度计算 Series 的状态：
     * 1. 如果 Series.played = true → 已看完（不更新）
     * 2. 如果有任何 Episode 有播放进度或已看完 → 正在看（更新 lastWatchTime）
     * 3. 如果一集都没看 → 没看过（lastWatchTime = 0）
     */
    suspend fun updateSeriesStatusFromLocalEpisodes() = withContext(Dispatchers.IO) {
        try {
            val allSeries = seriesDataStore.getAllSeries()
            
            allSeries.forEach { series ->
                // 如果已经标记为看完，不需要更新
                if (series.played) return@forEach
                
                // 获取该 Series 的所有季
                val seasons = episodeDataStore.getSeasons(series.id) ?: return@forEach
                
                // 检查是否有任何 Episode 有观看记录
                var hasWatchProgress = false
                for (season in seasons) {
                    val episodes = episodeDataStore.getEpisodes(season.id) ?: continue
                    if (episodes.any { it.playbackPositionTicks > 0 || it.played }) {
                        hasWatchProgress = true
                        break
                    }
                }
                
                // 更新 lastWatchTime
                val newLastWatchTime = if (hasWatchProgress) {
                    System.currentTimeMillis()  // 正在看
                } else {
                    0L  // 没看过
                }
                
                // 只有状态发生变化时才更新
                if (series.lastWatchTime != newLastWatchTime) {
                    seriesDataStore.saveSeries(series.copy(lastWatchTime = newLastWatchTime))
                }
            }
        } catch (e: Exception) {
            // 静默处理异常
        }
    }
    
    /**
     * 更新剧集的自定义标签
     * 
     * @param seriesId 剧集 ID
     * @param tags 标签列表
     */
    fun updateSeriesTags(seriesId: String, tags: List<String>) {
        val series = seriesDataStore.getSeries(seriesId) ?: return
        seriesDataStore.saveSeries(series.copy(customTags = tags))
    }
    
    /**
     * 添加剧集标签
     * 
     * @param seriesId 剧集 ID
     * @param tag 要添加的标签
     */
    fun addSeriesTag(seriesId: String, tag: String) {
        val series = seriesDataStore.getSeries(seriesId) ?: return
        if (tag !in series.customTags) {
            seriesDataStore.saveSeries(series.copy(customTags = series.customTags + tag))
        }
    }
    
    /**
     * 移除剧集标签
     * 
     * @param seriesId 剧集 ID
     * @param tag 要移除的标签
     */
    fun removeSeriesTag(seriesId: String, tag: String) {
        val series = seriesDataStore.getSeries(seriesId) ?: return
        seriesDataStore.saveSeries(series.copy(customTags = series.customTags.filter { it != tag }))
    }
    
    /**
     * 从所有剧集中移除指定标签
     * 
     * @param tag 要移除的标签
     */
    fun removeTagFromAllSeries(tag: String) {
        val allSeries = seriesDataStore.getAllSeries()
        allSeries.forEach { series ->
            if (tag in series.customTags) {
                seriesDataStore.saveSeries(series.copy(customTags = series.customTags.filter { it != tag }))
            }
        }
    }
    
    // ==================== 图片管理 ====================
    
    /**
     * 下载剧集图片
     * 
     * @param serverUrl 服务器地址
     * @param items 媒体项列表
     */
    private suspend fun downloadSeriesImages(
        serverUrl: String,
        items: List<MediaItem>
    ) = withContext(Dispatchers.IO) {
        items.forEach { item ->
            // 检查是否已有本地图片
            if (!ImageCacheUtils.hasLocalImage(item.id)) {
                // 下载图片
                val localPath = ImageCacheUtils.downloadAndCacheImage(
                    serverUrl = serverUrl,
                    itemId = item.id,
                    imageType = ImageCacheUtils.IMAGE_TYPE_PRIMARY,
                    imageTag = item.imageTags?.primary
                )
                
                // 更新本地数据
                if (localPath != null) {
                    seriesDataStore.updateSeriesImagePath(item.id, localPath)
                }
            }
        }
    }
    
    /**
     * 获取剧集图片 URL
     * 
     * 优先返回本地路径，如果没有则返回远程 URL
     * 
     * @param seriesId 剧集 ID
     * @return 图片路径（本地路径以 file:// 开头，远程为 http(s)://）
     */
    suspend fun getSeriesImageUrl(seriesId: String): String? = withContext(Dispatchers.IO) {
        // 先检查本地是否有缓存
        val localFile = ImageCacheUtils.getLocalImageFile(seriesId)
        if (localFile != null) {
            return@withContext "file://${localFile.absolutePath}"
        }
        
        // 返回远程 URL
        val serverConfig = authRepository.serverConfig.first() ?: return@withContext null
        val series = seriesDataStore.getSeries(seriesId) ?: return@withContext null
        
        // 使用当前激活的服务器地址（支持主备切换）
        val activeServerUrl = authRepository.getActiveServerUrl() ?: serverConfig.serverUrl
        
        ImageCacheUtils.getRemoteImageUrl(
            serverUrl = activeServerUrl,
            itemId = seriesId,
            imageTag = series.primaryImageTag
        )
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 获取设备名称
     */
    private fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }
    
    /**
     * 解析 ISO 8601 格式的日期时间字符串
     * 
     * Jellyfin 返回的格式如：2024-01-15T12:30:00.0000000Z
     * 
     * @param isoDateTime ISO 8601 格式的时间字符串
     * @return 毫秒时间戳，解析失败返回 0
     */
    private fun parseIsoDateTime(isoDateTime: String?): Long {
        if (isoDateTime.isNullOrEmpty()) return 0
        
        return try {
            // Jellyfin 返回的格式可能有多种，尝试解析
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss"
            )
            
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    return sdf.parse(isoDateTime)?.time ?: 0
                } catch (e: Exception) {
                    // 尝试下一个格式
                }
            }
            0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 获取最后刷新时间
     */
    fun getLastRefreshTime(): Long {
        return seriesDataStore.getLastRefreshTime()
    }
    
    /**
     * 清除所有本地数据
     */
    suspend fun clearAllData() {
        seriesDataStore.clearAll()
        episodeDataStore.clearAll()
        ImageCacheUtils.clearAllImageCache()
    }
    
    /**
     * 删除单个剧集及其所有相关缓存
     * 
     * 包括：
     * 1. SeriesLocalData（列表数据）
     * 2. EpisodeDataStore（季/集数据）
     * 3. 图片缓存
     * 
     * @param seriesId 剧集 ID
     */
    suspend fun deleteSeriesAndCleanup(seriesId: String) {
        // 获取该剧集的所有季 ID（用于清理剧集缓存）
        val seasons = episodeDataStore.getSeasons(seriesId)
        val seasonIds = seasons?.map { it.id } ?: emptyList()
        
        // 清理详情缓存（季和剧集）
        episodeDataStore.clearSeriesCache(seriesId, seasonIds)
        
        // 删除列表数据
        seriesDataStore.deleteSeries(seriesId)
        
        // 删除图片缓存
        ImageCacheUtils.deleteLocalImage(seriesId)
    }
    
    /**
     * 更新剧集的视频缓存状态
     * 
     * @param seriesId 剧集 ID
     * @param isCached 是否有视频缓存
     */
    fun updateVideoCacheStatus(seriesId: String, isCached: Boolean) {
        seriesDataStore.updateSeriesVideoCached(seriesId, isCached)
    }
    
    /**
     * 检查剧集是否已从服务器删除
     * 
     * @param seriesId 剧集 ID
     * @return 是否已从服务器删除
     */
    fun isSeriesDeletedFromServer(seriesId: String): Boolean {
        return seriesDataStore.getSeries(seriesId)?.isDeletedFromServer ?: false
    }
    
    /**
     * 观看状态枚举
     */
    private enum class WatchStatus {
        UNWATCHED,    // 没看过
        WATCHING,     // 正在看（有播放进度或有看完的集）
        COMPLETED     // 全部看完（暂不使用，因为 Series.played 已经表示这个状态）
    }
}

