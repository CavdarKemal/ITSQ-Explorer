# ITSQ-Explorer Docker Setup

Diese Anleitung beschreibt, wie Sie die ITSQ-Explorer-Anwendung mit PostgreSQL in Docker-Containern ausfuehren.

## Voraussetzungen

### Für alle Plattformen
- Docker Desktop oder Docker Engine
- Docker Compose

### Für Windows (X11 Display)
1. **VcXsrv installieren**: https://sourceforge.net/projects/vcxsrv/
2. **XLaunch starten** mit folgenden Einstellungen:
   - Multiple windows
   - Start no client
   - ✅ **"Disable access control"** (wichtig!)

### Für Linux
- X11 Display Server (normalerweise bereits vorhanden)
- `xhost` Befehl verfügbar

### Für macOS
1. **XQuartz installieren**: https://www.xquartz.org/
2. XQuartz starten und in Einstellungen:
   - Security → "Allow connections from network clients" aktivieren
3. Terminal: `xhost +localhost`

## Schnellstart

### Windows
```batch
cd docker
start-windows.bat
```

### Linux
```bash
cd docker
chmod +x start-linux.sh
./start-linux.sh
```

### Manueller Start
```bash
# Im Projektverzeichnis
docker-compose up --build
```

## Container stoppen

### Windows
```batch
docker\stop.bat
```

### Linux/macOS
```bash
docker-compose down
```

## Datenbank-Verbindung

Die PostgreSQL-Datenbank ist unter folgenden Einstellungen erreichbar:

| Parameter | Wert |
|-----------|------|
| Host | `localhost` (von außen) oder `postgres` (im Container) |
| Port | `5432` |
| Datenbank | `itsq-explorer` |
| Benutzer | `postgres` |
| Passwort | `postgres` |

### JDBC URL
```
jdbc:postgresql://postgres:5432/itsq-explorer
```

## Struktur

```
docker/
├── init-db.sql         # Datenbank-Initialisierung (Tabellen, Beispieldaten)
├── start-windows.bat   # Windows Starter
├── start-linux.sh      # Linux Starter
├── stop.bat            # Windows Stopper
└── README.md           # Diese Datei

Dockerfile              # Multi-Stage Build für die Java-App
docker-compose.yml      # Container-Orchestrierung
```

## Beispiel-Daten

Die Datenbank wird automatisch mit folgenden Tabellen initialisiert:

- **customers** - Kundenstammdaten
- **orders** - Bestellungen
- **products** - Produkte
- **v_order_summary** - View für Bestellübersicht

## Troubleshooting

### Fenster erscheint nicht (Windows)
1. Stellen Sie sicher, dass VcXsrv läuft
2. Prüfen Sie, ob "Disable access control" aktiviert ist
3. Firewall: Erlauben Sie VcXsrv den Netzwerkzugriff

### Fenster erscheint nicht (Linux)
```bash
# X11 Zugriff erlauben
xhost +local:docker

# DISPLAY Variable prüfen
echo $DISPLAY
```

### Container startet nicht
```bash
# Logs anzeigen
docker-compose logs app

# Container Status
docker-compose ps
```

### Datenbank-Verbindung schlägt fehl
```bash
# PostgreSQL Container prüfen
docker-compose logs postgres

# Manuell verbinden
docker exec -it itsq-explorer-db psql -U postgres -d itsq-explorer
```

### Alles neu bauen
```bash
docker-compose down -v
docker-compose build --no-cache
docker-compose up
```

## Volumes

| Volume | Beschreibung |
|--------|--------------|
| `postgres_data` | PostgreSQL Datenbankdateien |
| `app_config` | Anwendungskonfiguration |

### Volumes löschen
```bash
docker-compose down -v
```
