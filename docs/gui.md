# ITSQ-Explorer GUI Dokumentation

## Uebersicht

Die ITSQ-Explorer GUI ist eine Java Swing MDI-Anwendung (Multiple Document Interface) mit strikter **Design-View-Trennung**. Das Architekturkonzept basiert auf dem StandardMDIGUI-Framework.

## Architektur-Prinzipien

### Design-View-Trennung

```
                          ┌───────────────┐
                          │   ViewInfo    │
                          │  (Interface)  │
                          └───────┬───────┘
                                  │ implements
┌──────────────────────────┴──────────────────────────────────┐
│                         BaseView                            │
│                   (abstrakte Klasse)                        │
│              extends JInternalFrame                         │
│                                                             │
│  Template Methods:                                          │
│  - createPanel()         → Panel erstellen                  │
│  - setupToolbarActions() → Button-Actions binden            │
│  - setupListeners()      → Weitere Listener (optional)      │
│                                                             │
│  Features:                                                  │
│  - executeTask(Runnable) → Async mit SwingWorker            │
│  - Cancel-Mechanismus                                       │
│  - Progress-Anzeige                                         │
└──────────────────────────┬──────────────────────────────────┘
                           │ verwendet
┌──────────────────────────┴──────────────────────────────────┐
│                      BaseViewPanel                          │
│                   (abstrakte Klasse)                        │
│                                                             │
│  Wrapper fuer JFormDesigner-generierte Panels               │
│                                                             │
│  Abstrakte Methoden:                                        │
│  - getViewToolbar()  → JToolBar                             │
│  - getProgressBar()  → JProgressBar                         │
│  - getCancelButton() → JButton                              │
└─────────────────────────────────────────────────────────────┘
```

### Kernprinzip: Komposition statt Vererbung

- **Design-Klassen** (JFormDesigner): Nur GUI-Komponenten, keine Logik
- **Panel-Wrapper**: Kapseln JFormDesigner-Panels, bieten einheitliche Schnittstelle
- **View-Klassen**: Nur Business-Logik und Event-Handler

## Package-Struktur

```
de.cavdar.gui
├── Main.java                   # Einstiegspunkt
├── design/
│   ├── base/                   # BaseViewPanel, MainFrame, DesktopPanel, SettingsPanel, EmbeddablePanel
│   └── db/                     # DatabaseViewPanel
├── view/
│   ├── base/                   # BaseView, ViewInfo
│   ├── db/                     # DatabaseView
│   └── itsq/                   # ItsqExplorerView
├── model/base/                 # AppConfig, ConfigEntry, ConnectionInfo
├── util/                       # Utilities
├── exception/                  # Exceptions
└── itsq/                       # ITSQ Explorer Subsystem (46 Klassen)
    ├── design/                 # ItsqMainPanel, ItsqTreePanel, ItsqEditorPanel, etc.
    ├── model/                  # ItsqItem, ItsqRoot, ItsqCustomer, ItsqScenario, etc.
    ├── tree/                   # ItsqTreeModel, ItsqTreeNode, etc.
    └── view/                   # ItsqMainView, ItsqTreeView, ItsqEditorView, etc.
```

## Klassen-Dokumentation

### ViewInfo (Interface)

Definiert Metadaten fuer automatische Menue- und Toolbar-Generierung.

```java
public interface ViewInfo {
    String getMenuLabel();                    // Menue-Text (erforderlich)
    default String getToolbarLabel();         // Toolbar-Text (optional)
    default Icon getIcon();                   // Icon (optional)
    default KeyStroke getKeyboardShortcut();  // Tastenkuerzel (optional)
    default String getMenuGroup();            // Menue-Gruppe (optional)
    default String getToolbarTooltip();       // Tooltip (optional)
}
```

### BaseViewPanel (Abstrakt)

Abstrakte Basisklasse fuer alle GUI-Panels.

```java
public abstract class BaseViewPanel extends JPanel {
    public abstract JToolBar getViewToolbar();
    public abstract JProgressBar getProgressBar();
    public abstract JButton getCancelButton();
    public void setProgressVisible(boolean visible, boolean indeterminate);
}
```

### BaseView (Abstrakt)

Abstrakte Basisklasse fuer alle Views. Implementiert das Template Method Pattern.

```java
public abstract class BaseView extends JInternalFrame implements ViewInfo {
    protected BaseViewPanel panel;
    protected SwingWorker<Void, Void> currentWorker;

    protected abstract BaseViewPanel createPanel();
    protected abstract void setupToolbarActions();
    protected void setupListeners() { }
    protected void executeTask(Runnable taskLogic);
}
```

## Anwendungslayout

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Config-Toolbar                                                              │
│  [Config▼][↻] [DB▼][🗄] [Source▼] [Type▼] [Rev▼] ☐Dump ☐SFTP ☐Export...     │
├─────────────────────────────────────────────────────────────────────────────┤
│  View-Toolbar                                                                │
│  [Views:] [ITSQ Explorer]                                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                         JDesktopPane                                         │
│                                                                              │
│      ┌─────────────────────────────┐   ┌─────────────────────────────┐      │
│      │ ItsqExplorerView            │   │ DatabaseView                │      │
│      │  📁 ITSQ Root               │   │  [SQL-Editor]               │      │
│      │   └─📁 Customers            │   │  [Ergebnis-Tabelle]         │      │
│      │      └─📁 Scenarios         │   │                             │      │
│      │         └─📄 Files          │   │                             │      │
│      └─────────────────────────────┘   └─────────────────────────────┘      │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Dual-Toolbar Konzept

**Config-Toolbar** (obere Zeile):
- Konfigurationsdatei-Auswahl (`*-config.properties`)
- DB-Verbindungs-Auswahl
- Testquellen, Testtypen, ITSQ-Revisionen
- Feature-Flags (Checkboxen)

**View-Toolbar** (zweite Zeile):
- Buttons zum Oeffnen der registrierten Views
- Dynamisch basierend auf `registerView()` Aufrufen

## ItsqExplorerView (ITSQ-TestSet Verwaltung)

Die ItsqExplorerView bietet eine Verwaltungsoberflaeche fuer ITSQ-Testsets.

### Features

- **TestSet-Auswahl**: ComboBox mit Historie der zuletzt verwendeten Verzeichnisse
- **Mehrfache Filter**: Kombinierbare Filter fuer praezise Suche
- **Tree-Ansicht**: Hierarchische Darstellung der ITSQ-Verzeichnisstruktur
- **Detail-Views**: Kontextabhaengige Detailansichten per CardLayout

### Filter-Funktionen

| Filter | Beschreibung | Werte |
|--------|--------------|-------|
| **Text-Filter** | Sucht nach Datei-/Ordnernamen (case-insensitive) | Freie Eingabe |
| **Quelle** | Filtert nach Hauptverzeichnis | Alle, ARCHIV-BESTAND, REF-EXPORTS |
| **Phase** | Filtert nach Phase-Unterverzeichnis | Alle, PHASE-1, PHASE-2 |
| **Active Only** | Zeigt nur aktive Elemente | Checkbox |

### Verzeichnisstruktur

```
ITSQ/
├── ARCHIV-BESTAND/
│   ├── PHASE-1/
│   │   └── *.xml
│   └── PHASE-2/
│       └── *.xml
└── REF-EXPORTS/
    ├── PHASE-1/
    │   └── c0x/ (Customer)
    │       ├── Options.cfg
    │       └── Relevanz-xyz/ (Scenario)
    │           ├── *.xml
    │           └── *.properties
    └── PHASE-2/
        └── ...
```

### Tastenkuerzel

| Shortcut | Funktion |
|----------|----------|
| Ctrl+J | ItsqExplorerView oeffnen |
| Ctrl+Shift+M | ItsqMigrationToolView oeffnen |

## ItsqMigrationToolView (OLD -> NEW Migration)

Die ItsqMigrationToolView ermoeglicht die Migration von der OLD ITSQ-Struktur (ohne Phasen in REF-EXPORTS) zur NEW-Struktur (mit PHASE-1/PHASE-2 Trennung).

### Features

- **Vorschau-Modus**: Analysiert die OLD-Struktur und zeigt detaillierte Statistiken
- **Phasenzuordnung**: Automatische Berechnung basierend auf ARCHIV-BESTAND Verfuegbarkeit
- **Gefilterte Dateigenerierung**: TestCrefos.properties und Relevanz.properties werden gefiltert
- **Fortschrittsanzeige**: Echtzeit-Progress mit Prozentanzeige und Statusmeldungen
- **Hintergrund-Ausfuehrung**: Alle Operationen laufen in SwingWorker
- **Backup-Option**: Optionales Backup vor Migration

### Migrationslogik

Fuer jeden Testfall (p0x, x0x, n0x) in Relevanz.properties:

| Testfall-Typ | Bedingung fuer PHASE-1 | Bedingung fuer PHASE-2 |
|--------------|------------------------|------------------------|
| p0x (positiv) | ARCHIV-BESTAND-PH1/{crefo}.xml MUSS existieren | ARCHIV-BESTAND-PH2/{crefo}.xml MUSS existieren |
| x0x (loeschsatz) | ARCHIV-BESTAND-PH1/{crefo}.xml MUSS existieren | ARCHIV-BESTAND-PH2/{crefo}.xml MUSS existieren |
| n0x (negativ) | ARCHIV-BESTAND-PH1/{crefo}.xml DARF NICHT existieren | ARCHIV-BESTAND-PH2/{crefo}.xml DARF NICHT existieren |

Ein Kunde gehoert zu einer Phase, wenn mindestens ein Testfall fuer diese Phase gueltig ist.

### GUI Layout

```
+---------------------------------------------------------------+
| [Quelle: OLD Pfad] [...] [Ziel: NEW Pfad] [...] [Vorschau]    |
+---------------------------------------------------------------+
|  +--- Vorschau ------------------------------------------------+
|  | Kunden:    7 -> PHASE-1: 2, PHASE-2: 5                      |
|  | Szenarien: 14 -> PHASE-1: 4, PHASE-2: 10                    |
|  | Testfaelle: 45 -> PHASE-1: 12, PHASE-2: 33                  |
|  +-------------------------------------------------------------+
|  +--- Details Tabelle -----------------------------------------+
|  | Kunde | Szenario        | Test | PH1 | PH2 | Status         |
|  | c01   | Relevanz_Positiv| p01  | JA  | JA  | OK             |
|  | c01   | Relevanz_Negativ| n01  | JA  | NEIN| WARNUNG        |
|  +-------------------------------------------------------------+
+---------------------------------------------------------------+
| [x] Backup erstellen  [ ] Ueberschreiben  [Migration starten] |
| [================Fortschritt================] [Abbrechen]      |
+---------------------------------------------------------------+
```

### Service-Layer Architektur

```
de.cavdar.itsq.migration/
├── model/
│   ├── MigrationConfig.java           # Konfiguration (Quell-/Zielpfad, Optionen)
│   ├── MigrationResult.java           # Ergebnis mit Statistiken und Fehlern
│   ├── TestCasePhaseAssignment.java   # Phasenzuordnung pro Testfall
│   └── MigrationProblem.java          # Problem mit Loesungsoptionen
│
└── service/
    ├── OldStructureAnalyzer.java      # Liest und analysiert OLD-Struktur
    ├── PhaseAssignmentCalculator.java # Berechnet Phasenzuordnungen
    ├── NewStructureBuilder.java       # Erstellt NEW-Verzeichnisstruktur
    ├── FileMigrator.java              # Kopiert Dateien mit Validierung
    ├── MigrationService.java          # Orchestriert die Migration
    └── MigrationValidator.java        # Validiert vor/nach Migration
```

## ItsqEditorView (XML-Editor)

Die ItsqEditorView ist ein spezialisierter Editor fuer XML-Dateien.

### Features

- **Syntax-Highlighting** fuer XML mit RSyntaxTextArea
- **Zeilennummern** und **Code-Folding**
- **Suche**: Strg+F fokussiert Filter, F3 = weiter, Shift+F3 = zurueck
- **Gehe zu Zeile**: Strg+G
- **Speichern**: Strg+S

## Properties-Editoren

Fuer die verschiedenen Properties-Dateitypen gibt es spezialisierte Editoren:

### ItsqRefExportPropertiesEditorView (Relevanz.properties)

Editor fuer REF-EXPORT Testfall-Definitionen.

| Spalte | Beschreibung |
|--------|--------------|
| Testname | Name des Testfalls (p01, n01, x01, ...) |
| Crefonummer | Crefo-Nummer des Testfalls |
| Info | Kommentar/Beschreibung |
| Export | Soll exportiert werden (Boolean) |
| REF-Export-Datei | Zugeordnete XML-Datei |

**Features:**
- **Dateiformat**: `testname=crefonummer # kommentar`
- **Live-Filter**: Filtert nach Testname, Crefonummer oder Info
- **CRUD-Operationen**: Neu, Bearbeiten, Loeschen
- **Datei-Selektion**: FileChooser fuer REF-Export und ARCHIV-BESTAND Dateien
- **Automatische Verzeichnis-Navigation**: ARCHIV-BESTAND FileChooser oeffnet im korrekten Phase-Verzeichnis

### ItsqTestCrefosPropertiesEditorView (TestCrefos.properties)

Editor fuer AB30XML-Properties (Crefo-Stammdaten).

| Spalte | Beschreibung |
|--------|--------------|
| Crefonummer | Crefo-Nummer |
| Kunden | Liste der Kunden die diese Crefo verwenden |
| CLZ | Auftrags-CLZ |
| Btlg-List | Liste der Beteiligungen |
| Bilanz-Typ | BILANZ, HGB, IFRS, etc. |
| Prod-Auft. | EH-Produktauftrag-Typ |
| Statistik | CTA-Statistik aktiv (Boolean) |
| DSGVO-Sperre | DSGVO-Sperre aktiv (Boolean) |

**Features:**
- **Versions-Unterstuetzung**: Automatische Erkennung der Dateiversion
- **Live-Filter**: Filtert nach Crefonummer, Kunde oder CLZ
- **CRUD-Operationen** mit spezialisierten Dialogen (ComboBox fuer Enums)

### ItsqOptionsEditorView (Options.cfg)

Editor fuer allgemeine Konfigurations-Dateien.

| Spalte | Beschreibung |
|--------|--------------|
| Name | Property-Name |
| Wert | Property-Wert |

**Features:**
- **Dateiformat**: `name=wert` oder `name:wert`
- **Live-Filter**: Filtert nach Name oder Wert
- **CRUD-Operationen**: Neu, Bearbeiten, Loeschen
- **Kommentar-Erhaltung**: Zeilen mit `#` oder `!` bleiben beim Speichern erhalten

### Gemeinsame Tastenkuerzel (alle Properties-Editoren)

| Shortcut | Funktion |
|----------|----------|
| Strg+F | Filter fokussieren |
| Strg+S | Speichern |
| Strg+N | Neuer Eintrag |
| Enter | Eintrag bearbeiten |
| Delete | Eintrag loeschen |

## DatabaseView

Die DatabaseView bietet einen vollstaendigen SQL-Client.

### Features

- **Verbindungsverwaltung**: Speichern, Laden, Loeschen von Verbindungen
- **Tabellen-Browser**: Lazy-Loading von Tabellen und Spalten mit Typen
- **SQL-Editor**: Syntax fuer SQL-Abfragen
- **SQL-History**: Automatische Speicherung ausgefuehrter Abfragen
- **Favoriten**: Wichtige Abfragen als Favoriten speichern
- **CSV-Export**: Ergebnisse als CSV exportieren

### Unterstuetzte JDBC-Treiber

| Datenbank | Treiber-Klasse |
|-----------|----------------|
| PostgreSQL | org.postgresql.Driver |
| MySQL | com.mysql.cj.jdbc.Driver |
| Oracle | oracle.jdbc.OracleDriver |
| SQL Server | com.microsoft.sqlserver.jdbc.SQLServerDriver |
| H2 | org.h2.Driver |
| SQLite | org.sqlite.JDBC |

## Konfigurationsverwaltung

### AppConfig

Singleton-Konfigurationsmanager. Verwaltet Properties aus `config.properties`.

```java
AppConfig cfg = AppConfig.getInstance();

// Werte lesen
String value = cfg.getProperty("KEY");
String value = cfg.getProperty("KEY", "default");
String[] array = cfg.getArray("KEY");        // Semikolon-getrennt
boolean flag = cfg.getBool("KEY");
int num = cfg.getInt("KEY", defaultValue);

// Werte setzen und speichern
cfg.setProperty("KEY", "value");
cfg.save();

// Konfiguration aus anderer Datei laden
cfg.loadFrom("path/to/config.properties");
cfg.reload();
```

**Konfigurationsdatei-Prioritaet:**
1. System Property: `-Dconfig.file=path`
2. Environment Variable: `CONFIG_FILE_PATH`
3. Default: `config.properties` (im Arbeitsverzeichnis)

### ConnectionManager

Utility-Klasse fuer die Verwaltung von Datenbankverbindungen.

```java
ConnectionManager.loadConnections();
List<ConnectionInfo> conns = ConnectionManager.getConnections();
String[] names = ConnectionManager.getConnectionNames();
ConnectionInfo conn = ConnectionManager.getConnection("name");

ConnectionManager.saveConnection(conn);
ConnectionManager.deleteConnection("name");

String last = ConnectionManager.getLastConnectionName();
ConnectionManager.setLastConnectionName("name");

ConnectionManager.addListener(listener);
ConnectionManager.removeListener(listener);
```

## ITSQ-Subsystem Architektur

### Model-Klassen (itsq/model/)

```
ItsqItem (Interface)
    │
    ├── ItsqRoot           # Wurzel-Element
    ├── ItsqCustomer       # Kunde (c01, c02, ...)
    ├── ItsqScenario       # Szenario (Relevanz_Positiv, ...)
    ├── ItsqRefExports     # REF-EXPORTS Verzeichnis
    ├── ItsqRefExportsPhase # PHASE-1, PHASE-2
    ├── ItsqArchivBestand  # ARCHIV-BESTAND Verzeichnis
    ├── ItsqArchivBestandPhase # PHASE-1, PHASE-2
    ├── ItsqXmlFile        # XML-Datei
    ├── ItsqPropertiesFile # Properties-Datei
    └── ItsqOptionsFile    # Options.cfg
```

### TreeNode-Klassen (itsq/tree/)

```
ItsqTreeNode (abstrakt)
    │
    ├── ItsqRootTreeNode
    ├── ItsqCustomerTreeNode
    ├── ItsqScenarioTreeNode
    ├── ItsqRefExportsTreeNode
    ├── ItsqRefExportsPhaseTreeNode
    ├── ItsqArchivBestandTreeNode
    ├── ItsqArchivBestandPhaseTreeNode
    ├── ItsqXmlTreeNode
    ├── ItsqPropertiesTreeNode
    └── ItsqOptionsTreeNode
```

### View-Klassen (itsq/view/)

```
ItsqItemSelectable (Interface)
    │
    ├── ItsqMainView       # Haupt-Container
    ├── ItsqTreeView       # Tree-Ansicht
    ├── ItsqViewTabView    # Tab-Container (CardLayout)
    ├── ItsqMigrationView  # Migration OLD -> NEW
    │
    ├── Editor-Views:
    │   ├── ItsqEditorView                    # XML-Editor
    │   ├── ItsqRefExportPropertiesEditorView # Relevanz.properties Editor
    │   ├── ItsqTestCrefosPropertiesEditorView # TestCrefos.properties Editor
    │   └── ItsqOptionsEditorView             # Options.cfg Editor
    │
    ├── Detail-Views:
    │   ├── ItsqRootView       # Root-Details
    │   ├── ItsqCustomerView   # Kunden-Details
    │   ├── ItsqScenarioView   # Szenario-Details
    │   ├── ItsqRefExportsView # REF-EXPORTS-Details
    │   ├── ItsqRefExportsPhaseView
    │   ├── ItsqArchivBestandView
    │   └── ItsqArchibBestandPhaseView
    │
    └── Dialoge (itsq/dialog/):
        └── MigrationProblemDialog  # Interaktiver Dialog bei Migrationsproblemen
```

## Neue View erstellen

### Schritt 1: JFormDesigner-Panel erstellen

Erstellen Sie ein neues Panel in JFormDesigner.

### Schritt 2: Panel-Wrapper erstellen

```java
package de.cavdar.gui.design.myview;

public class MyViewPanel extends BaseViewPanel {
    private InternalFrameMyView myView;

    @Override
    protected void initComponents() {
        myView = new InternalFrameMyView();
        add(myView, BorderLayout.CENTER);
    }

    @Override
    public JToolBar getViewToolbar() {
        return myView.getToolBarMain();
    }

    @Override
    public JProgressBar getProgressBar() {
        return myView.getProgressBar();
    }

    @Override
    public JButton getCancelButton() {
        return myView.getCancelButton();
    }

    public JButton getMyButton() {
        return myView.getMyButton();
    }
}
```

### Schritt 3: View-Klasse erstellen

```java
package de.cavdar.gui.view.myview;

public class MyViewView extends BaseView {
    private MyViewPanel myPanel;

    public MyViewView() {
        super("Meine View");
    }

    @Override
    protected BaseViewPanel createPanel() {
        myPanel = new MyViewPanel();
        return myPanel;
    }

    @Override
    protected void setupToolbarActions() {
        myPanel.getMyButton().addActionListener(e -> doSomething());
    }

    @Override
    public String getMenuLabel() {
        return "Meine View";
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK);
    }

    private void doSomething() {
        executeTask(() -> {
            // Hintergrund-Arbeit
        });
    }
}
```

### Schritt 4: View registrieren

In `Main.java`:

```java
MainFrame frame = new MainFrame();
frame.registerView(ItsqExplorerView::new);
frame.registerView(ItsqMigrationToolView::new);
frame.registerView(MyViewView::new);  // Neue View
```

## Async-Task-Handling

```java
private void processData() {
    executeTask(() -> {
        // Laeuft im Hintergrund-Thread
        for (int i = 0; i < 100; i++) {
            // Arbeit...
            SwingUtilities.invokeLater(() -> updateUI());
        }
    });
}
```

**Features:**
- Automatische Progress-Bar-Anzeige (indeterminate)
- Cancel-Button wird sichtbar
- Bei Abbruch: Meldung "Aktion abgebrochen"

## Abhaengigkeiten

```xml
<dependencies>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.9</version>
    </dependency>
    <dependency>
        <groupId>log4j</groupId>
        <artifactId>log4j</artifactId>
        <version>1.2.12</version>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.7.4</version>
    </dependency>
    <dependency>
        <groupId>com.fifesoft</groupId>
        <artifactId>rsyntaxtextarea</artifactId>
        <version>3.4.0</version>
    </dependency>
</dependencies>
```

## Anwendung starten

### Aus IDE (IntelliJ)

Main-Klasse: `de.cavdar.gui.Main`

### Mit Maven

```bash
mvn exec:java
```

### Mit Debug-Modus

```bash
mvn exec:java -Dexec.args="D"
```
