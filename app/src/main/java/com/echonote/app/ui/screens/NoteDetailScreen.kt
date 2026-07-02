package com.echonote.app.ui.screens

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.echonote.app.R
import com.echonote.app.ui.components.MarkdownText
import com.echonote.app.ui.components.Waveform
import com.echonote.app.ui.theme.NoteTagColors
import com.echonote.app.util.formatDuration
import com.echonote.app.viewmodel.NoteDetailViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long,
    onBack: () -> Unit,
    viewModel: NoteDetailViewModel = run {
        val context = LocalContext.current
        viewModel(
            factory = viewModelFactory {
                initializer {
                    NoteDetailViewModel(context.applicationContext as Application, noteId)
                }
            }
        )
    },
) {
    val note by viewModel.note.collectAsState()
    val playback by viewModel.player.state.collectAsState()
    val availableFolders by viewModel.availableFolders.collectAsState()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }
    var previewMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var newFolderInput by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }

    LaunchedEffect(note?.id) {
        val current = note
        if (!initialized && current != null) {
            title = current.title
            content = current.content
            initialized = true
        }
    }

    LaunchedEffect(title, content, initialized) {
        if (!initialized) return@LaunchedEffect
        delay(500)
        viewModel.saveTitleAndContent(title, content)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.detail_delete_confirm_title)) },
            text = { Text(stringResource(R.string.detail_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteNote(onBack)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showFolderDialog) {
        var newFolderColor by remember { mutableStateOf(0) }
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text(stringResource(R.string.detail_move_to_folder)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.folder_none),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setFolder(null)
                                showFolderDialog = false
                            }
                            .padding(vertical = 10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    availableFolders.forEach { folder ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setFolder(folder.id)
                                    showFolderDialog = false
                                }
                                .padding(vertical = 10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        NoteTagColors.getOrElse(folder.colorIndex) { NoteTagColors.first() },
                                        CircleShape,
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(folder.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFolderInput,
                        onValueChange = { newFolderInput = it },
                        placeholder = { Text(stringResource(R.string.folder_new_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NoteTagColors.forEachIndexed { index, color ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(color, CircleShape)
                                    .then(
                                        if (index == newFolderColor) {
                                            Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { newFolderColor = index },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newFolderInput.trim()
                    if (name.isNotEmpty()) {
                        viewModel.createAndSetFolder(name, newFolderColor)
                        newFolderInput = ""
                    }
                    showFolderDialog = false
                }) { Text(stringResource(R.string.action_done)) }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    val accent = NoteTagColors.getOrElse(note?.colorTag ?: 0) { NoteTagColors.first() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.togglePin() }) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = stringResource(R.string.detail_pin),
                            tint = if (note?.isPinned == true) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box {
                        IconButton(onClick = { showColorPicker = true }) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(accent, CircleShape)
                            )
                        }
                        DropdownMenu(expanded = showColorPicker, onDismissRequest = { showColorPicker = false }) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                NoteTagColors.forEachIndexed { index, color ->
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(color, CircleShape)
                                            .then(
                                                if (index == note?.colorTag) {
                                                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable {
                                                viewModel.setColorTag(index)
                                                showColorPicker = false
                                            }
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = { showFolderDialog = true }) {
                        Icon(Icons.Filled.Folder, contentDescription = stringResource(R.string.detail_move_to_folder))
                    }
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, title)
                            putExtra(Intent.EXTRA_TEXT, "$title\n\n$content")
                        }
                        context.startActivity(Intent.createChooser(send, null))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.detail_share))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.detail_delete))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text(stringResource(R.string.detail_title_hint)) },
                textStyle = MaterialTheme.typography.headlineMedium,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            note?.audioFilePath?.let { path ->
                Spacer(modifier = Modifier.height(8.dp))
                AudioPlayerBar(
                    isPlaying = playback.isPlaying,
                    positionMs = playback.positionMs,
                    durationMs = if (playback.durationMs > 0) playback.durationMs else note!!.audioDurationMs.toInt(),
                    amplitudes = note!!.amplitudeList,
                    accent = accent,
                    onToggle = { viewModel.togglePlayback() },
                )
            }

            val currentFolder = availableFolders.find { it.id == note?.folderId }
            if (currentFolder != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showFolderDialog = true },
                ) {
                    val folderColor = NoteTagColors.getOrElse(currentFolder.colorIndex) { NoteTagColors.first() }
                    Icon(Icons.Filled.Folder, contentDescription = null, tint = folderColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(currentFolder.name, style = MaterialTheme.typography.labelLarge, color = folderColor)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                (note?.tagList ?: emptyList()).forEach { tag ->
                    Card(
                        shape = RoundedCornerShape(50),
                        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(tag, color = accent, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "×",
                                color = accent,
                                modifier = Modifier.clickable {
                                    viewModel.setTags((note?.tagList ?: emptyList()) - tag)
                                },
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                placeholder = { Text(stringResource(R.string.tag_add_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val newTag = tagInput.trim()
                    if (newTag.isNotEmpty()) {
                        viewModel.setTags((note?.tagList ?: emptyList()) + newTag)
                        tagInput = ""
                    }
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { previewMode = !previewMode }) {
                    Icon(Icons.Filled.Preview, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (previewMode) stringResource(R.string.detail_edit) else stringResource(R.string.detail_preview)
                    )
                }
            }

            if (previewMode) {
                MarkdownText(
                    text = content,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp),
                )
            } else {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text(stringResource(R.string.detail_content_hint)) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}

@Composable
private fun AudioPlayerBar(
    isPlaying: Boolean,
    positionMs: Int,
    durationMs: Int,
    amplitudes: List<Int>,
    accent: Color,
    onToggle: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Waveform(
                amplitudes = amplitudes,
                progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
                activeColor = accent,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = formatDuration((if (isPlaying) positionMs else durationMs).toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
