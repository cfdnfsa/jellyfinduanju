package com.wjp.jellyfinduanju.data.network

import com.wjp.jellyfinduanju.data.model.AuthenticateByNameRequest
import com.wjp.jellyfinduanju.data.model.AuthenticationResult
import com.wjp.jellyfinduanju.data.model.ItemsResponse
import com.wjp.jellyfinduanju.data.model.PublicSystemInfo
import com.wjp.jellyfinduanju.data.model.UserItemData
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * JellyfinApi.kt - Jellyfin REST API 接口定义
 * 
 * 使用 Retrofit 定义 API 接口
 * 每个方法对应一个 HTTP 请求
 * 
 * Retrofit 注解说明：
 * - @GET("path")：发送 GET 请求到指定路径
 * - @POST("path")：发送 POST 请求
 * - @Body：将参数序列化为 JSON 作为请求体
 * - @Header：添加 HTTP 请求头
 * - @Headers：添加固定的 HTTP 请求头
 * 
 * suspend 关键字表示这是一个挂起函数，需要在协程中调用
 * Response<T> 包装返回值，可以获取 HTTP 状态码等信息
 */
interface JellyfinApi {
    
    /**
     * 获取服务器公开信息
     * 
     * 不需要认证，用于：
     * 1. 验证服务器地址是否正确
     * 2. 检查服务器是否在线
     * 3. 获取服务器名称和版本
     * 
     * API: GET /System/Info/Public
     */
    @GET("System/Info/Public")
    suspend fun getPublicSystemInfo(): Response<PublicSystemInfo>
    
    /**
     * 用户名密码登录
     * 
     * 需要在请求头中添加客户端信息（Authorization header）
     * 成功后返回用户信息和访问令牌
     * 
     * API: POST /Users/AuthenticateByName
     * 
     * @param authorization Jellyfin 客户端认证头
     * @param request 包含用户名和密码的请求体
     * @return 认证结果，包含 AccessToken
     */
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST("Users/AuthenticateByName")
    suspend fun authenticateByName(
        @Header("X-Emby-Authorization") authorization: String,
        @Body request: AuthenticateByNameRequest
    ): Response<AuthenticationResult>
    
    /**
     * 退出登录
     * 
     * 通知服务器当前会话已结束，服务器会使 Token 失效
     * 需要在请求头中携带有效的 Token
     * 
     * API: POST /Sessions/Logout
     * 
     * @param authorization 包含 Token 的认证头
     * @return 空响应，成功返回 204 No Content
     */
    @POST("Sessions/Logout")
    suspend fun logout(
        @Header("X-Emby-Authorization") authorization: String
    ): Response<Unit>
    
    // ==================== 媒体相关 API ====================
    
    /**
     * 获取用户的媒体项列表
     * 
     * 可用于获取剧集、电影、视频等
     * 
     * API: GET /Users/{userId}/Items
     * 
     * @param authorization 认证头
     * @param userId 用户 ID
     * @param includeItemTypes 包含的项目类型，如 "Series", "Movie", "Episode"
     * @param recursive 是否递归搜索子文件夹
     * @param sortBy 排序字段，如 "DateCreated", "SortName"
     * @param sortOrder 排序方向，"Ascending" 或 "Descending"
     * @param fields 需要返回的字段，多个字段用逗号分隔
     * @param limit 返回数量限制
     * @param startIndex 起始索引（用于分页）
     * @return 媒体项列表响应
     */
    @GET("Users/{userId}/Items")
    suspend fun getUserItems(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("userId") userId: String,
        @Query("IncludeItemTypes") includeItemTypes: String? = null,
        @Query("Recursive") recursive: Boolean = true,
        @Query("SortBy") sortBy: String = "SortName",
        @Query("SortOrder") sortOrder: String = "Ascending",
        @Query("Fields") fields: String = "Overview,PrimaryImageAspectRatio,ChildCount",
        @Query("Limit") limit: Int? = null,
        @Query("StartIndex") startIndex: Int? = null,
        @Query("ParentId") parentId: String? = null
    ): Response<ItemsResponse>
    
    /**
     * 获取用户媒体库列表（Views）
     * 
     * 返回用户可访问的所有媒体库（如"电视剧"、"动漫"、"电影"等）
     * 
     * API: GET /Users/{userId}/Views
     * 
     * @param authorization 认证头
     * @param userId 用户 ID
     * @return 媒体库列表
     */
    @GET("Users/{userId}/Views")
    suspend fun getUserViews(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("userId") userId: String
    ): Response<ItemsResponse>
    
    /**
     * 获取媒体项图片（流式下载）
     * 
     * 用于下载图片到本地缓存
     * 
     * API: GET /Items/{itemId}/Images/{imageType}
     * 
     * @param itemId 媒体项 ID
     * @param imageType 图片类型：Primary, Backdrop, Thumb 等
     * @param maxWidth 最大宽度（可选，用于缩放）
     * @param maxHeight 最大高度（可选，用于缩放）
     * @param tag 图片标签（用于缓存验证）
     * @return 图片数据流
     */
    @Streaming
    @GET("Items/{itemId}/Images/{imageType}")
    suspend fun getItemImage(
        @Path("itemId") itemId: String,
        @Path("imageType") imageType: String = "Primary",
        @Query("maxWidth") maxWidth: Int? = null,
        @Query("maxHeight") maxHeight: Int? = null,
        @Query("tag") tag: String? = null
    ): Response<ResponseBody>
    
    // ==================== 剧集详情 API ====================
    
    /**
     * 获取剧集的所有季
     * 
     * API: GET /Shows/{seriesId}/Seasons
     * 
     * @param authorization 认证头
     * @param seriesId 剧集 ID
     * @param userId 用户 ID（用于获取用户相关数据）
     * @param fields 需要返回的字段
     * @return 季列表响应
     */
    @GET("Shows/{seriesId}/Seasons")
    suspend fun getSeasons(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("seriesId") seriesId: String,
        @Query("userId") userId: String,
        @Query("Fields") fields: String = "Overview,PrimaryImageAspectRatio,ChildCount"
    ): Response<ItemsResponse>
    
    /**
     * 获取某季的所有剧集
     * 
     * API: GET /Shows/{seriesId}/Episodes
     * 
     * @param authorization 认证头
     * @param seriesId 剧集 ID
     * @param seasonId 季 ID
     * @param userId 用户 ID（用于获取用户相关数据如播放进度）
     * @param fields 需要返回的字段
     * @return 剧集列表响应
     */
    @GET("Shows/{seriesId}/Episodes")
    suspend fun getEpisodes(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("seriesId") seriesId: String,
        @Query("seasonId") seasonId: String,
        @Query("userId") userId: String,
        @Query("Fields") fields: String = "Overview,PrimaryImageAspectRatio,UserData,MediaSources"
    ): Response<ItemsResponse>
    
    /**
     * 标记媒体项为已播放
     * 
     * API: POST /Users/{userId}/PlayedItems/{itemId}
     * 
     * @param authorization 认证头
     * @param userId 用户 ID
     * @param itemId 媒体项 ID
     * @return 更新后的用户数据
     */
    @POST("Users/{userId}/PlayedItems/{itemId}")
    suspend fun markAsPlayed(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): Response<UserItemData>
    
    /**
     * 取消标记媒体项为已播放
     * 
     * API: DELETE /Users/{userId}/PlayedItems/{itemId}
     * 
     * @param authorization 认证头
     * @param userId 用户 ID
     * @param itemId 媒体项 ID
     * @return 更新后的用户数据
     */
    @retrofit2.http.DELETE("Users/{userId}/PlayedItems/{itemId}")
    suspend fun unmarkAsPlayed(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): Response<UserItemData>
    
    /**
     * 更新用户的媒体项数据（播放进度等）
     * 
     * 这是最可靠的保存播放进度的方式
     * 
     * API: POST /Users/{userId}/Items/{itemId}/UserData
     * 
     * @param authorization 认证头
     * @param userId 用户 ID
     * @param itemId 媒体项 ID
     * @param request 用户数据请求体
     * @return 更新后的用户数据
     */
    @Headers("Content-Type: application/json")
    @POST("Users/{userId}/Items/{itemId}/UserData")
    suspend fun updateUserItemData(
        @Header("X-Emby-Authorization") authorization: String,
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Body request: UpdateUserDataRequest
    ): Response<UserItemData>
    
    // ==================== 播放进度 API ====================
    
    /**
     * 上报播放进度
     * 
     * 用于同步播放进度到服务器
     * 
     * API: POST /Sessions/Playing/Progress
     * 
     * @param authorization 认证头
     * @param request 播放进度请求体
     * @return 空响应
     */
    @Headers("Content-Type: application/json")
    @POST("Sessions/Playing/Progress")
    suspend fun reportPlaybackProgress(
        @Header("X-Emby-Authorization") authorization: String,
        @Body request: PlaybackProgressRequest
    ): Response<Unit>
    
    /**
     * 上报播放开始
     * 
     * API: POST /Sessions/Playing
     * 
     * @param authorization 认证头
     * @param request 播放开始请求体
     * @return 空响应
     */
    @Headers("Content-Type: application/json")
    @POST("Sessions/Playing")
    suspend fun reportPlaybackStart(
        @Header("X-Emby-Authorization") authorization: String,
        @Body request: PlaybackStartRequest
    ): Response<Unit>
    
    /**
     * 上报播放停止
     * 
     * API: POST /Sessions/Playing/Stopped
     * 
     * @param authorization 认证头
     * @param request 播放停止请求体
     * @return 空响应
     */
    @Headers("Content-Type: application/json")
    @POST("Sessions/Playing/Stopped")
    suspend fun reportPlaybackStopped(
        @Header("X-Emby-Authorization") authorization: String,
        @Body request: PlaybackStopRequest
    ): Response<Unit>
}

/**
 * 播放进度请求
 */
@kotlinx.serialization.Serializable
data class PlaybackProgressRequest(
    @kotlinx.serialization.SerialName("ItemId")
    val itemId: String,
    @kotlinx.serialization.SerialName("PositionTicks")
    val positionTicks: Long,
    @kotlinx.serialization.SerialName("IsPaused")
    val isPaused: Boolean = false
)

/**
 * 播放开始请求
 */
@kotlinx.serialization.Serializable
data class PlaybackStartRequest(
    @kotlinx.serialization.SerialName("ItemId")
    val itemId: String,
    @kotlinx.serialization.SerialName("PositionTicks")
    val positionTicks: Long = 0
)

/**
 * 播放停止请求
 */
@kotlinx.serialization.Serializable
data class PlaybackStopRequest(
    @kotlinx.serialization.SerialName("ItemId")
    val itemId: String,
    @kotlinx.serialization.SerialName("PositionTicks")
    val positionTicks: Long
)

/**
 * 更新用户数据请求
 * 
 * 用于直接更新用户对某个媒体项的数据（播放进度、是否已看完等）
 */
@kotlinx.serialization.Serializable
data class UpdateUserDataRequest(
    @kotlinx.serialization.SerialName("PlaybackPositionTicks")
    val playbackPositionTicks: Long,
    @kotlinx.serialization.SerialName("Played")
    val played: Boolean? = null,
    @kotlinx.serialization.SerialName("PlayCount")
    val playCount: Int? = null
)

/**
 * Jellyfin Authorization Header 构建器
 * 
 * Jellyfin 使用 X-Emby-Authorization 头进行客户端认证
 * 格式：MediaBrowser Client="...", Device="...", DeviceId="...", Version="...", Token="..."
 * 
 * 注意：Header 值中不能包含非 ASCII 字符，所以客户端名称使用英文
 */
object JellyfinAuthHeader {
    
    // 客户端名称（使用英文避免编码问题）
    private const val CLIENT_NAME = "JellyfinDuanju"
    
    // 客户端版本
    private const val CLIENT_VERSION = "1.0.0"
    
    /**
     * 构建登录前的 Authorization header
     * 登录时还没有 Token，所以不包含 Token 字段
     * 
     * @param deviceId 设备唯一标识
     * @param deviceName 设备名称
     */
    fun buildLoginHeader(deviceId: String, deviceName: String): String {
        // 设备名称需要过滤非 ASCII 字符
        val safeDeviceName = deviceName.replace(Regex("[^\\x20-\\x7E]"), "")
            .ifEmpty { "Android Device" }
        
        return buildString {
            append("MediaBrowser ")
            append("Client=\"$CLIENT_NAME\", ")
            append("Device=\"$safeDeviceName\", ")
            append("DeviceId=\"$deviceId\", ")
            append("Version=\"$CLIENT_VERSION\"")
        }
    }
    
    /**
     * 构建已登录状态的 Authorization header
     * 包含 Token，用于需要认证的 API 请求
     * 
     * @param deviceId 设备唯一标识
     * @param deviceName 设备名称
     * @param token 访问令牌（登录成功后获得）
     */
    fun buildAuthHeader(deviceId: String, deviceName: String, token: String): String {
        val safeDeviceName = deviceName.replace(Regex("[^\\x20-\\x7E]"), "")
            .ifEmpty { "Android Device" }
        
        return buildString {
            append("MediaBrowser ")
            append("Client=\"$CLIENT_NAME\", ")
            append("Device=\"$safeDeviceName\", ")
            append("DeviceId=\"$deviceId\", ")
            append("Version=\"$CLIENT_VERSION\", ")
            append("Token=\"$token\"")
        }
    }
}
