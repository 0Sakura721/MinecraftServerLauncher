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
        private val KEY_SERVER_NAME = stringPreferencesKey("server_name")
        private val KEY_JAR_PATH = stringPreferencesKey("jar_path")
        private val KEY_ALLOCATED_MEMORY = intPreferencesKey("allocated_memory_mb")
        // 保留旧键用于迁移
        private val KEY_MAX_RAM = intPreferencesKey("max_ram_mb")
        private val KEY_MIN_RAM = intPreferencesKey("min_ram_mb")
        private val KEY_PORT = intPreferencesKey("server_port")
        private val KEY_EXTRA_ARGS = stringPreferencesKey("extra_args")
        private val KEY_AUTO_RESTART = booleanPreferencesKey("auto_restart")
        private val KEY_NOGUI = booleanPreferencesKey("nogui")
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
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.fromKey(prefs[KEY_THEME] ?: "dark")
    }

    val serverConfig: Flow<ServerConfig> = context.dataStore.data.map { prefs ->
        val allocated = prefs[KEY_ALLOCATED_MEMORY]
            ?: prefs[KEY_MAX_RAM]  // 从旧版 maxRamMB 迁移
            ?: 2048

        ServerConfig(
            name = prefs[KEY_SERVER_NAME] ?: "Minecraft Server",
            jarPath = prefs[KEY_JAR_PATH] ?: "",
            allocatedMemoryMB = allocated,
            serverPort = prefs[KEY_PORT] ?: 25565,
            additionalArgs = prefs[KEY_EXTRA_ARGS] ?: "-XX:+UseG1GC",
            autoRestart = prefs[KEY_AUTO_RESTART] ?: false,
            nogui = prefs[KEY_NOGUI] ?: true,
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
            backupOnStop = prefs[KEY_BACKUP_ON_STOP] ?: false
        )
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[KEY_THEME] = mode.key }
    }

    suspend fun saveServerConfig(config: ServerConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_NAME] = config.name
            prefs[KEY_JAR_PATH] = config.jarPath
            prefs[KEY_ALLOCATED_MEMORY] = config.allocatedMemoryMB
            prefs[KEY_PORT] = config.serverPort
            prefs[KEY_EXTRA_ARGS] = config.additionalArgs
            prefs[KEY_AUTO_RESTART] = config.autoRestart
            prefs[KEY_NOGUI] = config.nogui
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
        }
    }

    suspend fun setJarPath(path: String) {
        context.dataStore.edit { prefs -> prefs[KEY_JAR_PATH] = path }
    }
}
