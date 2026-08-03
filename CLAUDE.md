# Plainvoice — Projektwissen

Sprachnotizen-App für Android mit Live-Transkription, Ordnern, Tags und
mehreren Export-/Import-Formaten. Dieses Dokument fasst zusammen, wie die App
aufgebaut ist, welche Entscheidungen warum getroffen wurden, und wie der
Build-/Release-Workflow funktioniert — als Gedächtnisstütze für zukünftige
Änderungen (durch Claude oder andere Entwickler).

## Tech-Stack

- Kotlin, Jetpack Compose (Material3), `Compose BOM 2024.10.01`
- Navigation-Compose für die Screen-Navigation
- Room (SQLite) als lokale Datenbank mit **echten Migrationen** — Schema
  version 6 ist die Basis, Export nach `app/schemas/`, `MIGRATIONS` in
  `NoteDatabase`. `fallbackToDestructiveMigration()` wurde entfernt
- MVVM: `AndroidViewModel` + `StateFlow`, kein Dependency-Injection-Framework
  (manuelles Singleton-Pattern über `companion object getInstance(context)`)
- Android `SpeechRecognizer` für Live-Transkription (keine Cloud-API)
- `com.tom-roush:pdfbox-android` für Offline-PDF-Textextraktion (Import) und
  Android-Bordmittel (`PdfDocument`) für PDF-Export
- Package: `com.plainvoice.app` · App-Name: „Plainvoice" (bis 2026-08-03
  „EchoNote", Paket `com.echonote.app`)
- GitHub: `Kevea/Plainvoice` (öffentlich)
- **Sprachen:** Englisch ist Standard (`values/`), Deutsch in `values-de/`.
  Keine Anzeigetexte mehr im Kotlin-Code — alles über `strings.xml`

## Architektur / Paketstruktur

```
com.plainvoice.app/
├── PlainvoiceApp.kt              Application-Klasse: Singletons (repository,
│                                themePreferences, exportPreferences),
│                                Notification-Channel-Setup, PDFBoxResourceLoader.init()
├── MainActivity.kt              NavHost-Setup, globaler Hintergrund-Brush,
│                                CompositionLocalProvider für Schriftgrößen-Skalierung
├── data/
│   ├── Note.kt                  @Entity: title, content, audioFilePath,
│   │                             audioDurationMs, amplitudes (CSV-String), tags
│   │                             (CSV-String), folderId, isPinned, colorTag,
│   │                             sortOrder, reminderAt, createdAt, updatedAt
│   ├── Folder.kt                @Entity: name, colorIndex, sortOrder
│   ├── NoteDao.kt / FolderDao.kt Room-DAOs (Flow-basiert für Observability)
│   ├── NoteDatabase.kt          @Database version=6, fallbackToDestructiveMigration
│   └── NoteRepository.kt        Fasst beide DAOs zusammen, Singleton
├── util/
│   ├── VoiceCaptureController.kt  Kapselt SpeechRecognizer-Lifecycle (siehe unten)
│   ├── AudioPlayerController.kt   MediaPlayer-Wrapper für Wiedergabe
│   ├── NoteExporter.kt            Export als .txt/.md/.pdf (Share-Sheet, FileProvider) +
│   │                              exportToFolder() für den Auto-Export in einen
│   │                              SAF-Zielordner (DocumentFile, siehe unten)
│   ├── NoteImporter.kt            Import aus .txt/.pdf (pdfbox-android für PDF)
│   ├── ThemePreferences.kt        SharedPreferences-basierte Theme-Settings
│   │                              (Akzent-/Grundfarbe, Dark-Mode, Kartenstil,
│   │                              Hintergrundstil, Schriftgröße, Schriftfarbe)
│   ├── ExportPreferences.kt       SharedPreferences-basierte Export-Settings
│   │                              (Format .md/.txt, SAF-Ordner-URI, Auto-Export-
│   │                              Schalter, Fehlerflag, noteId→Dateiname-Map)
│   ├── ReminderScheduler.kt       AlarmManager (exact-alarm mit Fallback)
│   ├── ReminderReceiver.kt        BroadcastReceiver → Notification
│   ├── BootReceiver.kt            Reminder nach Geräteneustart neu planen
│   └── TimeFormat.kt              Datum/Dauer-Formatierung
├── viewmodel/
│   ├── NotesViewModel.kt          Listen-/Ordner-/Filter-/Mehrfachauswahl-Logik
│   ├── RecordingViewModel.kt      Aufnahme-Flow, generiert Titel aus Transkript
│   ├── NoteDetailViewModel.kt     Einzel-Notiz: speichern, Tags, Ordner, Reminder,
│   │                              triggert Auto-Export bei Tag-Änderungen
│   └── SettingsViewModel.kt       Dünner Wrapper um ThemePreferences + ExportPreferences
├── ui/
│   ├── screens/                   NoteListScreen, RecordScreen, NoteDetailScreen,
│   │                              SettingsScreen
│   ├── components/                NoteCard, MarkdownText (Mini-Renderer),
│   │                              PulsingMicButton, Waveform
│   └── theme/                     Color.kt, Theme.kt, Type.kt
└── widget/
    └── RecordWidgetProvider.kt     Homescreen-Widget: 1-Tap-Aufnahme-Start
```

## Wichtige Funktionsweisen

**Sprachaufnahme (`VoiceCaptureController`)**: Nutzt ausschließlich
`SpeechRecognizer`, kein paralleles `MediaRecorder` — auf manchen Geräten
(z. B. Samsung) bekommt nur eine Capture-Session das Mikrofon, daher wurde
MediaRecorder entfernt (Transkription ist das Kernfeature). Ein
`recognizerGeneration`-Zähler verhindert, dass verspätete Callbacks eines
bereits zerstörten Recognizers (z. B. nach `pause()`) alten Text erneut
anhängen. `pause()` faltet `partialTranscript` sofort in `finalTranscript`,
da das finale `onResults`-Callback nach `destroy()` oft gar nicht mehr kommt.

**Notizkarten-Übersicht (`NoteCard`)**: Zeigt Titel + Textvorschau; beide
respektieren die optionale globale Schriftfarbe aus den Settings
(`themeSettings.textColorIndex`), Fallback ist der Theme-Standard
(`Color.Unspecified` bzw. `onSurfaceVariant` für die Vorschau).

**Globale Schriftgröße**: In `MainActivity` wird `LocalDensity` per
`CompositionLocalProvider` mit einem skalierten `fontScale` überschrieben
(`density.fontScale * settings.fontSize.scale()`), damit alle `sp`-Werte
app-weit skalieren, ohne jede Text-Komponente einzeln anzufassen.

**Hintergrund-Rendering**: `MainActivity`s `Surface` behält immer die
Standard-(opake, theme-korrekte) Hintergrundfarbe; der Verlauf/Radial/Mesh-
Brush wird in einer separaten `Box` darüber gelegt. Grund: Ein
`Surface(color = Color.Transparent)` ließ den systemeigenen (immer hellen)
Fenster-Hintergrund durch den halbtransparenten Brush durchscheinen —
das brach Dark Mode und erzeugte helle Flecken.

**Import (.txt/.pdf)**: `NoteImporter` nutzt `ActivityResultContracts
.OpenDocument()` (Storage Access Framework) für die Dateiauswahl. Android hat
keine eingebaute PDF-Textextraktion (`PdfRenderer` rastert nur Bitmaps), daher
`com.tom-roush:pdfbox-android` (Init via `PDFBoxResourceLoader.init()` in
`PlainvoiceApp.onCreate()`, sonst crasht `PDDocument.load()`). Proguard braucht
explizite Keep-Regeln für `com.tom_roush.**`, sonst bricht die Release-APK
durch Minifizierung.

**Auto-Export in Sync-Ordner**: Erste Nutzung von `OpenDocumentTree` +
`takePersistableUriPermission` im Repo (bisher gab es nur `OpenDocument()` für
den Notiz-Import, ohne Persistierung) — kein Vorbild für Revoke-Handling war
vorhanden, daher: `NoteExporter.exportToFolder()` prüft vor jedem Schreiben
`DocumentFile.fromTreeUri(...).canWrite()` und schlägt sauber mit
`Result.failure` fehl statt zu crashen, falls der Nutzer den Ordnerzugriff
extern entzogen hat. Export wird **nicht** beim Speichern einer neuen Notiz
ausgelöst, sondern bei jeder Tag-Änderung (`NoteDetailViewModel.setTags`) —
das ist der bewusste Zeitpunkt, an dem der Nutzer die Notiz taggt und damit
"synct". Der Dateiname pro Notiz wird einmalig erzeugt und in
`ExportPreferences` (noteId → Dateiname) gemerkt, damit spätere Re-Exports
dieselbe Datei überschreiben statt Duplikate anzulegen, auch wenn sich der
Titel zwischenzeitlich ändert.

**Reminders**: `AlarmManager.setExactAndAllowWhileIdle`, mit Fallback auf
ungenaue `set()`, falls `canScheduleExactAlarms()` false ist oder eine
`SecurityException` fliegt (manche OEMs entziehen das Recht ohne Vorwarnung).
`BootReceiver` plant nach Neustart alle offenen Reminder neu, da AlarmManager-
Einträge einen Reboot nicht überleben.

## Sprachumschaltung

Die Sprache wird **nicht** über `AppCompatDelegate.setApplicationLocales`
gesetzt. Dessen Backport greift nur in einer `AppCompatActivity`; diese App
nutzt `ComponentActivity` mit `android:Theme.Material.Light.NoActionBar`, und
ein Wechsel auf AppCompat würde einen Theme-Wechsel erzwingen (sonst stürzt die
App beim Start ab).

Stattdessen `util/LocalePreferences`:

- speichert den Sprach-Tag in SharedPreferences
- `wrap()` erzeugt per `createConfigurationContext` einen Context mit der
  gewählten Sprache
- aufgerufen in `attachBaseContext` von **MainActivity und PlainvoiceApp** —
  nur dort greift es, später sind die Ressourcen bereits aufgelöst. Die
  Application mitzunehmen sorgt dafür, dass auch Benachrichtigungen und Widget
  dieselbe Sprache verwenden
- ein **leerer Tag** heißt „Systemsprache" und überschreibt bewusst nichts, damit
  die ab Android 13 systemseitig verwaltete App-Sprache weiter funktioniert
  (`android:localeConfig` im Manifest bleibt dafür bestehen)
- die Auswahl in den Einstellungen ruft `recreate()` auf, damit
  `attachBaseContext` erneut läuft

## Datenbank-Schema

Ab 2026-08-03 gibt es **echte Migrationen**. Jede Schema-Änderung braucht
einen Eintrag in `NoteDatabase.MIGRATIONS` und einen Versionsbump; fehlt die
Migration, wirft Room beim Start eine `IllegalStateException`. Das ist gewollt
— ein Absturz im eigenen Test schlägt stillen Datenverlust beim Nutzer.

Das exportierte Schema liegt unter `app/schemas/` und gehört ins Repo.

`DatabaseBackup` legt zusätzlich einmal pro App-Version eine Kopie der
Datenbank an, bevor Room sie öffnet — inklusive `-wal` und `-shm`, sonst
fehlten der Kopie die letzten Transaktionen.

## Build & Release

- Es gibt **keine lokale Android-SDK-Umgebung** in der Sandbox — jeder
  echte Build läuft über GitHub Actions (`.github/workflows/build-apk.yml`),
  angestoßen über die `mcp__github__actions_*`-Tools.
- **`versionCode`/`versionName` in `app/build.gradle.kts` vor jedem Build
  hochzählen.** Ein stehengebliebener `versionCode` war wiederholt die Ursache
  für Icon-/Ressourcen-Caching-Probleme auf dem Testgerät.
- **Signierte Releases:** Der Workflow hat zwei Jobs. `debug` läuft bei
  Branch-Pushes und sieht die Signier-Secrets nie. `release` läuft nur bei
  Tag-Push oder `workflow_dispatch`, baut `assembleRelease` (APK für den
  Direktverkauf) **und** `bundleRelease` (AAB für Play), prüft die Signatur
  mit `apksigner` und löscht den Keystore danach wieder.
- Secrets im Repo: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
  `KEY_PASSWORD`. Der Keystore selbst liegt auf UpTown unter `~/.keystores/`
  und auf der zweiten Festplatte; das Passwort zusätzlich in Vaultwarden.
- Ablauf zum Synchronisieren: Feature-Branch → push → PR → merge (Squash).
- **README bei jedem Release aktuell halten** (neue Features ergänzen) —
  das ist mittlerweile Standardvorgehen, nicht nur einmalig.
- Vor dem Pushen größerer Änderungen: Hintergrund-Review per `Explore`-Subagent
  (statische Prüfung auf offensichtliche Compile-Fehler, da lokal nicht
  kompiliert werden kann).

## Bekannte, bereits gelöste Stolpersteine (nicht wiederholen)

- In `Application.attachBaseContext` ist `applicationContext` **null** —
  `super.attachBaseContext()` lief dort noch nicht. Ein Zugriff darauf (etwa
  für `getSharedPreferences`) lässt die App beim Start abstürzen, noch bevor
  Oberfläche erscheint. Der übergebene `base`-Context genügt; SharedPreferences
  zeigen prozessweit auf dieselbe Datei.
- **Der CI-Build fängt Startfehler nicht ab** — er kompiliert nur. Alles, was in
  `attachBaseContext`, `Application.onCreate` oder der Room-Initialisierung
  schiefgehen kann, zeigt sich ausschliesslich auf dem Gerät. Nach Eingriffen an
  diesen Stellen immer eine echte Installation testen.
- Material3 `showSnackbar()` setzt `duration` implizit auf `Indefinite`,
  sobald ein `actionLabel` gesetzt ist — für "kurze" Snackbars immer explizit
  `duration = SnackbarDuration.Short` übergeben.
- `pointerInput(key)`-Closures sehen keine späteren Recompositions, wenn sich
  `key` während der Geste nicht ändert (Drag&Drop-Bug bei Ordnern) — Zielposition
  im `onDragEnd`-Callback immer live aus den aktuellen State-Werten neu berechnen,
  nicht aus einer äußeren `val`, die zu Gestenbeginn eingefroren wurde.
- Schriftfarbe-Setting aus den Settings muss an **jeder** Text-Stelle einzeln
  durchgereicht werden (Detailscreen, MarkdownText, NoteCard) — es gibt keinen
  globalen Compose-Mechanismus dafür wie bei der Schriftgröße.

## Offene GitHub Issues (Stand zuletzt geprüft)

Issues #6, #7, #8 wurden in v1.13 behoben (Snackbar-Fix, Import txt/pdf,
Schriftgröße/-farbe). Bei Bedarf `mcp__github__list_issues` erneut prüfen,
ob neue Issues oder Diskussionen vorliegen.
