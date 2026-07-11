package com.mcserver.launcher.server

import android.content.Context
import android.os.Build
import com.mcserver.launcher.data.JreInfo
import com.mcserver.launcher.data.JreStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * JRE 管理器：检测、下载、管理 ARM 架构的 Java 运行时
 * 使用 Adoptium (Eclipse Temurin) 提供的 ARM 构建版本
 */
class JreManager(private val context: Context) {

    private val _jreInfo = MutableStateFlow(JreInfo())
    val jreInfo: StateFlow<JreInfo> = _jreInfo.asStateFlow()

    private val jreDir: File
        get() = File(context.filesDir, "jre")

    private val javaExecutable: File
        get() {
            val binDir = File(jreDir, "bin")
            return File(binDir, "java")
        }

    companion object {
        // Adoptium JRE 21 ARM 构建下载地址
        private const val JRE_VERSION = "21.0.3"
        private const val BASE_URL = "https://api.adoptium.net/v3/binary/latest/21/ga"

        fun getJreDownloadUrl(): String {
            val arch = if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty() &&
                Build.SUPPORTED_64_BIT_ABIS[0].contains("arm64")
            ) {
                "aarch64"
            } else {
                "arm"
            }
            return "$BASE_URL/linux/$arch/jre/hotspot/normal/eclipse?project=jdk"
        }
    }

    /**
     * 检查 JRE 是否已安装
     */
    fun checkJre(): JreInfo {
        val installed = javaExecutable.exists() && javaExecutable.canExecute()
        return if (installed) {
            JreInfo(
                status = JreStatus.INSTALLED,
                path = javaExecutable.absolutePath
            )
        } else {
            JreInfo(status = JreStatus.NOT_INSTALLED)
        }
    }

    init {
        _jreInfo.value = checkJre()
    }

    /**
     * 下载并安装 JRE
     */
    suspend fun downloadAndInstall(
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            _jreInfo.value = _jreInfo.value.copy(status = JreStatus.DOWNLOADING, downloadProgress = 0f)
            onProgress(0f)

            val url = getJreDownloadUrl()
            val tempFile = File(context.cacheDir, "jre_download.tar.gz")

            // 下载
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 300000
            connection.connect()

            val contentLength = connection.contentLengthLong
            var downloaded = 0L

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        if (contentLength > 0) {
                            val progress = (downloaded.toFloat() / contentLength).coerceIn(0f, 1f)
                            _jreInfo.value = _jreInfo.value.copy(downloadProgress = progress)
                            onProgress(progress)
                        }
                    }
                }
            }
            connection.disconnect()

            // 解压
            _jreInfo.value = _jreInfo.value.copy(status = JreStatus.EXTRACTING)
            onProgress(1f)

            if (jreDir.exists()) jreDir.deleteRecursively()
            jreDir.mkdirs()

            extractTarGz(tempFile, jreDir)

            // 确保可执行
            javaExecutable.setExecutable(true)
            tempFile.delete()

            val info = checkJre()
            _jreInfo.value = info
            Result.success(info.path)
        } catch (e: Exception) {
            _jreInfo.value = JreInfo(status = JreStatus.ERROR)
            Result.failure(e)
        }
    }

    private fun extractTarGz(tarGzFile: File, destDir: File) {
        // 使用 Android 内置的 tar 命令解压（如果可用）
        val process = ProcessBuilder()
            .command("tar", "xzf", tarGzFile.absolutePath, "-C", destDir.absolutePath, "--strip-components=1")
            .redirectErrorStream(true)
            .start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            // 备选方案：使用 busybox 或手动解压
            throw RuntimeException("无法解压 JRE 包，退出码: $exitCode")
        }
    }
}
