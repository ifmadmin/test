# ICS Calendar Sync

Eine Android-App zum Synchronisieren von ICS-Kalenderdateien mit dem Google Kalender.

## Funktionen

- **ICS-URL Synchronisierung**: Gib eine URL zu einer ICS-Datei an und die App synchronisiert die Ereignisse automatisch
- **Flexibles Sync-Intervall**: Wähle zwischen 15 Minuten und 24 Stunden für automatische Synchronisierung
- **Mehrere Kalender**: Füge beliebig viele ICS-Kalender hinzu
- **Benutzerdefinierte Farben**: Wähle eine Farbe für jeden Kalender
- **Hintergrund-Synchronisierung**: Die App synchronisiert auch im Hintergrund
- **Google Kalender Integration**: Alle Ereignisse erscheinen direkt in der Google Kalender App

## Installation

### APK Download
Lade die neueste APK aus den [GitHub Actions Artifacts](../../actions) herunter oder baue die App selbst.

### Selbst bauen

1. Klone das Repository:
   ```bash
   git clone <repository-url>
   cd ICSCalendarSync
   ```

2. Baue die App:
   ```bash
   ./gradlew assembleDebug
   ```

3. Die APK findest du unter: `app/build/outputs/apk/debug/app-debug.apk`

## Berechtigungen

Die App benötigt folgende Berechtigungen:
- **Kalender**: Zum Lesen und Schreiben von Kalenderereignissen
- **Internet**: Zum Abrufen der ICS-Dateien
- **Hintergrundaktivität**: Für automatische Synchronisierung

## Verwendung

1. Öffne die App und erteile die Kalenderberechtigungen
2. Tippe auf das **+** Symbol um einen neuen Kalender hinzuzufügen
3. Gib den Kalendernamen, die ICS-URL und das Sync-Intervall ein
4. Wähle eine Farbe für den Kalender
5. Tippe auf "Speichern"

Die App wird den Kalender sofort synchronisieren und dann automatisch im gewählten Intervall.

## Technische Details

- **Minimum SDK**: Android 8.0 (API Level 26)
- **Target SDK**: Android 14 (API Level 34)
- **Sprache**: Kotlin
- **Build-System**: Gradle mit Kotlin DSL
- **Background Tasks**: Android WorkManager

## Lizenz

MIT License
