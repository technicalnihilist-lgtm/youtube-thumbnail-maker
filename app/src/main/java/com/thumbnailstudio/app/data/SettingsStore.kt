package com.thumbnailstudio.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Lightweight preferences backed by SharedPreferences (no extra libraries). */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("thumbnail_studio_settings", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(prefs.getString("theme_mode", "system") ?: "system")
        private set
    var showGrid by mutableStateOf(prefs.getBoolean("show_grid", false))
        private set
    var showSafeArea by mutableStateOf(prefs.getBoolean("show_safe_area", true))
        private set
    var snapToCenter by mutableStateOf(prefs.getBoolean("snap_center", true))
        private set
    var exportFormat by mutableStateOf(prefs.getString("export_format", "PNG") ?: "PNG")
        private set
    var jpegQuality by mutableStateOf(prefs.getInt("jpeg_quality", 92))
        private set
    var lastUpdateCheck by mutableStateOf(prefs.getLong("last_update_check", 0L))
        private set

    fun updateThemeMode(mode: String) {
        themeMode = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun updateShowGrid(value: Boolean) {
        showGrid = value
        prefs.edit().putBoolean("show_grid", value).apply()
    }

    fun updateShowSafeArea(value: Boolean) {
        showSafeArea = value
        prefs.edit().putBoolean("show_safe_area", value).apply()
    }

    fun updateSnapToCenter(value: Boolean) {
        snapToCenter = value
        prefs.edit().putBoolean("snap_center", value).apply()
    }

    fun updateExportFormat(value: String) {
        exportFormat = value
        prefs.edit().putString("export_format", value).apply()
    }

    fun updateJpegQuality(value: Int) {
        jpegQuality = value
        prefs.edit().putInt("jpeg_quality", value).apply()
    }

    fun markUpdateChecked(timestamp: Long = System.currentTimeMillis()) {
        lastUpdateCheck = timestamp
        prefs.edit().putLong("last_update_check", timestamp).apply()
    }

    fun darkTheme(systemDark: Boolean): Boolean = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
}
