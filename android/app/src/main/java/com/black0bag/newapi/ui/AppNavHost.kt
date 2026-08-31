package com.black0bag.newapi.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.black0bag.newapi.MainViewModel
import com.black0bag.newapi.ui.screen.ChannelsScreen
import com.black0bag.newapi.ui.screen.DashboardScreen
import com.black0bag.newapi.ui.screen.HomeScreen
import com.black0bag.newapi.ui.screen.LoginScreen
import com.black0bag.newapi.ui.screen.LogsScreen
import com.black0bag.newapi.ui.screen.ModelsScreen
import com.black0bag.newapi.ui.screen.SettingsScreen
import com.black0bag.newapi.ui.screen.TokensScreen
import com.black0bag.newapi.ui.screen.WebViewScreen

/**
 * AppNavHost
 *
 * 应用主框架：底部导航（4 tab）+ 二级路由（渠道/令牌/模型/网页版）
 * - 首屏永远是主页（可先启动后端，再进登录）
 * - 登录入口：主页快捷入口「登录/账号」（登录后存 PAT）
 * - 已登录（有 PAT）→ 各功能页正常使用；未登录 → 登录页提示
 */
@Composable
fun AppNavHost(
    mainViewModel: MainViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val pat by mainViewModel.pat.collectAsState()

    // 首屏永远是主页（后端可能未启动，主页有启停按钮）
    val startDestination = Routes.HOME

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // 是否显示底部导航（登录页不显示）
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    isLoggedIn = pat.isNotBlank(),
                    onLoginClick = {
                        navController.navigate(Routes.LOGIN)
                    },
                )
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen()
            }
            composable(Routes.LOGS) {
                LogsScreen()
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(Routes.CHANNELS) {
                ChannelsScreen()
            }
            composable(Routes.TOKENS) {
                TokensScreen()
            }
            composable(Routes.MODELS) {
                ModelsScreen()
            }
            composable(Routes.WEBVIEW) {
                WebViewScreen()
            }
        }
    }
}