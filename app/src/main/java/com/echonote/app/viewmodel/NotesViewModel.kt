package com.echonote.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echonote.app.EchoNoteApp
import com.echonote.app.data.Note
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as EchoNoteApp).repository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _pinnedOnly = MutableStateFlow(false)
    val pinnedOnly: StateFlow<Boolean> = _pinnedOnly.asStateFlow()

    private val _activeTag = MutableStateFlow<String?>(null)
    val activeTag: StateFlow<String?> = _activeTag.asStateFlow()

    private val _activeFolder = MutableStateFlow<String?>(null)
    val activeFolder: StateFlow<String?> = _activeFolder.asStateFlow()

    private val allFiltered: StateFlow<List<Note>> = _searchQuery
        .flatMapLatest { repository.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>> = combine(
        allFiltered, _pinnedOnly, _activeTag, _activeFolder,
    ) { list, pinnedOnly, tag, folder ->
        list.filter { note ->
            (!pinnedOnly || note.isPinned) &&
                (tag == null || note.tagList.contains(tag)) &&
                (folder == null || note.folder == folder)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableTags: StateFlow<List<String>> = allFiltered
        .map { list -> list.flatMap { it.tagList }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableFolders: StateFlow<List<String>> = allFiltered
        .map { list -> list.mapNotNull { it.folder.takeIf { f -> f.isNotBlank() } }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun togglePinnedOnly() {
        _pinnedOnly.value = !_pinnedOnly.value
    }

    fun setActiveTag(tag: String?) {
        _activeTag.value = if (_activeTag.value == tag) null else tag
    }

    fun setActiveFolder(folder: String?) {
        _activeFolder.value = if (_activeFolder.value == folder) null else folder
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { repository.update(note.copy(isPinned = !note.isPinned)) }
    }

    private var lastDeleted: Note? = null

    fun delete(note: Note, onDeleted: () -> Unit) {
        viewModelScope.launch {
            lastDeleted = note
            repository.delete(note)
            onDeleted()
        }
    }

    fun undoDelete() {
        val note = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { repository.save(note) }
    }
}
