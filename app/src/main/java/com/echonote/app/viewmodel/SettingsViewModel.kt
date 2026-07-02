package com.echonote.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.echonote.app.EchoNoteApp
import com.echonote.app.util.BackgroundStyle
import com.echonote.app.util.DarkModeOption
import com.echonote.app.util.ThemeSettings
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = (application as EchoNoteApp).themePreferences

    val settings: StateFlow<ThemeSettings> = preferences.settings

    fun setAccentColor(index: Int) = preferences.setAccentColor(index)

    fun setDarkMode(mode: DarkModeOption) = preferences.setDarkMode(mode)

    fun setColorfulCards(enabled: Boolean) = preferences.setColorfulCards(enabled)

    fun setRoundedCards(enabled: Boolean) = preferences.setRoundedCards(enabled)

    fun setBackgroundStyle(style: BackgroundStyle) = preferences.setBackgroundStyle(style)
}
