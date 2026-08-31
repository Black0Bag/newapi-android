package com.black0bag.newapi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.black0bag.newapi.ui.AppNavHost
import com.black0bag.newapi.ui.theme.NewApiTheme

/**
 * MainActivity
 *
 * APP 入口：应用主题 + 主导航框架。
 * - 主题跟随系统/设置（亮/暗/自动）
 * - 未登录进登录页，已登录进主页
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val theme by mainViewModel.theme.collectAsState()
            val darkTheme = when (theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            NewApiTheme(darkTheme = darkTheme) {
                Surface {
                    AppNavHost(mainViewModel = mainViewModel)
                }
            }
        }
    }
}