# Datenschutz

**Kurzfassung: Plainvoice sammelt nichts, sendet nichts und braucht kein Konto.**

## Die App hat keinen Netzzugriff

Plainvoice fordert die Android-Berechtigung `INTERNET` **nicht** an. Das ist
keine Zusicherung, der du glauben musst — das Betriebssystem setzt es durch. Eine
App ohne diese Berechtigung kann keine Netzwerkverbindung aufbauen, auch nicht
versehentlich.

Nachprüfen kannst du es selbst:

- in dieser Quelle: [`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml)
- oder im fertigen APK mit `aapt dump permissions plainvoice.apk`

Es sind ausserdem keine Netzwerk- oder Analyse-Bibliotheken eingebunden: kein
Firebase, kein Crashlytics, keine Werbe- oder Tracking-SDKs, keine
Absturzberichte.

## Welche Berechtigungen die App braucht, und wofür

| Berechtigung | Wofür |
|---|---|
| `RECORD_AUDIO` | Aufnehmen und Transkribieren deiner Sprachnotizen |
| `POST_NOTIFICATIONS` | Anzeigen der Erinnerungen, die du selbst setzt |
| `RECEIVE_BOOT_COMPLETED` | Gesetzte Erinnerungen nach einem Neustart wieder einplanen |

## Wo deine Daten liegen

Notizen, Transkripte, Ordner und Einstellungen liegen ausschliesslich lokal in
der App. Exporte schreibt die App in einen Ordner, den **du** auswählst — dorthin
und nirgendwo anders. Wenn du diesen Ordner mit Syncthing, einer Cloud oder
sonst etwas synchronisierst, gelten die Regeln des jeweiligen Dienstes; das
liegt ausserhalb von Plainvoice.

Deinstallierst du die App, sind ihre Daten weg. Es gibt keine Serverkopie, weil
es keinen Server gibt.

## Die eine Einschränkung: Spracherkennung

Für die Live-Transkription nutzt Plainvoice `SpeechRecognizer` — die
Spracherkennung, die zu Android gehört. Das ist ein **Systemdienst ausserhalb
dieser App**.

Auf den meisten Geräten überträgt dieser Dienst die Audiodaten zur Auswertung an
den Spracherkennungsanbieter des Geräts, in aller Regel Google. Das passiert
unter dessen Datenschutzbestimmungen und ist von Plainvoice weder steuerbar noch
verhinderbar — die App bekommt lediglich den erkannten Text zurück.

Manche Geräte und Android-Versionen bieten Offline-Spracherkennung an. Ist sie in
den Systemeinstellungen aktiv, bleibt auch die Transkription auf dem Gerät. Zu
finden meist unter *Einstellungen → System → Sprachen & Eingabe → Spracherkennung*
(die Bezeichnung unterscheidet sich je nach Hersteller).

Wer das ganz vermeiden will: Notizen lassen sich in Plainvoice auch direkt tippen
oder aus `.txt`- und `.pdf`-Dateien importieren, ohne dass die Spracherkennung
ins Spiel kommt.

## Kauf über Gumroad

Der Kauf läuft über Gumroad. Die dabei anfallenden Daten — E-Mail-Adresse,
Zahlungsabwicklung — verarbeitet Gumroad als Anbieter der Plattform nach seinen
eigenen Bestimmungen. Der Entwickler von Plainvoice erhält daraus lediglich die
Angaben, die Gumroad Verkäufern zur Verfügung stellt, und nutzt sie
ausschliesslich zur Abwicklung und für Rückfragen zum Kauf.

## Kontakt

Fragen dazu gern als
[Issue im Repository](https://github.com/Kevea/Plainvoice/issues).

---

*Stand: 2026-08-03*
