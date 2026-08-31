package com.black0bag.newapi.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.black0bag.newapi.data.ApiClient
import com.black0bag.newapi.data.model.ApiResponse
import com.black0bag.newapi.data.model.LoginResponse
import com.black0bag.newapi.data.model.TokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * LoginScreen
 *
 * 登录页：用户名 + 密码 → 获取 access_token → 换 PAT 永久令牌 → 存 DataStore。
 * 首次使用 root/123456（后端自动创建）。
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("root") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 标题
        Text(
            text = "New API 网关",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "登录后自动获取管理令牌",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        // 用户名
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("用户名") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        // 密码
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        // 错误提示
        if (errorText.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(24.dp))

        // 登录按钮
        Button(
            onClick = {
                isLoading = true
                errorText = ""
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            // 1. 登录拿 access_token（POST JSON: {username, password}）
                            val loginBody = mapOf(
                                "username" to username,
                                "password" to password,
                            )
                            val loginResp: ApiResponse<LoginResponse> = ApiClient.post(
                                "/api/user/login", loginBody, LoginResponse.serializer()
                            )
                            val token = loginResp.data?.accessToken ?: ""
                            // 2. 用 access_token 换 PAT（永久令牌）
                            // GenerateAccessToken 返回 {"success":true,"data":"sk-xxx"}，需提取 data 字段
                            ApiClient.pat = token
                            val rawResp = ApiClient.getRaw("/api/user/token")
                            ApiClient.pat = ""
                            // 从 JSON 响应中提取 data 字段（裸字符串 key）
                            val pat = org.json.JSONObject(rawResp).optString("data", "")
                            pat
                        }
                    }
                    result.onSuccess { pat ->
                        if (pat.isNotBlank()) {
                            // 存 PAT 到 DataStore（MainViewModel 会同步到 ApiClient）
                            com.black0bag.newapi.data.SettingsStore.getInstance(context).setPat(pat)
                            onLoginSuccess()
                        } else {
                            errorText = "登录失败：未获取到令牌"
                        }
                    }.onFailure { e ->
                        errorText = "登录失败：${e.message ?: "网络错误"}"
                    }
                    isLoading = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading,
        ) {
            Text(if (isLoading) "登录中..." else "登录", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(16.dp))

        // 提示
        Text(
            text = "首次使用默认账号 root / 123456",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}