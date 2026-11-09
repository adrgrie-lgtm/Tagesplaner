# Familienplaner

Persönlicher Familienplaner für den Alltag mit Kindern:

- Schützt feste Termine und erkennt flexible Zeiten
- Hält Puffer im Tagesablauf frei
- Schlägt bei Änderungen sinnvolle neue Planungen vor

## Web-App (PWA)

`docs/` enthält den Familien-Tagesplaner als installierbare Web-App: geteilter Kalender und Aufgaben für die Familie mit Live-Sync über Firebase. Hosting über GitHub Pages aus `docs/`, Anleitung in `docs/README.md`.

- URL: https://adrgrie-lgtm.github.io/Tagesplaner/
- Firebase-Sync (Projekt `familienplaner-6ac08`), Konfiguration in `docs/index.html`
- Auf dem Smartphone: URL im Browser öffnen, „Zum Startbildschirm hinzufügen"

## Technologie

Android-App (Kotlin, Gradle) · Web-PWA (Vanilla JS) · Firebase (Firestore, Rules)

## Setup

Konfiguration über `.env` (Vorlage: `.env.example`). Firestore-Regeln liegen in `firestore.rules`, zugehörige Tests in `firestore.test.js`.
