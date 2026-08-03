package com.plainvoice.app.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FolderSyncMode { COPY, MOVE }

// Per-folder "keep this folder synced" setting: which Plainvoice folders auto-export notes to the
// SAF export folder (see ExportPreferences) as soon as a note lands there, and whether that's a
// copy or a move. Keyed by folder id in SharedPreferences rather than a Folder column, so this
// doesn't need a Room schema migration - same pattern as TagColorPreferences.
class FolderSyncPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _modes = MutableStateFlow(load())
    val modes: StateFlow<Map<Long, FolderSyncMode>> = _modes.asStateFlow()

    private fun load(): Map<Long, FolderSyncMode> =
        prefs.all.mapNotNull { (key, value) ->
            val folderId = key.removePrefix(KEY_PREFIX).toLongOrNull()?.takeIf { key.startsWith(KEY_PREFIX) }
                ?: return@mapNotNull null
            val mode = (value as? Int)?.let { FolderSyncMode.entries.getOrNull(it) } ?: return@mapNotNull null
            folderId to mode
        }.toMap()

    fun modeFor(folderId: Long): FolderSyncMode? = _modes.value[folderId]

    // mode == null disables auto-sync for the folder again.
    fun setMode(folderId: Long, mode: FolderSyncMode?) {
        val key = KEY_PREFIX + folderId
        prefs.edit().apply {
            if (mode == null) remove(key) else putInt(key, mode.ordinal)
        }.apply()
        _modes.value = if (mode == null) _modes.value - folderId else _modes.value + (folderId to mode)
    }

    companion object {
        private const val PREFS_NAME = "plainvoice_folder_sync"
        private const val KEY_PREFIX = "mode_"

        @Volatile
        private var instance: FolderSyncPreferences? = null

        fun getInstance(context: Context): FolderSyncPreferences =
            instance ?: synchronized(this) {
                instance ?: FolderSyncPreferences(context).also { instance = it }
            }
    }
}
