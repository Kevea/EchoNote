# EchoNote

Sprachnotizen-App für Android mit Live-Transkription: aufnehmen, während du sprichst wird der Text automatisch erzeugt, in Ordnern organisieren und bei Bedarf nachbearbeiten.

## Funktionen

- Sprachaufnahme mit Live-Transkription (Android SpeechRecognizer), ein Tap genügt
- Aufnahme lässt sich pausieren und fortsetzen, ohne dass Text verloren geht
- Notizen auch ohne Aufnahme direkt als Text schreiben
- Notizen aus .txt- oder .pdf-Dateien importieren (Dateiauswahl über den Drawer)
- Homescreen-Widget für die Aufnahme mit einem Tap
- Notizen in Ordnern organisieren, mit eigener Farbe pro Ordner
- Ordner und Notizen jeweils per Drag & Drop frei sortieren
- Mehrfachauswahl: Notizen anheften, verschieben oder löschen
- Tags, Anheften, Suche (auch offline im Transkript), Markdown-Vorschau im Editor
  (öffnet standardmäßig in der Vorschau, mit optionalem Umschalter für den
  Bearbeitungshintergrund)
- Erinnerungen auf Notizen mit Datum und Uhrzeit
- Notizen als Text-, Markdown- oder PDF-Datei exportieren und teilen
- Umfangreiche Design-Anpassung: eigene Akzent- und Grundfarbe, mehrere Hintergrund-Muster
  (Verlauf, radial, mehrfarbig), Kartenstil, Hell-/Dunkel-/System-Design, Schriftgröße und
  optionale eigene Schriftfarbe für Notizen

## Build

Das Projekt ist ein Standard-Gradle-Android-Projekt (Kotlin, Jetpack Compose). Zum Bauen:

```
./gradlew assembleDebug
```

APKs werden außerdem automatisch über GitHub Actions gebaut (`.github/workflows/build-apk.yml`) und als Artifact bzw. bei manuellem Trigger mit Versionsnummer als [Release](../../releases) veröffentlicht.
