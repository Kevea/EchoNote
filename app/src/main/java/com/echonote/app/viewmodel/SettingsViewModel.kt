package com.echonote.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echonote.app.EchoNoteApp
import com.echonote.app.data.Folder
import com.echonote.app.util.BackgroundStyle
import com.echonote.app.util.DarkModeOption
import com.echonote.app.util.ExportFormat
import com.echonote.app.util.ExportSettings
import com.echonote.app.util.FolderSyncMode
import com.echonote.app.util.FontSizeOption
import com.echonote.app.util.NoteExporter
import com.echonote.app.util.ReminderScheduler
import com.echonote.app.util.ThemeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = (application as EchoNoteApp).themePreferences
    private val exportPreferences = (application as EchoNoteApp).exportPreferences
    private val folderSyncPreferences = (application as EchoNoteApp).folderSyncPreferences
    private val repository = (application as EchoNoteApp).repository

    val settings: StateFlow<ThemeSettings> = preferences.settings
    val exportSettings: StateFlow<ExportSettings> = exportPreferences.settings
    val folderSyncModes: StateFlow<Map<Long, FolderSyncMode>> = folderSyncPreferences.modes

    val folders: StateFlow<List<Folder>> = repository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _syncingFolderIds = MutableStateFlow<Set<Long>>(emptySet())
    val syncingFolderIds: StateFlow<Set<Long>> = _syncingFolderIds.asStateFlow()

    private val _syncResultMessage = MutableStateFlow<String?>(null)
    val syncResultMessage: StateFlow<String?> = _syncResultMessage.asStateFlow()

    fun setAccentColor(index: Int) = preferences.setAccentColor(index)

    fun setBaseColor(index: Int) = preferences.setBaseColor(index)

    fun setDarkMode(mode: DarkModeOption) = preferences.setDarkMode(mode)

    fun setColorfulCards(enabled: Boolean) = preferences.setColorfulCards(enabled)

    fun setRoundedCards(enabled: Boolean) = preferences.setRoundedCards(enabled)

    fun setBackgroundStyle(style: BackgroundStyle) = preferences.setBackgroundStyle(style)

    fun setFontSize(option: FontSizeOption) = preferences.setFontSize(option)

    fun setTextColor(index: Int?) = preferences.setTextColor(index)

    fun setExportFormat(format: ExportFormat) = exportPreferences.setFormat(format)

    fun setExportFolder(uri: String?) = exportPreferences.setFolderUri(uri)

    fun clearSyncResultMessage() {
        _syncResultMessage.value = null
    }

    fun setFolderSyncMode(folderId: Long, mode: FolderSyncMode?) = folderSyncPreferences.setMode(folderId, mode)

    // A deliberate, per-folder action (not a background trigger): copies or moves every note
    // currently in `folder` into the SAF-picked sync folder. Only notes that exported
    // successfully are deleted on move, so a failed export never loses a note.
    fun syncFolder(folder: Folder, move: Boolean) {
        val folderUri = exportPreferences.settings.value.folderUri ?: return
        _syncingFolderIds.value += folder.id
        viewModelScope.launch {
            var succeeded = 0
            var failed = 0
            val format = exportPreferences.settings.value.format
            repository.getNotesInFolder(folder.id).forEach { note ->
                val fileName = exportPreferences.resolveExportFileName(note, format)
                val result = NoteExporter.exportToFolder(
                    getApplication(), note, Uri.parse(folderUri), format, fileName,
                )
                if (result.isSuccess) {
                    succeeded++
                    if (move) {
                        if (note.reminderAt != null) ReminderScheduler.cancel(getApplication(), note.id)
                        repository.delete(note)
                    }
                } else {
                    failed++
                }
            }
            _syncResultMessage.value = "$succeeded synchronisiert" + if (failed > 0) ", $failed fehlgeschlagen" else ""
            _syncingFolderIds.value -= folder.id
        }
    }
}
