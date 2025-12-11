package com.wjp.jellyfinduanju.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * AuthModels.kt - 认证相关的数据模型
 * 
 * 使用 Kotlin Serialization 进行 JSON 序列化/反序列化
 * @Serializable 注解让类可以被自动转换为 JSON
 * @SerialName 用于指定 JSON 字段名（当与属性名不同时）
 */

// ==================== 请求模型 ====================

/**
 * 登录请求体
 * 
 * 对应 Jellyfin API: POST /Users/AuthenticateByName
 * 
 * @property username 用户名
 * @property pw 密码（Jellyfin 使用 "Pw" 作为字段名）
 */
@Serializable
data class AuthenticateByNameRequest(
    @SerialName("Username")
    val username: String,
    @SerialName("Pw")
    val pw: String
)

// ==================== 响应模型 ====================

/**
 * 登录响应
 * 
 * 包含用户信息和访问令牌
 */
@Serializable
data class AuthenticationResult(
    @SerialName("User")
    val user: UserDto,
    @SerialName("AccessToken")
    val accessToken: String,
    @SerialName("ServerId")
    val serverId: String
)

/**
 * 用户信息
 */
@Serializable
data class UserDto(
    @SerialName("Id")
    val id: String,
    @SerialName("Name")
    val name: String,
    @SerialName("ServerId")
    val serverId: String? = null,
    @SerialName("HasPassword")
    val hasPassword: Boolean = true,
    @SerialName("HasConfiguredPassword")
    val hasConfiguredPassword: Boolean = true
)

// ==================== 服务器信息 ====================

/**
 * 服务器公开信息
 * 
 * 对应 Jellyfin API: GET /System/Info/Public
 * 用于验证服务器是否可用
 */
@Serializable
data class PublicSystemInfo(
    @SerialName("ServerName")
    val serverName: String? = null,
    @SerialName("Version")
    val version: String? = null,
    @SerialName("Id")
    val id: String? = null,
    @SerialName("LocalAddress")
    val localAddress: String? = null
)

// ==================== 本地存储模型 ====================

/**
 * 服务器配置
 * 
 * 存储在 DataStore 中的服务器连接信息
 */
data class ServerConfig(
    val serverUrl: String,      // 服务器地址，如 "http://192.168.1.100:8096"
    val serverName: String,     // 服务器名称
    val serverId: String        // 服务器 ID
)

/**
 * 用户会话
 * 
 * 存储在 DataStore 中的登录状态
 */
data class UserSession(
    val userId: String,         // 用户 ID
    val username: String,       // 用户名
    val accessToken: String,    // 访问令牌
    val serverId: String        // 服务器 ID
)

