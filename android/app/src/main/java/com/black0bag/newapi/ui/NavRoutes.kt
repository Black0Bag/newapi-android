package com.black0bag.newapi.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.ui.graphics.vector.ImageVector

/** 应用路由常量 */
object Routes {
    const val HOME = "home"
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val CHANNELS = "channels"
    const val TOKENS = "tokens"
    const val MODELS = "models"
    const val LOGS = "logs"
    const val SETTINGS = "settings"
    const val WEBVIEW = "webview"
}

/** 底部导航目的地 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/** 底部导航项（4 个主 tab） */
val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "主页", Icons.Default.Home),
    BottomNavItem(Routes.DASHBOARD, "仪表盘", Icons.Default.Speed),
    BottomNavItem(Routes.LOGS, "日志", Icons.Default.ReceiptLong),
    BottomNavItem(Routes.SETTINGS, "设置", Icons.Default.Settings),
)

/** 二级功能入口（从主页进入，不走底部导航） */
val featureItems = listOf(
    BottomNavItem(Routes.CHANNELS, "渠道", Icons.Default.ViewModule),
    BottomNavItem(Routes.TOKENS, "令牌", Icons.Default.Key),
    BottomNavItem(Routes.MODELS, "模型", Icons.Default.Key),
    BottomNavItem(Routes.WEBVIEW, "网页版", Icons.Default.Settings),
)