package com.wjp.jellyfinduanju.data.repository

import android.os.Build
import com.wjp.jellyfinduanju.data.local.AppDataStore
import com.wjp.jellyfinduanju.data.model.AuthenticateByNameRequest
import com.wjp.jellyfinduanju.data.model.ServerConfig
import com.wjp.jellyfinduanju.data.model.UserSession
import com.wjp.jellyfinduanju.data.network.JellyfinApi
import com.wjp.jellyfinduanju.data.network.JellyfinAuthHeader
import com.wjp.jellyfinduanju.data.network.NetworkModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * AuthRepository.kt - 认证相关的数据仓库
 * 
 * Repository 模式的作用：
 * 1. 封装数据来源（网络、本地存储）
 * 2. 提供统一的数据访问接口
 * 3. ViewModel 不需要关心数据是从哪里来的
 * 
 * 架构图：
 * ┌────────────┐     ┌──────────────┐     ┌─────────────┐
 * │ ViewModel  │ ──> │  Repository  │ ──> │ API / Store │
 * └────────────┘     └──────────────┘     └─────────────┘
 */
class AuthRepository(
    private val dataStore: AppDataStore
) {
    
    // ==================== 状态获取 ====================
    
    /**
     * 获取用户会话状态
     * 
     * 返回 Flow，UI 可以收集这个 Flow 来响应登录状态变化
     */
    val userSession: Flow<UserSession?> = dataStore.userSession
    
    /**
     * 获取服务器配置
     */
    val serverConfig: Flow<ServerConfig?> = dataStore.serverConfig
    
    /**
     * 获取当前激活的服务器地址
     */
    fun getActiveServerUrl(): String? = dataStore.getActiveServerUrl()
    
    /**
     * 创建使用当前激活服务器地址的 API 客户端
     * 
     * @param fallbackUrl 如果没有激活地址时使用的备用地址
     */
    fun createActiveApi(fallbackUrl: String): JellyfinApi {
        val activeUrl = getActiveServerUrl() ?: fallbackUrl
        return NetworkModule.createJellyfinApi(activeUrl)
    }
    
    // ==================== 服务器验证 ====================
    
    /**
     * 验证服务器地址是否有效
     * 
     * @param serverUrl 服务器地址
     * @return Result<ServerConfig> 成功返回服务器配置，失败返回异常
     * 
     * Result 是 Kotlin 标准库的结果类型：
     * - Result.success(value)：成功
     * - Result.failure(exception)：失败
     */
    suspend fun validateServer(serverUrl: String): Result<ServerConfig> {
        return try {
            // 创建临时 API 客户端
            val api = NetworkModule.createJellyfinApi(serverUrl)
            
            // 调用公开信息接口
            val response = api.getPublicSystemInfo()
            
            if (response.isSuccessful) {
                val info = response.body()
                if (info != null) {
                    val config = ServerConfig(
                        serverUrl = serverUrl,
                        serverName = info.serverName ?: "Jellyfin Server",
                        serverId = info.id ?: UUID.randomUUID().toString()
                    )
                    // 保存服务器配置（MMKV 是同步的，不需要 suspend）
                    dataStore.saveServerConfig(config)
                    Result.success(config)
                } else {
                    Result.failure(Exception("服务器响应为空"))
                }
            } else {
                Result.failure(Exception("服务器验证失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("无法连接服务器: ${e.message}"))
        }
    }
    
    // ==================== 登录 ====================
    
    /**
     * 用户名密码登录
     * 
     * @param serverUrl 服务器地址
     * @param username 用户名
     * @param password 密码
     * @return Result<UserSession> 成功返回用户会话，失败返回异常
     */
    suspend fun login(
        serverUrl: String,
        username: String,
        password: String
    ): Result<UserSession> {
        return try {
            // 确保有设备 ID
            val deviceId = getOrCreateDeviceId()
            val deviceName = getDeviceName()
            
            // 创建 API 客户端
            val api = NetworkModule.createJellyfinApi(serverUrl)
            
            // 构建 Authorization header
            val authHeader = JellyfinAuthHeader.buildLoginHeader(deviceId, deviceName)
            
            // 发送登录请求
            val response = api.authenticateByName(
                authorization = authHeader,
                request = AuthenticateByNameRequest(
                    username = username,
                    pw = password
                )
            )
            
            if (response.isSuccessful) {
                val result = response.body()
                if (result != null) {
                    // 构建用户会话
                    val session = UserSession(
                        userId = result.user.id,
                        username = result.user.name,
                        accessToken = result.accessToken,
                        serverId = result.serverId
                    )
                    
                    // 保存用户会话（MMKV 是同步的，不需要 suspend）
                    dataStore.saveUserSession(session)
                    
                    Result.success(session)
                } else {
                    Result.failure(Exception("登录响应为空"))
                }
            } else {
                // 根据状态码返回不同的错误信息
                val errorMessage = when (response.code()) {
                    401 -> "用户名或密码错误"
                    403 -> "账号被禁用"
                    404 -> "用户不存在"
                    else -> "登录失败: ${response.code()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("登录请求失败: ${e.message}"))
        }
    }
    
    // ==================== 登出 ====================
    
    /**
     * 退出登录
     * 
     * 执行流程：
     * 1. 调用服务器 API 使 Token 失效
     * 2. 清除本地保存的用户会话
     * 
     * 即使服务器调用失败，也会清除本地会话（确保用户能退出）
     * 
     * @return Result<Unit> 成功或失败
     */
    suspend fun logout(): Result<Unit> {
        return try {
            // 获取当前会话信息
            val session = dataStore.userSession.first()
            val config = dataStore.serverConfig.first()
            
            if (session != null && config != null) {
                // 尝试调用服务器登出 API
                val deviceId = dataStore.deviceId.first()
                val deviceName = getDeviceName()
                
                // 使用当前激活的服务器地址
                val activeUrl = getActiveServerUrl() ?: config.serverUrl
                val api = NetworkModule.createJellyfinApi(activeUrl)
                val authHeader = JellyfinAuthHeader.buildAuthHeader(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    token = session.accessToken
                )
                
                // 调用登出接口（不管成功失败都清除本地数据）
                try {
                    api.logout(authHeader)
                } catch (e: Exception) {
                    // 忽略网络错误，继续清除本地数据
                }
            }
            
            // 清除本地用户会话
            dataStore.clearUserSession()
            
            Result.success(Unit)
        } catch (e: Exception) {
            // 即使出错也清除本地数据
            dataStore.clearUserSession()
            Result.success(Unit)
        }
    }
    
    // ==================== 备用服务器管理 ====================
    
    /**
     * 保存备用服务器地址
     */
    fun saveSecondaryServerUrl(url: String?) {
        dataStore.saveSecondaryServerUrl(url)
    }
    
    /**
     * 切换服务器地址
     */
    fun toggleActiveServer() {
        dataStore.toggleActiveServer()
    }
    
    /**
     * 获取备用服务器地址（同步）
     */
    fun getSecondaryServerUrlSync(): String? = dataStore.getSecondaryServerUrlSync()
    
    /**
     * 获取当前激活的服务器索引（同步）
     */
    fun getActiveServerIndexSync(): Int = dataStore.getActiveServerIndexSync()
    
    // ==================== 辅助方法 ====================
    
    /**
     * 获取或创建设备 ID
     * 
     * 设备 ID 用于 Jellyfin 识别设备
     * 首次使用时生成，之后复用
     */
    private suspend fun getOrCreateDeviceId(): String {
        val existingId = dataStore.deviceId.first()
        return existingId.ifEmpty {
            // 生成新的 UUID 作为设备 ID
            val newId = UUID.randomUUID().toString()
            dataStore.saveDeviceId(newId)
            newId
        }
    }
    
    /**
     * 获取设备 ID（公开方法）
     * 
     * 供其他 Repository 使用
     */
    suspend fun getDeviceId(): String {
        return getOrCreateDeviceId()
    }
    
    /**
     * 获取设备名称
     * 
     * 使用 Android 设备型号作为设备名
     */
    private fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }
}
