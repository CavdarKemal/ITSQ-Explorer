# TemplateGUI

Ein Template-Projekt fuer Java Swing MDI-Anwendungen mit PostgreSQL-Unterstuetzung und Docker-Integration.

## Features

- **MDI-Framework**: Multiple Document Interface mit BaseView/BaseViewPanel Pattern
- **CustomerTreeView**: Hierarchische Verwaltung von Kunden/Szenarien/Testfaellen mit Checkbox-Tree
- **DatabaseView**: Generische Datenbankansicht mit SQL-Editor und Ergebnistabelle
- **Konfigurationsmanagement**: AppConfig Singleton mit Property-Gruppen
- **Docker-Support**: PostgreSQL Container und optionale GUI-Container

## Voraussetzungen

- Java 17+
- Maven 3.6+
- Docker (optional, fuer PostgreSQL)

## Build

```bash
# Kompilieren
mvn clean compile

# Distribution erstellen
mvn package

# Distribution entpacken
unzip target/TemplateGUI-1.0.0-SNAPSHOT-distribution.zip -d target/
```

## Starten

### Windows
```cmd
cd target\TemplateGUI-1.0.0-SNAPSHOT
startGUI.cmd
```

### Linux/macOS
```bash
cd target/TemplateGUI-1.0.0-SNAPSHOT
java -cp "lib/*" de.cavdar.gui.view.Main
```

## Docker

### PostgreSQL starten
```bash
cd docker
docker-compose up -d postgres
```

### PostgreSQL stoppen
```bash
cd docker
docker-compose down
```

## Projektstruktur

```
TemplateGUI/
├── pom.xml                     # Maven Build-Konfiguration
├── docker/
│   ├── docker-compose.yml      # Docker Services
│   ├── Dockerfile              # GUI Container (optional)
│   └── init-db.sql             # Datenbank-Initialisierung
├── delivery/
│   ├── config.properties       # Anwendungskonfiguration
│   ├── log4j.properties        # Logging-Konfiguration
│   └── startGUI.cmd            # Windows Startskript
├── docs/
│   ├── CLAUDE_CONTEXT.md       # Projekt-Kontext fuer Claude
│   ├── DOCKER_GUIDE.md         # Docker-Anleitung
│   ├── gui.md                  # GUI-Architektur
│   └── Maven-Artefakt-Integration.md
└── src/
    ├── assembly/
    │   └── distribution.xml    # Assembly Deskriptor
    └── main/
        ├── java/de/cavdar/gui/
        │   ├── design/         # GUI Panels (Layout)
        │   ├── view/           # Views (Logik)
        │   ├── model/          # Datenmodelle
        │   ├── util/           # Utilities
        │   └── exception/      # Exceptions
        └── resources/
            └── icons/          # GUI Icons
```

## Packages

| Package | Beschreibung |
|---------|--------------|
| `de.cavdar.gui.design` | GUI-Komponenten (Panels, Frames) |
| `de.cavdar.gui.view` | View-Logik und Event-Handler |
| `de.cavdar.gui.model` | Datenmodelle (AppConfig, ConnectionInfo, etc.) |
| `de.cavdar.gui.util` | Utilities (ConnectionManager, TestDataLoader, etc.) |
| `de.cavdar.gui.exception` | Exceptions |

## Architektur

Das Projekt folgt dem **Design-View-Trennung** Pattern:

- **Panel-Klassen** (design/): Enthalten nur GUI-Komponenten und Layout
- **View-Klassen** (view/): Enthalten Geschaeftslogik und Event-Handler

```
BaseView (abstrakt)
   └── createPanel() → BaseViewPanel
   └── setupToolbarActions()
   └── setupListeners()

BaseViewPanel (abstrakt)
   └── initComponents()
   └── Toolbar, Content, StatusBar
```

## Basiert auf

Dieses Template vereint Funktionalitaeten aus:
- **StandardMDIGUI**: MDI-Framework, Views, Design-Pattern
- **ITSQ-Test**: Maven-Artefakt-Integration, Assembly, Dokumentation

## Dokumentation

| Dokument | Beschreibung |
|----------|--------------|
| [CLAUDE_CONTEXT.md](docs/CLAUDE_CONTEXT.md) | Projekt-Kontext fuer Claude Code Sessions |
| [DOCKER_GUIDE.md](docs/DOCKER_GUIDE.md) | Docker-Anleitung fuer Java-Projekte mit PostgreSQL |
| [Maven-Artefakt-Integration.md](docs/Maven-Artefakt-Integration.md) | Artefakte aus einem Maven-Projekt in ein anderes integrieren |
| [gui.md](docs/gui.md) | GUI-Architektur und Design-Patterns |

### Docker-Schnellstart

Siehe [DOCKER_GUIDE.md](docs/DOCKER_GUIDE.md) fuer eine ausfuehrliche Anleitung.

```bash
# PostgreSQL starten
cd docker && docker-compose up -d postgres

# Verbindung testen
docker exec -it template-postgres psql -U template -d templatedb

# Container stoppen
docker-compose down
```

### Maven-Artefakt-Integration

Siehe [Maven-Artefakt-Integration.md](docs/Maven-Artefakt-Integration.md) fuer Details.

```bash
# SNAPSHOT-Build (Standard)
mvn package

# Release-Build
mvn package -Prelease

# Mit spezifischem Branch
mvn package -Dtestfaelle.branch=feature-xyz
```

### Claude Code fortsetzen

```
Ich arbeite am Java-Projekt TemplateGUI unter E:\Projekte\ClaudeCode\TemplateGUI.
Bitte lies die Datei docs/CLAUDE_CONTEXT.md fuer den Kontext.
```

## Lizenz

Proprietary
