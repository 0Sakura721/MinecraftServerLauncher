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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen() {
    var tasks by remember { mutableStateOf(ScheduleManager.tasks) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<ScheduleManager.ScheduleTask?>(null) }

    // 新建/编辑任务的状态
    var taskName by remember { mutableStateOf("") }
    var taskType by remember { mutableStateOf(ScheduleManager.TaskType.COMMAND) }
    var taskCron by remember { mutableStateOf("0 * * * *") }
    var taskPayload by remember { mutableStateOf("") }
    var showPresets by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ScheduleManager.load()
        tasks = ScheduleManager.tasks
        ScheduleManager.startScheduler()
    }

    fun refreshTasks() {
        tasks = ScheduleManager.tasks.toList()
    }

    fun openAddDialog() {
        taskName = ""; taskType = ScheduleManager.TaskType.COMMAND
        taskCron = "0 * * * *"; taskPayload = ""
        showPresets = false
        editingTask = null
        showAddDialog = true
    }

    fun openEditDialog(task: ScheduleManager.ScheduleTask) {
        taskName = task.name; taskType = task.type
        taskCron = task.cronExpression; taskPayload = task.payload
        showPresets = false
        editingTask = task
        showAddDialog = true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("定时任务") },
            actions = {
                IconButton(onClick = { openAddDialog() }) {
                    Icon(Icons.Filled.Add, contentDescription = "添加任务")
                }
            }
        )

        if (tasks.isEmpty()) {
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
                        "点击右上角 + 添加定时任务。\n支持定时执行命令、备份、重启、广播、\n保存世界、停止服务器等操作。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { openAddDialog() }) {
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
                        onRunNow = {
                            ScheduleManager.runTaskNow(task.id)
                            refreshTasks()
                        },
                        onEdit = { openEditDialog(task) },
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

    // 添加/编辑任务对话框
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            icon = {
                Icon(
                    if (editingTask != null) Icons.Filled.Edit else Icons.Filled.AddAlarm,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(if (editingTask != null) "编辑定时任务" else "添加定时任务") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = taskName,
                        onValueChange = { taskName = it },
                        label = { Text("任务名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 任务类型
                    Text("任务类型", style = MaterialTheme.typography.labelMedium)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 第一行：COMMAND, BACKUP, RESTART
                        listOf(
                            ScheduleManager.TaskType.COMMAND,
                            ScheduleManager.TaskType.BACKUP,
                            ScheduleManager.TaskType.RESTART
                        ).forEach { type ->
                            FilterChip(
                                selected = taskType == type,
                                onClick = { taskType = type },
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
                                            ScheduleManager.TaskType.STOP -> Icons.Filled.Stop
                                            ScheduleManager.TaskType.WORLD_SAVE -> Icons.Filled.Save
                                        },
                                        null, Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            ScheduleManager.TaskType.BROADCAST,
                            ScheduleManager.TaskType.STOP,
                            ScheduleManager.TaskType.WORLD_SAVE
                        ).forEach { type ->
                            FilterChip(
                                selected = taskType == type,
                                onClick = { taskType = type },
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
                                            ScheduleManager.TaskType.STOP -> Icons.Filled.Stop
                                            ScheduleManager.TaskType.WORLD_SAVE -> Icons.Filled.Save
                                        },
                                        null, Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    // Cron 表达式
                    OutlinedTextField(
                        value = taskCron,
                        onValueChange = { taskCron = it },
                        label = { Text("Cron 表达式") },
                        supportingText = {
                            val isValid = ScheduleManager.validateCron(taskCron)
                            Text(
                                if (isValid) ScheduleManager.describeCron(taskCron)
                                else "格式: 分 时 日 月 周（5字段，用空格分隔）",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isValid) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = taskCron.isNotBlank() && !ScheduleManager.validateCron(taskCron)
                    )

                    // 预设按钮
                    TextButton(onClick = { showPresets = !showPresets }) {
                        Icon(
                            if (showPresets) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            null, Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("常用预设")
                    }

                    if (showPresets) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                ScheduleManager.getCronPresets().chunked(3).forEach { row ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        row.forEach { (cron, label) ->
                                            AssistChip(
                                                onClick = {
                                                    taskCron = cron
                                                    showPresets = false
                                                },
                                                label = {
                                                    Text(
                                                        label,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        // 补齐不足3个的
                                        repeat(3 - row.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }

                    // 负载内容
                    OutlinedTextField(
                        value = taskPayload,
                        onValueChange = { taskPayload = it },
                        label = {
                            Text(
                                when (taskType) {
                                    ScheduleManager.TaskType.COMMAND -> "命令内容"
                                    ScheduleManager.TaskType.BACKUP -> "备份标签 (可选)"
                                    ScheduleManager.TaskType.RESTART -> "重启前广播消息 (可选)"
                                    ScheduleManager.TaskType.BROADCAST -> "广播消息"
                                    ScheduleManager.TaskType.STOP -> "停止前广播消息 (可选)"
                                    ScheduleManager.TaskType.WORLD_SAVE -> "（自动执行 save-all，无需配置）"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 1,
                        maxLines = 3,
                        enabled = taskType != ScheduleManager.TaskType.WORLD_SAVE
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskName.isNotBlank() && ScheduleManager.validateCron(taskCron)) {
                            val editTask = editingTask
                            if (editTask != null) {
                                ScheduleManager.updateTask(editTask.id) {
                                    it.copy(
                                        name = taskName,
                                        type = taskType,
                                        cronExpression = taskCron,
                                        payload = taskPayload
                                    )
                                }
                            } else {
                                ScheduleManager.addTask(
                                    ScheduleManager.ScheduleTask(
                                        name = taskName,
                                        type = taskType,
                                        cronExpression = taskCron,
                                        payload = taskPayload
                                    )
                                )
                            }
                            refreshTasks()
                            showAddDialog = false
                        }
                    },
                    enabled = taskName.isNotBlank() && ScheduleManager.validateCron(taskCron)
                ) {
                    Text(if (editingTask != null) "保存" else "添加")
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
    onRunNow: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                        ScheduleManager.TaskType.STOP -> Icons.Filled.Stop
                        ScheduleManager.TaskType.WORLD_SAVE -> Icons.Filled.Save
                    },
                    null,
                    tint = if (task.enabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            task.name.ifBlank { "未命名任务" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (task.enabled) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        if (task.enabled) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    "运行中",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        "${task.type.label} · ${ScheduleManager.describeCron(task.cronExpression)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.payload.isNotBlank()) {
                        Text(
                            task.payload.take(60),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Switch(
                    checked = task.enabled,
                    onCheckedChange = { onToggle() }
                )
            }

            Spacer(Modifier.height(8.dp))

            // 运行统计和操作按钮
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "上次: ${ScheduleManager.formatLastRun(task.lastRun)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.enabled && task.nextRun > 0) {
                        Text(
                            "下次: ${ScheduleManager.formatNextRun(task.nextRun)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "执行 ${task.runCount} 次",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    IconButton(
                        onClick = onRunNow,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, "立即执行",
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Filled.Edit, "编辑",
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(28.dp)
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
            text = { Text("确定要删除定时任务「${task.name.ifBlank { "未命名" }}」吗？\n此操作不可撤销。") },
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
