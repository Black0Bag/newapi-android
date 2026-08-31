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

    /** DELETE 请求 */
    suspend fun <T> delete(path: String, serializer: KSerializer<T>): ApiResponse<T> =
        request("DELETE", path, null, serializer)

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
            return json.decodeFromString(serializer, bodyStr)
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