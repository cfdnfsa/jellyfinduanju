package com.wjp.jellyfinduanju.data.local

import com.tencent.mmkv.MMKV
import com.wjp.jellyfinduanju.data.model.SeriesLocalData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SeriesDataStore - 剧集本地数据存储管理
 * 
 * 使用 MMKV 存储剧集信息，以 Map 形式组织数据
 * 支持后续扩展观看记录、下载状态等功能
 * 
 * 存储结构：
 * - series_map: Map<String, SeriesLocalData> 存储所有剧集信息
 * - 每个剧集以 ID 为 key
 * 
 * 使用方式：
 * ```kotlin
 * val store = SeriesDataStore()
 * 
 * // 保存剧集
 * store.saveSeries(seriesData)
 * 
 * // 获取所有剧集
 * val allSeries = store.getAllSeries()
 * 
 * // 监听剧集变化
 * store.seriesListFlow.collect { list -> ... }
 * ```
 */
class SeriesDataStore {
    
    // 使用独立的 MMKV 实例存储剧集数据
    private val mmkv = MMKV.mmkvWithID("series_data")
    
    // JSON 序列化器
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // ==================== MMKV Keys ====================
    
    private object Keys {
        const val SERIES_MAP = "series_map"
        const val LAST_REFRESH_TIME = "last_refresh_time"
    }
    
    // ==================== 响应式数据流 ====================
    
    /**
     * 剧集列表的 StateFlow
     * 
     * UI 可以订阅此 Flow 来响应数据变化
     */
    private val _seriesListFlow = MutableStateFlow<List<SeriesLocalData>>(loadAllSeries())
    val seriesListFlow: Flow<List<SeriesLocalData>> = _seriesListFlow
    
    /**
     * 最后刷新时间的 StateFlow
     */
    private val _lastRefreshTimeFlow = MutableStateFlow(loadLastRefreshTime())
    val lastRefreshTimeFlow: Flow<Long> = _lastRefreshTimeFlow
    
    // ==================== 保存方法 ====================
    
    /**
     * 保存单个剧集
     * 
     * @param series 剧集数据
     */
    fun saveSeries(series: SeriesLocalData) {
        val currentMap = loadSeriesMap().toMutableMap()
        currentMap[series.id] = series
        saveSeriesMap(currentMap)
        
        // 更新 Flow
        _seriesListFlow.value = currentMap.values.toList()
    }
    
    /**
     * 批量保存剧集
     * 
     * @param seriesList 剧集列表
     * @param clearOld 是否清除旧数据
     */
    fun saveSeriesList(seriesList: List<SeriesLocalData>, clearOld: Boolean = false) {
        val currentMap = if (clearOld) {
            mutableMapOf()
        } else {
            loadSeriesMap().toMutableMap()
        }
        
        seriesList.forEach { series ->
            currentMap[series.id] = series
        }
        
        saveSeriesMap(currentMap)
        
        // 更新 Flow
        _seriesListFlow.value = currentMap.values.toList()
    }
    
    /**
     * 更新剧集的本地图片路径
     * 
     * @param seriesId 剧集 ID
     * @param imagePath 本地图片路径
     */
    fun updateSeriesImagePath(seriesId: String, imagePath: String) {
        val currentMap = loadSeriesMap().toMutableMap()
        currentMap[seriesId]?.let { series ->
            currentMap[seriesId] = series.copy(localImagePath = imagePath)
            saveSeriesMap(currentMap)
            _seriesListFlow.value = currentMap.values.toList()
        }
    }
    
    /**
     * 更新剧集的视频缓存状态
     * 
     * @param seriesId 剧集 ID
     * @param isCached 是否已缓存
     */
    fun updateSeriesVideoCached(seriesId: String, isCached: Boolean) {
        val currentMap = loadSeriesMap().toMutableMap()
        currentMap[seriesId]?.let { series ->
            currentMap[seriesId] = series.copy(isVideoCached = isCached)
            saveSeriesMap(currentMap)
            _seriesListFlow.value = currentMap.values.toList()
        }
    }
    
    /**
     * 保存最后刷新时间
     */
    fun saveLastRefreshTime(time: Long = System.currentTimeMillis()) {
        mmkv.encode(Keys.LAST_REFRESH_TIME, time)
        _lastRefreshTimeFlow.value = time
    }
    
    // ==================== 查询方法 ====================
    
    /**
     * 获取所有剧集
     * 
     * @return 剧集列表
     */
    fun getAllSeries(): List<SeriesLocalData> {
        return loadAllSeries()
    }
    
    /**
     * 获取单个剧集
     * 
     * @param seriesId 剧集 ID
     * @return 剧集数据，不存在返回 null
     */
    fun getSeries(seriesId: String): SeriesLocalData? {
        return loadSeriesMap()[seriesId]
    }
    
    /**
     * 检查剧集是否存在
     * 
     * @param seriesId 剧集 ID
     * @return 是否存在
     */
    fun hasSeries(seriesId: String): Boolean {
        return loadSeriesMap().containsKey(seriesId)
    }
    
    /**
     * 检查剧集是否有视频缓存
     * 
     * @param seriesId 剧集 ID
     * @return 是否有视频缓存
     */
    fun isSeriesVideoCached(seriesId: String): Boolean {
        return loadSeriesMap()[seriesId]?.isVideoCached == true
    }
    
    /**
     * 获取所有剧集 ID
     * 
     * @return ID 集合
     */
    fun getAllSeriesIds(): Set<String> {
        return loadSeriesMap().keys
    }
    
    /**
     * 获取最后刷新时间
     */
    fun getLastRefreshTime(): Long {
        return loadLastRefreshTime()
    }
    
    // ==================== 删除方法 ====================
    
    /**
     * 删除单个剧集
     * 
     * @param seriesId 剧集 ID
     */
    fun deleteSeries(seriesId: String) {
        val currentMap = loadSeriesMap().toMutableMap()
        currentMap.remove(seriesId)
        saveSeriesMap(currentMap)
        _seriesListFlow.value = currentMap.values.toList()
    }
    
    /**
     * 批量删除剧集
     * 
     * @param seriesIds 要删除的剧集 ID 列表
     */
    fun deleteSeriesList(seriesIds: List<String>) {
        val currentMap = loadSeriesMap().toMutableMap()
        seriesIds.forEach { id ->
            currentMap.remove(id)
        }
        saveSeriesMap(currentMap)
        _seriesListFlow.value = currentMap.values.toList()
    }
    
    /**
     * 清除所有剧集数据
     */
    fun clearAll() {
        mmkv.clearAll()
        _seriesListFlow.value = emptyList()
        _lastRefreshTimeFlow.value = 0
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 从 MMKV 加载剧集 Map
     */
    private fun loadSeriesMap(): Map<String, SeriesLocalData> {
        val jsonString = mmkv.decodeString(Keys.SERIES_MAP)
        return if (jsonString.isNullOrEmpty()) {
            emptyMap()
        } else {
            try {
                json.decodeFromString<Map<String, SeriesLocalData>>(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyMap()
            }
        }
    }
    
    /**
     * 从 MMKV 加载所有剧集列表
     */
    private fun loadAllSeries(): List<SeriesLocalData> {
        return loadSeriesMap().values.toList()
    }
    
    /**
     * 保存剧集 Map 到 MMKV
     */
    private fun saveSeriesMap(map: Map<String, SeriesLocalData>) {
        val jsonString = json.encodeToString(map)
        mmkv.encode(Keys.SERIES_MAP, jsonString)
    }
    
    /**
     * 加载最后刷新时间
     */
    private fun loadLastRefreshTime(): Long {
        return mmkv.decodeLong(Keys.LAST_REFRESH_TIME, 0)
    }
}

