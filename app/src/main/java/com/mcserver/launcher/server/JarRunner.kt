package com.mcserver.launcher.server

import android.util.Log
import com.mcserver.launcher.data.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * JAR 文件执行器
 * 使用 ProcessBuilder 运行 Java JAR，捕获控制台输出
 */
class JarRunner {

    companion object {
        private const val TAG = "JarRunner"
    }

    private var process: Process? = null
    private var isRunning = false

    private val _consoleOutput = MutableSharedFlow<String>(replay = 200)
    val consoleOutput: SharedFlow<String> = _consoleOutput.asSharedFlow()

    private val _stateChanged = MutableSharedFlow<Boolean>(replay = 1)
    val stateChanged: SharedFlow<Boolean> = _stateChanged.asSharedFlow()

    val running: Boolean get() = isRunning

    /**
     * 启动 JAR
     */
    suspend fun start(config: ServerConfig, javaPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val jarFile = File(config.jarPath)
                if (!jarFile.exists()) {
                    return@withContext Result.failure(
                        Exception("找不到 JAR 文件: ${config.jarPath}")
                    )
                }

                val javaFile = File(javaPath)
                if (!javaFile.exists() || !javaFile.canExecute()) {
                    return@withContext Result.failure(
                        Exception("JRE 不可用，请先安装 Java 运行时")
                    )
                }

                val workDir = jarFile.parentFile ?: File("/")

                val command = mutableListOf<String>().apply {
                    add(javaPath)
                    addAll(config.toCommandArgs())
                }

                Log.d(TAG, "启动命令: ${command.joinToString(" ")}")
                Log.d(TAG, "工作目录: ${workDir.absolutePath}")

                _consoleOutput.emit("> ${command.joinToString(" ")}")
                _consoleOutput.emit("> 工作目录: ${workDir.absolutePath}")

                val pb = ProcessBuilder(command)
                    .directory(workDir)
                    .redirectErrorStream(true)

                // 设置环境变量
                val env = pb.environment()
                env["JAVA_HOME"] = File(javaPath).parentFile?.parentFile?.absolutePath ?: ""

                process = pb.start()
                isRunning = true
                _stateChanged.emit(true)

                // 读取输出
                val reader = BufferedReader(InputStreamReader(process!!.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    _consoleOutput.emit(line!!)
                }

                // 进程结束
                val exitCode = process?.waitFor() ?: -1
                isRunning = false
                process = null
                _consoleOutput.emit("> 服务器已停止 (退出码: $exitCode)")
                _stateChanged.emit(false)

                Result.success(Unit)
            } catch (e: Exception) {
                isRunning = false
                process = null
                Log.e(TAG, "服务器启动失败", e)
                _consoleOutput.emit("> 错误: ${e.message}")
                _stateChanged.emit(false)
                Result.failure(e)
            }
        }

    /**
     * 向服务器发送命令
     */
    fun sendCommand(command: String) {
        val proc = process ?: return
        if (!isRunning) return
        try {
            val writer = OutputStreamWriter(proc.outputStream)
            writer.write(command)
            writer.write("\n")
            writer.flush()
            _consoleOutput.tryEmit("> $command")
        } catch (e: Exception) {
            Log.e(TAG, "发送命令失败", e)
        }
    }

    /**
     * 停止服务器
     */
    fun stop() {
        try {
            // 先尝试优雅关闭
            sendCommand("stop")
            // 等待几秒后强制终止
            Thread.sleep(5000)
            if (isRunning) {
                process?.destroy()
            }
            // 如果还没停止，强制 kill
            Thread.sleep(3000)
            if (isRunning) {
                process?.destroyForcibly()
            }
            isRunning = false
            process = null
            _stateChanged.tryEmit(false)
        } catch (e: Exception) {
            Log.e(TAG, "停止服务器失败", e)
        }
    }

    /**
     * 强制终止
     */
    fun forceStop() {
        try {
            process?.destroyForcibly()
            isRunning = false
            process = null
            _consoleOutput.tryEmit("> 服务器已被强制终止")
            _stateChanged.tryEmit(false)
        } catch (e: Exception) {
            Log.e(TAG, "强制停止失败", e)
        }
    }
}
