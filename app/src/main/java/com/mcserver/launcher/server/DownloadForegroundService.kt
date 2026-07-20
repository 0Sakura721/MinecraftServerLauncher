package com.mcserver.launcher.server

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mcserver.launcher.MainActivity
import com.mcserver.launcher.McApplication
import com.mcserver.launcher.R
import com.mcserver.launcher.data.JreInfo
import com.mcserver.launcher.data.JreStatus
import kotlinx.coroutines.*

/**
 * JRE 下载前台服务。
 * 保持下载过程在后台不被系统杀死，并在通知栏显示实时进度。
 *
 * 借鉴 ServerForegroundService 的模式：
 * - 定期更新通知内容（下载百分比、速度、ETA）
 * - 提供取消下载的快捷操作
 * - 下载完成后自动停止
 */
class DownloadForegroundService : Service() {

    companion object {
        const val NOTIFICATION_ID = 2001
        const val ACTION_CANCEL = "com.mcserver.launcher.action.CANCEL_DOWNLOAD"

        /** 标记本前台服务是否存活 */
        @Volatile
        var isRunning = false
            private set
    }

    private var updateJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialNotification = NotificationCompat.Builder(this, McApplication.CHANNEL_NOTIFICATIONS)
            .setContentTitle("准备下载...")
            .setContentText("正在准备 Java 运行时下载")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, initialNotification)

        when (intent?.action) {
            ACTION_CANCEL -> {
                // 通知用户取消下载（由 JreManager 处理实际取消逻辑）
                ServerManager.instance.cancelDownload()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // 启动通知更新循环（每 2 秒更新进度）
        startNotificationUpdates()

        return START_STICKY
    }

    private fun startNotificationUpdates() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive && isRunning) {
                updateNotification()
                delay(2000)
            }
        }
    }

    private fun updateNotification() {
        try {
            val jreInfo = ServerManager.instance.jreInfo.value

            val pendingIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val cancelIntent = PendingIntent.getService(
                this, 1,
                Intent(this, DownloadForegroundService::class.java).apply {
                    action = ACTION_CANCEL
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 构建通知内容：显示下载进度
            val titleText = when (jreInfo.status) {
                JreStatus.DOWNLOADING -> "正在下载 Java ${jreInfo.version}"
                JreStatus.PAUSED -> "Java 下载已暂停"
                JreStatus.EXTRACTING -> "正在解压 Java ${jreInfo.version}"
                JreStatus.INSTALLED -> "Java ${jreInfo.version} 安装完成"
                else -> "Java 运行时下载"
            }

            val contentText = buildString {
                when (jreInfo.status) {
                    JreStatus.DOWNLOADING -> {
                        if (jreInfo.totalBytes > 0) {
                            val percent = (jreInfo.downloadProgress * 100).toInt()
                            append("$percent%")
                            append(" · ${formatBytes(jreInfo.downloadedBytes)}/${formatBytes(jreInfo.totalBytes)}")
                            if (jreInfo.downloadSpeedBytesPerSec > 0) {
                                append(" · ${formatSpeed(jreInfo.downloadSpeedBytesPerSec)}")
                            }
                            if (jreInfo.remainingSeconds > 0) {
                                append(" · 剩余 ${formatRemaining(jreInfo.remainingSeconds)}")
                            }
                        } else {
                            append("准备中...")
                        }
                    }
                    JreStatus.PAUSED -> {
                        val percent = (jreInfo.downloadProgress * 100).toInt()
                        append("已暂停 — $percent%")
                    }
                    JreStatus.EXTRACTING -> append("正在解压安装...")
                    else -> append("处理中...")
                }
            }

            val notificationBuilder = NotificationCompat.Builder(this, McApplication.CHANNEL_NOTIFICATIONS)
                .setContentTitle(titleText)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(jreInfo.status == JreStatus.DOWNLOADING || jreInfo.status == JreStatus.EXTRACTING)
                .setOnlyAlertOnce(true)

            // 下载中：显示进度条 + 取消按钮
            if (jreInfo.status == JreStatus.DOWNLOADING && jreInfo.totalBytes > 0) {
                notificationBuilder.setProgress(
                    100,
                    (jreInfo.downloadProgress * 100).toInt().coerceIn(0, 100),
                    false
                )
                notificationBuilder.addAction(
                    android.R.drawable.ic_media_pause,
                    "取消",
                    cancelIntent
                )
            }

            // 暂停中：显示暂停状态
            if (jreInfo.status == JreStatus.PAUSED) {
                notificationBuilder.setProgress(
                    100,
                    (jreInfo.downloadProgress * 100).toInt().coerceIn(0, 100),
                    false
                )
            }

            val notification = notificationBuilder.build()
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.notify(NOTIFICATION_ID, notification)

            // 如果下载已完成或失败，自动停止服务
            if (jreInfo.status == JreStatus.INSTALLED || jreInfo.status == JreStatus.ERROR) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        updateJob?.cancel()
        serviceScope.cancel()
    }

    // ─── 格式化工具（避免依赖外部 utils，保持服务独立性） ───

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        return "%.2f GB".format(mb / 1024.0)
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return ""
        val mbps = bytesPerSec / (1024.0 * 1024.0)
        if (mbps >= 1.0) return "%.1f MB/s".format(mbps)
        val kbps = bytesPerSec / 1024.0
        return "%.0f KB/s".format(kbps)
    }

    private fun formatRemaining(seconds: Long): String {
        if (seconds < 60) return "${seconds}s"
        val m = seconds / 60
        if (m < 60) return "${m}min"
        val h = m / 60
        val rm = m % 60
        return if (rm > 0) "${h}h${rm}min" else "${h}h"
    }
}
