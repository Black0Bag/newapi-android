package com.black0bag.newapi.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.black0bag.newapi.data.ApiClient
import com.black0bag.newapi.data.model.ApiResponse
import com.black0bag.newapi.util.CopyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/**
 * ModelsScreen
 *
 * 模型市场：当前渠道可用模型列表
 * - 实测：GET /api/user/models → data 为字符串数组 ["gpt-4o", ...]
 * - 没有渠道时返回空数组（提示先建渠道）
 * - 每个模型右侧"复制"按钮（一键拷贝到剪贴板）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    fun load() {
        scope.launch {
            isLoading = true; error = null
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val resp: ApiResponse<List<String>> = ApiClient.get(
                        "/api/user/models", ListSerializer(String.serializer())
                    )
                    if (!resp.success) error("加载失败: ${resp.message}")
                    resp.data ?: emptyList()
                }
            }
            r.onSuccess { models = it }.onFailure { error = it.message }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("模型市场", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = ::load) {
                Text(if (isLoading) "…" else "↻", style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(Modifier.height(8.dp))
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
        if (models.isEmpty() && !isLoading) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("暂无可用模型", style = MaterialTheme.typography.titleMedium)
                    Text("请先到「渠道管理」添加一个渠道，模型的可用性取自已启用渠道的 models 字段。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(models) { name ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            CopyUtils.copy(context, "模型名", name)
                            scope.launch { snackbar.showSnackbar("已复制 $name") }
                        }) { Text("复制") }
                    }
                }
            }
        }
    }
}