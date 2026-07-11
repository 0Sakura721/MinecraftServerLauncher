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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConfigScreen(
    config: ServerConfig,
    onConfigSave: (ServerConfig) -> Unit
) {
    var name by remember { mutableStateOf(config.name) }
    var jarPath by remember { mutableStateOf(config.jarPath) }
    var maxRam by remember { mutableStateOf(config.maxRamMB.toString()) }
    var minRam by remember { mutableStateOf(config.minRamMB.toString()) }
    var port by remember { mutableStateOf(config.serverPort.toString()) }
    var extraArgs by remember { mutableStateOf(config.additionalArgs) }
    var autoRestart by remember { mutableStateOf(config.autoRestart) }
    var nogui by remember { mutableStateOf(config.nogui) }

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

        // 内存配置
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
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = maxRam,
                        onValueChange = { maxRam = it.filter { c -> c.isDigit() } },
                        label = { Text("最大内存 (MB)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minRam,
                        onValueChange = { minRam = it.filter { c -> c.isDigit() } },
                        label = { Text("最小内存 (MB)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }

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
                        maxRamMB = maxRam.toIntOrNull() ?: 2048,
                        minRamMB = minRam.toIntOrNull() ?: 1024,
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
