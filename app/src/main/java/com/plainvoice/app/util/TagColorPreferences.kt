package com.plainvoice.app.util

import android.content.Context

// Deterministic, on-demand tag -> color assignment, persisted per tag name so it stays stable
// across app restarts. First-write-wins: once a tag has a color it never changes. No StateFlow
// needed since callers (Composables) already recompose from the Note/Folder data that changed.
class TagColorPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun colorIndexFor(tagName: String): Int {
        val key = colorKey(tagName)
        val existing = prefs.getInt(key, -1)
        if (existing >= 0) return existing
        val next = prefs.getInt(KEY_NEXT_INDEX, 0)
        prefs.edit().putInt(key, next).putInt(KEY_NEXT_INDEX, (next + 1) % COLOR_COUNT).apply()
        return next
    }

    private fun colorKey(tagName: String) = "color_$tagName"

    companion object {
        private const val PREFS_NAME = "plainvoice_tag_colors"
        private const val KEY_NEXT_INDEX = "next_index"
        private const val COLOR_COUNT = 12

        @Volatile
        private var instance: TagColorPreferences? = null

        fun getInstance(context: Context): TagColorPreferences =
            instance ?: synchronized(this) {
                instance ?: TagColorPreferences(context).also { instance = it }
            }
    }
}
