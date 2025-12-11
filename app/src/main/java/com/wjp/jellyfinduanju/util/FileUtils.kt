package com.wjp.jellyfinduanju.util

import java.io.File

/**
 * FileUtils - 文件操作工具类
 * 
 * 提供常用的文件操作方法
 */
object FileUtils {
    
    /**
     * 确保目录存在
     * 
     * @param path 目录路径
     * @return 是否成功创建或目录已存在
     */
    fun ensureDirectoryExists(path: String): Boolean {
        val dir = File(path)
        return if (!dir.exists()) {
            dir.mkdirs()
        } else {
            true
        }
    }
    
    /**
     * 删除目录及其所有内容
     * 
     * @param path 目录路径
     * @return 是否成功删除
     */
    fun deleteDirectory(path: String): Boolean {
        val dir = File(path)
        return deleteDirectoryRecursively(dir)
    }
    
    /**
     * 递归删除目录
     */
    private fun deleteDirectoryRecursively(file: File): Boolean {
        if (!file.exists()) return true
        
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteDirectoryRecursively(child)
            }
        }
        return file.delete()
    }
    
    /**
     * 清空目录内容（保留目录本身）
     * 
     * @param path 目录路径
     * @return 是否成功清空
     */
    fun clearDirectory(path: String): Boolean {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return false
        
        var success = true
        dir.listFiles()?.forEach { child ->
            if (!deleteDirectoryRecursively(child)) {
                success = false
            }
        }
        return success
    }
    
    /**
     * 获取目录大小（字节）
     * 
     * @param path 目录路径
     * @return 目录大小（字节）
     */
    fun getDirectorySize(path: String): Long {
        val dir = File(path)
        return getDirectorySizeRecursively(dir)
    }
    
    /**
     * 递归计算目录大小
     */
    private fun getDirectorySizeRecursively(file: File): Long {
        if (!file.exists()) return 0L
        
        if (file.isFile) {
            return file.length()
        }
        
        var size = 0L
        file.listFiles()?.forEach { child ->
            size += getDirectorySizeRecursively(child)
        }
        return size
    }
    
    /**
     * 获取目录中的文件数量
     * 
     * @param path 目录路径
     * @return 文件数量
     */
    fun getFileCount(path: String): Int {
        val dir = File(path)
        return getFileCountRecursively(dir)
    }
    
    /**
     * 递归计算文件数量
     */
    private fun getFileCountRecursively(file: File): Int {
        if (!file.exists()) return 0
        
        if (file.isFile) return 1
        
        var count = 0
        file.listFiles()?.forEach { child ->
            count += getFileCountRecursively(child)
        }
        return count
    }
    
    /**
     * 格式化文件大小为可读字符串
     * 
     * @param bytes 字节数
     * @return 格式化后的字符串（如 "1.5 MB"）
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return if (unitIndex == 0) {
            "${bytes.toInt()} ${units[unitIndex]}"
        } else {
            String.format("%.2f %s", size, units[unitIndex])
        }
    }
    
    /**
     * 检查目录是否存在
     * 
     * @param path 目录路径
     * @return 是否存在
     */
    fun directoryExists(path: String): Boolean {
        val dir = File(path)
        return dir.exists() && dir.isDirectory
    }
    
    /**
     * 检查目录是否为空
     * 
     * @param path 目录路径
     * @return 是否为空（不存在也返回 true）
     */
    fun isDirectoryEmpty(path: String): Boolean {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return true
        return dir.listFiles()?.isEmpty() ?: true
    }
    
    /**
     * 移动目录内容到新位置
     * 
     * @param sourcePath 源目录
     * @param destPath 目标目录
     * @return 是否成功
     */
    fun moveDirectoryContents(sourcePath: String, destPath: String): Boolean {
        val sourceDir = File(sourcePath)
        val destDir = File(destPath)
        
        if (!sourceDir.exists() || !sourceDir.isDirectory) return false
        
        // 确保目标目录存在
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        
        var success = true
        sourceDir.listFiles()?.forEach { file ->
            val destFile = File(destDir, file.name)
            if (!file.renameTo(destFile)) {
                success = false
            }
        }
        return success
    }
}

