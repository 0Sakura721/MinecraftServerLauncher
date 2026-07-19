package com.mcserver.launcher.ui.theme

enum class ThemeMode(val key: String, val label: String, val description: String) {
    LIGHT("light", "明亮", "白色主题，适合白天使用"),
    DARK("dark", "暗色", "深色主题，护眼舒适"),
    AMOLED("amoled", "AMOLED 省电", "纯黑背景，极致省电");

    companion object {
        fun fromKey(key: String): ThemeMode =
            entries.firstOrNull { it.key == key } ?: DARK
    }
}
