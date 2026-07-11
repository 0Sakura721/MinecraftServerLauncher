package com.mcserver.launcher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class McApplication : Application() {
    companion object {
        const val CHANNEL_SERVER = "server_status"
        const val CHANNEL_NOTIFICATIONS = "server_notifications"
        lateinit var instance: McApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val statusChannel = NotificationChannel(
            CHANNEL_SERVER,
            "服务器状态",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示 Minecraft 服务器运行状态"
            setShowBadge(false)
        }

        val notifChannel = NotificationChannel(
            CHANNEL_NOTIFICATIONS,
            "服务器通知",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "服务器事件通知"
        }

        manager.createNotificationChannel(statusChannel)
        manager.createNotificationChannel(notifChannel)
    }
}
