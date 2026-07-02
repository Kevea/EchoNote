package com.echonote.app.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DarkModeOption { SYSTEM, LIGHT, DARK }
enum class BackgroundStyle { SOLID, GRADIENT }

data class ThemeSettings(
    val accentColorIndex: Int = 0,
    val darkMode: DarkModeOption = DarkModeOption.SYSTEM,
    val colorfulCards: Boolean = true,
    val roundedCards: Boolean = true,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.SOLID,
)

class ThemePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<ThemeSettings> = _settings.asStateFlow()

    private fun load(): ThemeSettings = ThemeSettings(
        accentColorIndex = prefs.getInt(KEY_COLOR, 0),
        darkMode = DarkModeOption.entries.getOrElse(prefs.getInt(KEY_MODE, 0)) { DarkModeOption.SYSTEM },
        colorfulCards = prefs.getBoolean(KEY_COLORFUL_CARDS, true),
        roundedCards = prefs.getBoolean(KEY_ROUNDED_CARDS, true),
        backgroundStyle = BackgroundStyle.entries.getOrElse(prefs.getInt(KEY_BACKGROUND, 0)) { BackgroundStyle.SOLID },
    )

    fun setAccentColor(index: Int) {
        prefs.edit().putInt(KEY_COLOR, index).apply()
        _settings.value = _settings.value.copy(accentColorIndex = index)
    }

    fun setDarkMode(mode: DarkModeOption) {
        prefs.edit().putInt(KEY_MODE, mode.ordinal).apply()
        _settings.value = _settings.value.copy(darkMode = mode)
    }

    fun setColorfulCards(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COLORFUL_CARDS, enabled).apply()
        _settings.value = _settings.value.copy(colorfulCards = enabled)
    }

    fun setRoundedCards(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ROUNDED_CARDS, enabled).apply()
        _settings.value = _settings.value.copy(roundedCards = enabled)
    }

    fun setBackgroundStyle(style: BackgroundStyle) {
        prefs.edit().putInt(KEY_BACKGROUND, style.ordinal).apply()
        _settings.value = _settings.value.copy(backgroundStyle = style)
    }

    companion object {
        private const val PREFS_NAME = "echonote_settings"
        private const val KEY_COLOR = "accent_color"
        private const val KEY_MODE = "dark_mode"
        private const val KEY_COLORFUL_CARDS = "colorful_cards"
        private const val KEY_ROUNDED_CARDS = "rounded_cards"
        private const val KEY_BACKGROUND = "background_style"

        @Volatile
        private var instance: ThemePreferences? = null

        fun getInstance(context: Context): ThemePreferences =
            instance ?: synchronized(this) {
                instance ?: ThemePreferences(context).also { instance = it }
            }
    }
}
