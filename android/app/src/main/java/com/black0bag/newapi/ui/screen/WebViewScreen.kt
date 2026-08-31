package com.black0bag.newapi.ui.screen

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import com.black0bag.newapi.data.ApiClient

/**
 * WebViewScreen
 *
 * 网页版兜底：内嵌浏览器打开 New API 完整 Web 界面。
 * 覆盖 APP 未提供的管理功能（兑换码、系统设置等）。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen() {
    val context = LocalContext.current
    val url = ApiClient.baseUrl()

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()
                loadUrl(url)
            }
        },
    )
}