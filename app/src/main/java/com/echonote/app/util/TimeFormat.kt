package com.echonote.app.util

import java.util.concurrent.TimeUnit

fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private val dateTimeFormat = java.text.SimpleDateFormat("dd.MM.yyyy, HH:mm", java.util.Locale.GERMANY)

fun formatDateTime(timestampMs: Long): String = dateTimeFormat.format(java.util.Date(timestampMs))
