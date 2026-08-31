package com.black0bag.newapi.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 顶层 DataStore 扩展（官方推荐写法：单例 DataStore）
private val Context.dataStore by preferencesDataStore(name = "newapi_config")

/**
 * SettingsStore
 *
 * 配置持久化（DataStore）：
 * - pat：永久访问令牌（登录后获取，管理接口鉴权用）
 * - port：后端端口（默认 13000）
 * - theme：主题（system / light / dark）
 *
 * 用 class + Context 构造（官方推荐），getInstance 获取单例。
 */
class SettingsStore(private val context: Context) {

    private val PAT_KEY = stringPreferencesKey("pat")
    private val PORT_KEY = stringPreferencesKey("port")
    private val THEME_KEY = stringPreferencesKey("theme")

    val pat: Flow<String> = context.dataStore.data.map { it[PAT_KEY] ?: "" }

    val port: Flow<Int> = context.dataStore.data.map { it[PORT_KEY]?.toIntOrNull() ?: 13000 }

    val theme: Flow<String> = context.dataStore.data.map { it[THEME_KEY] ?: "system" }

    suspend fun setPat(value: String) {
        context.dataStore.edit { it[PAT_KEY] = value }
    }

    suspend fun setPort(value: Int) {
        context.dataStore.edit { it[PORT_KEY] = value.toString() }
    }

    suspend fun setTheme(value: String) {
        context.dataStore.edit { it[THEME_KEY] = value }
    }

    companion object {
        @Volatile
        private var instance: SettingsStore? = null

        /** 获取单例 */
        fun getInstance(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context.applicationContext).also { instance = it }
            }
    }
}