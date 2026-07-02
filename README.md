# EchoNote

Sprachnotizen-App für Android mit Live-Transkription: aufnehmen, während du sprichst wird der Text automatisch erzeugt, in Ordnern organisieren und bei Bedarf nachbearbeiten.

## Funktionen

- Sprachaufnahme mit Live-Transkription (Android SpeechRecognizer), ein Tap genügt
- Homescreen-Widget für die Aufnahme mit einem Tap
- Notizen in Ordnern organisieren, mit eigener Farbe pro Ordner
- Mehrfachauswahl: Notizen anheften, verschieben oder löschen
- Notizen per Drag & Drop bzw. manuellem Reorder sortieren
- Tags, Anheften, Suche (auch offline im Transkript), Markdown-Vorschau im Editor
- Erinnerungen auf Notizen mit Datum und Uhrzeit
- Notizen als Text- oder PDF-Datei exportieren und teilen
- Umfangreiche Design-Anpassung: Akzentfarbe, Kartenstil, Hintergrund, Hell-/Dunkel-/System-Design

## Build

Das Projekt ist ein Standard-Gradle-Android-Projekt (Kotlin, Jetpack Compose). Zum Bauen:

```
./gradlew assembleDebug
```

APKs werden außerdem automatisch über GitHub Actions gebaut (`.github/workflows/build-apk.yml`) und als Artifact bzw. bei manuellem Trigger mit Versionsnummer als [Release](../../releases) veröffentlicht.
