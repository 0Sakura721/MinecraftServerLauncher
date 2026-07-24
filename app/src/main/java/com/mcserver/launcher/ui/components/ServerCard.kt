package com.mcserver.launcher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mcserver.launcher.data.ServerState
import com.mcserver.launcher.data.ServerStatus
import com.mcserver.launcher.ui.theme.extendedColorScheme

@Composable
fun ServerStatusCard(
    status: ServerStatus,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = extendedColorScheme()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusDot(state = status.state)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = when (status.state) {
                        ServerState.STOPPED -> "服务器已停止"
                        ServerState.STARTING -> "正在启动..."
                        ServerState.RUNNING -> "运行中"
                        ServerState.STOPPING -> "正在停止..."
                        ServerState.ERROR -> "错误"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (status.state == ServerState.RUNNING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoItem("运行时间", formatUptime(status.uptimeSeconds))
                    InfoItem("内存", "${status.memoryUsedMB} MB")
                    InfoItem("玩家", "${status.playerCount}")
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (status.players.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.People,
                            contentDescription = null,
                            tint = extendedColors.online,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = status.players.joinToString("、"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (status.state == ServerState.STOPPED || status.state == ServerState.ERROR) {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("启动服务器")
                    }
                } else if (status.state == ServerState.RUNNING) {
                    OutlinedButton(
                        onClick = onStop,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = extendedColors.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("停止服务器")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDot(state: ServerState) {
    val extendedColors = extendedColorScheme()
    val color = when (state) {
        ServerState.STOPPED -> extendedColors.offline
        ServerState.STARTING -> extendedColors.warning
        ServerState.RUNNING -> extendedColors.online
        ServerState.STOPPING -> extendedColors.warning
        ServerState.ERROR -> extendedColors.error
    }
    Surface(
        modifier = Modifier.size(12.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = color
    ) {}
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatUptime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m ${s}s"
}