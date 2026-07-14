package com.mcserver.launcher.server

import android.os.*
import com.mcserver.launcher.McApplication
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.RandomAccessFile

/**
 * 性能监控器 — 实时采集 CPU、内存、TPS 等指标。
 * 借鉴 Pterodactyl 的资源监控面板设计。
 */
class PerformanceMonitor private constructor() {

    companion object {
        val instance: PerformanceMonitor by lazy { PerformanceMonitor() }
    }

    data class Metrics(
        val cpuPercent: Float = 0f,
        val memoryUsedMB: Long = 0,
        val memoryTotalMB: Long = 0,
        val tps: Float = 20f,              // Ticks Per Second（默认 20）
        val mspt: Float = 50f,              // Milliseconds Per Tick
        val playerCount: Int = 0,
        val uptimeSeconds: Long = 0,
        val networkRxBytes: Long = 0,
        val networkTxBytes: Long = 0
    )

    private val _metrics = MutableStateFlow(Metrics())
    val metrics: StateFlow<Metrics> = _metrics.asStateFlow()

    private var monitorJob: Job? = null
    private var startTime = 0L

    private val context get() = McApplication.instance

    fun startMonitoring() {
        monitorJob?.cancel()
        startTime = System.currentTimeMillis()
        monitorJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                _metrics.value = collectMetrics()
                delay(2000)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        _metrics.value = Metrics()
    }

    private fun collectMetrics(): Metrics {
        val uptime = (System.currentTimeMillis() - startTime) / 1000
        val memInfo = collectMemoryInfo()
        val cpu = collectCpuUsage()
        val tpsMetrics = collectTpsEstimate()

        return Metrics(
            cpuPercent = cpu,
            memoryUsedMB = memInfo.first,
            memoryTotalMB = memInfo.second,
            tps = tpsMetrics.first,
            mspt = tpsMetrics.second,
            playerCount = ServerManager.instance.serverStatus.value.playerCount,
            uptimeSeconds = uptime
        )
    }

    private fun collectMemoryInfo(): Pair<Long, Long> {
        val runtime = Runtime.getRuntime()
        val usedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val totalMB = runtime.maxMemory() / (1024 * 1024)
        return usedMB to totalMB
    }

    /**
     * 采集系统 CPU 使用率（基于 /proc/stat 两次采样差值）。
     */
    private fun collectCpuUsage(): Float {
        return try {
            val stat1 = readProcStat()
            Thread.sleep(500)
            val stat2 = readProcStat()
            if (stat1 == null || stat2 == null) return 0f

            val total1 = stat1.total()
            val idle1 = stat1.idle
            val total2 = stat2.total()
            val idle2 = stat2.idle

            val totalDiff = total2 - total1
            val idleDiff = idle2 - idle1
            if (totalDiff == 0L) return 0f

            ((totalDiff - idleDiff).toFloat() / totalDiff * 100f).coerceIn(0f, 100f)
        } catch (_: Exception) { 0f }
    }

    private data class CpuStat(val user: Long, val nice: Long, val system: Long, val idle: Long, val iowait: Long, val irq: Long, val softirq: Long) {
        fun total(): Long = user + nice + system + idle + iowait + irq + softirq
    }

    private fun readProcStat(): CpuStat? {
        return try {
            val line = RandomAccessFile("/proc/stat", "r").use { it.readLine() }
            val parts = line.removePrefix("cpu ").trim().split("\\s+".toRegex())
            if (parts.size < 7) return null
            CpuStat(
                parts[0].toLong(), parts[1].toLong(), parts[2].toLong(),
                parts[3].toLong(), parts[4].toLong(), parts[5].toLong(), parts[6].toLong()
            )
        } catch (_: Exception) { null }
    }

    /**
     * TPS 估算 — 从最近的日志行分析 "Done" 耗时。
     * 这只是一个粗略估算，真实 TPS 需要从服务器内部获取。
     */
    private var lastTpsCheck = 0L
    private var estimatedTps = 20f
    private var estimatedMspt = 50f

    private fun collectTpsEstimate(): Pair<Float, Float> {
        // 在没有原生 TPS 数据的情况下，保持默认值
        // 后续可以从 /server health 命令或 Spark 插件获取
        return estimatedTps to estimatedMspt
    }

    /**
     * 当服务器日志输出中包含 TPS 相关信息时调用。
     * 支持 Spark 插件格式: "TPS from last 1m, 5m, 15m: 20.0, 20.0, 20.0"
     * 以及 Paper 内置格式: "Mean TPS: 20.0"
     */
    fun feedLogLine(line: String) {
        try {
            // Spark 插件 TPS 输出
            if (line.contains("TPS from last")) {
                val match = Regex("""(\d+\.\d+).*?(\d+\.\d+).*?(\d+\.\d+)""").find(line)
                match?.let {
                    estimatedTps = it.groupValues[1].toFloatOrNull() ?: estimatedTps
                }
            }
            // Paper 内置 MSPT
            if (line.contains("MSPT")) {
                val match = Regex("""MSPT.*?(\d+\.\d+)""").find(line)
                match?.let {
                    estimatedMspt = it.groupValues[1].toFloatOrNull() ?: estimatedMspt
                }
            }
        } catch (_: Exception) {}
    }
}
