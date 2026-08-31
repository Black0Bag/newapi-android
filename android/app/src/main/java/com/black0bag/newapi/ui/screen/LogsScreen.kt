package com.black0bag.newapi.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.black0bag.newapi.data.ApiClient
import com.black0bag.newapi.data.model.ApiResponse
import com.black0bag.newapi.data.model.LogItem
import com.black0bag.newapi.data.model.PageData
import com.black0bag.newapi.util.CopyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * LogsScreen
 *
 * 请求日志（仅当前用户的）：
 * - GET /api/log/self?p=N&page_size=20
 * - type: 0未知 1充值 2消费 3管理 4系统 5错误 6退款 7登录
 * - 每条日志有"复制"按钮（用户硬性要求：request_id 等关键字段可复制）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<LogItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    fun load() {
        scope.launch {
            isLoading = true; error = null
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val resp: ApiResponse<PageData<LogItem>> = ApiClient.get(
                        "/api/log/self?p=0&page_size=20", PageData.serializer(LogItem.serializer())
                    )
                    if (!resp.success) error("加载失败: ${resp.message}")
                    resp.data?.items ?: emptyList()
                }
            }
            r.onSuccess { items = it }.onFailure { error = it.message }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("请求日志", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = ::load) {
                Text(if (isLoading) "…" else "↻", style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(Modifier.height(8.dp))
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
        if (items.isEmpty() && !isLoading) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("暂无日志", style = MaterialTheme.typography.titleMedium)
                    Text("配置渠道 + 创建令牌后产生请求就有日志了", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { l ->
                LogCard(l, onCopy = {
                    CopyUtils.copy(context, "request_id", l.requestId)
                    scope.launch { snackbar.showSnackbar("request_id 已复制") }
                })
            }
        }
    }
}

@Composable
private fun LogCard(l: LogItem, onCopy: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {},
                    label = { Text(logTypeName(l.type), style = MaterialTheme.typography.labelSmall) },
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    formatTime(l.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "复制 request_id", modifier = Modifier.size(18.dp))
                }
            }
            if (l.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(l.content, style = MaterialTheme.typography.bodyMedium)
            }
            val meta = buildList {
                if (l.modelName.isNotBlank()) add("model=${l.modelName}")
                if (l.tokenName.isNotBlank()) add("token=${l.tokenName}")
                if (l.channelName.isNotBlank()) add("channel=${l.channelName}")
                if (l.promptTokens > 0 || l.completionTokens > 0) {
                    add("tokens=${l.promptTokens}+${l.completionTokens}")
                }
                if (l.ip.isNotBlank()) add("ip=${l.ip}")
            }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun logTypeName(type: Int) = when (type) {
    1 -> "充值"
    2 -> "消费"
    3 -> "管理"
    4 -> "系统"
    5 -> "错误"
    6 -> "退款"
    7 -> "登录"
    else -> "类型$type"
}

private fun formatTime(ts: Long): String {
    if (ts <= 0) return "—"
    return java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(ts * 1000))
}