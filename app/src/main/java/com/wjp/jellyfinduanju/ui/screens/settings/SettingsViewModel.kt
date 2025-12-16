package com.wjp.jellyfinduanju.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wjp.jellyfinduanju.data.model.ServerConfig
import com.wjp.jellyfinduanju.data.model.UserSession
import com.wjp.jellyfinduanju.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SettingsViewModel - 设置页面的 ViewModel
 * 
 * 功能：
 * 1. 显示当前登录的用户信息
 * 2. 显示当前连接的服务器信息
 * 3. 退出登录
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // ==================== UI 状态 ====================
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    // ==================== 初始化 ====================
    
    init {
        // 加载用户和服务器信息
        loadUserInfo()
    }
    
    /**
     * 加载用户信息
     */
    private fun loadUserInfo() {
        viewModelScope.launch {
            // 获取用户会话
            val session = authRepository.userSession.first()
            val serverConfig = authRepository.serverConfig.first()
            val secondaryUrl = authRepository.getSecondaryServerUrlSync()
            val activeIndex = authRepository.getActiveServerIndexSync()
            
            _uiState.update {
                it.copy(
                    userSession = session,
                    serverConfig = serverConfig,
                    secondaryServerUrl = secondaryUrl,
                    activeServerIndex = activeIndex
                )
            }
        }
    }
    
    // ==================== 用户操作 ====================
    
    /**
     * 退出登录
     */
    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            
            // 调用 Repository 退出登录
            authRepository.logout()
            
            _uiState.update { 
                it.copy(
                    isLoggingOut = false,
                    isLoggedOut = true
                ) 
            }
        }
    }
    
    /**
     * 显示退出确认对话框
     */
    fun showLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = true) }
    }
    
    /**
     * 隐藏退出确认对话框
     */
    fun hideLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = false) }
    }
    
    // ==================== 备用服务器管理 ====================
    
    /**
     * 显示添加/编辑备用地址对话框
     */
    fun showSecondaryServerDialog() {
        _uiState.update { 
            it.copy(
                showSecondaryServerDialog = true,
                editingSecondaryUrl = it.secondaryServerUrl ?: ""
            ) 
        }
    }
    
    /**
     * 隐藏备用地址对话框
     */
    fun hideSecondaryServerDialog() {
        _uiState.update { 
            it.copy(
                showSecondaryServerDialog = false,
                editingSecondaryUrl = ""
            ) 
        }
    }
    
    /**
     * 更新编辑中的备用地址
     */
    fun updateEditingSecondaryUrl(url: String) {
        _uiState.update { it.copy(editingSecondaryUrl = url) }
    }
    
    /**
     * 保存备用服务器地址
     */
    fun saveSecondaryServerUrl() {
        val url = _uiState.value.editingSecondaryUrl.trim()
        
        // 保存到 Repository
        authRepository.saveSecondaryServerUrl(url.ifEmpty { null })
        
        // 更新 UI 状态
        _uiState.update { 
            it.copy(
                secondaryServerUrl = url.ifEmpty { null },
                showSecondaryServerDialog = false,
                editingSecondaryUrl = "",
                // 如果删除了备用地址，切换回主地址
                activeServerIndex = if (url.isEmpty()) 0 else it.activeServerIndex
            ) 
        }
    }
    
    /**
     * 删除备用服务器地址
     */
    fun deleteSecondaryServerUrl() {
        authRepository.saveSecondaryServerUrl(null)
        
        _uiState.update { 
            it.copy(
                secondaryServerUrl = null,
                activeServerIndex = 0
            ) 
        }
    }
    
    /**
     * 切换服务器地址
     */
    fun toggleActiveServer() {
        authRepository.toggleActiveServer()
        
        val newIndex = authRepository.getActiveServerIndexSync()
        _uiState.update { it.copy(activeServerIndex = newIndex) }
    }
}

/**
 * SettingsUiState - 设置页面的 UI 状态
 */
data class SettingsUiState(
    val userSession: UserSession? = null,       // 当前用户会话
    val serverConfig: ServerConfig? = null,     // 当前服务器配置
    val isLoggingOut: Boolean = false,          // 是否正在退出
    val isLoggedOut: Boolean = false,           // 是否已退出
    val showLogoutDialog: Boolean = false,      // 是否显示退出确认对话框
    
    // 备用服务器相关
    val secondaryServerUrl: String? = null,     // 备用服务器地址
    val activeServerIndex: Int = 0,             // 当前激活的服务器索引（0=主，1=备用）
    val showSecondaryServerDialog: Boolean = false, // 是否显示添加/编辑备用地址对话框
    val editingSecondaryUrl: String = ""        // 编辑中的备用地址
)

