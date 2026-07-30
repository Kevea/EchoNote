package com.echonote.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.echonote.app.ui.theme.NoteTagColors
import com.echonote.app.util.BackgroundStyle
import com.echonote.app.util.DarkModeOption
import com.echonote.app.util.ExportFormat
import com.echonote.app.util.FontSizeOption
import com.echonote.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val exportSettings by viewModel.exportSettings.collectAsState()
    val context = LocalContext.current

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

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
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
            Text("Akzentfarbe", style = MaterialTheme.typography.titleMedium)
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
            Text("Grundfarbe (Hintergrund)", style = MaterialTheme.typography.titleMedium)
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
            Text("Schriftfarbe", style = MaterialTheme.typography.titleMedium)
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
                        contentDescription = "Standard",
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
            Text("Darstellung", style = MaterialTheme.typography.titleMedium)
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
            Text("Hintergrund", style = MaterialTheme.typography.titleMedium)
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
            Text("Notizkarten", style = MaterialTheme.typography.titleMedium)
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
                    Text("Bunte Karten", modifier = Modifier.weight(1f))
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
                    Text("Abgerundete Ecken", modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.roundedCards,
                        onCheckedChange = { viewModel.setRoundedCards(it) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Schriftgröße", style = MaterialTheme.typography.titleMedium)
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
            Text("Export-Format", style = MaterialTheme.typography.titleMedium)
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
            Text("Automatischer Export", style = MaterialTheme.typography.titleMedium)
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
                        folderName ?: "Kein Ordner ausgewählt",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = { folderPickerLauncher.launch(null) }) {
                        Text(if (folderName == null) "Ordner auswählen" else "Ordner ändern")
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
                            Text("Entfernen")
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("Automatisch exportieren", modifier = Modifier.weight(1f))
                    Switch(
                        checked = exportSettings.autoExportEnabled,
                        enabled = exportSettings.folderUri != null,
                        onCheckedChange = { viewModel.setAutoExportEnabled(it) },
                    )
                }
            }
            if (exportSettings.lastExportFailed) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        "Zugriff auf den Exportordner verloren – bitte Ordner erneut auswählen.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun darkModeLabel(option: DarkModeOption): String = when (option) {
    DarkModeOption.SYSTEM -> "System"
    DarkModeOption.LIGHT -> "Hell"
    DarkModeOption.DARK -> "Dunkel"
}

private fun backgroundStyleLabel(option: BackgroundStyle): String = when (option) {
    BackgroundStyle.SOLID -> "Einfarbig"
    BackgroundStyle.GRADIENT -> "Farbverlauf"
    BackgroundStyle.RADIAL -> "Radialer Verlauf"
    BackgroundStyle.MESH -> "Mesh (mehrfarbig)"
}

private fun fontSizeLabel(option: FontSizeOption): String = when (option) {
    FontSizeOption.SMALL -> "Klein"
    FontSizeOption.NORMAL -> "Normal"
    FontSizeOption.LARGE -> "Groß"
    FontSizeOption.EXTRA_LARGE -> "Sehr groß"
}

private fun exportFormatLabel(option: ExportFormat): String = when (option) {
    ExportFormat.MARKDOWN -> "Markdown (.md)"
    ExportFormat.TEXT -> "Text (.txt)"
}
