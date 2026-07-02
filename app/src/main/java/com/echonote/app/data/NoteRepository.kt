package com.echonote.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class NoteRepository(
    private val context: Context,
    private val dao: NoteDao,
) {
    val audioDir: File by lazy {
        File(context.filesDir, "audio").apply { mkdirs() }
    }

    fun observeAll(): Flow<List<Note>> = dao.observeAll()

    fun search(query: String): Flow<List<Note>> =
        if (query.isBlank()) dao.observeAll() else dao.search(query)

    fun observeById(id: Long): Flow<Note?> = dao.observeById(id)

    fun newAudioFile(): File = File(audioDir, "voice_${UUID.randomUUID()}.m4a")

    suspend fun save(note: Note): Long = withContext(Dispatchers.IO) {
        dao.insert(note)
    }

    suspend fun update(note: Note) = withContext(Dispatchers.IO) {
        dao.update(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(note: Note) = withContext(Dispatchers.IO) {
        note.audioFilePath?.let { path -> File(path).takeIf { it.exists() }?.delete() }
        dao.delete(note)
    }

    companion object {
        @Volatile
        private var instance: NoteRepository? = null

        fun getInstance(context: Context): NoteRepository =
            instance ?: synchronized(this) {
                instance ?: NoteRepository(
                    context.applicationContext,
                    NoteDatabase.getInstance(context).noteDao()
                ).also { instance = it }
            }
    }
}
