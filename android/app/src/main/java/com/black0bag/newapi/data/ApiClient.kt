package com.black0bag.newapi.data

import com.black0bag.newapi.data.model.ApiResponse
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * ApiClient
 *
 * New API 管理接口的轻量客户端（OkHttp 直连，不引入 Retrofit 保持轻量）。
 * - 统一 baseUrl：http://127.0.0.1:端口
 * - 统一鉴权：Authorization: Bearer PAT
 * - 统一 JSON 解析（kotlinx.serialization）
 *
 * 注意：APP 控制的后端跑在本地 127.0.0.1，所有管理接口走这里。
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

    /** GET 请求，返回 ApiResponse<T> */
    suspend inline fun <reified T> get(path: String): ApiResponse<T> = request("GET", path, null)

    /** POST 请求 */
    suspend inline fun <reified T> post(path: String, body: Any? = null): ApiResponse<T> =
        request("POST", path, body)

    /** PUT 请求 */
    suspend inline fun <reified T> put(path: String, body: Any? = null): ApiResponse<T> =
        request("PUT", path, body)

    /** DELETE 请求 */
    suspend inline fun <reified T> delete(path: String): ApiResponse<T> = request("DELETE", path, null)

    /** 通用请求（需在协程/IO 线程调用） */
    suspend fun <T> request(method: String, path: String, body: Any?): ApiResponse<T> {
        val url = baseUrl() + path
        val builder = Request.Builder()
            .url(url)
            .method(method, buildBody(body))

        // 鉴权：PAT（如果有）
        if (pat.isNotBlank()) {
            builder.header("Authorization", "Bearer $pat")
        }

        client.newCall(builder.build()).execute().use { resp ->
            val bodyStr = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw ApiException("HTTP ${resp.code}: $bodyStr", resp.code)
            }
            return json.decodeFromString(bodyStr)
        }
    }

    private fun buildBody(body: Any?): okhttp3.RequestBody? {
        if (body == null) return null
        return json.encodeToString(kotlinx.serialization.serializer(body::class), body)
            .toRequestBody("application/json".toMediaType())
    }
}