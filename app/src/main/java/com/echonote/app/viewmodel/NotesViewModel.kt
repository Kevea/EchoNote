package com.echonote.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echonote.app.EchoNoteApp
import com.echonote.app.data.Folder
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

sealed class FolderFilter {
    data object Unfiled : FolderFilter()
    data object All : FolderFilter()
    data class Specific(val folderId: Long) : FolderFilter()
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as EchoNoteApp).repository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _pinnedOnly = MutableStateFlow(false)
    val pinnedOnly: StateFlow<Boolean> = _pinnedOnly.asStateFlow()

    private val _activeTag = MutableStateFlow<String?>(null)
    val activeTag: StateFlow<String?> = _activeTag.asStateFlow()

    private val _folderFilter = MutableStateFlow<FolderFilter>(FolderFilter.Unfiled)
    val folderFilter: StateFlow<FolderFilter> = _folderFilter.asStateFlow()

    val folders: StateFlow<List<Folder>> = repository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val allFiltered: StateFlow<List<Note>> = _searchQuery
        .flatMapLatest { repository.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>> = combine(
        allFiltered, _pinnedOnly, _activeTag, _folderFilter, _searchQuery,
    ) { list, pinnedOnly, tag, folderFilter, query ->
        list.filter { note ->
            val matchesFolder = when {
                query.isNotBlank() -> true
                folderFilter is FolderFilter.All -> true
                folderFilter is FolderFilter.Specific -> note.folderId == folderFilter.folderId
                else -> note.folderId == null
            }
            matchesFolder &&
                (!pinnedOnly || note.isPinned) &&
                (tag == null || note.tagList.contains(tag))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableTags: StateFlow<List<String>> = allFiltered
        .map { list -> list.flatMap { it.tagList }.distinct().sorted() }
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

    fun setFolderFilter(filter: FolderFilter) {
        _folderFilter.value = filter
    }

    fun createFolder(name: String, colorIndex: Int, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch { onCreated(repository.createFolder(name, colorIndex)) }
    }

    fun updateFolder(folder: Folder) {
        viewModelScope.launch { repository.updateFolder(folder) }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
            if (_folderFilter.value == FolderFilter.Specific(folder.id)) {
                _folderFilter.value = FolderFilter.Unfiled
            }
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { repository.update(note.copy(isPinned = !note.isPinned)) }
    }

    private var lastDeleted: List<Note> = emptyList()

    fun delete(note: Note, onDeleted: () -> Unit) {
        viewModelScope.launch {
            lastDeleted = listOf(note)
            repository.delete(note)
            onDeleted()
        }
    }

    fun undoDelete() {
        val deleted = lastDeleted
        if (deleted.isEmpty()) return
        lastDeleted = emptyList()
        viewModelScope.launch { deleted.forEach { repository.save(it) } }
    }

    fun moveNote(note: Note, folderId: Long?) {
        viewModelScope.launch { repository.update(note.copy(folderId = folderId)) }
    }

    // --- Multi-select ---

    fun toggleSelected(id: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }

    fun startSelection(id: Long) {
        _selectedIds.value = setOf(id)
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun bulkPin(pin: Boolean) {
        val ids = _selectedIds.value
        viewModelScope.launch {
            notes.value.filter { it.id in ids }.forEach { repository.update(it.copy(isPinned = pin)) }
            clearSelection()
        }
    }

    fun bulkDelete(onDeleted: () -> Unit) {
        val ids = _selectedIds.value
        val toDelete = notes.value.filter { it.id in ids }
        viewModelScope.launch {
            lastDeleted = toDelete
            toDelete.forEach { repository.delete(it) }
            clearSelection()
            onDeleted()
        }
    }

    fun bulkMove(folderId: Long?) {
        val ids = _selectedIds.value
        viewModelScope.launch {
            notes.value.filter { it.id in ids }.forEach { repository.update(it.copy(folderId = folderId)) }
            clearSelection()
        }
    }
}
