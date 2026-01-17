package de.cavdar.itsq;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

/**
 * Basisklasse fuer Crefo-Konsistenztests.
 *
 * Stellt gemeinsame Funktionalitaet fuer OLD und NEW Strukturen bereit.
 * Verwendet die Model-Klassen TestCustomer, TestScenario und TestCrefo.
 */
public abstract class CrefoConsistencyTestBase {

    protected static final String TEST_CREFOS_FILE = "TestCrefos.properties";
    protected static final String RELEVANZ_POSITIV = "Relevanz_Positiv";
    protected static final String RELEVANZ_FILE = "Relevanz.properties";

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
     * Laedt die erwarteten Zuordnungen aus einer TestCrefos.properties Datei vom Classpath.
     * Verwendet AB30XMLProperties fuer das Parsing.
     * Crefos ohne Kunden (leere Kundenliste) werden ignoriert.
     */
    protected Map<String, Set<String>> loadMappingsFromResource(String resourcePath) throws IOException, URISyntaxException {
        Map<String, Set<String>> mappings = new HashMap<>();

        Path path = getResourcePath(resourcePath);
        if (path == null || !Files.exists(path)) {
            return mappings;
        }

        // Verwende AB30MapperUtil zum Laden der Properties
        AB30MapperUtil ab30MapperUtil = new AB30MapperUtil();
        Map<Long, AB30XMLProperties> ab30Map = ab30MapperUtil.initAb30CrefoPropertiesMap(path.toFile());

        // Konvertiere zu String-basierter Map
        for (Map.Entry<Long, AB30XMLProperties> entry : ab30Map.entrySet()) {
            AB30XMLProperties props = entry.getValue();
            List<String> customers = props.getUsedByCustomersList();
            if (!customers.isEmpty()) {
                // Kunden-Keys normalisieren (lowercase)
                Set<String> normalizedCustomers = new HashSet<>();
                for (String customer : customers) {
                    normalizedCustomers.add(customer.toLowerCase(Locale.ROOT));
                }
                mappings.put(String.valueOf(entry.getKey()), normalizedCustomers);
            }
        }

        return mappings;
    }

    /**
     * Laedt die tatsaechlichen Zuordnungen aus den REF-EXPORTS.
     * Verwendet TestCustomer, TestScenario und TestCrefo.
     */
    protected Map<String, Set<String>> loadActualMappings(String refExportsPath, List<String> customerKeys)
            throws IOException, URISyntaxException {
        Map<String, Set<String>> mappings = new HashMap<>();

        Path basePath = getResourcePath(refExportsPath);
        if (basePath == null || !Files.exists(basePath)) {
            return mappings;
        }

        for (String customerKey : customerKeys) {
            Path customerDir = basePath.resolve(customerKey);
            if (!Files.exists(customerDir)) {
                continue;
            }

            // Erstelle TestCustomer mit den notwendigen Verzeichnissen
            TestCustomer testCustomer = new TestCustomer(customerKey, customerKey);
            testCustomer.setItsqRefExportsDir(customerDir.toFile());
            // AB30-XMLs-Dir wird fuer das Laden der Relevanz nicht benoetigt,
            // aber wir setzen es auf das gleiche Verzeichnis um NPEs zu vermeiden
            testCustomer.setItsqAB30XmlsDir(customerDir.toFile());

            // Lade nur das Relevanz_Positiv Szenario
            Path scenarioDir = customerDir.resolve(RELEVANZ_POSITIV);
            if (Files.exists(scenarioDir)) {
                try {
                    // TestScenario liest automatisch die Relevanz.properties und erstellt TestCrefo-Objekte
                    List<File> emptyXmlList = Collections.emptyList();
                    TestScenario testScenario = new TestScenario(testCustomer, RELEVANZ_POSITIV, emptyXmlList);
                    testCustomer.addTestScenario(testScenario);

                    // Extrahiere alle positiven Crefos (pXX und xXX, nicht nXX)
                    for (TestCrefo testCrefo : testScenario.getTestCrefosAsList()) {
                        if (testCrefo.isShouldBeExported()) {
                            String crefo = String.valueOf(testCrefo.getItsqTestCrefoNr());
                            mappings.computeIfAbsent(crefo, k -> new HashSet<>()).add(customerKey.toLowerCase(Locale.ROOT));
                        }
                    }
                } catch (RuntimeException e) {
                    // Szenario konnte nicht geladen werden (z.B. fehlende Properties-Datei)
                    System.err.println("Warnung: Szenario " + RELEVANZ_POSITIV + " fuer Kunde " + customerKey +
                            " konnte nicht geladen werden: " + e.getMessage());
                }
            }
        }

        return mappings;
    }

    /**
     * Laedt TestCustomer-Objekte fuer alle Kunden in einem REF-EXPORTS Verzeichnis.
     * Nützlich fuer erweiterte Tests, die auf die vollstaendigen Model-Objekte zugreifen muessen.
     */
    protected Map<String, TestCustomer> loadTestCustomers(String refExportsPath, List<String> customerKeys)
            throws URISyntaxException {
        Map<String, TestCustomer> customerMap = new TreeMap<>();

        Path basePath = getResourcePath(refExportsPath);
        if (basePath == null || !Files.exists(basePath)) {
            return customerMap;
        }

        for (String customerKey : customerKeys) {
            Path customerDir = basePath.resolve(customerKey);
            if (!Files.exists(customerDir)) {
                continue;
            }

            TestCustomer testCustomer = new TestCustomer(customerKey, customerKey);
            testCustomer.setItsqRefExportsDir(customerDir.toFile());
            testCustomer.setItsqAB30XmlsDir(customerDir.toFile());

            // Lade alle Szenarien (Unterverzeichnisse)
            File[] scenarioDirs = customerDir.toFile().listFiles(File::isDirectory);
            if (scenarioDirs != null) {
                for (File scenarioDir : scenarioDirs) {
                    try {
                        List<File> emptyXmlList = Collections.emptyList();
                        TestScenario testScenario = new TestScenario(testCustomer, scenarioDir.getName(), emptyXmlList);
                        testCustomer.addTestScenario(testScenario);
                    } catch (RuntimeException e) {
                        // Szenario konnte nicht geladen werden
                        System.err.println("Warnung: Szenario " + scenarioDir.getName() + " fuer Kunde " + customerKey +
                                " konnte nicht geladen werden: " + e.getMessage());
                    }
                }
            }

            customerMap.put(customerKey, testCustomer);
        }

        return customerMap;
    }

    /**
     * Prueft, ob alle erwarteten Kunden in den tatsaechlichen Kunden enthalten sind (Untermenge).
     * @return Liste von Fehlermeldungen (leer wenn keine Fehler)
     */
    protected List<String> checkSubsetConsistency(
            Map<String, Set<String>> expectedMappings,
            Map<String, Set<String>> actualMappings,
            String phaseName) {

        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : expectedMappings.entrySet()) {
            String crefo = entry.getKey();
            Set<String> expectedMinCustomers = entry.getValue();
            Set<String> actualCustomers = actualMappings.getOrDefault(crefo, Collections.emptySet());

            Set<String> missingCustomers = new HashSet<>(expectedMinCustomers);
            missingCustomers.removeAll(actualCustomers);

            if (!missingCustomers.isEmpty()) {
                errors.add(String.format("Crefo %s: Fehlt bei %s (muss mindestens bei %s vorkommen, ist bei %s)",
                        crefo, missingCustomers, expectedMinCustomers, actualCustomers));
            }
        }

        return errors;
    }

    /**
     * Prueft exakte Uebereinstimmung zwischen erwarteten und tatsaechlichen Zuordnungen.
     * @return Liste von Fehlermeldungen (leer wenn keine Fehler)
     */
    protected List<String> checkExactConsistency(
            Map<String, Set<String>> expectedMappings,
            Map<String, Set<String>> actualMappings,
            String phaseName) {

        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : expectedMappings.entrySet()) {
            String crefo = entry.getKey();
            Set<String> expectedCustomers = entry.getValue();
            Set<String> actualCustomers = actualMappings.getOrDefault(crefo, Collections.emptySet());

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

        return errors;
    }

    /**
     * Prueft, ob Phase-1 Zuordnungen eine Untermenge von Phase-2 sind.
     * @return Liste von Fehlermeldungen (leer wenn keine Fehler)
     */
    protected List<String> checkPhase1SubsetOfPhase2(
            Map<String, Set<String>> phase1Mappings,
            Map<String, Set<String>> phase2Mappings) {

        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : phase1Mappings.entrySet()) {
            String crefo = entry.getKey();
            Set<String> phase1Customers = entry.getValue();
            Set<String> phase2Customers = phase2Mappings.getOrDefault(crefo, Collections.emptySet());

            if (!phase2Customers.containsAll(phase1Customers)) {
                Set<String> notInPhase2 = new HashSet<>(phase1Customers);
                notInPhase2.removeAll(phase2Customers);
                errors.add(String.format("Crefo %s: PHASE-1 hat %s, aber PHASE-2 hat nur %s (fehlt: %s)",
                        crefo, phase1Customers, phase2Customers, notInPhase2));
            }
        }

        return errors;
    }

    /**
     * Erstellt einen Konsistenz-Report.
     */
    protected void printConsistencyReport(
            String structureName,
            Map<String, Set<String>> phase1Mappings,
            Map<String, Set<String>> phase2Mappings,
            Map<String, Set<String>> actualMappings) {

        System.out.println("\n===== CREFO KONSISTENZ-REPORT (" + structureName + ") =====\n");

        System.out.println("--- PHASE-1 (Untermenge) ---");
        phase1Mappings.forEach((crefo, customers) ->
                System.out.printf("  %s -> %s%n", crefo, customers));

        System.out.println("\n--- PHASE-2 (Vollstaendig) ---");
        phase2Mappings.forEach((crefo, customers) ->
                System.out.printf("  %s -> %s%n", crefo, customers));

        System.out.println("\n--- Tatsaechlich (REF-EXPORTS) ---");
        actualMappings.forEach((crefo, customers) ->
                System.out.printf("  %s -> %s%n", crefo, customers));

        System.out.println("\n===== ENDE REPORT =====\n");
    }
}
