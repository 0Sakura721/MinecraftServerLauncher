package com.mcserver.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mcserver.launcher.server.PerformanceMonitor

/**
 * 性能历史折线图组件。
 * 借鉴 Pterodactyl 面板的实时性能图表设计。
 *
 * 使用 Compose Canvas 绘制 CPU、内存、TPS 的历史趋势线。
 * 数据来源：PerformanceMonitor.MetricsHistory（最近 60 个采样点，约 2 分钟）。
 */
@Composable
fun PerformanceChartCard(
    history: PerformanceMonitor.MetricsHistory,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "性能趋势",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        // CPU 图表
        ChartSection(
            title = "CPU",
            values = history.cpuHistory,
            maxValue = 100f,
            unit = "%",
            color = MaterialTheme.colorScheme.primary,
            dangerColor = MaterialTheme.colorScheme.error,
            dangerThreshold = 80f
        )

        Spacer(Modifier.height(12.dp))

        // 内存图表
        ChartSection(
            title = "内存 (MB)",
            values = history.memoryHistory.map { it.toFloat() },
            maxValue = history.memoryHistory.maxOrNull()?.toFloat()?.coerceAtLeast(512f) ?: 2048f,
            unit = "MB",
            color = MaterialTheme.colorScheme.tertiary,
            dangerColor = MaterialTheme.colorScheme.error,
            dangerThreshold = 0.9f // 相对于最大值的比例
        )

        Spacer(Modifier.height(12.dp))

        // TPS 图表
        ChartSection(
            title = "TPS",
            values = history.tpsHistory,
            maxValue = 20f,
            unit = "",
            color = Color(0xFF4CAF50),
            dangerColor = MaterialTheme.colorScheme.error,
            dangerThreshold = 15f
        )
    }
}

@Composable
private fun ChartSection(
    title: String,
    values: List<Float>,
    maxValue: Float,
    unit: String,
    color: Color,
    dangerColor: Color,
    dangerThreshold: Float
) {
    val surfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val current = values.lastOrNull()
            if (current != null) {
                Text(
                    text = "${"%.1f".format(current)}$unit",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isInDanger(title, current, maxValue, dangerThreshold)) dangerColor else color
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            if (values.isEmpty()) return@Canvas

            val width = size.width
            val height = size.height
            val effectiveMax = maxValue.coerceAtLeast(values.maxOrNull() ?: maxValue) * 1.05f

            // 背景网格线
            val gridLines = 3
            for (i in 1..gridLines) {
                val y = height - (height * i / gridLines)
                drawLine(
                    color = surfaceVariant,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // 危险阈值线
            val dangerY = if (title == "内存 (MB)") {
                height * (1f - dangerThreshold)
            } else {
                height * (1f - dangerThreshold / effectiveMax)
            }
            drawLine(
                color = dangerColor.copy(alpha = 0.15f),
                start = Offset(0f, dangerY),
                end = Offset(width, dangerY),
                strokeWidth = 2f
            )

            // 折线路径
            if (values.size >= 2) {
                val path = Path()
                val stepX = width / (values.size - 1).coerceAtLeast(1)

                values.forEachIndexed { index, value ->
                    val x = index * stepX
                    val normalizedValue = (value / effectiveMax).coerceIn(0f, 1f)
                    val y = height * (1f - normalizedValue)

                    if (index == 0) path.moveTo(x, y)
                    else path.lineTo(x, y)
                }

                // 渐变填充区域
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo((values.size - 1) * stepX, height)
                    lineTo(0f, height)
                    close()
                }

                // 绘制渐变填充
                drawPath(
                    path = fillPath,
                    color = color.copy(alpha = 0.1f)
                )

                // 绘制折线
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )

                // 绘制数据点（只绘制最后几个点）
                val showPoints = values.size <= 10 || values.takeLast(3).isNotEmpty()
                values.forEachIndexed { index, value ->
                    val x = index * stepX
                    val normalizedValue = (value / effectiveMax).coerceIn(0f, 1f)
                    val y = height * (1f - normalizedValue)

                    val isLast = index == values.size - 1
                    val pointRadius = if (isLast) 3f else 0f

                    if (pointRadius > 0) {
                        drawCircle(
                            color = color,
                            radius = pointRadius,
                            center = Offset(x, y)
                        )
                    }
                }
            }

            // Y 轴标签
            val labelColor = surfaceVariant
            val paint = android.graphics.Paint().apply {
                this.color = android.graphics.Color.argb(
                    (labelColor.alpha * 255).toInt(),
                    (labelColor.red * 255).toInt(),
                    (labelColor.green * 255).toInt(),
                    (labelColor.blue * 255).toInt()
                )
                textSize = 22f
                isAntiAlias = true
            }

            for (i in 0..gridLines) {
                val y = height - (height * i / gridLines)
                val labelValue = effectiveMax * i / gridLines
                val label = if (labelValue >= 1000) "%.0f".format(labelValue)
                            else "%.0f".format(labelValue)
                drawContext.canvas.nativeCanvas.drawText(label, 2f, y - 4f, paint)
            }
        }
    }
}

private fun isInDanger(title: String, current: Float, maxValue: Float, threshold: Float): Boolean {
    return when (title) {
        "CPU" -> current > threshold
        "内存 (MB)" -> current / maxValue > threshold
        "TPS" -> current < threshold
        else -> false
    }
}
