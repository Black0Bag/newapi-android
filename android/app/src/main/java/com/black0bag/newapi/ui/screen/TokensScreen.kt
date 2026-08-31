package com.black0bag.newapi.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * TokensScreen
 *
 * 令牌管理：列表/创建/删除/复制 sk-。
 * 待完善：接 /api/token。
 */
@Composable
fun TokensScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("令牌管理", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "生成 sk- 密钥（开发中）",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("暂无令牌", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "创建第一个令牌后显示",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text("创建令牌")
        }
    }
}