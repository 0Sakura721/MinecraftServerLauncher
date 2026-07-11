package com.mcserver.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mcserver.launcher.data.JreInfo
import com.mcserver.launcher.data.JreStatus
import com.mcserver.launcher.server.ServerManager
import com.mcserver.launcher.ui.components.ThemeSelectorCard
import com.mcserver.launcher.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    val serverManager = ServerManager.instance
    val jreInfo by serverManager.jreInfo.collectAsState()
    val scope = rememberCoroutineScope()

    var showJreProgress by remember { mutableStateOf(false) }
    var jreProgress by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // 主题选择
        ThemeSelectorCard(
            currentTheme = currentTheme,
            onThemeSelected = onThemeChange
        )

        // JRE 管理
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Java 运行时管理",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (jreInfo.status) {
                                JreStatus.INSTALLED -> "已安装"
                                JreStatus.NOT_INSTALLED -> "未安装"
                                JreStatus.DOWNLOADING -> "下载中..."
                                JreStatus.EXTRACTING -> "解压中..."
                                JreStatus.ERROR -> "错误"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        if (jreInfo.status == JreStatus.INSTALLED) {
                            Text(
                                text = jreInfo.path,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    when (jreInfo.status) {
                        JreStatus.NOT_INSTALLED, JreStatus.ERROR -> {
                            Button(
                                onClick = {
                                    scope.launch {
                                        showJreProgress = true
                                        serverManager.installJre { progress ->
                                            jreProgress = progress
                                        }
                                        showJreProgress = false
                                    }
                                },
                                enabled = !showJreProgress
                            ) {
                                Text(if (showJreProgress) "安装中..." else "安装 JRE")
                            }
                        }
                        JreStatus.INSTALLED -> {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    serverManager.installJre { jreProgress = it }
                                }
                            }) {
                                Text("重新安装")
                            }
                        }
                        else -> {}
                    }
                }

                if (showJreProgress) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { jreProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${(jreProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // 关于
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "关于",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("版本", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("目标架构", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "ARM64 (v7a / v8a)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("平台", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Android 8.0+ (API 26+)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
