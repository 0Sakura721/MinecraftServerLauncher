package com.mcserver.launcher.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mcserver.launcher.data.ServerConfig
import com.mcserver.launcher.utils.MemoryInfo
import com.mcserver.launcher.utils.getDeviceMemoryInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConfigScreen(
    config: ServerConfig,
    onConfigSave: (ServerConfig) -> Unit
) {
    var name by remember { mutableStateOf(config.name) }
    var jarPath by remember { mutableStateOf(config.jarPath) }
    var allocatedMemory by remember { mutableIntStateOf(config.allocatedMemoryMB) }
    var port by remember { mutableStateOf(config.serverPort.toString()) }
    var extraArgs by remember { mutableStateOf(config.additionalArgs) }
    var autoRestart by remember { mutableStateOf(config.autoRestart) }
    var nogui by remember { mutableStateOf(config.nogui) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val memoryInfo = remember { context.getDeviceMemoryInfo() }

    val jarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            jarPath = it.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "服务器配置",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // 服务器名称
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("服务器名称") },
            leadingIcon = { Icon(Icons.Filled.Label, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // JAR 文件选择
        OutlinedTextField(
            value = jarPath.substringAfterLast("/").ifEmpty { jarPath },
            onValueChange = {},
            label = { Text("服务器 JAR 文件") },
            leadingIcon = { Icon(Icons.Filled.InsertDriveFile, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { jarPicker.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = "选择文件")
                }
            },
            supportingText = {
                if (jarPath.isNotBlank()) {
                    Text(
                        jarPath,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        // 内存分配 — 滑块 + 步进器
        MemoryAllocationCard(
            memoryInfo = memoryInfo,
            allocatedMemory = allocatedMemory,
            onMemoryChange = { allocatedMemory = it }
        )

        // 网络配置
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "网络",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() } },
                    label = { Text("服务器端口") },
                    leadingIcon = { Icon(Icons.Filled.Dns, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // 高级选项
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "高级",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = extraArgs,
                    onValueChange = { extraArgs = it },
                    label = { Text("JVM 参数") },
                    supportingText = { Text("额外的 Java 虚拟机启动参数") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("无 GUI 模式")
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(checked = nogui, onCheckedChange = { nogui = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("自动重启")
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(checked = autoRestart, onCheckedChange = { autoRestart = it })
                    }
                }
            }
        }

        // 保存按钮
        Button(
            onClick = {
                onConfigSave(
                    config.copy(
                        name = name,
                        jarPath = jarPath,
                        allocatedMemoryMB = allocatedMemory,
                        serverPort = port.toIntOrNull() ?: 25565,
                        additionalArgs = extraArgs,
                        autoRestart = autoRestart,
                        nogui = nogui
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("保存配置", style = MaterialTheme.typography.titleSmall)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * 内存分配卡片 — 仿照截图中的滑块 + 步进器 + 设备内存条
 */
@Composable
private fun MemoryAllocationCard(
    memoryInfo: MemoryInfo,
    allocatedMemory: Int,
    onMemoryChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "内存分配",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "控制给 Minecraft 分配多少内存",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 第一行：滑块 + 步进器
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 滑块
                Slider(
                    value = allocatedMemory.toFloat(),
                    onValueChange = { onMemoryChange(it.toInt()) },
                    valueRange = 256f..memoryInfo.totalMB.toFloat().coerceAtLeast(1024f),
                    steps = 0,
                    modifier = Modifier.weight(1f)
                )

                // 步进按钮 + 数值显示
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onMemoryChange((allocatedMemory - 256).coerceAtLeast(256)) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.ChevronLeft,
                                contentDescription = "减少",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "${allocatedMemory}MB",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.widthIn(min = 72.dp)
                        )

                        IconButton(
                            onClick = { onMemoryChange((allocatedMemory + 256).coerceAtMost(memoryInfo.totalMB.toInt())) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = "增加",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 设备内存使用情况条
            DeviceMemoryBar(
                memoryInfo = memoryInfo,
                allocatedMemory = allocatedMemory
            )
        }
    }
}

/**
 * 设备内存可视化条：
 * 已使用（深色） + 已分配（半透明主色） + 剩余（背景色）
 */
@Composable
private fun DeviceMemoryBar(
    memoryInfo: MemoryInfo,
    allocatedMemory: Int
) {
    val total = memoryInfo.totalMB.toFloat()
    val usedRatio = memoryInfo.usedMB.toFloat() / total
    val allocatedRatio = allocatedMemory.toFloat() / total

    Column {
        // 进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(MaterialTheme.shapes.small)
        ) {
            // 背景：总内存
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {}

            // 第一层：已使用
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(usedRatio.coerceIn(0f, 1f)),
                color = MaterialTheme.colorScheme.primary
            ) {}

            // 第二层：已分配（覆盖在已使用之上，用更浅的同色系）
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((usedRatio + allocatedRatio).coerceIn(0f, 1f)),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            ) {}
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 文字说明
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "已使用：${memoryInfo.usedMB}MB / ${memoryInfo.totalMB}MB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "已分配：${allocatedMemory}MB",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
