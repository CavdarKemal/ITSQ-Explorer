package de.cavdar.itsq;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Basisklasse fuer Crefo-Konsistenztests.
 *
 * Stellt gemeinsame Funktionalitaet fuer OLD und NEW Strukturen bereit.
 */
public abstract class CrefoConsistencyTestBase {

    protected static final String TEST_CREFOS_FILE = "TestCrefos.properties";
    protected static final String RELEVANZ_POSITIV = "Relevanz_Positiv";
    protected static final String RELEVANZ_FILE = "Relevanz.properties";

    // Pattern fuer TestCrefos.properties: CREFO::[kunden],[clz],[btlg],[bilanz],[transfer],[statistik],[dsgvo]
    protected static final Pattern CREFO_PATTERN = Pattern.compile("^(\\d+)::\\[([^\\]]*)\\].*");

    // Pattern fuer Relevanz.properties: pXX=CREFO oder xXX=CREFO (nicht nXX)
    protected static final Pattern RELEVANZ_PATTERN = Pattern.compile("^[px]\\d+=\\s*(\\d+).*");

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
     * Laedt die Zuordnungen aus einer TestCrefos.properties Datei vom Classpath.
     * Crefos ohne Kunden (leere Kundenliste) werden ignoriert.
     */
    protected Map<String, Set<String>> loadMappingsFromResource(String resourcePath) throws IOException, URISyntaxException {
        Map<String, Set<String>> mappings = new HashMap<>();

        Path path = getResourcePath(resourcePath);
        if (path == null || !Files.exists(path)) {
            return mappings;
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
                // Nur Crefos mit mindestens einem Kunden aufnehmen
                if (!customers.isEmpty()) {
                    mappings.put(crefo, customers);
                }
            }
        }
        return mappings;
    }

    /**
     * Laedt die tatsaechlichen Zuordnungen aus den Relevanz_Positiv/Relevanz.properties Dateien.
     */
    protected Map<String, Set<String>> loadActualMappings(String refExportsPath, List<String> customers)
            throws IOException, URISyntaxException {
        Map<String, Set<String>> mappings = new HashMap<>();

        for (String customer : customers) {
            String resourcePath = refExportsPath + "/" + customer + "/" + RELEVANZ_POSITIV + "/" + RELEVANZ_FILE;
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
