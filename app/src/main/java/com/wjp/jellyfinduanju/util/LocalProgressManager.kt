package com.wjp.jellyfinduanju.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.wjp.jellyfinduanju.data.model.EpisodeLocalData
import com.wjp.jellyfinduanju.data.model.EpisodeProgress
import com.wjp.jellyfinduanju.data.model.LocalProgressData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * LocalProgressManager - 本地播放进度管理器
 * 
 * 功能：
 * 1. 将播放进度写入剧集文件夹下的 .jellyfinduanju_progress.json
 * 2. 更新现有的进度文件
 * 3. 处理文件读写异常
 * 
 * 进度文件结构：
 * {
 *   "seriesId": "local_xxx",
 *   "seriesName": "剧集名称",
 *   "lastScanTime": 1234567890,
 *   "fileModifiedTime": 1234567890,
 *   "episodes": [
 *     {
 *       "fileName": "第1集.mp4",
 *       "episodeNumber": 1,
 *       "playbackPositionTicks": 1000000,
 *       "runTimeTicks": 10000000,
 *       "played": false,
 *       "lastPlayedDate": 1234567890
 *     }
 *   ]
 * }
 */
object LocalProgressManager {
    
    private const val PROGRESS_FILE_NAME = ".jellyfinduanju_progress.json"
    
    // JSON 序列化器（带格式化）
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true // 格式化输出，方便调试
    }
    
    /**
     * 保存播放进度到剧集文件夹
     * 
     * @param context 应用上下文
     * @param seriesFolderUri 剧集文件夹的 URI
     * @param seriesId 剧集 ID
     * @param seriesName 剧集名称
     * @param episodes 剧集列表（包含播放进度）
     * @return 是否保存成功
     */
    fun saveProgress(
        context: Context,
        seriesFolderUri: String,
        seriesId: String,
        seriesName: String,
        episodes: List<EpisodeLocalData>
    ): Boolean {
        return try {
            val folderUri = Uri.parse(seriesFolderUri)
            val seriesFolder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
            
            // 查找或创建进度文件
            val progressFile = findOrCreateProgressFile(seriesFolder) ?: return false
            
            // 构建进度数据
            val progressData = buildProgressData(
                seriesId = seriesId,
                seriesName = seriesName,
                episodes = episodes
            )
            
            // 写入文件
            writeProgressFile(context, progressFile, progressData)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 更新单个剧集的播放进度
     * 
     * @param context 应用上下文
     * @param seriesFolderUri 剧集文件夹的 URI
     * @param seriesId 剧集 ID
     * @param seriesName 剧集名称
     * @param episode 要更新的剧集数据
     * @param allEpisodes 所有剧集列表（用于完整写入）
     * @return 是否更新成功
     */
    fun updateEpisodeProgress(
        context: Context,
        seriesFolderUri: String,
        seriesId: String,
        seriesName: String,
        episode: EpisodeLocalData,
        allEpisodes: List<EpisodeLocalData>
    ): Boolean {
        return saveProgress(
            context = context,
            seriesFolderUri = seriesFolderUri,
            seriesId = seriesId,
            seriesName = seriesName,
            episodes = allEpisodes
        )
    }
    
    /**
     * 查找或创建进度文件
     */
    private fun findOrCreateProgressFile(seriesFolder: DocumentFile): DocumentFile? {
        // 先查找是否已存在
        seriesFolder.listFiles().forEach { file ->
            if (file.name == PROGRESS_FILE_NAME && !file.isDirectory) {
                return file
            }
        }
        
        // 不存在则创建
        return seriesFolder.createFile("application/json", PROGRESS_FILE_NAME)
    }
    
    /**
     * 构建进度数据
     */
    private fun buildProgressData(
        seriesId: String,
        seriesName: String,
        episodes: List<EpisodeLocalData>
    ): LocalProgressData {
        val episodeProgressList = episodes.map { episode ->
            // 从 localVideoPath 中提取文件名
            val fileName = extractFileNameFromUri(episode.localVideoPath ?: "")
            
            EpisodeProgress(
                fileName = fileName,
                episodeNumber = episode.episodeNumber,
                playbackPositionTicks = episode.playbackPositionTicks,
                runTimeTicks = episode.runTimeTicks,
                played = episode.played,
                lastPlayedDate = episode.lastPlayedDate
            )
        }
        
        return LocalProgressData(
            seriesId = seriesId,
            seriesName = seriesName,
            lastScanTime = System.currentTimeMillis(),
            fileModifiedTime = System.currentTimeMillis(),
            episodes = episodeProgressList
        )
    }
    
    /**
     * 写入进度文件
     */
    private fun writeProgressFile(
        context: Context,
        progressFile: DocumentFile,
        progressData: LocalProgressData
    ) {
        context.contentResolver.openOutputStream(progressFile.uri, "wt")?.use { outputStream ->
            val jsonString = json.encodeToString(progressData)
            outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
            outputStream.flush()
        }
    }
    
    /**
     * 从 URI 中提取文件名
     */
    private fun extractFileNameFromUri(uriString: String): String {
        return try {
            val uri = Uri.parse(uriString)
            val lastSegment = uri.lastPathSegment ?: "unknown"
            // 解码 URI 编码的文件名
            Uri.decode(lastSegment)
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * 读取进度文件（用于调试和验证）
     * 
     * @param context 应用上下文
     * @param seriesFolderUri 剧集文件夹的 URI
     * @return 进度数据，如果读取失败则返回 null
     */
    fun readProgress(
        context: Context,
        seriesFolderUri: String
    ): LocalProgressData? {
        return try {
            val folderUri = Uri.parse(seriesFolderUri)
            val seriesFolder = DocumentFile.fromTreeUri(context, folderUri) ?: return null
            
            // 查找进度文件
            val progressFile = seriesFolder.listFiles().find { 
                it.name == PROGRESS_FILE_NAME && !it.isDirectory 
            } ?: return null
            
            // 读取文件内容
            context.contentResolver.openInputStream(progressFile.uri)?.use { inputStream ->
                val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                json.decodeFromString<LocalProgressData>(jsonString)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 删除进度文件
     * 
     * @param context 应用上下文
     * @param seriesFolderUri 剧集文件夹的 URI
     * @return 是否删除成功
     */
    fun deleteProgress(
        context: Context,
        seriesFolderUri: String
    ): Boolean {
        return try {
            val folderUri = Uri.parse(seriesFolderUri)
            val seriesFolder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
            
            // 查找进度文件
            val progressFile = seriesFolder.listFiles().find { 
                it.name == PROGRESS_FILE_NAME && !it.isDirectory 
            } ?: return false
            
            // 删除文件
            progressFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

