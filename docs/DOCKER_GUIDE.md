# Docker-Anleitung: Java-Projekte mit PostgreSQL

Diese Anleitung zeigt, wie man ein Java-Projekt (z.B. Swing-Anwendung) mit Docker und PostgreSQL containerisiert.

## Inhaltsverzeichnis

1. [Grundlagen](#1-grundlagen)
2. [Dockerfile erstellen](#2-dockerfile-erstellen)
3. [Docker Compose mit PostgreSQL](#3-docker-compose-mit-postgresql)
4. [X11-Forwarding für GUI-Anwendungen](#4-x11-forwarding-für-gui-anwendungen)
5. [Best Practices](#5-best-practices)
6. [Troubleshooting](#6-troubleshooting)

---

## 1. Grundlagen

### Voraussetzungen

- **Docker Desktop** (Windows/macOS) oder **Docker Engine** (Linux)
- **Docker Compose** (in Docker Desktop enthalten)

### Installation (Windows)

```powershell
# Via winget
winget install Docker.DockerDesktop

# Nach Installation: Docker Desktop starten und WSL2-Integration aktivieren
```

### Projektstruktur

```
mein-projekt/
├── src/                    # Java-Quellcode
├── pom.xml                 # Maven Build-Datei
├── Dockerfile              # Container-Definition
├── docker-compose.yml      # Multi-Container Setup
└── docker/
    ├── init-db.sql         # Datenbank-Initialisierung
    ├── config/             # Konfigurationsdateien
    ├── start-windows.bat   # Windows Starter
    └── start-linux.sh      # Linux Starter
```

---

## 2. Dockerfile erstellen

### Multi-Stage Build (empfohlen)

Ein Multi-Stage Build trennt Build- und Runtime-Umgebung, was kleinere Images erzeugt:

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-23 AS builder

WORKDIR /app

# Dependencies zuerst (wird gecacht, wenn sich pom.xml nicht ändert)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Quellcode kopieren und bauen
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:23-jre

WORKDIR /app

# JAR aus Builder-Stage kopieren
COPY --from=builder /app/target/*.jar ./app.jar

# Nicht als Root laufen
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

CMD ["java", "-jar", "app.jar"]
```

### Für GUI-Anwendungen (Swing/JavaFX)

GUI-Anwendungen brauchen X11-Bibliotheken:

```dockerfile
FROM eclipse-temurin:23-jdk

# X11-Bibliotheken für Swing GUI
RUN apt-get update && apt-get install -y --no-install-recommends \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libfreetype6 \
    fontconfig \
    fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/target/*.jar ./app.jar

# Display-Variable für X11
ENV DISPLAY=:0
ENV _JAVA_OPTIONS="-Dawt.useSystemAAFontSettings=on -Dswing.aatext=true"

CMD ["java", "-jar", "app.jar"]
```

---

## 3. Docker Compose mit PostgreSQL

### Basis docker-compose.yml

```yaml
version: '3.8'

services:
  # PostgreSQL Datenbank
  postgres:
    image: postgres:16-alpine
    container_name: myapp-db
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: myappdb
    volumes:
      # Persistente Daten
      - postgres_data:/var/lib/postgresql/data
      # Initialisierungs-Skript
      - ./docker/init-db.sql:/docker-entrypoint-initdb.d/init.sql:ro
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5
    networks:
      - app-network

  # Java-Anwendung
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: myapp
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      # Verbindung zur DB (Hostname = Service-Name)
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: myappdb
      DB_USER: postgres
      DB_PASSWORD: postgres
    networks:
      - app-network

networks:
  app-network:
    driver: bridge

volumes:
  postgres_data:
```

### Datenbank-Initialisierung (init-db.sql)

```sql
-- Tabellen erstellen
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Beispieldaten
INSERT INTO users (username, email) VALUES
    ('admin', 'admin@example.com'),
    ('user1', 'user1@example.com');
```

### JDBC-Verbindung im Java-Code

```java
// Aus Container: Hostname ist der Service-Name
String url = "jdbc:postgresql://postgres:5432/myappdb";

// Von außen (z.B. IDE): localhost
String url = "jdbc:postgresql://localhost:5432/myappdb";

// Flexibel mit Umgebungsvariablen
String host = System.getenv().getOrDefault("DB_HOST", "localhost");
String port = System.getenv().getOrDefault("DB_PORT", "5432");
String db = System.getenv().getOrDefault("DB_NAME", "myappdb");
String url = String.format("jdbc:postgresql://%s:%s/%s", host, port, db);
```

---

## 4. X11-Forwarding für GUI-Anwendungen

GUI-Container brauchen eine Verbindung zum Display des Host-Systems.

### Windows mit VcXsrv

1. **VcXsrv installieren:**
   ```powershell
   winget install marha.VcXsrv
   ```

2. **XLaunch starten** mit Einstellungen:
   - Multiple windows
   - Start no client
   - **"Disable access control"** aktivieren

3. **docker-compose.yml anpassen:**
   ```yaml
   app:
     environment:
       DISPLAY: host.docker.internal:0
   ```

4. **Start-Script (start-windows.bat):**
   ```batch
   @echo off
   echo Starte VcXsrv falls nicht aktiv...
   tasklist /FI "IMAGENAME eq vcxsrv.exe" | find /I "vcxsrv.exe" >nul || (
       start "" "C:\Program Files\VcXsrv\vcxsrv.exe" :0 -multiwindow -clipboard -ac
       timeout /t 2
   )

   echo Starte Docker Container...
   docker-compose up --build
   ```

### Linux

```yaml
# docker-compose.yml
app:
  environment:
    DISPLAY: ${DISPLAY}
  volumes:
    - /tmp/.X11-unix:/tmp/.X11-unix:rw
```

```bash
# Vor dem Start: X11-Zugriff erlauben
xhost +local:docker

# Container starten
docker-compose up --build
```

### macOS mit XQuartz

1. **XQuartz installieren:** https://www.xquartz.org/
2. **Einstellungen:** Security → "Allow connections from network clients"
3. **Terminal:**
   ```bash
   xhost +localhost
   ```
4. **docker-compose.yml:**
   ```yaml
   app:
     environment:
       DISPLAY: host.docker.internal:0
   ```

---

## 5. Best Practices

### Layer-Caching optimieren

```dockerfile
# SCHLECHT: Alles auf einmal kopieren
COPY . .
RUN mvn package

# GUT: Dependencies separat (werden gecacht)
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package
```

### .dockerignore verwenden

```dockerignore
# .dockerignore
target/
*.class
*.jar
.git/
.gitignore
.idea/
*.iml
docker/
*.md
```

### Nicht als Root laufen

```dockerfile
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser
```

### Healthchecks definieren

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/health || exit 1
```

### Umgebungsvariablen statt hardcoded Werte

```java
// Im Code
String dbHost = System.getenv().getOrDefault("DB_HOST", "localhost");

// In docker-compose.yml
environment:
  DB_HOST: postgres
```

### Volumes für Persistenz

```yaml
volumes:
  # Named Volume (von Docker verwaltet)
  postgres_data:/var/lib/postgresql/data

  # Bind Mount (lokales Verzeichnis)
  ./docker/config:/app/config
```

---

## 6. Troubleshooting

### Container startet nicht

```bash
# Logs anzeigen
docker-compose logs app
docker-compose logs postgres

# Status prüfen
docker-compose ps

# Interaktiv starten für Debugging
docker-compose run --rm app bash
```

### Datenbank-Verbindung schlägt fehl

```bash
# PostgreSQL Container prüfen
docker-compose logs postgres

# Manuell verbinden
docker exec -it myapp-db psql -U postgres -d myappdb

# Netzwerk prüfen
docker network inspect myproject_app-network
```

### GUI erscheint nicht (Windows)

1. VcXsrv läuft? (Tray-Icon prüfen)
2. "Disable access control" aktiviert?
3. Windows Firewall: VcXsrv erlauben
4. `DISPLAY` Variable korrekt? (`host.docker.internal:0`)

### Alles neu bauen

```bash
# Container stoppen und Volumes löschen
docker-compose down -v

# Cache löschen und neu bauen
docker-compose build --no-cache

# Neu starten
docker-compose up
```

### Port bereits belegt

```bash
# Wer benutzt Port 5432?
netstat -ano | findstr :5432

# Anderen Port in docker-compose.yml verwenden
ports:
  - "5433:5432"
```

---

## Schnellreferenz

| Befehl | Beschreibung |
|--------|--------------|
| `docker-compose up` | Container starten |
| `docker-compose up --build` | Neu bauen und starten |
| `docker-compose up -d` | Im Hintergrund starten |
| `docker-compose down` | Container stoppen |
| `docker-compose down -v` | Stoppen + Volumes löschen |
| `docker-compose logs -f` | Logs verfolgen |
| `docker-compose ps` | Container-Status |
| `docker-compose exec app bash` | Shell im Container öffnen |

---

## Weiterführende Links

- [Docker Dokumentation](https://docs.docker.com/)
- [Docker Compose Referenz](https://docs.docker.com/compose/compose-file/)
- [PostgreSQL Docker Image](https://hub.docker.com/_/postgres)
- [Eclipse Temurin (Java) Images](https://hub.docker.com/_/eclipse-temurin)
