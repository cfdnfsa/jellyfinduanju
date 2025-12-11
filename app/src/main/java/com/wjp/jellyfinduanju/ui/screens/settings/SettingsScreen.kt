package com.wjp.jellyfinduanju.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wjp.jellyfinduanju.data.model.ServerConfig
import com.wjp.jellyfinduanju.data.model.UserSession
import com.wjp.jellyfinduanju.data.settings.AppSettings
import com.wjp.jellyfinduanju.ui.theme.JellyfinDuanjuTheme

/**
 * SettingsScreen - 设置页面
 * 
 * 功能：
 * 1. 显示当前登录的用户信息
 * 2. 显示当前连接的服务器信息
 * 3. 缓存设置入口
 * 4. 退出登录按钮
 * 
 * @param viewModel 设置页面的 ViewModel
 * @param onNavigateToCacheSettings 导航到缓存设置页面的回调
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToCacheSettings: () -> Unit = {}
) {
    // 收集 UI 状态
    val uiState by viewModel.uiState.collectAsState()
    
    // 渲染内容
    SettingsContent(
        uiState = uiState,
        onCacheSettingsClick = onNavigateToCacheSettings,
        onLogoutClick = { viewModel.showLogoutDialog() },
        onLogoutConfirm = {
            viewModel.hideLogoutDialog()
            viewModel.logout()
        },
        onLogoutCancel = { viewModel.hideLogoutDialog() }
    )
}

/**
 * SettingsContent - 设置页面内容（无状态组件）
 * 
 * 分离出来便于预览和测试
 */
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onCacheSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onLogoutConfirm: () -> Unit,
    onLogoutCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ==================== 账号信息卡片 ====================
        
        Text(
            text = "账号",
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
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 用户信息
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = uiState.userSession?.username ?: "未登录",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "用户 ID: ${uiState.userSession?.userId ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                // 服务器信息
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = uiState.serverConfig?.serverName ?: "未连接",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.serverConfig?.serverUrl ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // ==================== 应用设置卡片 ====================
        
        Text(
            text = "应用设置",
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
            // 缓存设置入口
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCacheSettingsClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "缓存设置",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "图片和视频缓存路径、清除缓存",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // ==================== 播放设置卡片 ====================
        
        Text(
            text = "播放设置",
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
                // 长按倍速设置
                SpeedSettingItem(
                    icon = Icons.Default.Speed,
                    title = "长按倍速",
                    subtitle = "长按屏幕时的播放速度",
                    currentSpeed = AppSettings.longPressSpeed,
                    speedOptions = listOf(2.0f, 3.0f, 4.0f, 5.0f),
                    onSpeedSelected = { AppSettings.longPressSpeed = it }
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                // 默认播放倍速设置
                SpeedSettingItem(
                    icon = Icons.Default.PlayArrow,
                    title = "默认播放倍速",
                    subtitle = "进入播放器时的默认速度",
                    currentSpeed = AppSettings.defaultPlaybackSpeed,
                    speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f),
                    onSpeedSelected = { AppSettings.defaultPlaybackSpeed = it }
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                // 自动播放下一集
                SwitchSettingItem(
                    icon = Icons.Default.SkipNext,
                    title = "自动播放下一集",
                    subtitle = "播放完成后自动播放下一集",
                    checked = AppSettings.autoPlayNext,
                    onCheckedChange = { AppSettings.autoPlayNext = it }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // ==================== 操作卡片 ====================
        
        Text(
            text = "操作",
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
            // 退出登录按钮
            TextButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                enabled = !uiState.isLoggingOut
            ) {
                if (uiState.isLoggingOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("正在退出...")
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "退出登录",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // ==================== 关于卡片 ====================
        
        Text(
            text = "关于",
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Jellyfin 短剧",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "版本 1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    // ==================== 退出确认对话框 ====================
    
    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = onLogoutCancel,
            title = { Text("退出登录") },
            text = { Text("确定要退出当前账号吗？") },
            confirmButton = {
                TextButton(onClick = onLogoutConfirm) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onLogoutCancel) {
                    Text("取消")
                }
            }
        )
    }
}

// ==================== 预览 ====================

@Preview(
    name = "设置页面",
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun PreviewSettingsScreen() {
    JellyfinDuanjuTheme(darkTheme = true) {
        SettingsContent(
            uiState = SettingsUiState(
                userSession = UserSession(
                    userId = "abc123",
                    username = "admin",
                    accessToken = "token",
                    serverId = "server1"
                ),
                serverConfig = ServerConfig(
                    serverUrl = "http://192.168.1.100:8096",
                    serverName = "我的 Jellyfin 服务器",
                    serverId = "server1"
                )
            ),
            onCacheSettingsClick = {},
            onLogoutClick = {},
            onLogoutConfirm = {},
            onLogoutCancel = {}
        )
    }
}

@Preview(
    name = "退出确认对话框",
    showBackground = true
)
@Composable
private fun PreviewLogoutDialog() {
    JellyfinDuanjuTheme {
        SettingsContent(
            uiState = SettingsUiState(
                showLogoutDialog = true
            ),
            onCacheSettingsClick = {},
            onLogoutClick = {},
            onLogoutConfirm = {},
            onLogoutCancel = {}
        )
    }
}

// ==================== 倍速设置项组件 ====================

/**
 * SpeedSettingItem - 倍速设置项
 * 
 * 带下拉菜单的设置项，用于选择倍速
 * 
 * @param icon 设置项图标
 * @param title 设置项标题
 * @param subtitle 设置项副标题
 * @param currentSpeed 当前选中的倍速
 * @param speedOptions 可选的倍速列表
 * @param onSpeedSelected 选择倍速后的回调
 */
@Composable
private fun SpeedSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    currentSpeed: Float,
    speedOptions: List<Float>,
    onSpeedSelected: (Float) -> Unit
) {
    // 下拉菜单展开状态
    var expanded by remember { mutableStateOf(false) }
    // 当前选中的速度（用于 UI 更新）
    var selectedSpeed by remember { mutableStateOf(currentSpeed) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 当前值显示 + 下拉菜单
        androidx.compose.foundation.layout.Box {
            Text(
                text = formatSpeed(selectedSpeed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                speedOptions.forEach { speed ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = formatSpeed(speed),
                                color = if (speed == selectedSpeed) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (speed == selectedSpeed) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            selectedSpeed = speed
                            onSpeedSelected(speed)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * 格式化倍速显示
 * 1.0 显示为 "正常"，其他显示为 "Xx"
 */
private fun formatSpeed(speed: Float): String {
    return if (speed == 1.0f) "正常" else "${speed}x"
}

/**
 * SwitchSettingItem - 开关设置项
 * 
 * @param icon 设置项图标
 * @param title 设置项标题
 * @param subtitle 设置项副标题
 * @param checked 是否选中
 * @param onCheckedChange 切换回调
 */
@Composable
private fun SwitchSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var isChecked by remember { mutableStateOf(checked) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                isChecked = !isChecked
                onCheckedChange(isChecked)
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = { 
                isChecked = it
                onCheckedChange(it)
            }
        )
    }
}

