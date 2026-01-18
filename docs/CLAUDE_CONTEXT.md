# Claude Code Kontext - ITSQ-Explorer

## Projekt-Uebersicht

**Pfad:** `E:\Projekte\ClaudeCode\ITSQ-Explorer`
**Typ:** Java Swing MDI-Anwendung (Multi-Document Interface)
**Build:** Maven, Java 17
**Package:** `de.cavdar.gui` (GUI) + `de.cavdar.itsq` (Logik)
**Ziel:** ITSQ-Test-Set Verwaltung und Konsistenzpruefung

## Quellprojekte

| Projekt | Beitrag |
|---------|---------|
| **StandardMDIGUI** | MDI-Framework, Views, Design-Pattern, Docker-Setup |
| **ITSQ-Test** | Maven-Artefakt-Integration, Assembly-Konzept, Dokumentation |
| **TemplateGUI** | Basis-Projekt, abgespeckt auf ITSQ-Funktionalitaet |

## Erstellungsdatum

**03.01.2026** - Initiale Erstellung durch Zusammenfuehrung der Quellprojekte

## Letzte Aenderungen

**18.01.2026** - Refactoring ITSQ-Konsistenztests:
- **ITSQConsistencyTestBase**: Neue abstrakte Basisklasse mit Template-Pattern
- **OldITSQConsistencyTest**: Tests fuer OLD ITSQ-Struktur (c01-c06)
- **NewITSQConsistencyTest**: Tests fuer NEW ITSQ-Struktur (phasenspezifisch)
- Alte Klassen entfernt (CrefoConsistencyTestBase, CrefoConsistencyTest, CrefoConsistencyNewTest)
- Neuer Kunde c06 mit Testdaten hinzugefuegt
- Verwendet Model-Klassen (TestCustomer, TestScenario, TestCrefo)
- Unterstuetzt Classpath-Ressourcen fuer Testdaten

**17.01.2026** - Neue Model-Klassen fuer ITSQ-Daten:
- **TestCustomer**: Repraesentiert einen Testkunden (c01-c06)
- **TestScenario**: Repraesentiert ein Szenario (Relevanz_Positiv, etc.)
- **TestCrefo**: Repraesentiert einen Testfall (pXX, xXX, nXX)
- **AB30XMLProperties**: Parsing von TestCrefos.properties
- Trennung von GUI (de.cavdar.gui) und Logik (de.cavdar.itsq)

**07.01.2026** - Umgebungs-Lock-System:
- **EnvironmentLockManager**: Neue Utility-Klasse mit ServerSocket (Ports 47100-47102)
- **Lock-Datei**: TEST-ENVS/{ENV}/.env.lock (nur Info, Lock via Port)
- **TestEnvironmentManager**: Lock-Integration in switchEnvironment()
- **MainFrame**: Lock-Pruefung beim Startup + Fehlermeldung bei Umgebungswechsel
- **Main**: Shutdown Hook fuer automatische Lock-Freigabe
- **Verhalten**:
  - Startup: Wenn Umgebung gesperrt, automatischer Wechsel zu freier Umgebung
  - Umgebungswechsel: Fehlermeldung wenn Ziel-Umgebung gesperrt
  - App-Crash: Port wird vom OS freigegeben

**05.01.2026** - ItsqEditorView Dual-Modus:
- **XML-Modus**: RSyntaxTextArea mit Syntax-Highlighting, Suche (F3/Shift+F3)
- **Properties-Modus**: Tabellen-Editor mit Name/Wert-Spalten, CRUD-Buttons
- **ItsqEditorPanel**: JFormDesigner-Toolbar mit Neu/Aendern/Loeschen/Filter/Speichern
- **CardLayout**: Automatischer Modus-Wechsel basierend auf Dateityp (.xml vs .cfg/.properties)
- **PropertiesTableModel**: Innere Klasse mit Filter und Kommentar-Erhaltung

## Projektstruktur

```
ITSQ-Explorer/
├── pom.xml                     # Maven Build mit Artefakt-Integration
├── README.md                   # Projektueberblick
├── docs/
│   ├── CLAUDE_CONTEXT.md       # Dieses Dokument
│   ├── gui.md                  # GUI-Architektur
│   ├── DOCKER_GUIDE.md         # Docker-Anleitung
│   ├── ENVIRONMENT_LOCKING.md  # Lock-System Doku
│   └── Maven-Artefakt-Integration.md
├── src/main/java/de/cavdar/
│   ├── gui/                    # GUI-Komponenten
│   │   ├── Main.java           # Einstiegspunkt mit main()
│   │   ├── design/base/        # BaseViewPanel, MainFrame, DesktopPanel
│   │   ├── view/base/          # BaseView, ViewInfo
│   │   ├── view/db/            # DatabaseView
│   │   ├── view/itsq/          # ItsqExplorerView
│   │   ├── model/base/         # AppConfig, ConfigEntry, ConnectionInfo
│   │   ├── util/               # Utilities
│   │   ├── exception/          # Exceptions
│   │   └── itsq/               # ITSQ Explorer Subsystem (46 Klassen)
│   │       ├── design/         # JFD-generierte Panels
│   │       ├── model/          # ItsqItem Model-Klassen
│   │       ├── tree/           # TreeNode-Klassen
│   │       └── view/           # View-Klassen
│   └── itsq/                   # ITSQ-Logik (8 Klassen)
│       ├── TestCustomer.java
│       ├── TestScenario.java
│       ├── TestCrefo.java
│       ├── AB30XMLProperties.java
│       ├── AB30MapperUtil.java
│       ├── TestSupportClientKonstanten.java
│       ├── DateTimeFinderFunction.java
│       └── TestFallExtendsArchivBestandCrefos.java
└── src/test/java/de/cavdar/
    ├── gui/                    # GUI-Tests
    └── itsq/                   # ITSQ-Tests (5 Klassen)
        ├── ITSQTestFaelleUtil.java
        ├── TestFallExtendsArchivBestandCrefosTest.java
        ├── ITSQConsistencyTestBase.java  # Abstrakte Basisklasse
        ├── OldITSQConsistencyTest.java   # Tests fuer OLD-Struktur (c01-c06)
        └── NewITSQConsistencyTest.java   # Tests fuer NEW-Struktur
```

## Package-Struktur

```
de.cavdar.gui/
├── Main.java                   # Einstiegspunkt mit main()
├── design/
│   ├── base/                   # BaseViewPanel, MainFrame, DesktopPanel, SettingsPanel
│   └── db/                     # DatabaseViewPanel
├── view/
│   ├── base/                   # BaseView, ViewInfo
│   ├── db/                     # DatabaseView
│   └── itsq/                   # ItsqExplorerView
├── model/base/                 # AppConfig, ConfigEntry, ConnectionInfo
├── util/                       # ConnectionManager, IconLoader, TestEnvironmentManager, EnvironmentLockManager
├── exception/                  # ConfigurationException, ViewException
└── itsq/                       # ITSQ Explorer (JFormDesigner)
    ├── design/                 # ItsqMainPanel, ItsqTreePanel, ItsqEditorPanel, etc.
    ├── model/                  # ItsqItem, ItsqRoot, ItsqCustomer, ItsqScenario, etc.
    ├── tree/                   # ItsqTreeModel, ItsqTreeNode, ItsqRootTreeNode, etc.
    └── view/                   # ItsqMainView, ItsqTreeView, ItsqEditorView, etc.

de.cavdar.itsq/                 # ITSQ-Logik (separates Package)
├── TestSupportClientKonstanten.java  # Konstanten (Verzeichnisse, Patterns)
├── TestCustomer.java           # Model: Kunde mit Szenarien
├── TestScenario.java           # Model: Szenario mit Testfaellen
├── TestCrefo.java              # Model: Einzelner Testfall
├── AB30XMLProperties.java      # Parsing von TestCrefos.properties
├── AB30MapperUtil.java         # Mapping-Utilities
├── DateTimeFinderFunction.java # Datum-Extraktion aus Dateinamen
└── TestFallExtendsArchivBestandCrefos.java  # Hauptlogik
```

## ITSQ-Strukturen

### OLD-Struktur
```
ITSQ/OLD/
├── ARCHIV-BESTAND-PH1/
│   ├── TestCrefos.properties
│   └── {crefo}.xml
├── ARCHIV-BESTAND-PH2/
│   ├── TestCrefos.properties
│   └── {crefo}.xml
└── REF-EXPORTS/
    └── c01-c06/
        └── Relevanz_Positiv/
            ├── Relevanz.properties
            └── {testfall}_{stammsatz|loeschsatz}_{crefo}.xml
```

### NEW-Struktur (phasenspezifische REF-EXPORTS)
```
ITSQ/NEW/
├── ARCHIV-BESTAND/
│   ├── PHASE-1/TestCrefos.properties
│   └── PHASE-2/TestCrefos.properties
└── REF-EXPORTS/
    ├── PHASE-1/c01,c02/
    └── PHASE-2/c01-c05/
```

## Design-View-Trennung Pattern

1. **Panel-Klasse** (`design/`): Nur GUI-Komponenten
2. **View-Klasse** (`view/`): Nur Logik und Event-Handler

## Registrierte Views

| View | Shortcut | Icon |
|------|----------|------|
| ItsqExplorerView | Ctrl+J | folder_cubes.png |
| DatabaseView | - | (via DB-Button) |

## Abhaengigkeiten

| Dependency | Version | Zweck |
|------------|---------|-------|
| log4j | 1.2.12 | Logging |
| slf4j-api | 2.0.9 | Logging Facade |
| postgresql | 42.7.4 | JDBC Driver |
| jackson-databind | 2.17.0 | JSON Parsing |
| junit-jupiter | 5.10.2 | Unit Tests |
| assertj-swing | 3.17.1 | GUI Tests |
| rsyntaxtextarea | 3.4.0 | XML-Editor |

## Model-Klassen Hierarchie

```
TestCustomer (c01, c02, ..., c06)
    ├── customerKey
    ├── testPhase (PHASE_1 oder PHASE_2)
    ├── itsqRefExportsDir
    ├── itsqAB30XmlsDir
    └── testScenariosMap
            │
            ▼
        TestScenario (Relevanz_Positiv, Relevanz_Negativ, ...)
            ├── scenarioName
            ├── testCustomer (Referenz)
            ├── itsqRefExportsFile
            └── testFallNameToTestCrefoMap
                    │
                    ▼
                TestCrefo (p01, x01, n01, ...)
                    ├── testFallName
                    ├── itsqTestCrefoNr
                    ├── shouldBeExported (true fuer pXX/xXX)
                    ├── activated
                    └── itsqRefExportXmlFile
```

## Konsistenz-Tests

Die Tests pruefen, ob die Zuordnungen in `TestCrefos.properties` mit den tatsaechlichen Dateien in `REF-EXPORTS` uebereinstimmen:

- **PHASE-1**: Untermenge - Crefos muessen bei MINDESTENS den definierten Kunden vorkommen
- **PHASE-2**: Vollstaendig - Crefos muessen EXAKT bei den definierten Kunden vorkommen

```bash
# Tests ausfuehren
mvn test -Dtest=OldITSQConsistencyTest     # OLD-Struktur (c01-c06)
mvn test -Dtest=NewITSQConsistencyTest     # NEW-Struktur (phasenspezifisch)
```

## Prompt zum Fortsetzen

```
Ich arbeite am Java-Projekt ITSQ-Explorer unter E:\Projekte\ClaudeCode\ITSQ-Explorer.
Bitte lies die Datei docs/CLAUDE_CONTEXT.md fuer den Kontext.

Aktueller Stand (18.01.2026):
- ITSQConsistencyTestBase mit Template-Pattern (abstrakte Basisklasse)
- OldITSQConsistencyTest (c01-c06), NewITSQConsistencyTest (phasenspezifisch)
- Trennung von GUI (de.cavdar.gui) und Logik (de.cavdar.itsq)
- 46 Klassen im ITSQ-Subsystem, 8 ITSQ-Logik-Klassen, 5 Test-Klassen

Naechste moegliche Aufgaben:
- GUI testen und Feintuning
- Weitere Views ueberarbeiten
- Neue Features implementieren
- Test-Coverage erhoehen
```
