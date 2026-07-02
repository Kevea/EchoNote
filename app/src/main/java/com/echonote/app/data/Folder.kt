package com.echonote.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorIndex: Int = 0,
    val sortOrder: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
)
