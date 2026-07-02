package com.echonote.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echonote.app.EchoNoteApp
import com.echonote.app.data.Note
import com.echonote.app.util.AudioPlayerController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteDetailViewModel(application: Application, private val noteId: Long) : AndroidViewModel(application) {

    private val repository = (application as EchoNoteApp).repository
    val player = AudioPlayerController()

    val note: StateFlow<Note?> = repository.observeById(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    fun deleteNote(onDeleted: () -> Unit) {
        val current = note.value ?: return
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
