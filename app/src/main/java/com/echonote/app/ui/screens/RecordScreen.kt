package com.echonote.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.echonote.app.R
import com.echonote.app.ui.components.LiveWaveform
import com.echonote.app.ui.components.PulsingMicButton
import com.echonote.app.util.formatDuration
import com.echonote.app.viewmodel.RecordingViewModel

@Composable
fun RecordScreen(
    onFinished: (noteId: Long?) -> Unit,
    onCancel: () -> Unit,
    viewModel: RecordingViewModel = viewModel(),
) {
    val context = LocalContext.current
    val captureState by viewModel.captureState.collectAsState()
    val savedNoteId by viewModel.savedNoteId.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.startRecording()
        } else {
            permissionDenied = true
        }
    }

    LaunchedEffect(savedNoteId) {
        savedNoteId?.let {
            onFinished(it)
            viewModel.consumeSavedNoteId()
        }
    }

    // One-tap recording: if permission is already granted, start immediately instead of
    // requiring a second tap on the mic button once this screen is on screen.
    LaunchedEffect(Unit) {
        if (hasPermission && !captureState.isRecording) {
            viewModel.startRecording()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
    ) {
        IconButton(
            onClick = {
                if (captureState.isRecording) viewModel.cancelRecording()
                onCancel()
            },
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .align(Alignment.TopStart),
        ) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.record_cancel))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (captureState.isRecording) {
                Text(
                    text = formatDuration(captureState.elapsedMs),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (captureState.isPaused) {
                        stringResource(R.string.record_paused)
                    } else {
                        stringResource(R.string.record_listening)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(24.dp))

                val scrollState = rememberScrollState()
                LaunchedEffect(captureState.liveTranscript) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
                Box(modifier = Modifier.height(180.dp)) {
                    Text(
                        text = captureState.liveTranscript.ifBlank { "…" },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                LiveWaveform(
                    amplitude = captureState.amplitude,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                )
                Spacer(modifier = Modifier.height(20.dp))
                FilledTonalIconButton(
                    onClick = {
                        if (captureState.isPaused) viewModel.resumeRecording() else viewModel.pauseRecording()
                    },
                ) {
                    Icon(
                        imageVector = if (captureState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (captureState.isPaused) {
                            stringResource(R.string.record_resume)
                        } else {
                            stringResource(R.string.record_pause)
                        },
                    )
                }
            } else {
                RecordIdleHint(permissionDenied = permissionDenied)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PulsingMicButton(
                isRecording = captureState.isRecording,
                onClick = {
                    if (captureState.isRecording) {
                        viewModel.stopAndSave()
                    } else if (hasPermission) {
                        viewModel.startRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedContent(targetState = captureState.isRecording, label = "hint") { recording ->
                Text(
                    text = if (recording) stringResource(R.string.record_save) else stringResource(R.string.record_tap_to_start),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecordIdleHint(permissionDenied: Boolean) {
    val infinite = rememberInfiniteTransition(label = "hintFade")
    val alpha by infinite.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "hintAlpha",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Bereit, wenn du es bist",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (permissionDenied) {
                stringResource(R.string.record_permission_denied)
            } else {
                stringResource(R.string.record_permission_rationale)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            textAlign = TextAlign.Center,
        )
    }
}
