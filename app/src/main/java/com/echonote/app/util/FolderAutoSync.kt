package com.echonote.app.util

import android.app.Application
import android.net.Uri
import com.echonote.app.data.Note
import com.echonote.app.data.NoteRepository

// Fire-and-forget export of a note that just landed in (or was moved into) a folder, shared by
// every place that assigns a note's folderId: tagging (auto-folder), the folder-dialog, and
// bulk-move. Only exports if that folder has a FolderSyncMode configured (see
// FolderSyncPreferences) and a SAF export folder is picked; on MOVE the note is only deleted
// after a successful export, so a failed export never loses a note - same rule as the manual
// per-folder Sync button in SettingsViewModel.
object FolderAutoSync {
    suspend fun syncIfEnabled(
        application: Application,
        repository: NoteRepository,
        exportPreferences: ExportPreferences,
        folderSyncPreferences: FolderSyncPreferences,
        note: Note,
    ) {
        val folderId = note.folderId ?: return
        val mode = folderSyncPreferences.modeFor(folderId) ?: return
        val export = exportPreferences.settings.value
        val folderUri = export.folderUri ?: return
        val fileName = exportPreferences.resolveExportFileName(note, export.format)
        val result = NoteExporter.exportToFolder(application, note, Uri.parse(folderUri), export.format, fileName)
        if (result.isSuccess && mode == FolderSyncMode.MOVE) {
            if (note.reminderAt != null) ReminderScheduler.cancel(application, note.id)
            repository.delete(note)
        }
    }
}
