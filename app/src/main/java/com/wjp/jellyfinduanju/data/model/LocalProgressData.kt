package com.wjp.jellyfinduanju.data.model

import kotlinx.serialization.Serializable

/**
 * LocalProgressData - 本地进度文件数据结构
 * 
 * 保存在剧集文件夹下的 .jellyfinduanju_progress.json 文件中
 * 用于记录播放进度，即使文件夹被重命名或移动，进度也不会丢失
 * 
 * 文件位置示例：
 * /Movies/剧集A/.jellyfinduanju_progress.json
 * 
 * @property seriesId 剧集 ID（用于关联）
 * @property seriesName 剧集名称
 * @property lastScanTime 最后扫描时间（毫秒时间戳）
 * @property fileModifiedTime 文件修改时间（用于冲突检测）
 * @property episodes 剧集列表
 */
@Serializable
data class LocalProgressData(
    val seriesId: String,
    val seriesName: String,
    val lastScanTime: Long,
    val fileModifiedTime: Long = System.currentTimeMillis(),
    val episodes: List<EpisodeProgress>
) {
    companion object {
        /** 进度文件名 */
        const val PROGRESS_FILE_NAME = ".jellyfinduanju_progress.json"
    }
}

/**
 * EpisodeProgress - 单集播放进度
 * 
 * 记录每一集的播放状态，通过文件名来匹配
 * 
 * @property fileName 文件名（含扩展名，用于匹配视频文件）
 * @property episodeNumber 集编号（1, 2, 3...）
 * @property playbackPositionTicks 播放进度（单位：ticks，1秒 = 10,000,000 ticks）
 * @property runTimeTicks 总时长（单位：ticks）
 * @property played 是否已看完
 * @property lastPlayedDate 最后播放时间（毫秒时间戳）
 */
@Serializable
data class EpisodeProgress(
    val fileName: String,
    val episodeNumber: Int,
    val playbackPositionTicks: Long = 0,
    val runTimeTicks: Long = 0,
    val played: Boolean = false,
    val lastPlayedDate: Long = 0
) {
    /**
     * 计算播放进度百分比
     * 
     * @return 0-100 的进度值
     */
    fun getProgressPercent(): Int {
        if (played) return 100
        if (runTimeTicks <= 0) return 0
        return ((playbackPositionTicks.toDouble() / runTimeTicks) * 100).toInt().coerceIn(0, 100)
    }
    
    /**
     * 格式化进度显示
     * 
     * @return 进度文本
     */
    fun getProgressText(): String {
        return when {
            played -> "100%"
            playbackPositionTicks > 0 -> "${getProgressPercent()}%"
            else -> "--"
        }
    }
    
    companion object {
        /**
         * 从 EpisodeLocalData 创建进度数据
         * 
         * @param episode 剧集数据
         * @param fileName 文件名（含扩展名）
         * @return 进度数据
         */
        fun fromEpisodeLocalData(episode: EpisodeLocalData, fileName: String): EpisodeProgress {
            return EpisodeProgress(
                fileName = fileName,
                episodeNumber = episode.episodeNumber,
                playbackPositionTicks = episode.playbackPositionTicks,
                runTimeTicks = episode.runTimeTicks,
                played = episode.played,
                lastPlayedDate = episode.lastPlayedDate
            )
        }
    }
}

