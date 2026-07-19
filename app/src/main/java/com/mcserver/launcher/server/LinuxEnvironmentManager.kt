package com.mcserver.launcher.server

import android.content.Context
import com.mcserver.launcher.McApplication
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Linux 环境状态
 */
enum class LinuxEnvState {
    /** 未初始化 */
    NOT_INITIALIZED,
    /** 正在设置（下载/解压/安装依赖） */
    SETTING_UP,
    /** 环境就绪 */
    READY,
    /** 出错 */
    ERROR
}

/**
 * 单个下载项的状态
 */
enum class DownloadItemState {
    PENDING,
    DOWNLOADING,
    EXTRACTING,
    COMPLETED,
    FAILED
}

/**
 * 单个下载项
 */
data class DownloadItem(
    val id: String,
    val name: String,
    val description: String,
    val totalBytes: Long = 0,
    val state: DownloadItemState = DownloadItemState.PENDING,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0,
    val speedBytesPerSec: Long = 0,
    val errorMessage: String? = null
)

/**
 * Linux 环境管理器 — proot + Ubuntu 24.04（内置）
 *
 * 下载清单（顺序）：
 *   1. proot 二进制（~1.2 MB）— APK 内置，无需网络
 *   2. Ubuntu 24.04 base rootfs（~30 MB 压缩）— APK 内置，无需网络
 *   3. JDK 8（~80 MB）— 通过 apt 网络安装
 *   4. JDK 11（~82 MB）
 *   5. JDK 17（~88 MB）
 *   6. JDK 21（~90 MB）
 *
 * 架构支持：
 *   - arm64-v8a（aarch64）
 *   - armeabi-v7a（armhf）
 *
 * 为什么是 Ubuntu 而不是 Alpine：
 *   - glibc 兼容性（Alpine 用 musl，某些 Java 库可能不兼容）
 *   - apt 生态更完整，Minecraft server 社区主流
 *   - Operit 同款方案（proot + Ubuntu）
 */
object LinuxEnvironmentManager {

    private const val TAG = "LinuxEnvManager"

    // ── 下载源（多源 + 自动测速） ──
    // 镜像源由 MirrorSpeedTester 预定义，下载前自动测速选择最优

    /** JDK 版本列表 — Ubuntu apt 包名 */
    data class JdkVersion(val version: Int, val aptPackage: String, val label: String) {
        companion object {
            val ALL = listOf(
                JdkVersion(8,  "openjdk-8-jdk-headless",  "Java 8"),
                JdkVersion(11, "openjdk-11-jdk-headless", "Java 11"),
                JdkVersion(17, "openjdk-17-jdk-headless", "Java 17"),
                JdkVersion(21, "openjdk-21-jdk-headless", "Java 21")
            )
            fun forVersion(v: Int) = ALL.firstOrNull { it.version == v }
        }
    }

    // ── 内部状态 ──
    private val _envState = MutableStateFlow(LinuxEnvState.NOT_INITIALIZED)
    val envState: StateFlow<LinuxEnvState> = _envState.asStateFlow()

    private val _downloadItems = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloadItems: StateFlow<List<DownloadItem>> = _downloadItems.asStateFlow()

    private val _setupLog = MutableSharedFlow<String>(replay = 100, extraBufferCapacity = 50)
    val setupLog: SharedFlow<String> = _setupLog.asSharedFlow()

    /** 当前下载使用的镜像源（暴露给 UI 显示） */
    private val _mirrorResults = MutableStateFlow<List<MirrorTestResult>>(emptyList())
    val mirrorResults: StateFlow<List<MirrorTestResult>> = _mirrorResults.asStateFlow()

    /** 当前镜像测试状态 */
    private val _isTestingMirrors = MutableStateFlow(false)
    val isTestingMirrors: StateFlow<Boolean> = _isTestingMirrors.asStateFlow()

    private val context: Context get() = McApplication.instance

    /** 应用内 Linux 工作目录 */
    private val linuxDir: File
        get() {
            val dir = File(context.filesDir, "linux")
            dir.mkdirs()
            return dir
        }

    /** proot 二进制路径 */
    private val prootBinary: File get() = File(linuxDir, "proot")
    /** rootfs 目录 */
    val rootfsDir: File get() = File(linuxDir, "rootfs")
    /** Ubuntu 内 Java 根目录 */
    val javaHomeDir: File get() = File(rootfsDir, "usr/lib/jvm")
    /** Ubuntu JDK 路径的架构后缀（arm64 / armhf） */
    private val jdkArchSuffix: String get() = if (isAarch64) "arm64" else "armhf"
    /** 服务器工作目录（外部共享） */
    val serverDir: File
        get() {
            val dir = File(android.os.Environment.getExternalStorageDirectory(), "mcserver")
            dir.mkdirs()
            return dir
        }

    // ── 架构检测 ──
    private val isAarch64: Boolean
        get() = android.os.Build.SUPPORTED_ABIS.any {
            it.contains("arm64-v8a", ignoreCase = true) || it.contains("aarch64", ignoreCase = true)
        }

    private val archName: String get() = if (isAarch64) "aarch64" else "armhf"

    // ── 内置资源提取 ──
    /**
     * 尝试从 APK assets/bundled 提取文件到目标路径。
     * @return true 表示提取成功，false 表示资源不存在
     */
    private fun extractBundledAsset(assetName: String, dest: File): Boolean {
        if (dest.exists() && dest.length() > 0) return true // 已存在
        return try {
            dest.parentFile?.mkdirs()
            context.assets.open("bundled/$assetName").use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // ── 环境检查 ──
    fun isEnvironmentReady(): Boolean {
        return prootBinary.exists() && prootBinary.canExecute() &&
            rootfsDir.exists() && File(rootfsDir, "bin/sh").exists()
    }

    fun isJdkInstalled(version: Int): Boolean {
        // Ubuntu 安装后路径如 /usr/lib/jvm/java-17-openjdk-arm64/bin/java
        val javaDir = File(javaHomeDir, "java-$version-openjdk-$jdkArchSuffix")
        val javaBin = File(javaDir, "bin/java")
        return javaBin.exists() && javaBin.canExecute()
    }

    fun getJavaPath(version: Int): String {
        val suffix = "java-$version-openjdk-$jdkArchSuffix"
        val candidate = File(javaHomeDir, suffix)
        if (candidate.exists()) return "/usr/lib/jvm/$suffix/bin/java"
        // 回退：某些 Ubuntu 版本无架构后缀
        val fallback = File(javaHomeDir, "java-$version-openjdk")
        if (fallback.exists()) return "/usr/lib/jvm/java-$version-openjdk/bin/java"
        return "/usr/lib/jvm/$suffix/bin/java"
    }

    // ── 全自动初始化 ──
    suspend fun runFullSetup(): Result<Unit> = withContext(Dispatchers.IO) {
        if (_envState.value == LinuxEnvState.READY) return@withContext Result.success(Unit)
        _envState.value = LinuxEnvState.SETTING_UP

        try {
            val items = mutableListOf(
                DownloadItem("proot", "proot 运行时", "Linux 进程模拟器"),
                DownloadItem("rootfs", "Ubuntu 24.04", "完整 glibc Linux 根文件系统"),
                DownloadItem("jdk8", "Java 8", "Minecraft 1.8-1.12"),
                DownloadItem("jdk11", "Java 11", "Minecraft 1.13-1.16"),
                DownloadItem("jdk17", "Java 17", "Minecraft 1.17-1.20.4"),
                DownloadItem("jdk21", "Java 21", "Minecraft 1.20.5+")
            )
            _downloadItems.value = items

            // ── Step 1: proot 二进制 ──
            log(">>> 阶段 1/6：获取 proot 二进制（$archName）")
            val prootAssetName = "proot-$archName"
            if (extractBundledAsset(prootAssetName, prootBinary)) {
                log("  ✓ 从内置资源提取 proot（无需下载）")
                updateItem("proot", DownloadItemState.COMPLETED)
            } else {
                // 回退到网络下载
                log("  内置资源不可用，从网络下载...")
                val prootMirrors = if (isAarch64) MirrorSpeedTester.PROOT_MIRRORS_AARCH64
                                    else MirrorSpeedTester.PROOT_MIRRORS_ARMHF
                _isTestingMirrors.value = true
                log("  正在测速 ${prootMirrors.size} 个镜像源...")
                val prootResults = MirrorSpeedTester.testMirrors(prootMirrors)
                _mirrorResults.value = prootResults
                _isTestingMirrors.value = false
                val bestProot = prootResults.firstOrNull { it.error == null }
                if (bestProot != null) {
                    log("  ✓ 最优: ${bestProot.name} (${bestProot.latencyMs}ms)")
                } else {
                    log("  ⚠ 所有镜像超时，回退到 GitHub 官方")
                }
                val prootUrl = bestProot?.url ?: prootMirrors.first().url
                updateItem("proot", DownloadItemState.DOWNLOADING)
                downloadFile(prootUrl, prootBinary) { progress, downloaded, total, speed ->
                    updateProgress("proot", progress, downloaded, total, speed)
                }
                updateItem("proot", DownloadItemState.COMPLETED)
            }
            prootBinary.setExecutable(true)
            log("  ✓ proot 就绪")

            // ── Step 2: Ubuntu rootfs ──
            log(">>> 阶段 2/6：获取 Ubuntu 24.04 rootfs（$archName）")
            val ubuntuArch = if (isAarch64) "arm64" else "armhf"
            val rootfsAssetName = "ubuntu-base-24.04-$ubuntuArch.tar.gz"
            val rootfsTarball = File(linuxDir, "rootfs.tar.gz")
            if (extractBundledAsset(rootfsAssetName, rootfsTarball)) {
                log("  ✓ 从内置资源提取 Ubuntu rootfs（无需下载）")
                updateItem("rootfs", DownloadItemState.COMPLETED)
            } else {
                // 回退到网络下载
                log("  内置资源不可用，从网络下载...")
                val ubuntuMirrors = if (isAarch64) MirrorSpeedTester.UBUNTU_ROOTFS_MIRRORS_AARCH64
                                    else MirrorSpeedTester.UBUNTU_ROOTFS_MIRRORS_ARMHF
                _isTestingMirrors.value = true
                log("  正在测速 ${ubuntuMirrors.size} 个镜像源...")
                val ubuntuResults = MirrorSpeedTester.testMirrors(ubuntuMirrors)
                _mirrorResults.value = ubuntuResults
                _isTestingMirrors.value = false
                val bestUbuntu = ubuntuResults.firstOrNull { it.error == null }
                if (bestUbuntu != null) {
                    log("  ✓ 最优: ${bestUbuntu.name} (${bestUbuntu.latencyMs}ms)")
                } else {
                    log("  ⚠ 所有镜像超时，回退到 Ubuntu 官方")
                }
                updateItem("rootfs", DownloadItemState.DOWNLOADING)
                val rootfsUrl = bestUbuntu?.url ?: ubuntuMirrors.first().url
                downloadFile(rootfsUrl, rootfsTarball) { progress, downloaded, total, speed ->
                    updateProgress("rootfs", progress, downloaded, total, speed)
                }
                updateItem("rootfs", DownloadItemState.COMPLETED)
            }
            // 解压 rootfs（无论来源）
            if (!File(rootfsDir, "bin/sh").exists()) {
                updateItem("rootfs", DownloadItemState.EXTRACTING)
                log("  解压 rootfs（约 200 MB，请耐心等待）...")
                extractTarGz(rootfsTarball, rootfsDir)
                log("  ✓ Ubuntu rootfs 就绪")
            }
            rootfsTarball.delete()

            // ── Step 3: 初始化 Ubuntu 包管理器 ──
            log(">>> 阶段 3/6：初始化 apt 包管理器")
            setupUbuntuRepos()
            log("  ✓ apt 就绪")

            // ── Step 4-6: 安装各版本 JDK ──
            val jdkList = listOf(8 to "jdk8", 11 to "jdk11", 17 to "jdk17", 21 to "jdk21")
            for ((index, pair) in jdkList.withIndex()) {
                val (version, itemId) = pair
                val jdk = JdkVersion.forVersion(version) ?: continue
                val stepNum = index + 4
                log(">>> 阶段 $stepNum/6：安装 ${jdk.label}（${jdk.aptPackage}）")
                updateItem(itemId, DownloadItemState.DOWNLOADING)
                installUbuntuPackage(jdk.aptPackage) { progress, downloaded, total, speed ->
                    updateProgress(itemId, progress, downloaded, total, speed)
                }
                updateItem(itemId, DownloadItemState.COMPLETED)
                log("  ✓ ${jdk.label} 安装完成")
            }

            _envState.value = LinuxEnvState.READY
            log(">>> 环境初始化完成！所有 JDK 已就绪")
            Result.success(Unit)
        } catch (e: Exception) {
            _envState.value = LinuxEnvState.ERROR
            log("> 错误：${e.message}")
            Result.failure(e)
        }
    }

    // ── 文件下载（带进度回调） ──
    private suspend fun downloadFile(
        urlStr: String,
        dest: File,
        onProgress: (Float, Long, Long, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        dest.parentFile?.mkdirs()
        val url = URL(urlStr)
        var connection = url.openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 15000
        connection.readTimeout = 60000
        // 处理重定向
        var redirectCount = 0
        while (redirectCount < 5) {
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_MOVED_TEMP ||
                code == HttpURLConnection.HTTP_MOVED_PERM ||
                code == HttpURLConnection.HTTP_SEE_OTHER ||
                code == 307 || code == 308
            ) {
                val newUrl = connection.getHeaderField("Location") ?: throw RuntimeException("重定向缺少 Location")
                connection.disconnect()
                connection = URL(newUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 15000
                connection.readTimeout = 60000
                redirectCount++
                continue
            }
            break
        }
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw RuntimeException("HTTP ${connection.responseCode}")
        }

        val totalBytes = connection.contentLengthLong
        val buffer = ByteArray(8192)
        var downloaded = 0L
        val startTime = System.currentTimeMillis()
        var lastUpdateTime = startTime
        var lastUpdateBytes = 0L

        connection.inputStream.use { input ->
            FileOutputStream(dest).use { output ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read

                    val now = System.currentTimeMillis()
                    val elapsed = now - lastUpdateTime
                    if (elapsed >= 200) {
                        val progress = if (totalBytes > 0) downloaded.toFloat() / totalBytes else 0f
                        val overallElapsed = (now - startTime).coerceAtLeast(1)
                        val speed = downloaded * 1000 / overallElapsed
                        onProgress(progress, downloaded, totalBytes, speed)
                        lastUpdateTime = now
                        lastUpdateBytes = downloaded
                    }
                }
            }
        }
        connection.disconnect()
    }

    // ── tar.gz 解压 ──
    private fun extractTarGz(tarGzFile: File, destDir: File) {
        destDir.mkdirs()
        val proc = ProcessBuilder()
            .command("tar", "xzf", tarGzFile.absolutePath, "-C", destDir.absolutePath)
            .redirectErrorStream(true)
            .start()
        val exitCode = proc.waitFor()
        if (exitCode != 0) {
            val err = proc.inputStream.bufferedReader().readText()
            throw RuntimeException("解压失败 ($exitCode): $err")
        }
    }

    // ── Ubuntu apt 包安装 ──
    private suspend fun installUbuntuPackage(
        pkgName: String,
        onProgress: (Float, Long, Long, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        // 使用 proot 在 Ubuntu rootfs 中安装包
        val proc = ProcessBuilder()
            .command(
                prootBinary.absolutePath,
                "-0",
                "-r", rootfsDir.absolutePath,
                "-b", "/dev:/dev",
                "-b", "/proc:/proc",
                "-b", "/sys:/sys",
                "-b", "${serverDir.absolutePath}:${serverDir.absolutePath}",
                "/bin/sh", "-c",
                "apt-get update -qq && DEBIAN_FRONTEND=noninteractive apt-get install -y -qq $pkgName"
            )
            .redirectErrorStream(true)
            .start()

        // 按时间估算进度
        val startTime = System.currentTimeMillis()
        val estimatedBytes = when {
            pkgName.contains("openjdk") -> 120_000_000L
            else -> 50_000_000L
        }
        val estimatedSeconds = 180 // apt 比 apk 慢

        CoroutineScope(Dispatchers.IO).launch {
            var elapsed = 1L
            while (proc.isAlive) {
                delay(500)
                elapsed = ((System.currentTimeMillis() - startTime) / 1000).coerceAtLeast(1)
                val fraction = (elapsed.toFloat() / estimatedSeconds).coerceAtMost(0.99f)
                val estimated = (estimatedBytes * fraction).toLong()
                onProgress(fraction, estimated, estimatedBytes, estimatedBytes / elapsed)
            }
        }

        val output = proc.inputStream.bufferedReader().readText()
        val exitCode = proc.waitFor()
        if (exitCode != 0) {
            throw RuntimeException("apt install $pkgName 失败 ($exitCode): ${output.take(500)}")
        }
        onProgress(1f, estimatedBytes, estimatedBytes, 0)
    }

    // ── Proot 执行命令（在 Ubuntu rootfs 中） ──
    /**
     * 通过 proot 在 Ubuntu 环境中执行命令。
     * @param command 完整 shell 命令
     * @param workDir 工作目录（相对 rootfs 或绝对路径）
     * @param onOutput 标准输出回调（逐行）
     * @return 退出码
     */
    fun executeCommand(
        command: String,
        workDir: String = "/root",
        onOutput: ((String) -> Unit)? = null
    ): Int {
        check(prootBinary.exists()) { "proot 未安装" }
        check(rootfsDir.exists()) { "rootfs 未解压" }

        val args = mutableListOf(
            prootBinary.absolutePath,
            "-0",
            "-r", rootfsDir.absolutePath,
            "-b", "/dev:/dev",
            "-b", "/proc:/proc",
            "-b", "/sys:/sys",
            "-b", "${serverDir.absolutePath}:${serverDir.absolutePath}",
            "/bin/sh", "-c",
            "cd $workDir && $command"
        )

        val proc = ProcessBuilder(args)
            .redirectErrorStream(true)
            .start()

        if (onOutput != null) {
            Thread {
                proc.inputStream.bufferedReader().use { reader ->
                    reader.lines().forEach { onOutput(it) }
                }
            }.start()
        }

        return proc.waitFor()
    }

    /**
     * 异步执行命令并通过 Flow 输出。
     */
    fun executeAsync(command: String, workDir: String = "/root"): SharedFlow<String> {
        val flow = MutableSharedFlow<String>(extraBufferCapacity = 200)
        CoroutineScope(Dispatchers.IO).launch {
            val args = listOf(
                prootBinary.absolutePath, "-0",
                "-r", rootfsDir.absolutePath,
                "-b", "/dev:/dev",
                "-b", "/proc:/proc",
                "-b", "/sys:/sys",
                "-b", "${serverDir.absolutePath}:${serverDir.absolutePath}",
                "/bin/sh", "-c", "cd $workDir && $command"
            )
            val proc = ProcessBuilder(args).redirectErrorStream(true).start()
            proc.inputStream.bufferedReader().use { reader ->
                reader.lines().forEach { flow.tryEmit(it) }
            }
            proc.waitFor()
        }
        return flow.asSharedFlow()
    }

    // ── 工具函数 ──
    private fun setupUbuntuRepos() {
        // 配置 DNS
        val resolvConf = File(rootfsDir, "etc/resolv.conf")
        resolvConf.parentFile?.mkdirs()
        resolvConf.writeText("nameserver 8.8.8.8\nnameserver 8.8.4.4\nnameserver 114.114.114.114\n")

        // 自动选择与下载 rootfs 时相同的最优国内镜像
        val bestMirror = _mirrorResults.value.firstOrNull { it.error == null }
        val mirrorHost = when (bestMirror?.key) {
            "tuna" -> "https://mirrors.tuna.tsinghua.edu.cn/ubuntu"
            "ustc" -> "https://mirrors.ustc.edu.cn/ubuntu"
            "aliyun" -> "https://mirrors.aliyun.com/ubuntu"
            else -> "http://ports.ubuntu.com/ubuntu-ports"  // ARM 官方源
        }
        val sourcesFile = File(rootfsDir, "etc/apt/sources.list")
        sourcesFile.parentFile?.mkdirs()
        if (!sourcesFile.exists() || sourcesFile.readText().isBlank()) {
            sourcesFile.writeText(
                "deb $mirrorHost noble main restricted universe multiverse\n" +
                "deb $mirrorHost noble-updates main restricted universe multiverse\n" +
                "deb $mirrorHost noble-security main restricted universe multiverse\n"
            )
            log("  Ubuntu 软件源: $mirrorHost")
        }
    }

    private fun ensureFile(rootfs: File, path: String, content: String) {
        val file = File(rootfs, path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    private fun updateItem(id: String, state: DownloadItemState) {
        val items = _downloadItems.value.toMutableList()
        val idx = items.indexOfFirst { it.id == id }
        if (idx >= 0) {
            items[idx] = items[idx].copy(state = state)
            _downloadItems.value = items
        }
    }

    private fun updateProgress(id: String, progress: Float, downloaded: Long, total: Long, speed: Long) {
        val items = _downloadItems.value.toMutableList()
        val idx = items.indexOfFirst { it.id == id }
        if (idx >= 0) {
            items[idx] = items[idx].copy(
                progress = progress,
                downloadedBytes = downloaded,
                totalBytes = total,
                speedBytesPerSec = speed
            )
            _downloadItems.value = items
        }
    }

    private fun log(msg: String) {
        _setupLog.tryEmit(msg)
    }
}