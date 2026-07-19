package com.mcserver.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mcserver.launcher.data.ServerConfig
import com.mcserver.launcher.server.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    config: ServerConfig,
    onNavigateToCrashReports: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val serverManager = ServerManager.instance
    val serverStatus by serverManager.serverStatus.collectAsState()
    val metrics by PerformanceMonitor.instance.metrics.collectAsState()

    var healthResult by remember { mutableStateOf<HealthChecker.HealthResult?>(null) }
    var performanceAdvice by remember { mutableStateOf<List<PerformanceAdvisor.Advice>>(emptyList()) }
    var crashCount by remember { mutableIntStateOf(0) }
    var latestCrash by remember { mutableStateOf<CrashAnalyzer.CrashReport?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedReport by remember { mutableStateOf("") }

    fun runDiagnostics() {
        scope.launch {
            isChecking = true
            // 运行健康检查
            healthResult = HealthChecker.runAllChecks(config)
            // 性能分析
            performanceAdvice = PerformanceAdvisor.analyze(
                PerformanceAdvisor.PerformanceSnapshot(
                    tps = metrics.tps.toDouble(),
                    mspt = metrics.mspt.toDouble(),
                    memoryUsedMB = metrics.memoryUsedMB,
                    memoryMaxMB = config.maxRamMB.toLong(),
                    cpuPercent = metrics.cpuPercent.toDouble(),
                    playerCount = serverStatus.playerCount,
                    threadCount = metrics.threadCount,
                    uptimeMinutes = (serverStatus.uptimeSeconds / 60),
                    serverType = "",
                    mcVersion = "",
                    jvmArgs = config.additionalArgs,
                    allocatedMemoryMB = config.allocatedMemoryMB.toLong()
                )
            )
            // 崩溃报告
            crashCount = CrashAnalyzer.getCrashReportCount()
            latestCrash = CrashAnalyzer.getLatestCrashReport()
            isChecking = false
        }
    }

    LaunchedEffect(Unit) {
        runDiagnostics()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("诊断报告") },
            actions = {
                IconButton(onClick = { runDiagnostics() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "刷新诊断")
                }
                IconButton(onClick = {
                    scope.launch {
                        try {
                            val file = HealthChecker.exportDiagnosticReport(config)
                            exportedReport = "报告已导出到: ${file.absolutePath}"
                        } catch (e: Exception) {
                            exportedReport = "导出失败: ${e.message}"
                        }
                        showExportDialog = true
                    }
                }) {
                    Icon(Icons.Filled.Share, contentDescription = "导出报告")
                }
            }
        )

        if (isChecking) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("正在运行诊断...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ---- 总体评分 ----
                item {
                    val result = healthResult
                    OverallHealthCard(
                        healthResult = result,
                        performanceAdvice = performanceAdvice,
                        crashCount = crashCount
                    )
                }

                // ---- 崩溃报告入口 ----
                if (crashCount > 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.BugReport, null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "发现 $crashCount 个崩溃报告",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    latestCrash?.let { crash ->
                                        Text(
                                            "最近: ${crash.crashTimeStr.take(19)} · ${crash.suspectedCause.label}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(onClick = onNavigateToCrashReports) {
                                    Text("查看")
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // ---- 健康检查结果 ----
                val checks = healthResult?.checks
                if (checks != null && checks.isNotEmpty()) {
                    item {
                        Text(
                            "健康检查",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    items(checks) { check ->
                        HealthCheckItem(check = check)
                    }
                }

                // ---- 性能优化建议 ----
                if (performanceAdvice.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "性能分析",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    items(performanceAdvice) { advice ->
                        AdviceCard(advice = advice)
                    }
                }

                // ---- 服务器基本信息 ----
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "服务器信息",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            InfoRow("服务端核心", "Minecraft Server")
                            InfoRow("分配内存", "${config.allocatedMemoryMB} MB")
                            InfoRow("Java 路径", config.javaPath.ifBlank { "未设置" })
                            InfoRow("JAR 路径", config.jarPath.ifBlank { "未设置" })
                            InfoRow("服务器端口", "${config.serverPort}")
                            InfoRow("RCON", if (config.rconEnabled) "已启用:${config.rconPort}" else "已禁用")
                            InfoRow("自动重启", if (config.autoRestart) "已启用" else "已禁用")
                            InfoRow("JVM 参数", config.additionalArgs.ifBlank { "默认" })
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    // 导出报告对话框
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            icon = { Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("报告已导出") },
            text = {
                Text(
                    exportedReport.ifBlank { "诊断报告已生成。" },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("确定") }
            }
        )
    }
}

@Composable
private fun OverallHealthCard(
    healthResult: HealthChecker.HealthResult?,
    performanceAdvice: List<PerformanceAdvisor.Advice>,
    crashCount: Int
) {
    val checks = healthResult?.checks ?: emptyList()
    val passedCount = checks.count { it.passed }
    val totalChecks = checks.size
    val criticalCount = performanceAdvice.count { it.severity == PerformanceAdvisor.Severity.CRITICAL }
    val warningCount = performanceAdvice.count { it.severity == PerformanceAdvisor.Severity.WARNING }

    val (score, color) = when {
        crashCount > 0 && criticalCount > 0 -> 20 to MaterialTheme.colorScheme.error
        criticalCount > 0 -> 35 to MaterialTheme.colorScheme.error
        warningCount > 0 -> 55 to Color(0xFFFF9800)
        totalChecks > 0 && passedCount == totalChecks -> 95 to Color(0xFF4CAF50)
        totalChecks > 0 -> 70 to Color(0xFF2196F3)
        else -> 50 to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$score",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "服务器健康评分",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    when {
                        score >= 90 -> "运行状况优秀"
                        score >= 70 -> "运行状况良好"
                        score >= 50 -> "存在一些警告"
                        else -> "需要立即关注"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (totalChecks > 0) {
                    Text(
                        "健康检查: $passedCount/$totalChecks 通过",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthCheckItem(check: HealthChecker.HealthCheck) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (check.passed)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (check.passed) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                null,
                tint = if (check.passed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    check.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (check.message.isNotBlank()) {
                    Text(
                        check.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (check.detail.isNotBlank()) {
                    Text(
                        check.detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = if (check.passed)
                    Color(0xFF4CAF50).copy(alpha = 0.15f)
                else
                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
            ) {
                Text(
                    if (check.passed) "通过" else "失败",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (check.passed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun AdviceCard(advice: PerformanceAdvisor.Advice) {
    val color = when (advice.severity) {
        PerformanceAdvisor.Severity.OK -> Color(0xFF4CAF50)
        PerformanceAdvisor.Severity.INFO -> Color(0xFF2196F3)
        PerformanceAdvisor.Severity.WARNING -> Color(0xFFFF9800)
        PerformanceAdvisor.Severity.CRITICAL -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = color.copy(alpha = 0.15f)
                ) {
                    Text(
                        advice.severity.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    advice.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                advice.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (advice.actionable && advice.suggestedAction.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        advice.suggestedAction,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
