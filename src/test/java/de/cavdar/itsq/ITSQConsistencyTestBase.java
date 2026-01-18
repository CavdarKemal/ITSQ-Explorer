package de.cavdar.itsq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    protected Map<TestSupportClientKonstanten.TEST_PHASE, Map<String, TestCustomer>> customerTestInfoMapMap;
    protected AB30MapperUtil ab30MapperUtil;
    protected List<String> errors;

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

        if (!errors.isEmpty()) {
            fail("Konsistenzfehler gefunden:\n" + String.join("\n", errors));
        }
    }

    @Nested
    @DisplayName("REF-EXPORTS Konsistenz")
    class RefExportsConsistencyTests {

        @Test
        @DisplayName("Alle Testfaelle in REF-EXPORTS pruefen")
        void testRefExportsConsistency() {
            checkRefExportsConsistency();
            if (!errors.isEmpty()) {
                fail("REF-EXPORTS Konsistenzfehler:\n" + String.join("\n", errors));
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
            if (!errors.isEmpty()) {
                fail("ARCHIV-BESTAND Konsistenzfehler:\n" + String.join("\n", errors));
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
            if (!errors.isEmpty()) {
                fail("TestCrefos.properties Konsistenzfehler:\n" + String.join("\n", errors));
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
                                errors.add(String.format(
                                        "[REF-EXPORTS] %s/%s/%s/%s: XML-Datei fuer Crefo %d fehlt (erwartet: %s_*_%d.xml)",
                                        phase.getDirName(), customerKey, scenarioName, testFallName,
                                        crefoNr, testFallName, crefoNr));
                            } else if (!xmlFile.exists()) {
                                errors.add(String.format(
                                        "[REF-EXPORTS] %s/%s/%s/%s: XML-Datei existiert nicht: %s",
                                        phase.getDirName(), customerKey, scenarioName, testFallName,
                                        xmlFile.getName()));
                            }
                        } else {
                            if (xmlFile != null && xmlFile.exists()) {
                                errors.add(String.format(
                                        "[REF-EXPORTS] %s/%s/%s/%s: XML-Datei darf NICHT existieren fuer negative Testfaelle: %s",
                                        phase.getDirName(), customerKey, scenarioName, testFallName,
                                        xmlFile.getName()));
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
                errors.add(String.format("[ARCHIV-BESTAND] Fehler beim Laden von %s: %s",
                        archivBestandPathStr, e.getMessage()));
                continue;
            }

            if (archivBestandPhasePath == null || !Files.exists(archivBestandPhasePath)) {
                errors.add(String.format("[ARCHIV-BESTAND] Verzeichnis existiert nicht: %s",
                        archivBestandPathStr));
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
                                errors.add(String.format(
                                        "[ARCHIV-BESTAND] %s/%s/%s/%s: XML-Datei fehlt: %s.xml",
                                        archivBestandPathStr, customerKey, scenarioName, testFallName,
                                        crefoNr));
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
                errors.add(String.format("[TestCrefos] Fehler beim Laden von %s/%s: %s",
                        archivBestandPathStr, TEST_CREFOS_FILE, e.getMessage()));
                continue;
            }

            if (testCrefosPath == null || !Files.exists(testCrefosPath)) {
                errors.add(String.format("[TestCrefos] Datei existiert nicht: %s/%s",
                        archivBestandPathStr, TEST_CREFOS_FILE));
                continue;
            }

            Map<Long, AB30XMLProperties> propsMap;
            try {
                propsMap = ab30MapperUtil.initAb30CrefoPropertiesMap(testCrefosPath.toFile());
            } catch (Exception e) {
                errors.add(String.format("[TestCrefos] Fehler beim Laden von %s/%s: %s",
                        archivBestandPathStr, TEST_CREFOS_FILE, e.getMessage()));
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
                                errors.add(String.format(
                                        "[TestCrefos] %s/%s/%s/%s: Kein Eintrag fuer Crefo %d",
                                        archivBestandPathStr, customerKey.toUpperCase(), scenarioName,
                                        testFallName, crefoNr));
                            } else {
                                List<String> usedByCustomers = props.getUsedByCustomersList();
                                boolean customerFound = usedByCustomers.stream()
                                        .map(c -> c.toLowerCase(Locale.ROOT))
                                        .anyMatch(c -> c.equals(customerKey));

                                if (!customerFound) {
                                    errors.add(String.format(
                                            "[TestCrefos] %s/%s/%s/%s: Kunde '%s' fehlt in Used-By-Customer fuer Crefo %d (vorhanden: %s)",
                                            archivBestandPathStr, customerKey.toUpperCase(), scenarioName,
                                            testFallName, customerKey, crefoNr, usedByCustomers));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
