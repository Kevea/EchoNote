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
)

class ExportPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<ExportSettings> = _settings.asStateFlow()

    private fun load(): ExportSettings = ExportSettings(
        format = ExportFormat.entries.getOrElse(prefs.getInt(KEY_FORMAT, 0)) { ExportFormat.MARKDOWN },
        folderUri = prefs.getString(KEY_FOLDER_URI, null),
    )

    fun setFormat(format: ExportFormat) {
        prefs.edit().putInt(KEY_FORMAT, format.ordinal).apply()
        _settings.value = _settings.value.copy(format = format)
    }

    fun setFolderUri(uri: String?) {
        prefs.edit().putString(KEY_FOLDER_URI, uri).apply()
        _settings.value = _settings.value.copy(folderUri = uri)
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
        private val FILE_TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

        @Volatile
        private var instance: ExportPreferences? = null

        fun getInstance(context: Context): ExportPreferences =
            instance ?: synchronized(this) {
                instance ?: ExportPreferences(context).also { instance = it }
            }
    }
}
