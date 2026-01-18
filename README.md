# ITSQ-Explorer

Abgespeckte Version von TemplateGUI mit Fokus auf ITSQ-Test-Set Verwaltung.

## Features

- **MainFrame**: Hauptfenster mit Config-Toolbar
- **ItsqExplorerView**: ITSQ-Test-Set Verwaltung (Kunden, Szenarien, Testfaelle)
- **DatabaseView**: SQL-Editor mit Ergebnistabelle (via DB-Button)
- **ITSQ-Subsystem**: Komplettes Tree-basiertes Navigations- und Bearbeitungssystem (46 Klassen)
- **ITSQ-Logik**: Model-Klassen fuer Testdaten-Verarbeitung (8 Klassen)
- **Konsistenz-Tests**: Tests fuer OLD und NEW ITSQ-Strukturen

## Entfernte Komponenten (vs. TemplateGUI)

- ProzessView
- ItsqTreeView (JSON-Explorer)

## Voraussetzungen

- Java 17+
- Maven 3.6+
- Docker (optional, fuer PostgreSQL)

## Build & Start

```bash
# Kompilieren
mvn clean compile

# Starten
mvn exec:java

# Tests ausfuehren
mvn test
```

## Projektstruktur

```
ITSQ-Explorer/
├── pom.xml                     # Maven Build-Konfiguration
├── docker/                     # Docker-Konfiguration
├── docs/                       # Dokumentation
├── src/main/java/de/cavdar/
│   ├── gui/                    # GUI-Komponenten
│   │   ├── Main.java           # Einstiegspunkt
│   │   ├── design/base/        # MainFrame, BaseViewPanel
│   │   ├── view/base/          # BaseView, ViewInfo
│   │   ├── view/db/            # DatabaseView
│   │   ├── view/itsq/          # ItsqExplorerView
│   │   ├── itsq/               # ITSQ-Subsystem (46 Klassen)
│   │   │   ├── design/         # ITSQ Panels
│   │   │   ├── view/           # ITSQ Views
│   │   │   ├── tree/           # Tree-Nodes und -Model
│   │   │   └── model/          # ITSQ Datenmodelle
│   │   ├── model/base/         # AppConfig, ConnectionInfo
│   │   ├── util/               # Utilities
│   │   └── exception/          # Exceptions
│   └── itsq/                   # ITSQ-Logik (8 Klassen)
│       ├── TestCustomer.java   # Kunde mit Szenarien
│       ├── TestScenario.java   # Szenario mit Testfaellen
│       ├── TestCrefo.java      # Einzelner Testfall
│       ├── AB30XMLProperties.java  # Properties-Parsing
│       ├── AB30MapperUtil.java # Mapping-Utilities
│       └── ...
└── src/test/java/de/cavdar/
    ├── gui/                    # GUI-Tests
    └── itsq/                   # ITSQ-Tests (5 Klassen)
        ├── CrefoConsistencyTestBase.java  # Basisklasse
        ├── CrefoConsistencyTest.java      # OLD-Struktur Tests
        └── newstructure/
            └── CrefoConsistencyNewTest.java  # NEW-Struktur Tests
```

## Packages

| Package | Beschreibung | Klassen |
|---------|--------------|---------|
| `de.cavdar.gui.design.base` | MainFrame, BaseViewPanel, DesktopPanel | 5 |
| `de.cavdar.gui.view.base` | BaseView, ViewInfo | 2 |
| `de.cavdar.gui.view.db` | DatabaseView | 1 |
| `de.cavdar.gui.view.itsq` | ItsqExplorerView | 1 |
| `de.cavdar.gui.itsq.*` | ITSQ-Subsystem (design, view, tree, model) | 46 |
| `de.cavdar.gui.model.base` | AppConfig, ConfigEntry, ConnectionInfo | 3 |
| `de.cavdar.gui.util` | Utilities (ConnectionManager, IconLoader, etc.) | 5 |
| `de.cavdar.itsq` | ITSQ-Logik und Model-Klassen | 8 |

## ITSQ-Struktur

Das Projekt unterstuetzt zwei ITSQ-Verzeichnisstrukturen:

### OLD-Struktur
```
ITSQ/OLD/
├── ARCHIV-BESTAND-PH1/     # Phase 1 Archiv
├── ARCHIV-BESTAND-PH2/     # Phase 2 Archiv
└── REF-EXPORTS/            # Gemeinsame REF-EXPORTS
    └── c01-c05/            # Kunden-Verzeichnisse
```

### NEW-Struktur
```
ITSQ/NEW/
├── ARCHIV-BESTAND/
│   ├── PHASE-1/
│   └── PHASE-2/
└── REF-EXPORTS/
    ├── PHASE-1/            # Phasenspezifische REF-EXPORTS
    │   └── c01, c02/
    └── PHASE-2/
        └── c01-c05/
```

## Basiert auf

- **TemplateGUI**: Vollstaendiges MDI-Framework
- Abgespeckt auf ITSQ-Test-Set Funktionalitaet

## Lizenz

Proprietary
