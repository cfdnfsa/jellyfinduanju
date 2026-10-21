package com.wjp.jellyfinduanju.ui.screens.download

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wjp.jellyfinduanju.data.model.DownloadStatus
import com.wjp.jellyfinduanju.data.model.DownloadTask

/**
 * DownloadScreen - 下载管理页面
 * 
 * 显示：
 * 1. 正在下载的任务（带进度条和暂停/取消按钮）
 * 2. 等待中的任务（排队中）
 * 3. 失败的任务（带重试按钮）
 * 4. 已完成的下载（按剧集分组）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel,
    onNavigateToDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 删除确认对话框状态
    var showDeleteDialog by remember { mutableStateOf<DeleteDialogState?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("下载管理") }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)  // 禁用Scaffold的自动系统栏padding，实现边到边效果
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.downloadingTasks.isEmpty() && 
                   uiState.pendingTasks.isEmpty() && 
                   uiState.failedTasks.isEmpty() && 
                   uiState.completedGroups.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无下载任务",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "在详情页点击缓存按钮添加下载",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 正在下载
                if (uiState.downloadingTasks.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "正在下载",
                            count = uiState.downloadingTasks.size
                        )
                    }
                    items(uiState.downloadingTasks, key = { it.id }) { task ->
                        DownloadingTaskItem(
                            task = task,
                            onPause = { viewModel.pauseDownload(task.id) },
                            onCancel = { 
                                showDeleteDialog = DeleteDialogState.CancelTask(task)
                            }
                        )
                    }
                }
                
                // 等待中/已暂停
                if (uiState.pendingTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(
                            title = "等待中",
                            count = uiState.pendingTasks.size
                        )
                    }
                    items(uiState.pendingTasks, key = { it.id }) { task ->
                        PendingTaskItem(
                            task = task,
                            onResume = { viewModel.resumeDownload(task.id) },
                            onCancel = { 
                                showDeleteDialog = DeleteDialogState.CancelTask(task)
                            }
                        )
                    }
                }
                
                // 失败
                if (uiState.failedTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(
                            title = "下载失败",
                            count = uiState.failedTasks.size
                        )
                    }
                    items(uiState.failedTasks, key = { it.id }) { task ->
                        FailedTaskItem(
                            task = task,
                            onRetry = { viewModel.retryDownload(task.id) },
                            onCancel = { 
                                showDeleteDialog = DeleteDialogState.CancelTask(task)
                            }
                        )
                    }
                }
                
                // 已完成
                if (uiState.completedGroups.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(
                            title = "已完成",
                            count = uiState.completedGroups.sumOf { it.episodeCount }
                        )
                    }
                    items(uiState.completedGroups, key = { it.seriesId }) { group ->
                        CompletedSeriesItem(
                            group = group,
                            onNavigateToDetail = { onNavigateToDetail(group.seriesId) },
                            onDeleteEpisode = { task ->
                                showDeleteDialog = DeleteDialogState.DeleteEpisode(task)
                            },
                            onDeleteSeries = {
                                showDeleteDialog = DeleteDialogState.DeleteSeries(group)
                            }
                        )
                    }
                }
                
                // 底部留白
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
    
    // 删除确认对话框
    showDeleteDialog?.let { state ->
        DeleteConfirmDialog(
            state = state,
            onConfirm = {
                when (state) {
                    is DeleteDialogState.CancelTask -> viewModel.cancelDownload(state.task.id)
                    is DeleteDialogState.DeleteEpisode -> viewModel.deleteCompletedDownload(state.task.id)
                    is DeleteDialogState.DeleteSeries -> viewModel.deleteSeriesDownloads(state.group.seriesId)
                }
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }
}

// ==================== Section Header ====================

@Composable
private fun SectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== Downloading Task Item ====================

@Composable
private fun DownloadingTaskItem(
    task: DownloadTask,
    onPause: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.seriesName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "第${task.seasonNumber}季 第${task.episodeNumber}集",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 暂停按钮
                IconButton(onClick = onPause) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "暂停",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 取消按钮
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "取消",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 进度条
            LinearProgressIndicator(
                progress = { task.getProgressPercent() / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 进度文字
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = task.getProgressText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${task.getProgressPercent()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ==================== Pending Task Item ====================

@Composable
private fun PendingTaskItem(
    task: DownloadTask,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val isPaused = task.status == DownloadStatus.PAUSED
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.seriesName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "第${task.seasonNumber}季 第${task.episodeNumber}集 · ${if (isPaused) "已暂停" else "等待中..."}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 如果是暂停状态，显示恢复按钮
            if (isPaused) {
                IconButton(onClick = onResume) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "恢复",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 取消按钮
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "取消",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ==================== Failed Task Item ====================

@Composable
private fun FailedTaskItem(
    task: DownloadTask,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.seriesName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "第${task.seasonNumber}季 第${task.episodeNumber}集",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                task.errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // 重试按钮
            IconButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "重试",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // 取消按钮
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ==================== Completed Series Item ====================

@Composable
private fun CompletedSeriesItem(
    group: CompletedSeriesGroup,
    onNavigateToDetail: () -> Unit,
    onDeleteEpisode: (DownloadTask) -> Unit,
    onDeleteSeries: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (group.isDeletedFromServer) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 标题行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToDetail() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = group.seriesName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (group.isDeletedFromServer) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "已从服务器删除",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${group.episodeCount}集 · ${group.getTotalSizeText()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 删除整个剧集
                IconButton(onClick = onDeleteSeries) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除全部",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                
                // 展开/收起
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 展开的剧集列表
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                ) {
                    group.episodes.forEach { episode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "第${episode.seasonNumber}季 第${episode.episodeNumber}集",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = episode.getTotalSizeText(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = { onDeleteEpisode(episode) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== Delete Dialog ====================

sealed class DeleteDialogState {
    data class CancelTask(val task: DownloadTask) : DeleteDialogState()
    data class DeleteEpisode(val task: DownloadTask) : DeleteDialogState()
    data class DeleteSeries(val group: CompletedSeriesGroup) : DeleteDialogState()
}

@Composable
private fun DeleteConfirmDialog(
    state: DeleteDialogState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, message) = when (state) {
        is DeleteDialogState.CancelTask -> {
            "取消下载" to "确定要取消下载「${state.task.seriesName} 第${state.task.seasonNumber}季 第${state.task.episodeNumber}集」吗？"
        }
        is DeleteDialogState.DeleteEpisode -> {
            "删除缓存" to "确定要删除「${state.task.seriesName} 第${state.task.seasonNumber}季 第${state.task.episodeNumber}集」的缓存吗？"
        }
        is DeleteDialogState.DeleteSeries -> {
            "删除全部缓存" to "确定要删除「${state.group.seriesName}」的全部 ${state.group.episodeCount} 集缓存吗？\n\n这将释放 ${state.group.getTotalSizeText()} 的存储空间。"
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "确定",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

