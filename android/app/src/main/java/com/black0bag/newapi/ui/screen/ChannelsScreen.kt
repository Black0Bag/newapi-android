package com.black0bag.newapi.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.black0bag.newapi.data.model.Channel
import com.black0bag.newapi.data.model.EmptyData
import com.black0bag.newapi.data.model.PageData
import com.black0bag.newapi.ui.ConfirmDialog
import com.black0bag.newapi.util.CopyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ChannelsScreen
 *
 * 渠道管理：列表 / 新建 / 测试 / 启停 / 删除
 * - 创建请求体是包裹结构：{"mode":"single","channel":{name,type,key,base_url,models,group}}
 * - 列表里 key 为空串（后端脱敏），不显示复制按钮避免误导
 * - type 用下拉选（基于 constant/channel.go 枚举）
 * - 删除/启停二次确认
 */

private val ChannelTypes = listOf(
    1 to "OpenAI",
    3 to "Azure OpenAI",
    8 to "Custom (OpenAI 兼容)",
    14 to "Anthropic",
    16 to "智谱 GLM",
    20 to "OpenRouter",
    24 to "Gemini",
    25 to "Moonshot",
    33 to "AWS Bedrock",
    40 to "SiliconFlow",
    41 to "Google Vertex",
    43 to "DeepSeek",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Channel?>(null) }
    val snackbar = remember { SnackbarHostState() }

    fun load() {
        scope.launch {
            isLoading = true; error = null
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val resp: ApiResponse<PageData<Channel>> = ApiClient.get(
                        "/api/channel/", PageData.serializer(Channel.serializer())
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

    fun toggleStatus(c: Channel) {
        val newStatus = if (c.status == 1) 2 else 1
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val resp: ApiResponse<EmptyData> = ApiClient.post(
                        "/api/channel/${c.id}/status",
                        mapOf("status" to newStatus),
                        EmptyData.serializer(),
                    )
                    if (!resp.success) error("切换失败: ${resp.message}")
                }
            }
            r.onFailure { snackbar.showSnackbar(it.message ?: "失败") }
            load()
        }
    }

    fun test(c: Channel) {
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = ApiClient.getRaw("/api/channel/test/${c.id}")
                    raw
                }
            }
            r.onSuccess { msg ->
                snackbar.showSnackbar(if (msg.length > 120) msg.take(120) + "..." else msg)
            }.onFailure { snackbar.showSnackbar(it.message ?: "失败") }
        }
    }

    fun doDelete(c: Channel) {
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val resp: ApiResponse<EmptyData> = ApiClient.delete(
                        "/api/channel/${c.id}", EmptyData.serializer()
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
                Icon(Icons.Default.Add, contentDescription = "新建渠道")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("渠道管理", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
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
                        Text("暂无渠道", style = MaterialTheme.typography.titleMedium)
                        Text("点右下角 + 添加第一个上游 API 渠道", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { c ->
                    ChannelCard(
                        c = c,
                        typeName = ChannelTypes.firstOrNull { it.first == c.type }?.second ?: "type=${c.type}",
                        onTest = { test(c) },
                        onToggle = { toggleStatus(c) },
                        onDelete = { pendingDelete = c },
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateChannelDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, type, key, baseUrl, models, group ->
                scope.launch {
                    val r = withContext(Dispatchers.IO) {
                        runCatching {
                            val body = mapOf(
                                "mode" to "single",
                                "channel" to mapOf(
                                    "name" to name,
                                    "type" to type,
                                    "key" to key,
                                    "base_url" to baseUrl,
                                    "models" to models,
                                    "group" to group,
                                ),
                            )
                            val resp: ApiResponse<EmptyData> = ApiClient.post(
                                "/api/channel/", body, EmptyData.serializer()
                            )
                            if (!resp.success) error("创建失败: ${resp.message}")
                        }
                    }
                    r.onSuccess { _ -> load() }.onFailure { snackbar.showSnackbar(it.message ?: "失败") }
                    showCreate = false
                }
            },
        )
    }

    pendingDelete?.let { c ->
        ConfirmDialog(
            title = "删除渠道？",
            message = "「${c.name}」删除后使用该渠道的请求将立即失败。",
            confirmText = "删除",
            onConfirm = { doDelete(c) },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun ChannelCard(
    c: Channel,
    typeName: String,
    onTest: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    c.name.ifBlank { "(未命名)" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                AssistChip(
                    onClick = onToggle,
                    label = { Text(if (c.status == 1) "启用" else "禁用") },
                    colors = if (c.status == 1) AssistChipDefaults.assistChipColors() else AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        labelColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "ID #${c.id} · $typeName · 分组=${c.group.ifBlank { "default" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            c.baseUrl?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "Base URL: $it",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
            if (c.models.isNotBlank()) {
                Text(
                    "Models: ${c.models}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = onTest) { Text("测试") }
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
private fun CreateChannelDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, type: Int, key: String, baseUrl: String, models: String, group: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var typeIndex by remember { mutableStateOf(0) }
    var key by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("https://api.openai.com") }
    var models by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("default") }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var fetching by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun fetchModels() {
        if (key.isBlank()) { fetchError = "请先填 Key"; return }
        scope.launch {
            fetching = true; fetchError = null
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    com.black0bag.newapi.data.ApiClient.fetchUpstreamModels(
                        ChannelTypes[typeIndex].first, key, baseUrl,
                    )
                }
            }
            r.onSuccess { list ->
                if (list.isEmpty()) fetchError = "上游返回空模型列表"
                else models = list.joinToString(",")
            }.onFailure { e ->
                fetchError = e.message ?: "获取失败"
            }
            fetching = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建渠道") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = typeMenuExpanded, onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }) {
                    OutlinedTextField(
                        readOnly = true,
                        value = "${ChannelTypes[typeIndex].first} - ${ChannelTypes[typeIndex].second}",
                        onValueChange = {},
                        label = { Text("类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                        ChannelTypes.forEachIndexed { idx, (t, n) ->
                            DropdownMenuItem(
                                text = { Text("$t - $n") },
                                onClick = { typeIndex = idx; typeMenuExpanded = false },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("Key（上游 API key）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = models, onValueChange = { models = it }, label = { Text("Models（逗号分隔）") }, modifier = Modifier.fillMaxWidth())
                fetchError?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = ::fetchModels, enabled = !fetching) {
                    Text(if (fetching) "获取中..." else "自动获取上游模型")
                }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = group, onValueChange = { group = it }, label = { Text("分组") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && key.isNotBlank(),
                onClick = {
                    onCreate(
                        name,
                        ChannelTypes[typeIndex].first,
                        key,
                        baseUrl,
                        models,
                        group.ifBlank { "default" },
                    )
                },
            ) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}