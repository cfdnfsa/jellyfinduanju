package com.wjp.jellyfinduanju.data.local

import com.tencent.mmkv.MMKV
import com.wjp.jellyfinduanju.data.model.DownloadStatus
import com.wjp.jellyfinduanju.data.model.DownloadTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * DownloadDataStore - 下载任务本地数据存储
 * 
 * 使用 MMKV 持久化下载任务状态
 * 
 * 存储结构：
 * - download_tasks: Map<String, DownloadTask> 所有下载任务
 * 
 * 功能：
 * 1. 保存/更新下载任务
 * 2. 查询下载任务
 * 3. 删除下载任务
 * 4. 提供响应式数据流
 */
class DownloadDataStore {
    
    private val mmkv = MMKV.mmkvWithID("download_data")
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // ==================== Keys ====================
    
    private object Keys {
        const val DOWNLOAD_TASKS = "download_tasks"
    }
    
    // ==================== 响应式数据流 ====================
    
    private val _tasksFlow = MutableStateFlow(loadAllTasks())
    val tasksFlow: Flow<List<DownloadTask>> = _tasksFlow
    
    // ==================== 保存/更新 ====================
    
    /**
     * 保存或更新下载任务
     */
    fun saveTask(task: DownloadTask) {
        val tasks = loadTasksMap().toMutableMap()
        tasks[task.id] = task.copy(updateTime = System.currentTimeMillis())
        saveTasksMap(tasks)
        _tasksFlow.value = tasks.values.toList()
    }
    
    /**
     * 批量保存下载任务
     */
    fun saveTasks(taskList: List<DownloadTask>) {
        val tasks = loadTasksMap().toMutableMap()
        taskList.forEach { task ->
            tasks[task.id] = task.copy(updateTime = System.currentTimeMillis())
        }
        saveTasksMap(tasks)
        _tasksFlow.value = tasks.values.toList()
    }
    
    /**
     * 更新任务进度
     */
    fun updateProgress(taskId: String, downloadedBytes: Long, totalBytes: Long) {
        val tasks = loadTasksMap().toMutableMap()
        tasks[taskId]?.let { task ->
            tasks[taskId] = task.copy(
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                updateTime = System.currentTimeMillis()
            )
            saveTasksMap(tasks)
            _tasksFlow.value = tasks.values.toList()
        }
    }
    
    /**
     * 更新任务状态
     */
    fun updateStatus(taskId: String, status: DownloadStatus, errorMessage: String? = null) {
        val tasks = loadTasksMap().toMutableMap()
        tasks[taskId]?.let { task ->
            tasks[taskId] = task.copy(
                status = status,
                errorMessage = errorMessage,
                updateTime = System.currentTimeMillis()
            )
            saveTasksMap(tasks)
            _tasksFlow.value = tasks.values.toList()
        }
    }
    
    /**
     * 标记任务完成
     */
    fun markCompleted(taskId: String, localPath: String) {
        val tasks = loadTasksMap().toMutableMap()
        tasks[taskId]?.let { task ->
            tasks[taskId] = task.copy(
                status = DownloadStatus.COMPLETED,
                localPath = localPath,
                downloadedBytes = task.totalBytes,
                updateTime = System.currentTimeMillis()
            )
            saveTasksMap(tasks)
            _tasksFlow.value = tasks.values.toList()
        }
    }
    
    // ==================== 查询 ====================
    
    /**
     * 获取所有任务
     */
    fun getAllTasks(): List<DownloadTask> {
        return loadAllTasks()
    }
    
    /**
     * 获取单个任务
     */
    fun getTask(taskId: String): DownloadTask? {
        return loadTasksMap()[taskId]
    }
    
    /**
     * 获取指定状态的任务
     */
    fun getTasksByStatus(status: DownloadStatus): List<DownloadTask> {
        return loadAllTasks().filter { it.status == status }
    }
    
    /**
     * 获取正在进行的任务（下载中 + 等待中）
     */
    fun getActiveTasks(): List<DownloadTask> {
        return loadAllTasks().filter { 
            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING 
        }
    }
    
    /**
     * 获取指定剧集的所有任务
     */
    fun getTasksBySeries(seriesId: String): List<DownloadTask> {
        return loadAllTasks().filter { it.seriesId == seriesId }
    }
    
    /**
     * 获取正在下载的任务数量
     */
    fun getDownloadingCount(): Int {
        return loadAllTasks().count { it.status == DownloadStatus.DOWNLOADING }
    }
    
    /**
     * 检查任务是否存在
     */
    fun hasTask(taskId: String): Boolean {
        return loadTasksMap().containsKey(taskId)
    }
    
    // ==================== 删除 ====================
    
    /**
     * 删除任务
     */
    fun deleteTask(taskId: String) {
        val tasks = loadTasksMap().toMutableMap()
        tasks.remove(taskId)
        saveTasksMap(tasks)
        _tasksFlow.value = tasks.values.toList()
    }
    
    /**
     * 删除指定剧集的所有任务
     */
    fun deleteTasksBySeries(seriesId: String) {
        val tasks = loadTasksMap().toMutableMap()
        tasks.entries.removeAll { it.value.seriesId == seriesId }
        saveTasksMap(tasks)
        _tasksFlow.value = tasks.values.toList()
    }
    
    /**
     * 删除已完成的任务
     */
    fun deleteCompletedTasks() {
        val tasks = loadTasksMap().toMutableMap()
        tasks.entries.removeAll { it.value.status == DownloadStatus.COMPLETED }
        saveTasksMap(tasks)
        _tasksFlow.value = tasks.values.toList()
    }
    
    /**
     * 清除所有任务
     */
    fun clearAll() {
        mmkv.removeValueForKey(Keys.DOWNLOAD_TASKS)
        _tasksFlow.value = emptyList()
    }
    
    // ==================== 私有方法 ====================
    
    private fun loadTasksMap(): Map<String, DownloadTask> {
        val jsonString = mmkv.decodeString(Keys.DOWNLOAD_TASKS)
        return if (jsonString.isNullOrEmpty()) {
            emptyMap()
        } else {
            try {
                json.decodeFromString<Map<String, DownloadTask>>(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyMap()
            }
        }
    }
    
    private fun loadAllTasks(): List<DownloadTask> {
        return loadTasksMap().values.toList().sortedByDescending { it.createTime }
    }
    
    private fun saveTasksMap(tasks: Map<String, DownloadTask>) {
        val jsonString = json.encodeToString(tasks)
        mmkv.encode(Keys.DOWNLOAD_TASKS, jsonString)
    }
}

