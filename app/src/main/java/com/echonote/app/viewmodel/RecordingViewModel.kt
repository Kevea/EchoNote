package com.echonote.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echonote.app.EchoNoteApp
import com.echonote.app.data.Note
import com.echonote.app.util.CaptureUiState
import com.echonote.app.util.VoiceCaptureController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as EchoNoteApp).repository
    private val controller = VoiceCaptureController(application)

    val captureState: StateFlow<CaptureUiState> = controller.state

    private val _savedNoteId = MutableStateFlow<Long?>(null)
    val savedNoteId: StateFlow<Long?> = _savedNoteId.asStateFlow()

    fun startRecording() {
        controller.start(viewModelScope)
    }

    fun cancelRecording() {
        controller.cancel()
    }

    fun stopAndSave() {
        val result = controller.stop()
        if (result.transcript.isBlank()) return

        viewModelScope.launch {
            val title = generateTitle(result.transcript)
            val note = Note(
                title = title,
                content = result.transcript,
                audioDurationMs = result.durationMs,
                amplitudes = result.amplitudes.filterIndexed { i, _ -> i % 3 == 0 }.joinToString(","),
            )
            val id = repository.save(note)
            _savedNoteId.value = id
        }
    }

    private fun generateTitle(transcript: String): String {
        if (transcript.isBlank()) {
            val fmt = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY)
            return "Notiz vom ${fmt.format(Date())}"
        }
        val words = transcript.trim().split(Regex("\\s+"))
        val short = words.take(7).joinToString(" ")
        return if (words.size > 7) "$short…" else short
    }

    fun consumeSavedNoteId() {
        _savedNoteId.value = null
    }

    override fun onCleared() {
        controller.cancel()
        super.onCleared()
    }
}
