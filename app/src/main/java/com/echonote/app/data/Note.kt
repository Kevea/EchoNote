package com.echonote.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val audioFilePath: String? = null,
    val audioDurationMs: Long = 0,
    val amplitudes: String = "",
    val tags: String = "",
    val folderId: Long? = null,
    val isPinned: Boolean = false,
    val colorTag: Int = 0,
    val sortOrder: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val tagList: List<String>
        get() = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val amplitudeList: List<Int>
        get() = if (amplitudes.isBlank()) emptyList() else amplitudes.split(",").mapNotNull { it.toIntOrNull() }
}
