package com.plainvoice.app.util

import android.content.Context
import android.content.res.Configuration
import android.util.Log
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

    private const val TAG = "LocalePreferences"

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
     *
     * Faellt bei jedem Fehler auf den unveraenderten Context zurueck: eine
     * Spracheinstellung darf den Start der App niemals verhindern.
     */
    fun wrap(base: Context): Context = try {
        val tag = currentTag(base)
        if (tag == SYSTEM_DEFAULT) {
            base
        } else {
            val locale = Locale.forLanguageTag(tag)
            Locale.setDefault(locale)

            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            base.createConfigurationContext(config)
        }
    } catch (e: Exception) {
        Log.w(TAG, "Sprache konnte nicht gesetzt werden, nutze Systemsprache", e)
        base
    }

    /**
     * Bewusst **ohne** `applicationContext`: In `Application.attachBaseContext`
     * ist `super.attachBaseContext()` noch nicht gelaufen, dort liefert
     * `applicationContext` null — und der Zugriff darauf liess die App beim
     * Start abstuerzen. Der uebergebene Context reicht; SharedPreferences
     * zeigen prozessweit auf dieselbe Datei.
     */
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
