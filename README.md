# ITSQ-Explorer

Abgespeckte Version von TemplateGUI mit Fokus auf ITSQ-Test-Set Verwaltung.

## Features

- **MainFrame**: Hauptfenster mit Config-Toolbar
- **ItsqExplorerView**: ITSQ-Test-Set Verwaltung (Kunden, Szenarien, Testfaelle)
- **DatabaseView**: SQL-Editor mit Ergebnistabelle (via DB-Button)
- **ITSQ-Subsystem**: Komplettes Tree-basiertes Navigations- und Bearbeitungssystem

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
```

## Projektstruktur

```
ITSQ-Explorer/
├── pom.xml                     # Maven Build-Konfiguration
├── docker/                     # Docker-Konfiguration
├── docs/                       # Dokumentation
└── src/main/java/de/cavdar/gui/
    ├── Main.java               # Einstiegspunkt
    ├── design/base/            # MainFrame, BaseViewPanel
    ├── view/base/              # BaseView, ViewInfo
    ├── view/db/                # DatabaseView
    ├── view/itsq/              # ItsqExplorerView
    ├── itsq/                   # ITSQ-Subsystem
    │   ├── design/             # ITSQ Panels
    │   ├── view/               # ITSQ Views
    │   ├── tree/               # Tree-Nodes und -Model
    │   └── model/              # ITSQ Datenmodelle
    ├── model/base/             # AppConfig, ConnectionInfo
    ├── util/                   # Utilities
    └── exception/              # Exceptions
```

## Packages

| Package | Beschreibung |
|---------|--------------|
| `de.cavdar.gui.design.base` | MainFrame, BaseViewPanel, DesktopPanel |
| `de.cavdar.gui.view.base` | BaseView, ViewInfo |
| `de.cavdar.gui.view.db` | DatabaseView |
| `de.cavdar.gui.view.itsq` | ItsqExplorerView |
| `de.cavdar.gui.itsq.*` | Komplettes ITSQ-Subsystem (51 Klassen) |
| `de.cavdar.gui.model.base` | AppConfig, ConfigEntry, ConnectionInfo |
| `de.cavdar.gui.util` | Utilities |

## Basiert auf

- **TemplateGUI**: Vollstaendiges MDI-Framework
- Abgespeckt auf ITSQ-Test-Set Funktionalitaet

## Lizenz

Proprietary
