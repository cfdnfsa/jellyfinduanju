package com.wjp.jellyfinduanju.ui.screens.settings.folder

import android.content.Intent
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
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
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
 * 4. 显示文件夹状态（权限、剧集数量等）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalFolderManageScreen(
    onBack: () -> Unit,
    viewModel: LocalFolderManageViewModel = viewModel(
        factory = LocalFolderManageViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 删除确认对话框状态
    var folderToDelete by remember { mutableStateOf<LocalFolderReference?>(null) }

    // 文件夹选择器的 Launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addFolder(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("本地文件夹") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { folderPickerLauncher.launch(null) }
            ) {
                Icon(Icons.Default.Add, "添加文件夹")
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        if (uiState.folders.isEmpty()) {
            EmptyFolderState(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.folders, key = { it.uri }) { folderRef ->
                    FolderItem(
                        folderRef = folderRef,
                        isScanning = uiState.scanningFolderUri == folderRef.uri,
                        scanProgress = uiState.scanProgress,
                        onDelete = { folderToDelete = folderRef },
                        onRescan = { viewModel.scanFolder(folderRef.uri, folderRef.displayName) }
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
            text = { Text("确定要删除文件夹「${folder.displayName}」的引用吗？\n这将删除该文件夹下所有剧集的记录，但不会删除本地文件。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFolder(folder.uri)
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

@Composable
private fun FolderItem(
    folderRef: LocalFolderReference,
    isScanning: Boolean,
    scanProgress: Float,
    onDelete: () -> Unit,
    onRescan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (folderRef.permissionStatus) {
                            -1 -> Icons.Default.Warning // 权限失效
                            else -> Icons.Default.Folder
                        },
                        contentDescription = null,
                        tint = when (folderRef.permissionStatus) {
                            -1 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = folderRef.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 重新扫描/重新授权按钮
                IconButton(
                    onClick = onRescan,
                    enabled = !isScanning
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (folderRef.permissionStatus == -1) "重新授权" else "扫描",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 删除按钮
                IconButton(
                    onClick = onDelete,
                    enabled = !isScanning
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // 状态和进度
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    isScanning -> "正在扫描..."
                    folderRef.permissionStatus == -1 -> "权限已失效，请重新授权"
                    folderRef.seriesCount == 0 -> "未扫描或无剧集"
                    else -> "${folderRef.seriesCount} 个剧集"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isScanning -> MaterialTheme.colorScheme.primary
                    folderRef.permissionStatus == -1 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (folderRef.lastScanTime > 0 && !isScanning) {
                Text(
                    text = "最后扫描：${formatTime(folderRef.lastScanTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 扫描进度条
            if (isScanning) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { scanProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

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
            text = "点击右下角按钮添加本地文件夹",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * 从 URI 中提取文件夹名称
 */
private fun extractFolderName(uri: Uri): String {
    return try {
        val path = uri.lastPathSegment ?: uri.toString()
        path.split(":").lastOrNull()?.split("/")?.lastOrNull() ?: path
    } catch (e: Exception) {
        uri.toString()
    }
}

/**
 * 格式化时间戳为可读字符串
 */
private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
