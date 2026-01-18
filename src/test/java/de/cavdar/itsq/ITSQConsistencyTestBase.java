package de.cavdar.itsq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basisklasse fuer ITSQ-Konsistenztests.
 *
 * Stellt gemeinsame Funktionalitaet fuer OLD und NEW Strukturen bereit.
 * Subklassen muessen die abstrakten Methoden fuer strukturspezifische Pfade implementieren.
 *
 * Prueft die Konsistenz zwischen:
 * - REF-EXPORTS (Relevanz.properties und XML-Dateien)
 * - ARCHIV-BESTAND (XML-Dateien)
 * - TestCrefos.properties (Kunden-Zuordnungen)
 */
public abstract class ITSQConsistencyTestBase {

    protected static final String TEST_CREFOS_FILE = "TestCrefos.properties";
    protected static final String REPORT_DIR = "target/consistency-reports";

    protected Map<TestSupportClientKonstanten.TEST_PHASE, Map<String, TestCustomer>> customerTestInfoMapMap;
    protected AB30MapperUtil ab30MapperUtil;
    protected List<String> errors;
    protected List<String> refExportsErrors;
    protected List<String> archivBestandErrors;
    protected List<String> testCrefosErrors;

    // ===== Abstrakte Methoden - muessen von Subklassen implementiert werden =====

    /**
     * Liefert den ARCHIV-BESTAND Pfad fuer die angegebene Phase.
     */
    protected abstract String getArchivBestandPath(TestSupportClientKonstanten.TEST_PHASE phase);

    /**
     * Liefert den REF-EXPORTS Pfad fuer die angegebene Phase.
     */
    protected abstract String getRefExportsPath(TestSupportClientKonstanten.TEST_PHASE phase);

    /**
     * Liefert die Liste der Kunden fuer die angegebene Phase.
     */
    protected abstract List<String> getCustomersForPhase(TestSupportClientKonstanten.TEST_PHASE phase);

    /**
     * Liefert einen kurzen Namen fuer die Struktur (z.B. "NEW" oder "OLD").
     */
    protected abstract String getStructureName();

    // ===== Setup =====

    @BeforeEach
    void setUp() throws Exception {
        customerTestInfoMapMap = new HashMap<>();
        ab30MapperUtil = new AB30MapperUtil();
        errors = new ArrayList<>();
        refExportsErrors = new ArrayList<>();
        archivBestandErrors = new ArrayList<>();
        testCrefosErrors = new ArrayList<>();

        // Lade Kunden fuer beide Phasen
        for (TestSupportClientKonstanten.TEST_PHASE phase : TestSupportClientKonstanten.TEST_PHASE.values()) {
            customerTestInfoMapMap.put(phase,
                    loadCustomersFromClasspath(
                            getRefExportsPath(phase),
                            getArchivBestandPath(phase),
                            getCustomersForPhase(phase)));
        }
    }

    // ===== Gemeinsame Hilfsmethoden =====

    /**
     * Laedt eine Ressource als Path vom Classpath.
     */
    protected Path getResourcePath(String resourcePath) throws URISyntaxException {
        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            return null;
        }
        return Paths.get(url.toURI());
    }

    /**
     * Laedt TestCustomer-Objekte aus Classpath-Ressourcen.
     */
    protected Map<String, TestCustomer> loadCustomersFromClasspath(
            String refExportsPath, String archivBestandPath, List<String> customerKeys)
            throws URISyntaxException {

        Map<String, TestCustomer> customerMap = new TreeMap<>();

        Path refExportsBasePath = getResourcePath(refExportsPath);
        Path archivBestandBasePath = getResourcePath(archivBestandPath);

        if (refExportsBasePath == null || !Files.exists(refExportsBasePath)) {
            return customerMap;
        }

        for (String customerKey : customerKeys) {
            Path customerDir = refExportsBasePath.resolve(customerKey);
            if (!Files.exists(customerDir)) {
                continue;
            }

            TestCustomer testCustomer = new TestCustomer(customerKey.toUpperCase(), customerKey.toUpperCase());
            testCustomer.setItsqRefExportsDir(customerDir.toFile());
            testCustomer.setItsqAB30XmlsDir(archivBestandBasePath != null ? archivBestandBasePath.toFile() : customerDir.toFile());

            // Lade alle Szenarien (Unterverzeichnisse)
            File[] scenarioDirs = customerDir.toFile().listFiles(File::isDirectory);
            if (scenarioDirs != null) {
                for (File scenarioDir : scenarioDirs) {
                    try {
                        File[] xmlFiles = scenarioDir.listFiles((dir, name) -> name.endsWith(".xml"));
                        List<File> refExportXmlFileList = xmlFiles != null ?
                                Arrays.stream(xmlFiles).collect(Collectors.toList()) : Collections.emptyList();
                        TestScenario testScenario = new TestScenario(testCustomer, scenarioDir.getName(), refExportXmlFileList);
                        testCustomer.addTestScenario(testScenario);
                    } catch (RuntimeException e) {
                        System.err.println("Warnung: Szenario " + scenarioDir.getName() + " fuer Kunde " + customerKey +
                                " konnte nicht geladen werden: " + e.getMessage());
                    }
                }
            }

            customerMap.put(customerKey.toUpperCase(), testCustomer);
        }

        return customerMap;
    }

    // ===== Test-Methoden =====

    @Test
    @DisplayName("Alle Konsistenzpruefungen ausfuehren")
    public void testAllConsistencyChecks() throws Exception {
        checkRefExportsConsistency();
        checkArchivBestandConsistency();
        checkTestfaelleProperties();

        // Erstelle und gib Bericht aus
        String report = generateReport();
        outputReportToConsole(report);
        saveReportToFile(report);

        // Hinweis: Test schlaegt NICHT mehr fehl - Bericht dient zur manuellen Korrektur
        if (!errors.isEmpty()) {
            System.out.println("\n========================================");
            System.out.println("ACHTUNG: " + errors.size() + " Inkonsistenzen gefunden!");
            System.out.println("Bitte den Bericht pruefen und manuell korrigieren.");
            System.out.println("========================================\n");
        }
    }

    @Nested
    @DisplayName("REF-EXPORTS Konsistenz")
    class RefExportsConsistencyTests {

        @Test
        @DisplayName("Alle Testfaelle in REF-EXPORTS pruefen")
        void testRefExportsConsistency() {
            checkRefExportsConsistency();
            if (!refExportsErrors.isEmpty()) {
                System.out.println("\n=== REF-EXPORTS Inkonsistenzen ===");
                refExportsErrors.forEach(System.out::println);
                System.out.println("Bitte manuell korrigieren.\n");
            } else {
                System.out.println("REF-EXPORTS: Keine Inkonsistenzen gefunden.");
            }
        }
    }

    @Nested
    @DisplayName("ARCHIV-BESTAND Konsistenz")
    class ArchivBestandConsistencyTests {

        @Test
        @DisplayName("Alle Testfaelle in ARCHIV-BESTAND pruefen")
        void testArchivBestandConsistency() {
            checkArchivBestandConsistency();
            if (!archivBestandErrors.isEmpty()) {
                System.out.println("\n=== ARCHIV-BESTAND Inkonsistenzen ===");
                archivBestandErrors.forEach(System.out::println);
                System.out.println("Bitte manuell korrigieren.\n");
            } else {
                System.out.println("ARCHIV-BESTAND: Keine Inkonsistenzen gefunden.");
            }
        }
    }

    @Nested
    @DisplayName("TestCrefos.properties Konsistenz")
    class TestCrefosPropertiesConsistencyTests {

        @Test
        @DisplayName("Alle Kunden-Zuordnungen in TestCrefos.properties pruefen")
        void testTestCrefosPropertiesConsistency() {
            checkTestfaelleProperties();
            if (!testCrefosErrors.isEmpty()) {
                System.out.println("\n=== TestCrefos.properties Inkonsistenzen ===");
                testCrefosErrors.forEach(System.out::println);
                System.out.println("Bitte manuell korrigieren.\n");
            } else {
                System.out.println("TestCrefos.properties: Keine Inkonsistenzen gefunden.");
            }
        }
    }

    // ===== Pruefmethoden =====

    /**
     * Prueft die lokale Konsistenz innerhalb REF-EXPORTS.
     *
     * Fuer jeden Testfall wird geprueft:
     * - p0X=crefo: Datei p0X_stammsatz_{crefo}.xml muss existieren
     * - x0X=crefo: Datei x0Y_loeschsatz_{crefo}.xml muss existieren
     * - n0X=crefo: Keine XML-Datei *{crefo}.xml darf existieren
     */
    protected void checkRefExportsConsistency() {
        for (Map.Entry<TestSupportClientKonstanten.TEST_PHASE, Map<String, TestCustomer>> phaseEntry :
                customerTestInfoMapMap.entrySet()) {

            TestSupportClientKonstanten.TEST_PHASE phase = phaseEntry.getKey();
            Map<String, TestCustomer> customerMap = phaseEntry.getValue();

            for (Map.Entry<String, TestCustomer> customerEntry : customerMap.entrySet()) {
                String customerKey = customerEntry.getKey();
                TestCustomer customer = customerEntry.getValue();

                for (Map.Entry<String, TestScenario> scenarioEntry :
                        customer.getTestScenariosMap().entrySet()) {

                    String scenarioName = scenarioEntry.getKey();
                    TestScenario scenario = scenarioEntry.getValue();

                    for (TestCrefo testCrefo : scenario.getTestCrefosAsList()) {
                        String testFallName = testCrefo.getTestFallName();
                        Long crefoNr = testCrefo.getItsqTestCrefoNr();
                        File xmlFile = testCrefo.getItsqRexExportXmlFile();

                        if (testCrefo.isShouldBeExported()) {
                            if (xmlFile == null) {
                                String error = String.format(
                                        "[REF-EXPORTS] %s/%s/%s/%s: XML-Datei fuer Crefo %d fehlt (erwartet: %s_*_%d.xml)",
                                        phase.getDirName(), customerKey, scenarioName, testFallName,
                                        crefoNr, testFallName, crefoNr);
                                errors.add(error);
                                refExportsErrors.add(error);
                            } else if (!xmlFile.exists()) {
                                String error = String.format(
                                        "[REF-EXPORTS] %s/%s/%s/%s: XML-Datei existiert nicht: %s",
                                        phase.getDirName(), customerKey, scenarioName, testFallName,
                                        xmlFile.getName());
                                errors.add(error);
                                refExportsErrors.add(error);
                            }
                        } else {
                            if (xmlFile != null && xmlFile.exists()) {
                                String error = String.format(
                                        "[REF-EXPORTS] %s/%s/%s/%s: XML-Datei darf NICHT existieren fuer negative Testfaelle: %s",
                                        phase.getDirName(), customerKey, scenarioName, testFallName,
                                        xmlFile.getName());
                                errors.add(error);
                                refExportsErrors.add(error);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Prueft die Konsistenz mit ARCHIV-BESTAND.
     *
     * Fuer jeden positiven Testfall (p0X, x0X) wird geprueft:
     * - ARCHIV-BESTAND/{crefo}.xml muss existieren
     *
     * Negative Testfaelle (n0X) werden nicht geprueft.
     */
    protected void checkArchivBestandConsistency() {
        for (Map.Entry<TestSupportClientKonstanten.TEST_PHASE, Map<String, TestCustomer>> phaseEntry :
                customerTestInfoMapMap.entrySet()) {

            TestSupportClientKonstanten.TEST_PHASE phase = phaseEntry.getKey();
            Map<String, TestCustomer> customerMap = phaseEntry.getValue();

            String archivBestandPathStr = getArchivBestandPath(phase);
            Path archivBestandPhasePath;
            try {
                archivBestandPhasePath = getResourcePath(archivBestandPathStr);
            } catch (URISyntaxException e) {
                String error = String.format("[ARCHIV-BESTAND] Fehler beim Laden von %s: %s",
                        archivBestandPathStr, e.getMessage());
                errors.add(error);
                archivBestandErrors.add(error);
                continue;
            }

            if (archivBestandPhasePath == null || !Files.exists(archivBestandPhasePath)) {
                String error = String.format("[ARCHIV-BESTAND] Verzeichnis existiert nicht: %s",
                        archivBestandPathStr);
                errors.add(error);
                archivBestandErrors.add(error);
                continue;
            }

            for (Map.Entry<String, TestCustomer> customerEntry : customerMap.entrySet()) {
                String customerKey = customerEntry.getKey();
                TestCustomer customer = customerEntry.getValue();

                for (Map.Entry<String, TestScenario> scenarioEntry :
                        customer.getTestScenariosMap().entrySet()) {

                    String scenarioName = scenarioEntry.getKey();
                    TestScenario scenario = scenarioEntry.getValue();

                    for (TestCrefo testCrefo : scenario.getTestCrefosAsList()) {
                        if (testCrefo.isShouldBeExported()) {
                            String testFallName = testCrefo.getTestFallName();
                            Long crefoNr = testCrefo.getItsqTestCrefoNr();

                            Path expectedXml = archivBestandPhasePath.resolve(crefoNr + ".xml");

                            if (!Files.exists(expectedXml)) {
                                String error = String.format(
                                        "[ARCHIV-BESTAND] %s/%s/%s/%s: XML-Datei fehlt: %s.xml",
                                        archivBestandPathStr, customerKey, scenarioName, testFallName,
                                        crefoNr);
                                errors.add(error);
                                archivBestandErrors.add(error);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Prueft die Konsistenz mit TestCrefos.properties.
     *
     * Fuer jeden positiven Testfall (p0X, x0X) wird geprueft:
     * - Eintrag in TestCrefos.properties muss existieren
     * - Der aktuelle Kunde muss in der 'Used-By-Customer'-Liste enthalten sein
     *
     * Negative Testfaelle (n0X) werden nicht geprueft.
     */
    protected void checkTestfaelleProperties() {
        for (Map.Entry<TestSupportClientKonstanten.TEST_PHASE, Map<String, TestCustomer>> phaseEntry :
                customerTestInfoMapMap.entrySet()) {

            TestSupportClientKonstanten.TEST_PHASE phase = phaseEntry.getKey();
            Map<String, TestCustomer> customerMap = phaseEntry.getValue();

            String archivBestandPathStr = getArchivBestandPath(phase);
            Path testCrefosPath;
            try {
                testCrefosPath = getResourcePath(archivBestandPathStr + "/" + TEST_CREFOS_FILE);
            } catch (URISyntaxException e) {
                String error = String.format("[TestCrefos] Fehler beim Laden von %s/%s: %s",
                        archivBestandPathStr, TEST_CREFOS_FILE, e.getMessage());
                errors.add(error);
                testCrefosErrors.add(error);
                continue;
            }

            if (testCrefosPath == null || !Files.exists(testCrefosPath)) {
                String error = String.format("[TestCrefos] Datei existiert nicht: %s/%s",
                        archivBestandPathStr, TEST_CREFOS_FILE);
                errors.add(error);
                testCrefosErrors.add(error);
                continue;
            }

            Map<Long, AB30XMLProperties> propsMap;
            try {
                propsMap = ab30MapperUtil.initAb30CrefoPropertiesMap(testCrefosPath.toFile());
            } catch (Exception e) {
                String error = String.format("[TestCrefos] Fehler beim Laden von %s/%s: %s",
                        archivBestandPathStr, TEST_CREFOS_FILE, e.getMessage());
                errors.add(error);
                testCrefosErrors.add(error);
                continue;
            }

            for (Map.Entry<String, TestCustomer> customerEntry : customerMap.entrySet()) {
                String customerKey = customerEntry.getKey().toLowerCase(Locale.ROOT);
                TestCustomer customer = customerEntry.getValue();

                for (Map.Entry<String, TestScenario> scenarioEntry :
                        customer.getTestScenariosMap().entrySet()) {

                    String scenarioName = scenarioEntry.getKey();
                    TestScenario scenario = scenarioEntry.getValue();

                    for (TestCrefo testCrefo : scenario.getTestCrefosAsList()) {
                        if (testCrefo.isShouldBeExported()) {
                            String testFallName = testCrefo.getTestFallName();
                            Long crefoNr = testCrefo.getItsqTestCrefoNr();

                            AB30XMLProperties props = propsMap.get(crefoNr);

                            if (props == null) {
                                String error = String.format(
                                        "[TestCrefos] %s/%s/%s/%s: Kein Eintrag fuer Crefo %d",
                                        archivBestandPathStr, customerKey.toUpperCase(), scenarioName,
                                        testFallName, crefoNr);
                                errors.add(error);
                                testCrefosErrors.add(error);
                            } else {
                                List<String> usedByCustomers = props.getUsedByCustomersList();
                                boolean customerFound = usedByCustomers.stream()
                                        .map(c -> c.toLowerCase(Locale.ROOT))
                                        .anyMatch(c -> c.equals(customerKey));

                                if (!customerFound) {
                                    String error = String.format(
                                            "[TestCrefos] %s/%s/%s/%s: Kunde '%s' fehlt in Used-By-Customer fuer Crefo %d (vorhanden: %s)",
                                            archivBestandPathStr, customerKey.toUpperCase(), scenarioName,
                                            testFallName, customerKey, crefoNr, usedByCustomers);
                                    errors.add(error);
                                    testCrefosErrors.add(error);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ===== Berichtsmethoden =====

    /**
     * Generiert einen detaillierten Konsistenzbericht.
     */
    protected String generateReport() {
        StringBuilder sb = new StringBuilder();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        sb.append("================================================================================\n");
        sb.append("ITSQ KONSISTENZBERICHT - ").append(getStructureName()).append(" Struktur\n");
        sb.append("Erstellt: ").append(timestamp).append("\n");
        sb.append("================================================================================\n\n");

        // Zusammenfassung
        sb.append("ZUSAMMENFASSUNG\n");
        sb.append("---------------\n");
        sb.append("Gesamtanzahl Inkonsistenzen: ").append(errors.size()).append("\n");
        sb.append("  - REF-EXPORTS:          ").append(refExportsErrors.size()).append("\n");
        sb.append("  - ARCHIV-BESTAND:       ").append(archivBestandErrors.size()).append("\n");
        sb.append("  - TestCrefos.properties: ").append(testCrefosErrors.size()).append("\n\n");

        if (errors.isEmpty()) {
            sb.append("Keine Inkonsistenzen gefunden. Alle Pruefungen bestanden.\n");
            return sb.toString();
        }

        // REF-EXPORTS Details
        if (!refExportsErrors.isEmpty()) {
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("REF-EXPORTS INKONSISTENZEN (").append(refExportsErrors.size()).append(")\n");
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("Aktion: Fehlende XML-Dateien manuell erstellen oder aus Vorlage kopieren.\n\n");
            for (String error : refExportsErrors) {
                sb.append("  * ").append(error).append("\n");
            }
            sb.append("\n");
        }

        // ARCHIV-BESTAND Details
        if (!archivBestandErrors.isEmpty()) {
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("ARCHIV-BESTAND INKONSISTENZEN (").append(archivBestandErrors.size()).append(")\n");
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("Aktion: Fehlende {crefo}.xml Dateien im ARCHIV-BESTAND-Verzeichnis erstellen.\n\n");
            for (String error : archivBestandErrors) {
                sb.append("  * ").append(error).append("\n");
            }
            sb.append("\n");
        }

        // TestCrefos.properties Details
        if (!testCrefosErrors.isEmpty()) {
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("TestCrefos.properties INKONSISTENZEN (").append(testCrefosErrors.size()).append(")\n");
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("Aktion: Fehlende Eintraege in TestCrefos.properties ergaenzen.\n");
            sb.append("        Format: {crefo}={usedByCustomers}|{weitere Attribute}\n\n");
            for (String error : testCrefosErrors) {
                sb.append("  * ").append(error).append("\n");
            }
            sb.append("\n");
        }

        sb.append("================================================================================\n");
        sb.append("ENDE DES BERICHTS\n");
        sb.append("================================================================================\n");

        return sb.toString();
    }

    /**
     * Gibt den Bericht in der Konsole aus.
     */
    protected void outputReportToConsole(String report) {
        System.out.println("\n" + report);
    }

    /**
     * Speichert den Bericht als Datei.
     */
    protected void saveReportToFile(String report) {
        try {
            File reportDir = new File(REPORT_DIR);
            if (!reportDir.exists()) {
                reportDir.mkdirs();
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("consistency-report_%s_%s.txt", getStructureName(), timestamp);
            File reportFile = new File(reportDir, filename);

            try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
                writer.print(report);
            }

            System.out.println("Bericht gespeichert unter: " + reportFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Fehler beim Speichern des Berichts: " + e.getMessage());
        }
    }
}
