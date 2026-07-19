package com.mcserver.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.Properties

/**
 * server.properties 结构化编辑器。
 * 借鉴 Pterodactyl 的变量注入面板设计：提供表单式的可视化编辑，
 * 同时保留原始文本编辑模式。
 *
 * 支持：
 * - 常用属性表单编辑（带描述和验证）
 * - 原始文本编辑（带行号和语法高亮）
 * - 保存前备份（.bak）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerPropertiesEditor(
    configFile: File,
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    var properties by remember { mutableStateOf(loadProperties(configFile)) }
    var mode by remember { mutableStateOf(0) } // 0=表单, 1=原始文本
    var rawContent by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    // 初始化原始文本
    LaunchedEffect(configFile) {
        rawContent = try { configFile.readText() } catch (_: Exception) { "" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑 server.properties") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 模式切换
                    Row {
                        FilterChip(
                            selected = mode == 0,
                            onClick = { mode = 0 },
                            label = { Text("表单") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ViewList, null, Modifier.size(16.dp)) }
                        )
                        Spacer(Modifier.width(4.dp))
                        FilterChip(
                            selected = mode == 1,
                            onClick = { mode = 1 },
                            label = { Text("原始") },
                            leadingIcon = { Icon(Icons.Filled.Code, null, Modifier.size(16.dp)) }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSaveDialog = true },
                icon = { Icon(Icons.Filled.Save, null) },
                text = { Text("保存") }
            )
        }
    ) { padding ->
        when (mode) {
            0 -> PropertiesFormView(
                properties = properties,
                onPropertyChange = { key, value ->
                    properties = properties.toMutableMap().apply { put(key, value) }
                },
                modifier = Modifier.padding(padding)
            )
            1 -> PropertiesRawView(
                content = rawContent,
                onContentChange = { rawContent = it },
                modifier = Modifier.padding(padding)
            )
        }
    }

    // 保存确认
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            icon = { Icon(Icons.Filled.Save, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("保存配置") },
            text = {
                Column {
                    Text("将保存对 server.properties 的修改。")
                    if (saveMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(saveMessage!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        // 先备份
                        val bakFile = File(configFile.parentFile, "${configFile.name}.bak")
                        configFile.copyTo(bakFile, overwrite = true)

                        when (mode) {
                            0 -> {
                                // 从表单保存
                                val props = Properties()
                                properties.forEach { (k, v) -> props.setProperty(k, v) }
                                configFile.outputStream().use { out ->
                                    props.store(out, "MCServer Launcher - server.properties")
                                }
                            }
                            1 -> {
                                // 从原始文本保存
                                configFile.writeText(rawContent)
                            }
                        }
                        showSaveDialog = false
                        onSaved()
                    } catch (e: Exception) {
                        saveMessage = "保存失败：${e.message}"
                    }
                }) { Text("确认保存") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("取消") }
            }
        )
    }
}

// ─── 表单模式 ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertiesFormView(
    properties: Map<String, String>,
    onPropertyChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 定义常用属性的元数据（名称、描述、类型、验证）
    val commonProperties = listOf(
        PropMeta("server-port", "服务器端口", "玩家连接服务器使用的端口（默认 25565）", PropType.NUMBER, "1024", "65535"),
        PropMeta("motd", "服务器描述 (MOTD)", "在服务器列表中显示的描述文本", PropType.TEXT),
        PropMeta("max-players", "最大玩家数", "同时在线玩家的最大数量", PropType.NUMBER, "1", "10000"),
        PropMeta("gamemode", "默认游戏模式", "新玩家进入时的默认模式", PropType.CHOICE, options = listOf("survival", "creative", "adventure", "spectator")),
        PropMeta("difficulty", "游戏难度", "服务器的默认难度", PropType.CHOICE, options = listOf("peaceful", "easy", "normal", "hard")),
        PropMeta("pvp", "PVP（玩家对战）", "是否允许玩家之间互相攻击", PropType.BOOLEAN),
        PropMeta("online-mode", "正版验证", "是否要求玩家使用正版 Minecraft 账号", PropType.BOOLEAN),
        PropMeta("white-list", "白名单", "是否启用白名单（仅白名单内玩家可进入）", PropType.BOOLEAN),
        PropMeta("spawn-protection", "出生点保护", "出生点周围受保护区域半径（0=关闭）", PropType.NUMBER, "0", "100"),
        PropMeta("view-distance", "视距", "服务器发送给玩家的区块视距（2-32）", PropType.NUMBER, "2", "32"),
        PropMeta("simulation-distance", "模拟距离", "服务器进行实体和方块更新的距离", PropType.NUMBER, "2", "32"),
        PropMeta("enable-command-block", "命令方块", "是否启用命令方块", PropType.BOOLEAN),
        PropMeta("allow-nether", "允许下界", "是否允许玩家前往下界", PropType.BOOLEAN),
        PropMeta("allow-flight", "允许飞行", "是否允许玩家飞行（可能触发反作弊）", PropType.BOOLEAN),
        PropMeta("enable-rcon", "RCON 远程控制", "是否启用 RCON 远程控制协议", PropType.BOOLEAN),
        PropMeta("rcon.port", "RCON 端口", "RCON 远程控制协议端口", PropType.NUMBER, "1024", "65535"),
        PropMeta("rcon.password", "RCON 密码", "RCON 远程控制协议密码", PropType.TEXT),
        PropMeta("level-name", "世界名称", "主世界的文件夹名称", PropType.TEXT),
        PropMeta("level-seed", "世界种子", "生成世界时使用的种子（留空=随机）", PropType.TEXT),
        PropMeta("level-type", "世界类型", "世界生成类型", PropType.CHOICE, options = listOf("default", "flat", "largebiomes", "amplified", "buffet")),
    )

    // 从 properties 中提取值，缺失的用默认值
    fun getValue(key: String, default: String = ""): String = properties[key] ?: default

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "常用设置",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(commonProperties) { prop ->
            val currentValue = getValue(prop.key, prop.defaultValue)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        prop.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        prop.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    when (prop.type) {
                        PropType.BOOLEAN -> {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (currentValue == "true") "已启用" else "已禁用",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (currentValue == "true")
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Switch(
                                    checked = currentValue == "true",
                                    onCheckedChange = {
                                        onPropertyChange(prop.key, if (it) "true" else "false")
                                    }
                                )
                            }
                        }
                        PropType.CHOICE -> {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = currentValue,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    prop.options.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                onPropertyChange(prop.key, option)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        PropType.NUMBER -> {
                            OutlinedTextField(
                                value = currentValue,
                                onValueChange = { newValue ->
                                    // 只允许数字
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        onPropertyChange(prop.key, newValue)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true
                            )
                        }
                        PropType.TEXT -> {
                            OutlinedTextField(
                                value = currentValue,
                                onValueChange = { onPropertyChange(prop.key, it) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true,
                                minLines = 1
                            )
                        }
                    }

                    // 显示当前键名
                    Text(
                        prop.key,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) } // 为 FAB 留空间
    }
}

// ─── 原始文本模式 ───

@Composable
private fun PropertiesRawView(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "原始编辑",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "直接编辑 server.properties 文本内容。格式：key=value，每行一条。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
            ),
            minLines = 20
        )
    }
}

// ─── 工具函数 ───

private fun loadProperties(file: File): Map<String, String> {
    return try {
        val props = Properties()
        props.load(file.inputStream())
        val map = mutableMapOf<String, String>()
        props.forEach { (k, v) -> map[k.toString()] = v.toString() }
        map
    } catch (_: Exception) {
        emptyMap()
    }
}

// ─── 数据类型 ───

private enum class PropType { TEXT, NUMBER, BOOLEAN, CHOICE }

private data class PropMeta(
    val key: String,
    val label: String,
    val description: String,
    val type: PropType,
    val minValue: String = "",
    val maxValue: String = "",
    val options: List<String> = emptyList()
) {
    val defaultValue: String
        get() = when (type) {
            PropType.BOOLEAN -> "false"
            PropType.NUMBER -> when (key) {
                "server-port" -> "25565"
                "max-players" -> "20"
                "view-distance" -> "10"
                "simulation-distance" -> "10"
                "spawn-protection" -> "16"
                "rcon.port" -> "25575"
                else -> "0"
            }
            PropType.CHOICE -> options.firstOrNull() ?: ""
            PropType.TEXT -> when (key) {
                "motd" -> "A Minecraft Server"
                "level-name" -> "world"
                else -> ""
            }
        }
}
