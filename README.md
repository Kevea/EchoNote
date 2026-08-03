# Plainvoice

Voice notes for Android that land in your vault as Markdown. Record, and the
text appears while you speak; organise notes in folders and edit them afterwards.

**No network access.** Plainvoice does not request Android's `INTERNET`
permission — the operating system enforces that it cannot send anything
anywhere. See [DATENSCHUTZ.md](DATENSCHUTZ.md) for what that does and does not
cover.

## Features

- Voice recording with live transcription (Android `SpeechRecognizer`), one tap
- Pause and resume a recording without losing what you already said
- Write notes as plain text without recording at all
- Import notes from `.txt` and `.pdf` files
- Home screen widget for one-tap recording
- Folders with their own colour per folder
- Free ordering of folders and notes by drag and drop
- Multi-select: pin, move or delete several notes at once
- Tags, pinning, search (offline, including the transcript), Markdown preview in
  the editor (opens in preview by default, with an optional switch for the
  editing background)
- Reminders on notes with date and time
- Export and share notes as text, Markdown or PDF
- Automatic export: tagged notes are written as `.md`/`.txt` with YAML
  frontmatter (tags, creation date) into a folder you choose — handy for syncing
  into an Obsidian vault with Syncthing
- **English and German**, switchable in Settings independently of the system
  language
- Extensive theming: accent and base colour, several background patterns
  (gradient, radial, mesh), card style, light/dark/system, font size and an
  optional custom text colour

The version and commit are shown at the bottom of Settings; tapping them opens
the [releases page](../../releases).

## Privacy

No accounts, no analytics, no crash reporting, no tracking. Notes stay on the
device; exports go only into the folder you pick.

One caveat is stated plainly rather than glossed over: live transcription uses
Android's own `SpeechRecognizer`, a system service that on most devices sends
the audio to your device's speech provider. Details in
[DATENSCHUTZ.md](DATENSCHUTZ.md).

## Build

A standard Gradle Android project (Kotlin, Jetpack Compose):

```
./gradlew assembleDebug
```

APKs are also built by GitHub Actions (`.github/workflows/build-apk.yml`).
Branch pushes produce a debug build; tag pushes and manual runs produce a
**signed** APK plus an AAB and publish them as a
[release](../../releases).

Signing is fed purely from environment variables. Without `KEYSTORE_PATH` — a
local build, for instance — `assembleRelease` still succeeds and produces an
unsigned artifact instead of failing.
