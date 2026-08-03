package com.plainvoice.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.plainvoice.app.PlainvoiceApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as PlainvoiceApp
        CoroutineScope(Dispatchers.IO).launch {
            app.repository.getNotesWithReminders().forEach { note ->
                val reminderAt = note.reminderAt
                if (reminderAt != null && reminderAt > System.currentTimeMillis()) {
                    ReminderScheduler.schedule(context, note.id, reminderAt, note.title)
                }
            }
        }
    }
}
