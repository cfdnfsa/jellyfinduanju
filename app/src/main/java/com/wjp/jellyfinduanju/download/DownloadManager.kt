package com.wjp.jellyfinduanju.download

import com.wjp.jellyfinduanju.data.local.DownloadDataStore
import com.wjp.jellyfinduanju.data.local.EpisodeDataStore
import com.wjp.jellyfinduanju.data.local.SeriesDataStore
import com.wjp.jellyfinduanju.data.model.DownloadStatus
import com.wjp.jellyfinduanju.data.model.DownloadTask
import com.wjp.jellyfinduanju.data.model.EpisodeLocalData
import com.wjp.jellyfinduanju.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * DownloadManager - 下载管理器
 * 
 * 职责：
 * 1. 管理下载队列
 * 2. 控制并发下载数量
 * 3. 处理下载进度
 * 4. 支持断点续传
 * 5. 处理下载失败和重试
 * 
 * 使用单例模式，全局唯一
 */
object DownloadManager {
    
    // ==================== 依赖 ====================
    
    private lateinit var downloadDataStore: DownloadDataStore
    private lateinit var episodeDataStore: EpisodeDataStore
    private lateinit var seriesDataStore: SeriesDataStore
    private lateinit var scope: CoroutineScope
    
    // OkHttp 客户端（用于下载）
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    // ==================== 状态 ====================
    
    // 正在下载的任务 Job
    private val downloadJobs = ConcurrentHashMap<String, Job>()
    
    // 下载状态 Flow
    private val _downloadingTasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadingTasks: StateFlow<List<DownloadTask>> = _downloadingTasks.asStateFlow()
    
    // 是否已初始化
    private var isInitialized = false
    
    // ==================== 初始化 ====================
    
    /**
     * 初始化下载管理器
     * 
     * @param dataStore 下载数据存储
     * @param episodeStore 剧集数据存储（用于更新缓存状态）
     * @param seriesStore 剧集列表数据存储（用于更新缓存状态）
     */
    fun init(
        dataStore: DownloadDataStore,
        episodeStore: EpisodeDataStore,
        seriesStore: SeriesDataStore
    ) {
        if (isInitialized) return
        
        downloadDataStore = dataStore
        episodeDataStore = episodeStore
        seriesDataStore = seriesStore
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        isInitialized = true
        
        // 恢复之前未完成的下载
        scope.launch {
            restorePendingDownloads()
        }
    }
    
    /**
     * 恢复之前未完成的下载
     */
    private suspend fun restorePendingDownloads() {
        // 将之前"下载中"的任务改为"等待中"
        val downloadingTasks = downloadDataStore.getTasksByStatus(DownloadStatus.DOWNLOADING)
        downloadingTasks.forEach { task ->
            downloadDataStore.updateStatus(task.id, DownloadStatus.PENDING)
        }
        
        // 更新状态
        updateDownloadingTasksFlow()
        
        // 尝试开始下载
        processQueue()
    }
    
    // ==================== 公开 API ====================
    
    /**
     * 获取所有下载任务 Flow
     */
    fun getTasksFlow(): Flow<List<DownloadTask>> {
        return downloadDataStore.tasksFlow
    }
    
    /**
     * 添加下载任务
     * 
     * @param episode 要下载的剧集
     * @param seriesName 剧集名称
     * @param videoUrl 视频流 URL
     */
    fun addDownload(
        episode: EpisodeLocalData,
        seriesName: String,
        videoUrl: String
    ) {
        // 检查是否已存在
        if (downloadDataStore.hasTask(episode.id)) {
            val existingTask = downloadDataStore.getTask(episode.id)
            // 如果已完成或失败，可以重新下载
            if (existingTask?.status == DownloadStatus.COMPLETED) {
                return // 已完成，不重复下载
            }
            if (existingTask?.status == DownloadStatus.DOWNLOADING || 
                existingTask?.status == DownloadStatus.PENDING) {
                return // 正在下载或等待中，不重复添加
            }
        }
        
        // 创建下载任务
        val localPath = getVideoLocalPath(episode.seriesId, episode.id)
        val task = DownloadTask.fromEpisode(episode, seriesName, localPath).copy(
            videoUrl = videoUrl,
            status = DownloadStatus.PENDING
        )
        
        // 保存任务
        downloadDataStore.saveTask(task)
        
        // 尝试开始下载
        scope.launch {
            processQueue()
        }
    }
    
    /**
     * 批量添加下载任务
     */
    fun addDownloads(
        episodes: List<EpisodeLocalData>,
        seriesName: String,
        videoUrls: Map<String, String>  // episodeId -> videoUrl
    ) {
        val tasks = episodes.mapNotNull { episode ->
            val videoUrl = videoUrls[episode.id] ?: return@mapNotNull null
            
            // 检查是否已存在且正在进行
            if (downloadDataStore.hasTask(episode.id)) {
                val existingTask = downloadDataStore.getTask(episode.id)
                if (existingTask?.status == DownloadStatus.COMPLETED ||
                    existingTask?.status == DownloadStatus.DOWNLOADING ||
                    existingTask?.status == DownloadStatus.PENDING) {
                    return@mapNotNull null
                }
            }
            
            val localPath = getVideoLocalPath(episode.seriesId, episode.id)
            DownloadTask.fromEpisode(episode, seriesName, localPath).copy(
                videoUrl = videoUrl,
                status = DownloadStatus.PENDING
            )
        }
        
        if (tasks.isNotEmpty()) {
            downloadDataStore.saveTasks(tasks)
            scope.launch {
                processQueue()
            }
        }
    }
    
    /**
     * 暂停下载
     */
    fun pauseDownload(taskId: String) {
        // 取消下载 Job
        downloadJobs[taskId]?.cancel()
        downloadJobs.remove(taskId)
        
        // 更新状态
        downloadDataStore.updateStatus(taskId, DownloadStatus.PAUSED)
        
        scope.launch {
            updateDownloadingTasksFlow()
            processQueue()
        }
    }
    
    /**
     * 暂停所有下载
     */
    fun pauseAll() {
        downloadJobs.keys.toList().forEach { taskId ->
            pauseDownload(taskId)
        }
    }
    
    /**
     * 恢复下载
     */
    fun resumeDownload(taskId: String) {
        val task = downloadDataStore.getTask(taskId) ?: return
        
        if (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.FAILED) {
            downloadDataStore.updateStatus(taskId, DownloadStatus.PENDING)
            scope.launch {
                processQueue()
            }
        }
    }
    
    /**
     * 重试下载
     */
    fun retryDownload(taskId: String) {
        resumeDownload(taskId)
    }
    
    /**
     * 取消下载
     */
    fun cancelDownload(taskId: String) {
        // 取消下载 Job
        downloadJobs[taskId]?.cancel()
        downloadJobs.remove(taskId)
        
        // 先获取任务信息（用于删除文件）
        val task = downloadDataStore.getTask(taskId)
        
        // 删除已下载的文件（包括未完成的部分文件）
        task?.localPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        
        // 删除任务记录
        downloadDataStore.deleteTask(taskId)
        
        scope.launch {
            updateDownloadingTasksFlow()
            processQueue()
        }
    }
    
    /**
     * 取消所有下载任务
     */
    fun cancelAllDownloads() {
        // 取消所有下载 Job
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        
        // 删除所有已下载的文件
        downloadDataStore.getAllTasks().forEach { task ->
            task.localPath?.let { path ->
                File(path).delete()
            }
        }
        
        scope.launch {
            updateDownloadingTasksFlow()
        }
    }
    
    /**
     * 删除已完成的下载（包括文件）
     */
    suspend fun deleteCompletedDownload(taskId: String) {
        val task = downloadDataStore.getTask(taskId) ?: return
        
        // 删除文件
        task.localPath?.let { path ->
            File(path).delete()
        }
        
        // 删除任务记录
        downloadDataStore.deleteTask(taskId)
        
        // 更新 EpisodeDataStore 中的缓存状态
        episodeDataStore.updateEpisodeCacheStatus(
            seasonId = task.seasonId,
            episodeId = task.episodeId,
            isCached = false,
            localVideoPath = null
        )
        
        // 检查该剧集是否还有其他视频缓存
        val hasRemainingCache = episodeDataStore.hasAnyVideoCache(task.seriesId)
        if (!hasRemainingCache) {
            seriesDataStore.getSeries(task.seriesId)?.let { series ->
                seriesDataStore.saveSeries(series.copy(isVideoCached = false))
            }
        }
    }
    
    /**
     * 获取当前并发下载数量
     */
    fun getCurrentDownloadingCount(): Int {
        return downloadJobs.size
    }
    
    // ==================== 队列处理 ====================
    
    /**
     * 处理下载队列
     * 
     * 检查是否有空闲槽位，如果有则开始下载等待中的任务
     */
    private suspend fun processQueue() {
        val maxConcurrent = AppSettings.maxConcurrentDownloads
        val currentDownloading = downloadJobs.size
        
        if (currentDownloading >= maxConcurrent) {
            return // 已达到最大并发数
        }
        
        // 获取等待中的任务
        val pendingTasks = downloadDataStore.getTasksByStatus(DownloadStatus.PENDING)
            .sortedBy { it.createTime }
        
        // 计算可以开始的任务数
        val slotsAvailable = maxConcurrent - currentDownloading
        val tasksToStart = pendingTasks.take(slotsAvailable)
        
        // 开始下载
        tasksToStart.forEach { task ->
            startDownload(task)
        }
        
        updateDownloadingTasksFlow()
    }
    
    /**
     * 开始下载任务
     */
    private fun startDownload(task: DownloadTask) {
        if (downloadJobs.containsKey(task.id)) {
            return // 已经在下载
        }
        
        val job = scope.launch {
            executeDownload(task)
        }
        
        downloadJobs[task.id] = job
        
        // 任务完成后清理
        job.invokeOnCompletion {
            downloadJobs.remove(task.id)
            scope.launch {
                updateDownloadingTasksFlow()
                processQueue()
            }
        }
    }
    
    /**
     * 执行下载
     */
    private suspend fun executeDownload(task: DownloadTask) = withContext(Dispatchers.IO) {
        val videoUrl = task.videoUrl ?: run {
            downloadDataStore.updateStatus(task.id, DownloadStatus.FAILED, "视频 URL 为空")
            return@withContext
        }
        
        val localPath = task.localPath ?: run {
            downloadDataStore.updateStatus(task.id, DownloadStatus.FAILED, "本地路径为空")
            return@withContext
        }
        
        // 更新状态为下载中
        downloadDataStore.updateStatus(task.id, DownloadStatus.DOWNLOADING)
        updateDownloadingTasksFlow()
        
        try {
            // 创建目录
            val file = File(localPath)
            file.parentFile?.mkdirs()
            
            // 检查已下载的大小（用于断点续传）
            val downloadedBytes = if (file.exists()) file.length() else 0L
            
            // 构建请求
            val requestBuilder = Request.Builder().url(videoUrl)
            
            // 断点续传：添加 Range 头
            if (downloadedBytes > 0) {
                requestBuilder.addHeader("Range", "bytes=$downloadedBytes-")
            }
            
            val request = requestBuilder.build()
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                downloadDataStore.updateStatus(task.id, DownloadStatus.FAILED, "HTTP ${response.code}")
                return@withContext
            }
            
            val body = response.body ?: run {
                downloadDataStore.updateStatus(task.id, DownloadStatus.FAILED, "响应体为空")
                return@withContext
            }
            
            // 获取文件总大小
            val contentLength = body.contentLength()
            val totalBytes = if (response.code == 206) {
                // 断点续传响应，需要解析 Content-Range
                val contentRange = response.header("Content-Range")
                contentRange?.substringAfterLast("/")?.toLongOrNull() ?: (downloadedBytes + contentLength)
            } else {
                contentLength
            }
            
            // 更新总大小
            downloadDataStore.updateProgress(task.id, downloadedBytes, totalBytes)
            
            // 写入文件
            val raf = RandomAccessFile(file, "rw")
            raf.seek(downloadedBytes)
            
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var currentDownloaded = downloadedBytes
            var lastUpdateTime = System.currentTimeMillis()
            
            body.byteStream().use { inputStream ->
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    raf.write(buffer, 0, bytesRead)
                    currentDownloaded += bytesRead
                    
                    // 每 500ms 更新一次进度
                    val now = System.currentTimeMillis()
                    if (now - lastUpdateTime > 500) {
                        downloadDataStore.updateProgress(task.id, currentDownloaded, totalBytes)
                        updateDownloadingTasksFlow()
                        lastUpdateTime = now
                    }
                }
            }
            
            raf.close()
            
            // 下载完成
            downloadDataStore.markCompleted(task.id, localPath)
            
            // 更新 EpisodeDataStore 中的缓存状态
            episodeDataStore.updateEpisodeCacheStatus(
                seasonId = task.seasonId,
                episodeId = task.episodeId,
                isCached = true,
                localVideoPath = localPath
            )
            
            // 更新 SeriesDataStore 中的缓存状态
            seriesDataStore.getSeries(task.seriesId)?.let { series ->
                if (!series.isVideoCached) {
                    seriesDataStore.saveSeries(series.copy(isVideoCached = true))
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            downloadDataStore.updateStatus(task.id, DownloadStatus.FAILED, e.message ?: "下载失败")
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 更新正在下载的任务 Flow
     */
    private fun updateDownloadingTasksFlow() {
        _downloadingTasks.value = downloadDataStore.getActiveTasks()
    }
    
    /**
     * 获取视频本地路径
     */
    private fun getVideoLocalPath(seriesId: String, episodeId: String): String {
        val cacheDir = File(AppSettings.videoCachePath, seriesId)
        return File(cacheDir, "$episodeId.mp4").absolutePath
    }
    
    /**
     * 检查视频是否已缓存
     */
    fun isVideoCached(seriesId: String, episodeId: String): Boolean {
        val path = getVideoLocalPath(seriesId, episodeId)
        return File(path).exists()
    }
    
    /**
     * 获取缓存的视频文件
     */
    fun getCachedVideoFile(seriesId: String, episodeId: String): File? {
        val path = getVideoLocalPath(seriesId, episodeId)
        val file = File(path)
        return if (file.exists()) file else null
    }
    
    /**
     * 删除某个剧集的所有缓存
     * 
     * @param seriesId 剧集 ID
     * @return 删除的文件数量
     */
    suspend fun deleteAllCacheForSeries(seriesId: String): Int {
        var deletedCount = 0
        
        try {
            // 1. 取消该剧集的所有下载任务
            val allTasks = downloadDataStore.getAllTasks()
            allTasks.filter { it.seriesId == seriesId }.forEach { task ->
                cancelDownload(task.episodeId)
            }
            
            // 2. 删除该剧集目录下的所有视频文件
            val seriesDir = File(AppSettings.videoCachePath, seriesId)
            if (seriesDir.exists() && seriesDir.isDirectory) {
                seriesDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.delete()) {
                        deletedCount++
                    }
                }
                // 删除空目录
                seriesDir.delete()
            }
            
            // 3. 更新该剧集所有季的剧集缓存状态
            val seasons = episodeDataStore.getSeasons(seriesId)
            seasons?.forEach { season ->
                val episodes = episodeDataStore.getEpisodes(season.id)
                episodes?.forEach { episode ->
                    if (episode.isCached) {
                        episodeDataStore.updateEpisodeCacheStatus(
                            seasonId = season.id,
                            episodeId = episode.id,
                            isCached = false,
                            localVideoPath = null
                        )
                    }
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return deletedCount
    }
}

