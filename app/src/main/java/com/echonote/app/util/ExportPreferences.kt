package com.echonote.app.util

import android.content.Context
import com.echonote.app.data.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class ExportFormat { MARKDOWN, TEXT }

val ExportFormat.fileExtension: String
    get() = when (this) {
        ExportFormat.MARKDOWN -> "md"
        ExportFormat.TEXT -> "txt"
    }

val ExportFormat.mimeType: String
    get() = when (this) {
        ExportFormat.MARKDOWN -> "text/markdown"
        ExportFormat.TEXT -> "text/plain"
    }

data class ExportSettings(
    val format: ExportFormat = ExportFormat.MARKDOWN,
    // SAF tree Uri.toString(), null = not picked
    val folderUri: String? = null,
    val autoExportEnabled: Boolean = false,
    // surfaced as a warning banner in Settings when the last background export failed
    val lastExportFailed: Boolean = false,
)

class ExportPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<ExportSettings> = _settings.asStateFlow()

    private fun load(): ExportSettings = ExportSettings(
        format = ExportFormat.entries.getOrElse(prefs.getInt(KEY_FORMAT, 0)) { ExportFormat.MARKDOWN },
        folderUri = prefs.getString(KEY_FOLDER_URI, null),
        autoExportEnabled = prefs.getBoolean(KEY_AUTO_EXPORT, false),
        lastExportFailed = prefs.getBoolean(KEY_LAST_FAILED, false),
    )

    fun setFormat(format: ExportFormat) {
        prefs.edit().putInt(KEY_FORMAT, format.ordinal).apply()
        _settings.value = _settings.value.copy(format = format)
    }

    // Picking a folder implicitly (re-)enables auto-export; clearing it disables auto-export too,
    // since the toggle is meaningless (and disabled in the UI) without a folder.
    fun setFolderUri(uri: String?) {
        val autoExport = uri != null
        prefs.edit()
            .putString(KEY_FOLDER_URI, uri)
            .putBoolean(KEY_AUTO_EXPORT, autoExport)
            .apply()
        _settings.value = _settings.value.copy(folderUri = uri, autoExportEnabled = autoExport)
    }

    fun setAutoExportEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_EXPORT, enabled).apply()
        _settings.value = _settings.value.copy(autoExportEnabled = enabled)
    }

    fun setLastExportFailed(failed: Boolean) {
        if (_settings.value.lastExportFailed == failed) return
        prefs.edit().putBoolean(KEY_LAST_FAILED, failed).apply()
        _settings.value = _settings.value.copy(lastExportFailed = failed)
    }

    // Resolves the file name to (re-)export a note under: stable per note id, so repeated
    // exports (e.g. after tag edits) overwrite the same file instead of creating duplicates,
    // even if the note's title changes later.
    fun resolveExportFileName(note: Note, format: ExportFormat): String {
        prefs.getString(fileNameKey(note.id), null)?.let { return it }
        val timestamp = Instant.ofEpochMilli(note.createdAt).atZone(ZoneId.systemDefault()).format(FILE_TIMESTAMP_FORMAT)
        val name = "$timestamp-${NoteExporter.sanitizeFileName(note.title)}.${format.fileExtension}"
        prefs.edit().putString(fileNameKey(note.id), name).apply()
        return name
    }

    private fun fileNameKey(noteId: Long) = "export_filename_$noteId"

    companion object {
        private const val PREFS_NAME = "echonote_export_settings"
        private const val KEY_FORMAT = "format"
        private const val KEY_FOLDER_URI = "folder_uri"
        private const val KEY_AUTO_EXPORT = "auto_export_enabled"
        private const val KEY_LAST_FAILED = "last_export_failed"
        private val FILE_TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

        @Volatile
        private var instance: ExportPreferences? = null

        fun getInstance(context: Context): ExportPreferences =
            instance ?: synchronized(this) {
                instance ?: ExportPreferences(context).also { instance = it }
            }
    }
}
