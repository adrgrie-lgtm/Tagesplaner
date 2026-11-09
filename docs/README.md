Familien-Tagesplaner (PWA)

Gemeinsame Termine und Aufgaben fuer die Familie. Gehostet im Ordner docs/ dieses Repositories, ausgeliefert ueber GitHub Pages.

Hinweis: Diese PWA wurde aus dem Branch tagesplaner-web des Repos DokumentenApp hierher umgezogen; das ist jetzt ihr Zuhause.

GitHub Pages aktivieren: Settings -> Pages -> Branch: main, Ordner: /docs -> Save. (Achtung: Pages fuer private Repos erfordert einen bezahlten GitHub-Plan; alternativ das Repo auf public stellen.)

Firebase-Sync einrichten (einmalig): Firebase-Projekt anlegen, Web-App registrieren, die Werte oben in docs/index.html bei FIREBASE_CONFIG eintragen, Firestore-Datenbank erstellen, anonyme Anmeldung aktivieren und in den Firestore-Regeln den Zugriff auf families-Collections auf angemeldete Nutzer beschraenken (siehe Regeln im Projekt, z. B. allow read, write: if request.auth != null).

Nutzung: Ein Handy gruendet in der App die Familie (Tab Familie) und zeigt den Einladungscode; alle weiteren Geraete treten mit dem Code bei. Danach synchronisieren Termine, Aufgaben und Mitglieder live zwischen den Geraeten.

Installation auf dem Smartphone: App-Adresse im Browser oeffnen, im Menue "Zum Startbildschirm hinzufuegen" waehlen.
