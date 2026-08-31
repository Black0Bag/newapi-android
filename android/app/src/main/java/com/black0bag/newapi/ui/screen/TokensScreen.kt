package com.black0bag.newapi.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.black0bag.newapi.data.ApiClient
import com.black0bag.newapi.data.model.ApiResponse
import com.black0bag.newapi.data.model.EmptyData
import com.black0bag.newapi.data.model.PageData
import com.black0bag.newapi.data.model.Token
import com.black0bag.newapi.data.model.TokenKeyResponse
import com.black0bag.newapi.ui.ConfirmDialog
import com.black0bag.newapi.util.CopyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * TokensScreen
 *
 * 令牌管理：列表 / 创建 / 复制完整 sk- key / 启停 / 删除
 * - 实测：列表 key 脱敏 "8PHH**...**x9Iw"；完整 key 需 POST /api/token/:id/key
 * - 实测：sk- 前缀在调用方过滤层加（middleware/auth.go TrimPrefix），后端存裸串
 * - 删除/启停二次确认（用户要求）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokensScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<Token>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var revealedKey by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<Token?>(null) }
    val snackbar = remember { SnackbarHostState() }

    fun load() {
        scope.launch {
            isLoading = true; error = null
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val resp: ApiResponse<PageData<Token>> = ApiClient.get(
                        "/api/token/", PageData.serializer(Token.serializer())
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

    fun toggleStatus(t: Token) {
        val newStatus = if (t.status == 1) 2 else 1
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val resp: ApiResponse<Token> = ApiClient.putWithQuery(
                        path = "/api/token/",
                        query = "status_only=true",
                        body = mapOf("id" to t.id, "status" to newStatus),
                        serializer = Token.serializer(),
                    )
                    if (!resp.success) error("切换失败: ${resp.message}")
                }
            }
            r.onFailure { snackbar.showSnackbar(it.message ?: "失败") }
            load()
        }
    }

    fun revealKey(t: Token) {
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val resp: ApiResponse<TokenKeyResponse> = ApiClient.post(
                        "/api/token/${t.id}/key", null, TokenKeyResponse.serializer()
                    )
                    if (!resp.success) error("揭示失败: ${resp.message}")
                    resp.data?.key
                }
            }
            r.onSuccess { k ->
                if (k.isNullOrBlank()) snackbar.showSnackbar("未取到 key")
                else revealedKey = k
            }.onFailure { snackbar.showSnackbar(it.message ?: "失败") }
        }
    }

    fun doDelete(t: Token) {
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val resp: ApiResponse<TokenKeyResponse> = ApiClient.delete(
                        "/api/token/${t.id}", TokenKeyResponse.serializer()
                    )
                    if (!resp.success) error("删除失败: ${resp.message}")
                }
            }
            r.onFailure { snackbar.showSnackbar(it.message ?: "失败") }
            load()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "新建令牌")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("令牌管理", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
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
                        Text("暂无令牌", style = MaterialTheme.typography.titleMedium)
                        Text("点右下角 + 创建第一个", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { t ->
                    TokenCard(
                        t = t,
                        onReveal = { revealKey(t) },
                        onToggle = { toggleStatus(t) },
                        onDelete = { pendingDelete = t },
                        onCopyKey = { CopyUtils.copy(context, "令牌 key", "sk-$it") },
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateTokenDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, unlimited, days ->
                scope.launch {
                    val r = withContext(Dispatchers.IO) {
                        runCatching {
                            val body = buildMap<String, Any> {
                                put("name", name)
                                put("unlimited_quota", unlimited)
                                put("expired_time", if (days <= 0) -1 else -days * 86400 + (System.currentTimeMillis() / 1000).toInt())
                            }
                            val resp: ApiResponse<EmptyData> = ApiClient.post(
                                "/api/token/", body, EmptyData.serializer()
                            )
                            if (!resp.success) error("创建失败: ${resp.message}")
                            // 创建接口实测无 data 字段，创建后调用 revealKey 拿完整 key
                            null
                        }
                    }
                    r.onSuccess { _ ->
                        // 创建后刷新列表（用户可到列表点"揭示"按钮取完整 key）
                        load()
                    }.onFailure { snackbar.showSnackbar(it.message ?: "失败") }
                    showCreate = false
                }
            },
        )
    }

    revealedKey?.let { k ->
        AlertDialog(
            onDismissRequest = { revealedKey = null },
            title = { Text("令牌已生成（仅此一次可见）") },
            text = {
                Column {
                    Text("请妥善保存，关闭后不可再查看：", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "sk-$k",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { CopyUtils.copy(context, "令牌 key", "sk-$k"); revealedKey = null }) {
                    Text("复制并关闭")
                }
            },
        )
    }

    pendingDelete?.let { t ->
        ConfirmDialog(
            title = "删除令牌？",
            message = "「${t.name.ifBlank { "(未命名)" }}」删除后无法恢复，已使用该 key 的 agent 将立即失效。",
            confirmText = "删除",
            onConfirm = { doDelete(t) },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun TokenCard(
    t: Token,
    onReveal: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onCopyKey: (String) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    t.name.ifBlank { "(未命名)" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                AssistChip(
                    onClick = onToggle,
                    label = { Text(if (t.status == 1) "启用" else "禁用") },
                    colors = if (t.status == 1) AssistChipDefaults.assistChipColors() else AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        labelColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "ID #${t.id} · 创建于 ${formatTime(t.createdTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "sk-${t.key}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onReveal) { Text("揭示") }
            }
            Spacer(Modifier.height(4.dp))
            Row {
                TextButton(onClick = { onCopyKey(t.key) }) { Text("复制") }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTokenDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, unlimitedQuota: Boolean, validDays: Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var unlimited by remember { mutableStateOf(true) }
    var days by remember { mutableStateOf("30") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建令牌") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = unlimited, onCheckedChange = { unlimited = it })
                    Spacer(Modifier.width(8.dp))
                    Text("无限额度（自用推荐）")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = days, onValueChange = { days = it.filter { c -> c.isDigit() } },
                    label = { Text("有效期（天，0=永不过期）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(name.ifBlank { "token" }, unlimited, days.toIntOrNull() ?: 0) }) { Text("创建") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun formatTime(ts: Long): String {
    if (ts <= 0) return "—"
    return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts * 1000))
}