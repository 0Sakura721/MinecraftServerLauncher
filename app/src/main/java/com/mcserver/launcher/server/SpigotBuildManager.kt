package com.mcserver.launcher.server

import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object SpigotBuildManager {

    data class SpigotVersion(val version: String, val isStable: Boolean = true)

    /**
     * 从 SpigotMC Hub 获取可用版本列表。
     * 解析 https://hub.spigotmc.org/versions/ 的 HTML 页面，提取版本号。
     * 失败时回退到已知版本列表。
     */
    suspend fun fetchVersions(): Result<List<SpigotVersion>> = withContext(Dispatchers.IO) {
        try {
            val conn = URL("https://hub.spigotmc.org/versions/").openConnection() as HttpURLConnection
            conn.connectTimeout = 10000; conn.readTimeout = 10000
            conn.setRequestProperty("User-Agent", "MCServerLauncher/1.0")
            conn.connect()
            val html = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            // 从HTML中提取版本号，格式如: <a href="1.21.json">1.21</a>
            val versions = mutableListOf<SpigotVersion>()
            val pattern = Regex("""<a\s+href="([^"]+\.json)">([^<]+)</a>""")
            pattern.findAll(html).forEach { match ->
                val fileName = match.groupValues[1]
                val version = match.groupValues[2].trim()
                if (fileName.endsWith(".json")) {
                    versions.add(SpigotVersion(version))
                }
            }
            Result.success(versions.sortedByDescending { it.version })
        } catch (e: Exception) {
            // 回退到已知版本列表
            val fallback = listOf(
                "1.21.5", "1.21.4", "1.21.3", "1.21.1", "1.21",
                "1.20.6", "1.20.4", "1.20.2", "1.20.1", "1.20",
                "1.19.4", "1.19.3", "1.19.2", "1.19.1", "1.19",
                "1.18.2", "1.18.1", "1.18", "1.17.1", "1.17",
                "1.16.5", "1.16.4", "1.16.3", "1.16.2", "1.16.1", "1.16"
            ).map { SpigotVersion(it) }
            Result.success(fallback)
        }
    }

    /**
     * 获取 Spigot 构建说明。
     * Spigot 需要通过 BuildTools 手动构建，这里提供指引和备用下载链接。
     */
    fun getBuildInstructions(mcVersion: String): String {
        return """
            |Spigot 需要通过 BuildTools 手动构建：
            |1. 在 PC 上下载 BuildTools.jar
            |2. 运行: java -jar BuildTools.jar --rev $mcVersion
            |3. 将生成的 spigot-$mcVersion.jar 传输到手机
            |
            |建议使用 Paper 或 Purpur（功能更完善，无需构建）
        """.trimMargin()
    }

    /**
     * 尝试从 GetBukkit 获取预构建的 CraftBukkit/Spigot JAR。
     * GetBukkit.org 提供一些预构建版本。
     */
    fun getDirectDownloadUrl(version: String): String {
        return "https://download.getbukkit.org/spigot/spigot-$version.jar"
    }

    /**
     * 下载 Spigot JAR 文件。
     * 委托给 ServerCoreManager 的通用下载方法。
     */
    suspend fun downloadSpigot(
        version: String,
        targetDir: File,
        onProgress: (Float, Long, Long) -> Unit = { _, _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val url = getDirectDownloadUrl(version)
            val coreManager = ServerCoreManager()
            coreManager.downloadJar(url, targetDir, "spigot-$version.jar", onProgress)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
