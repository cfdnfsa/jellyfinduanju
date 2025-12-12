package com.wjp.jellyfinduanju.ui.screens.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wjp.jellyfinduanju.data.settings.AppSettings

/**
 * TagManageScreen - 自定义标签管理页面
 * 
 * 功能：
 * 1. 显示所有自定义标签列表
 * 2. 添加新标签
 * 3. 删除标签
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManageScreen(
    onBack: () -> Unit,
    onDeleteTag: (String) -> Unit = {}  // 删除标签时的回调，用于同步移除剧集中的标签
) {
    // 标签列表状态
    var tags by remember { mutableStateOf(AppSettings.customTags) }
    
    // 添加标签对话框状态
    var showAddDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    
    // 删除确认对话框状态
    var tagToDelete by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自定义标签") },
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
                onClick = { showAddDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加标签"
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)  // 禁用Scaffold的自动系统栏padding，实现边到边效果
    ) { paddingValues ->
        if (tags.isEmpty()) {
            // 空状态
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Label,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无自定义标签",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "点击右下角按钮添加标签",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        } else {
            // 标签列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tags) { tag ->
                    TagItem(
                        tag = tag,
                        onDelete = { tagToDelete = tag }
                    )
                }
            }
        }
    }
    
    // 添加标签对话框
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                newTagName = ""
            },
            title = { Text("添加标签") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("标签名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            AppSettings.addCustomTag(newTagName)
                            tags = AppSettings.customTags
                            newTagName = ""
                        }
                        showAddDialog = false
                    },
                    enabled = newTagName.isNotBlank()
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showAddDialog = false
                        newTagName = ""
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
    
    // 删除确认对话框
    tagToDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("删除标签") },
            text = { Text("确定要删除标签「$tag」吗？\n已使用该标签的剧集会同步移除此标签。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 先从所有剧集中移除该标签
                        onDeleteTag(tag)
                        // 再从标签列表中移除
                        AppSettings.removeCustomTag(tag)
                        tags = AppSettings.customTags
                        tagToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * TagItem - 标签列表项
 */
@Composable
private fun TagItem(
    tag: String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Label,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = tag,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

