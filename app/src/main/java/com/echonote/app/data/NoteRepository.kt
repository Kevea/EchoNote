package com.echonote.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class NoteRepository(
    private val dao: NoteDao,
    private val folderDao: FolderDao,
) {

    fun observeAll(): Flow<List<Note>> = dao.observeAll()

    fun search(query: String): Flow<List<Note>> =
        if (query.isBlank()) dao.observeAll() else dao.search(query)

    fun observeById(id: Long): Flow<Note?> = dao.observeById(id)

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

    fun observeFolders(): Flow<List<Folder>> = folderDao.observeAll()

    suspend fun createFolder(name: String, colorIndex: Int): Long = withContext(Dispatchers.IO) {
        folderDao.insert(Folder(name = name, colorIndex = colorIndex))
    }

    suspend fun updateFolder(folder: Folder) = withContext(Dispatchers.IO) {
        folderDao.update(folder)
    }

    suspend fun deleteFolder(folder: Folder) = withContext(Dispatchers.IO) {
        dao.clearFolder(folder.id)
        folderDao.delete(folder)
    }

    suspend fun getNotesWithReminders(): List<Note> = withContext(Dispatchers.IO) {
        dao.getNotesWithReminders()
    }

    companion object {
        @Volatile
        private var instance: NoteRepository? = null

        fun getInstance(context: Context): NoteRepository =
            instance ?: synchronized(this) {
                instance ?: NoteDatabase.getInstance(context).let { db ->
                    NoteRepository(db.noteDao(), db.folderDao())
                }.also { instance = it }
            }
    }
}
