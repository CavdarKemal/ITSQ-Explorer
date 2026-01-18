package de.cavdar.itsq.migration.service;

import de.cavdar.itsq.migration.model.MigrationConfig;
import de.cavdar.itsq.migration.model.TestCasePhaseAssignment;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Analysiert die OLD ITSQ-Struktur und extrahiert alle relevanten Informationen
 * fuer die Migration zur NEW-Struktur.
 */
public class OldStructureAnalyzer {

    private static final Pattern CRF_XML_PATTERN = Pattern.compile("\\d{10}\\.xml");
    private static final Pattern CUSTOMER_PATTERN = Pattern.compile("c\\d+");

    private final MigrationConfig config;

    // Gesammelte Daten
    private Set<Long> phase1Crefos;
    private Set<Long> phase2Crefos;
    private Map<String, Map<String, List<TestCasePhaseAssignment>>> customerScenarioAssignments;
    private List<String> customerKeys;
    private Map<String, File> optionsCfgFiles;

    public OldStructureAnalyzer(MigrationConfig config) {
        this.config = config;
        this.phase1Crefos = new TreeSet<>();
        this.phase2Crefos = new TreeSet<>();
        this.customerScenarioAssignments = new TreeMap<>();
        this.customerKeys = new ArrayList<>();
        this.optionsCfgFiles = new TreeMap<>();
    }

    /**
     * Analysiert die OLD-Struktur und befuellt alle Sammlungen.
     */
    public void analyze() throws IOException {
        // Schritt 1: Sammle alle Crefos aus ARCHIV-BESTAND Verzeichnissen
        phase1Crefos = scanArchivBestandCrefos(config.getArchivBestandPh1Dir());
        phase2Crefos = scanArchivBestandCrefos(config.getArchivBestandPh2Dir());

        // Schritt 2: Scanne REF-EXPORTS nach Kunden und Szenarien
        scanRefExports();
    }

    /**
     * Scannt ein ARCHIV-BESTAND Verzeichnis und gibt alle gefundenen Crefo-Nummern zurueck.
     */
    private Set<Long> scanArchivBestandCrefos(File archivDir) {
        Set<Long> crefos = new TreeSet<>();
        if (archivDir == null || !archivDir.exists() || !archivDir.isDirectory()) {
            return crefos;
        }

        File[] xmlFiles = archivDir.listFiles((dir, name) -> CRF_XML_PATTERN.matcher(name).matches());
        if (xmlFiles != null) {
            for (File xmlFile : xmlFiles) {
                String name = xmlFile.getName();
                String crefoStr = name.substring(0, name.length() - 4); // remove .xml
                try {
                    crefos.add(Long.parseLong(crefoStr));
                } catch (NumberFormatException e) {
                    // Ungueltige Dateinamen ignorieren
                }
            }
        }
        return crefos;
    }

    /**
     * Scannt das REF-EXPORTS Verzeichnis nach Kunden und deren Szenarien.
     */
    private void scanRefExports() throws IOException {
        File refExportsDir = config.getRefExportsDir();
        if (refExportsDir == null || !refExportsDir.exists() || !refExportsDir.isDirectory()) {
            return;
        }

        File[] customerDirs = refExportsDir.listFiles(file ->
                file.isDirectory() && CUSTOMER_PATTERN.matcher(file.getName()).matches());

        if (customerDirs != null) {
            Arrays.sort(customerDirs, Comparator.comparing(File::getName));
            for (File customerDir : customerDirs) {
                String customerKey = customerDir.getName();
                customerKeys.add(customerKey);
                scanCustomerDir(customerDir, customerKey);
            }
        }
    }

    /**
     * Scannt ein Kundenverzeichnis nach Szenarien und Options.cfg.
     */
    private void scanCustomerDir(File customerDir, String customerKey) throws IOException {
        // Pruefe auf Options.cfg
        File optionsCfg = new File(customerDir, "Options.cfg");
        if (optionsCfg.exists()) {
            optionsCfgFiles.put(customerKey, optionsCfg);
        }

        // Scanne Szenarien (Relevanz_* Verzeichnisse)
        File[] scenarioDirs = customerDir.listFiles(file ->
                file.isDirectory() && file.getName().startsWith("Relevanz_"));

        if (scenarioDirs != null) {
            Map<String, List<TestCasePhaseAssignment>> scenarioAssignments = new TreeMap<>();
            for (File scenarioDir : scenarioDirs) {
                String scenarioName = scenarioDir.getName();
                List<TestCasePhaseAssignment> assignments = parseScenario(customerKey, scenarioDir, scenarioName);
                if (!assignments.isEmpty()) {
                    scenarioAssignments.put(scenarioName, assignments);
                }
            }
            if (!scenarioAssignments.isEmpty()) {
                customerScenarioAssignments.put(customerKey, scenarioAssignments);
            }
        }
    }

    /**
     * Parst ein Szenario-Verzeichnis und erstellt TestCasePhaseAssignment Objekte.
     */
    private List<TestCasePhaseAssignment> parseScenario(String customerKey, File scenarioDir,
                                                         String scenarioName) throws IOException {
        List<TestCasePhaseAssignment> assignments = new ArrayList<>();

        // Finde Relevanz.properties Datei
        File relevanzProps = new File(scenarioDir, "Relevanz.properties");
        if (!relevanzProps.exists()) {
            return assignments;
        }

        // Sammle alle XML-Dateien im Szenario-Verzeichnis
        Map<Long, File> xmlFilesByCrefo = new HashMap<>();
        File[] xmlFiles = scenarioDir.listFiles((dir, name) -> name.endsWith(".xml"));
        if (xmlFiles != null) {
            for (File xmlFile : xmlFiles) {
                Long crefo = extractCrefoFromXmlName(xmlFile.getName());
                if (crefo != null) {
                    xmlFilesByCrefo.put(crefo, xmlFile);
                }
            }
        }

        // Parse Relevanz.properties
        List<String> lines = FileUtils.readLines(relevanzProps, "UTF-8");
        for (String line : lines) {
            if (line.isBlank() || line.trim().startsWith("#")) {
                continue;
            }

            TestCasePhaseAssignment assignment = parseRelevanzLine(customerKey, scenarioName, line, xmlFilesByCrefo);
            if (assignment != null) {
                assignments.add(assignment);
            }
        }

        return assignments;
    }

    /**
     * Parst eine einzelne Zeile aus Relevanz.properties.
     * Format: testFallName=crefoNr # Kommentar
     */
    private TestCasePhaseAssignment parseRelevanzLine(String customerKey, String scenarioName,
                                                       String line, Map<Long, File> xmlFilesByCrefo) {
        String[] splitEqual = line.split("=");
        if (splitEqual.length < 2) {
            return null;
        }

        String testFallName = splitEqual[0].trim();
        String[] splitHash = splitEqual[1].trim().split("#");
        String crefoStr = splitHash[0].trim();
        String testFallInfo = splitHash.length > 1 ? splitHash[1].trim() : "";

        try {
            Long crefoNr = Long.parseLong(crefoStr);
            TestCasePhaseAssignment assignment = new TestCasePhaseAssignment(
                    customerKey, scenarioName, testFallName, crefoNr, testFallInfo);

            // Setze Quell-REF-EXPORT XML falls vorhanden
            File sourceXml = xmlFilesByCrefo.get(crefoNr);
            if (sourceXml == null) {
                // Versuche anhand des testFallName im Dateinamen zu finden
                for (Map.Entry<Long, File> entry : xmlFilesByCrefo.entrySet()) {
                    if (entry.getValue().getName().contains(testFallName)) {
                        sourceXml = entry.getValue();
                        break;
                    }
                }
            }
            assignment.setSourceRefExportXml(sourceXml);

            return assignment;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Extrahiert Crefo-Nummer aus XML-Dateinamen.
     * Z.B. "p01_stammsatz_1234567894.xml" -> 1234567894
     */
    private Long extractCrefoFromXmlName(String filename) {
        // Versuche Pattern: *_1234567890.xml
        int lastUnderscore = filename.lastIndexOf('_');
        if (lastUnderscore >= 0) {
            String suffix = filename.substring(lastUnderscore + 1);
            if (suffix.endsWith(".xml")) {
                String crefoStr = suffix.substring(0, suffix.length() - 4);
                try {
                    return Long.parseLong(crefoStr);
                } catch (NumberFormatException e) {
                    // Ignorieren
                }
            }
        }
        // Versuche Pattern: 1234567890.xml
        if (filename.endsWith(".xml")) {
            String crefoStr = filename.substring(0, filename.length() - 4);
            try {
                return Long.parseLong(crefoStr);
            } catch (NumberFormatException e) {
                // Ignorieren
            }
        }
        return null;
    }

    // Getter-Methoden

    public Set<Long> getPhase1Crefos() {
        return Collections.unmodifiableSet(phase1Crefos);
    }

    public Set<Long> getPhase2Crefos() {
        return Collections.unmodifiableSet(phase2Crefos);
    }

    public boolean hasCrefoInPhase1(Long crefo) {
        return phase1Crefos.contains(crefo);
    }

    public boolean hasCrefoInPhase2(Long crefo) {
        return phase2Crefos.contains(crefo);
    }

    public File getArchivBestandXmlPhase1(Long crefo) {
        File xmlFile = new File(config.getArchivBestandPh1Dir(), crefo + ".xml");
        return xmlFile.exists() ? xmlFile : null;
    }

    public File getArchivBestandXmlPhase2(Long crefo) {
        File xmlFile = new File(config.getArchivBestandPh2Dir(), crefo + ".xml");
        return xmlFile.exists() ? xmlFile : null;
    }

    public List<String> getCustomerKeys() {
        return Collections.unmodifiableList(customerKeys);
    }

    public Map<String, Map<String, List<TestCasePhaseAssignment>>> getCustomerScenarioAssignments() {
        return customerScenarioAssignments;
    }

    public List<TestCasePhaseAssignment> getAssignmentsForCustomer(String customerKey) {
        List<TestCasePhaseAssignment> all = new ArrayList<>();
        Map<String, List<TestCasePhaseAssignment>> scenarios = customerScenarioAssignments.get(customerKey);
        if (scenarios != null) {
            scenarios.values().forEach(all::addAll);
        }
        return all;
    }

    public List<TestCasePhaseAssignment> getAllAssignments() {
        List<TestCasePhaseAssignment> all = new ArrayList<>();
        customerScenarioAssignments.values().forEach(scenarios ->
                scenarios.values().forEach(all::addAll));
        return all;
    }

    public File getOptionsCfgFile(String customerKey) {
        return optionsCfgFiles.get(customerKey);
    }

    public File getTestCrefosPropsPhase1() {
        File propsFile = new File(config.getArchivBestandPh1Dir(), "TestCrefos.properties");
        return propsFile.exists() ? propsFile : null;
    }

    public File getTestCrefosPropsPhase2() {
        File propsFile = new File(config.getArchivBestandPh2Dir(), "TestCrefos.properties");
        return propsFile.exists() ? propsFile : null;
    }

    public int getTotalCustomers() {
        return customerKeys.size();
    }

    public int getTotalScenarios() {
        int count = 0;
        for (Map<String, List<TestCasePhaseAssignment>> scenarios : customerScenarioAssignments.values()) {
            count += scenarios.size();
        }
        return count;
    }

    public int getTotalTestCases() {
        return getAllAssignments().size();
    }
}
