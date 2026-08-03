package com.plainvoice.app.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Sprachwahl innerhalb der App.
 *
 * Warum nicht `AppCompatDelegate.setApplicationLocales`: Dessen Backport wendet
 * die Sprache nur an, wenn die Activity eine `AppCompatActivity` ist. Diese App
 * baut auf `ComponentActivity` und ein reines Material-Theme; der Wechsel auf
 * AppCompat haette einen Theme-Wechsel erzwungen und damit deutlich mehr Risiko
 * bedeutet als dieser kleine Wrapper.
 *
 * Ein leerer Tag heisst „Systemsprache". Dann wird bewusst **nichts**
 * ueberschrieben — so greift ab Android 13 weiterhin die vom System verwaltete
 * App-Sprache aus den Systemeinstellungen.
 */
object LocalePreferences {

    const val SYSTEM_DEFAULT = ""

    private const val PREFS = "locale_settings"
    private const val KEY_TAG = "language_tag"

    fun currentTag(context: Context): String =
        prefs(context).getString(KEY_TAG, SYSTEM_DEFAULT) ?: SYSTEM_DEFAULT

    fun setTag(context: Context, tag: String) {
        prefs(context).edit().putString(KEY_TAG, tag).apply()
    }

    /**
     * Legt die gewaehlte Sprache ueber einen Context. Muss in
     * `attachBaseContext` passieren — spaeter sind die Ressourcen bereits
     * aufgeloest und eine Aenderung bliebe wirkungslos.
     */
    fun wrap(base: Context): Context {
        val tag = currentTag(base)
        if (tag == SYSTEM_DEFAULT) return base

        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
