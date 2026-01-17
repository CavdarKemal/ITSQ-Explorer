package de.cavdar.itsq.old;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Konsistenztest fuer die Zuordnung von Crefo-Nummern zu Kunden.
 *
 * Prueft, ob die Crefos in den Relevanz_Positiv/Relevanz.properties Dateien
 * mit den Definitionen in TestCrefos.properties uebereinstimmen.
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
class CrefoConsistencyTest {

    private static final String BASE_PATH = "/ITSQ/OLD";
    private static final String ARCHIV_BESTAND_PH1 = BASE_PATH + "/ARCHIV-BESTAND-PH1";
    private static final String ARCHIV_BESTAND_PH2 = BASE_PATH + "/ARCHIV-BESTAND-PH2";
    private static final String REF_EXPORTS = BASE_PATH + "/REF-EXPORTS";
    private static final String TEST_CREFOS_FILE = "TestCrefos.properties";
    private static final String RELEVANZ_POSITIV = "Relevanz_Positiv";
    private static final String RELEVANZ_FILE = "Relevanz.properties";

    private static final List<String> CUSTOMERS = List.of("c01", "c02", "c03", "c04", "c05");

    // Pattern fuer TestCrefos.properties: CREFO::[kunden],[clz],[btlg],[bilanz],[transfer],[statistik],[dsgvo]
    private static final Pattern CREFO_PATTERN = Pattern.compile("^(\\d+)::\\[([^\\]]*)\\].*");

    // Pattern fuer Relevanz.properties: pXX=CREFO oder xXX=CREFO (nicht nXX)
    private static final Pattern RELEVANZ_PATTERN = Pattern.compile("^[px]\\d+=\\s*(\\d+).*");

    private Map<String, Set<String>> phase1CrefoToCustomers;
    private Map<String, Set<String>> phase2CrefoToCustomers;
    private Map<String, Set<String>> actualCrefoToCustomers;

    @BeforeEach
    void setUp() throws IOException, URISyntaxException {
        phase1CrefoToCustomers = loadMappingsFromResource(ARCHIV_BESTAND_PH1 + "/" + TEST_CREFOS_FILE);
        phase2CrefoToCustomers = loadMappingsFromResource(ARCHIV_BESTAND_PH2 + "/" + TEST_CREFOS_FILE);
        actualCrefoToCustomers = loadActualMappings();
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

    /**
     * Laedt die Zuordnungen aus einer TestCrefos.properties Datei vom Classpath.
     */
    private Map<String, Set<String>> loadMappingsFromResource(String resourcePath) throws IOException, URISyntaxException {
        Map<String, Set<String>> mappings = new HashMap<>();

        Path path = getResourcePath(resourcePath);
        if (path == null || !Files.exists(path)) {
            return mappings; // Leere Map wenn Datei nicht existiert
        }

        List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }

            Matcher matcher = CREFO_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                String crefo = matcher.group(1);
                String customersStr = matcher.group(2);

                Set<String> customers = new HashSet<>();
                if (!customersStr.isBlank()) {
                    for (String customer : customersStr.split(";")) {
                        String trimmed = customer.trim();
                        if (!trimmed.isEmpty()) {
                            customers.add(trimmed);
                        }
                    }
                }
                mappings.put(crefo, customers);
            }
        }
        return mappings;
    }

    /**
     * Laedt die tatsaechlichen Zuordnungen aus den Relevanz_Positiv/Relevanz.properties Dateien.
     */
    private Map<String, Set<String>> loadActualMappings() throws IOException, URISyntaxException {
        Map<String, Set<String>> mappings = new HashMap<>();

        for (String customer : CUSTOMERS) {
            String resourcePath = REF_EXPORTS + "/" + customer + "/" + RELEVANZ_POSITIV + "/" + RELEVANZ_FILE;
            Path path = getResourcePath(resourcePath);

            if (path == null || !Files.exists(path)) {
                continue;
            }

            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                if (line.startsWith("#") || line.isBlank()) {
                    continue;
                }

                Matcher matcher = RELEVANZ_PATTERN.matcher(line.trim());
                if (matcher.matches()) {
                    String crefo = matcher.group(1);
                    mappings.computeIfAbsent(crefo, k -> new HashSet<>()).add(customer);
                }
            }
        }
        return mappings;
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
            List<String> errors = new ArrayList<>();

            for (Map.Entry<String, Set<String>> entry : phase1CrefoToCustomers.entrySet()) {
                String crefo = entry.getKey();
                Set<String> expectedMinCustomers = entry.getValue();
                Set<String> actualCustomers = actualCrefoToCustomers.getOrDefault(crefo, Collections.emptySet());

                // Pruefe ob ALLE erwarteten Kunden enthalten sind (mehr ist erlaubt!)
                Set<String> missingCustomers = new HashSet<>(expectedMinCustomers);
                missingCustomers.removeAll(actualCustomers);

                if (!missingCustomers.isEmpty()) {
                    errors.add(String.format("Crefo %s: Fehlt bei %s (muss mindestens bei %s vorkommen, ist bei %s)",
                            crefo, missingCustomers, expectedMinCustomers, actualCustomers));
                }
            }

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
            List<String> errors = new ArrayList<>();

            for (Map.Entry<String, Set<String>> entry : phase2CrefoToCustomers.entrySet()) {
                String crefo = entry.getKey();
                Set<String> expectedCustomers = entry.getValue();
                Set<String> actualCustomers = actualCrefoToCustomers.getOrDefault(crefo, Collections.emptySet());

                // Pruefe auf unerwartete Kunden
                Set<String> unexpectedCustomers = new HashSet<>(actualCustomers);
                unexpectedCustomers.removeAll(expectedCustomers);

                if (!unexpectedCustomers.isEmpty()) {
                    errors.add(String.format("Crefo %s: Unerwartet bei %s (erwartet nur bei %s)",
                            crefo, unexpectedCustomers, expectedCustomers));
                }

                // Pruefe auf fehlende Kunden
                Set<String> missingCustomers = new HashSet<>(expectedCustomers);
                missingCustomers.removeAll(actualCustomers);

                if (!missingCustomers.isEmpty()) {
                    errors.add(String.format("Crefo %s: Fehlt bei %s (erwartet bei %s)",
                            crefo, missingCustomers, expectedCustomers));
                }
            }

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
            List<String> errors = new ArrayList<>();

            for (Map.Entry<String, Set<String>> entry : phase1CrefoToCustomers.entrySet()) {
                String crefo = entry.getKey();
                Set<String> phase1Customers = entry.getValue();
                Set<String> phase2Customers = phase2CrefoToCustomers.getOrDefault(crefo, Collections.emptySet());

                if (!phase2Customers.containsAll(phase1Customers)) {
                    Set<String> notInPhase2 = new HashSet<>(phase1Customers);
                    notInPhase2.removeAll(phase2Customers);
                    errors.add(String.format("Crefo %s: PHASE-1 hat %s, aber PHASE-2 hat nur %s (fehlt: %s)",
                            crefo, phase1Customers, phase2Customers, notInPhase2));
                }
            }

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
        @DisplayName("TestCrefos Pattern sollte korrekt matchen")
        void testCrefosPatternShouldMatch() {
            String line = "1234567891::[c02;c03;c05],[412],[1234567895],[BILANZ],[FIRMA_FIRMA],[CTA_STATISTIK],[DSGVO_SPERRE]";
            Matcher matcher = CREFO_PATTERN.matcher(line);

            assertTrue(matcher.matches(), "Pattern sollte matchen");
            assertEquals("1234567891", matcher.group(1), "Crefo sollte extrahiert werden");
            assertEquals("c02;c03;c05", matcher.group(2), "Kunden sollten extrahiert werden");
        }

        @Test
        @DisplayName("Relevanz Pattern sollte pXX und xXX matchen")
        void relevanzPatternShouldMatchPAndX() {
            assertTrue(RELEVANZ_PATTERN.matcher("p01=1234567891 # kommentar").matches());
            assertTrue(RELEVANZ_PATTERN.matcher("x01=1234567892").matches());
            assertFalse(RELEVANZ_PATTERN.matcher("n01=1234567893").matches(), "nXX sollte nicht matchen");
            assertFalse(RELEVANZ_PATTERN.matcher("# kommentar").matches());
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
