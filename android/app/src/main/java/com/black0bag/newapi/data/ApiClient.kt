package com.black0bag.newapi.data

import com.black0bag.newapi.data.model.ApiResponse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * ApiClient
 *
 * New API 管理接口的轻量客户端（OkHttp 直连）。
 * - 统一 baseUrl：http://127.0.0.1:端口
 * - 统一鉴权：Authorization: Bearer PAT
 * - 响应解析：kotlinx.serialization（显式传 KSerializer，官方推荐做法）
 * - 请求体序列化：org.json（Android 内置，序列化 Map 最稳）
 *
 * 注意：不 inline，显式传 serializer（避免 Public-API inline 访问限制与泛型擦除）。
 */
object ApiClient {

    private const val DEFAULT_PORT = 13000

    @Volatile
    var port: Int = DEFAULT_PORT

    @Volatile
    var pat: String = ""

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** 当前 baseUrl */
    fun baseUrl(): String = "http://127.0.0.1:$port"

    /** 需要登录/认证时的错误 */
    class ApiException(message: String, val code: Int = 0) : Exception(message)

    /** GET 请求（显式传 serializer） */
    suspend fun <T> get(path: String, serializer: KSerializer<T>): ApiResponse<T> =
        request("GET", path, null, serializer)

    /** POST 请求 */
    suspend fun <T> post(path: String, body: Map<String, Any?>? = null, serializer: KSerializer<T>): ApiResponse<T> =
        request("POST", path, body, serializer)
/** PUT 请求 */
    suspend fun <T> put(path: String, body: Map<String, Any?>? = null, serializer: KSerializer<T>): ApiResponse<T> =
        request("PUT", path, body, serializer)

    /** PUT 带 query 参数（实测：令牌状态切换要 ?status_only=true） */
    suspend fun <T> putWithQuery(
        path: String,
        query: String,
        body: Map<String, Any?>?,
        serializer: KSerializer<T>,
    ): ApiResponse<T> = request("PUT", "$path?$query", body, serializer)


    /** DELETE 请求 */
    suspend fun <T> delete(path: String, serializer: KSerializer<T>): ApiResponse<T> =
        request("DELETE", path, null, serializer)

    /** GET 请求（返回原始字符串 body，用于 data 是裸字符串的接口） */
    suspend fun getRaw(path: String): String {
        val url = baseUrl() + path
        val builder = Request.Builder()
            .url(url)
            .method("GET", null)

        if (pat.isNotBlank()) {
            builder.header("Authorization", "Bearer $pat")
        }

        client.newCall(builder.build()).execute().use { resp ->
            val bodyStr = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw ApiException("HTTP ${resp.code}: $bodyStr", resp.code)
            }
            return bodyStr
        }
    }

    /** POST 请求（返回原始字符串 body，用于响应结构不规则的接口，如 /api/setup） */
    suspend fun postRaw(path: String, body: Map<String, Any?>? = null): String {
        val url = baseUrl() + path
        val builder = Request.Builder()
            .url(url)
            .method("POST", buildBody(body))

        if (pat.isNotBlank()) {
            builder.header("Authorization", "Bearer $pat")
        }

        client.newCall(builder.build()).execute().use { resp ->
            val bodyStr = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw ApiException("HTTP ${resp.code}: $bodyStr", resp.code)
            }
            return bodyStr
        }
    }

    /**
     * 确保后端已初始化。
     *
     * 关键：新版 New API 不自动创建 root 用户（源码 createRootAccountIfNeed 无人调用），
     * 必须先调用 POST /api/setup 创建管理员，否则无法登录。
     * 实测后端 /api/setup 返回 {"data":{"status":bool,"root_init":bool,...},"success":true}，
     * status=false 表示未初始化。新版密码强制 >= 8 位。
     */
    suspend fun ensureInitialized(rootUser: String = "root", rootPass: String = "root123456") {
        val setupStr = getRaw("/api/setup")
        val data = org.json.JSONObject(setupStr).optJSONObject("data")
        val initialized = data?.optBoolean("status") ?: false
        if (!initialized) {
            postRaw(
                "/api/setup",
                mapOf(
                    "username" to rootUser,
                    "password" to rootPass,
                    "confirmPassword" to rootPass,
                    "SelfUseModeEnabled" to true,
                    "DemoSiteEnabled" to false,
                ),
            )
        }
    }

    /** 通用请求（需在协程/IO 线程调用） */
    private suspend fun <T> request(
        method: String,
        path: String,
        body: Map<String, Any?>?,
        serializer: KSerializer<T>,
    ): ApiResponse<T> {
        val url = baseUrl() + path
        val builder = Request.Builder()
            .url(url)
            .method(method, buildBody(body))

        if (pat.isNotBlank()) {
            builder.header("Authorization", "Bearer $pat")
        }

        client.newCall(builder.build()).execute().use { resp ->
            val bodyStr = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw ApiException("HTTP ${resp.code}: $bodyStr", resp.code)
            }
            return json.decodeFromString(ApiResponse.serializer(serializer), bodyStr)
        }
    }

    /** 用 org.json 序列化请求体（最稳，无泛型坑） */
    private fun buildBody(body: Map<String, Any?>?): okhttp3.RequestBody? {
        if (body == null) return null
        val obj = JSONObject()
        body.forEach { (k, v) ->
            obj.put(k, v)
        }
        return obj.toString().toRequestBody("application/json".toMediaType())
    }
}