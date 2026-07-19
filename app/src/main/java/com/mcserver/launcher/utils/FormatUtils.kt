package com.mcserver.launcher.utils

/**
 * 共享格式化工具函数。
 * 集中管理项目中所有格式化逻辑，避免重复代码。
 */
object FormatUtils {

    /** 格式化文件大小（字节 → 人类可读） */
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        return "%.2f GB".format(mb / 1024.0)
    }

    /** 格式化下载速度 */
    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return ""
        val mbps = bytesPerSec / (1024.0 * 1024.0)
        if (mbps >= 1.0) return "%.1f MB/s".format(mbps)
        val kbps = bytesPerSec / 1024.0
        return "%.0f KB/s".format(kbps)
    }

    /** 格式化剩余时间 */
    fun formatRemaining(seconds: Long): String {
        if (seconds < 0) return ""
        if (seconds < 60) return "${seconds}s"
        val m = seconds / 60
        if (m < 60) return "${m}min"
        val h = m / 60
        val rm = m % 60
        return if (rm > 0) "${h}h${rm}min" else "${h}h"
    }

    /** 格式化运行时间 */
    fun formatUptime(seconds: Long): String {
        if (seconds < 0) return "0s"
        if (seconds < 60) return "${seconds}s"
        if (seconds < 3600) {
            val m = seconds / 60
            val s = seconds % 60
            return "${m}m${s}s"
        }
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return "${h}h${m}m"
    }

    /** 格式化运行时间（简短版本，用于通知栏等紧凑空间） */
    fun formatUptimeShort(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return when {
            h > 0 -> "${h}h${m}m"
            m > 0 -> "${m}m"
            else -> "${seconds}s"
        }
    }

    /** 别名：formatBytes */
    fun formatSize(bytes: Long): String = formatBytes(bytes)

    /** 格式化百分比 */
    fun formatPercent(value: Float): String = "%.1f%%".format(value)

    /** 格式化毫秒为日期字符串 */
    fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0) return "—"
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    /** 相对时间（如 "3 分钟前"） */
    fun formatRelativeTime(timestamp: Long): String {
        if (timestamp <= 0) return "—"
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "刚刚"
            diff < 3_600_000 -> "${diff / 60_000} 分钟前"
            diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
            diff < 604_800_000 -> "${diff / 86_400_000} 天前"
            else -> {
                val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                sdf.format(java.util.Date(timestamp))
            }
        }
    }
}
