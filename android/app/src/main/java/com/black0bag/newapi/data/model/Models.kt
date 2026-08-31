package com.black0bag.newapi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** New API 统一响应包装：{success, message, data} */
@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val message: String = "",
    val data: T? = null,
)

/** 登录响应 */
@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String = "",
)

/** 获取 PAT 响应 */
@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String = "",
)

/** 用户信息 */
@Serializable
data class UserInfo(
    val id: Int = 0,
    val username: String = "",
    val displayName: String = "",
    val role: Int = 0,
    val status: Int = 0,
    val email: String = "",
    val quota: Long = 0,
    @SerialName("used_quota") val usedQuota: Long = 0,
    @SerialName("request_count") val requestCount: Int = 0,
)

/** 渠道 */
@Serializable
data class Channel(
    val id: Int = 0,
    val name: String = "",
    val type: Int = 0,
    val key: String = "",
    val baseURL: String = "",
    val models: String = "",
    val status: Int = 1,
    val priority: Int = 0,
    @SerialName("used_quota") val usedQuota: Long = 0,
    val balance: String = "",
    @SerialName("created_time") val createdTime: Long = 0,
    @SerialName("test_time") val testTime: Long = 0,
)

/** 令牌 */
@Serializable
data class Token(
    val id: Int = 0,
    val name: String = "",
    val key: String = "",
    val status: Int = 1,
    @SerialName("expired_time") val expiredTime: Long = 0,
    @SerialName("created_time") val createdTime: Long = 0,
)

/** 日志 */
@Serializable
data class LogItem(
    val id: Int = 0,
    val model: String = "",
    val type: Int = 0,
    val channelId: Int = 0,
    @SerialName("channel_name") val channelName: String = "",
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val quota: Long = 0,
    val status: Boolean = true,
    @SerialName("created_at") val createdAt: Long = 0,
    val time: String = "",
)

/** 分页数据 */
@Serializable
data class PageData<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 10,
)