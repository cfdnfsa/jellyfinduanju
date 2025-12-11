package com.wjp.jellyfinduanju.data.repository

import android.os.Build
import com.wjp.jellyfinduanju.data.local.EpisodeDataStore
import com.wjp.jellyfinduanju.data.model.EpisodeLocalData
import com.wjp.jellyfinduanju.data.model.SeasonLocalData
import com.wjp.jellyfinduanju.data.network.JellyfinAuthHeader
import com.wjp.jellyfinduanju.data.network.NetworkModule
import com.wjp.jellyfinduanju.download.DownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * DetailRepository - 剧集详情数据仓库
 * 
 * 职责：
 * 1. 获取剧集的季列表（优先本地缓存）
 * 2. 获取季的剧集列表（优先本地缓存）
 * 3. 刷新数据（从服务器同步并保存到本地）
 * 4. 标记剧集为已看完
 * 
 * 缓存策略：
 * - 有缓存时使用本地缓存
 * - 无缓存时请求网络
 * - 手动刷新时强制请求网络并更新缓存
 * - 一切以服务器数据为准
 * 
 * @param authRepository 认证仓库，用于获取用户会话和设备信息
 * @param episodeDataStore 剧集本地数据存储
 */
class DetailRepository(
    private val authRepository: AuthRepository,
    private val episodeDataStore: EpisodeDataStore
) {
    
    // ==================== 获取季列表（带缓存） ====================
    
    /**
     * 获取剧集的所有季
     * 
     * 缓存策略：
     * - 有缓存 → 返回缓存
     * - 无缓存 → 请求网络 → 保存缓存 → 返回
     * 
     * @param seriesId 剧集 ID
     * @return 成功返回季列表，失败返回异常
     */
    suspend fun getSeasons(seriesId: String): Result<List<SeasonLocalData>> = withContext(Dispatchers.IO) {
        // 先检查本地缓存
        val cachedSeasons = episodeDataStore.getSeasons(seriesId)
        if (cachedSeasons != null) {
            return@withContext Result.success(cachedSeasons)
        }
        
        // 无缓存，从网络获取
        fetchSeasonsFromNetwork(seriesId)
    }
    
    /**
     * 获取某季的所有剧集
     * 
     * 缓存策略：
     * - 有缓存 → 返回缓存
     * - 无缓存 → 请求网络 → 保存缓存 → 返回
     * 
     * @param seriesId 剧集 ID
     * @param seasonId 季 ID
     * @return 成功返回剧集列表，失败返回异常
     */
    suspend fun getEpisodes(seriesId: String, seasonId: String): Result<List<EpisodeLocalData>> = withContext(Dispatchers.IO) {
        // 先检查本地缓存
        val cachedEpisodes = episodeDataStore.getEpisodes(seasonId)
        if (cachedEpisodes != null) {
            return@withContext Result.success(cachedEpisodes)
        }
        
        // 无缓存，从网络获取
        fetchEpisodesFromNetwork(seriesId, seasonId)
    }
    
    /**
     * 检查是否有缓存
     * 
     * @param seriesId 剧集 ID
     * @return 是否有缓存
     */
    fun hasCache(seriesId: String): Boolean {
        return episodeDataStore.hasSeasonsCache(seriesId)
    }
    
    // ==================== 刷新数据（强制网络） ====================
    
    /**
     * 刷新季列表
     * 
     * 强制从网络获取最新数据并更新缓存
     * 
     * @param seriesId 剧集 ID
     * @return 成功返回季列表，失败返回异常
     */
    suspend fun refreshSeasons(seriesId: String): Result<List<SeasonLocalData>> = withContext(Dispatchers.IO) {
        fetchSeasonsFromNetwork(seriesId)
    }
    
    /**
     * 刷新某季的剧集列表
     * 
     * 强制从网络获取最新数据并更新缓存
     * 
     * @param seriesId 剧集 ID
     * @param seasonId 季 ID
     * @return 成功返回剧集列表，失败返回异常
     */
    suspend fun refreshEpisodes(seriesId: String, seasonId: String): Result<List<EpisodeLocalData>> = withContext(Dispatchers.IO) {
        fetchEpisodesFromNetwork(seriesId, seasonId)
    }
    
    // ==================== 网络请求（私有） ====================
    
    /**
     * 从网络获取季列表并保存到缓存
     */
    private suspend fun fetchSeasonsFromNetwork(seriesId: String): Result<List<SeasonLocalData>> {
        try {
            // 获取服务器配置
            val serverConfig = authRepository.serverConfig.first()
                ?: return Result.failure(Exception("未配置服务器"))
            
            // 获取用户会话
            val userSession = authRepository.userSession.first()
                ?: return Result.failure(Exception("未登录"))
            
            // 获取设备信息
            val deviceId = authRepository.getDeviceId()
            val deviceName = getDeviceName()
            
            // 创建 API 客户端
            val api = NetworkModule.createJellyfinApi(serverConfig.serverUrl)
            
            // 构建认证头
            val authHeader = JellyfinAuthHeader.buildAuthHeader(
                deviceId = deviceId,
                deviceName = deviceName,
                token = userSession.accessToken
            )
            
            // 获取季列表
            val response = api.getSeasons(
                authorization = authHeader,
                seriesId = seriesId,
                userId = userSession.userId
            )
            
            return if (response.isSuccessful) {
                val itemsResponse = response.body()
                if (itemsResponse != null) {
                    // 转换为本地数据模型
                    val seasons = itemsResponse.items.map { item ->
                        SeasonLocalData.fromMediaItem(item, seriesId)
                    }.sortedBy { it.seasonNumber }
                    
                    // 保存到缓存
                    episodeDataStore.saveSeasons(seriesId, seasons)
                    
                    Result.success(seasons)
                } else {
                    Result.failure(Exception("服务器响应为空"))
                }
            } else {
                Result.failure(Exception("获取季列表失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            return Result.failure(Exception("请求季列表失败: ${e.message}"))
        }
    }
    
    /**
     * 从网络获取剧集列表并保存到缓存
     */
    private suspend fun fetchEpisodesFromNetwork(seriesId: String, seasonId: String): Result<List<EpisodeLocalData>> {
        try {
            // 获取服务器配置
            val serverConfig = authRepository.serverConfig.first()
                ?: return Result.failure(Exception("未配置服务器"))
            
            // 获取用户会话
            val userSession = authRepository.userSession.first()
                ?: return Result.failure(Exception("未登录"))
            
            // 获取设备信息
            val deviceId = authRepository.getDeviceId()
            val deviceName = getDeviceName()
            
            // 创建 API 客户端
            val api = NetworkModule.createJellyfinApi(serverConfig.serverUrl)
            
            // 构建认证头
            val authHeader = JellyfinAuthHeader.buildAuthHeader(
                deviceId = deviceId,
                deviceName = deviceName,
                token = userSession.accessToken
            )
            
            // 获取剧集列表
            val response = api.getEpisodes(
                authorization = authHeader,
                seriesId = seriesId,
                seasonId = seasonId,
                userId = userSession.userId
            )
            
            return if (response.isSuccessful) {
                val itemsResponse = response.body()
                if (itemsResponse != null) {
                    // 获取本地已有的剧集数据（用于保留缓存状态）
                    val localEpisodes = episodeDataStore.getEpisodes(seasonId)
                    val localEpisodeMap = localEpisodes?.associateBy { it.id } ?: emptyMap()
                    
                    // 转换为本地数据模型
                    val episodes = itemsResponse.items.map { item ->
                        val serverData = EpisodeLocalData.fromMediaItem(item, seriesId).copy(
                            seasonId = seasonId
                        )
                        
                        // 检查实际缓存文件是否存在（防止数据丢失但文件还在的情况）
                        val cachedFile = DownloadManager.getCachedVideoFile(seriesId, item.id)
                        val actualIsCached = cachedFile != null && cachedFile.exists()
                        val actualLocalPath = if (actualIsCached) cachedFile?.absolutePath else null
                        
                        // 优先使用实际文件状态，其次用本地数据
                        val localData = localEpisodeMap[item.id]
                        serverData.copy(
                            isCached = actualIsCached || (localData?.isCached ?: false),
                            localVideoPath = actualLocalPath ?: localData?.localVideoPath
                        )
                    }.sortedBy { it.episodeNumber }
                    
                    // 保存到缓存
                    episodeDataStore.saveEpisodes(seasonId, episodes)
                    
                    Result.success(episodes)
                } else {
                    Result.failure(Exception("服务器响应为空"))
                }
            } else {
                Result.failure(Exception("获取剧集列表失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            return Result.failure(Exception("请求剧集列表失败: ${e.message}"))
        }
    }
    
    // ==================== 标记已看完 ====================
    
    /**
     * 标记剧集为已看完
     * 
     * @param episodeId 剧集 ID
     * @param seasonId 季 ID（用于更新本地缓存）
     * @return 成功返回 true，失败返回异常
     */
    suspend fun markAsPlayed(episodeId: String, seasonId: String): Result<Boolean> = withContext(Dispatchers.IO) {
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
            
            // 创建 API 客户端
            val api = NetworkModule.createJellyfinApi(serverConfig.serverUrl)
            
            // 构建认证头
            val authHeader = JellyfinAuthHeader.buildAuthHeader(
                deviceId = deviceId,
                deviceName = deviceName,
                token = userSession.accessToken
            )
            
            // 调用标记已看完 API
            val response = api.markAsPlayed(
                authorization = authHeader,
                userId = userSession.userId,
                itemId = episodeId
            )
            
            if (response.isSuccessful) {
                // 更新本地缓存
                val cachedEpisodes = episodeDataStore.getEpisodes(seasonId)
                cachedEpisodes?.find { it.id == episodeId }?.let { episode ->
                    episodeDataStore.updateEpisode(seasonId, episode.copy(played = true))
                }
                
                Result.success(true)
            } else {
                Result.failure(Exception("标记失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("请求失败: ${e.message}"))
        }
    }
    
    /**
     * 取消标记剧集为已看完
     * 
     * @param episodeId 剧集 ID
     * @param seasonId 季 ID（用于更新本地缓存）
     * @return 成功返回 true，失败返回异常
     */
    suspend fun unmarkAsPlayed(episodeId: String, seasonId: String): Result<Boolean> = withContext(Dispatchers.IO) {
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
            
            // 创建 API 客户端
            val api = NetworkModule.createJellyfinApi(serverConfig.serverUrl)
            
            // 构建认证头
            val authHeader = JellyfinAuthHeader.buildAuthHeader(
                deviceId = deviceId,
                deviceName = deviceName,
                token = userSession.accessToken
            )
            
            // 调用取消标记 API
            val response = api.unmarkAsPlayed(
                authorization = authHeader,
                userId = userSession.userId,
                itemId = episodeId
            )
            
            if (response.isSuccessful) {
                // 更新本地缓存
                val cachedEpisodes = episodeDataStore.getEpisodes(seasonId)
                cachedEpisodes?.find { it.id == episodeId }?.let { episode ->
                    episodeDataStore.updateEpisode(seasonId, episode.copy(played = false))
                }
                
                Result.success(true)
            } else {
                Result.failure(Exception("取消标记失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("请求失败: ${e.message}"))
        }
    }
    
    // ==================== 获取图片 URL ====================
    
    /**
     * 获取剧集图片 URL
     * 
     * @param itemId 项目 ID
     * @param imageTag 图片标签
     * @return 图片 URL
     */
    suspend fun getImageUrl(itemId: String, imageTag: String?): String? {
        if (imageTag == null) return null
        
        val serverConfig = authRepository.serverConfig.first() ?: return null
        return "${serverConfig.serverUrl}/Items/$itemId/Images/Primary?tag=$imageTag"
    }
    
    // ==================== 获取视频流 URL ====================
    
    /**
     * 获取视频流 URL
     * 
     * Jellyfin 的视频流 URL 格式:
     * {serverUrl}/Videos/{itemId}/stream?Static=true&api_key={token}
     * 
     * @param episodeId 剧集 ID
     * @return 视频流 URL，如果未登录则返回 null
     */
    suspend fun getVideoStreamUrl(episodeId: String): String? {
        val serverConfig = authRepository.serverConfig.first() ?: return null
        val userSession = authRepository.userSession.first() ?: return null
        
        // Jellyfin 视频流 URL
        // Static=true 表示直接传输原始文件，不进行转码
        return "${serverConfig.serverUrl}/Videos/$episodeId/stream?Static=true&api_key=${userSession.accessToken}"
    }
    
    /**
     * 批量获取视频流 URL
     * 
     * @param episodeIds 剧集 ID 列表
     * @return episodeId 到 videoUrl 的映射
     */
    suspend fun getVideoStreamUrls(episodeIds: List<String>): Map<String, String> {
        val serverConfig = authRepository.serverConfig.first() ?: return emptyMap()
        val userSession = authRepository.userSession.first() ?: return emptyMap()
        
        return episodeIds.associateWith { episodeId ->
            "${serverConfig.serverUrl}/Videos/$episodeId/stream?Static=true&api_key=${userSession.accessToken}"
        }
    }
    
    // ==================== 播放进度管理 ====================
    
    /**
     * 保存播放进度到本地
     * 
     * @param seasonId 季 ID
     * @param episodeId 剧集 ID
     * @param positionTicks 播放位置（ticks，1秒=10,000,000 ticks）
     */
    fun savePlaybackPositionLocal(seasonId: String, episodeId: String, positionTicks: Long) {
        val episodes = episodeDataStore.getEpisodes(seasonId) ?: return
        val episode = episodes.find { it.id == episodeId } ?: return
        
        val updatedEpisode = episode.copy(
            playbackPositionTicks = positionTicks,
            lastPlayedDate = System.currentTimeMillis()
        )
        episodeDataStore.updateEpisode(seasonId, updatedEpisode)
    }
    
    /**
     * 上报播放进度到服务器
     * 
     * @param episodeId 剧集 ID
     * @param positionTicks 播放位置（ticks）
     * @param isPaused 是否暂停
     * @return 是否成功
     */
    suspend fun reportPlaybackProgress(
        episodeId: String,
        positionTicks: Long,
        isPaused: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverConfig = authRepository.serverConfig.first() ?: return@withContext false
            val userSession = authRepository.userSession.first() ?: return@withContext false
            val deviceId = authRepository.getDeviceId()
            val deviceName = getDeviceName()
            
            val api = NetworkModule.createJellyfinApi(serverConfig.serverUrl)
            val authHeader = JellyfinAuthHeader.buildAuthHeader(
                deviceId = deviceId,
                deviceName = deviceName,
                token = userSession.accessToken
            )
            
            val request = com.wjp.jellyfinduanju.data.network.PlaybackProgressRequest(
                itemId = episodeId,
                positionTicks = positionTicks,
                isPaused = isPaused
            )
            
            val response = api.reportPlaybackProgress(authHeader, request)
            response.isSuccessful
        } catch (e: Exception) {
            android.util.Log.e("DetailRepository", "上报播放进度失败", e)
            false
        }
    }
    
    /**
     * 上报播放开始
     * 
     * @param episodeId 剧集 ID
     * @param positionTicks 起始位置
     * @return 是否成功
     */
    suspend fun reportPlaybackStart(
        episodeId: String,
        positionTicks: Long = 0
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverConfig = authRepository.serverConfig.first() ?: return@withContext false
            val userSession = authRepository.userSession.first() ?: return@withContext false
            val deviceId = authRepository.getDeviceId()
            val deviceName = getDeviceName()
            
            val api = NetworkModule.createJellyfinApi(serverConfig.serverUrl)
            val authHeader = JellyfinAuthHeader.buildAuthHeader(
                deviceId = deviceId,
                deviceName = deviceName,
                token = userSession.accessToken
            )
            
            val request = com.wjp.jellyfinduanju.data.network.PlaybackStartRequest(
                itemId = episodeId,
                positionTicks = positionTicks
            )
            
            val response = api.reportPlaybackStart(authHeader, request)
            response.isSuccessful
        } catch (e: Exception) {
            android.util.Log.e("DetailRepository", "上报播放开始失败", e)
            false
        }
    }
    
    /**
     * 上报播放停止
     * 
     * @param episodeId 剧集 ID
     * @param positionTicks 停止位置
     * @return 是否成功
     */
    suspend fun reportPlaybackStopped(
        episodeId: String,
        positionTicks: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverConfig = authRepository.serverConfig.first() ?: return@withContext false
            val userSession = authRepository.userSession.first() ?: return@withContext false
            val deviceId = authRepository.getDeviceId()
            val deviceName = getDeviceName()
            
            val api = NetworkModule.createJellyfinApi(serverConfig.serverUrl)
            val authHeader = JellyfinAuthHeader.buildAuthHeader(
                deviceId = deviceId,
                deviceName = deviceName,
                token = userSession.accessToken
            )
            
            val request = com.wjp.jellyfinduanju.data.network.PlaybackStopRequest(
                itemId = episodeId,
                positionTicks = positionTicks
            )
            
            val response = api.reportPlaybackStopped(authHeader, request)
            response.isSuccessful
        } catch (e: Exception) {
            android.util.Log.e("DetailRepository", "上报播放停止失败", e)
            false
        }
    }
    
    /**
     * 批量上报本地进度到服务器
     * 
     * 遍历所有季的所有剧集，将本地保存的进度上报到服务器
     * 
     * @param seriesId 剧集 ID
     * @param seasons 季列表
     * @return 上报成功的数量
     */
    suspend fun uploadLocalProgressToServer(
        seriesId: String,
        seasons: List<SeasonLocalData>
    ): Int = withContext(Dispatchers.IO) {
        var successCount = 0
        
        try {
            val serverConfig = authRepository.serverConfig.first() ?: return@withContext 0
            val userSession = authRepository.userSession.first() ?: return@withContext 0
            val deviceId = authRepository.getDeviceId()
            val deviceName = getDeviceName()
            
            val api = NetworkModule.createJellyfinApi(serverConfig.serverUrl)
            val authHeader = JellyfinAuthHeader.buildAuthHeader(
                deviceId = deviceId,
                deviceName = deviceName,
                token = userSession.accessToken
            )
            
            // 遍历所有季
            for (season in seasons) {
                val episodes = episodeDataStore.getEpisodes(season.id) ?: continue
                
                // 遍历所有剧集
                for (episode in episodes) {
                    // 只上报有进度或已看完的剧集
                    if (episode.playbackPositionTicks > 0 || episode.played) {
                        try {
                            // 使用 updateUserItemData API 直接更新用户数据
                            val request = com.wjp.jellyfinduanju.data.network.UpdateUserDataRequest(
                                playbackPositionTicks = episode.playbackPositionTicks,
                                played = episode.played
                            )
                            val response = api.updateUserItemData(
                                authorization = authHeader,
                                userId = userSession.userId,
                                itemId = episode.id,
                                request = request
                            )
                            if (response.isSuccessful) {
                                successCount++
                            }
                        } catch (_: Exception) { }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DetailRepository", "批量上报进度失败", e)
        }
        
        successCount
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 获取设备名称
     */
    private fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }
}

