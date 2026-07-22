package com.mcserver.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mcserver.launcher.data.ServerConfig
import com.mcserver.launcher.server.ServerManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupsScreen(
    config: ServerConfig,
    onConfigSave: (ServerConfig) -> Unit
) {
    val serverManager = ServerManager.instance
    val backups by serverManager.backups.collectAsState()
    val scope = rememberCoroutineScope()

    var backupOnStop by remember { mutableStateOf(config.backupOnStop) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmRestore by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { serverManager.refreshBackups() }

    // 提示信息自动消失
    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3000)
            message = null
        }
    }

    confirmRestore?.let { name ->
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            icon = { Icon(Icons.Filled.Restore, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("恢复备份") },
            text = { Text("将用备份「$name」覆盖当前服务器文件。恢复前会自动再备份一次当前状态以便回滚。") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        busy = true
                        serverManager.restoreBackup(name)
                            .onSuccess { message = "已恢复备份：$name" }
                            .onFailure { message = "恢复失败：${it.message}" }
                        busy = false; confirmRestore = null
                    }
                }) { Text("恢复") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text("取消") } }
        )
    }

    confirmDelete?.let { name ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            icon = { Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("删除备份") },
            text = { Text("确定删除备份「$name」吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            serverManager.deleteBackup(name)
                                .onSuccess { message = "已删除备份：$name" }
                                .onFailure { message = "删除失败：${it.message}" }
                        }
                        confirmDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("备份与恢复", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // 手动备份
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("手动备份", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "完整复制服务器目录（JAR、server.properties、world/、插件等）到带时间戳的备份目录。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            serverManager.createBackup()
                                .onSuccess { message = "备份完成：$it" }
                                .onFailure { message = "备份失败：${it.message}" }
                            busy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy
                ) {
                    Icon(Icons.Filled.Backup, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (busy) "备份中..." else "立即备份")
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("停止服务器时自动备份", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = backupOnStop, onCheckedChange = {
                        backupOnStop = it
                        scope.launch { onConfigSave(config.copy(backupOnStop = it)) }
                    })
                }
            }
        }

        // 备份列表
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("历史备份", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { scope.launch { serverManager.refreshBackups() } }) {
                        Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("刷新")
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (backups.isEmpty()) {
                    Text(
                        "还没有备份。点击上方「立即备份」创建第一个。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    backups.forEach { entry ->
                        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                            .format(Date(entry.createdAt))
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.FolderCopy, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    "$time · ${entry.sizeMB} MB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { confirmRestore = entry.name }) {
                                Icon(Icons.Filled.Restore, "恢复", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { confirmDelete = entry.name }) {
                                Icon(Icons.Filled.DeleteOutline, "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }

        message?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
