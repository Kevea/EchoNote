package com.echonote.app.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.io.File
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
    val audioFile: File?,
    val durationMs: Long,
    val amplitudes: List<Int>,
    val transcript: String,
)

/**
 * Records audio to a file while simultaneously running live speech recognition.
 * Recorder and recognizer are independent OS audio clients; either can fail on a given
 * device without taking the other down, so both are wrapped defensively.
 */
class VoiceCaptureController(private val context: Context) {

    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state.asStateFlow()

    private var recorder: MediaRecorder? = null
    private var recognizer: SpeechRecognizer? = null
    private var outputFile: File? = null
    private var startElapsedRealtime = 0L
    private var tickerJob: Job? = null
    private var wantsListening = false

    fun start(scope: CoroutineScope, targetFile: File) {
        outputFile = targetFile
        startElapsedRealtime = System.currentTimeMillis()
        _state.value = CaptureUiState(isRecording = true)

        startRecorder(targetFile)
        startRecognizer()

        tickerJob = scope.launch {
            while (isActive && _state.value.isRecording) {
                val amp = try {
                    recorder?.maxAmplitude ?: 0
                } catch (_: IllegalStateException) {
                    0
                }
                _state.update {
                    it.copy(
                        elapsedMs = System.currentTimeMillis() - startElapsedRealtime,
                        amplitude = amp,
                        amplitudeHistory = it.amplitudeHistory + amp,
                    )
                }
                delay(100)
            }
        }
    }

    private fun startRecorder(targetFile: File) {
        try {
            @Suppress("DEPRECATION")
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96000)
                setAudioSamplingRate(44100)
                setOutputFile(targetFile.absolutePath)
                prepare()
                start()
            }
            recorder = mr
        } catch (_: Exception) {
            recorder = null
        }
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
            override fun onRmsChanged(rmsdB: Float) = Unit
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
            recorder?.stop()
        } catch (_: Exception) {
            // no audio captured (e.g. too short or device rejected concurrent capture)
        }
        recorder?.release()
        recorder = null

        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (_: Exception) {
            // already destroyed
        }
        recognizer = null

        val finalState = _state.value
        _state.value = finalState.copy(isRecording = false)

        val file = outputFile
        val hasAudio = file != null && file.exists() && file.length() > 0
        return CaptureResult(
            audioFile = if (hasAudio) file else null,
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
            recorder?.stop()
        } catch (_: Exception) {
            // ignore
        }
        recorder?.release()
        recorder = null
        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (_: Exception) {
            // ignore
        }
        recognizer = null
        outputFile?.takeIf { it.exists() }?.delete()
        _state.value = CaptureUiState()
    }
}
