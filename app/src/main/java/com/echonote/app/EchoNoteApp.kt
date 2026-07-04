package com.echonote.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.echonote.app.data.NoteRepository
import com.echonote.app.util.ReminderReceiver
import com.echonote.app.util.ThemePreferences
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class EchoNoteApp : Application() {
    val repository: NoteRepository by lazy { NoteRepository.getInstance(this) }
    val themePreferences: ThemePreferences by lazy { ThemePreferences.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderReceiver.CHANNEL_ID,
                "Erinnerungen",
                NotificationManager.IMPORTANCE_HIGH,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
