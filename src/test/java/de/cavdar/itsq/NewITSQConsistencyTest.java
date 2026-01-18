package de.cavdar.itsq;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Konsistenztest fuer ITSQ-Testdaten (NEW Struktur).
 *
 * Prueft die Konsistenz zwischen:
 * - REF-EXPORTS (Relevanz.properties und XML-Dateien)
 * - ARCHIV-BESTAND (XML-Dateien)
 * - TestCrefos.properties (Kunden-Zuordnungen)
 *
 * Testdaten liegen unter: src/test/resources/ITSQ/NEW/
 */
@DisplayName("ITSQ Konsistenz Tests (NEW)")
public class NewITSQConsistencyTest {

    private static final String BASE_PATH = "/ITSQ/NEW";

    private static final String ARCHIV_BESTAND = "ARCHIV-BESTAND";
    private static final String REF_EXPORTS = "REF-EXPORTS";
    private static final String PHASE_1 = "PHASE-1";
    private static final String PHASE_2 = "PHASE-2";

    private static final String ARCHIV_BESTAND_ROOT = BASE_PATH + "/" + ARCHIV_BESTAND;
    private static final String ARCHIV_BESTAND_PHASE_1_ROOT = ARCHIV_BESTAND_ROOT + "/" + PHASE_1;
    private static final String ARCHIV_BESTAND_PHASE_2_ROOT = ARCHIV_BESTAND_ROOT + "/" + PHASE_2;

    private static final String REF_EXPORTS_ROOT = BASE_PATH + "/" + REF_EXPORTS;
    private static final String REF_EXPORTS_PHASE_1_ROOT = REF_EXPORTS_ROOT + "/" + PHASE_1;
    private static final String REF_EXPORTS_PHASE_2_ROOT = REF_EXPORTS_ROOT + "/" + PHASE_2;

    private static final String TEST_CREFOS_FILE = "TestCrefos.properties";

    // PHASE-1 hat nur c01, c02
    private static final List<String> CUSTOMERS_PHASE1 = List.of("c01", "c02");
    // PHASE-2 hat c01-c05
    private static final List<String> CUSTOMERS_PHASE2 = List.of("c01", "c02", "c03", "c04", "c05");

    private Map<TestSupportClientKonstanten.TEST_PHASE, Map<String, TestCustomer>> customerTestInfoMapMap;
    private AB30MapperUtil ab30MapperUtil;
    private List<String> errors;

    @BeforeEach
    void setUp() throws Exception {
        customerTestInfoMapMap = new HashMap<>();
        ab30MapperUtil = new AB30MapperUtil();
        errors = new ArrayList<>();

        // Lade Kunden fuer beide Phasen
        customerTestInfoMapMap.put(TestSupportClientKonstanten.TEST_PHASE.PHASE_1,
                loadCustomersFromClasspath(REF_EXPORTS_PHASE_1_ROOT, ARCHIV_BESTAND_PHASE_1_ROOT, CUSTOMERS_PHASE1));
        customerTestInfoMapMap.put(TestSupportClientKonstanten.TEST_PHASE.PHASE_2,
                loadCustomersFromClasspath(REF_EXPORTS_PHASE_2_ROOT, ARCHIV_BESTAND_PHASE_2_ROOT, CUSTOMERS_PHASE2));
    }

    /**
     * Laedt TestCustomer-Objekte aus Classpath-Ressourcen.
     */
    private Map<String, TestCustomer> loadCustomersFromClasspath(
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
                        // Szenario konnte nicht geladen werden
                        System.err.println("Warnung: Szenario " + scenarioDir.getName() + " fuer Kunde " + customerKey +
                                " konnte nicht geladen werden: " + e.getMessage());
                    }
                }
            }

            customerMap.put(customerKey.toUpperCase(), testCustomer);
        }

        return customerMap;
    }

    /**
     * Laedt eine Ressource als Path vom Classpath.
     */
    private Path getResourcePath(String resourcePath) throws URISyntaxException {
        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            return null;
        }
        return Paths.get(url.toURI());
    }

    @Test
    @DisplayName("Alle Konsistenzpruefungen ausfuehren")
    public void testAllConsistencyChecks() throws Exception {
        // Pruefe lokale Konsistenz in REF-EXPORTS
        checkRefExportsConsistency(customerTestInfoMapMap);

        // Pruefe Konsistenz mit ARCHIV-BESTAND
        checkArchivBestandConsistency(customerTestInfoMapMap);

        // Pruefe Konsistenz mit TestCrefos.properties
        checkTestfaelleProperties(customerTestInfoMapMap);

        // Wenn Fehler gesammelt wurden, Test fehlschlagen lassen
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
            checkRefExportsConsistency(customerTestInfoMapMap);
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
            checkArchivBestandConsistency(customerTestInfoMapMap);
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
            checkTestfaelleProperties(customerTestInfoMapMap);
            if (!errors.isEmpty()) {
                fail("TestCrefos.properties Konsistenzfehler:\n" + String.join("\n", errors));
            }
        }
    }

    /**
     * Prueft die lokale Konsistenz innerhalb REF-EXPORTS.
     *
     * Fuer jeden Testfall wird geprueft:
     * - p0X=crefo: Datei p0X_stammsatz_{crefo}.xml muss existieren
     * - x0X=crefo: Datei x0Y_loeschsatz_{crefo}.xml muss existieren
     * - n0X=crefo: Keine XML-Datei *{crefo}.xml darf existieren
     */
    private void checkRefExportsConsistency(
            Map<TestSupportClientKonstanten.TEST_PHASE, Map<String, TestCustomer>> customerTestInfoMapMap) {

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
                            // p0X oder x0X: XML-Datei muss existieren
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
                            // n0X: Keine XML-Datei darf existieren
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
     * - ARCHIV-BESTAND/PHASE-x/{crefo}.xml muss existieren
     *
     * Negative Testfaelle (n0X) werden nicht geprueft.
     */
    private void checkArchivBestandConsistency(
            Map<TestSupportClientKonstanten.TEST_PHASE, Map<String, TestCustomer>> customerTestInfoMapMap) {

        for (Map.Entry<TestSupportClientKonstanten.TEST_PHASE, Map<String, TestCustomer>> phaseEntry :
                customerTestInfoMapMap.entrySet()) {

            TestSupportClientKonstanten.TEST_PHASE phase = phaseEntry.getKey();
            Map<String, TestCustomer> customerMap = phaseEntry.getValue();

            // Hole ARCHIV-BESTAND Pfad fuer diese Phase
            Path archivBestandPhasePath;
            try {
                archivBestandPhasePath = getResourcePath(ARCHIV_BESTAND_ROOT + "/" + phase.getDirName());
            } catch (URISyntaxException e) {
                errors.add(String.format("[ARCHIV-BESTAND] Fehler beim Laden von %s: %s",
                        phase.getDirName(), e.getMessage()));
                continue;
            }

            if (archivBestandPhasePath == null || !Files.exists(archivBestandPhasePath)) {
                errors.add(String.format("[ARCHIV-BESTAND] Verzeichnis existiert nicht: %s/%s",
                        ARCHIV_BESTAND, phase.getDirName()));
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
                        // Nur positive Testfaelle pruefen
                        if (testCrefo.isShouldBeExported()) {
                            String testFallName = testCrefo.getTestFallName();
                            Long crefoNr = testCrefo.getItsqTestCrefoNr();

                            Path expectedXml = archivBestandPhasePath.resolve(crefoNr + ".xml");

                            if (!Files.exists(expectedXml)) {
                                errors.add(String.format(
                                        "[ARCHIV-BESTAND] %s/%s/%s/%s: XML-Datei fehlt: %s.xml",
                                        phase.getDirName(), customerKey, scenarioName, testFallName,
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
    private void checkTestfaelleProperties(
            Map<TestSupportClientKonstanten.TEST_PHASE, Map<String, TestCustomer>> customerTestInfoMapMap) {

        for (Map.Entry<TestSupportClientKonstanten.TEST_PHASE, Map<String, TestCustomer>> phaseEntry :
                customerTestInfoMapMap.entrySet()) {

            TestSupportClientKonstanten.TEST_PHASE phase = phaseEntry.getKey();
            Map<String, TestCustomer> customerMap = phaseEntry.getValue();

            // Hole TestCrefos.properties Pfad fuer diese Phase
            Path testCrefosPath;
            try {
                testCrefosPath = getResourcePath(ARCHIV_BESTAND_ROOT + "/" + phase.getDirName() + "/" + TEST_CREFOS_FILE);
            } catch (URISyntaxException e) {
                errors.add(String.format("[TestCrefos] Fehler beim Laden von %s/%s: %s",
                        phase.getDirName(), TEST_CREFOS_FILE, e.getMessage()));
                continue;
            }

            if (testCrefosPath == null || !Files.exists(testCrefosPath)) {
                errors.add(String.format("[TestCrefos] Datei existiert nicht: %s/%s/%s",
                        ARCHIV_BESTAND, phase.getDirName(), TEST_CREFOS_FILE));
                continue;
            }

            // Lade TestCrefos.properties
            Map<Long, AB30XMLProperties> propsMap;
            try {
                propsMap = ab30MapperUtil.initAb30CrefoPropertiesMap(testCrefosPath.toFile());
            } catch (Exception e) {
                errors.add(String.format("[TestCrefos] Fehler beim Laden von %s/%s: %s",
                        phase.getDirName(), TEST_CREFOS_FILE, e.getMessage()));
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
                        // Nur positive Testfaelle pruefen
                        if (testCrefo.isShouldBeExported()) {
                            String testFallName = testCrefo.getTestFallName();
                            Long crefoNr = testCrefo.getItsqTestCrefoNr();

                            AB30XMLProperties props = propsMap.get(crefoNr);

                            if (props == null) {
                                errors.add(String.format(
                                        "[TestCrefos] %s/%s/%s/%s: Kein Eintrag fuer Crefo %d",
                                        phase.getDirName(), customerKey.toUpperCase(), scenarioName,
                                        testFallName, crefoNr));
                            } else {
                                List<String> usedByCustomers = props.getUsedByCustomersList();
                                boolean customerFound = usedByCustomers.stream()
                                        .map(c -> c.toLowerCase(Locale.ROOT))
                                        .anyMatch(c -> c.equals(customerKey));

                                if (!customerFound) {
                                    errors.add(String.format(
                                            "[TestCrefos] %s/%s/%s/%s: Kunde '%s' fehlt in Used-By-Customer fuer Crefo %d (vorhanden: %s)",
                                            phase.getDirName(), customerKey.toUpperCase(), scenarioName,
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
