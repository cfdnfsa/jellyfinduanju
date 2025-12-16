package com.wjp.jellyfinduanju.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wjp.jellyfinduanju.data.model.ServerConfig
import com.wjp.jellyfinduanju.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * LoginViewModel.kt - 登录页面的 ViewModel
 * 
 * ViewModel 的职责：
 * 1. 持有 UI 状态（使用 StateFlow）
 * 2. 处理用户操作（调用 Repository）
 * 3. 在配置变更（如屏幕旋转）时保持数据
 * 
 * 状态管理：
 * - 使用 StateFlow 持有状态
 * - UI 收集 StateFlow，状态变化时自动更新
 * - 单向数据流：用户操作 -> ViewModel 更新状态 -> UI 响应
 * 
 * @HiltViewModel 注解：
 * - 让 Hilt 自动注入 ViewModel 的依赖
 * - 无需手动创建 ViewModelFactory
 * 
 * @Inject 注解：
 * - 标记构造函数，告诉 Hilt 如何创建这个 ViewModel
 * - Hilt 会自动注入 authRepository
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // ==================== UI 状态 ====================
    
    /**
     * MutableStateFlow：可变的状态流
     * - 私有，只有 ViewModel 能修改
     * - 使用 update {} 进行状态更新，保证线程安全
     */
    private val _uiState = MutableStateFlow(LoginUiState())
    
    /**
     * StateFlow：不可变的状态流
     * - 公开给 UI 层收集
     * - UI 只能读取，不能修改
     */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    // ==================== 用户输入处理 ====================
    
    /**
     * 更新服务器地址
     */
    fun updateServerUrl(url: String) {
        _uiState.update { it.copy(serverUrl = url) }
    }
    
    /**
     * 更新用户名
     */
    fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username) }
    }
    
    /**
     * 更新密码
     */
    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }
    
    // ==================== 业务逻辑 ====================
    
    /**
     * 验证服务器
     * 
     * 在用户输入服务器地址后调用
     * 验证成功后显示用户名密码输入框
     */
    fun validateServer() {
        val serverUrl = _uiState.value.serverUrl.trim()
        
        // 基础验证
        if (serverUrl.isEmpty()) {
            _uiState.update { it.copy(error = "请输入服务器地址") }
            return
        }
        
        // 启动协程执行网络请求
        // viewModelScope：ViewModel 的协程作用域
        // 当 ViewModel 被销毁时，协程会自动取消
        viewModelScope.launch {
            // 设置加载状态
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // 调用 Repository 验证服务器
            val result = authRepository.validateServer(serverUrl)
            
            // 处理结果
            result.fold(
                onSuccess = { config ->
                    // 验证成功，更新状态
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            serverConfig = config,
                            isServerValidated = true,
                            error = null
                        )
                    }
                },
                onFailure = { exception ->
                    // 验证失败，显示错误
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message
                        )
                    }
                }
            )
        }
    }
    
    /**
     * 执行登录
     */
    fun login() {
        val state = _uiState.value
        
        // 验证输入
        if (state.username.isEmpty()) {
            _uiState.update { it.copy(error = "请输入用户名") }
            return
        }
        if (state.password.isEmpty()) {
            _uiState.update { it.copy(error = "请输入密码") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = authRepository.login(
                serverUrl = state.serverUrl,
                username = state.username,
                password = state.password
            )
            
            result.fold(
                onSuccess = {
                    // 登录成功
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoginSuccess = true,
                            error = null
                        )
                    }
                },
                onFailure = { exception ->
                    // 登录失败
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message
                        )
                    }
                }
            )
        }
    }
    
    /**
     * 返回服务器输入步骤
     */
    fun backToServerInput() {
        _uiState.update {
            it.copy(
                isServerValidated = false,
                serverConfig = null,
                username = "",
                password = "",
                error = null
            )
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * LoginUiState - 登录页面的 UI 状态
 * 
 * 使用 data class 定义，利用 copy() 方法进行不可变更新
 * 
 * @property serverUrl 服务器地址输入
 * @property username 用户名输入
 * @property password 密码输入
 * @property isLoading 是否正在加载
 * @property isServerValidated 服务器是否已验证
 * @property serverConfig 服务器配置信息
 * @property isLoginSuccess 是否登录成功
 * @property error 错误信息
 */
data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isServerValidated: Boolean = false,
    val serverConfig: ServerConfig? = null,
    val isLoginSuccess: Boolean = false,
    val error: String? = null
)

