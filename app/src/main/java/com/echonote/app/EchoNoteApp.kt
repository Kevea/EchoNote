package com.echonote.app

import android.app.Application
import com.echonote.app.data.NoteRepository
import com.echonote.app.util.ThemePreferences

class EchoNoteApp : Application() {
    val repository: NoteRepository by lazy { NoteRepository.getInstance(this) }
    val themePreferences: ThemePreferences by lazy { ThemePreferences.getInstance(this) }
}
