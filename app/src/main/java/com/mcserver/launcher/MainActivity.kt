package com.mcserver.launcher

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mcserver.launcher.data.PreferencesManager
import com.mcserver.launcher.data.ServerConfig
import com.mcserver.launcher.data.StartupMode
import com.mcserver.launcher.ui.components.ErrorBoundary
import com.mcserver.launcher.ui.navigation.Screen
import com.mcserver.launcher.ui.navigation.bottomNavItems
import com.mcserver.launcher.ui.screens.*
import com.mcserver.launcher.ui.theme.McServerTheme
import com.mcserver.launcher.ui.theme.ThemeMode
import com.mcserver.launcher.utils.PermissionHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var notificationLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        PermissionHelper.bind(this)
        notificationLauncher = PermissionHelper.createNotificationLauncher(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            PermissionHelper.shouldRequestNotificationPermission(this)
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val prefsManager = PreferencesManager(this)

        setContent {
            val themeMode by prefsManager.themeMode.collectAsState(initial = ThemeMode.DARK)
            val config by prefsManager.serverConfig.collectAsState(initial = ServerConfig())
            val setupCompleted by prefsManager.setupCompleted.collectAsState(initial = false)

            McServerTheme(themeMode = themeMode) {
                ErrorBoundary {
                    if (!setupCompleted) {
                        val setupScope = rememberCoroutineScope()
                        EnvSetupScreen(
                            onSetupComplete = {
                                setupScope.launch {
                                    prefsManager.setSetupCompleted()
                                }
                            }
                        )
                    } else {
                        MainApp(
                            themeMode = themeMode,
                            config = config,
                            prefsManager = prefsManager,
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PermissionHelper.refreshNotificationStatus(this)
    }
}

@Composable
fun MainApp(
    themeMode: ThemeMode,
    config: ServerConfig,
    prefsManager: PreferencesManager,
    onRequestNotificationPermission: () -> Unit = {}
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isSubScreen = currentDestination?.route in listOf(
        Screen.ServerConfig.route, Screen.Plugins.route,
        Screen.Players.route, Screen.Files.route,
        Screen.Backups.route, Screen.CoreDownload.route,
        Screen.ResourcePacks.route, Screen.Schedules.route,
        Screen.Worlds.route, Screen.Diagnostics.route,
        Screen.Modrinth.route, Screen.CrashReports.route,
        Screen.Appearance.route, Screen.Terminal.route,
        Screen.ServerList.route
    )

    val currentTitle = when (currentDestination?.route) {
        Screen.Home.route -> "首页"
        Screen.Console.route -> "控制台"
        Screen.Management.route -> "管理"
        Screen.Settings.route -> "设置"
        Screen.ServerConfig.route -> "服务器配置"
        Screen.Plugins.route -> "插件管理"
        Screen.Players.route -> "玩家管理"
        Screen.Files.route -> "文件管理"
        Screen.Backups.route -> "备份恢复"
        Screen.CoreDownload.route -> "下载核心"
        Screen.Modrinth.route -> "Modrinth"
        Screen.ResourcePacks.route -> "资源包"
        Screen.Schedules.route -> "定时任务"
        Screen.Worlds.route -> "世界管理"
        Screen.Diagnostics.route -> "诊断报告"
        Screen.CrashReports.route -> "崩溃报告"
        Screen.Appearance.route -> "外观"
        Screen.Terminal.route -> "Linux 终端"
        Screen.ServerList.route -> "服务器列表"
        else -> "MC Server"
    }

    Scaffold(
        topBar = {
            if (isSubScreen) {
                TopAppBar(
                    title = { Text(currentTitle) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    )
                )
            } else {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = currentTitle,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    )
                )
            }
        },
        bottomBar = {
            if (!isSubScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.label,
                                    tint = if (selected) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            label = {
                                Text(
                                    text = screen.label,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.95f)
            },
            exitTransition = {
                fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.95f)
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.95f)
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.95f)
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    config = config,
                    onNavigateToConfig = {
                        navController.navigate(Screen.ServerConfig.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToConsole = {
                        navController.navigate(Screen.Console.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToManagement = {
                        navController.navigate(Screen.Management.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToServerList = {
                        navController.navigate(Screen.ServerList.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToTerminal = {
                        navController.navigate(Screen.Terminal.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Console.route) {
                ConsoleScreen()
            }
            composable(Screen.Management.route) {
                ManagementScreen(
                    config = config,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.ServerConfig.route) {
                ServerConfigScreen(
                    config = config,
                    onConfigSave = { newConfig ->
                        scope.launch {
                            prefsManager.saveServerConfig(newConfig)
                        }
                    }
                )
            }
            composable(Screen.CoreDownload.route) {
                ServerCoreDownloadScreen(
                    config = config,
                    onJarDownloaded = { path, startupMode ->
                        scope.launch {
                            prefsManager.saveServerConfig(
                                config.copy(
                                    jarPath = path,
                                    startupMode = startupMode,
                                    shScriptPath = if (startupMode == StartupMode.SHELL_SCRIPT) path else config.shScriptPath
                                )
                            )
                        }
                    }
                )
            }
            composable(Screen.Modrinth.route) {
                ModrinthScreen()
            }
            composable(Screen.Plugins.route) {
                PluginsScreen()
            }
            composable(Screen.Players.route) {
                PlayersScreen()
            }
            composable(Screen.Files.route) {
                FilesScreen()
            }
            composable(Screen.Backups.route) {
                BackupsScreen(
                    config = config,
                    onConfigSave = { newConfig ->
                        scope.launch {
                            prefsManager.saveServerConfig(newConfig)
                        }
                    }
                )
            }
            composable(Screen.ResourcePacks.route) {
                ResourcePacksScreen()
            }
            composable(Screen.Schedules.route) {
                SchedulesScreen()
            }
            composable(Screen.Worlds.route) {
                WorldsScreen()
            }
            composable(Screen.Diagnostics.route) {
                DiagnosticsScreen(
                    config = config,
                    onNavigateToCrashReports = {
                        navController.navigate(Screen.CrashReports.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.CrashReports.route) {
                CrashReportsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    currentTheme = themeMode,
                    onThemeChange = { mode ->
                        scope.launch {
                            prefsManager.setTheme(mode)
                        }
                    },
                    onNavigateToAppearance = {
                        navController.navigate(Screen.Appearance.route) {
                            launchSingleTop = true
                        }
                    },
                    prefsManager = prefsManager
                )
            }
            composable(Screen.Appearance.route) {
                AppearanceScreen(
                    prefsManager = prefsManager,
                    currentTheme = themeMode,
                    onThemeChange = { mode ->
                        scope.launch {
                            prefsManager.setTheme(mode)
                        }
                    }
                )
            }
            composable(Screen.Terminal.route) {
                TerminalScreen()
            }
            composable(Screen.ServerList.route) {
                ServerListScreen(
                    onBack = { navController.popBackStack() },
                    onServerSelected = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}