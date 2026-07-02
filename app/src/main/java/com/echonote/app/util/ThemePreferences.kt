package com.echonote.app.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DarkModeOption { SYSTEM, LIGHT, DARK }

data class ThemeSettings(
    val accentColorIndex: Int = 0,
    val darkMode: DarkModeOption = DarkModeOption.SYSTEM,
)

class ThemePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<ThemeSettings> = _settings.asStateFlow()

    private fun load(): ThemeSettings {
        val color = prefs.getInt(KEY_COLOR, 0)
        val mode = DarkModeOption.entries.getOrElse(prefs.getInt(KEY_MODE, 0)) { DarkModeOption.SYSTEM }
        return ThemeSettings(color, mode)
    }

    fun setAccentColor(index: Int) {
        prefs.edit().putInt(KEY_COLOR, index).apply()
        _settings.value = _settings.value.copy(accentColorIndex = index)
    }

    fun setDarkMode(mode: DarkModeOption) {
        prefs.edit().putInt(KEY_MODE, mode.ordinal).apply()
        _settings.value = _settings.value.copy(darkMode = mode)
    }

    companion object {
        private const val PREFS_NAME = "echonote_settings"
        private const val KEY_COLOR = "accent_color"
        private const val KEY_MODE = "dark_mode"

        @Volatile
        private var instance: ThemePreferences? = null

        fun getInstance(context: Context): ThemePreferences =
            instance ?: synchronized(this) {
                instance ?: ThemePreferences(context).also { instance = it }
            }
    }
}
