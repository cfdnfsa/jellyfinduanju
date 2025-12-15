package com.wjp.jellyfinduanju.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.wjp.jellyfinduanju.data.model.EpisodeLocalData
import com.wjp.jellyfinduanju.data.model.LocalProgressData
import com.wjp.jellyfinduanju.data.model.SeriesLocalData
import kotlinx.serialization.json.Json

// 支持的视频格式
private val VIDEO_EXTENSIONS = setOf(
    "mp4", "mkv", "avi", "mov", "wmv", 
    "flv", "m4v", "webm", "rmvb", "rm"
)

// 支持的图片格式
private val IMAGE_EXTENSIONS = setOf(
    "jpg", "jpeg", "png", "webp"
)

/**
 * LocalFolderParser - 本地文件夹解析器
 * 
 * 功能：
 * 1. 递归扫描文件夹（最多3层深度）
 * 2. 识别包含视频文件的文件夹作为剧集
 * 3. 查找封面图片（按 Jellyfin 规则）
 * 4. 视频文件自然排序
 * 5. 读取已有的进度文件
 * 6. 生成剧集和剧集数据
 * 
 * 支持的文件夹结构：
 * - 结构1：文件夹直接包含视频
 * - 结构2：子文件夹包含视频（最多3层）
 */
object LocalFolderParser {
    
    // JSON 解析器
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // 最大扫描深度（相对于根文件夹）
    private const val MAX_DEPTH = 3
    
    /**
     * 解析文件夹
     * 
     * @param context 上下文
     * @param folderUri 文件夹 URI
     * @param rootFolderName 根文件夹名称（用作标签）
     * @return 扫描到的剧集列表
     */
    fun parseFolder(
        context: Context,
        folderUri: Uri,
        rootFolderName: String
    ): List<ParsedSeries> {
        val rootFolder = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()
        val result = mutableListOf<ParsedSeries>()
        
        // 扫描文件夹（深度为0）
        // 根文件夹的父文件夹名称就是根文件夹本身
        scanFolder(
            context = context,
            folder = rootFolder,
            rootFolderName = rootFolderName,
            currentDepth = 0,
            parentFolderName = rootFolderName,  // 根文件夹的父文件夹名称就是自己
            result = result
        )
        
        return result
    }
    
    /**
     * 递归扫描文件夹
     * 
     * @param context 上下文
     * @param folder 当前文件夹
     * @param rootFolderName 根文件夹名称
     * @param currentDepth 当前深度（0 = 根文件夹）
     * @param parentFolderName 父文件夹名称（用作标签）
     * @param result 结果列表
     */
    private fun scanFolder(
        context: Context,
        folder: DocumentFile,
        rootFolderName: String,
        currentDepth: Int,
        parentFolderName: String?,
        result: MutableList<ParsedSeries>
    ) {
        // 超过最大深度，停止递归
        if (currentDepth > MAX_DEPTH) {
            return
        }
        
        // 读取文件夹内容
        val childDirs = mutableListOf<DocumentFile>()
        val videoFiles = mutableListOf<DocumentFile>()
        val imageFiles = mutableListOf<DocumentFile>()
        
        folder.listFiles().forEach { file ->
            when {
                file.isDirectory -> childDirs.add(file)
                file.isVideoFile() -> videoFiles.add(file)
                file.isImageFile() -> imageFiles.add(file)
            }
        }
        
        // 判断：如果当前文件夹包含视频，作为一个剧集
        if (videoFiles.isNotEmpty()) {
            val series = parseSingleSeries(
                context = context,
                folder = folder,
                videoFiles = videoFiles,
                imageFiles = imageFiles,
                rootFolderName = rootFolderName,
                parentFolderName = parentFolderName
            )
            result.add(series)
            // 不再向下递归（剧集文件夹的子文件夹不处理）
            return
        }
        
        // 如果当前文件夹不包含视频，递归扫描子文件夹
        childDirs.forEach { childDir ->
            // 当前文件夹的名称将作为子文件夹的父文件夹名称（标签）
            // 如果是根文件夹(depth=0)，使用根文件夹名；否则使用当前文件夹名
            val currentFolderName = if (currentDepth == 0) {
                rootFolderName
            } else {
                folder.name ?: rootFolderName
            }
            
            scanFolder(
                context = context,
                folder = childDir,
                rootFolderName = rootFolderName,
                currentDepth = currentDepth + 1,
                parentFolderName = currentFolderName,  // 传递当前文件夹名称
                result = result
            )
        }
    }
    
    /**
     * 解析单个剧集文件夹
     * 
     * @param context 上下文
     * @param folder 剧集文件夹
     * @param videoFiles 视频文件列表
     * @param imageFiles 图片文件列表
     * @param rootFolderName 根文件夹名称
     * @param parentFolderName 父文件夹名称（标签）
     * @return 解析后的剧集数据
     */
    private fun parseSingleSeries(
        context: Context,
        folder: DocumentFile,
        videoFiles: List<DocumentFile>,
        imageFiles: List<DocumentFile>,
        rootFolderName: String,
        parentFolderName: String?
    ): ParsedSeries {
        val folderName = folder.name ?: "未命名剧集"
        val folderUri = folder.uri.toString()
        
        // 1. 视频文件自然排序
        val sortedVideos = videoFiles.sortedWith(NaturalOrderComparator())
        
        // 2. 查找封面图片
        val coverImage = findCoverImage(imageFiles, folderName)
        
        // 3. 尝试读取进度文件
        val progressData = readProgressFile(context, folder)
        
        // 4. 创建剧集数据
        val seriesData = SeriesLocalData.fromLocalFolder(
            folderName = folderName,
            folderPath = folderUri,
            coverImagePath = coverImage?.uri?.toString(),
            episodeCount = sortedVideos.size,
            rootFolderName = rootFolderName,
            parentFolderName = parentFolderName
        )
        
        // 5. 创建剧集数据（包含进度）
        val episodes = sortedVideos.mapIndexed { index, videoFile ->
            val episodeNumber = index + 1
            val fileName = videoFile.name ?: "未命名"
            val fileNameWithoutExt = fileName.substringBeforeLast('.')
            
            // 从进度文件中查找该视频的进度
            val progress = progressData?.episodes?.find { 
                it.fileName.equals(fileName, ignoreCase = true) 
            }
            
            // 创建剧集数据
            val episode = EpisodeLocalData.fromLocalFile(
                seriesId = seriesData.id,
                fileName = fileNameWithoutExt,
                filePath = videoFile.uri.toString(),
                episodeNumber = episodeNumber,
                fileSize = videoFile.length(),
                fileFormat = videoFile.extension
            )
            
            // 如果有进度，更新播放进度
            if (progress != null) {
                episode.copy(
                    playbackPositionTicks = progress.playbackPositionTicks,
                    runTimeTicks = progress.runTimeTicks,
                    played = progress.played,
                    lastPlayedDate = progress.lastPlayedDate
                )
            } else {
                episode
            }
        }
        
        return ParsedSeries(
            series = seriesData,
            episodes = episodes
        )
    }
    
    /**
     * 查找封面图片
     * 
     * 按照 Jellyfin 规则查找：
     * 1. poster.*
     * 2. cover.*
     * 3. folder.*
     * 4. [剧集名称].*
     * 5. 按字母顺序的第一个图片
     * 
     * @param imageFiles 图片文件列表
     * @param folderName 文件夹名称
     * @return 封面图片文件，如果没有返回 null
     */
    private fun findCoverImage(
        imageFiles: List<DocumentFile>,
        folderName: String
    ): DocumentFile? {
        if (imageFiles.isEmpty()) return null
        
        // 优先级列表
        val priorityNames = listOf("poster", "cover", "folder", folderName.lowercase())
        
        // 按优先级查找
        priorityNames.forEach { name ->
            imageFiles.find { 
                it.nameWithoutExtension.equals(name, ignoreCase = true) 
            }?.let { return it }
        }
        
        // 如果没有匹配，返回按字母顺序的第一个
        return imageFiles.minByOrNull { it.name ?: "" }
    }
    
    /**
     * 读取进度文件
     * 
     * @param context 上下文
     * @param folder 剧集文件夹
     * @return 进度数据，如果不存在返回 null
     */
    private fun readProgressFile(
        context: Context,
        folder: DocumentFile
    ): LocalProgressData? {
        return try {
            // 查找进度文件
            val progressFile = folder.listFiles().find { 
                it.name == LocalProgressData.PROGRESS_FILE_NAME 
            } ?: return null
            
            // 读取文件内容
            val inputStream = context.contentResolver.openInputStream(progressFile.uri)
                ?: return null
            
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            
            // 解析 JSON
            json.decodeFromString<LocalProgressData>(jsonString)
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 自然排序比较器
     * 
     * 支持数字排序：第1集 < 第2集 < 第10集
     * 而不是字典序：第1集 < 第10集 < 第2集
     */
    private class NaturalOrderComparator : Comparator<DocumentFile> {
        override fun compare(a: DocumentFile, b: DocumentFile): Int {
            return compareNatural(a.name ?: "", b.name ?: "")
        }
        
        private fun compareNatural(a: String, b: String): Int {
            // 提取所有数字
            val numA = extractNumbers(a)
            val numB = extractNumbers(b)
            
            // 如果都有数字，按数字比较
            if (numA.isNotEmpty() && numB.isNotEmpty()) {
                for (i in 0 until minOf(numA.size, numB.size)) {
                    val cmp = numA[i].compareTo(numB[i])
                    if (cmp != 0) return cmp
                }
                // 数字相同，按长度比较
                return numA.size.compareTo(numB.size)
            }
            
            // 否则按字符串比较
            return a.compareTo(b)
        }
        
        private fun extractNumbers(str: String): List<Int> {
            val regex = """\d+""".toRegex()
            return regex.findAll(str)
                .mapNotNull { it.value.toIntOrNull() }
                .toList()
        }
    }
}

/**
 * ParsedSeries - 解析后的剧集数据
 * 
 * @property series 剧集数据
 * @property episodes 剧集列表
 */
data class ParsedSeries(
    val series: SeriesLocalData,
    val episodes: List<EpisodeLocalData>
)

// ==================== DocumentFile 扩展函数 ====================

/**
 * 判断是否为视频文件
 */
private fun DocumentFile.isVideoFile(): Boolean {
    return this.extension?.lowercase() in VIDEO_EXTENSIONS
}

/**
 * 判断是否为图片文件
 */
private fun DocumentFile.isImageFile(): Boolean {
    return this.extension?.lowercase() in IMAGE_EXTENSIONS
}

/**
 * 获取文件扩展名（不含点）
 */
private val DocumentFile.extension: String?
    get() = this.name?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() }

/**
 * 获取文件名（不含扩展名）
 */
private val DocumentFile.nameWithoutExtension: String
    get() = this.name?.substringBeforeLast('.') ?: "未命名"

