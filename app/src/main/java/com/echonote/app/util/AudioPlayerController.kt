package com.echonote.app.util

import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackState(
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
)

class AudioPlayerController {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var progressJob: Job? = null
    private var loadedPath: String? = null

    fun togglePlayback(scope: CoroutineScope, filePath: String) {
        val current = player
        if (current != null && loadedPath == filePath) {
            if (current.isPlaying) {
                current.pause()
                _state.update { it.copy(isPlaying = false) }
            } else {
                current.start()
                _state.update { it.copy(isPlaying = true) }
                trackProgress(scope, current)
            }
            return
        }

        release()
        val mp = MediaPlayer().apply {
            setDataSource(filePath)
            setOnCompletionListener {
                _state.update { it.copy(isPlaying = false, positionMs = 0) }
                seekTo(0)
            }
            prepare()
        }
        player = mp
        loadedPath = filePath
        _state.value = PlaybackState(isPlaying = true, positionMs = 0, durationMs = mp.duration)
        mp.start()
        trackProgress(scope, mp)
    }

    fun seekTo(positionMs: Int) {
        player?.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs) }
    }

    private fun trackProgress(scope: CoroutineScope, mp: MediaPlayer) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mp.isPlaying) {
                _state.update { it.copy(positionMs = mp.currentPosition, durationMs = mp.duration) }
                delay(80)
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        progressJob = null
        player?.apply {
            try {
                stop()
            } catch (_: Exception) {
                // already stopped
            }
            release()
        }
        player = null
        loadedPath = null
        _state.value = PlaybackState()
    }
}
