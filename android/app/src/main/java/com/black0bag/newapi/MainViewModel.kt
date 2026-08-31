package com.black0bag.newapi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.black0bag.newapi.data.ApiClient
import com.black0bag.newapi.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * MainViewModel
 *
 * 全局状态持有者：
 * - pat：永久访问令牌（DataStore 持久化）
 * - port：后端端口（DataStore 持久化）
 * - theme：主题模式
 *
 * 所有子页面通过它共享鉴权状态。
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsStore.getInstance(app)

    private val _pat = MutableStateFlow("")
    val pat: StateFlow<String> = _pat.asStateFlow()

    private val _port = MutableStateFlow(13000)
    val port: StateFlow<Int> = _port.asStateFlow()

    val theme: StateFlow<String> = settings.theme
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    init {
        viewModelScope.launch {
            settings.pat.collect { stored ->
                _pat.value = stored
                ApiClient.pat = stored
            }
        }
        viewModelScope.launch {
            settings.port.collect { stored ->
                _port.value = stored
                ApiClient.port = stored
            }
        }
    }

    /** 保存 PAT（登录成功后调用） */
    fun setPat(value: String) {
        viewModelScope.launch {
            settings.setPat(value)
            ApiClient.pat = value
        }
    }

    /** 保存端口 */
    fun setPort(value: Int) {
        viewModelScope.launch {
            settings.setPort(value)
            ApiClient.port = value
        }
    }

    /** 保存主题 */
    fun setTheme(value: String) {
        viewModelScope.launch {
            settings.setTheme(value)
        }
    }

    /** 登出（清空 PAT） */
    fun logout() {
        viewModelScope.launch {
            settings.setPat("")
            ApiClient.pat = ""
            _pat.value = ""
        }
    }
}