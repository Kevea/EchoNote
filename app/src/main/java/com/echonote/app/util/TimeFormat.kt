package com.echonote.app.util

import java.util.concurrent.TimeUnit

fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun formatRelativeTime(timestampMs: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestampMs
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "gerade eben"
        minutes < 60 -> "vor ${minutes} Min."
        hours < 24 -> "vor ${hours} Std."
        days < 7 -> "vor ${days} Tg."
        else -> {
            val fmt = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.GERMANY)
            fmt.format(java.util.Date(timestampMs))
        }
    }
}
