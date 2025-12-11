package com.wjp.jellyfinduanju.data.local

import android.content.Context
import com.tencent.mmkv.MMKV
import com.wjp.jellyfinduanju.data.model.ServerConfig
import com.wjp.jellyfinduanju.data.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * AppDataStore.kt - 应用数据持久化存储
 * 
 * 使用腾讯 MMKV 进行数据存储
 * 
 * MMKV 优势：
 * 1. 高性能：使用 mmap 内存映射，读写速度极快
 * 2. 进程安全：支持多进程访问
 * 3. 简单易用：API 类似 SharedPreferences
 * 4. 数据可靠：写入即持久化，不会丢失数据
 * 
 * 使用方式：
 * - 写入：mmkv.encode(key, value)
 * - 读取：mmkv.decodeString(key, defaultValue)
 */
class AppDataStore(context: Context) {
    
    // ==================== 初始化 ====================
    
    init {
        // 初始化 MMKV
        // 只需要初始化一次，放在 Application 创建时调用
        MMKV.initialize(context)
    }
    
    // 获取默认的 MMKV 实例
    private val mmkv: MMKV = MMKV.defaultMMKV()
    
    // ==================== 存储键定义 ====================
    
    private companion object {
        // 服务器配置
        const val KEY_SERVER_URL = "server_url"
        const val KEY_SERVER_NAME = "server_name"
        const val KEY_SERVER_ID = "server_id"
        
        // 备用服务器地址
        const val KEY_SECONDARY_SERVER_URL = "secondary_server_url"
        // 当前激活的服务器索引（0=主地址，1=备用地址）
        const val KEY_ACTIVE_SERVER_INDEX = "active_server_index"
        
        // 用户会话
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_ACCESS_TOKEN = "access_token"
        
        // 设备信息
        const val KEY_DEVICE_ID = "device_id"
    }
    
    // ==================== 状态流（用于响应式更新） ====================
    
    /**
     * 使用 StateFlow 提供响应式数据
     * 当数据变化时，收集者会自动收到更新
     */
    private val _serverConfigFlow = MutableStateFlow(loadServerConfig())
    private val _userSessionFlow = MutableStateFlow(loadUserSession())
    private val _deviceIdFlow = MutableStateFlow(loadDeviceId())
    private val _secondaryServerUrlFlow = MutableStateFlow(loadSecondaryServerUrl())
    private val _activeServerIndexFlow = MutableStateFlow(loadActiveServerIndex())
    
    // ==================== 服务器配置 ====================
    
    /**
     * 获取服务器配置 Flow
     */
    val serverConfig: Flow<ServerConfig?> = _serverConfigFlow
    
    /**
     * 保存服务器配置
     */
    fun saveServerConfig(config: ServerConfig) {
        mmkv.encode(KEY_SERVER_URL, config.serverUrl)
        mmkv.encode(KEY_SERVER_NAME, config.serverName)
        mmkv.encode(KEY_SERVER_ID, config.serverId)
        // 更新 Flow
        _serverConfigFlow.value = config
    }
    
    /**
     * 从 MMKV 加载服务器配置
     */
    private fun loadServerConfig(): ServerConfig? {
        val url = mmkv.decodeString(KEY_SERVER_URL)
        val name = mmkv.decodeString(KEY_SERVER_NAME)
        val id = mmkv.decodeString(KEY_SERVER_ID)
        
        return if (url != null && name != null && id != null) {
            ServerConfig(serverUrl = url, serverName = name, serverId = id)
        } else {
            null
        }
    }
    
    // ==================== 备用服务器地址 ====================
    
    /**
     * 获取备用服务器地址 Flow
     */
    val secondaryServerUrl: Flow<String?> = _secondaryServerUrlFlow
    
    /**
     * 获取当前激活的服务器索引 Flow
     * 0 = 主地址，1 = 备用地址
     */
    val activeServerIndex: Flow<Int> = _activeServerIndexFlow
    
    /**
     * 保存备用服务器地址
     */
    fun saveSecondaryServerUrl(url: String?) {
        if (url.isNullOrBlank()) {
            mmkv.removeValueForKey(KEY_SECONDARY_SERVER_URL)
            _secondaryServerUrlFlow.value = null
            // 如果删除备用地址，自动切换回主地址
            if (_activeServerIndexFlow.value == 1) {
                setActiveServerIndex(0)
            }
        } else {
            mmkv.encode(KEY_SECONDARY_SERVER_URL, url)
            _secondaryServerUrlFlow.value = url
        }
    }
    
    /**
     * 从 MMKV 加载备用服务器地址
     */
    private fun loadSecondaryServerUrl(): String? {
        return mmkv.decodeString(KEY_SECONDARY_SERVER_URL)
    }
    
    /**
     * 设置当前激活的服务器索引
     * @param index 0 = 主地址，1 = 备用地址
     */
    fun setActiveServerIndex(index: Int) {
        val validIndex = if (index == 1 && _secondaryServerUrlFlow.value != null) 1 else 0
        mmkv.encode(KEY_ACTIVE_SERVER_INDEX, validIndex)
        _activeServerIndexFlow.value = validIndex
    }
    
    /**
     * 从 MMKV 加载激活的服务器索引
     */
    private fun loadActiveServerIndex(): Int {
        return mmkv.decodeInt(KEY_ACTIVE_SERVER_INDEX, 0)
    }
    
    /**
     * 切换服务器地址
     * 在主地址和备用地址之间切换
     */
    fun toggleActiveServer() {
        val currentIndex = _activeServerIndexFlow.value
        val newIndex = if (currentIndex == 0 && _secondaryServerUrlFlow.value != null) 1 else 0
        setActiveServerIndex(newIndex)
    }
    
    /**
     * 获取当前激活的服务器地址
     * @return 当前使用的服务器 URL，如果没有配置则返回 null
     */
    fun getActiveServerUrl(): String? {
        val config = _serverConfigFlow.value ?: return null
        return if (_activeServerIndexFlow.value == 1 && _secondaryServerUrlFlow.value != null) {
            _secondaryServerUrlFlow.value
        } else {
            config.serverUrl
        }
    }
    
    /**
     * 获取当前激活的服务器索引（同步方法）
     */
    fun getActiveServerIndexSync(): Int {
        return _activeServerIndexFlow.value
    }
    
    /**
     * 获取备用服务器地址（同步方法）
     */
    fun getSecondaryServerUrlSync(): String? {
        return _secondaryServerUrlFlow.value
    }
    
    // ==================== 用户会话 ====================
    
    /**
     * 获取用户会话 Flow
     */
    val userSession: Flow<UserSession?> = _userSessionFlow
    
    /**
     * 保存用户会话（登录成功后调用）
     */
    fun saveUserSession(session: UserSession) {
        mmkv.encode(KEY_USER_ID, session.userId)
        mmkv.encode(KEY_USERNAME, session.username)
        mmkv.encode(KEY_ACCESS_TOKEN, session.accessToken)
        mmkv.encode(KEY_SERVER_ID, session.serverId)
        // 更新 Flow
        _userSessionFlow.value = session
    }
    
    /**
     * 清除用户会话（退出登录时调用）
     */
    fun clearUserSession() {
        mmkv.removeValueForKey(KEY_USER_ID)
        mmkv.removeValueForKey(KEY_USERNAME)
        mmkv.removeValueForKey(KEY_ACCESS_TOKEN)
        // 注意：不清除服务器配置，用户可能只是想切换账号
        // 更新 Flow
        _userSessionFlow.value = null
    }
    
    /**
     * 从 MMKV 加载用户会话
     */
    private fun loadUserSession(): UserSession? {
        val userId = mmkv.decodeString(KEY_USER_ID)
        val username = mmkv.decodeString(KEY_USERNAME)
        val token = mmkv.decodeString(KEY_ACCESS_TOKEN)
        val serverId = mmkv.decodeString(KEY_SERVER_ID)
        
        return if (userId != null && username != null && token != null && serverId != null) {
            UserSession(
                userId = userId,
                username = username,
                accessToken = token,
                serverId = serverId
            )
        } else {
            null
        }
    }
    
    // ==================== 设备信息 ====================
    
    /**
     * 获取设备 ID Flow
     */
    val deviceId: Flow<String> = _deviceIdFlow
    
    /**
     * 保存设备 ID
     */
    fun saveDeviceId(deviceId: String) {
        mmkv.encode(KEY_DEVICE_ID, deviceId)
        _deviceIdFlow.value = deviceId
    }
    
    /**
     * 从 MMKV 加载设备 ID
     */
    private fun loadDeviceId(): String {
        return mmkv.decodeString(KEY_DEVICE_ID) ?: ""
    }
    
    // ==================== 清除所有数据 ====================
    
    /**
     * 清除所有数据（重置应用时使用）
     */
    fun clearAll() {
        mmkv.clearAll()
        // 更新所有 Flow
        _serverConfigFlow.value = null
        _userSessionFlow.value = null
        _deviceIdFlow.value = ""
        _secondaryServerUrlFlow.value = null
        _activeServerIndexFlow.value = 0
    }
}
