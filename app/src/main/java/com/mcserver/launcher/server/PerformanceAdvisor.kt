package com.mcserver.launcher.server

/**
 * 服务器性能优化顾问。
 * 借鉴 Pterodactyl 的 Egg 配置优化和 MCSManager 的实例调优系统。
 *
 * 基于实时性能监控数据，自动给出优化建议：
 * - TPS/MSPT 分析与优化
 * - 内存使用分析
 * - JVM 参数调优建议
 * - 服务器配置优化（view-distance、entity activation range 等）
 * - 模组/插件优化建议
 */
object PerformanceAdvisor {

    data class PerformanceSnapshot(
        val tps: Double = 20.0,
        val mspt: Double = 0.0,
        val memoryUsedMB: Long = 0,
        val memoryMaxMB: Long = 0,
        val cpuPercent: Double = 0.0,
        val playerCount: Int = 0,
        val threadCount: Int = 0,
        val uptimeMinutes: Long = 0,
        val serverType: String = "",
        val mcVersion: String = "",
        val jvmArgs: String = "",
        val allocatedMemoryMB: Long = 0
    )

    enum class Severity(val label: String, val color: Long) {
        OK("正常", 0xFF4CAF50),
        WARNING("警告", 0xFFFF9800),
        CRITICAL("严重", 0xFFF44336),
        INFO("建议", 0xFF2196F3)
    }

    data class Advice(
        val title: String,
        val description: String,
        val severity: Severity,
        val category: Category,
        val actionable: Boolean = false,
        val suggestedAction: String = ""
    )

    enum class Category(val label: String) {
        TPS("TPS 优化"),
        MEMORY("内存优化"),
        CPU("CPU 优化"),
        JVM("JVM 参数"),
        CONFIG("服务器配置"),
        PLUGIN("插件/模组"),
        GENERAL("综合建议")
    }

    /**
     * 分析性能数据并生成建议列表
     */
    fun analyze(snapshot: PerformanceSnapshot): List<Advice> {
        val adviceList = mutableListOf<Advice>()

        // TPS 分析
        analyzeTps(snapshot, adviceList)

        // 内存分析
        analyzeMemory(snapshot, adviceList)

        // CPU 分析
        analyzeCpu(snapshot, adviceList)

        // JVM 分析
        analyzeJvm(snapshot, adviceList)

        // 综合建议
        analyzeGeneral(snapshot, adviceList)

        return adviceList.sortedByDescending { it.severity.ordinal }
    }

    private fun analyzeTps(snapshot: PerformanceSnapshot, advice: MutableList<Advice>) {
        val tps = snapshot.tps

        when {
            tps <= 5 -> {
                advice.add(Advice(
                    "TPS 极低 ($tps)",
                    "服务器 TPS 低于 5，游戏体验严重受影响，延迟和卡顿明显。",
                    Severity.CRITICAL, Category.TPS, true,
                    "立即执行：1) 减少视距 (view-distance=6)；2) 检查是否有卡顿实体/区块；3) 使用 /tps 或 spark 分析卡顿源"
                ))
            }
            tps <= 10 -> {
                advice.add(Advice(
                    "TPS 偏低 ($tps)",
                    "服务器 TPS 在 $tps 左右，可能存在性能瓶颈。",
                    Severity.WARNING, Category.TPS, true,
                    "建议：1) 降低 view-distance 到 8-10；2) 减少实体数量 (mob-spawn-range)；3) 使用 timings 报告分析"
                ))
            }
            tps <= 15 -> {
                advice.add(Advice(
                    "TPS 略低 ($tps)",
                    "TPS 略低于理想值 20，可能有轻微性能问题。",
                    Severity.INFO, Category.TPS, true,
                    "可以：1) 优化 entity activation range；2) 清理掉落物 (item-despawn-rate)"
                ))
            }
            tps >= 19.5 -> {
                advice.add(Advice(
                    "TPS 正常 ($tps)",
                    "服务器 TPS 接近满值 20，运行状态良好。",
                    Severity.OK, Category.TPS
                ))
            }
        }

        // MSPT 分析
        val mspt = snapshot.mspt
        if (mspt > 0) {
            when {
                mspt > 45 -> advice.add(Advice(
                    "MSPT 过高 (${mspt}ms)",
                    "每个 tick 平均耗时超过 45ms（50ms 为上限），服务器即将无法维持 20 TPS。",
                    Severity.CRITICAL, Category.TPS, true,
                    "紧急处理：使用 spark profiler 或 timings 查看是什么导致高 MSPT"
                ))
                mspt > 35 -> advice.add(Advice(
                    "MSPT 偏高 (${mspt}ms)",
                    "MSPT 超过 35ms，TPS 可能开始下降。",
                    Severity.WARNING, Category.TPS, true,
                    "检查：使用 /spark healthreport 或 /timings report 分析"
                ))
            }
        }
    }

    private fun analyzeMemory(snapshot: PerformanceSnapshot, advice: MutableList<Advice>) {
        val usedMB = snapshot.memoryUsedMB
        val maxMB = snapshot.memoryMaxMB
        val allocatedMB = snapshot.allocatedMemoryMB

        if (maxMB <= 0) return

        val usagePercent = (usedMB.toDouble() / maxMB.toDouble() * 100)

        when {
            usagePercent >= 95 -> {
                advice.add(Advice(
                    "内存使用率极高 (${usagePercent.toInt()}%)",
                    "已使用 ${usedMB}MB / ${maxMB}MB，接近上限，随时可能 OOM 崩溃。",
                    Severity.CRITICAL, Category.MEMORY, true,
                    "立即：1) 在设置中增加内存分配；2) 减少视距和实体数量；3) 考虑使用优化模组 (Lithium/FerriteCore)"
                ))
            }
            usagePercent >= 85 -> {
                advice.add(Advice(
                    "内存使用率偏高 (${usagePercent.toInt()}%)",
                    "已使用 ${usedMB}MB / ${maxMB}MB，内存压力较大。",
                    Severity.WARNING, Category.MEMORY, true,
                    "建议：1) 考虑增加内存分配；2) 使用 Aikar's Flags 优化 GC；3) 定期重启释放内存"
                ))
            }
            usagePercent <= 30 && allocatedMB >= 2048 -> {
                advice.add(Advice(
                    "内存利用率低 (${usagePercent.toInt()}%)",
                    "分配了 ${allocatedMB}MB 但仅使用 ${usagePercent.toInt()}%，可以适当减少分配以释放系统资源。",
                    Severity.INFO, Category.MEMORY, true,
                    "建议：可将内存分配降低到 ${(usedMB * 1.5).toLong()}MB 左右，留出余量即可"
                ))
            }
            usagePercent <= 70 -> {
                advice.add(Advice(
                    "内存使用正常 (${usagePercent.toInt()}%)",
                    "内存使用率 ${usagePercent.toInt()}%，在安全范围内。",
                    Severity.OK, Category.MEMORY
                ))
            }
        }

        // GC 建议
        if (usagePercent > 70 && allocatedMB >= 4096) {
            advice.add(Advice(
                "大内存 GC 优化",
                "分配内存超过 4GB 时建议使用 G1GC 或 ZGC 以减少 GC 暂停时间。",
                Severity.INFO, Category.JVM, true,
                "建议 JVM 参数：-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=8M"
            ))
        }
    }

    private fun analyzeCpu(snapshot: PerformanceSnapshot, advice: MutableList<Advice>) {
        val cpu = snapshot.cpuPercent
        val threads = snapshot.threadCount

        when {
            cpu > 90 -> {
                advice.add(Advice(
                    "CPU 使用率极高 (${cpu.toInt()}%)",
                    "CPU 接近满载，服务器性能可能严重受限。",
                    Severity.CRITICAL, Category.CPU, true,
                    "建议：1) 降低视距；2) 减少实体数量；3) 关闭不必要的区块加载；4) 使用 spark 检查 CPU 热点"
                ))
            }
            cpu > 70 -> {
                advice.add(Advice(
                    "CPU 使用率偏高 (${cpu.toInt()}%)",
                    "CPU 使用率较高，可能影响性能。",
                    Severity.WARNING, Category.CPU, true,
                    "建议：1) 降低 simulation-distance；2) 优化红石和实体"
                ))
            }
            cpu <= 50 -> {
                advice.add(Advice(
                    "CPU 使用率正常 (${cpu.toInt()}%)",
                    "CPU 负载在健康范围内。",
                    Severity.OK, Category.CPU
                ))
            }
        }

        // 线程数分析
        if (threads > 100) {
            advice.add(Advice(
                "线程数较多 ($threads)",
                "服务器创建了 $threads 个线程，可能导致上下文切换开销增加。",
                Severity.WARNING, Category.CPU, true,
                "检查是否有模组/插件创建了过多线程"
            ))
        }
    }

    private fun analyzeJvm(snapshot: PerformanceSnapshot, advice: MutableList<Advice>) {
        val jvmArgs = snapshot.jvmArgs

        if (jvmArgs.isBlank()) return

        // 检查是否使用了优化的 GC
        if (!jvmArgs.contains("G1GC") && !jvmArgs.contains("ZGC") && !jvmArgs.contains("Shenandoah")) {
            advice.add(Advice(
                "未使用优化 GC",
                "当前 JVM 参数未配置专门的垃圾回收器，建议使用 G1GC。",
                Severity.INFO, Category.JVM, true,
                "建议添加：-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200"
            ))
        }

        // 检查是否有 GC 日志
        if (!jvmArgs.contains("Xlog:gc")) {
            advice.add(Advice(
                "未启用 GC 日志",
                "启用 GC 日志有助于排查内存问题。",
                Severity.INFO, Category.JVM, true,
                "建议添加：-Xlog:gc*:gc.log:time,level,tags"
            ))
        }

        // Aikar's Flags 检查
        if (jvmArgs.contains("Aikar") || jvmArgs.contains("aikar")) {
            advice.add(Advice(
                "已使用 Aikar's Flags",
                "检测到 Aikar's Flags 配置，这是推荐的 JVM 参数方案。",
                Severity.OK, Category.JVM
            ))
        }

        // 检查堆内存比例
        val xms = Regex("-Xms(\\d+)([MG])").find(jvmArgs)
        val xmx = Regex("-Xmx(\\d+)([MG])").find(jvmArgs)
        if (xms != null && xmx != null) {
            val minMem = xms.groupValues[1].toIntOrNull() ?: 0
            val maxMem = xmx.groupValues[1].toIntOrNull() ?: 0
            if (minMem != maxMem) {
                advice.add(Advice(
                    "堆内存最小/最大值不一致",
                    "-Xms${xms.groupValues[1]}${xms.groupValues[2]} ≠ -Xmx${xmx.groupValues[1]}${xmx.groupValues[2]}，建议设为相同值以避免堆扩容开销。",
                    Severity.INFO, Category.JVM, true,
                    "建议：将 -Xms 和 -Xmx 设为相同的值"
                ))
            }
        }
    }

    private fun analyzeGeneral(snapshot: PerformanceSnapshot, advice: MutableList<Advice>) {
        val playerCount = snapshot.playerCount
        val uptime = snapshot.uptimeMinutes

        // 玩家数量建议
        if (playerCount > 20) {
            advice.add(Advice(
                "玩家数量较多 ($playerCount 人)",
                "当前有 $playerCount 名玩家在线，建议优化网络和区块加载。",
                Severity.INFO, Category.GENERAL, true,
                "建议：1) 降低 view-distance；2) 使用 network-compression-threshold；3) 考虑使用 Velocity/BungeeCord 代理"
            ))
        }

        // 运行时间建议
        if (uptime > 1440) { // 超过 24 小时
            advice.add(Advice(
                "服务器已运行 ${uptime / 60} 小时",
                "长时间运行可能导致内存碎片化和性能下降。",
                Severity.INFO, Category.GENERAL, true,
                "建议：设置定时重启任务，每天凌晨自动重启服务器"
            ))
        }

        // Paper/Purpur 特定优化建议
        val serverType = snapshot.serverType.lowercase()
        if (serverType.contains("paper") || serverType.contains("purpur")) {
            advice.add(Advice(
                "Paper/Purpur 优化建议",
                "使用 Paper 或 Purpur 服务端可以配置以下性能参数：\n" +
                "- paper.yml: 调整 per-player-mob-spawns, max-entity-collisions\n" +
                "- purpur.yml: 调整 entity-activation-range, use-alternate-keepalive\n" +
                "- spigot.yml: 调整 item-despawn-rate, entity-activation-range",
                Severity.INFO, Category.CONFIG, true,
                "在文件管理页面编辑 paper.yml / purpur.yml / spigot.yml"
            ))
        }

        // 综合评分
        val healthScore = calculateHealthScore(snapshot)
        if (healthScore >= 90) {
            advice.add(Advice(
                "服务器运行状况良好",
                "综合评分: $healthScore/100，各项指标正常。",
                Severity.OK, Category.GENERAL
            ))
        } else if (healthScore >= 60) {
            advice.add(Advice(
                "服务器运行状况一般",
                "综合评分: $healthScore/100，建议关注上述警告项。",
                Severity.WARNING, Category.GENERAL
            ))
        } else {
            advice.add(Advice(
                "服务器需要优化",
                "综合评分: $healthScore/100，存在多个性能问题需要处理。",
                Severity.CRITICAL, Category.GENERAL, true,
                "请按照上述建议逐项优化服务器配置"
            ))
        }
    }

    /**
     * 计算综合健康评分 (0-100)
     */
    private fun calculateHealthScore(snapshot: PerformanceSnapshot): Int {
        var score = 100

        // TPS 扣分
        when {
            snapshot.tps <= 5 -> score -= 40
            snapshot.tps <= 10 -> score -= 25
            snapshot.tps <= 15 -> score -= 15
            snapshot.tps <= 18 -> score -= 5
        }

        // 内存使用扣分
        if (snapshot.memoryMaxMB > 0) {
            val usagePercent = snapshot.memoryUsedMB.toDouble() / snapshot.memoryMaxMB * 100
            when {
                usagePercent >= 95 -> score -= 30
                usagePercent >= 85 -> score -= 15
                usagePercent >= 75 -> score -= 5
            }
        }

        // CPU 扣分
        when {
            snapshot.cpuPercent > 90 -> score -= 20
            snapshot.cpuPercent > 70 -> score -= 10
            snapshot.cpuPercent > 50 -> score -= 3
        }

        // MSPT 扣分
        when {
            snapshot.mspt > 45 -> score -= 20
            snapshot.mspt > 35 -> score -= 10
            snapshot.mspt > 25 -> score -= 3
        }

        return score.coerceIn(0, 100)
    }
}
