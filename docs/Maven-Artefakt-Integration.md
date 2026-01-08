# Maven: Artefakte aus einem Projekt in ein anderes integrieren

Diese Anleitung beschreibt, wie man auslieferbare Artefakte (z.B. ZIP-Dateien) aus einem Maven-Projekt in ein anderes Projekt einbindet - ohne klassische Dependency-Verwaltung, sondern durch explizites Herunterladen aus dem Repository.

## Inhaltsverzeichnis

1. [Übersicht](#übersicht)
2. [Voraussetzungen](#voraussetzungen)
3. [Schritt 1: Quell-Projekt konfigurieren](#schritt-1-quell-projekt-konfigurieren)
4. [Schritt 2: Ziel-Projekt konfigurieren](#schritt-2-ziel-projekt-konfigurieren)
5. [Schritt 3: Assembly-Descriptor erstellen](#schritt-3-assembly-descriptor-erstellen)
6. [Schritt 4: Build und Deployment](#schritt-4-build-und-deployment)
7. [Versionsverwaltung mit Profilen](#versionsverwaltung-mit-profilen)
8. [Dependencies und zusätzliche Dateien](#dependencies-und-zusätzliche-dateien)
9. [Konfigurationsoptionen](#konfigurationsoptionen)
10. [Troubleshooting](#troubleshooting)

---

## Übersicht

### Architektur

```
┌─────────────────────────────────────────────────────────────────┐
│  QUELL-PROJEKT (z.B. testfaelle)                                │
│  ├── pom.xml                                                    │
│  └── src/assemble/distribution.xml                              │
│                        │                                        │
│                        ▼  mvn install / mvn deploy              │
│              ┌─────────────────────┐                            │
│              │ artifact-distribution.zip                        │
│              └─────────────────────┘                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼  Maven Repository (lokal/remote)
┌─────────────────────────────────────────────────────────────────┐
│  ~/.m2/repository/ oder Nexus/Artifactory                       │
│  └── groupId/artifactId/version/artifact-classifier.zip        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼  maven-dependency-plugin
┌─────────────────────────────────────────────────────────────────┐
│  ZIEL-PROJEKT (z.B. ITSQ-Test)                                  │
│  ├── pom.xml (mit dependency-plugin)                            │
│  ├── src/assembly/distribution.xml                              │
│  └── target/                                                    │
│      ├── downloaded-artifact.zip      (copy)                    │
│      ├── unpacked-content/            (unpack)                  │
│      └── final-distribution.zip       (assembly)                │
└─────────────────────────────────────────────────────────────────┘
```

### Verwendete Maven-Plugins

| Plugin | Zweck |
|--------|-------|
| `maven-assembly-plugin` | Erstellt auslieferbare Archive (ZIP, TAR, etc.) |
| `maven-dependency-plugin` | Lädt Artefakte aus Repository herunter |

---

## Voraussetzungen

- Maven 3.x installiert
- Java JDK (passend zur Projektversion)
- Zugriff auf Maven Repository (lokal oder remote)

---

## Schritt 1: Quell-Projekt konfigurieren

### 1.1 Assembly-Descriptor erstellen

Erstelle die Datei `src/assemble/distribution.xml` im Quell-Projekt:

```xml
<assembly xmlns="http://maven.apache.org/ASSEMBLY/2.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/ASSEMBLY/2.2.0
                              http://maven.apache.org/xsd/assembly-2.2.0.xsd">

    <!-- ID wird an den Artefaktnamen angehängt: artifact-{id}.zip -->
    <id>distribution</id>

    <formats>
        <format>zip</format>
    </formats>

    <!-- false = kein zusätzliches Wurzelverzeichnis im Archiv -->
    <includeBaseDirectory>false</includeBaseDirectory>

    <fileSets>
        <!-- Verzeichnisse/Dateien die ins Archiv sollen -->
        <fileSet>
            <directory>${basedir}/pfad/zum/inhalt</directory>
            <outputDirectory>/ziel-verzeichnis</outputDirectory>
            <includes>
                <include>**/*</include>
            </includes>
        </fileSet>
    </fileSets>

</assembly>
```

### 1.2 POM.xml konfigurieren

Füge das Assembly-Plugin zur `pom.xml` hinzu:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-assembly-plugin</artifactId>
            <version>3.6.0</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>single</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <descriptors>
                    <descriptor>src/assemble/distribution.xml</descriptor>
                </descriptors>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 1.3 Artefakt ins Repository installieren

```bash
# Lokales Repository
mvn clean install

# Remote Repository (Nexus, Artifactory)
mvn clean deploy
```

Das erzeugte Artefakt hat folgende Maven-Koordinaten:
- **groupId**: aus pom.xml
- **artifactId**: aus pom.xml
- **version**: aus pom.xml
- **classifier**: `distribution` (aus assembly-id)
- **type**: `zip`

---

## Schritt 2: Ziel-Projekt konfigurieren

### 2.1 Properties definieren

Definiere die Koordinaten des Quell-Artefakts als Properties:

```xml
<properties>
    <!-- Quell-Artefakt Koordinaten -->
    <source.groupId>testfaelle</source.groupId>
    <source.artifactId>itsq</source.artifactId>
    <source.version>1.0.0-SNAPSHOT</source.version>
    <source.classifier>distribution</source.classifier>
    <source.type>zip</source.type>

    <!-- Plugin Versionen -->
    <maven-dependency-plugin.version>3.6.1</maven-dependency-plugin.version>
    <maven-assembly-plugin.version>3.6.0</maven-assembly-plugin.version>
</properties>
```

### 2.2 Dependency-Plugin konfigurieren

Es gibt zwei Möglichkeiten: **copy** (ZIP bleibt gepackt) und **unpack** (ZIP wird entpackt).

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-dependency-plugin</artifactId>
            <version>${maven-dependency-plugin.version}</version>
            <executions>

                <!-- OPTION 1: Nur herunterladen (ZIP bleibt gepackt) -->
                <execution>
                    <id>download-artifact</id>
                    <phase>generate-resources</phase>
                    <goals>
                        <goal>copy</goal>
                    </goals>
                    <configuration>
                        <artifactItems>
                            <artifactItem>
                                <groupId>${source.groupId}</groupId>
                                <artifactId>${source.artifactId}</artifactId>
                                <version>${source.version}</version>
                                <classifier>${source.classifier}</classifier>
                                <type>${source.type}</type>
                                <outputDirectory>${project.build.directory}/downloads</outputDirectory>
                                <destFileName>source-artifact.zip</destFileName>
                            </artifactItem>
                        </artifactItems>
                    </configuration>
                </execution>

                <!-- OPTION 2: Herunterladen UND entpacken -->
                <execution>
                    <id>unpack-artifact</id>
                    <phase>generate-resources</phase>
                    <goals>
                        <goal>unpack</goal>
                    </goals>
                    <configuration>
                        <artifactItems>
                            <artifactItem>
                                <groupId>${source.groupId}</groupId>
                                <artifactId>${source.artifactId}</artifactId>
                                <version>${source.version}</version>
                                <classifier>${source.classifier}</classifier>
                                <type>${source.type}</type>
                                <outputDirectory>${project.build.directory}/unpacked</outputDirectory>
                            </artifactItem>
                        </artifactItems>
                    </configuration>
                </execution>

            </executions>
        </plugin>
    </plugins>
</build>
```

---

## Schritt 3: Assembly-Descriptor erstellen

Erstelle `src/assembly/distribution.xml` im Ziel-Projekt:

```xml
<assembly xmlns="http://maven.apache.org/ASSEMBLY/2.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/ASSEMBLY/2.2.0
                              http://maven.apache.org/xsd/assembly-2.2.0.xsd">

    <id>distribution</id>

    <formats>
        <format>zip</format>
    </formats>

    <includeBaseDirectory>false</includeBaseDirectory>

    <fileSets>
        <!-- Entpackte Inhalte einbinden -->
        <fileSet>
            <directory>${project.build.directory}/unpacked</directory>
            <outputDirectory>/UNTERVERZEICHNIS</outputDirectory>
            <includes>
                <include>**/*</include>
            </includes>
        </fileSet>

        <!-- Optional: Weitere eigene Dateien hinzufügen -->
        <fileSet>
            <directory>${basedir}/src/main/resources</directory>
            <outputDirectory>/config</outputDirectory>
        </fileSet>
    </fileSets>

</assembly>
```

### Assembly-Plugin zum Ziel-Projekt hinzufügen

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <version>${maven-assembly-plugin.version}</version>
    <executions>
        <execution>
            <id>create-distribution</id>
            <phase>package</phase>
            <goals>
                <goal>single</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <descriptors>
            <descriptor>src/assembly/distribution.xml</descriptor>
        </descriptors>
    </configuration>
</plugin>
```

---

## Schritt 4: Build und Deployment

### Build ausführen

```bash
# Ziel-Projekt bauen
mvn clean package
```

### Ergebnis

Nach dem Build findest du im `target/`-Verzeichnis:

```
target/
├── downloads/
│   └── source-artifact.zip          # Kopiertes ZIP (Option 1)
├── unpacked/                         # Entpackter Inhalt (Option 2)
│   ├── ARCHIV-BESTAND/
│   └── REF-EXPORTS/
└── projekt-version-distribution.zip  # Finales Artefakt
```

---

## Versionsverwaltung mit Profilen

Die Artefakt-Version und der Branch können flexibel über Maven-Profile gesteuert werden. Dies ermöglicht den einfachen Wechsel zwischen verschiedenen Versionen und Branches ohne die POM-Datei zu ändern.

### Properties definieren

Definiere Version und Branch als Properties:

```xml
<properties>
    <!-- Artefakt Koordinaten -->
    <testfaelle.groupId>testfaelle</testfaelle.groupId>
    <testfaelle.artifactId>itsq</testfaelle.artifactId>
    <!-- Version und Branch werden über Profile gesteuert -->
    <!-- Überschreibbar mit -Dtestfaelle.version=X.X.X -Dtestfaelle.branch=feature-xyz -->
    <testfaelle.branch>master</testfaelle.branch>
    <testfaelle.classifier>distribution-${testfaelle.branch}</testfaelle.classifier>
    <testfaelle.type>zip</testfaelle.type>
</properties>
```

### Profile definieren

Füge folgende Profile zur `pom.xml` hinzu:

```xml
<!-- Profile zur Steuerung der Testfaelle-Version und Branch -->
<profiles>
    <!-- SNAPSHOT-Version vom main-Branch (Standard) -->
    <profile>
        <id>snapshot</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <testfaelle.version>1.0.0-SNAPSHOT</testfaelle.version>
            <testfaelle.branch>master</testfaelle.branch>
        </properties>
    </profile>

    <!-- Release-Version vom main-Branch -->
    <profile>
        <id>release</id>
        <properties>
            <testfaelle.version>1.0.0</testfaelle.version>
            <testfaelle.branch>master</testfaelle.branch>
        </properties>
    </profile>

    <!-- Entwicklungs-Branch -->
    <profile>
        <id>develop</id>
        <properties>
            <testfaelle.version>1.0.0-SNAPSHOT</testfaelle.version>
            <testfaelle.branch>develop</testfaelle.branch>
        </properties>
    </profile>

    <!-- Feature-Branch (Branch muss mit -Dtestfaelle.branch=feature-xyz angegeben werden) -->
    <profile>
        <id>feature</id>
        <properties>
            <testfaelle.version>1.0.0-SNAPSHOT</testfaelle.version>
            <!-- Branch wird via -Dtestfaelle.branch=feature-xyz überschrieben -->
        </properties>
    </profile>
</profiles>
```

### Verwendung

#### Profil-basierte Steuerung

| Profil | Kommando | Version | Branch | Classifier |
|--------|----------|---------|--------|------------|
| `snapshot` | `mvn package` | `1.0.0-SNAPSHOT` | `main` | `distribution-main` |
| `release` | `mvn package -Prelease` | `1.0.0` | `main` | `distribution-main` |
| `develop` | `mvn package -Pdevelop` | `1.0.0-SNAPSHOT` | `develop` | `distribution-develop` |
| `feature` | `mvn package -Pfeature -Dtestfaelle.branch=xyz` | `1.0.0-SNAPSHOT` | `xyz` | `distribution-xyz` |

```bash
# Standard (SNAPSHOT vom main-Branch)
mvn clean package

# Release-Version vom main-Branch
mvn clean package -Prelease

# Entwicklungs-Branch
mvn clean package -Pdevelop

# Feature-Branch
mvn clean package -Pfeature -Dtestfaelle.branch=feature-login
```

#### Direkte Angabe von Version und Branch

Version und Branch können auch direkt beim Build überschrieben werden:

```bash
# Version und Branch direkt angeben
mvn clean package -Dtestfaelle.version=2.0.0 -Dtestfaelle.branch=hotfix-123

# Nur Branch ändern (Version aus Profil)
mvn clean package -Dtestfaelle.branch=release-1.5

# Nur Version ändern (Branch aus Profil)
mvn clean package -Dtestfaelle.version=1.5.0-RC1
```

### Quell-Projekt für Branch-Support konfigurieren

Damit das Quell-Projekt Artefakte mit Branch im Classifier erzeugt, muss die `distribution.xml` angepasst werden:

```xml
<!-- In src/assemble/distribution.xml des Quell-Projekts -->
<assembly xmlns="http://maven.apache.org/ASSEMBLY/2.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/ASSEMBLY/2.2.0
                              http://maven.apache.org/xsd/assembly-2.2.0.xsd">

    <!-- Branch wird über Property gesteuert: -Dtestfaelle.branch=xyz -->
    <id>distribution-${testfaelle.branch}</id>

    <formats>
        <format>zip</format>
    </formats>
    <!-- ... rest der Konfiguration ... -->
</assembly>
```

Und in der `pom.xml` des Quell-Projekts die Branch-Property definieren:

```xml
<properties>
    <!-- Branch für Distribution-Classifier (überschreibbar mit -Dtestfaelle.branch=xyz) -->
    <testfaelle.branch>master</testfaelle.branch>
</properties>

<profiles>
    <!-- Branch-Profile für Distribution -->
    <profile>
        <id>branch-main</id>
        <properties>
            <testfaelle.branch>master</testfaelle.branch>
        </properties>
    </profile>
    <profile>
        <id>branch-develop</id>
        <properties>
            <testfaelle.branch>develop</testfaelle.branch>
        </properties>
    </profile>
</profiles>
```

#### Build des Quell-Projekts mit Branch

```bash
# Standard (main-Branch)
mvn clean install

# Mit develop-Branch
mvn clean install -Dtestfaelle.branch=develop

# Mit Feature-Branch
mvn clean install -Dtestfaelle.branch=feature-new-tests
```

Das erzeugte Artefakt hat dann den Namen:
- `itsq-1.0.0-SNAPSHOT-distribution-main.zip`
- `itsq-1.0.0-SNAPSHOT-distribution-develop.zip`
- `itsq-1.0.0-SNAPSHOT-distribution-feature-new-tests.zip`

### Erweiterte Profile

#### Profil mit mehreren Artefakten und Branches

```xml
<profile>
    <id>full-integration</id>
    <properties>
        <testfaelle.version>1.0.0</testfaelle.version>
        <testfaelle.branch>master</testfaelle.branch>
        <additional.artifact.version>2.0.0</additional.artifact.version>
        <additional.artifact.branch>release</additional.artifact.branch>
    </properties>
</profile>
```

#### Profil mit Umgebungs-Aktivierung (CI/CD)

```xml
<profile>
    <id>ci-build</id>
    <activation>
        <!-- Automatisch aktiv wenn CI-Umgebungsvariable gesetzt -->
        <property>
            <name>env.CI</name>
            <value>true</value>
        </property>
    </activation>
    <properties>
        <testfaelle.version>${env.ARTIFACT_VERSION}</testfaelle.version>
        <testfaelle.branch>${env.GIT_BRANCH}</testfaelle.branch>
    </properties>
</profile>
```

#### Profil mit JDK-Aktivierung

```xml
<profile>
    <id>java17-compat</id>
    <activation>
        <jdk>[17,)</jdk>
    </activation>
    <properties>
        <testfaelle.version>2.0.0</testfaelle.version>
        <testfaelle.branch>java17</testfaelle.branch>
    </properties>
</profile>
```

### Best Practices

1. **Standard-Profil definieren**: Immer ein Profil mit `<activeByDefault>true</activeByDefault>` setzen
2. **Sprechende Namen**: Profile nach ihrem Zweck benennen (`snapshot`, `release`, `develop`, `feature`)
3. **Branch-Konvention**: Einheitliche Branch-Namen verwenden (z.B. `main`, `develop`, `feature-*`, `hotfix-*`)
4. **Dokumentation**: Profile im POM-Kommentar dokumentieren
5. **CI/CD-Integration**: Profile für automatisierte Builds mit Umgebungsvariablen vorsehen
6. **Konsistenz**: Quell- und Ziel-Projekt sollten die gleichen Branch-Namen verwenden

---

## Dependencies und zusätzliche Dateien

Neben den heruntergeladenen Artefakten können auch Projekt-Dependencies (JARs) und zusätzliche Dateien (Konfiguration, Start-Skripte) in die Distribution aufgenommen werden.

### Dependencies in lib-Verzeichnis kopieren

Um alle Runtime-Dependencies in ein lib-Verzeichnis zu kopieren, füge folgende Execution zum `maven-dependency-plugin` hinzu:

```xml
<execution>
    <id>copy-dependencies</id>
    <phase>prepare-package</phase>
    <goals>
        <goal>copy-dependencies</goal>
    </goals>
    <configuration>
        <outputDirectory>${project.build.directory}/lib</outputDirectory>
        <includeScope>runtime</includeScope>
    </configuration>
</execution>
```

### Dependencies als Maven-Abhängigkeiten definieren

```xml
<properties>
    <log4j.version>1.2.12</log4j.version>
    <slf4j-api.version>2.0.9</slf4j-api.version>
    <slf4j-reload4j.version>2.0.6</slf4j-reload4j.version>
</properties>

<dependencies>
    <dependency>
        <groupId>log4j</groupId>
        <artifactId>log4j</artifactId>
        <version>${log4j.version}</version>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>${slf4j-api.version}</version>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-reload4j</artifactId>
        <version>${slf4j-reload4j.version}</version>
    </dependency>
</dependencies>
```

### Assembly-Descriptor für komplette Distribution

Der Assembly-Descriptor kann so konfiguriert werden, dass er folgende Inhalte enthält:
- Delivery-Dateien (Konfiguration, Start-Skripte)
- Heruntergeladene Artefakte
- Projekt-JAR und Dependencies

```xml
<assembly xmlns="http://maven.apache.org/ASSEMBLY/2.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/ASSEMBLY/2.2.0
                              http://maven.apache.org/xsd/assembly-2.2.0.xsd">

    <id>distribution</id>

    <formats>
        <format>zip</format>
    </formats>

    <!-- true = Wurzelverzeichnis mit Projektname im Archiv -->
    <includeBaseDirectory>true</includeBaseDirectory>

    <fileSets>
        <!-- Delivery-Dateien aus Projektwurzel -->
        <fileSet>
            <directory>${project.basedir}/delivery</directory>
            <includes>
                <include>log4j.properties</include>
                <include>config.properties</include>
                <include>startGUI.cmd</include>
            </includes>
            <outputDirectory>/</outputDirectory>
        </fileSet>

        <!-- Entpackte Testfaelle einbinden -->
        <fileSet>
            <directory>${project.build.directory}/testfaelle</directory>
            <outputDirectory>/ITSQ</outputDirectory>
            <includes>
                <include>**/*</include>
            </includes>
        </fileSet>

        <!-- Dependencies in lib-Verzeichnis -->
        <fileSet>
            <directory>${project.build.directory}/lib</directory>
            <outputDirectory>/lib</outputDirectory>
            <includes>
                <include>*.jar</include>
            </includes>
        </fileSet>
    </fileSets>

    <!-- Projekt-JAR einbinden -->
    <files>
        <file>
            <source>${project.build.directory}/${project.build.finalName}.jar</source>
            <outputDirectory>/lib</outputDirectory>
        </file>
    </files>

</assembly>
```

### Ergebnis-Struktur

Nach dem Build enthält die Distribution-ZIP folgende Struktur:

```
ITSQ-Test-1.0-SNAPSHOT/
├── config.properties          # Aus delivery/
├── log4j.properties           # Aus delivery/
├── startGUI.cmd               # Aus delivery/
├── ITSQ/                      # Heruntergeladenes Artefakt
│   ├── ARCHIV-BESTAND/
│   └── REF-EXPORTS/
└── lib/                       # Alle JARs
    ├── ITSQ-Test-1.0-SNAPSHOT.jar   # Projekt-JAR
    ├── log4j-1.2.12.jar
    ├── reload4j-1.2.22.jar
    ├── slf4j-api-2.0.9.jar
    └── slf4j-reload4j-2.0.6.jar
```

### Wichtige Hinweise

1. **Pfade**: `${project.basedir}` zeigt auf die Projektwurzel, `${project.build.directory}` auf `target/`
2. **Phase**: `copy-dependencies` sollte in Phase `prepare-package` laufen, damit die JARs vor dem Assembly verfügbar sind
3. **includeScope**: Mit `runtime` werden nur Runtime-Dependencies kopiert (keine Test-Dependencies)
4. **Projekt-JAR**: Das `<files>`-Element bindet das eigene Projekt-JAR separat ein

---

## Konfigurationsoptionen

### Mehrere Artefakte einbinden

```xml
<artifactItems>
    <artifactItem>
        <groupId>com.example</groupId>
        <artifactId>artifact-1</artifactId>
        <version>1.0.0</version>
        <classifier>distribution</classifier>
        <type>zip</type>
        <outputDirectory>${project.build.directory}/artifact1</outputDirectory>
    </artifactItem>
    <artifactItem>
        <groupId>com.example</groupId>
        <artifactId>artifact-2</artifactId>
        <version>2.0.0</version>
        <classifier>resources</classifier>
        <type>zip</type>
        <outputDirectory>${project.build.directory}/artifact2</outputDirectory>
    </artifactItem>
</artifactItems>
```

### Dateien filtern beim Entpacken

```xml
<artifactItem>
    <groupId>${source.groupId}</groupId>
    <artifactId>${source.artifactId}</artifactId>
    <version>${source.version}</version>
    <classifier>${source.classifier}</classifier>
    <type>${source.type}</type>
    <outputDirectory>${project.build.directory}/unpacked</outputDirectory>
    <!-- Nur bestimmte Dateien entpacken -->
    <includes>**/*.xml,**/*.cfg</includes>
    <!-- Bestimmte Dateien ausschließen -->
    <excludes>**/test/**</excludes>
</artifactItem>
```

### Remote Repository konfigurieren

Falls das Artefakt auf einem Remote-Repository liegt:

```xml
<repositories>
    <repository>
        <id>company-nexus</id>
        <url>https://nexus.company.com/repository/maven-releases/</url>
    </repository>
</repositories>
```

### Verschiedene Formate

```xml
<formats>
    <format>zip</format>
    <format>tar.gz</format>
    <format>tar.bz2</format>
</formats>
```

---

## Troubleshooting

### Problem: Artefakt nicht gefunden

**Fehlermeldung:**
```
Could not find artifact groupId:artifactId:zip:classifier:version
```

**Lösung:**
1. Prüfen ob das Quell-Projekt mit `mvn install` installiert wurde
2. Classifier und Type korrekt angegeben
3. Repository-Konfiguration prüfen

```bash
# Prüfen ob Artefakt im lokalen Repo existiert
ls ~/.m2/repository/groupId/artifactId/version/
```

### Problem: Leeres ZIP

**Ursache:** Das Quellverzeichnis im Assembly-Descriptor existiert nicht oder ist leer.

**Lösung:** Pfade im Assembly-Descriptor prüfen:
```xml
<!-- ${project.build.directory} = target/ -->
<directory>${project.build.directory}/unpacked</directory>
```

### Problem: Falscher Pfad im Assembly

**Ursache:** `${basedir}` zeigt auf das aktuelle Modul-Verzeichnis.

**Lösung bei Multi-Modul-Projekten:**
```xml
<!-- Relativ zum aktuellen Modul -->
<directory>${basedir}/../anderes-modul/verzeichnis</directory>

<!-- Oder absoluter Pfad via Property -->
<directory>${project.parent.basedir}/anderes-modul/verzeichnis</directory>
```

### Build-Reihenfolge sicherstellen

Die Phase `generate-resources` läuft vor `package`. Falls Timing-Probleme auftreten:

```xml
<execution>
    <id>unpack-artifact</id>
    <!-- Frühere Phase wählen -->
    <phase>initialize</phase>
    <goals>
        <goal>unpack</goal>
    </goals>
</execution>
```

---

## Vollständiges Beispiel

### Ziel-Projekt pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>ITSQ-Test</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Quell-Artefakt Koordinaten -->
        <testfaelle.groupId>testfaelle</testfaelle.groupId>
        <testfaelle.artifactId>itsq</testfaelle.artifactId>
        <!-- Version und Branch werden über Profile gesteuert -->
        <!-- Überschreibbar mit -Dtestfaelle.version=X.X.X -Dtestfaelle.branch=feature-xyz -->
        <testfaelle.branch>master</testfaelle.branch>
        <testfaelle.classifier>distribution-${testfaelle.branch}</testfaelle.classifier>
        <testfaelle.type>zip</testfaelle.type>

        <!-- Plugin Versionen -->
        <maven-dependency-plugin.version>3.6.1</maven-dependency-plugin.version>
        <maven-assembly-plugin.version>3.6.0</maven-assembly-plugin.version>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>${maven-dependency-plugin.version}</version>
                <executions>
                    <execution>
                        <id>unpack-testfaelle</id>
                        <phase>generate-resources</phase>
                        <goals>
                            <goal>unpack</goal>
                        </goals>
                        <configuration>
                            <artifactItems>
                                <artifactItem>
                                    <groupId>${testfaelle.groupId}</groupId>
                                    <artifactId>${testfaelle.artifactId}</artifactId>
                                    <version>${testfaelle.version}</version>
                                    <classifier>${testfaelle.classifier}</classifier>
                                    <type>${testfaelle.type}</type>
                                    <outputDirectory>${project.build.directory}/testfaelle</outputDirectory>
                                </artifactItem>
                            </artifactItems>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>${maven-assembly-plugin.version}</version>
                <executions>
                    <execution>
                        <id>create-distribution</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <descriptors>
                        <descriptor>src/assembly/distribution.xml</descriptor>
                    </descriptors>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <!-- Profile zur Steuerung der Testfaelle-Version und Branch -->
    <profiles>
        <!-- SNAPSHOT-Version vom main-Branch (Standard) -->
        <profile>
            <id>snapshot</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <testfaelle.version>1.0.0-SNAPSHOT</testfaelle.version>
                <testfaelle.branch>master</testfaelle.branch>
            </properties>
        </profile>

        <!-- Release-Version vom main-Branch -->
        <profile>
            <id>release</id>
            <properties>
                <testfaelle.version>1.0.0</testfaelle.version>
                <testfaelle.branch>master</testfaelle.branch>
            </properties>
        </profile>

        <!-- Entwicklungs-Branch -->
        <profile>
            <id>develop</id>
            <properties>
                <testfaelle.version>1.0.0-SNAPSHOT</testfaelle.version>
                <testfaelle.branch>develop</testfaelle.branch>
            </properties>
        </profile>

        <!-- Feature-Branch -->
        <profile>
            <id>feature</id>
            <properties>
                <testfaelle.version>1.0.0-SNAPSHOT</testfaelle.version>
                <!-- Branch wird via -Dtestfaelle.branch=feature-xyz überschrieben -->
            </properties>
        </profile>
    </profiles>

</project>
```

### Build-Kommandos

```bash
# Standard-Build (SNAPSHOT-Version vom main-Branch)
mvn clean package

# Build mit Release-Version vom main-Branch
mvn clean package -Prelease

# Build mit develop-Branch
mvn clean package -Pdevelop

# Build mit Feature-Branch
mvn clean package -Pfeature -Dtestfaelle.branch=feature-login

# Build mit spezifischer Version und Branch
mvn clean package -Dtestfaelle.version=2.0.0 -Dtestfaelle.branch=hotfix-123
```

---

## Referenzen

- [Maven Assembly Plugin Dokumentation](https://maven.apache.org/plugins/maven-assembly-plugin/)
- [Maven Dependency Plugin Dokumentation](https://maven.apache.org/plugins/maven-dependency-plugin/)
- [Assembly Descriptor Format](https://maven.apache.org/plugins/maven-assembly-plugin/assembly.html)
