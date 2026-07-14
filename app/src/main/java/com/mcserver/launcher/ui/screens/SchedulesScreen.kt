package com.mcserver.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.mcserver.launcher.server.ScheduleManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen() {
    val scope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf(ScheduleManager.tasks) }
    var showAddDialog by remember { mutableStateOf(false) }

    // 新建任务的状态
    var newName by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf(ScheduleManager.TaskType.COMMAND) }
    var newCron by remember { mutableStateOf("0 * * * *") }
    var newPayload by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        ScheduleManager.load()
        tasks = ScheduleManager.tasks
        ScheduleManager.startScheduler()
    }

    fun refreshTasks() {
        tasks = ScheduleManager.tasks.toList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("定时任务") },
            actions = {
                IconButton(onClick = {
                    newName = ""; newType = ScheduleManager.TaskType.COMMAND
                    newCron = "0 * * * *"; newPayload = ""
                    showAddDialog = true
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "添加任务")
                }
            }
        )

        if (tasks.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Schedule, null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "暂无定时任务",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "点击右上角 + 添加定时任务。\n支持定时执行命令、备份、重启、广播等操作。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        newName = ""; newType = ScheduleManager.TaskType.COMMAND
                        newCron = "0 * * * *"; newPayload = ""
                        showAddDialog = true
                    }) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("添加任务")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks) { task ->
                    ScheduleTaskCard(
                        task = task,
                        onToggle = {
                            ScheduleManager.toggleTask(task.id)
                            refreshTasks()
                        },
                        onDelete = {
                            ScheduleManager.deleteTask(task.id)
                            refreshTasks()
                        }
                    )
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    // 添加任务对话框
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            icon = { Icon(Icons.Filled.AddAlarm, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("添加定时任务") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("任务名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 任务类型
                    Text("任务类型", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ScheduleManager.TaskType.entries.forEach { type ->
                            FilterChip(
                                selected = newType == type,
                                onClick = { newType = type },
                                label = {
                                    Text(type.label, style = MaterialTheme.typography.labelSmall)
                                },
                                leadingIcon = {
                                    Icon(
                                        when (type) {
                                            ScheduleManager.TaskType.COMMAND -> Icons.Filled.Terminal
                                            ScheduleManager.TaskType.BACKUP -> Icons.Filled.Backup
                                            ScheduleManager.TaskType.RESTART -> Icons.Filled.RestartAlt
                                            ScheduleManager.TaskType.BROADCAST -> Icons.Filled.Campaign
                                        },
                                        null,
                                        Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    // Cron 表达式
                    OutlinedTextField(
                        value = newCron,
                        onValueChange = { newCron = it },
                        label = { Text("Cron 表达式") },
                        supportingText = {
                            Text(
                                "格式: 分 时 日 月 周 (例: 0 3 * * * = 每天凌晨3点)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 快捷 cron 选择
                    Text("快捷设置", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "0 * * * *" to "每小时",
                            "0 0 * * *" to "每天0点",
                            "0 3 * * *" to "每天3点",
                            "0 */6 * * *" to "每6小时",
                            "*/30 * * * *" to "每30分钟"
                        ).forEach { (cron, label) ->
                            AssistChip(
                                onClick = { newCron = cron },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // 负载内容
                    OutlinedTextField(
                        value = newPayload,
                        onValueChange = { newPayload = it },
                        label = {
                            Text(
                                when (newType) {
                                    ScheduleManager.TaskType.COMMAND -> "命令内容"
                                    ScheduleManager.TaskType.BACKUP -> "备份标签 (可选)"
                                    ScheduleManager.TaskType.RESTART -> "重启前广播消息 (可选)"
                                    ScheduleManager.TaskType.BROADCAST -> "广播消息"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 1,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        ScheduleManager.addTask(
                            ScheduleManager.ScheduleTask(
                                name = newName,
                                type = newType,
                                cronExpression = newCron,
                                payload = newPayload
                            )
                        )
                        refreshTasks()
                        showAddDialog = false
                    }
                }) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ScheduleTaskCard(
    task: ScheduleManager.ScheduleTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 类型图标
                Icon(
                    when (task.type) {
                        ScheduleManager.TaskType.COMMAND -> Icons.Filled.Terminal
                        ScheduleManager.TaskType.BACKUP -> Icons.Filled.Backup
                        ScheduleManager.TaskType.RESTART -> Icons.Filled.RestartAlt
                        ScheduleManager.TaskType.BROADCAST -> Icons.Filled.Campaign
                    },
                    null,
                    tint = if (task.enabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.name.ifBlank { "未命名任务" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (task.enabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        "${task.type.label} · ${ScheduleManager.describeCron(task.cronExpression)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.payload.isNotBlank()) {
                        Text(
                            task.payload.take(50),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // 启用开关
                Switch(
                    checked = task.enabled,
                    onCheckedChange = { onToggle() }
                )
            }

            Spacer(Modifier.height(8.dp))

            // 运行统计
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "上次: ${ScheduleManager.formatLastRun(task.lastRun)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "已执行 ${task.runCount} 次",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.Delete, "删除",
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }

    // 删除确认
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除任务") },
            text = { Text("确定要删除定时任务「${task.name.ifBlank { "未命名" }}」吗？") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}
