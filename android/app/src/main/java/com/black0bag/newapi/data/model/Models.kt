package com.black0bag.newapi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 数据模型 —— 全部字段以 2026-08-31 本地后端实测 JSON 为准（rules.md：实测优先，禁止猜字段）
 * 实测记录见 docs/plan.md M5-③
 */

/** New API 统一响应包装：{success, message, data} */
@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val message: String = "",
    val data: T? = null,
)

/** 登录响应（实测：data.access_token） */
@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String = "",
)

/** 无 data 的通用返回（创建/删除类接口实测 data 缺省） */
@Serializable
class EmptyData

/** 用户信息（实测 GET /api/user/self data 字段） */
@Serializable
data class UserInfo(
    val id: Int = 0,
    val username: String = "",
    @SerialName("display_name") val displayName: String = "",
    val role: Int = 0,
    val status: Int = 0,
    val email: String = "",
    val group: String = "",
    @SerialName("used_quota") val usedQuota: Long = 0,
    @SerialName("request_count") val requestCount: Int = 0,
)

/** 使用统计（实测 GET /api/log/self/stat → {quota, rpm, tpm}） */
@Serializable
data class UserStat(
    val quota: Long = 0,
    val rpm: Int = 0,
    val tpm: Int = 0,
)

/**
 * 令牌（实测 GET /api/token/ items 字段：
 * key 是脱敏串"8PHH**...**x9Iw"，完整 key 需 POST /api/token/:id/key 揭示）
 */
@Serializable
data class Token(
    val id: Int = 0,
    val name: String = "",
    val key: String = "",
    val status: Int = 1,
    @SerialName("created_time") val createdTime: Long = 0,
    @SerialName("accessed_time") val accessedTime: Long = 0,
    @SerialName("expired_time") val expiredTime: Long = 0,
    @SerialName("unlimited_quota") val unlimitedQuota: Boolean = false,
    val group: String = "",
)

/** 令牌完整 key（实测 POST /api/token/:id/key → data:{key:"8PHH..."}，无 sk- 前缀，前端展示时补） */
@Serializable
data class TokenKeyResponse(
    val key: String = "",
)

/**
 * 渠道（实测 GET /api/channel/ items 字段；列表里 key 为空串（脱敏不回显）。
 * 创建请求要包一层：{"mode":"single","channel":{...}}
 * type 枚举（constant/channel.go）：1=OpenAI 3=Azure 8=Custom 14=Anthropic 16=智谱
 * 20=OpenRouter 24=Gemini 25=Moonshot 33=AWS 40=SiliconFlow 41=Vertex 43=DeepSeek
 */
@Serializable
data class Channel(
    val id: Int = 0,
    val name: String = "",
    val type: Int = 0,
    val key: String = "",
    val status: Int = 1,
    @SerialName("base_url") val baseUrl: String? = "",
    val models: String = "",
    val group: String = "",
    val priority: Int? = 0,
    val weight: Int? = 0,
    @SerialName("created_time") val createdTime: Long = 0,
    @SerialName("response_time") val responseTime: Int = 0,
    val tag: String? = null,
)

/**
 * 日志（实测 GET /api/log/self items 字段；
 * type：0未知 1充值 2消费 3管理 4系统 5错误 6退款 7登录）
 */
@Serializable
data class LogItem(
    val id: Int = 0,
    @SerialName("created_at") val createdAt: Long = 0,
    val type: Int = 0,
    val content: String = "",
    val username: String = "",
    @SerialName("token_name") val tokenName: String = "",
    @SerialName("model_name") val modelName: String = "",
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    val useTime: Int = 0,
    val channel: Int = 0,
    @SerialName("channel_name") val channelName: String = "",
    val ip: String = "",
    @SerialName("request_id") val requestId: String = "",
)

/** 分页（实测 data：{items,total,page,page_size}，注意 page_size 下划线） */
@Serializable
data class PageData<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 10,
)