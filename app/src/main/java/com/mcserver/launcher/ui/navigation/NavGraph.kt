package com.mcserver.launcher.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "服务器", Icons.Filled.Dns)
    data object Console : Screen("console", "控制台", Icons.Filled.Terminal)
    data object ServerConfig : Screen("server_config", "配置", Icons.Filled.Tune)
    data object Settings : Screen("settings", "设置", Icons.Filled.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Console,
    Screen.ServerConfig,
    Screen.Settings
)
