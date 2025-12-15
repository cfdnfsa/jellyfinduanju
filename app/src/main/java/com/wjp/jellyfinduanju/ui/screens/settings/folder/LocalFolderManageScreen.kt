package com.wjp.jellyfinduanju.ui.screens.settings.folder

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wjp.jellyfinduanju.data.settings.AppSettings
import com.wjp.jellyfinduanju.data.settings.LocalFolderReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * LocalFolderManageScreen - 本地文件夹管理页面
 * 
 * 功能：
 * 1. 显示已添加的文件夹列表
 * 2. 添加新文件夹（通过 SAF）
 * 3. 删除文件夹引用
 * 4. 手动扫描文件夹
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalFolderManageScreen(
    onBack: () -> Unit,
    onDeleteFolder: (String) -> Unit = {},  // 删除文件夹时的回调
    onScanFolder: (String) -> Unit = {}     // 扫描文件夹的回调
) {
    val context = LocalContext.current
    
    // 文件夹列表状态
    var folders by remember { mutableStateOf(AppSettings.localFolderReferences) }
    
    // 删除确认对话框状态
    var folderToDelete by remember { mutableStateOf<LocalFolderReference?>(null) }
    
    // 正在扫描的文件夹URI（用于显示扫描状态）
    var scanningFolderUri by remember { mutableStateOf<String?>(null) }
    
    // 文件夹选择器的 Launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // 获取持久化权限
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                
                // 提取文件夹显示名称
                val displayName = extractFolderName(uri)
                
                // 保存文件夹引用（不自动扫描）
                val success = AppSettings.addLocalFolderReference(uri.toString(), displayName)
                
                if (success) {
                    // 更新列表
                    folders = AppSettings.localFolderReferences
                    
                    // TODO: 显示 Toast 提示已添加
                } else {
                    // TODO: 显示 Toast 提示文件夹已存在
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                // TODO: 显示 Toast 提示权限获取失败
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("本地文件夹") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { folderPickerLauncher.launch(null) }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加文件夹"
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        if (folders.isEmpty()) {
            // 空状态
            EmptyFolderState(modifier = Modifier.padding(paddingValues))
        } else {
            // 文件夹列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(folders) { folder ->
                    FolderItem(
                        folder = folder,
                        isScanning = scanningFolderUri == folder.uri,
                        onDelete = { folderToDelete = folder },
                        onScan = { 
                            // 如果当前没有正在扫描的文件夹，才允许扫描
                            if (scanningFolderUri == null) {
                                scanningFolderUri = folder.uri
                                onScanFolder(folder.uri)
                                // TODO: 扫描完成后需要重置 scanningFolderUri 并刷新列表
                            }
                        }
                    )
                }
            }
        }
    }
    
    // 删除确认对话框
    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("删除文件夹引用") },
            text = { 
                Text(
                    "确定要删除文件夹「${folder.displayName}」的引用吗？\n\n" +
                    "该文件夹下的所有剧集记录和播放进度将被删除。\n" +
                    "⚠️ 本地文件不会被删除。"
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 删除文件夹引用和相关剧集数据
                        onDeleteFolder(folder.uri)
                        AppSettings.removeLocalFolderReference(folder.uri)
                        folders = AppSettings.localFolderReferences
                        folderToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * EmptyFolderState - 空状态组件
 */
@Composable
private fun EmptyFolderState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无本地文件夹",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击右下角按钮添加文件夹",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * FolderItem - 文件夹列表项
 */
@Composable
private fun FolderItem(
    folder: LocalFolderReference,
    isScanning: Boolean,
    onDelete: () -> Unit,
    onScan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (folder.permissionStatus < 0) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 顶部：图标 + 名称 + 状态图标
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (folder.permissionStatus < 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = folder.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        // 权限失效警告图标
                        if (folder.permissionStatus < 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "权限失效",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 状态文本
                    Text(
                        text = when {
                            isScanning -> "正在扫描..."
                            folder.permissionStatus < 0 -> "权限已失效，请重新授权"
                            folder.seriesCount == 0 -> "未扫描或无剧集"
                            else -> "${folder.seriesCount} 个剧集"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isScanning -> MaterialTheme.colorScheme.primary
                            folder.permissionStatus < 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    // 最后扫描时间
                    if (folder.lastScanTime > 0) {
                        Text(
                            text = "最后扫描：${formatTime(folder.lastScanTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // 扫描进度条
            if (isScanning) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 底部：操作按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                // 扫描按钮
                TextButton(
                    onClick = onScan,
                    enabled = !isScanning  // 正在扫描时禁用按钮
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (folder.permissionStatus < 0) "重新授权" else "扫描")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 删除按钮
                TextButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/**
 * 从 URI 中提取文件夹名称
 */
private fun extractFolderName(uri: Uri): String {
    return try {
        val path = uri.lastPathSegment ?: uri.toString()
        // 提取最后一段路径作为显示名称
        // 格式通常是 "primary:Movies/MyVideos" 或 "1234-5678:Movies"
        val segments = path.split(":")
        if (segments.size > 1) {
            // 取冒号后面的部分
            val pathPart = segments[1]
            // 取最后一个斜杠后面的部分
            pathPart.split("/").lastOrNull()?.ifEmpty { null } ?: pathPart
        } else {
            path
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "未知文件夹"
    }
}

/**
 * 格式化时间戳为可读字符串
 */
private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3600_000 -> "${diff / 60_000}分钟前"
        diff < 86400_000 -> "${diff / 3600_000}小时前"
        diff < 604800_000 -> "${diff / 86400_000}天前"
        else -> {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

