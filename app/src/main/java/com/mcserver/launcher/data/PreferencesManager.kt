package com.mcserver.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mcserver.launcher.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mc_server_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
        private val KEY_SERVER_NAME = stringPreferencesKey("server_name")
        private val KEY_JAR_PATH = stringPreferencesKey("jar_path")
        private val KEY_JAVA_VERSION = intPreferencesKey("java_version")
        private val KEY_ALLOCATED_MEMORY = intPreferencesKey("allocated_memory_mb")
        // 保留旧键用于迁移
        private val KEY_MAX_RAM = intPreferencesKey("max_ram_mb")
        private val KEY_MIN_RAM = intPreferencesKey("min_ram_mb")
        private val KEY_PORT = intPreferencesKey("server_port")
        private val KEY_EXTRA_ARGS = stringPreferencesKey("extra_args")
        private val KEY_AUTO_RESTART = booleanPreferencesKey("auto_restart")
        private val KEY_NOGUI = booleanPreferencesKey("nogui")
        // 启动方式
        private val KEY_STARTUP_MODE = stringPreferencesKey("startup_mode")
        private val KEY_SH_SCRIPT_PATH = stringPreferencesKey("sh_script_path")
        private val KEY_SH_MAX_RAM = intPreferencesKey("sh_max_ram")
        private val KEY_SH_MIN_RAM = intPreferencesKey("sh_min_ram")
        // server.properties 相关
        private val KEY_MOTD = stringPreferencesKey("motd")
        private val KEY_MAX_PLAYERS = intPreferencesKey("max_players")
        private val KEY_GAMEMODE = stringPreferencesKey("gamemode")
        private val KEY_DIFFICULTY = stringPreferencesKey("difficulty")
        private val KEY_PVP = booleanPreferencesKey("pvp")
        private val KEY_ONLINE_MODE = booleanPreferencesKey("online_mode")
        private val KEY_WHITE_LIST = booleanPreferencesKey("white_list")
        private val KEY_SPAWN_PROTECTION = intPreferencesKey("spawn_protection")
        private val KEY_VIEW_DISTANCE = intPreferencesKey("view_distance")
        // 自动重启保护
        private val KEY_MAX_RESTARTS = intPreferencesKey("max_restarts")
        private val KEY_RESTART_COOLDOWN = intPreferencesKey("restart_cooldown")
        private val KEY_BACKUP_ON_STOP = booleanPreferencesKey("backup_on_stop")
        // RCON 配置
        private val KEY_RCON_ENABLED = booleanPreferencesKey("rcon_enabled")
        private val KEY_RCON_PASSWORD = stringPreferencesKey("rcon_password")
        private val KEY_RCON_PORT = intPreferencesKey("rcon_port")
        // 外观自定义
        private val KEY_BG_TYPE = stringPreferencesKey("bg_type")
        private val KEY_BG_PATH = stringPreferencesKey("bg_path")
        private val KEY_BG_BLUR = floatPreferencesKey("bg_blur")
        private val KEY_BG_DARK_MASK = floatPreferencesKey("bg_dark_mask")
        private val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        private val KEY_CORNER_RADIUS = intPreferencesKey("corner_radius")
        private val KEY_LAYOUT_DENSITY = stringPreferencesKey("layout_density")
        private val KEY_FONT_SIZE = stringPreferencesKey("font_size")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.fromKey(prefs[KEY_THEME] ?: "dark")
    }

    val setupCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SETUP_COMPLETED] ?: false
    }

    // ── 外观偏好 ──
    val backgroundType: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BG_TYPE] ?: "none"
    }
    val backgroundPath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BG_PATH] ?: ""
    }
    val backgroundBlur: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_BG_BLUR] ?: 0f
    }
    val backgroundDarkMask: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_BG_DARK_MASK] ?: 0.3f
    }
    val accentColor: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACCENT_COLOR] ?: "default"
    }
    val cornerRadius: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_CORNER_RADIUS] ?: 12
    }
    val layoutDensity: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAYOUT_DENSITY] ?: "normal"
    }
    val fontSize: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_FONT_SIZE] ?: "medium"
    }

    // ── 外观保存 ──
    suspend fun setBackgroundType(type: String) {
        context.dataStore.edit { prefs -> prefs[KEY_BG_TYPE] = type }
    }
    suspend fun setBackgroundPath(path: String) {
        context.dataStore.edit { prefs -> prefs[KEY_BG_PATH] = path }
    }
    suspend fun setBackgroundBlur(value: Float) {
        context.dataStore.edit { prefs -> prefs[KEY_BG_BLUR] = value }
    }
    suspend fun setBackgroundDarkMask(value: Float) {
        context.dataStore.edit { prefs -> prefs[KEY_BG_DARK_MASK] = value }
    }
    suspend fun setAccentColor(color: String) {
        context.dataStore.edit { prefs -> prefs[KEY_ACCENT_COLOR] = color }
    }
    suspend fun setCornerRadius(radius: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_CORNER_RADIUS] = radius }
    }
    suspend fun setLayoutDensity(density: String) {
        context.dataStore.edit { prefs -> prefs[KEY_LAYOUT_DENSITY] = density }
    }
    suspend fun setFontSize(size: String) {
        context.dataStore.edit { prefs -> prefs[KEY_FONT_SIZE] = size }
    }

    val serverConfig: Flow<ServerConfig> = context.dataStore.data.map { prefs ->
        val allocated = prefs[KEY_ALLOCATED_MEMORY]
            ?: prefs[KEY_MAX_RAM]  // 从旧版 maxRamMB 迁移
            ?: 2048

        ServerConfig(
            name = prefs[KEY_SERVER_NAME] ?: "Minecraft Server",
            jarPath = prefs[KEY_JAR_PATH] ?: "",
            javaVersion = prefs[KEY_JAVA_VERSION] ?: 21,
            allocatedMemoryMB = allocated,
            serverPort = prefs[KEY_PORT] ?: 25565,
            additionalArgs = prefs[KEY_EXTRA_ARGS] ?: "-XX:+UseG1GC",
            autoRestart = prefs[KEY_AUTO_RESTART] ?: false,
            nogui = prefs[KEY_NOGUI] ?: true,
            startupMode = try { StartupMode.valueOf(prefs[KEY_STARTUP_MODE] ?: "DIRECT_JAR") } catch (_: Exception) { StartupMode.DIRECT_JAR },
            shScriptPath = prefs[KEY_SH_SCRIPT_PATH] ?: "",
            shMaxRamMB = prefs[KEY_SH_MAX_RAM] ?: 4096,
            shMinRamMB = prefs[KEY_SH_MIN_RAM] ?: 2048,
            motd = prefs[KEY_MOTD] ?: "A Minecraft Server",
            maxPlayers = prefs[KEY_MAX_PLAYERS] ?: 20,
            gamemode = prefs[KEY_GAMEMODE] ?: "survival",
            difficulty = prefs[KEY_DIFFICULTY] ?: "easy",
            pvp = prefs[KEY_PVP] ?: true,
            onlineMode = prefs[KEY_ONLINE_MODE] ?: true,
            whiteList = prefs[KEY_WHITE_LIST] ?: false,
            spawnProtection = prefs[KEY_SPAWN_PROTECTION] ?: 16,
            viewDistance = prefs[KEY_VIEW_DISTANCE] ?: 10,
            maxRestarts = prefs[KEY_MAX_RESTARTS] ?: 3,
            restartCooldownSec = prefs[KEY_RESTART_COOLDOWN] ?: 5,
            backupOnStop = prefs[KEY_BACKUP_ON_STOP] ?: false,
            rconEnabled = prefs[KEY_RCON_ENABLED] ?: true,
            rconPassword = prefs[KEY_RCON_PASSWORD] ?: "",
            rconPort = prefs[KEY_RCON_PORT] ?: 25575
        )
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[KEY_THEME] = mode.key }
    }

    suspend fun setSetupCompleted() {
        context.dataStore.edit { prefs -> prefs[KEY_SETUP_COMPLETED] = true }
    }

    suspend fun saveServerConfig(config: ServerConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_NAME] = config.name
            prefs[KEY_JAR_PATH] = config.jarPath
            prefs[KEY_JAVA_VERSION] = config.javaVersion
            prefs[KEY_ALLOCATED_MEMORY] = config.allocatedMemoryMB
            prefs[KEY_PORT] = config.serverPort
            prefs[KEY_EXTRA_ARGS] = config.additionalArgs
            prefs[KEY_AUTO_RESTART] = config.autoRestart
            prefs[KEY_NOGUI] = config.nogui
            prefs[KEY_STARTUP_MODE] = config.startupMode.name
            prefs[KEY_SH_SCRIPT_PATH] = config.shScriptPath
            prefs[KEY_SH_MAX_RAM] = config.shMaxRamMB
            prefs[KEY_SH_MIN_RAM] = config.shMinRamMB
            prefs[KEY_MOTD] = config.motd
            prefs[KEY_MAX_PLAYERS] = config.maxPlayers
            prefs[KEY_GAMEMODE] = config.gamemode
            prefs[KEY_DIFFICULTY] = config.difficulty
            prefs[KEY_PVP] = config.pvp
            prefs[KEY_ONLINE_MODE] = config.onlineMode
            prefs[KEY_WHITE_LIST] = config.whiteList
            prefs[KEY_SPAWN_PROTECTION] = config.spawnProtection
            prefs[KEY_VIEW_DISTANCE] = config.viewDistance
            prefs[KEY_MAX_RESTARTS] = config.maxRestarts
            prefs[KEY_RESTART_COOLDOWN] = config.restartCooldownSec
            prefs[KEY_BACKUP_ON_STOP] = config.backupOnStop
            prefs[KEY_RCON_ENABLED] = config.rconEnabled
            prefs[KEY_RCON_PASSWORD] = config.rconPassword
            prefs[KEY_RCON_PORT] = config.rconPort
        }
    }

    suspend fun setJarPath(path: String) {
        context.dataStore.edit { prefs -> prefs[KEY_JAR_PATH] = path }
    }
}
