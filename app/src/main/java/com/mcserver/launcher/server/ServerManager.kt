package com.mcserver.launcher.server

import android.content.Context
import android.content.Intent
import com.mcserver.launcher.McApplication
import com.mcserver.launcher.data.JreStatus
import com.mcserver.launcher.data.ServerConfig
import com.mcserver.launcher.data.ServerState
import com.mcserver.launcher.data.ServerStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 服务器管理器：统一管理 JRE、服务器启动/停止、状态跟踪
 */
class ServerManager private constructor() {

    companion object {
        val instance: ServerManager by lazy { ServerManager() }
    }

    private val context: Context get() = McApplication.instance
    private val jreManager = JreManager(context)
    private val jarRunner = JarRunner()

    private val _serverStatus = MutableStateFlow(ServerStatus())
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

    val jreInfo: StateFlow<com.mcserver.launcher.data.JreInfo> = jreManager.jreInfo
    val consoleOutput: SharedFlow<String> = jarRunner.consoleOutput
    val isRunning: Boolean get() = jarRunner.running

    private var serverJob: Job? = null
    private var uptimeJob: Job? = null

    /**
     * 启动服务器
     */
    fun startServer(config: ServerConfig, scope: CoroutineScope) {
        if (jarRunner.running) return

        val jre = jreManager.checkJre()
        if (jre.status != JreStatus.INSTALLED) {
            return
        }

        _serverStatus.value = ServerStatus(state = ServerState.STARTING)

        // 启动前台服务
        val serviceIntent = Intent(context, ServerForegroundService::class.java)
        context.startForegroundService(serviceIntent)

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                _serverStatus.value = _serverStatus.value.copy(state = ServerState.RUNNING)
                startUptimeCounter(scope)
                jarRunner.start(config, jre.path)
                // 服务器停止后
                stopUptimeCounter()
                _serverStatus.value = ServerStatus(state = ServerState.STOPPED)
                context.stopService(Intent(context, ServerForegroundService::class.java))
            } catch (e: Exception) {
                stopUptimeCounter()
                _serverStatus.value = ServerStatus(state = ServerState.ERROR)
                context.stopService(Intent(context, ServerForegroundService::class.java))
            }
        }
    }

    /**
     * 停止服务器
     */
    fun stopServer() {
        _serverStatus.value = _serverStatus.value.copy(state = ServerState.STOPPING)
        jarRunner.stop()
    }

    /**
     * 发送命令
     */
    fun sendCommand(cmd: String) {
        jarRunner.sendCommand(cmd)
    }

    /**
     * 下载/安装 JRE
     */
    suspend fun installJre(onProgress: (Float) -> Unit = {}): Result<String> {
        return jreManager.downloadAndInstall(onProgress)
    }

    private fun startUptimeCounter(scope: CoroutineScope) {
        uptimeJob = scope.launch {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                val uptime = (System.currentTimeMillis() - startTime) / 1000
                _serverStatus.value = _serverStatus.value.copy(uptimeSeconds = uptime)
                delay(1000)
            }
        }
    }

    private fun stopUptimeCounter() {
        uptimeJob?.cancel()
        uptimeJob = null
    }
}
