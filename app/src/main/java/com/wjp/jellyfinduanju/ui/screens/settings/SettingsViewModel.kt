package com.wjp.jellyfinduanju.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wjp.jellyfinduanju.data.model.ServerConfig
import com.wjp.jellyfinduanju.data.model.UserSession
import com.wjp.jellyfinduanju.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * SettingsViewModel - 设置页面的 ViewModel
 * 
 * 功能：
 * 1. 显示当前登录的用户信息
 * 2. 显示当前连接的服务器信息
 * 3. 退出登录
 */
class SettingsViewModel(
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
            
            _uiState.update {
                it.copy(
                    userSession = session,
                    serverConfig = serverConfig
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
}

/**
 * SettingsUiState - 设置页面的 UI 状态
 */
data class SettingsUiState(
    val userSession: UserSession? = null,       // 当前用户会话
    val serverConfig: ServerConfig? = null,     // 当前服务器配置
    val isLoggingOut: Boolean = false,          // 是否正在退出
    val isLoggedOut: Boolean = false,           // 是否已退出
    val showLogoutDialog: Boolean = false       // 是否显示退出确认对话框
)

