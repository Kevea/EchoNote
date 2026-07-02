# EchoNote

Sprachnotizen-App für Android mit Live-Transkription: aufnehmen, während du sprichst wird der Text automatisch erzeugt, in Ordnern organisieren und bei Bedarf nachbearbeiten.

## Funktionen

- Sprachaufnahme mit Live-Transkription (Android SpeechRecognizer)
- Notizen in Ordnern organisieren, mit eigener Farbe pro Ordner
- Mehrfachauswahl: Notizen anheften, verschieben oder löschen
- Notizen per Drag & Drop zwischen Ordnern verschieben
- Tags, Anheften, Suche, Markdown-Vorschau im Editor
- Einstellbare Akzentfarbe sowie Hell-/Dunkel-/System-Design

## Build

Das Projekt ist ein Standard-Gradle-Android-Projekt (Kotlin, Jetpack Compose). Zum Bauen:

```
./gradlew assembleDebug
```

APKs werden außerdem automatisch über GitHub Actions gebaut (`.github/workflows/build-apk.yml`) und als Artifact bzw. bei manuellem Trigger mit Versionsnummer als [Release](../../releases) veröffentlicht.
