package com.black0bag.newapi.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "newapi_config")

/**
 * SettingsStore
 *
 * 配置持久化（DataStore）：
 * - pat：永久访问令牌（登录后获取，管理接口鉴权用）
 * - port：后端端口（默认 13000）
 * - theme：主题（system / light / dark）
 */
object SettingsStore {

    private val PAT_KEY = stringPreferencesKey("pat")
    private val PORT_KEY = stringPreferencesKey("port")
    private val THEME_KEY = stringPreferencesKey("theme")

    val pat: Flow<String> = dataStore.data.map { it[PAT_KEY] ?: "" }

    val port: Flow<Int> = dataStore.data.map { it[PORT_KEY]?.toIntOrNull() ?: 13000 }

    val theme: Flow<String> = dataStore.data.map { it[THEME_KEY] ?: "system" }

    suspend fun setPat(context: Context, value: String) {
        context.dataStore.edit { it[PAT_KEY] = value }
    }

    suspend fun setPort(context: Context, value: Int) {
        context.dataStore.edit { it[PORT_KEY] = value.toString() }
    }

    suspend fun setTheme(context: Context, value: String) {
        context.dataStore.edit { it[THEME_KEY] = value }
    }
}