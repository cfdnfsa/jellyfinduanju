package com.wjp.jellyfinduanju.data.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * NetworkModule.kt - 网络模块配置
 * 
 * 负责创建和配置 OkHttp 客户端和 Retrofit 实例
 * 
 * 架构说明：
 * ┌─────────────┐
 * │   Retrofit  │  ← 将接口方法转换为 HTTP 请求
 * ├─────────────┤
 * │   OkHttp    │  ← 实际执行 HTTP 请求
 * ├─────────────┤
 * │  Converter  │  ← JSON 序列化/反序列化（使用 Kotlin Serialization）
 * └─────────────┘
 */
object NetworkModule {
    
    /**
     * JSON 解析器配置
     * 
     * ignoreUnknownKeys：忽略 JSON 中未知的字段，提高兼容性
     * isLenient：宽松模式，允许一些不规范的 JSON
     * coerceInputValues：将 null 值转换为默认值
     */
    private val json = Json {
        ignoreUnknownKeys = true  // API 返回的字段可能比我们定义的多，忽略多余的
        isLenient = true          // 宽松解析
        coerceInputValues = true  // null 转为默认值
    }
    
    /**
     * 创建 OkHttpClient
     * 
     * OkHttp 是底层的 HTTP 客户端，负责：
     * - 连接管理
     * - 请求/响应拦截
     * - 超时设置
     * - 日志记录
     */
    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            // 连接超时：建立 TCP 连接的最长时间
            .connectTimeout(30, TimeUnit.SECONDS)
            // 读取超时：等待服务器响应的最长时间
            .readTimeout(30, TimeUnit.SECONDS)
            // 写入超时：发送请求体的最长时间
            .writeTimeout(30, TimeUnit.SECONDS)
            // 日志拦截器：在调试时打印请求/响应信息
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    // BODY 级别会打印请求和响应的完整内容
                    // 生产环境应该改为 NONE 或 BASIC
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
    }
    
    /**
     * 创建 Retrofit 实例
     * 
     * @param baseUrl 服务器基础地址，如 "http://192.168.1.100:8096/"
     *                注意：必须以 "/" 结尾
     * @return 配置好的 Retrofit 实例
     */
    fun createRetrofit(baseUrl: String): Retrofit {
        // 确保 baseUrl 以 "/" 结尾
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        return Retrofit.Builder()
            // 设置基础 URL
            .baseUrl(normalizedUrl)
            // 设置 HTTP 客户端
            .client(createOkHttpClient())
            // 设置 JSON 转换器（使用 Kotlin Serialization）
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }
    
    /**
     * 创建 JellyfinApi 实例
     * 
     * @param baseUrl 服务器地址
     * @return JellyfinApi 接口的实现
     */
    fun createJellyfinApi(baseUrl: String): JellyfinApi {
        return createRetrofit(baseUrl).create(JellyfinApi::class.java)
    }
}

