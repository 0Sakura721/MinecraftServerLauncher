package com.mcserver.launcher.server

import android.content.Context
import com.mcserver.launcher.McApplication
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 定时任务管理器。
 * 借鉴 Pterodactyl Schedules 和 MCSManager 的定时任务系统。
 *
 * 支持的任务类型：
 * - COMMAND: 执行控制台命令
 * - BACKUP: 创建服务器备份
 * - RESTART: 重启服务器
 * - BROADCAST: 广播消息（通过 say 命令）
 *
 * 调度方式：简化 Cron 表达式（分钟级精度）
 * 格式: "minute hour day month weekday"（每个字段可用 * 或数字）
 */
object ScheduleManager {

    private val context: Context get() = McApplication.instance
    private val storageFile: File get() = File(context.filesDir, "schedules.json")
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var schedulerJob: Job? = null

    enum class TaskType(val label: String) {
        COMMAND("执行命令"),
        BACKUP("创建备份"),
        RESTART("重启服务器"),
        BROADCAST("广播消息");

        companion object {
            fun fromName(name: String): TaskType =
                entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: COMMAND
        }
    }

    data class ScheduleTask(
        val id: String = java.util.UUID.randomUUID().toString().take(8),
        val name: String = "",
        val type: TaskType = TaskType.COMMAND,
        val cronExpression: String = "0 * * * *",  // 默认每小时
        val payload: String = "",  // 命令内容 / 备份标签 / 广播消息
        val enabled: Boolean = true,
        val lastRun: Long = 0,
        val nextRun: Long = 0,
        val runCount: Int = 0
    )

    private val _tasks = mutableListOf<ScheduleTask>()
    val tasks: List<ScheduleTask> get() = _tasks.toList()

    /** 加载持久化的定时任务 */
    fun load() {
        try {
            if (!storageFile.exists()) return
            val json = JSONArray(storageFile.readText())
            _tasks.clear()
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                _tasks.add(ScheduleTask(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString().take(8)),
                    name = obj.optString("name", ""),
                    type = TaskType.fromName(obj.optString("type", "COMMAND")),
                    cronExpression = obj.optString("cron", "0 * * * *"),
                    payload = obj.optString("payload", ""),
                    enabled = obj.optBoolean("enabled", true),
                    lastRun = obj.optLong("lastRun", 0),
                    nextRun = obj.optLong("nextRun", 0),
                    runCount = obj.optInt("runCount", 0)
                ))
            }
        } catch (_: Exception) {
            _tasks.clear()
        }
    }

    /** 保存定时任务到文件 */
    private fun save() {
        try {
            val json = JSONArray()
            _tasks.forEach { task ->
                json.put(JSONObject().apply {
                    put("id", task.id)
                    put("name", task.name)
                    put("type", task.type.name)
                    put("cron", task.cronExpression)
                    put("payload", task.payload)
                    put("enabled", task.enabled)
                    put("lastRun", task.lastRun)
                    put("nextRun", task.nextRun)
                    put("runCount", task.runCount)
                })
            }
            storageFile.writeText(json.toString(2))
        } catch (_: Exception) {}
    }

    /** 添加一个定时任务 */
    fun addTask(task: ScheduleTask): ScheduleTask {
        val newTask = task.copy(id = java.util.UUID.randomUUID().toString().take(8))
        _tasks.add(newTask)
        save()
        recalculateNextRuns()
        return newTask
    }

    /** 更新定时任务 */
    fun updateTask(taskId: String, transform: (ScheduleTask) -> ScheduleTask) {
        val index = _tasks.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            _tasks[index] = transform(_tasks[index])
            save()
            recalculateNextRuns()
        }
    }

    /** 删除定时任务 */
    fun deleteTask(taskId: String) {
        _tasks.removeAll { it.id == taskId }
        save()
    }

    /** 启用/禁用定时任务 */
    fun toggleTask(taskId: String) {
        updateTask(taskId) { it.copy(enabled = !it.enabled) }
    }

    /** 启动调度器 */
    fun startScheduler() {
        load()
        schedulerJob?.cancel()
        schedulerJob = scope.launch {
            while (isActive) {
                try {
                    checkAndExecuteTasks()
                } catch (_: Exception) {}
                delay(30000) // 每 30 秒检查一次
            }
        }
    }

    /** 停止调度器 */
    fun stopScheduler() {
        schedulerJob?.cancel()
        schedulerJob = null
    }

    /** 检查并执行到期任务 */
    private suspend fun checkAndExecuteTasks() {
        val now = System.currentTimeMillis()
        val currentMinute = SimpleDateFormat("mm HH dd MM E", Locale.US).format(Date())
            .split(" ").map { it.toIntOrNull() ?: 0 }

        for (task in _tasks) {
            if (!task.enabled) continue
            if (!cronMatches(task.cronExpression, currentMinute)) continue
            // 防止同一分钟内重复执行
            if (task.lastRun > 0 && (now - task.lastRun) < 60000) continue

            // 执行任务
            executeTask(task)
            updateTask(task.id) {
                it.copy(lastRun = now, runCount = it.runCount + 1)
            }
        }
    }

    /** 执行单个任务 */
    private fun executeTask(task: ScheduleTask) {
        val serverManager = ServerManager.instance
        val termux = TermuxManager()

        try {
            when (task.type) {
                TaskType.COMMAND -> {
                    if (task.payload.isNotBlank()) {
                        termux.sendCommand(task.payload)
                    }
                }
                TaskType.BACKUP -> {
                    if (serverManager.isRunning) {
                        termux.notifyConsole("> [定时任务] 创建自动备份: ${task.name}")
                        CoroutineScope(Dispatchers.IO).launch {
                            BackupManager.createBackup("auto_${task.id.take(4)}").onSuccess {
                                termux.notifyConsole("> [定时任务] 备份完成: $it")
                            }.onFailure {
                                termux.notifyConsole("> [定时任务] 备份失败: ${it.message}")
                            }
                        }
                    } else {
                        CoroutineScope(Dispatchers.IO).launch {
                            BackupManager.createBackup("auto_${task.id.take(4)}")
                        }
                    }
                }
                TaskType.RESTART -> {
                    if (serverManager.isRunning) {
                        termux.notifyConsole("> [定时任务] 执行定时重启: ${task.name}")
                        // 先广播警告，再重启
                        if (task.payload.isNotBlank()) {
                            termux.sendCommand("say ${task.payload}")
                        }
                        termux.sendCommand("say [定时任务] 服务器将在 30 秒后重启")
                        // 延迟 30 秒后重启
                        CoroutineScope(Dispatchers.IO).launch {
                            delay(30000)
                            serverManager.stopServer()
                            delay(5000)
                            // 重启需要配置信息，这里简化处理：仅停止
                            termux.notifyConsole("> [定时任务] 服务器已停止，如需自动启动请开启自动重启")
                        }
                    }
                }
                TaskType.BROADCAST -> {
                    if (task.payload.isNotBlank() && serverManager.isRunning) {
                        termux.sendCommand("say [定时] ${task.payload}")
                    }
                }
            }
        } catch (e: Exception) {
            termux.notifyConsole("> [定时任务] 执行失败: ${task.name} - ${e.message}")
        }
    }

    /** 重新计算所有任务的下次执行时间 */
    private fun recalculateNextRuns() {
        val now = System.currentTimeMillis()
        _tasks.forEachIndexed { index, task ->
            if (task.enabled) {
                _tasks[index] = task.copy(nextRun = calculateNextRun(task.cronExpression, now))
            }
        }
        save()
    }

    /** 计算 cron 表达式的下次执行时间（简化实现） */
    private fun calculateNextRun(cron: String, from: Long): Long {
        // 简化：返回下一个整分钟
        val now = from
        val minuteMs = 60000L
        return now + minuteMs - (now % minuteMs)
    }

    /**
     * 检查 cron 表达式是否匹配当前时间。
     * 简化实现：只支持数字和 *，5 字段格式（分 时 日 月 周）
     */
    private fun cronMatches(cron: String, current: List<Int>): Boolean {
        try {
            val parts = cron.trim().split("\\s+".toRegex())
            if (parts.size < 5) return false
            // current: [minute, hour, day, month, weekday]
            for (i in 0 until 5) {
                val field = parts[i].trim()
                if (field == "*") continue
                if (field == current[i].toString()) continue
                return false
            }
            return true
        } catch (_: Exception) { return false }
    }

    /** 格式化最后运行时间 */
    fun formatLastRun(timestamp: Long): String {
        if (timestamp <= 0) return "从未执行"
        return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    /** 获取 cron 表达式的可读描述 */
    fun describeCron(cron: String): String {
        val parts = cron.trim().split("\\s+".toRegex())
        if (parts.size < 5) return cron
        val minute = parts[0]
        val hour = parts[1]
        return when {
            minute == "*" && hour == "*" -> "每分钟"
            minute != "*" && hour == "*" -> "每小时第 ${minute} 分"
            hour != "*" && minute != "*" -> "每天 $hour:${minute.padStart(2, '0')}"
            else -> cron
        }
    }
}
