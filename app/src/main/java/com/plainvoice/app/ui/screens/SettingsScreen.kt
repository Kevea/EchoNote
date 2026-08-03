package com.plainvoice.app.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.plainvoice.app.BuildConfig
import com.plainvoice.app.R
import com.plainvoice.app.util.LocalePreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plainvoice.app.data.Folder
import com.plainvoice.app.ui.theme.NoteTagColors
import com.plainvoice.app.util.BackgroundStyle
import com.plainvoice.app.util.DarkModeOption
import com.plainvoice.app.util.ExportFormat
import com.plainvoice.app.util.FolderSyncMode
import com.plainvoice.app.util.FontSizeOption
import com.plainvoice.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val exportSettings by viewModel.exportSettings.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val syncingFolderIds by viewModel.syncingFolderIds.collectAsState()
    val syncResultMessage by viewModel.syncResultMessage.collectAsState()
    val folderSyncModes by viewModel.folderSyncModes.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSyncFolder by remember { mutableStateOf<Folder?>(null) }

    LaunchedEffect(syncResultMessage) {
        syncResultMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearSyncResultMessage()
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            exportSettings.folderUri?.let { previous ->
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        Uri.parse(previous),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                }
            }
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.setExportFolder(uri.toString())
        }
    }

    pendingSyncFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { pendingSyncFolder = null },
            title = { Text(stringResource(R.string.sync_dialog_title)) },
            text = { Text(stringResource(R.string.sync_dialog_body, folder.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.syncFolder(folder, move = false)
                    pendingSyncFolder = null
                }) { Text(stringResource(R.string.settings_copy_only)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.syncFolder(folder, move = true)
                        pendingSyncFolder = null
                    }) { Text(stringResource(R.string.settings_move)) }
                    TextButton(onClick = { pendingSyncFolder = null }) { Text(stringResource(R.string.action_cancel)) }
                }
            },
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(stringResource(R.string.settings_accent_color), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                NoteTagColors.forEachIndexed { index, color ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(color, CircleShape)
                            .then(
                                if (index == settings.accentColorIndex) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { viewModel.setAccentColor(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (index == settings.accentColorIndex) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.settings_base_color), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                NoteTagColors.forEachIndexed { index, color ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(color, CircleShape)
                            .then(
                                if (index == settings.baseColorIndex) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { viewModel.setBaseColor(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (index == settings.baseColorIndex) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.settings_text_color), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .then(
                            if (settings.textColorIndex == null) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                            } else {
                                Modifier
                            }
                        )
                        .clickable { viewModel.setTextColor(null) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.settings_default),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                NoteTagColors.forEachIndexed { index, color ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(color, CircleShape)
                            .then(
                                if (index == settings.textColorIndex) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { viewModel.setTextColor(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (index == settings.textColorIndex) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                DarkModeOption.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setDarkMode(option) }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = settings.darkMode == option,
                            onClick = { viewModel.setDarkMode(option) },
                        )
                        Text(darkModeLabel(option))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.settings_background), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                BackgroundStyle.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setBackgroundStyle(option) }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = settings.backgroundStyle == option,
                            onClick = { viewModel.setBackgroundStyle(option) },
                        )
                        Text(backgroundStyleLabel(option))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.settings_note_cards), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(R.string.settings_colourful_cards), modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.colorfulCards,
                        onCheckedChange = { viewModel.setColorfulCards(it) },
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(R.string.settings_rounded_corners), modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.roundedCards,
                        onCheckedChange = { viewModel.setRoundedCards(it) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.settings_font_size), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                FontSizeOption.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setFontSize(option) }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = settings.fontSize == option,
                            onClick = { viewModel.setFontSize(option) },
                        )
                        Text(fontSizeLabel(option))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.settings_export_format), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                ExportFormat.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setExportFormat(option) }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = exportSettings.format == option,
                            onClick = { viewModel.setExportFormat(option) },
                        )
                        Text(exportFormatLabel(option))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.settings_export_folder), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                val folderName = remember(exportSettings.folderUri) {
                    exportSettings.folderUri
                        ?.let { runCatching { DocumentFile.fromTreeUri(context, Uri.parse(it)) }.getOrNull()?.name }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        folderName ?: stringResource(R.string.settings_no_folder_selected),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = { folderPickerLauncher.launch(null) }) {
                        Text(if (folderName == null) stringResource(R.string.settings_choose_folder) else stringResource(R.string.settings_change_folder))
                    }
                }
                if (folderName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = {
                            runCatching {
                                context.contentResolver.releasePersistableUriPermission(
                                    Uri.parse(exportSettings.folderUri),
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                                )
                            }
                            viewModel.setExportFolder(null)
                        }) {
                            Text(stringResource(R.string.settings_remove))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.sync_dialog_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                if (folders.isEmpty()) {
                    Text(
                        stringResource(R.string.settings_no_folders_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    if (exportSettings.folderUri == null) {
                        Text(
                            stringResource(R.string.settings_pick_target_first),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    folders.forEach { folder ->
                        val isSyncing = folder.id in syncingFolderIds
                        val autoMode = folderSyncModes[folder.id]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            NoteTagColors.getOrElse(folder.colorIndex) { NoteTagColors.first() },
                                            CircleShape,
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(folder.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    TextButton(
                                        onClick = { pendingSyncFolder = folder },
                                        enabled = exportSettings.folderUri != null,
                                    ) {
                                        Text(stringResource(R.string.settings_sync))
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    stringResource(R.string.settings_auto_sync),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                Switch(
                                    checked = autoMode != null,
                                    enabled = exportSettings.folderUri != null,
                                    onCheckedChange = { enabled ->
                                        viewModel.setFolderSyncMode(
                                            folder.id,
                                            if (enabled) FolderSyncMode.MOVE else null,
                                        )
                                    },
                                )
                            }
                            if (autoMode != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = autoMode == FolderSyncMode.MOVE,
                                        onClick = { viewModel.setFolderSyncMode(folder.id, FolderSyncMode.MOVE) },
                                        label = { Text(stringResource(R.string.settings_move)) },
                                    )
                                    FilterChip(
                                        selected = autoMode == FolderSyncMode.COPY,
                                        onClick = { viewModel.setFolderSyncMode(folder.id, FolderSyncMode.COPY) },
                                        label = { Text(stringResource(R.string.settings_copy)) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                // Leerer Tag bedeutet "Systemsprache". Die Auswahl speichert
                // AppCompat selbst (autoStoreLocales im Manifest), deshalb gibt es
                // dafuer keine eigene Preference. Ein Wechsel legt die Activity neu
                // an — danach liest getApplicationLocales() den neuen Wert.
                val languages = listOf(
                    LocalePreferences.SYSTEM_DEFAULT to stringResource(R.string.settings_language_system),
                    "en" to stringResource(R.string.settings_language_english),
                    "de" to stringResource(R.string.settings_language_german),
                )
                val current = LocalePreferences.currentTag(context)
                val applyLanguage: (String) -> Unit = { tag ->
                    if (tag != current) {
                        LocalePreferences.setTag(context, tag)
                        // Neu erstellen, damit attachBaseContext erneut laeuft.
                        (context as? Activity)?.recreate()
                    }
                }

                languages.forEach { (tag, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { applyLanguage(tag) }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = current == tag,
                            onClick = { applyLanguage(tag) },
                        )
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Versionsfussnote. Zwei Ziele: Updates kommen aus dem Store, weil die
            // App ohne INTERNET-Berechtigung nicht selbst danach suchen kann. Der
            // Quellcode-Link bleibt daneben stehen — er ist der Beleg fuer genau
            // diese Berechtigung.
            val openUrl: (String) -> Unit = { url ->
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: ActivityNotFoundException) {
                    // Kein Browser installiert — dann passiert eben nichts,
                    // statt die App abstuerzen zu lassen.
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Plainvoice ${BuildConfig.APP_VERSION} · ${BuildConfig.APP_COMMIT}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.settings_check_updates),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { openUrl(BuildConfig.APP_STORE_URL) }
                        .padding(vertical = 6.dp, horizontal = 12.dp),
                )
                Text(
                    text = stringResource(R.string.settings_view_on_github),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { openUrl(BuildConfig.APP_REPO_URL) }
                        .padding(vertical = 6.dp, horizontal = 12.dp),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun darkModeLabel(option: DarkModeOption): String = when (option) {
    DarkModeOption.SYSTEM -> stringResource(R.string.dark_mode_system)
    DarkModeOption.LIGHT -> stringResource(R.string.dark_mode_light)
    DarkModeOption.DARK -> stringResource(R.string.dark_mode_dark)
}

@Composable
private fun backgroundStyleLabel(option: BackgroundStyle): String = when (option) {
    BackgroundStyle.SOLID -> stringResource(R.string.background_solid)
    BackgroundStyle.GRADIENT -> stringResource(R.string.background_gradient)
    BackgroundStyle.RADIAL -> stringResource(R.string.background_radial)
    BackgroundStyle.MESH -> stringResource(R.string.background_mesh)
}

@Composable
private fun fontSizeLabel(option: FontSizeOption): String = when (option) {
    FontSizeOption.SMALL -> stringResource(R.string.font_size_small)
    FontSizeOption.NORMAL -> stringResource(R.string.font_size_normal)
    FontSizeOption.LARGE -> stringResource(R.string.font_size_large)
    FontSizeOption.EXTRA_LARGE -> stringResource(R.string.font_size_extra_large)
}

@Composable
private fun exportFormatLabel(option: ExportFormat): String = when (option) {
    ExportFormat.MARKDOWN -> stringResource(R.string.export_format_markdown)
    ExportFormat.TEXT -> stringResource(R.string.export_format_text)
}
