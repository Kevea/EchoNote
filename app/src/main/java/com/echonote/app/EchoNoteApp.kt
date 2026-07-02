package com.echonote.app

import android.app.Application
import com.echonote.app.data.NoteRepository

class EchoNoteApp : Application() {
    val repository: NoteRepository by lazy { NoteRepository.getInstance(this) }
}
