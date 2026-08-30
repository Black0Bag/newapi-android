package com.black0bag.newapi

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * BackendProcessManager
 *
 * 负责 New API 后端二进制的生命周期管理：
 *  - 定位 .so 二进制（nativeLibraryDir 下的 libnewapi.so）
 *  - 启动进程（ProcessBuilder + 环境变量）
 *  - 健康检查（/api/status）
 *  - 停止进程（优雅关停，超时强杀）
 *
 * 关键技术点（W^X 限制）：
 *  Android 10+ 禁止执行 filesDir 里的文件，必须把二进制伪装成 .so
 *  放在 jniLibs/arm64-v8a/，配合 extractNativeLibs=true 解压到
 *  nativeLibraryDir 后执行。
 */
object BackendProcessManager {
    private const val TAG = "BackendProcess"
    private const val BINARY_NAME = "libnewapi.so"
    private const val DEFAULT_PORT = 13000
    private const val START_TIMEOUT_MS = 20_000L

    @Volatile
    var process: Process? = null
        private set

    private val isStarting = AtomicBoolean(false)
    private val isStopping = AtomicBoolean(false)

    val isRunning: Boolean
        get() = process?.isAlive == true

    /** 定位二进制路径 */
    fun findBinary(context: Context): File {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val binary = File(nativeDir, BINARY_NAME)
        if (!binary.exists()) {
            throw IllegalStateException("Binary not found: ${binary.absolutePath}")
        }
        return binary
    }

    /** 启动后端 */
    @Synchronized
    fun start(context: Context): Result<Int> {
        if (isRunning) return Result.success(port())
        if (isStarting.get()) return Result.failure(IllegalStateException("Backend is starting..."))

        return try {
            isStarting.set(true)
            val binary = findBinary(context)
            val dataDir = context.getDir("newapi_data", Context.MODE_PRIVATE)
            val dbFile = File(dataDir, "newapi.db")
            val logFile = File(dataDir, "server.log")

            val port = DEFAULT_PORT

            val cmd = listOf(
                binary.absolutePath,
                "--port", port.toString(),
            )

            val pb = ProcessBuilder(cmd)
                .directory(dataDir)
                .redirectErrorStream(true)
                .redirectOutput(logFile)

            val env = pb.environment()
            env["PORT"] = port.toString()
            env["SQLITE_PATH"] = dbFile.absolutePath
            // New API 会自动创建管理员账号：root / 123456
            env["INITIAL_ROOT_TOKEN"] = ""
            env["TZ"] = "Asia/Shanghai"

            val proc = pb.start()
            process = proc

            // 等待健康检查
            val healthy = waitForHealthy(port, START_TIMEOUT_MS)
            if (!healthy) {
                proc.destroy()
                process = null
                Result.failure(IllegalStateException("Backend failed health check"))
            } else {
                Result.success(port)
            }
        } catch (e: Exception) {
            Log.e(TAG, "start failed", e)
            Result.failure(e)
        } finally {
            isStarting.set(false)
        }
    }

    /** 停止后端 */
    @Synchronized
    fun stop(): Boolean {
        if (isStopping.get()) return false
        isStopping.set(true)
        return try {
            val proc = process ?: return false
            // 先尝试优雅关停（SIGTERM）
            proc.destroy()
            val exited = proc.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            if (!exited) {
                // 超时强杀（SIGKILL）
                proc.destroyForcibly()
                proc.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
            }
            process = null
            true
        } catch (e: Exception) {
            Log.e(TAG, "stop failed", e)
            false
        } finally {
            isStopping.set(false)
        }
    }

    /** 获取当前端口 */
    fun port(): Int = DEFAULT_PORT

    /** 健康检查 */
    private fun waitForHealthy(port: Int, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        val url = "http://127.0.0.1:$port/api/status"
        while (System.currentTimeMillis() < deadline) {
            if (!isRunning) return false
            try {
                val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 1000
                    readTimeout = 1000
                }
                val code = conn.responseCode
                conn.disconnect()
                if (code == 200) return true
            } catch (_: Exception) {
                // 服务还没起来，继续等
            }
            Thread.sleep(500)
        }
        return false
    }

    /** 读取日志尾部 */
    fun tailLog(context: Context, lines: Int = 20): String {
        return try {
            val logFile = File(context.getDir("newapi_data", Context.MODE_PRIVATE), "server.log")
            if (!logFile.exists()) return "(no log yet)"
            logFile.readLines().takeLast(lines).joinToString("\n")
        } catch (e: Exception) {
            "log error: ${e.message}"
        }
    }
}