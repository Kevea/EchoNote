package com.plainvoice.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.plainvoice.app.data.NoteRepository
import com.plainvoice.app.util.ExportPreferences
import com.plainvoice.app.util.FolderSyncPreferences
import com.plainvoice.app.util.ReminderReceiver
import com.plainvoice.app.util.TagColorPreferences
import com.plainvoice.app.util.ThemePreferences
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PlainvoiceApp : Application() {
    val repository: NoteRepository by lazy { NoteRepository.getInstance(this) }
    val themePreferences: ThemePreferences by lazy { ThemePreferences.getInstance(this) }
    val exportPreferences: ExportPreferences by lazy { ExportPreferences.getInstance(this) }
    val tagColorPreferences: TagColorPreferences by lazy { TagColorPreferences.getInstance(this) }
    val folderSyncPreferences: FolderSyncPreferences by lazy { FolderSyncPreferences.getInstance(this) }

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
