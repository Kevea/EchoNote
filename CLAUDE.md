# EchoNote — Projektwissen

Sprachnotizen-App für Android mit Live-Transkription, Ordnern, Tags und
mehreren Export-/Import-Formaten. Dieses Dokument fasst zusammen, wie die App
aufgebaut ist, welche Entscheidungen warum getroffen wurden, und wie der
Build-/Release-Workflow funktioniert — als Gedächtnisstütze für zukünftige
Änderungen (durch Claude oder andere Entwickler).

## Tech-Stack

- Kotlin, Jetpack Compose (Material3), `Compose BOM 2024.10.01`
- Navigation-Compose für die Screen-Navigation
- Room (SQLite) als lokale Datenbank, `fallbackToDestructiveMigration()` —
  **jede Schema-Änderung löscht alle lokalen Daten** (keine echten Migrationen)
- MVVM: `AndroidViewModel` + `StateFlow`, kein Dependency-Injection-Framework
  (manuelles Singleton-Pattern über `companion object getInstance(context)`)
- Android `SpeechRecognizer` für Live-Transkription (keine Cloud-API)
- `com.tom-roush:pdfbox-android` für Offline-PDF-Textextraktion (Import) und
  Android-Bordmittel (`PdfDocument`) für PDF-Export
- Package: `com.echonote.app` · App-Name: „EchoNote"
- GitHub: Repo ist umbenannt zu `Kevea/EchoNote`, MCP-Tools referenzieren aber
  weiterhin `owner=Kevea, repo=coder` (GitHub leitet automatisch weiter)

## Architektur / Paketstruktur

```
com.echonote.app/
├── EchoNoteApp.kt              Application-Klasse: Singletons (repository,
│                                themePreferences), Notification-Channel-Setup,
│                                PDFBoxResourceLoader.init()
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
│   ├── NoteExporter.kt            Export als .txt/.md/.pdf (PdfDocument, Bordmittel)
│   ├── NoteImporter.kt            Import aus .txt/.pdf (pdfbox-android für PDF)
│   ├── ThemePreferences.kt        SharedPreferences-basierte Theme-Settings
│   │                              (Akzent-/Grundfarbe, Dark-Mode, Kartenstil,
│   │                              Hintergrundstil, Schriftgröße, Schriftfarbe)
│   ├── ReminderScheduler.kt       AlarmManager (exact-alarm mit Fallback)
│   ├── ReminderReceiver.kt        BroadcastReceiver → Notification
│   ├── BootReceiver.kt            Reminder nach Geräteneustart neu planen
│   └── TimeFormat.kt              Datum/Dauer-Formatierung
├── viewmodel/
│   ├── NotesViewModel.kt          Listen-/Ordner-/Filter-/Mehrfachauswahl-Logik
│   ├── RecordingViewModel.kt      Aufnahme-Flow, generiert Titel aus Transkript
│   ├── NoteDetailViewModel.kt     Einzel-Notiz: speichern, Tags, Ordner, Reminder
│   └── SettingsViewModel.kt       Dünner Wrapper um ThemePreferences
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
`EchoNoteApp.onCreate()`, sonst crasht `PDDocument.load()`). Proguard braucht
explizite Keep-Regeln für `com.tom_roush.**`, sonst bricht die Release-APK
durch Minifizierung.

**Reminders**: `AlarmManager.setExactAndAllowWhileIdle`, mit Fallback auf
ungenaue `set()`, falls `canScheduleExactAlarms()` false ist oder eine
`SecurityException` fliegt (manche OEMs entziehen das Recht ohne Vorwarnung).
`BootReceiver` plant nach Neustart alle offenen Reminder neu, da AlarmManager-
Einträge einen Reboot nicht überleben.

## Datenbank-Schema-Vorsicht

`fallbackToDestructiveMigration()` heißt: **jede Änderung an `Note`/`Folder`
oder ein Versionsbump von `NoteDatabase` löscht alle lokalen Notizen des
Nutzers beim nächsten App-Start.** Vor jeder Schema-Änderung explizit beim
Nutzer nachfragen/darauf hinweisen.

## Build & Release

- Es gibt **keine lokale Android-SDK-Umgebung** in der Sandbox — jeder
  echte Build läuft über GitHub Actions (`.github/workflows/build-apk.yml`),
  angestoßen über die `mcp__github__actions_*`-Tools.
- **`versionCode`/`versionName` in `app/build.gradle.kts` vor jedem Build
  hochzählen.** Ein stehengebliebener `versionCode` war wiederholt die Ursache
  für Icon-/Ressourcen-Caching-Probleme auf dem Testgerät.
- Release-Workflow: `workflow_dispatch` mit Input `release_version` (z. B.
  `"1.13"`) über `mcp__github__actions_run_trigger` (Methode `run_workflow`).
  Ein direkter `git push` eines Tags schlägt in dieser Sandbox mit 403 fehl.
- Direkter `git push` auf `main` scheitert mit HTTP 503 im Sandbox-Git-Proxy.
  Ablauf zum Synchronisieren: auf dem Feature-Branch
  `claude/voice-note-transcription-app-x8ymeu` entwickeln → pushen → PR via
  `mcp__github__create_pull_request` → `mcp__github__merge_pull_request`.
- **README bei jedem Release aktuell halten** (neue Features ergänzen) —
  das ist mittlerweile Standardvorgehen, nicht nur einmalig.
- Vor dem Pushen größerer Änderungen: Hintergrund-Review per `Explore`-Subagent
  (statische Prüfung auf offensichtliche Compile-Fehler, da lokal nicht
  kompiliert werden kann).

## Bekannte, bereits gelöste Stolpersteine (nicht wiederholen)

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
