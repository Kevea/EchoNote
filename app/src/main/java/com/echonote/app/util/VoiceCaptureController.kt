package com.echonote.app.util

import android.content.Context
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class CaptureUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedMs: Long = 0L,
    val amplitude: Int = 0,
    val amplitudeHistory: List<Int> = emptyList(),
    val finalTranscript: String = "",
    val partialTranscript: String = "",
    val speechAvailable: Boolean = true,
) {
    val liveTranscript: String
        get() = listOf(finalTranscript, partialTranscript).filter { it.isNotBlank() }.joinToString(" ").trim()
}

data class CaptureResult(
    val durationMs: Long,
    val amplitudes: List<Int>,
    val transcript: String,
)

private const val RMS_REFERENCE = 12f
private const val AMPLITUDE_SCALE = 14000f

/**
 * Speech recognition is the sole mic client here. Earlier this ran a MediaRecorder in
 * parallel to also save playable audio, but on several devices (e.g. Samsung) the OS grants
 * the mic to only one capture session at a time, so the recognizer silently got no audio.
 * Transcription is the feature that matters, so it now owns the mic exclusively.
 */
class VoiceCaptureController(private val context: Context) {

    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private var segmentStartRealtime = 0L
    private var accumulatedElapsedMs = 0L
    private var tickerJob: Job? = null
    private var wantsListening = false

    fun start(scope: CoroutineScope) {
        accumulatedElapsedMs = 0L
        segmentStartRealtime = System.currentTimeMillis()
        _state.value = CaptureUiState(isRecording = true)

        startRecognizer()
        startTicker(scope)
    }

    private fun startTicker(scope: CoroutineScope) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _state.value.isRecording && !_state.value.isPaused) {
                _state.update {
                    it.copy(
                        elapsedMs = accumulatedElapsedMs + (System.currentTimeMillis() - segmentStartRealtime),
                        amplitudeHistory = it.amplitudeHistory + it.amplitude,
                    )
                }
                delay(100)
            }
        }
    }

    fun pause() {
        val current = _state.value
        if (!current.isRecording || current.isPaused) return
        wantsListening = false
        tickerJob?.cancel()
        tickerJob = null
        accumulatedElapsedMs += System.currentTimeMillis() - segmentStartRealtime
        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (_: Exception) {
            // already destroyed
        }
        recognizer = null
        _state.update { it.copy(isPaused = true, amplitude = 0) }
    }

    fun resume(scope: CoroutineScope) {
        val current = _state.value
        if (!current.isRecording || !current.isPaused) return
        segmentStartRealtime = System.currentTimeMillis()
        _state.update { it.copy(isPaused = false) }
        startRecognizer()
        startTicker(scope)
    }

    private fun startRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.update { it.copy(speechAvailable = false) }
            return
        }
        wantsListening = true
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) {
                // onRmsChanged can fire much more often than the 100ms ticker; only the
                // ticker samples this into amplitudeHistory, so this update stays O(1)
                // instead of copying a growing list on every callback.
                val amp = ((rmsdB.coerceIn(0f, RMS_REFERENCE) / RMS_REFERENCE) * AMPLITUDE_SCALE).toInt()
                _state.update { it.copy(amplitude = amp) }
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                if (wantsListening) restartListening(sr)
            }

            override fun onResults(results: android.os.Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) {
                    _state.update {
                        val combined = listOf(it.finalTranscript, text).filter { s -> s.isNotBlank() }.joinToString(" ")
                        it.copy(finalTranscript = combined, partialTranscript = "")
                    }
                }
                if (wantsListening) restartListening(sr)
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                _state.update { it.copy(partialTranscript = text) }
            }

            override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
        })
        recognizer = sr
        requestListening(sr)
    }

    private fun requestListening(sr: SpeechRecognizer) {
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        try {
            sr.startListening(intent)
        } catch (_: Exception) {
            // recognizer may already be listening or torn down concurrently; safe to ignore
        }
    }

    private fun restartListening(sr: SpeechRecognizer) {
        if (!wantsListening) return
        try {
            sr.cancel()
            requestListening(sr)
        } catch (_: Exception) {
            // ignore - controller may be stopping concurrently
        }
    }

    fun stop(): CaptureResult {
        wantsListening = false
        tickerJob?.cancel()
        tickerJob = null

        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (_: Exception) {
            // already destroyed
        }
        recognizer = null

        val finalState = _state.value
        _state.value = finalState.copy(isRecording = false)

        return CaptureResult(
            durationMs = finalState.elapsedMs,
            amplitudes = finalState.amplitudeHistory,
            transcript = finalState.liveTranscript,
        )
    }

    fun cancel() {
        wantsListening = false
        tickerJob?.cancel()
        tickerJob = null
        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (_: Exception) {
            // ignore
        }
        recognizer = null
        _state.value = CaptureUiState()
    }
}
