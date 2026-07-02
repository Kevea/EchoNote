package com.echonote.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.echonote.app.EchoNoteApp
import com.echonote.app.util.DarkModeOption

@Composable
fun EchoNoteTheme(
    content: @Composable () -> Unit,
) {
    val app = LocalContext.current.applicationContext as EchoNoteApp
    val settings by app.themePreferences.settings.collectAsState()
    val accent = NoteTagColors.getOrElse(settings.accentColorIndex) { NoteTagColors.first() }

    val darkTheme = when (settings.darkMode) {
        DarkModeOption.SYSTEM -> isSystemInDarkTheme()
        DarkModeOption.LIGHT -> false
        DarkModeOption.DARK -> true
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            secondary = BrandSecondary,
            background = BrandBackgroundDark,
            surface = BrandSurfaceDark,
        )
    } else {
        lightColorScheme(
            primary = accent,
            secondary = BrandSecondary,
            background = BrandBackgroundLight,
            surface = BrandSurfaceLight,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EchoNoteTypography,
        content = content,
    )
}
