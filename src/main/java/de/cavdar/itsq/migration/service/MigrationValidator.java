package de.cavdar.itsq.migration.service;

import de.cavdar.itsq.TestSupportClientKonstanten.TEST_PHASE;
import de.cavdar.itsq.migration.model.MigrationConfig;
import de.cavdar.itsq.migration.model.MigrationResult;
import de.cavdar.itsq.migration.model.TestCasePhaseAssignment;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Validiert die Migration vor und nach der Ausfuehrung.
 */
public class MigrationValidator {

    private static final Pattern CRF_XML_PATTERN = Pattern.compile("\\d{10}\\.xml");

    private final MigrationConfig config;
    private final MigrationResult result;

    public MigrationValidator(MigrationConfig config, MigrationResult result) {
        this.config = config;
        this.result = result;
    }

    /**
     * Validiert die Quell-OLD-Struktur vor der Migration.
     * @return Liste von Validierungsfehlern (leer wenn gueltig)
     */
    public List<String> validateSource() {
        List<String> errors = new ArrayList<>();

        // Pruefe ob Quellverzeichnis existiert
        File sourceDir = config.getSourceOldPath();
        if (sourceDir == null || !sourceDir.exists()) {
            errors.add("Quellverzeichnis existiert nicht: " + (sourceDir != null ? sourceDir.getAbsolutePath() : "null"));
            return errors;
        }

        // Pruefe ARCHIV-BESTAND-PH1
        File ph1Dir = config.getArchivBestandPh1Dir();
        if (!ph1Dir.exists() || !ph1Dir.isDirectory()) {
            errors.add("ARCHIV-BESTAND-PH1 Verzeichnis fehlt: " + ph1Dir.getAbsolutePath());
        }

        // Pruefe ARCHIV-BESTAND-PH2
        File ph2Dir = config.getArchivBestandPh2Dir();
        if (!ph2Dir.exists() || !ph2Dir.isDirectory()) {
            errors.add("ARCHIV-BESTAND-PH2 Verzeichnis fehlt: " + ph2Dir.getAbsolutePath());
        }

        // Pruefe REF-EXPORTS
        File refExportsDir = config.getRefExportsDir();
        if (!refExportsDir.exists() || !refExportsDir.isDirectory()) {
            errors.add("REF-EXPORTS Verzeichnis fehlt: " + refExportsDir.getAbsolutePath());
        }

        return errors;
    }

    /**
     * Validiert das Ziel vor der Migration.
     * @return Liste von Validierungsfehlern (leer wenn gueltig)
     */
    public List<String> validateTarget() {
        List<String> errors = new ArrayList<>();

        File targetDir = config.getTargetNewPath();
        if (targetDir == null) {
            errors.add("Zielverzeichnis nicht angegeben");
            return errors;
        }

        // Falls Ziel existiert und Ueberschreiben nicht erlaubt ist, warnen
        if (targetDir.exists() && !config.isOverwriteExisting() && !config.isCreateBackup()) {
            errors.add("Zielverzeichnis existiert bereits und Ueberschreiben ist nicht aktiviert: " + targetDir.getAbsolutePath());
        }

        // Pruefe ob Elternverzeichnis existiert oder erstellt werden kann
        File parentDir = targetDir.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                errors.add("Uebergeordnetes Verzeichnis kann nicht erstellt werden: " + parentDir.getAbsolutePath());
            } else {
                // Bereinige Testverzeichnis
                parentDir.delete();
            }
        }

        return errors;
    }

    /**
     * Validiert die NEW-Struktur nach der Migration.
     * @return Liste von Validierungsfehlern (leer wenn gueltig)
     */
    public List<String> validateResult() {
        List<String> errors = new ArrayList<>();

        // Validiere ARCHIV-BESTAND Struktur
        errors.addAll(validateArchivBestandPhase(TEST_PHASE.PHASE_1));
        errors.addAll(validateArchivBestandPhase(TEST_PHASE.PHASE_2));

        // Validiere REF-EXPORTS Struktur
        errors.addAll(validateRefExportsPhase(TEST_PHASE.PHASE_1));
        errors.addAll(validateRefExportsPhase(TEST_PHASE.PHASE_2));

        return errors;
    }

    /**
     * Validiert ARCHIV-BESTAND fuer eine Phase.
     */
    private List<String> validateArchivBestandPhase(TEST_PHASE phase) {
        List<String> errors = new ArrayList<>();

        File phaseDir = phase == TEST_PHASE.PHASE_1
                ? config.getNewArchivBestandPhase1Dir()
                : config.getNewArchivBestandPhase2Dir();

        if (!phaseDir.exists()) {
            errors.add("ARCHIV-BESTAND/" + phase.getDirName() + " fehlt");
            return errors;
        }

        // Pruefe ob TestCrefos.properties existiert
        File testCrefosProps = new File(phaseDir, "TestCrefos.properties");
        if (!testCrefosProps.exists()) {
            errors.add("TestCrefos.properties fehlt in ARCHIV-BESTAND/" + phase.getDirName());
        }

        // Sammle erwartete Crefos basierend auf gueltigen Zuordnungen
        Set<Long> expectedCrefos = new TreeSet<>();
        for (TestCasePhaseAssignment assignment : result.getAllAssignments()) {
            if (assignment.isValidForPhase(phase) && assignment.shouldBeExported()) {
                expectedCrefos.add(assignment.getCrefoNr());
            }
        }

        // Pruefe ob alle erwarteten XML-Dateien existieren
        for (Long crefo : expectedCrefos) {
            File xmlFile = new File(phaseDir, crefo + ".xml");
            if (!xmlFile.exists()) {
                errors.add("Fehlende XML in ARCHIV-BESTAND/" + phase.getDirName() + ": " + crefo + ".xml");
            }
        }

        return errors;
    }

    /**
     * Validiert REF-EXPORTS fuer eine Phase.
     */
    private List<String> validateRefExportsPhase(TEST_PHASE phase) {
        List<String> errors = new ArrayList<>();

        File phaseDir = phase == TEST_PHASE.PHASE_1
                ? config.getNewRefExportsPhase1Dir()
                : config.getNewRefExportsPhase2Dir();

        if (!phaseDir.exists()) {
            errors.add("REF-EXPORTS/" + phase.getDirName() + " fehlt");
            return errors;
        }

        // Pruefe jeden erwarteten Kunden
        Set<String> expectedCustomers = result.getCustomersForPhase(phase);
        for (String customerKey : expectedCustomers) {
            File customerDir = new File(phaseDir, customerKey);
            if (!customerDir.exists()) {
                errors.add("Kundenverzeichnis fehlt: REF-EXPORTS/" + phase.getDirName() + "/" + customerKey);
                continue;
            }

            // Pruefe Options.cfg
            File optionsCfg = new File(customerDir, "Options.cfg");
            // Options.cfg ist optional, daher nur protokollieren wenn fehlend, aber kein Fehler

            // Validiere Szenarien
            errors.addAll(validateCustomerScenarios(customerKey, phase, customerDir));
        }

        return errors;
    }

    /**
     * Validiert Szenarien fuer einen Kunden in einer Phase.
     */
    private List<String> validateCustomerScenarios(String customerKey, TEST_PHASE phase, File customerDir) {
        List<String> errors = new ArrayList<>();

        // Gruppiere Zuordnungen nach Szenario
        Map<String, List<TestCasePhaseAssignment>> scenarioAssignments = new HashMap<>();
        for (TestCasePhaseAssignment assignment : result.getAllAssignments()) {
            if (assignment.getCustomerKey().equals(customerKey) && assignment.isValidForPhase(phase)) {
                scenarioAssignments.computeIfAbsent(assignment.getScenarioName(), k -> new ArrayList<>())
                        .add(assignment);
            }
        }

        for (Map.Entry<String, List<TestCasePhaseAssignment>> entry : scenarioAssignments.entrySet()) {
            String scenarioName = entry.getKey();
            List<TestCasePhaseAssignment> assignments = entry.getValue();

            File scenarioDir = new File(customerDir, scenarioName);
            if (!scenarioDir.exists()) {
                errors.add("Szenario-Verzeichnis fehlt: " + customerKey + "/" + scenarioName + " in " + phase.getDirName());
                continue;
            }

            // Pruefe Relevanz.properties
            File relevanzProps = new File(scenarioDir, "Relevanz.properties");
            if (!relevanzProps.exists()) {
                errors.add("Relevanz.properties fehlt: " + customerKey + "/" + scenarioName + " in " + phase.getDirName());
            } else {
                // Validate Relevanz.properties content
                errors.addAll(validateRelevanzProperties(relevanzProps, assignments,
                        customerKey + "/" + scenarioName + "/" + phase.getDirName()));
            }

            // Pruefe XML-Dateien fuer positive Testfaelle
            for (TestCasePhaseAssignment assignment : assignments) {
                if (assignment.shouldBeExported()) {
                    // Pruefe ob mindestens eine XML mit dem Testfall-Namen und Crefo existiert
                    boolean found = hasMatchingXml(scenarioDir, assignment.getTestFallName(), assignment.getCrefoNr());
                    if (!found) {
                        errors.add("REF-EXPORT XML fehlt: " + assignment.getTestFallName() +
                                " (Crefo: " + assignment.getCrefoNr() + ") in " +
                                customerKey + "/" + scenarioName + "/" + phase.getDirName());
                    }
                }
            }
        }

        return errors;
    }

    /**
     * Validiert dass Relevanz.properties Inhalt mit erwarteten Testfaellen uebereinstimmt.
     */
    private List<String> validateRelevanzProperties(File propsFile, List<TestCasePhaseAssignment> expectedAssignments,
                                                     String location) {
        List<String> errors = new ArrayList<>();

        try {
            List<String> lines = FileUtils.readLines(propsFile, StandardCharsets.UTF_8);
            Set<String> foundTestFalls = new HashSet<>();

            for (String line : lines) {
                if (line.isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("=");
                if (parts.length >= 2) {
                    foundTestFalls.add(parts[0].trim());
                }
            }

            // Pruefe ob alle erwarteten Testfaelle vorhanden sind
            for (TestCasePhaseAssignment assignment : expectedAssignments) {
                if (!foundTestFalls.contains(assignment.getTestFallName())) {
                    errors.add("Fehlender Eintrag in Relevanz.properties: " +
                            assignment.getTestFallName() + " in " + location);
                }
            }

        } catch (IOException e) {
            errors.add("Fehler beim Lesen von Relevanz.properties in " + location + ": " + e.getMessage());
        }

        return errors;
    }

    /**
     * Prueft ob eine passende XML-Datei fuer einen Testfall existiert.
     */
    private boolean hasMatchingXml(File scenarioDir, String testFallName, Long crefoNr) {
        File[] xmlFiles = scenarioDir.listFiles((dir, name) ->
                name.endsWith(".xml") && name.contains(testFallName) && name.contains(crefoNr.toString()));
        return xmlFiles != null && xmlFiles.length > 0;
    }

    /**
     * Generiert einen Validierungsbericht.
     */
    public String generateValidationReport(List<String> errors) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Validierungsbericht ===\n\n");

        if (errors.isEmpty()) {
            sb.append("Keine Fehler gefunden. Die Migration war erfolgreich.\n");
        } else {
            sb.append("Es wurden ").append(errors.size()).append(" Fehler gefunden:\n\n");
            for (int i = 0; i < errors.size(); i++) {
                sb.append(String.format("%d. %s\n", i + 1, errors.get(i)));
            }
        }

        return sb.toString();
    }
}
