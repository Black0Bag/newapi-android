package com.black0bag.newapi.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// 品牌色：深蓝（AI 科技感）
private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF1A73E8),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = androidx.compose.ui.graphics.Color(0xFF34A853),
    tertiary = androidx.compose.ui.graphics.Color(0xFF7C4DFF),
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF8AB4F8),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF062E6F),
    secondary = androidx.compose.ui.graphics.Color(0xFF81C995),
    tertiary = androidx.compose.ui.graphics.Color(0xFFD0BCFF),
)

/**
 * 应用主题：Material 3
 * - Android 12+（API 31）：动态色（dynamicColor），跟随壁纸
 * - 旧版本：品牌色板回退
 * - 深色模式跟随系统
 */
@Composable
fun NewApiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}