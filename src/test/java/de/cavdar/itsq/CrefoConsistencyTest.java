package de.cavdar.itsq;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Konsistenztest fuer die Zuordnung von Crefo-Nummern zu Kunden (OLD Struktur).
 *
 * Prueft, ob die Crefos in den Relevanz_Positiv/Relevanz.properties Dateien
 * mit den Definitionen in TestCrefos.properties uebereinstimmen.
 *
 * Verwendet die Model-Klassen TestCustomer, TestScenario und TestCrefo
 * sowie AB30XMLProperties fuer das Parsing.
 *
 * WICHTIG:
 * - PHASE-1: Zuordnungen sind eine UNTERMENGE von PHASE-2
 *   -> Test prueft nur, dass Crefos bei MINDESTENS den definierten Kunden vorkommen
 * - PHASE-2: Zuordnungen sind VOLLSTAENDIG
 *   -> Test prueft EXAKTE Uebereinstimmung
 *
 * Hinweis: Nur pXX und xXX Eintraege werden geprueft (nicht nXX).
 *
 * Testdaten liegen unter: src/test/resources/ITSQ/OLD/
 */
@DisplayName("Crefo Konsistenz Tests (OLD)")
class CrefoConsistencyTest extends CrefoConsistencyTestBase {

    private static final String BASE_PATH = "/ITSQ/OLD";
    private static final String ARCHIV_BESTAND_PH1 = BASE_PATH + "/ARCHIV-BESTAND-PH1";
    private static final String ARCHIV_BESTAND_PH2 = BASE_PATH + "/ARCHIV-BESTAND-PH2";
    private static final String REF_EXPORTS = BASE_PATH + "/REF-EXPORTS";

    private static final List<String> CUSTOMERS = List.of("c01", "c02", "c03", "c04", "c05");

    private Map<String, Set<String>> phase1CrefoToCustomers;
    private Map<String, Set<String>> phase2CrefoToCustomers;
    private Map<String, Set<String>> actualCrefoToCustomers;

    @BeforeEach
    void setUp() throws IOException, URISyntaxException {
        phase1CrefoToCustomers = loadMappingsFromResource(ARCHIV_BESTAND_PH1 + "/" + TEST_CREFOS_FILE);
        phase2CrefoToCustomers = loadMappingsFromResource(ARCHIV_BESTAND_PH2 + "/" + TEST_CREFOS_FILE);
        actualCrefoToCustomers = loadActualMappings(REF_EXPORTS, CUSTOMERS);
    }

    // ===== Basis-Tests =====

    @Test
    @DisplayName("TestCrefos.properties (PHASE-1) sollte existieren")
    void testCrefosPhase1FileShouldExist() throws URISyntaxException {
        Path path = getResourcePath(ARCHIV_BESTAND_PH1 + "/" + TEST_CREFOS_FILE);
        assertNotNull(path, "TestCrefos.properties (PHASE-1) sollte im Classpath existieren");
        assertTrue(Files.exists(path), "TestCrefos.properties (PHASE-1) sollte existieren: " + path);
        assertFalse(phase1CrefoToCustomers.isEmpty(), "PHASE-1 sollte mindestens einen Eintrag enthalten");
    }

    @Test
    @DisplayName("TestCrefos.properties (PHASE-2) sollte existieren")
    void testCrefosPhase2FileShouldExist() throws URISyntaxException {
        Path path = getResourcePath(ARCHIV_BESTAND_PH2 + "/" + TEST_CREFOS_FILE);
        assertNotNull(path, "TestCrefos.properties (PHASE-2) sollte im Classpath existieren");
        assertTrue(Files.exists(path), "TestCrefos.properties (PHASE-2) sollte existieren: " + path);
        assertFalse(phase2CrefoToCustomers.isEmpty(), "PHASE-2 sollte mindestens einen Eintrag enthalten");
    }

    @Test
    @DisplayName("Relevanz_Positiv Dateien sollten existieren")
    void relevanzPositivFilesShouldExist() throws URISyntaxException {
        for (String customer : CUSTOMERS) {
            String resourcePath = REF_EXPORTS + "/" + customer + "/" + RELEVANZ_POSITIV + "/" + RELEVANZ_FILE;
            Path path = getResourcePath(resourcePath);
            assertNotNull(path, "Relevanz.properties fuer " + customer + " sollte im Classpath existieren");
            assertTrue(Files.exists(path),
                    "Relevanz.properties fuer " + customer + " sollte existieren: " + path);
        }
    }

    // ===== PHASE-1 Tests (Untermenge) =====

    @Nested
    @DisplayName("PHASE-1 Tests (Untermenge)")
    class Phase1Tests {

        @Test
        @DisplayName("PHASE-1: Crefos muessen bei MINDESTENS den definierten Kunden vorkommen")
        void phase1CrefosShouldAppearAtLeastAtDefinedCustomers() {
            List<String> errors = checkSubsetConsistency(phase1CrefoToCustomers, actualCrefoToCustomers, "PHASE-1");

            if (!errors.isEmpty()) {
                fail("PHASE-1 Konsistenzfehler gefunden:\n" + String.join("\n", errors));
            }
        }

        @ParameterizedTest(name = "PHASE-1: Crefo {0} muss mindestens bei {1} vorkommen")
        @CsvSource({
                "1234567891, 'c02;c05'",
                "1234567892, 'c01'"
        })
        @DisplayName("PHASE-1: Spezifische Crefo-Zuordnungen pruefen")
        void phase1SpecificCrefoMappings(String crefo, String expectedCustomersStr) {
            Set<String> expectedMinCustomers = Arrays.stream(expectedCustomersStr.split(";"))
                    .map(String::trim)
                    .collect(Collectors.toSet());

            Set<String> actualCustomers = actualCrefoToCustomers.getOrDefault(crefo, Collections.emptySet());

            // Alle erwarteten Kunden muessen enthalten sein
            assertTrue(actualCustomers.containsAll(expectedMinCustomers),
                    String.format("Crefo %s muss mindestens bei %s vorkommen, ist aber nur bei %s",
                            crefo, expectedMinCustomers, actualCustomers));
        }
    }

    // ===== PHASE-2 Tests (Exakte Uebereinstimmung) =====

    @Nested
    @DisplayName("PHASE-2 Tests (Exakt)")
    class Phase2Tests {

        @Test
        @DisplayName("PHASE-2: Crefos muessen EXAKT bei den definierten Kunden vorkommen")
        void phase2CrefosShouldMatchExactly() {
            List<String> errors = checkExactConsistency(phase2CrefoToCustomers, actualCrefoToCustomers, "PHASE-2");

            if (!errors.isEmpty()) {
                fail("PHASE-2 Konsistenzfehler gefunden:\n" + String.join("\n", errors));
            }
        }

        @ParameterizedTest(name = "PHASE-2: Crefo {0} muss exakt bei {1} vorkommen")
        @CsvSource({
                "1234567891, 'c02;c03;c05'",
                "1234567892, 'c01;c03;c05'",
                "1234567893, 'c02'",
                "1234567894, 'c01;c03'",
                "1234567895, 'c01;c04'",
                "1234567896, 'c01;c04;c05'",
                "1234567897, 'c04;c05'",
                "1234567898, 'c05'"
        })
        @DisplayName("PHASE-2: Spezifische Crefo-Zuordnungen pruefen")
        void phase2SpecificCrefoMappings(String crefo, String expectedCustomersStr) {
            Set<String> expectedCustomers = Arrays.stream(expectedCustomersStr.split(";"))
                    .map(String::trim)
                    .collect(Collectors.toSet());

            Set<String> actualCustomers = actualCrefoToCustomers.getOrDefault(crefo, Collections.emptySet());

            assertEquals(expectedCustomers, actualCustomers,
                    String.format("Crefo %s muss exakt bei %s vorkommen, ist aber bei %s",
                            crefo, expectedCustomers, actualCustomers));
        }
    }

    // ===== Validierungs-Tests =====

    @Nested
    @DisplayName("Validierungs-Tests")
    class ValidationTests {

        @Test
        @DisplayName("PHASE-1 Zuordnungen muessen Untermenge von PHASE-2 sein")
        void phase1ShouldBeSubsetOfPhase2() {
            List<String> errors = checkPhase1SubsetOfPhase2(phase1CrefoToCustomers, phase2CrefoToCustomers);

            if (!errors.isEmpty()) {
                fail("PHASE-1 ist keine Untermenge von PHASE-2:\n" + String.join("\n", errors));
            }
        }

        @Test
        @DisplayName("Keine undefinierten Crefos in Relevanz_Positiv (bezogen auf PHASE-2)")
        void noUndefinedCrefosInRelevanzFiles() {
            List<String> undefinedCrefos = new ArrayList<>();

            for (Map.Entry<String, Set<String>> entry : actualCrefoToCustomers.entrySet()) {
                String crefo = entry.getKey();
                if (!phase2CrefoToCustomers.containsKey(crefo)) {
                    undefinedCrefos.add(String.format("Crefo %s bei %s (nicht in PHASE-2 TestCrefos.properties definiert)",
                            crefo, entry.getValue()));
                }
            }

            if (!undefinedCrefos.isEmpty()) {
                System.out.println("WARNUNG: Undefinierte Crefos gefunden:\n" + String.join("\n", undefinedCrefos));
            }
        }
    }

    // ===== Helper Tests =====

    @Nested
    @DisplayName("Datei-Parsing Tests")
    class ParsingTests {

        @Test
        @DisplayName("AB30XMLProperties sollte TestCrefos-Zeile korrekt parsen")
        void ab30XmlPropertiesShouldParseCorrectly() {
            String line = "1234567891::[c02;c03;c05],[412],[1234567895],[BILANZ],[KEINE],[CTA_STATISTIK],[DSGVO_SPERRE]";
            AB30XMLProperties props = new AB30XMLProperties(line, 2);

            assertEquals(1234567891L, props.getCrefoNr(), "Crefo sollte extrahiert werden");
            assertEquals(List.of("c02", "c03", "c05"), props.getUsedByCustomersList(), "Kunden sollten extrahiert werden");
            assertEquals(412L, props.getAuftragClz(), "CLZ sollte extrahiert werden");
            assertTrue(props.getBtlgCrefosList().contains(1234567895L), "Beteiligter sollte extrahiert werden");
            assertEquals(AB30XMLProperties.BILANZEN_TYPE.BILANZ, props.getBilanzType(), "BilanzType sollte extrahiert werden");
            assertTrue(props.isMitCtaStatistik(), "CTA_STATISTIK sollte true sein");
            assertTrue(props.isMitDsgVoSperre(), "DSGVO_SPERRE sollte true sein");
        }

        @Test
        @DisplayName("TestScenario sollte Relevanz.properties korrekt laden")
        void testScenarioShouldLoadRelevanzCorrectly() throws URISyntaxException {
            // Lade TestCustomer fuer c01
            Map<String, TestCustomer> customers = loadTestCustomers(REF_EXPORTS, List.of("c01"));
            assertFalse(customers.isEmpty(), "Customer c01 sollte geladen werden");

            TestCustomer c01 = customers.get("c01");
            assertNotNull(c01, "TestCustomer c01 sollte existieren");

            // Pruefe ob Relevanz_Positiv Szenario geladen wurde
            Map<String, TestScenario> scenarios = c01.getTestScenariosMap();
            assertTrue(scenarios.containsKey(RELEVANZ_POSITIV), "Relevanz_Positiv Szenario sollte existieren");

            TestScenario relevanzPositiv = scenarios.get(RELEVANZ_POSITIV);
            assertFalse(relevanzPositiv.getTestCrefosAsList().isEmpty(), "TestCrefos sollten geladen sein");

            // Pruefe einzelne TestCrefo
            List<TestCrefo> testCrefos = relevanzPositiv.getTestCrefosAsList();
            boolean foundPositive = testCrefos.stream().anyMatch(tc -> tc.isShouldBeExported());
            assertTrue(foundPositive, "Es sollte mindestens eine positive TestCrefo geben");
        }
    }

    // ===== Reporting =====

    @Test
    @DisplayName("Konsistenz-Report erstellen")
    void createConsistencyReport() {
        System.out.println("\n===== CREFO KONSISTENZ-REPORT =====\n");

        System.out.println("--- PHASE-1 (Untermenge) ---");
        System.out.println("Definierte Crefos in ARCHIV-BESTAND-PH1/TestCrefos.properties:");
        phase1CrefoToCustomers.forEach((crefo, customers) ->
                System.out.printf("  %s -> %s%n", crefo, customers));

        System.out.println("\n--- PHASE-2 (Vollstaendig) ---");
        System.out.println("Definierte Crefos in ARCHIV-BESTAND-PH2/TestCrefos.properties:");
        phase2CrefoToCustomers.forEach((crefo, customers) ->
                System.out.printf("  %s -> %s%n", crefo, customers));

        System.out.println("\n--- Tatsaechlich (REF-EXPORTS) ---");
        System.out.println("Crefos in Relevanz_Positiv:");
        actualCrefoToCustomers.forEach((crefo, customers) ->
                System.out.printf("  %s -> %s%n", crefo, customers));

        System.out.println("\n===== ENDE REPORT =====\n");
    }
}
