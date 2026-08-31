package com.black0bag.newapi.ui.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.black0bag.newapi.util.CopyUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.black0bag.newapi.BackendProcessManager
import com.black0bag.newapi.ui.Routes

/**
 * HomeScreen
 *
 * 主页：后端启停 + 状态 + 端口/URL 复制 + 快速入口
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    isLoggedIn: Boolean = false,
    onLoginClick: () -> Unit = {},
) {
    var isRunning by remember { mutableStateOf(BackendProcessManager.isRunning) }
    var statusText by remember { mutableStateOf("就绪") }
    var isBusy by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    var logText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // 标题
            Text(
                text = "New API 网关",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "本地 AI API 中转站管理",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            // 状态卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRunning)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (isRunning) "● 运行中" else "○ 已停止",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isRunning)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "端口 ${BackendProcessManager.port()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 登录入口（未登录时显示，可先启动后端再登录）
            if (!isLoggedIn) {
                OutlinedCard(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.AccountCircle,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("登录 / 账号", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "启动后端后，在此登录获取管理权限",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("›", style = MaterialTheme.typography.titleLarge)
                    }
                }
            } else {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("已登录（管理令牌已保存）", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 启停按钮
            Button(
                onClick = {
                    if (isBusy) return@Button
                    isBusy = true
                    scope.launch {
                        if (isRunning) {
                            statusText = "正在停止..."
                            com.black0bag.newapi.BackendService.stopBackend(context)
                            var attempts = 0
                            while (BackendProcessManager.isRunning && attempts < 20) {
                                delay(300)
                                attempts++
                            }
                            statusText = if (BackendProcessManager.isRunning) "停止失败" else "已停止"
                        } else {
                            statusText = "正在启动..."
                            val result = withContext(Dispatchers.IO) {
                                BackendProcessManager.start(context)
                            }
                            statusText = if (result.isSuccess) {
                                "启动成功 (port ${result.getOrNull()})"
                            } else {
                                val err = result.exceptionOrNull()?.message ?: "未知错误"
                                logText = err
                                if (err.length > 500) err.take(500) + "\n... (点「查看日志」看完整)" else err
                            }
                        }
                        isRunning = BackendProcessManager.isRunning
                        isBusy = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isBusy,
            ) {
                Text(
                    text = when {
                        isBusy -> "处理中..."
                        isRunning -> "停止后端"
                        else -> "启动后端"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(Modifier.height(16.dp))

            // API 地址（带复制）
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "API 地址（Agent 用）",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "http://127.0.0.1:${BackendProcessManager.port()}/v1",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        )
                        TextButton(onClick = {
                            CopyUtils.copy(context, "API 地址", "http://127.0.0.1:${BackendProcessManager.port()}/v1")
                        }) { Text("复制") }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 快速入口
            Text(
                text = "功能入口",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))

            val features = listOf(
                Triple(Routes.CHANNELS, "渠道管理", "配置上游 API"),
                Triple(Routes.TOKENS, "令牌管理", "生成 sk- 密钥"),
                Triple(Routes.MODELS, "模型市场", "查看可用模型"),
                Triple(Routes.WEBVIEW, "网页版", "完整后台管理"),
            )
            features.forEach { (route, title, desc) ->
                OutlinedCard(
                    onClick = { onNavigate(route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleMedium)
                            Text(desc, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("›", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 查看日志按钮
            TextButton(
                onClick = {
                    scope.launch {
                        logText = withContext(Dispatchers.IO) {
                            BackendProcessManager.tailLog(context, 40)
                        }
                        showLog = true
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("查看后端日志")
            }
        }
    }

    // 日志对话框
    if (showLog) {
        AlertDialog(
            onDismissRequest = { showLog = false },
            title = { Text("后端日志 (server.log)") },
            text = {
                val scroll = rememberScrollState()
                Text(
                    text = logText,
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(scroll),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        CopyUtils.copy(context, "后端日志", logText)
                    }) { Text("复制") }
                    TextButton(onClick = { showLog = false }) { Text("关闭") }
                }
            },
        )
    }
}