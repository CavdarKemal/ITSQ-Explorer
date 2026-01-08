# Umgebungs-Lock-System

## Uebersicht

Das Umgebungs-Lock-System verhindert, dass mehrere Instanzen der Anwendung dieselbe Umgebung (z.B. ENE, ABE, GEE) gleichzeitig nutzen. Dies ist wichtig, um Datenkonflikte und Race-Conditions zu vermeiden.

## Technische Implementierung

### Warum ServerSocket statt FileLock?

| Methode | Vorteile | Nachteile |
|---------|----------|-----------|
| **FileLock (java.nio)** | Standard-API | Unzuverlaessig auf Windows zwischen Prozessen |
| **ServerSocket** | Zuverlaessig prozessuebergreifend, OS gibt Port bei Crash frei | Benoetigt freie Ports |

**Entscheidung:** ServerSocket ist die robustere Loesung fuer prozessuebergreifende Locks.

### Port-Zuordnung

Jede Umgebung bekommt einen eindeutigen Port auf `localhost`:

```
BASE_PORT = 47100

ENE -> 47100
ABE -> 47101
GEE -> 47102
Weitere -> BASE_PORT + hashCode(envName) % 100
```

## Architektur

```
┌─────────────────────────────────────────────────────────────┐
│                         Main.java                            │
│  - Registriert Shutdown Hook beim Start                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       MainFrame.java                         │
│  - Startup: switchEnvironment() mit Lock-Pruefung           │
│  - Combobox: loadSelectedConfig() mit Lock-Pruefung         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 TestEnvironmentManager.java                  │
│  - switchEnvironment(): Koordiniert Lock-Erwerb/Freigabe   │
│  - Ruft EnvironmentLockManager auf                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 EnvironmentLockManager.java                  │
│  - acquireLock(): Oeffnet ServerSocket auf Port             │
│  - releaseLock(): Schliesst ServerSocket                    │
│  - isLocked(): Prueft ob Port belegt                        │
└─────────────────────────────────────────────────────────────┘
```

## Dateien und ihre Aufgaben

### 1. EnvironmentLockManager.java (Kern-Komponente)

**Pfad:** `src/main/java/de/cavdar/gui/util/EnvironmentLockManager.java`

**Aufgaben:**
- Lock erwerben via ServerSocket
- Lock freigeben
- Pruefen ob Umgebung gesperrt ist
- Shutdown Hook registrieren

**Wichtige Methoden:**

```java
// Lock erwerben - gibt true zurueck wenn erfolgreich
public static synchronized boolean acquireLock(File envDir, String envName)

// Lock freigeben
public static synchronized void releaseLock()

// Pruefen ob Umgebung gesperrt
public static boolean isLocked(File envDir)

// Shutdown Hook registrieren (einmalig beim Start)
public static synchronized void registerShutdownHook()
```

### 2. TestEnvironmentManager.java (Integration)

**Aenderungen in switchEnvironment():**

```java
public static boolean switchEnvironment(String configFileName) {
    String envName = extractEnvironmentName(configFileName);

    // 1. Pruefen ob bereits in dieser Umgebung
    if (envName.equals(currentEnvironment)) {
        return true;
    }

    // 2. Verzeichnisse erstellen
    // ...

    // 3. NEU: Pruefen ob neue Umgebung gesperrt
    if (EnvironmentLockManager.isLocked(envDir)) {
        return false;
    }

    // 4. NEU: Alte Lock freigeben
    EnvironmentLockManager.releaseLock();

    // 5. NEU: Neue Lock erwerben
    if (!EnvironmentLockManager.acquireLock(envDir, envName)) {
        return false;
    }

    // 6. Logging konfigurieren etc.
    // ...

    return true;
}
```

### 3. MainFrame.java (UI-Integration)

**Startup-Pruefung in initConfigSelector():**

```java
if (!TestEnvironmentManager.switchEnvironment(currentConfigName)) {
    // Umgebung gesperrt - Alternative suchen
    for (int i = 0; i < configComboBox.getItemCount(); i++) {
        String altConfig = configComboBox.getItemAt(i);
        if (TestEnvironmentManager.switchEnvironment(altConfig)) {
            // Gefunden - wechseln und User informieren
            currentConfigName = altConfig;
            JOptionPane.showMessageDialog(...);
            break;
        }
    }

    if (!foundAlternative) {
        // Alle gesperrt - App beenden
        JOptionPane.showMessageDialog(...);
        System.exit(1);
    }
}
```

**Umgebungswechsel-Pruefung in loadSelectedConfig():**

```java
if (!TestEnvironmentManager.switchEnvironment(configName)) {
    JOptionPane.showMessageDialog(this,
        "Die Umgebung ist bereits gesperrt...",
        "Umgebung gesperrt", JOptionPane.WARNING_MESSAGE);
    configComboBox.setSelectedItem(currentConfigName); // Zuruecksetzen
    return;
}
```

### 4. Main.java (Shutdown Hook)

```java
public static void main(String[] args) {
    // Shutdown Hook registrieren - WICHTIG: Vor allem anderen!
    EnvironmentLockManager.registerShutdownHook();

    // Rest der Anwendung starten...
}
```

## Integration in bestehendes Projekt

### Schritt 1: EnvironmentLockManager.java kopieren

Kopiere die Klasse in dein Projekt und passe das Package an:

```java
package dein.projekt.util;

// ... Rest der Klasse
```

**Anpassungen:**
- `BASE_PORT`: Waehle einen freien Port-Bereich (z.B. 47100-47199)
- `ENV_PORTS`: Definiere deine Umgebungen

```java
private static final int BASE_PORT = 47100;

private static final Map<String, Integer> ENV_PORTS = new HashMap<>();
static {
    ENV_PORTS.put("DEV", BASE_PORT);
    ENV_PORTS.put("TEST", BASE_PORT + 1);
    ENV_PORTS.put("PROD", BASE_PORT + 2);
}
```

### Schritt 2: Shutdown Hook registrieren

In deiner `main()` Methode, **ganz am Anfang**:

```java
public static void main(String[] args) {
    EnvironmentLockManager.registerShutdownHook();
    // ... Rest
}
```

### Schritt 3: Lock beim Umgebungswechsel integrieren

Finde die Stelle, wo Umgebungen gewechselt werden, und fuege hinzu:

```java
public boolean switchToEnvironment(String envName) {
    File envDir = new File("path/to/" + envName);

    // Pruefen ob gesperrt
    if (EnvironmentLockManager.isLocked(envDir)) {
        showError("Umgebung " + envName + " ist gesperrt");
        return false;
    }

    // Alte Lock freigeben
    EnvironmentLockManager.releaseLock();

    // Neue Lock erwerben
    if (!EnvironmentLockManager.acquireLock(envDir, envName)) {
        showError("Konnte Lock nicht erwerben");
        return false;
    }

    // Umgebung wechseln...
    return true;
}
```

### Schritt 4: Startup-Pruefung hinzufuegen

Beim Anwendungsstart pruefen:

```java
String defaultEnv = getDefaultEnvironment();
if (!switchToEnvironment(defaultEnv)) {
    // Alternative suchen oder App beenden
    String alternative = findUnlockedEnvironment();
    if (alternative != null) {
        switchToEnvironment(alternative);
        showWarning("Gewechselt zu " + alternative);
    } else {
        showError("Keine freie Umgebung verfuegbar");
        System.exit(1);
    }
}
```

## Verhalten in verschiedenen Szenarien

| Szenario | Verhalten |
|----------|-----------|
| App startet, Umgebung frei | Lock wird erworben, App startet normal |
| App startet, Umgebung gesperrt | Automatischer Wechsel zu freier Umgebung |
| Alle Umgebungen gesperrt | Fehlermeldung, App beendet sich |
| User wechselt zu gesperrter Umgebung | Fehlermeldung, Combobox zurueckgesetzt |
| User wechselt zu freier Umgebung | Alte Lock freigegeben, neue erworben |
| App wird normal beendet | Shutdown Hook gibt Lock frei |
| App crasht | OS gibt Port automatisch frei |
| PC wird heruntergefahren | OS gibt Port automatisch frei |

## Fehlerbehebung

### Port bereits belegt (nicht durch diese App)

Wenn ein Port von einer anderen Anwendung belegt ist:

1. Port-Bereich aendern in `EnvironmentLockManager.java`
2. Oder belegende Anwendung finden: `netstat -ano | findstr 47100`

### Lock wird nicht freigegeben

Sollte normalerweise nicht passieren, da:
- Shutdown Hook bei normalem Beenden
- OS bei Crash

Falls doch: PC neustarten oder Port manuell freigeben.

### Firewall blockiert

ServerSocket verwendet nur `localhost` (127.0.0.1), sollte keine Firewall-Probleme verursachen.

## Abhaengigkeiten

Keine externen Abhaengigkeiten - nur Standard Java:
- `java.net.ServerSocket`
- `java.net.InetAddress`
- `java.nio.file.Files`

## Beispiel-Implementierung

Siehe vollstaendige Implementierung in:
- `src/main/java/de/cavdar/gui/util/EnvironmentLockManager.java`
- `src/main/java/de/cavdar/gui/util/TestEnvironmentManager.java`
- `src/main/java/de/cavdar/gui/design/base/MainFrame.java`
- `src/main/java/de/cavdar/gui/Main.java`
