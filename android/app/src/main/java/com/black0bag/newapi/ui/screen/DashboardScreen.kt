package com.black0bag.newapi.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.black0bag.newapi.data.ApiClient
import com.black0bag.newapi.data.model.ApiResponse
import com.black0bag.newapi.data.model.UserInfo
import com.black0bag.newapi.data.model.UserStat
import com.black0bag.newapi.util.CopyUtils
import com.black0bag.newapi.ui.InfoRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * DashboardScreen
 *
 * 仪表盘：端到端验证 PAT 鉴权链路（GET /api/user/self + /api/log/self/stat）
 * - 无余额展示（用户硬性要求）
 * - 用户名可复制（自用 APP 复制场景多）
 * - 刷新 + 下拉
 */
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<UserInfo?>(null) }
    var stat by remember { mutableStateOf<UserStat?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    fun load() {
        scope.launch {
            isLoading = true
            error = null
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val u: ApiResponse<UserInfo> = ApiClient.get(
                        "/api/user/self", UserInfo.serializer()
                    )
                    if (!u.success) error("用户信息: ${u.message}")
                    val s: ApiResponse<UserStat> = ApiClient.get(
                        "/api/log/self/stat", UserStat.serializer()
                    )
                    Pair(u.data, s.data)
                }
            }
            result.onSuccess { (u, s) ->
                user = u; stat = s
            }.onFailure { e ->
                error = e.message ?: "未知错误"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "仪表盘",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = ::load, enabled = !isLoading) {
                    Text(if (isLoading) "…" else "↻", style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.height(12.dp))

            error?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("加载失败", color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(it, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = ::load) { Text("重试") }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            user?.let { u ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("账号信息", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                u.displayName.ifBlank { u.username },
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                CopyUtils.copy(context, "用户名", u.username)
                            }) { Text("复制") }
                        }
                        Text(
                            "ID #${u.id} · ${roleName(u.role)} · ${groupName(u.group)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            stat?.let { s ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("使用统计", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        InfoRow("本周期消耗配额", "${s.quota}")
                        InfoRow("近 60s 请求数 (RPM)", "${s.rpm}")
                        InfoRow("近 60s Token 数 (TPM)", "${s.tpm}")
                    }
                }
            }

            if (isLoading && user == null) {
                Spacer(Modifier.height(40.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

private fun roleName(role: Int) = when (role) {
    100 -> "Root 管理员"
    50 -> "普通管理员"
    10 -> "普通用户"
    0 -> "游客"
    else -> "未知 ($role)"
}

private fun groupName(group: String) = if (group.isBlank()) "默认分组" else group
