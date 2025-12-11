package com.wjp.jellyfinduanju.data.local

import com.tencent.mmkv.MMKV
import com.wjp.jellyfinduanju.data.model.EpisodeLocalData
import com.wjp.jellyfinduanju.data.model.SeasonLocalData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * EpisodeDataStore - 季/剧集本地数据存储管理
 * 
 * 使用 MMKV 存储季和剧集信息
 * 
 * 存储结构：
 * - seasons_{seriesId}: List<SeasonLocalData> 存储某剧集的所有季
 * - episodes_{seasonId}: List<EpisodeLocalData> 存储某季的所有剧集
 * - cache_time_{seriesId}: Long 缓存时间戳
 * 
 * 缓存策略：
 * - 完全依赖手动刷新
 * - 一切以服务器数据为准
 */
class EpisodeDataStore {
    
    // 使用独立的 MMKV 实例
    private val mmkv = MMKV.mmkvWithID("episode_data")
    
    // JSON 序列化器
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // ==================== Keys ====================
    
    private fun seasonsKey(seriesId: String) = "seasons_$seriesId"
    private fun episodesKey(seasonId: String) = "episodes_$seasonId"
    private fun cacheTimeKey(seriesId: String) = "cache_time_$seriesId"
    
    // ==================== 季数据操作 ====================
    
    /**
     * 保存剧集的季列表
     * 
     * @param seriesId 剧集 ID
     * @param seasons 季列表
     */
    fun saveSeasons(seriesId: String, seasons: List<SeasonLocalData>) {
        val key = seasonsKey(seriesId)
        val jsonString = json.encodeToString(seasons)
        mmkv.encode(key, jsonString)
        // 更新缓存时间
        mmkv.encode(cacheTimeKey(seriesId), System.currentTimeMillis())
        android.util.Log.d("EpisodeDataStore", "saveSeasons: 保存 ${seasons.size} 个季到 key=$key")
    }
    
    /**
     * 获取剧集的季列表
     * 
     * @param seriesId 剧集 ID
     * @return 季列表，不存在返回 null
     */
    fun getSeasons(seriesId: String): List<SeasonLocalData>? {
        val key = seasonsKey(seriesId)
        val jsonString = mmkv.decodeString(key)
        
        if (jsonString.isNullOrEmpty()) {
            android.util.Log.d("EpisodeDataStore", "getSeasons: 缓存不存在 key=$key")
            return null
        }
        
        return try {
            val result = json.decodeFromString<List<SeasonLocalData>>(jsonString)
            android.util.Log.d("EpisodeDataStore", "getSeasons: 从缓存加载 ${result.size} 个季")
            result
        } catch (e: Exception) {
            android.util.Log.e("EpisodeDataStore", "getSeasons: 解析失败 key=$key", e)
            // 解析失败，删除损坏的缓存
            mmkv.removeValueForKey(key)
            null
        }
    }
    
    /**
     * 检查是否有季缓存
     * 
     * @param seriesId 剧集 ID
     * @return 是否有缓存
     */
    fun hasSeasonsCache(seriesId: String): Boolean {
        return mmkv.containsKey(seasonsKey(seriesId))
    }
    
    // ==================== 剧集数据操作 ====================
    
    /**
     * 保存某季的剧集列表
     * 
     * @param seasonId 季 ID
     * @param episodes 剧集列表
     */
    fun saveEpisodes(seasonId: String, episodes: List<EpisodeLocalData>) {
        val key = episodesKey(seasonId)
        val jsonString = json.encodeToString(episodes)
        mmkv.encode(key, jsonString)
        android.util.Log.d("EpisodeDataStore", "saveEpisodes: 保存 ${episodes.size} 个剧集到 key=$key")
    }
    
    /**
     * 获取某季的剧集列表
     * 
     * @param seasonId 季 ID
     * @return 剧集列表，不存在返回 null
     */
    fun getEpisodes(seasonId: String): List<EpisodeLocalData>? {
        val key = episodesKey(seasonId)
        val jsonString = mmkv.decodeString(key)
        
        if (jsonString.isNullOrEmpty()) {
            android.util.Log.d("EpisodeDataStore", "getEpisodes: 缓存不存在 key=$key")
            return null
        }
        
        return try {
            val result = json.decodeFromString<List<EpisodeLocalData>>(jsonString)
            android.util.Log.d("EpisodeDataStore", "getEpisodes: 从缓存加载 ${result.size} 个剧集")
            result
        } catch (e: Exception) {
            android.util.Log.e("EpisodeDataStore", "getEpisodes: 解析失败 key=$key", e)
            // 解析失败，删除损坏的缓存
            mmkv.removeValueForKey(key)
            null
        }
    }
    
    /**
     * 检查是否有剧集缓存
     * 
     * @param seasonId 季 ID
     * @return 是否有缓存
     */
    fun hasEpisodesCache(seasonId: String): Boolean {
        return mmkv.containsKey(episodesKey(seasonId))
    }
    
    /**
     * 更新单个剧集的数据
     * 
     * @param seasonId 季 ID
     * @param episode 更新后的剧集
     */
    fun updateEpisode(seasonId: String, episode: EpisodeLocalData) {
        val episodes = getEpisodes(seasonId)?.toMutableList() ?: return
        val index = episodes.indexOfFirst { it.id == episode.id }
        if (index != -1) {
            episodes[index] = episode
            saveEpisodes(seasonId, episodes)
        }
    }
    
    /**
     * 更新剧集的缓存状态
     * 
     * @param seasonId 季 ID
     * @param episodeId 剧集 ID
     * @param isCached 是否已缓存
     * @param localVideoPath 本地视频路径
     */
    fun updateEpisodeCacheStatus(
        seasonId: String,
        episodeId: String,
        isCached: Boolean,
        localVideoPath: String?
    ) {
        val episodes = getEpisodes(seasonId)?.toMutableList() ?: return
        val index = episodes.indexOfFirst { it.id == episodeId }
        if (index != -1) {
            episodes[index] = episodes[index].copy(
                isCached = isCached,
                localVideoPath = localVideoPath
            )
            saveEpisodes(seasonId, episodes)
        }
    }
    
    /**
     * 检查某个剧集是否有任何视频缓存
     * 
     * @param seriesId 剧集 ID
     * @return 是否有视频缓存
     */
    fun hasAnyVideoCache(seriesId: String): Boolean {
        val seasons = getSeasons(seriesId) ?: return false
        return seasons.any { season ->
            getEpisodes(season.id)?.any { it.isCached } == true
        }
    }
    
    /**
     * 删除剧集详情缓存（不需要季 ID 列表）
     * 
     * @param seriesId 剧集 ID
     */
    fun deleteSeriesDetailCache(seriesId: String) {
        // 先获取季列表
        val seasons = getSeasons(seriesId)
        
        // 删除季缓存
        mmkv.removeValueForKey(seasonsKey(seriesId))
        mmkv.removeValueForKey(cacheTimeKey(seriesId))
        
        // 删除每季的剧集缓存
        seasons?.forEach { season ->
            mmkv.removeValueForKey(episodesKey(season.id))
        }
    }
    
    // ==================== 缓存时间 ====================
    
    /**
     * 获取缓存时间
     * 
     * @param seriesId 剧集 ID
     * @return 缓存时间戳，不存在返回 0
     */
    fun getCacheTime(seriesId: String): Long {
        return mmkv.decodeLong(cacheTimeKey(seriesId), 0)
    }
    
    // ==================== 删除操作 ====================
    
    /**
     * 删除剧集的所有缓存（季和剧集）
     * 
     * @param seriesId 剧集 ID
     * @param seasonIds 该剧集的季 ID 列表
     */
    fun clearSeriesCache(seriesId: String, seasonIds: List<String>) {
        // 删除季缓存
        mmkv.removeValueForKey(seasonsKey(seriesId))
        mmkv.removeValueForKey(cacheTimeKey(seriesId))
        
        // 删除每季的剧集缓存
        seasonIds.forEach { seasonId ->
            mmkv.removeValueForKey(episodesKey(seasonId))
        }
    }
    
    /**
     * 清除所有缓存
     */
    fun clearAll() {
        mmkv.clearAll()
    }
}

