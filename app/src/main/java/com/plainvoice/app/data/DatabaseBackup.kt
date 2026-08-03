package com.plainvoice.app.data

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Sicherheitsnetz vor Datenbank-Upgrades.
 *
 * Room-Migrationen sind der eigentliche Schutz, aber eine fehlerhafte Migration
 * merkt man erst, wenn die Notizen weg sind. Deshalb wird die Datenbank einmal
 * pro App-Version kopiert, bevor Room sie ueberhaupt oeffnet — solange keine
 * Verbindung offen ist, ist ein einfaches Dateikopieren ein konsistentes Backup.
 *
 * Wichtig: Die WAL- und SHM-Dateien gehoeren mitkopiert. Ohne sie fehlen die
 * zuletzt geschriebenen Transaktionen, und die Kopie waere still veraltet.
 */
internal object DatabaseBackup {

    private const val TAG = "DatabaseBackup"
    private const val PREFS = "database_backup"
    private const val KEY_LAST_VERSION = "last_app_version_code"
    private const val BACKUP_SUFFIX = ".bak"

    /**
     * Legt eine Kopie an, wenn die App seit dem letzten Start aktualisiert wurde.
     * Beim allerersten Start passiert nichts — dann gibt es noch nichts zu retten.
     */
    fun backupIfUpgraded(context: Context, databaseName: String, appVersionCode: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastVersion = prefs.getInt(KEY_LAST_VERSION, NO_VERSION)

        if (lastVersion == appVersionCode) return

        val database = context.getDatabasePath(databaseName)
        if (lastVersion != NO_VERSION && database.exists()) {
            copyDatabaseAside(database)
        }

        prefs.edit().putInt(KEY_LAST_VERSION, appVersionCode).apply()
    }

    /** Pfad der letzten Sicherung, falls vorhanden — fuer eine spaetere Wiederherstellung. */
    fun existingBackup(context: Context, databaseName: String): File? =
        File(context.getDatabasePath(databaseName).path + BACKUP_SUFFIX).takeIf { it.exists() }

    private fun copyDatabaseAside(database: File) {
        val parts = listOf(database, File("${database.path}-wal"), File("${database.path}-shm"))
        try {
            parts.filter { it.exists() }.forEach { source ->
                val target = File(source.path.replace(database.path, database.path + BACKUP_SUFFIX))
                source.copyTo(target, overwrite = true)
            }
            Log.i(TAG, "Datenbank vor dem Upgrade gesichert.")
        } catch (e: Exception) {
            // Ein fehlgeschlagenes Backup darf den Start nicht verhindern — die
            // Migration selbst ist der eigentliche Schutz, das hier ist die Reserve.
            Log.w(TAG, "Sicherung vor dem Upgrade fehlgeschlagen: ${e.message}")
        }
    }

    private const val NO_VERSION = -1
}
