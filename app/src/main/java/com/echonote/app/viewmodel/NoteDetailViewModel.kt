package com.echonote.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echonote.app.EchoNoteApp
import com.echonote.app.data.Folder
import com.echonote.app.data.Note
import com.echonote.app.util.AudioPlayerController
import com.echonote.app.util.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteDetailViewModel(application: Application, private val noteId: Long) : AndroidViewModel(application) {

    private val repository = (application as EchoNoteApp).repository
    val player = AudioPlayerController()

    val note: StateFlow<Note?> = repository.observeById(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val availableFolders: StateFlow<List<Folder>> = repository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveTitleAndContent(title: String, content: String) {
        val current = note.value ?: return
        if (current.title == title && current.content == content) return
        viewModelScope.launch { repository.update(current.copy(title = title, content = content)) }
    }

    fun togglePin() {
        val current = note.value ?: return
        viewModelScope.launch { repository.update(current.copy(isPinned = !current.isPinned)) }
    }

    fun setColorTag(colorIndex: Int) {
        val current = note.value ?: return
        viewModelScope.launch { repository.update(current.copy(colorTag = colorIndex)) }
    }

    fun setTags(tags: List<String>) {
        val current = note.value ?: return
        viewModelScope.launch { repository.update(current.copy(tags = tags.joinToString(","))) }
    }

    fun setFolder(folderId: Long?) {
        val current = note.value ?: return
        viewModelScope.launch { repository.update(current.copy(folderId = folderId)) }
    }

    fun createAndSetFolder(name: String, colorIndex: Int) {
        val current = note.value ?: return
        viewModelScope.launch {
            val id = repository.createFolder(name, colorIndex)
            repository.update(current.copy(folderId = id))
        }
    }

    fun setReminder(timestampMs: Long?) {
        val current = note.value ?: return
        val app = getApplication<Application>()
        viewModelScope.launch { repository.update(current.copy(reminderAt = timestampMs)) }
        if (timestampMs != null) {
            ReminderScheduler.schedule(app, current.id, timestampMs, current.title.ifBlank { "Notiz" })
        } else {
            ReminderScheduler.cancel(app, current.id)
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        val current = note.value ?: return
        if (current.reminderAt != null) {
            ReminderScheduler.cancel(getApplication(), current.id)
        }
        viewModelScope.launch {
            repository.delete(current)
            onDeleted()
        }
    }

    fun togglePlayback() {
        val path = note.value?.audioFilePath ?: return
        player.togglePlayback(viewModelScope, path)
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
