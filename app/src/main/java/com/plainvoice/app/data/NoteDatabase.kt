package com.plainvoice.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.plainvoice.app.BuildConfig

/**
 * Schemaversion 6 ist die Ausgangsbasis der veroeffentlichten App.
 *
 * `fallbackToDestructiveMigration()` ist bewusst entfernt: Es haette bei jeder
 * Schemaaenderung saemtliche Notizen der Nutzer geloescht. Stattdessen bekommt
 * jede kuenftige Aenderung eine echte Migration in [MIGRATIONS], und das Schema
 * wird nach `app/schemas` exportiert, damit Room den Uebergang pruefen kann.
 *
 * Fehlt fuer einen Versionssprung eine Migration, wirft Room beim Start eine
 * IllegalStateException. Das ist gewollt — ein Absturz im eigenen Test ist
 * besser als stiller Datenverlust beim Nutzer.
 */
@Database(entities = [Note::class, Folder::class], version = 6, exportSchema = true)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao

    companion object {
        private const val DATABASE_NAME = "plainvoice.db"

        /**
         * Kuenftige Schemaaenderungen hier eintragen, zum Beispiel:
         *
         * ```
         * private val MIGRATION_6_7 = object : Migration(6, 7) {
         *     override fun migrate(db: SupportSQLiteDatabase) {
         *         db.execSQL("ALTER TABLE notes ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
         *     }
         * }
         * ```
         *
         * Version 6 ist die Startversion, deshalb ist die Liste noch leer.
         */
        private val MIGRATIONS: Array<Migration> = emptyArray()

        @Volatile
        private var instance: NoteDatabase? = null

        fun getInstance(context: Context): NoteDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): NoteDatabase {
            // Muss vor dem Oeffnen laufen: solange keine Verbindung besteht, ist
            // das Kopieren der Datenbankdateien konsistent.
            DatabaseBackup.backupIfUpgraded(context, DATABASE_NAME, BuildConfig.VERSION_CODE)

            return Room.databaseBuilder(context, NoteDatabase::class.java, DATABASE_NAME)
                .addMigrations(*MIGRATIONS)
                .build()
        }
    }
}
