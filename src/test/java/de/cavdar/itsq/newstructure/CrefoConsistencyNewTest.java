package de.cavdar.itsq.newstructure;

import de.cavdar.itsq.AB30XMLProperties;
import de.cavdar.itsq.CrefoConsistencyTestBase;
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
 * Konsistenztest fuer die NEW ITSQ-Struktur.
 *
 * NEW Struktur:
 * - ARCHIV-BESTAND/PHASE-1, ARCHIV-BESTAND/PHASE-2
 * - REF-EXPORTS/PHASE-1 (c01, c02), REF-EXPORTS/PHASE-2 (c01-c05)
 * - Phasenspezifische REF-EXPORTS!
 *
 * WICHTIG:
 * - PHASE-1: Zuordnungen sind eine UNTERMENGE von PHASE-2
 * - PHASE-2: Zuordnungen sind VOLLSTAENDIG
 * - Crefos ohne Kunden (nur Beteiligte) werden ignoriert
 *
 * Testdaten liegen unter: src/test/resources/ITSQ/NEW/
 */
@DisplayName("Crefo Konsistenz Tests (NEW)")
class CrefoConsistencyNewTest extends CrefoConsistencyTestBase {

    private static final String BASE_PATH = "/ITSQ/NEW";
    private static final String ARCHIV_BESTAND_PHASE1 = BASE_PATH + "/ARCHIV-BESTAND/PHASE-1";
    private static final String ARCHIV_BESTAND_PHASE2 = BASE_PATH + "/ARCHIV-BESTAND/PHASE-2";
    private static final String REF_EXPORTS_PHASE1 = BASE_PATH + "/REF-EXPORTS/PHASE-1";
    private static final String REF_EXPORTS_PHASE2 = BASE_PATH + "/REF-EXPORTS/PHASE-2";

    // PHASE-1 hat nur c01, c02
    private static final List<String> CUSTOMERS_PHASE1 = List.of("c01", "c02");
    // PHASE-2 hat c01-c05
    private static final List<String> CUSTOMERS_PHASE2 = List.of("c01", "c02", "c03", "c04", "c05");

    private Map<String, Set<String>> phase1CrefoToCustomers;
    private Map<String, Set<String>> phase2CrefoToCustomers;
    private Map<String, Set<String>> actualPhase1CrefoToCustomers;
    private Map<String, Set<String>> actualPhase2CrefoToCustomers;

    @BeforeEach
    void setUp() throws IOException, URISyntaxException {
        phase1CrefoToCustomers = loadMappingsFromResource(ARCHIV_BESTAND_PHASE1 + "/" + TEST_CREFOS_FILE);
        phase2CrefoToCustomers = loadMappingsFromResource(ARCHIV_BESTAND_PHASE2 + "/" + TEST_CREFOS_FILE);
        actualPhase1CrefoToCustomers = loadActualMappings(REF_EXPORTS_PHASE1, CUSTOMERS_PHASE1);
        actualPhase2CrefoToCustomers = loadActualMappings(REF_EXPORTS_PHASE2, CUSTOMERS_PHASE2);
    }

    // ===== Basis-Tests =====

    @Test
    @DisplayName("TestCrefos.properties (PHASE-1) sollte existieren")
    void testCrefosPhase1FileShouldExist() throws URISyntaxException {
        Path path = getResourcePath(ARCHIV_BESTAND_PHASE1 + "/" + TEST_CREFOS_FILE);
        assertNotNull(path, "TestCrefos.properties (PHASE-1) sollte im Classpath existieren");
        assertTrue(Files.exists(path), "TestCrefos.properties (PHASE-1) sollte existieren");
        assertFalse(phase1CrefoToCustomers.isEmpty(), "PHASE-1 sollte mindestens einen Eintrag enthalten");
    }

    @Test
    @DisplayName("TestCrefos.properties (PHASE-2) sollte existieren")
    void testCrefosPhase2FileShouldExist() throws URISyntaxException {
        Path path = getResourcePath(ARCHIV_BESTAND_PHASE2 + "/" + TEST_CREFOS_FILE);
        assertNotNull(path, "TestCrefos.properties (PHASE-2) sollte im Classpath existieren");
        assertTrue(Files.exists(path), "TestCrefos.properties (PHASE-2) sollte existieren");
        assertFalse(phase2CrefoToCustomers.isEmpty(), "PHASE-2 sollte mindestens einen Eintrag enthalten");
    }

    @Test
    @DisplayName("REF-EXPORTS/PHASE-1 Relevanz_Positiv Dateien sollten existieren")
    void relevanzPositivPhase1FilesShouldExist() throws URISyntaxException {
        for (String customer : CUSTOMERS_PHASE1) {
            String resourcePath = REF_EXPORTS_PHASE1 + "/" + customer + "/" + RELEVANZ_POSITIV + "/" + RELEVANZ_FILE;
            Path path = getResourcePath(resourcePath);
            assertNotNull(path, "Relevanz.properties fuer " + customer + " (PHASE-1) sollte existieren");
        }
    }

    @Test
    @DisplayName("REF-EXPORTS/PHASE-2 Relevanz_Positiv Dateien sollten existieren")
    void relevanzPositivPhase2FilesShouldExist() throws URISyntaxException {
        for (String customer : CUSTOMERS_PHASE2) {
            String resourcePath = REF_EXPORTS_PHASE2 + "/" + customer + "/" + RELEVANZ_POSITIV + "/" + RELEVANZ_FILE;
            Path path = getResourcePath(resourcePath);
            assertNotNull(path, "Relevanz.properties fuer " + customer + " (PHASE-2) sollte existieren");
        }
    }

    // ===== PHASE-1 Tests (Untermenge) =====

    @Nested
    @DisplayName("PHASE-1 Tests (Untermenge)")
    class Phase1Tests {

        @Test
        @DisplayName("PHASE-1: Crefos muessen bei MINDESTENS den definierten Kunden vorkommen")
        void phase1CrefosShouldAppearAtLeastAtDefinedCustomers() {
            // Filtere nur Crefos, die in PHASE-1 Kunden haben (c01, c02)
            Map<String, Set<String>> phase1Expected = filterByCustomers(phase1CrefoToCustomers, CUSTOMERS_PHASE1);

            List<String> errors = checkSubsetConsistency(phase1Expected, actualPhase1CrefoToCustomers, "PHASE-1");

            if (!errors.isEmpty()) {
                fail("PHASE-1 Konsistenzfehler gefunden:\n" + String.join("\n", errors));
            }
        }

        @ParameterizedTest(name = "PHASE-1: Crefo {0} muss mindestens bei {1} vorkommen")
        @CsvSource({
                "1234567891, 'c02'",
                "1234567892, 'c01'"
        })
        @DisplayName("PHASE-1: Spezifische Crefo-Zuordnungen pruefen")
        void phase1SpecificCrefoMappings(String crefo, String expectedCustomersStr) {
            Set<String> expectedMinCustomers = Arrays.stream(expectedCustomersStr.split(";"))
                    .map(String::trim)
                    .filter(c -> CUSTOMERS_PHASE1.contains(c))
                    .collect(Collectors.toSet());

            Set<String> actualCustomers = actualPhase1CrefoToCustomers.getOrDefault(crefo, Collections.emptySet());

            assertTrue(actualCustomers.containsAll(expectedMinCustomers),
                    String.format("Crefo %s muss mindestens bei %s vorkommen, ist aber bei %s",
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
            List<String> errors = checkExactConsistency(phase2CrefoToCustomers, actualPhase2CrefoToCustomers, "PHASE-2");

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

            Set<String> actualCustomers = actualPhase2CrefoToCustomers.getOrDefault(crefo, Collections.emptySet());

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
        @DisplayName("Keine undefinierten Crefos in PHASE-2 Relevanz_Positiv")
        void noUndefinedCrefosInPhase2RelevanzFiles() {
            List<String> undefinedCrefos = new ArrayList<>();

            for (Map.Entry<String, Set<String>> entry : actualPhase2CrefoToCustomers.entrySet()) {
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
        @DisplayName("Crefos ohne Kunden sollten ignoriert werden")
        void crefosWithoutCustomersShouldBeIgnored() {
            // 1234567895, 1234567899, 1234567890 haben keine Kunden in PHASE-2
            assertFalse(phase2CrefoToCustomers.containsKey("1234567899"),
                    "1234567899 sollte nicht in den Mappings sein (keine Kunden)");
            assertFalse(phase2CrefoToCustomers.containsKey("1234567890"),
                    "1234567890 sollte nicht in den Mappings sein (keine Kunden)");
        }
    }

    // ===== Reporting =====

    @Test
    @DisplayName("Konsistenz-Report erstellen")
    void createConsistencyReport() {
        System.out.println("\n===== CREFO KONSISTENZ-REPORT (NEW) =====\n");

        System.out.println("--- PHASE-1 (Untermenge) ---");
        System.out.println("Definierte Crefos:");
        phase1CrefoToCustomers.forEach((crefo, customers) ->
                System.out.printf("  %s -> %s%n", crefo, customers));
        System.out.println("Tatsaechlich (REF-EXPORTS/PHASE-1):");
        actualPhase1CrefoToCustomers.forEach((crefo, customers) ->
                System.out.printf("  %s -> %s%n", crefo, customers));

        System.out.println("\n--- PHASE-2 (Vollstaendig) ---");
        System.out.println("Definierte Crefos:");
        phase2CrefoToCustomers.forEach((crefo, customers) ->
                System.out.printf("  %s -> %s%n", crefo, customers));
        System.out.println("Tatsaechlich (REF-EXPORTS/PHASE-2):");
        actualPhase2CrefoToCustomers.forEach((crefo, customers) ->
                System.out.printf("  %s -> %s%n", crefo, customers));

        System.out.println("\n===== ENDE REPORT =====\n");
    }

    // ===== Helper Methods =====

    /**
     * Filtert die Mappings nach den angegebenen Kunden.
     */
    private Map<String, Set<String>> filterByCustomers(Map<String, Set<String>> mappings, List<String> allowedCustomers) {
        Map<String, Set<String>> filtered = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : mappings.entrySet()) {
            Set<String> filteredCustomers = entry.getValue().stream()
                    .filter(allowedCustomers::contains)
                    .collect(Collectors.toSet());
            if (!filteredCustomers.isEmpty()) {
                filtered.put(entry.getKey(), filteredCustomers);
            }
        }
        return filtered;
    }
}
