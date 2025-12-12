package com.wjp.jellyfinduanju.ui.screens.settings.cache

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * CacheSettingsScreen - 缓存设置页面（有状态组件）
 * 
 * 这是缓存设置页面的入口 Composable，负责：
 * 1. 创建和管理 [CacheSettingsViewModel]
 * 2. 收集 ViewModel 的状态
 * 3. 将状态和事件处理器传递给无状态的 [CacheSettingsContent]
 * 
 * 功能：
 * 1. 显示和修改图片缓存路径
 * 2. 显示和修改视频缓存路径
 * 3. 显示缓存大小信息
 * 4. 清除所有缓存
 * 
 * 设计模式：
 * - 状态提升（State Hoisting）：将状态提升到 ViewModel
 * - 有状态/无状态分离：此组件有状态，CacheSettingsContent 无状态
 * 
 * @param onBack 返回按钮点击回调，由导航层提供，用于返回上一页
 * @param viewModel 缓存设置页面的 ViewModel，默认通过 viewModel() 创建
 */
@Composable
fun CacheSettingsScreen(
    onBack: () -> Unit,
    viewModel: CacheSettingsViewModel = viewModel()
) {
    // 收集 ViewModel 的状态
    // collectAsState() 会将 StateFlow 转换为 Compose 的 State
    // 当状态变化时，此 Composable 会自动重组
    val uiState by viewModel.uiState.collectAsState()
    
    // 将状态和事件处理器传递给无状态组件
    // 所有的事件都通过 lambda 传递，遵循单向数据流
    CacheSettingsContent(
        uiState = uiState,
        onBack = onBack,
        onImagePathClick = { viewModel.showEditImagePathDialog() },
        onVideoPathClick = { viewModel.showEditVideoPathDialog() },
        onClearCacheClick = { viewModel.showClearCacheDialog() },
        onClearCacheConfirm = { viewModel.confirmClearCache() },
        onClearCacheCancel = { viewModel.hideClearCacheDialog() },
        onClearEpisodeDataClick = { viewModel.showClearDataDialog(ClearDataType.EPISODE_DATA) },
        onClearSeriesDataClick = { viewModel.showClearDataDialog(ClearDataType.SERIES_DATA) },
        onClearDataConfirm = { viewModel.confirmClearData() },
        onClearDataCancel = { viewModel.hideClearDataDialog() },
        onEditPathChange = { viewModel.updateEditingPath(it) },
        onEditPathConfirm = { viewModel.confirmEditPath() },
        onEditPathCancel = { viewModel.hideEditPathDialog() },
        onPathChangeConfirm = { viewModel.confirmPathChange() },
        onPathChangeCancel = { viewModel.hidePathChangeConfirmDialog() },
        onMessageShown = { viewModel.clearMessage() }
    )
}

/**
 * CacheSettingsContent - 缓存设置页面内容（无状态组件）
 * 
 * 这是缓存设置页面的 UI 实现，是一个纯函数：
 * - 输入：UI 状态和事件回调
 * - 输出：UI 界面
 * 
 * 无状态设计的优势：
 * 1. 可预览：可以在 Android Studio 中直接预览
 * 2. 可测试：不依赖 ViewModel，易于单元测试
 * 3. 可复用：可以在不同场景下使用
 * 
 * @param uiState 页面的 UI 状态，包含所有需要显示的数据
 * @param onBack 返回按钮点击回调，点击顶部栏返回箭头时触发
 * @param onImagePathClick 图片缓存路径点击回调，点击图片缓存项时触发，用于打开编辑对话框
 * @param onVideoPathClick 视频缓存路径点击回调，点击视频缓存项时触发，用于打开编辑对话框
 * @param onClearCacheClick 清除缓存点击回调，点击"清除所有缓存"按钮时触发，用于显示确认对话框
 * @param onClearCacheConfirm 清除缓存确认回调，在清除确认对话框中点击"确定"时触发
 * @param onClearCacheCancel 清除缓存取消回调，在清除确认对话框中点击"取消"或外部区域时触发
 * @param onEditPathChange 编辑路径变化回调，用户在路径输入框中输入时触发，参数为新的路径字符串
 * @param onEditPathConfirm 编辑路径确认回调，在编辑路径对话框中点击"确定"时触发
 * @param onEditPathCancel 编辑路径取消回调，在编辑路径对话框中点击"取消"或外部区域时触发
 * @param onPathChangeConfirm 路径变更最终确认回调，在路径变更确认对话框中点击"确定"时触发，会清除旧缓存
 * @param onPathChangeCancel 路径变更取消回调，在路径变更确认对话框中点击"取消"时触发
 * @param onMessageShown 消息已显示回调，Snackbar 显示消息后触发，用于清除消息状态防止重复显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CacheSettingsContent(
    uiState: CacheSettingsUiState,
    onBack: () -> Unit,
    onImagePathClick: () -> Unit,
    onVideoPathClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onClearCacheConfirm: () -> Unit,
    onClearCacheCancel: () -> Unit,
    onClearEpisodeDataClick: () -> Unit,
    onClearSeriesDataClick: () -> Unit,
    onClearDataConfirm: () -> Unit,
    onClearDataCancel: () -> Unit,
    onEditPathChange: (String) -> Unit,
    onEditPathConfirm: () -> Unit,
    onEditPathCancel: () -> Unit,
    onPathChangeConfirm: () -> Unit,
    onPathChangeCancel: () -> Unit,
    onMessageShown: () -> Unit
) {
    // Snackbar 状态管理器
    // 用于显示操作结果提示（如"缓存已清除"）
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 监听消息状态变化，显示 Snackbar
    // LaunchedEffect 的 key 是 uiState.message
    // 当 message 变化时，会取消之前的协程并启动新的协程
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            // 显示 Snackbar
            snackbarHostState.showSnackbar(message)
            // 通知 ViewModel 消息已显示，清除消息状态
            onMessageShown()
        }
    }
    
    // Scaffold 提供 Material Design 的基本布局结构
    // 包括顶部栏、底部栏、Snackbar 等
    Scaffold(
        // 顶部应用栏
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = { Text("缓存设置") },
                    // 左侧返回按钮
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        // Snackbar 宿主，用于显示消息
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)  // 禁用Scaffold的自动系统栏padding，实现边到边效果
    ) { paddingValues ->
        // 主内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)  // 应用 Scaffold 提供的内边距
                .verticalScroll(rememberScrollState())  // 支持垂直滚动
                .padding(16.dp)  // 内容边距
        ) {
            // 加载指示器
            // 当 isLoading 为 true 时显示
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // ==================== 缓存概览卡片 ====================
            
            // 分组标题
            Text(
                text = "缓存概览",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            
            // 总缓存大小卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 文件夹图标
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    // 缓存大小文本
                    Column {
                        Text(
                            text = "总缓存大小",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.totalCacheSize,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // ==================== 缓存路径设置卡片 ====================
            
            Text(
                text = "缓存路径",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column {
                    // 图片缓存路径项
                    // 点击后打开编辑对话框
                    CachePathItem(
                        icon = Icons.Default.Image,
                        title = "图片缓存",
                        path = uiState.imageCachePath,
                        size = uiState.imageCacheSize,
                        fileCount = uiState.imageFileCount,
                        onClick = onImagePathClick
                    )
                    
                    // 分隔线
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    
                    // 视频缓存路径项
                    // 点击后打开编辑对话框
                    CachePathItem(
                        icon = Icons.Default.VideoLibrary,
                        title = "视频缓存",
                        path = uiState.videoCachePath,
                        size = uiState.videoCacheSize,
                        fileCount = uiState.videoFileCount,
                        onClick = onVideoPathClick
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // ==================== 文件缓存操作卡片 ====================
            
            Text(
                text = "文件缓存",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                // 清除所有缓存按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = !uiState.isClearing,
                            onClick = onClearCacheClick
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isClearing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (uiState.isClearing) "正在清除..." else "清除所有缓存",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.isClearing) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Text(
                            text = "删除所有缓存（图片、视频、下载任务、剧集数据）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // ==================== 数据缓存操作卡片 ====================
            
            Text(
                text = "数据缓存",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            
            // 提示信息
            if (uiState.cachedSeriesCount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "有 ${uiState.cachedSeriesCount} 个剧集有视频缓存，其数据不会被清除",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column {
                    // 清除剧集详情缓存
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = !uiState.isClearing,
                                onClick = onClearEpisodeDataClick
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "清除剧集详情缓存",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "清除季和集的本地数据",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    
                    // 清除剧集列表缓存
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = !uiState.isClearing,
                                onClick = onClearSeriesDataClick
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "清除剧集列表缓存",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "清除列表数据，需要重新同步",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    // ==================== 对话框区域 ====================
    // 对话框放在 Scaffold 外部，因为它们是覆盖在整个屏幕上的
    
    // 清除缓存确认对话框
    // 当 showClearCacheDialog 为 true 时显示
    if (uiState.showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = onClearCacheCancel,  // 点击外部区域时触发取消
            title = { Text("清除所有缓存") },
            text = { 
                Text("确定要清除所有缓存吗？\n\n这将删除：\n• 图片和视频缓存文件（${uiState.totalCacheSize}）\n• 下载任务\n• 剧集列表和详情数据\n\n清除后需要重新同步数据。") 
            },
            confirmButton = {
                TextButton(onClick = onClearCacheConfirm) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onClearCacheCancel) {
                    Text("取消")
                }
            }
        )
    }
    
    // 编辑路径对话框
    // 当 showEditPathDialog 为 true 时显示
    if (uiState.showEditPathDialog) {
        AlertDialog(
            onDismissRequest = onEditPathCancel,
            title = { 
                // 根据编辑类型显示不同的标题
                Text(
                    when (uiState.editingPathType) {
                        PathType.IMAGE -> "修改图片缓存路径"
                        PathType.VIDEO -> "修改视频缓存路径"
                        null -> "修改缓存路径"
                    }
                ) 
            },
            text = {
                Column {
                    Text(
                        text = "请输入新的缓存路径：",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // 路径输入框
                    // 预填充当前路径，用户可以修改
                    OutlinedTextField(
                        value = uiState.editingPath,
                        onValueChange = onEditPathChange,  // 输入变化时通知 ViewModel
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onEditPathConfirm) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = onEditPathCancel) {
                    Text("取消")
                }
            }
        )
    }
    
    // 路径变更确认对话框
    // 这是第二步确认，提醒用户会清除旧缓存
    // 当 showPathChangeConfirmDialog 为 true 时显示
    if (uiState.showPathChangeConfirmDialog) {
        AlertDialog(
            onDismissRequest = onPathChangeCancel,
            title = { Text("确认更改路径") },
            text = { 
                Text("更改缓存路径将会自动清除当前目录下的缓存文件。\n\n确定要继续吗？") 
            },
            confirmButton = {
                TextButton(onClick = onPathChangeConfirm) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onPathChangeCancel) {
                    Text("取消")
                }
            }
        )
    }
    
    // 清除数据确认对话框
    if (uiState.showClearDataDialog) {
        val (title, message) = when (uiState.clearDataType) {
            ClearDataType.EPISODE_DATA -> "清除剧集详情缓存" to 
                if (uiState.cachedSeriesCount > 0)
                    "确定要清除剧集详情缓存吗？\n\n有 ${uiState.cachedSeriesCount} 个剧集有视频缓存，其详情数据将被保留。"
                else
                    "确定要清除所有剧集详情缓存吗？"
            ClearDataType.SERIES_DATA -> "清除剧集列表缓存" to 
                if (uiState.cachedSeriesCount > 0)
                    "确定要清除剧集列表缓存吗？\n\n有 ${uiState.cachedSeriesCount} 个剧集有视频缓存，其数据将被保留。\n清除后需要重新同步列表。"
                else
                    "确定要清除所有剧集列表缓存吗？\n\n清除后需要重新同步列表。"
            else -> "" to ""
        }
        
        AlertDialog(
            onDismissRequest = onClearDataCancel,
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onClearDataConfirm) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onClearDataCancel) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * CachePathItem - 缓存路径项组件
 * 
 * 显示单个缓存路径的信息，包括图标、标题、路径和缓存大小
 * 点击后可以编辑路径
 * 
 * @param icon 左侧显示的图标（如图片图标、视频图标）
 * @param title 项目标题（如"图片缓存"、"视频缓存"）
 * @param path 缓存路径
 * @param size 缓存大小（格式化后的字符串，如 "128.5 MB"）
 * @param fileCount 缓存文件数量
 * @param onClick 点击回调，用于打开编辑对话框
 */
@Composable
private fun CachePathItem(
    icon: ImageVector,
    title: String,
    path: String,
    size: String,
    fileCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)  // 整行可点击
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧图标
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        
        // 右侧内容
        Column(modifier = Modifier.weight(1f)) {
            // 第一行：标题 + 缓存大小
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 缓存大小和文件数
                Text(
                    text = "$size ($fileCount 个文件)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // 第二行：缓存路径
            Text(
                text = path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis  // 路径过长时显示省略号
            )
        }
    }
}
