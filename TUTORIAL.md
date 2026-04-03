# ITSQ-Explorer – Tutorial

> **Zielgruppe:** Erfahrene Java-Entwickler ohne Vorkenntnisse in dieser Anwendung  
> **Projekt:** [ITSQ-Explorer auf GitHub](https://github.com/CavdarKemal/ITSQ-Explorer)

---

## Inhaltsverzeichnis

1. [Was ist dieses Projekt?](#1-was-ist-dieses-projekt)
2. [Technologie-Stack](#2-technologie-stack)
3. [Projektstruktur](#3-projektstruktur)
4. [Voraussetzungen und Setup](#4-voraussetzungen-und-setup)
5. [Anwendung starten](#5-anwendung-starten)
6. [Grundkonzepte: ITSQ-Datenstruktur](#6-grundkonzepte-itsq-datenstruktur)
7. [Das MDI-Hauptfenster](#7-das-mdi-hauptfenster)
8. [ItsqExplorerView: Der Hauptnavigator](#8-itsqexplorerview-der-hauptnavigator)
9. [ItsqMigrationToolView: Struktur migrieren](#9-itsqmigrationtoolview-struktur-migrieren)
10. [DatabaseView: SQL-Client](#10-databaseview-sql-client)
11. [Konfigurationsverwaltung: AppConfig](#11-konfigurationsverwaltung-appconfig)
12. [Umgebungskonfiguration und -Locking](#12-umgebungskonfiguration-und--locking)
13. [Das Design-View-Muster](#13-das-design-view-muster)
14. [Tests](#14-tests)
15. [Beziehung zu TemplateGUI und StandardMDIGUI](#15-beziehung-zu-templategui-und-standardmdigui)
16. [Nächste Schritte](#16-nächste-schritte)

---

## 1. Was ist dieses Projekt?

**ITSQ-Explorer** ist eine spezialisierte Java Swing MDI-Anwendung zur Verwaltung und Migration von **ITSQ-Testmengen** (IT Service Quality). Sie ist eine fokussierte Variante von TemplateGUI: einige allgemeine Views wurden entfernt, dafür kommen Migrations-Werkzeuge und spezialisierte Editoren hinzu.

### Was die Anwendung kann

| Funktion | Beschreibung |
|----------|-------------|
| **ITSQ-Navigation** | Hierarchischer Baum durch Testmengen (Kunden → Szenarien → Dateien) |
| **Filterkombinationen** | Textsuche, Quelle, Phase, nur-aktive Einträge |
| **Dateieditor (Dual-Mode)** | XML-Editor mit Syntaxhervorhebung oder Properties-Tabellen-Editor |
| **Migrations-Tool** | Alte ITSQ-Strukturen (OLD) in neue (NEW) migrieren mit Phasen-Zuordnung |
| **SQL-Client** | Generischer Datenbankbrowser und SQL-Editor |
| **Multi-Umgebung** | ABE, ENE, GEE — per Konfigurationsdatei umschaltbar |
| **Umgebungs-Locking** | Verhindert gleichzeitigen Zugriff mehrerer Instanzen auf dieselbe Umgebung |

### Projektlinie

```
StandardMDIGUI  →  TemplateGUI  →  ITSQ-Explorer
  (Framework)      (Template)       (diese Anwendung)
```

---

## 2. Technologie-Stack

| Komponente | Version | Rolle |
|------------|---------|-------|
| **Java** | 17 | Laufzeitumgebung |
| **Swing** | Standard-Bibliothek | GUI-Framework |
| **Maven** | 3.6+ | Build und Artefakt-Integration |
| **PostgreSQL** | via JDBC | Datenbankanbindung |
| **RSyntaxTextArea** | 3.5.2 | XML/SQL-Syntaxhervorhebung |
| **Jackson** | 2.17 | JSON-Verarbeitung |
| **Commons IO** | 2.21 | Dateioperationen |
| **Log4j** | 1.2.12 | Logging |
| **JUnit 5** | 5.10.2 | Tests |
| **AssertJ Swing** | 3.17 | GUI-Tests |

---

## 3. Projektstruktur

```
ITSQ-Explorer/
├── pom.xml
├── README.md
├── ene-config.properties            # Standard-Umgebungskonfiguration
├── abe-config.properties
├── gee-config.properties
├── log4j.properties
├── docs/
│   ├── gui.md                       # GUI-Architektur-Dokumentation
│   ├── CLAUDE_CONTEXT.md            # Entwicklungshistorie
│   ├── DOCKER_GUIDE.md              # Docker-Setup
│   ├── ENVIRONMENT_LOCKING.md       # Umgebungs-Lock-Mechanismus
│   └── Maven-Artefakt-Integration.md
├── ITSQ/                            # Beispiel-Testdaten
│   ├── ARCHIV-BESTAND/
│   └── REF-EXPORTS/
└── src/main/java/de/cavdar/
    ├── gui/
    │   ├── Main.java                # Einstiegspunkt
    │   ├── design/base/             # BaseViewPanel, MainFrame, DesktopPanel
    │   ├── view/base/               # BaseView (abstract), ViewInfo (interface)
    │   ├── view/db/                 # DatabaseView
    │   ├── view/itsq/               # ItsqExplorerView, ItsqMigrationToolView
    │   ├── model/base/              # AppConfig, ConnectionInfo
    │   └── util/                    # ConnectionManager, IconLoader
    │
    └── itsq/                        # ITSQ-Fachlogik (18 Klassen)
        ├── TestCustomer.java        # Modell: Testkunde
        ├── TestScenario.java        # Modell: Testszenario
        ├── TestCrefo.java           # Modell: Einzelner Testfall
        ├── AB30XMLProperties.java
        └── migration/               # Migrations-Services (10 Klassen)
            ├── MigrationService.java
            ├── OldStructureAnalyzer.java
            ├── PhaseAssignmentCalculator.java
            └── NewStructureBuilder.java
```

---

## 4. Voraussetzungen und Setup

### Systemvoraussetzungen

- JDK 17 oder neuer
- Maven 3.6+
- Internetzugang (ITSQ-Artefakt beim ersten Build)
- Docker Desktop (optional, für PostgreSQL)

### Bauen

```cmd
cd E:\Projekte\ClaudeCode\ITSQ-Explorer
ci.cmd 17
```

Der Build lädt beim ersten Mal das ITSQ-Testdaten-Artefakt herunter und packt es in `target/testfaelle/`.

---

## 5. Anwendung starten

### Aus der IDE

Hauptklasse: `de.cavdar.gui.Main`

### Als JAR

```cmd
java -jar target/ITSQ-Explorer-1.0.0-SNAPSHOT.jar
```

### Mit eigener Konfigurationsdatei

```cmd
java -Dconfig.file=C:\MeineUmgebung\abe-config.properties -jar ITSQ-Explorer.jar
```

### Konfigurationsprioritäten

1. `-Dconfig.file=/pfad/zur/datei` (System-Property)
2. `CONFIG_FILE_PATH=/pfad/zur/datei` (Umgebungsvariable)
3. `ene-config.properties` im Arbeitsverzeichnis (Standard)

---

## 6. Grundkonzepte: ITSQ-Datenstruktur

### Verzeichnisstruktur einer ITSQ-Testmenge

Eine ITSQ-Testmenge ist ein Verzeichnis mit zwei Hauptästen:

```
ITSQ/
├── ARCHIV-BESTAND/          ← Archivierte Referenzdaten (XML-Dateien)
│   ├── PHASE-1/
│   │   └── 12345.xml        ← Crefo-Nummer als Dateiname
│   └── PHASE-2/
│       └── 67890.xml
│
└── REF-EXPORTS/             ← Referenz-Exporte (Szenarien und Properties)
    ├── PHASE-1/
    │   └── c01/             ← Kundenkürzel
    │       └── Relevanz_Positiv/  ← Szenarioname
    │           ├── Relevanz.properties
    │           └── TestCrefos.properties
    └── PHASE-2/
        └── c01/
            └── ...
```

### Das Domänenmodell

```
TestCustomer (c01, c02, ...)
└── testScenariosMap
    └── TestScenario (Relevanz_Positiv, Relevanz_Negativ, ...)
        └── testFallNameToTestCrefoMap
            └── TestCrefo (p01, n01, x01, ...)
                ├── testFallName    — Typ: p=positiv, n=negativ, x=lösch
                ├── itsqTestCrefoNr — Crefo-Nummer (z.B. 12345)
                └── shouldBeExported
```

### Testfall-Typen

| Präfix | Bedeutung | ARCHIV-BESTAND-Datei |
|--------|-----------|----------------------|
| `p01`, `p02`, … | Positiver Test (Treffer erwartet) | **muss** vorhanden sein |
| `n01`, `n02`, … | Negativer Test (kein Treffer erwartet) | **darf nicht** vorhanden sein |
| `x01`, `x02`, … | Lösch-Test | **muss** vorhanden sein |

Diese Regel ist zentral für das Migrations-Tool.

---

## 7. Das MDI-Hauptfenster

```
┌────────────────────────────────────────────────────────────────┐
│  Menüleiste: Datei │ Ansicht │ Fenster │ Hilfe                 │
├────────────────────────────────────────────────────────────────┤
│  CONFIG-TOOLBAR:  [Config-Datei ▼] [DB-Verbindung ▼]          │
├────────────────────────────────────────────────────────────────┤
│  VIEW-TOOLBAR:    [ITSQ-Explorer] [Migration] [Datenbank]      │
├──────────────┬─────────────────────────────────────────────────┤
│              │                                                  │
│  LINKES      │           DESKTOP                               │
│  PANEL       │   ┌────────────────────┐ ┌──────────────────┐  │
│              │   │  ItsqExplorerView  │ │  DatabaseView    │  │
│  Einstellg.  │   │                    │ │                  │  │
│  Baum        │   └────────────────────┘ └──────────────────┘  │
│              │                                                  │
└──────────────┴─────────────────────────────────────────────────┘
```

**Config-Toolbar:** Wechsel der aktiven Konfigurationsdatei (= Umgebung) und der Datenbankverbindung.

**View-Toolbar:** Schaltflächen zum Öffnen der Views als interne Fenster.

---

## 8. ItsqExplorerView: Der Hauptnavigator

Die zentrale View der Anwendung. Öffnen mit **Strg+J** oder Toolbar.

### Aufbau

```
┌─ ItsqExplorerView ──────────────────────────────────────────────┐
│  TestSet: [/pfad/zur/ITSQ ▼] [Öffnen]                          │
│  Filter: [Textsuche___] [Quelle ▼] [Phase ▼] □ Nur aktive      │
├─────────────────────┬───────────────────────────────────────────┤
│  BAUM               │  DETAIL-PANEL (kontextsensitiv)           │
│                     │                                            │
│  ▼ ARCHIV-BESTAND   │  (je nach Selektion im Baum:             │
│    ▼ PHASE-1        │   - Kunden-Übersicht                      │
│      12345.xml      │   - Szenario-Details                      │
│  ▼ REF-EXPORTS      │   - Datei-Editor im Dual-Mode)            │
│    ▼ PHASE-1        │                                            │
│      ▼ c01          │                                            │
│        Positiv/     │                                            │
└─────────────────────┴───────────────────────────────────────────┘
```

### TestSet öffnen

1. Pfad in die ComboBox eingeben oder aus Historie auswählen
2. **Öffnen** klicken
3. Baum wird aufgebaut — Knotenanzahl erscheint im Titel

### Filter verwenden

Alle Filter wirken **kombiniert**:

| Filter | Wirkung |
|--------|---------|
| Textsuche | Knotenname enthält den Begriff (Groß-/Kleinschreibung ignoriert) |
| Quelle | Nur `ARCHIV-BESTAND` oder nur `REF-EXPORTS` anzeigen |
| Phase | Nur `PHASE-1` oder nur `PHASE-2` anzeigen |
| Nur aktive | Nur Knoten anzeigen, die als aktiv markiert sind |

### Datei-Editor: Dual-Mode

Wenn eine Datei im Baum selektiert wird, erscheint rechts ein Editor. Er arbeitet in zwei Modi (CardLayout):

**XML-Modus:**
- RSyntaxTextArea mit XML-Syntaxhervorhebung
- Zeilennummern
- Suche: `Strg+F`, nächste: `F3`, vorherige: `Shift+F3`
- Zeile springen: `Strg+G`
- Speichern: `Strg+S`

**Properties-Modus (für `.properties`-Dateien):**
- Tabellen-Editor mit Spalten `Name` und `Wert`
- CRUD-Schaltflächen: Neu, Bearbeiten, Löschen
- Live-Filter
- Kommentare bleiben beim Speichern erhalten

### Relevanz.properties — Format

```properties
# Positiver Testfall: p01 = Crefo-Nummer
p01=12345
p02=67890

# Negativer Testfall:
n01=11111

# Lösch-Testfall:
x01=22222
```

---

## 9. ItsqMigrationToolView: Struktur migrieren

Öffnen mit **Strg+Shift+M** oder Toolbar.

### Wann wird das Tool benötigt?

Wenn eine ITSQ-Testmenge in einem alten Format (OLD-Struktur) vorliegt und in das neue Format (NEW-Struktur) mit expliziter Phasen-Zuordnung überführt werden muss.

### Die Phasen-Zuordnungsregel

Das Tool entscheidet für jeden Testfall automatisch, in welche Phase er gehört:

| Testfall-Typ | Bedingung | Zuordnung |
|--------------|-----------|-----------|
| `pXX` (positiv) | Hat eine ARCHIV-BESTAND-Datei | PHASE mit dieser Datei |
| `xXX` (lösch) | Hat eine ARCHIV-BESTAND-Datei | PHASE mit dieser Datei |
| `nXX` (negativ) | **Darf keine** ARCHIV-BESTAND-Datei haben | Andere PHASE |

### Migrationsablauf

```
1. Quellverzeichnis (OLD) auswählen
2. Zielverzeichnis (NEW) festlegen
3. [Analysieren] klicken — Vorschau ohne Änderungen (30 Sek.)
4. Ergebnis prüfen (Statistiken, Probleme, Phasen-Zuordnungen)
5. Probleme im Dialog klären (interaktiv)
6. Optional: Backup vor Migration anlegen
7. [Migrieren] klicken — NEW-Struktur wird aufgebaut
```

### Interne Architektur

```
OldStructureAnalyzer.analyze()
        │
        ▼
PhaseAssignmentCalculator.calculateAssignments()
        │
        ▼
NewStructureBuilder.build()
```

**OldStructureAnalyzer:** Liest die gesamte OLD-Verzeichnisstruktur und erstellt ein Modell aller Kunden, Szenarien und Testfälle.

**PhaseAssignmentCalculator:** Prüft für jeden Testfall anhand der Zuordnungsregel (s.o.), in welche Phase er gehört. Testfälle ohne eindeutige Zuordnung werden als „Problem" markiert.

**NewStructureBuilder:** Erstellt das NEW-Verzeichnis mit korrekter Phasen-Struktur. Filtert `TestCrefos.properties` und `Relevanz.properties` je Phase.

### Fortschrittsanzeige

```
Analysiere... 45%
Status: Verarbeite Kunde c03, Szenario Relevanz_Positiv
[████████████░░░░░░░░░░░░░░] Abbrechen
```

Der SwingWorker läuft im Hintergrund — die GUI bleibt responsiv.

---

## 10. DatabaseView: SQL-Client

### Verbindung einrichten

1. **Neue Verbindung:** Im Verbindungs-Panel auf **Neu** klicken
2. Daten eingeben:

| Feld | Beispiel |
|------|---------|
| Name | `Lokale PostgreSQL` |
| Treiber | `org.postgresql.Driver` |
| URL | `jdbc:postgresql://localhost:5432/meindb` |
| Benutzer | `postgres` |
| Passwort | `geheim` |

3. **Speichern** — wird in `config.properties` unter `DB_CONNECTIONS` abgelegt

### SQL ausführen

```sql
-- Beispiel: Tabellen auflisten (PostgreSQL)
SELECT tablename FROM pg_tables WHERE schemaname = 'public';

-- Daten abfragen
SELECT * FROM kunden LIMIT 100;
```

Ausführen mit **Strg+Enter** oder Schaltfläche **Ausführen**.

### Ergebnisse exportieren

Ergebnistabelle → Rechtsklick → **Als CSV exportieren**

---

## 11. Konfigurationsverwaltung: AppConfig

### Singleton-Zugriff

```java
AppConfig cfg = AppConfig.getInstance();

// Lesen
String basePath = cfg.getProperty("TEST-BASE-PATH");
String[] sources = cfg.getArray("TEST-SOURCES");    // semikolon-getrennt
boolean admin   = cfg.getBool("ADMIN_FUNCS_ENABLED");
int port        = cfg.getInt("DB_PORT", 5432);       // mit Standardwert

// Schreiben
cfg.setProperty("LAST_TESTSET_PATH", "/mein/pfad");
cfg.save();
```

### Typische Einstellungen

```properties
# ─── WINDOW ───────────────────────────────────────────────────
LAST_WINDOW_HEIGHT=825
LAST_WINDOW_WIDTH=1428
LAST_MAIN_SPLIT_DIVIDER=280
LAST_LEFT_SPLIT_DIVIDER=300

# ─── LATEST ───────────────────────────────────────────────────
LAST_TESTSET_PATH=/pfad/zur/ITSQ-Testmenge
LAST_DB_CONNECTION=Lokale PostgreSQL

# ─── FLAGS ────────────────────────────────────────────────────
ADMIN_FUNCS_ENABLED=true

# ─── DATABASE ─────────────────────────────────────────────────
DB_CONNECTIONS=Lokale PostgreSQL|org.postgresql.Driver|jdbc:postgresql://localhost:5432/db|user|cGFzc3dvcmQ=

# ─── TESTS ────────────────────────────────────────────────────
TEST-BASE-PATH=/pfad/zu/testmengen
TEST-SOURCES=ARCHIV-BESTAND;REF-EXPORTS
```

### Konfigurationsdatei wechseln

In der Config-Toolbar die gewünschte `.properties`-Datei auswählen. Die GUI aktualisiert sich automatisch — kein Neustart nötig.

---

## 12. Umgebungskonfiguration und -Locking

### Drei Umgebungen

| Datei | Umgebung | Typischer Einsatz |
|-------|----------|-------------------|
| `ene-config.properties` | ENE | Standard-Entwicklung |
| `abe-config.properties` | ABE | Abnahme-Tests |
| `gee-config.properties` | GEE | weitere Testumgebung |

Jede Datei zeigt auf ein eigenes Testdaten-Verzeichnis und enthält eigene Datenbankverbindungen.

### Umgebungs-Locking

Das Locking-System verhindert, dass zwei Instanzen gleichzeitig auf dieselbe Umgebung zugreifen und Konfigurationsdaten korrumpieren:

| Umgebung | Lock-Port |
|----------|-----------|
| ABE | 47100 |
| ENE | 47101 |
| GEE | 47102 |

**Ablauf beim Start:**
1. Anwendung erkennt die aktive Umgebung aus der Konfigurationsdatei
2. Versucht, den Lock-Port per `ServerSocket` zu reservieren
3. Port frei → Lock gehalten, Anwendung startet normal
4. Port belegt → automatischer Wechsel auf eine freie Umgebung
5. Beim Beenden: JVM-Shutdown-Hook gibt Socket und damit Lock frei

**Manueller Umgebungswechsel:**
- In der Config-Toolbar eine andere Konfigurationsdatei auswählen
- Falls die Zielumgebung gesperrt ist: Fehlerdialog, kein Wechsel möglich

Details: [`docs/ENVIRONMENT_LOCKING.md`](docs/ENVIRONMENT_LOCKING.md)

---

## 13. Das Design-View-Muster

Alle Views folgen einem einheitlichen Muster aus zwei Klassen:

| Klasse | Paket | Basisklasse | Zweck |
|--------|-------|-------------|-------|
| `*Panel` | `design/` | `BaseViewPanel` | GUI-Aufbau — kein Code |
| `*View` | `view/` | `BaseView` | Geschäftslogik — kein Layout |

### Neue View in 3 Schritten

**Schritt 1: Panel**

```java
public class MeinPanel extends BaseViewPanel {
    private JButton btnStart;

    @Override
    protected void initComponents() {
        btnStart = new JButton("Start");
        getContentPanel().add(btnStart);
    }

    public JButton getBtnStart() { return btnStart; }
}
```

**Schritt 2: View**

```java
public class MeinView extends BaseView {
    private MeinPanel panel;

    public MeinView() { super("Meine View"); }

    @Override
    protected BaseViewPanel createPanel() {
        panel = new MeinPanel();
        return panel;
    }

    @Override
    protected void setupToolbarActions() {
        panel.getBtnStart().addActionListener(e ->
            executeTask(() -> {
                // Hintergrundarbeit
                SwingUtilities.invokeLater(() -> {
                    // GUI-Update
                });
            })
        );
    }

    @Override public String getMenuLabel() { return "Meine View"; }
}
```

**Schritt 3: Registrieren** (in `Main.java`)

```java
frame.registerView(MeinView::new);
```

---

## 14. Tests

### Konsistenz-Tests

Die wichtigsten Tests prüfen, ob die ITSQ-Verzeichnisstrukturen intern konsistent sind:

| Testklasse | Beschreibung |
|------------|-------------|
| `OldITSQConsistencyTest` | Prüft OLD-Struktur auf Einhaltung der Typen-Regeln |
| `NewITSQConsistencyTest` | Prüft NEW-Struktur nach Migration auf Korrektheit |

```cmd
mvn test -Dtest=OldITSQConsistencyTest
mvn test -Dtest=NewITSQConsistencyTest
```

### GUI-Tests

```java
@Test
public void shouldOpenItsqExplorer() {
    mainFrame.menuItemWithPath("Ansicht", "ITSQ-Explorer").click();
    mainFrame.internalFrame("ITSQ-Explorer").requireVisible();
}
```

### Alle Tests ausführen

```cmd
cit.cmd 17
```

---

## 15. Beziehung zu TemplateGUI und StandardMDIGUI

### Was ITSQ-Explorer von TemplateGUI übernommen hat

- MDI-Architektur (MainFrame, DesktopPanel)
- BaseView/BaseViewPanel-Muster
- AppConfig, ConnectionManager
- View-Registrierungssystem
- DatabaseView
- GUI-Test-Infrastruktur

### Was ITSQ-Explorer gegenüber TemplateGUI **entfernt** hat

- `ProzessView` — nicht benötigt
- `ItsqTreeView` — in den Hauptexplorer integriert

### Was ITSQ-Explorer **hinzugefügt** hat

- `ItsqMigrationToolView` — OLD→NEW Strukturmigration mit Phasenzuordnung
- Migrations-Service-Schicht (10 Klassen)
- Spezialisierte Editoren (Properties, Options.cfg)
- Umgebungs-Locking
- Domänenmodell: `TestCustomer`, `TestScenario`, `TestCrefo`
- Konsistenz-Tests für ITSQ-Strukturen

### Vollständige Projektlinie

```
StandardMDIGUI (Java 23) — Framework
        ↓
TemplateGUI    (Java 17) — Vollständiges Template
        ↓
ITSQ-Explorer  (Java 17) — Fachanwendung für ITSQ-Testmengen
```

---

## 16. Nächste Schritte

### Docker für Datenbankentwicklung

```cmd
cd docker
docker-compose up -d postgres
```

Anschließend in der DatabaseView mit den lokalen Verbindungsdaten verbinden.

### Eigene Testmenge anlegen

```
MeineTestmenge/
├── ARCHIV-BESTAND/
│   ├── PHASE-1/
│   │   └── 12345.xml
│   └── PHASE-2/
├── REF-EXPORTS/
│   ├── PHASE-1/
│   │   └── c01/
│   │       └── Relevanz_Positiv/
│   │           ├── Relevanz.properties
│   │           └── TestCrefos.properties
│   └── PHASE-2/
```

In der ItsqExplorerView: Pfad eingeben → Öffnen.

### Weiterführende Dokumentation

| Dokument | Inhalt |
|----------|--------|
| [`docs/gui.md`](docs/gui.md) | Vollständige GUI-Architektur (644 Zeilen) |
| [`docs/CLAUDE_CONTEXT.md`](docs/CLAUDE_CONTEXT.md) | Entwicklungshistorie und Pakete |
| [`docs/DOCKER_GUIDE.md`](docs/DOCKER_GUIDE.md) | Docker-Setup |
| [`docs/ENVIRONMENT_LOCKING.md`](docs/ENVIRONMENT_LOCKING.md) | Lock-Mechanismus |

---

*Erstellt: April 2026*
