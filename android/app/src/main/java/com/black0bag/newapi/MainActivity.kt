package com.black0bag.newapi

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MainActivity
 *
 * M0 极简 UI：1 个启停按钮 + 1 个状态文字 + 端口信息。
 * 验证「APP 控制 New API 后端启停」的核心技术路线。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }

    @Composable
    fun MainScreen() {
        var isRunning by remember { mutableStateOf(BackendProcessManager.isRunning) }
        var statusText by remember { mutableStateOf("就绪") }
        var isBusy by remember { mutableStateOf(false) }

        // 定期刷新运行状态
        LaunchedEffect(Unit) {
            while (true) {
                isRunning = BackendProcessManager.isRunning
                delay(1000)
            }
        }

        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "New API",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "本地 AI API 网关",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(32.dp))

                // 状态指示
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRunning)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isRunning) "● 运行中" else "○ 已停止",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isRunning)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "端口 ${BackendProcessManager.port()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 启停按钮
                Button(
                    onClick = {
                        if (isBusy) return@Button
                        isBusy = true
                        lifecycleScope.launch {
                            if (isRunning) {
                                statusText = "正在停止..."
                                BackendService.stopBackend(this@MainActivity)
                                // 等待进程退出
                                var attempts = 0
                                while (BackendProcessManager.isRunning && attempts < 20) {
                                    delay(300)
                                    attempts++
                                }
                                statusText = if (BackendProcessManager.isRunning) "停止失败" else "已停止"
                            } else {
                                statusText = "正在启动..."
                                val result = withContext(Dispatchers.IO) {
                                    BackendProcessManager.start(this@MainActivity)
                                }
                                statusText = if (result.isSuccess) {
                                    "启动成功 (port ${result.getOrNull()})"
                                } else {
                                    "启动失败: ${result.exceptionOrNull()?.message}"
                                }
                            }
                            isRunning = BackendProcessManager.isRunning
                            isBusy = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isBusy
                ) {
                    Text(
                        text = when {
                            isBusy -> "处理中..."
                            isRunning -> "停止后端"
                            else -> "启动后端"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 测试接口按钮（可选）
                TextButton(
                    onClick = {
                        val url = "http://127.0.0.1:${BackendProcessManager.port()}/api/status"
                        lifecycleScope.launch {
                            statusText = "测试中..."
                            val result = withContext(Dispatchers.IO) {
                                try {
                                    val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                                        requestMethod = "GET"
                                        connectTimeout = 2000
                                        readTimeout = 2000
                                    }
                                    val code = conn.responseCode
                                    conn.disconnect()
                                    "HTTP $code"
                                } catch (e: Exception) {
                                    "失败: ${e.message}"
                                }
                            }
                            statusText = "健康检查: $result"
                        }
                    },
                    enabled = isRunning
                ) {
                    Text("健康检查 /api/status")
                }
            }
        }
    }
}