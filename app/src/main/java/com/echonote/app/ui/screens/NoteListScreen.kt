package com.echonote.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DrawerValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.echonote.app.R
import com.echonote.app.data.Folder
import com.echonote.app.ui.components.NoteCard
import com.echonote.app.ui.theme.NoteTagColors
import com.echonote.app.viewmodel.FolderFilter
import com.echonote.app.viewmodel.NotesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    onNoteClick: (Long) -> Unit,
    onRecordClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: NotesViewModel = viewModel(),
) {
    val notes by viewModel.notes.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val pinnedOnly by viewModel.pinnedOnly.collectAsState()
    val activeTag by viewModel.activeTag.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val folderFilter by viewModel.folderFilter.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val selectionMode = selectedIds.isNotEmpty()

    var searchActive by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<Folder?>(null) }
    var showMoveDialog by remember { mutableStateOf(false) }

    // drag-to-reorder state
    var draggingNoteId by remember { mutableStateOf<Long?>(null) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    val noteCardBounds = remember { mutableStateOf<Map<Long, Rect>>(emptyMap()) }
    val dragTargetId = dragPosition?.let { pos ->
        noteCardBounds.value.entries.find { it.key != draggingNoteId && it.value.contains(pos) }?.key
    }

    if (showNewFolderDialog || editingFolder != null) {
        FolderEditDialog(
            initial = editingFolder,
            onDismiss = {
                showNewFolderDialog = false
                editingFolder = null
            },
            onConfirm = { name, color ->
                val existing = editingFolder
                if (existing != null) {
                    viewModel.updateFolder(existing.copy(name = name, colorIndex = color))
                } else {
                    viewModel.createFolder(name, color)
                }
                showNewFolderDialog = false
                editingFolder = null
            },
            onDelete = editingFolder?.let { folder -> { viewModel.deleteFolder(folder); editingFolder = null } },
        )
    }

    if (showMoveDialog) {
        MoveToFolderDialog(
            folders = folders,
            onDismiss = { showMoveDialog = false },
            onSelected = { folderId ->
                viewModel.bulkMove(folderId)
                showMoveDialog = false
            },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(IntrinsicSize.Min)) {
                DrawerContent(
                    folderFilter = folderFilter,
                    folders = folders,
                    onSelectFilter = {
                        viewModel.setFolderFilter(it)
                        scope.launch { drawerState.close() }
                    },
                    onEditFolder = { editingFolder = it },
                    onNewFolder = { showNewFolderDialog = true },
                    onSettings = {
                        scope.launch { drawerState.close() }
                        onSettingsClick()
                    },
                )
            }
        },
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (selectionMode) {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Filled.Close, contentDescription = null)
                            }
                        },
                        title = { Text("${selectedIds.size} ausgewählt") },
                        actions = {
                            IconButton(onClick = { viewModel.bulkPin(true) }) {
                                Icon(Icons.Filled.PushPin, contentDescription = "Anheften")
                            }
                            IconButton(onClick = { showMoveDialog = true }) {
                                Icon(Icons.Filled.DriveFileMove, contentDescription = "Verschieben")
                            }
                            IconButton(onClick = {
                                viewModel.bulkDelete {
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.note_deleted),
                                            actionLabel = context.getString(R.string.action_undo),
                                        )
                                        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                            viewModel.undoDelete()
                                        }
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Löschen")
                            }
                        },
                    )
                } else {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = null)
                            }
                        },
                        title = {
                            if (searchActive) {
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = viewModel::onSearchQueryChange,
                                    placeholder = { Text(stringResource(R.string.search_hint)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                Text(folderScreenTitle(folderFilter, folders))
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                if (searchActive) viewModel.onSearchQueryChange("")
                                searchActive = !searchActive
                            }) {
                                Icon(
                                    imageVector = if (searchActive) Icons.Filled.Close else Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.search_hint),
                                )
                            }
                        },
                    )
                }
            },
            floatingActionButton = {
                if (!selectionMode) {
                    Column(horizontalAlignment = Alignment.End) {
                        SmallFloatingActionButton(
                            onClick = { viewModel.createBlankNote { id -> onNoteClick(id) } },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.list_write_note))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        ExtendedFloatingActionButton(
                            onClick = onRecordClick,
                            icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                            text = { Text("Aufnehmen") },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            },
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    FilterRow(
                        pinnedOnly = pinnedOnly,
                        activeTag = activeTag,
                        availableTags = availableTags,
                        onTogglePinned = viewModel::togglePinnedOnly,
                        onTagSelected = viewModel::setActiveTag,
                    )

                    if (notes.isEmpty()) {
                        EmptyState(folderFilter)
                    } else {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp, 4.dp, 12.dp, 96.dp),
                            verticalItemSpacing = 12.dp,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            staggeredItems(notes, key = { it.id }) { note ->
                                Box(
                                    modifier = Modifier.onGloballyPositioned { coords ->
                                        noteCardBounds.value = noteCardBounds.value + (note.id to coords.boundsInRoot())
                                    }
                                ) {
                                    NoteCard(
                                        note = note,
                                        folder = folders.find { it.id == note.folderId },
                                        isSelected = note.id in selectedIds,
                                        selectionMode = selectionMode,
                                        isDropTarget = note.id == dragTargetId,
                                        onClick = {
                                            if (selectionMode) viewModel.toggleSelected(note.id) else onNoteClick(note.id)
                                        },
                                        onLongPress = { viewModel.startSelection(note.id) },
                                        onDragStart = { draggingNoteId = note.id },
                                        onDrag = { pos -> dragPosition = pos },
                                        onDragEnd = { pos ->
                                            val targetId = noteCardBounds.value.entries
                                                .find { it.key != note.id && it.value.contains(pos) }
                                                ?.key
                                            val targetIndex = targetId?.let { id -> notes.indexOfFirst { it.id == id } }
                                            if (targetIndex != null && targetIndex >= 0) {
                                                val reordered = notes.toMutableList()
                                                reordered.remove(note)
                                                reordered.add(targetIndex, note)
                                                viewModel.reorderNotes(reordered)
                                            }
                                            draggingNoteId = null
                                            dragPosition = null
                                        },
                                        onDragCancel = {
                                            draggingNoteId = null
                                            dragPosition = null
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun folderScreenTitle(filter: FolderFilter, folders: List<Folder>): String = when (filter) {
    is FolderFilter.All -> "Alle Notizen"
    is FolderFilter.Unfiled -> "Meine Notizen"
    is FolderFilter.Specific -> folders.find { it.id == filter.folderId }?.name ?: "Ordner"
}

@Composable
private fun DrawerContent(
    folderFilter: FolderFilter,
    folders: List<Folder>,
    onSelectFilter: (FolderFilter) -> Unit,
    onEditFolder: (Folder) -> Unit,
    onNewFolder: () -> Unit,
    onSettings: () -> Unit,
) {
    // NavigationDrawerItem always fills the width it's given, so a drawer built from it can
    // only ever be as narrow as a fixed dp guess. This uses a plain (non-fillMaxWidth) row
    // instead, so IntrinsicSize.Min below can size the drawer to its actual widest label.
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .widthIn(max = 260.dp)
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = "EchoNote",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        DrawerRow(
            label = "Meine Notizen",
            icon = { Icon(Icons.Filled.NoteAlt, contentDescription = null, modifier = Modifier.size(20.dp)) },
            selected = folderFilter is FolderFilter.Unfiled,
            onClick = { onSelectFilter(FolderFilter.Unfiled) },
        )
        DrawerRow(
            label = "Alle Notizen",
            icon = { Icon(Icons.Filled.FolderOff, contentDescription = null, modifier = Modifier.size(20.dp)) },
            selected = folderFilter is FolderFilter.All,
            onClick = { onSelectFilter(FolderFilter.All) },
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp))
        Text(
            text = "ORDNER",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        folders.forEach { folder ->
          key(folder.id) {
            val color = NoteTagColors.getOrElse(folder.colorIndex) { NoteTagColors.first() }
            var menuExpanded by remember { mutableStateOf(false) }
            DrawerRow(
                label = folder.name,
                icon = { Box(modifier = Modifier.size(12.dp).background(color, CircleShape)) },
                selected = folderFilter is FolderFilter.Specific && folderFilter.folderId == folder.id,
                onClick = { onSelectFilter(FolderFilter.Specific(folder.id)) },
                trailing = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Bearbeiten") },
                                onClick = { menuExpanded = false; onEditFolder(folder) },
                            )
                        }
                    }
                },
            )
          }
        }
        DrawerRow(
            label = "Neuer Ordner",
            icon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
            selected = false,
            onClick = onNewFolder,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp))
        DrawerRow(
            label = "Einstellungen",
            icon = { Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(20.dp)) },
            selected = false,
            onClick = onSettings,
        )
    }
}

@Composable
private fun DrawerRow(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        if (trailing != null) {
            Spacer(modifier = Modifier.width(10.dp))
            trailing()
        }
    }
}

@Composable
private fun FolderEditDialog(
    initial: Folder?,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var colorIndex by remember { mutableStateOf(initial?.colorIndex ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Neuer Ordner" else "Ordner bearbeiten") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(R.string.folder_new_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NoteTagColors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(color, CircleShape)
                                .then(
                                    if (index == colorIndex) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { colorIndex = index },
                        )
                    }
                }
                if (onDelete != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onDelete) { Text("Ordner löschen") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = name.trim()
                if (trimmed.isNotEmpty()) onConfirm(trimmed, colorIndex)
            }) { Text(stringResource(R.string.action_done)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun MoveToFolderDialog(
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onSelected: (Long?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_move_to_folder)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.folder_none),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(null) }
                        .padding(vertical = 10.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
                folders.forEach { folder ->
                    Text(
                        text = folder.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(folder.id) }
                            .padding(vertical = 10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun FilterRow(
    pinnedOnly: Boolean,
    activeTag: String?,
    availableTags: List<String>,
    onTogglePinned: () -> Unit,
    onTagSelected: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = !pinnedOnly,
                onClick = { if (pinnedOnly) onTogglePinned() },
                label = { Text(stringResource(R.string.filter_all)) },
            )
        }
        item {
            FilterChip(
                selected = pinnedOnly,
                onClick = onTogglePinned,
                label = { Text(stringResource(R.string.filter_pinned)) },
            )
        }
        items(availableTags) { tag ->
            FilterChip(
                selected = activeTag == tag,
                onClick = { onTagSelected(tag) },
                label = { Text(tag) },
            )
        }
    }
}

@Composable
private fun EmptyState(folderFilter: FolderFilter) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(
                imageVector = Icons.Filled.NoteAlt,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.empty_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (folderFilter is FolderFilter.Unfiled) {
                    stringResource(R.string.empty_subtitle)
                } else {
                    "In diesem Bereich sind noch keine Notizen."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
