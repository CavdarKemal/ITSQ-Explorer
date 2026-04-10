# TODO

## DatabaseView-Umbau (in Arbeit vom User)

**Kontext:** In der Review-Runde am 2026-04-10 ist aufgefallen, dass die DB-Connections
bisher in den env-config.properties persistiert werden (`DB_CONNECTIONS=...` mit
base64-codierten Passwörtern). Der User baut dafür gerade die GUI um.

**Offen nach dem GUI-Umbau:**
- DB_CONNECTIONS-Persistierung im neuen Flow verifizieren
- `abe-config.properties` und `ene-config.properties` als `chore: config sync` committen
  (sind seit Session-Start dirty — enthalten legitime Updates: Fensterposition,
  `ITSQ_PATH=...\TemplateGUI\ITSQ` in abe, reorderte `itsq.testset.history`,
  geleerte `DB_CONNECTIONS` in ene)
- Abwägen: Sollen `DB_CONNECTIONS` überhaupt noch in der env-Config stehen,
  oder besser in einer separaten, nicht committeten Datei (Credentials)?

## Zurückgestellt — eigene Sessions wert

### String → char[] Password-Refactor
Viral durch `ConnectionInfo` / `DatabaseView` / `ConnectionManager`. Echter
Security-Gewinn, aber 3+ Files Touch und braucht eigenen Test-Pass.
Aktueller Stand: `ConnectionInfo.password` ist noch `String`.

### AssertJ-Swing Reflection-Warnings unter Java 17
Pre-existing, betrifft nur Test-Output, nicht Funktionalität. Evtl. mit
AssertJ-Swing-Update lösbar.

### Abgeschlossen am 2026-04-10 — Dokumentation des Stands
- 13 Commits gepusht (`6243242..246ff2a`), 172/172 Tests grün
- Concurrency-Cleanup (BaseView, ItsqMigrationView, EnvironmentLockManager, C3/C4/C6/C13/C17)
- Security-Fixes (URL-Credential-Masking im Log, write-statement-confirmation,
  query-timeout, identifier-quoting, UTF-8 Passwort-Serialisierung)
- Quality (unmodifiable Maps in OldStructureAnalyzer, Stack-Trace-Erhalt in
  MigrationService/TimelineLogger, Filter-Loops aufgeräumt)
- Concurrency-Regression-Tests (ConnectionManager + TestEnvironmentManager)
- Pre-existing Unicode-Test-Failure gefixt (ConnectionInfo UTF-8)
