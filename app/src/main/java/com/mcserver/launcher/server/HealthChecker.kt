package com.mcserver.launcher.server

import android.content.Context
import com.mcserver.launcher.McApplication
import com.mcserver.launcher.data.ServerConfig
import java.io.File
import java.io.RandomAccessFile
import java.net.InetSocketAddress
import java.net.Socket
import java.util.jar.JarFile

/**
 * 服务器启动前健康检查 / 诊断工具。
 * 借鉴 Pterodactyl 和 MCSManager 的启动前验证逻辑。
 */
object HealthChecker {

    private val context: Context get() = McApplication.instance
    private val serverDir: File get() = TermuxManager.serverDir(context)

    data class HealthResult(
        val passed: Boolean,
        val checks: List<HealthCheck>,
        val warnings: List<String> = emptyList()
    )

    data class HealthCheck(
        val name: String,
        val passed: Boolean,
        val message: String,
        val severity: Severity = Severity.ERROR
    )

    enum class Severity { INFO, WARNING, ERROR }

    /**
     * 执行完整的启动前健康检查。
     */
    fun runAllChecks(config: ServerConfig): HealthResult {
        val checks = mutableListOf<HealthCheck>()
        val warnings = mutableListOf<String>()

        // 1. JAR 文件检查
        checks.add(checkJarFile(config))

        // 2. 端口占用检查
        checks.add(checkPortAvailable(config.serverPort))

        // 3. 磁盘空间检查
        checks.add(checkDiskSpace())

        // 4. 内存分配检查
        checks.add(checkMemoryAllocation(config))

        // 5. EULA 检查
        checks.add(checkEula())

        // 6. Java 版本兼容性（尝试从 JAR 推断）
        val javaCheck = checkJavaCompatibility(config)
        if (javaCheck.severity == Severity.WARNING) warnings.add(javaCheck.message)
        checks.add(javaCheck)

        val allPassed = checks.none { !it.passed && it.severity == Severity.ERROR }

        return HealthResult(
            passed = allPassed,
            checks = checks,
            warnings = warnings
        )
    }

    private fun checkJarFile(config: ServerConfig): HealthCheck {
        if (config.jarPath.isBlank()) {
            return HealthCheck("JAR 文件", false, "未选择服务器 JAR 文件，请先在配置页选择或下载核心")
        }

        val jarFile = File(config.jarPath)
        if (!jarFile.exists()) {
            // 尝试在服务器目录查找
            val name = jarFile.name
            val inServerDir = File(serverDir, name)
            if (inServerDir.exists() && inServerDir.isFile) {
                return checkJarIntegrity(inServerDir)
            }
            return HealthCheck("JAR 文件", false, "JAR 文件不存在：${config.jarPath}")
        }

        return checkJarIntegrity(jarFile)
    }

    private fun checkJarIntegrity(jarFile: File): HealthCheck {
        return try {
            JarFile(jarFile).use { jar ->
                val manifest = jar.manifest
                val mainClass = manifest?.mainAttributes?.getValue("Main-Class")
                if (mainClass != null) {
                    HealthCheck("JAR 完整性", true, "JAR 有效，主类: $mainClass", Severity.INFO)
                } else {
                    // 很多 Minecraft 服务器 JAR 用不同的 manifest 结构
                    val hasClasses = jar.entries().asSequence().any {
                        it.name.endsWith(".class") && !it.isDirectory
                    }
                    if (hasClasses) {
                        HealthCheck("JAR 完整性", true, "JAR 有效 (${jarFile.name})", Severity.INFO)
                    } else {
                        HealthCheck("JAR 完整性", false, "JAR 文件似乎不包含 Java 类，可能已损坏")
                    }
                }
            }
        } catch (e: Exception) {
            HealthCheck("JAR 完整性", false, "JAR 文件无法读取，可能已损坏：${e.message}")
        }
    }

    private fun checkPortAvailable(port: Int): HealthCheck {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress("127.0.0.1", port), 2000)
            socket.close()
            HealthCheck("端口 $port", false, "端口 $port 已被占用，请更换端口或关闭占用程序")
        } catch (e: Exception) {
            HealthCheck("端口 $port", true, "端口 $port 可用", Severity.INFO)
        }
    }

    private fun checkDiskSpace(): HealthCheck {
        val freeSpace = serverDir.freeSpace
        val freeMB = freeSpace / (1024 * 1024)
        return when {
            freeMB < 100 -> HealthCheck("磁盘空间", false, "可用空间仅 ${freeMB}MB，可能不足以运行服务器（建议至少 500MB）")
            freeMB < 500 -> HealthCheck("磁盘空间", true, "可用空间 ${freeMB}MB，空间偏低", Severity.WARNING)
            else -> HealthCheck("磁盘空间", true, "可用空间 ${freeMB / 1024}GB", Severity.INFO)
        }
    }

    private fun checkMemoryAllocation(config: ServerConfig): HealthCheck {
        val runtime = Runtime.getRuntime()
        val deviceTotalMB = (runtime.maxMemory() / (1024 * 1024)).toInt()
        val requested = config.allocatedMemoryMB

        return when {
            requested > deviceTotalMB -> HealthCheck(
                "内存分配", false,
                "请求内存 ${requested}MB 超过 JVM 可用内存 ${deviceTotalMB}MB，请降低分配值"
            )
            requested > deviceTotalMB * 0.8 -> HealthCheck(
                "内存分配", true,
                "内存分配 ${requested}MB 偏高（可用 ${deviceTotalMB}MB），可能影响系统稳定性",
                Severity.WARNING
            )
            else -> HealthCheck("内存分配", true, "内存分配 ${requested}MB 合理", Severity.INFO)
        }
    }

    private fun checkEula(): HealthCheck {
        val eulaFile = File(serverDir, "eula.txt")
        if (!eulaFile.exists()) {
            return HealthCheck("EULA", true, "EULA 将在首次启动时自动接受", Severity.INFO)
        }
        val content = eulaFile.readText()
        return if (content.contains("eula=true")) {
            HealthCheck("EULA", true, "EULA 已接受", Severity.INFO)
        } else {
            HealthCheck("EULA", true, "EULA 将在启动时自动更新为 true", Severity.WARNING)
        }
    }

    /**
     * 尝试从 JAR 文件名和内部推断所需的 Java 版本。
     * 注意：这是一个启发式检查，不能 100% 准确。
     */
    private fun checkJavaCompatibility(config: ServerConfig): HealthCheck {
        if (config.jarPath.isBlank()) {
            return HealthCheck("Java 兼容性", true, "未选择 JAR，跳过检查", Severity.INFO)
        }

        val jarName = config.jarPath.lowercase()
        val requiredVersion = when {
            // Paper/Purpur 1.21+ 需要 Java 21+
            jarName.contains("1.21") || jarName.contains("1.22") || jarName.contains("1.23") -> 21
            // Paper/Purpur 1.18-1.20 需要 Java 17+
            jarName.contains("1.18") || jarName.contains("1.19") || jarName.contains("1.20") -> 17
            // 旧版本需要 Java 8+
            jarName.contains("1.17") -> 16
            jarName.contains("1.16") -> 8
            // 默认假设 Java 17+
            else -> 17
        }

        val currentJavaVersion = try {
            (System.getProperty("java.version") ?: "17").substringBefore(".").substringBefore("-").toIntOrNull() ?: 17
        } catch (_: Exception) { 17 }

        return if (currentJavaVersion >= requiredVersion) {
            HealthCheck("Java 兼容性", true, "Java $currentJavaVersion 满足最低要求 Java $requiredVersion", Severity.INFO)
        } else {
            HealthCheck(
                "Java 兼容性", true,
                "JAR 可能需要 Java $requiredVersion，当前为 Java $currentJavaVersion。如启动失败请在 Termux 中安装更高版本 Java",
                Severity.WARNING
            )
        }
    }
}
