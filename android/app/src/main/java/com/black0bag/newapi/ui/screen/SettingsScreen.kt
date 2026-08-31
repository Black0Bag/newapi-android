package com.black0bag.newapi.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.black0bag.newapi.MainViewModel
import com.black0bag.newapi.data.ApiClient
import com.black0bag.newapi.data.SettingsStore
import com.black0bag.newapi.ui.ConfirmDialog
import com.black0bag.newapi.util.CopyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * SettingsScreen
 *
 * 设置：主题切换 / 端口（显示当前）+ 配置备份导入导出
 * - 主题三选：跟随系统 / 浅色 / 深色（DataStore 持久化）
 * - 配置备份：导出 JSON 到 APP 私有目录 + 分享路径
 * - 导入：从指定路径读 JSON 并合并到 DataStore
 * - 注意：所有备份操作只在本地（不联网、不上传云端）—— 用户硬性要求
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: MainViewModel = viewModel()
    val theme by viewModel.theme.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var showExport by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var pendingLogout by remember { mutableStateOf(false) }
    var exportText by remember { mutableStateOf("") }
    var exportPath by remember { mutableStateOf("") }
    var importText by remember { mutableStateOf("") }
    val importPath = remember { mutableStateOf("") }
    val settings = remember { SettingsStore.getInstance(context) }

    fun exportNow() {
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val data = JSONObject()
                    data.put("port", ApiClient.port)
                    data.put("theme", theme)
                    data.put("pat", "")  // 安全：不导出 PAT（明文）
                    data.put("versionName", "0.5.0")
                    data.put("exportedAt", System.currentTimeMillis() / 1000)
                    val file = File(context.filesDir, "newapi-config.json")
                    file.writeText(data.toString(2))
                    file.absolutePath to data.toString(2)
                }
            }
            r.onSuccess { (path, text) ->
                exportPath = path; exportText = text; showExport = true
            }.onFailure { snackbar.showSnackbar("导出失败：${it.message}") }
        }
    }

    fun importNow() {
        val raw = importText.trim()
        if (raw.isEmpty()) {
            snackbar.showSnackbar("请先填入 JSON 内容")
            return
        }
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val obj = JSONObject(raw)
                    val port = obj.optInt("port", 13000)
                    val newTheme = obj.optString("theme", "system")
                    settings.setPort(port)
                    settings.setTheme(newTheme)
                    ApiClient.port = port
                }
            }
            r.onSuccess {
                importText = ""; importPath.value = ""
                snackbar.showSnackbar("已导入（注意：端口变更需重启后端）")
            }.onFailure { snackbar.showSnackbar("导入失败：${it.message}") }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("主题", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row {
                    listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (key, label) ->
                        FilterChip(
                            selected = theme == key,
                            onClick = { viewModel.setTheme(key) },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("后端端口", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("当前：${ApiClient.port}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "修改端口后需重启后端进程（停止再启动）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("配置备份", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "导出到 APP 私有目录，不上传任何云端。可手动复制导出文件或分享。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row {
                    OutlinedButton(onClick = ::exportNow, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("导出")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { showImport = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("导入")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("账户", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { pendingLogout = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("退出登录（清空 PAT）") }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "v0.5.0 · New API 安卓网关",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }

    if (showExport) {
        AlertDialog(
            onDismissRequest = { showExport = false },
            title = { Text("已导出") },
            text = {
                Column {
                    Text("保存路径：", style = MaterialTheme.typography.bodySmall)
                    Text(exportPath, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    Text("内容（前 200 字）：", style = MaterialTheme.typography.bodySmall)
                    Text(exportText.take(200) + if (exportText.length > 200) "\n..." else "", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    CopyUtils.copy(context, "配置导出", exportPath)
                    showExport = false
                }) { Text("复制路径") }
            },
            dismissButton = { TextButton(onClick = { showExport = false }) { Text("关闭") } },
        )
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("导入配置") },
            text = {
                Column {
                    Text(
                        "粘贴之前导出的 JSON 内容（注意：PAT 不会从文件导入，因为导出时不包含）",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("JSON 内容") },
                        minLines = 4, maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { importNow(); showImport = false }) { Text("导入") }
            },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text("取消") } },
        )
    }

    if (pendingLogout) {
        ConfirmDialog(
            title = "退出登录？",
            message = "将清空本地保存的 PAT，下次需重新输入 root / root123456。",
            confirmText = "退出",
            onConfirm = { viewModel.logout() },
            onDismiss = { pendingLogout = false },
        )
    }
}