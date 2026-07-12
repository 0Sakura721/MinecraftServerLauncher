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
            nogui = prefs[KEY_NOGUI] ?: true
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
        }
    }

    suspend fun setJarPath(path: String) {
        context.dataStore.edit { prefs -> prefs[KEY_JAR_PATH] = path }
    }
}
