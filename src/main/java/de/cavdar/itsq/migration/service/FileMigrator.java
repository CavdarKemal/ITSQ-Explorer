package de.cavdar.itsq.migration.service;

import de.cavdar.itsq.TestSupportClientKonstanten.TEST_PHASE;
import de.cavdar.itsq.migration.model.MigrationConfig;
import de.cavdar.itsq.migration.model.MigrationProblem;
import de.cavdar.itsq.migration.model.MigrationResult;
import de.cavdar.itsq.migration.model.TestCasePhaseAssignment;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * Handhabt Dateikopierung und -generierung waehrend der Migration.
 */
public class FileMigrator {

    private final MigrationConfig config;
    private final MigrationResult result;
    private final OldStructureAnalyzer analyzer;
    private final PhaseAssignmentCalculator calculator;
    private final NewStructureBuilder structureBuilder;
    private Consumer<String> progressCallback;

    public FileMigrator(MigrationConfig config, MigrationResult result,
                        OldStructureAnalyzer analyzer, PhaseAssignmentCalculator calculator,
                        NewStructureBuilder structureBuilder) {
        this.config = config;
        this.result = result;
        this.analyzer = analyzer;
        this.calculator = calculator;
        this.structureBuilder = structureBuilder;
    }

    public void setProgressCallback(Consumer<String> callback) {
        this.progressCallback = callback;
    }

    private void reportProgress(String message) {
        if (progressCallback != null) {
            progressCallback.accept(message);
        }
    }

    /**
     * Migriert alle Dateien von OLD nach NEW Struktur.
     */
    public void migrateFiles(Map<String, Set<TEST_PHASE>> customerPhases) throws IOException {
        // 1. Migriere ARCHIV-BESTAND XMLs
        migrateArchivBestandFiles(TEST_PHASE.PHASE_1);
        migrateArchivBestandFiles(TEST_PHASE.PHASE_2);

        // 2. Migriere TestCrefos.properties
        migrateTestCrefosProperties(TEST_PHASE.PHASE_1);
        migrateTestCrefosProperties(TEST_PHASE.PHASE_2);

        // 3. Migriere REF-EXPORTS pro Kunde
        for (Map.Entry<String, Set<TEST_PHASE>> entry : customerPhases.entrySet()) {
            String customerKey = entry.getKey();
            Set<TEST_PHASE> phases = entry.getValue();

            for (TEST_PHASE phase : phases) {
                migrateCustomerFiles(customerKey, phase);
            }
        }
    }

    /**
     * Migriert ARCHIV-BESTAND XML-Dateien fuer eine Phase.
     * Kopiert nur XMLs, die tatsaechlich von gueltigen Testfaellen benoetigt werden.
     */
    private void migrateArchivBestandFiles(TEST_PHASE phase) throws IOException {
        reportProgress("Migriere ARCHIV-BESTAND " + phase.getDirName() + "...");

        Set<Long> neededCrefos = collectNeededCrefos(phase);
        File targetDir = structureBuilder.getArchivBestandTargetDir(phase);

        for (Long crefo : neededCrefos) {
            File sourceXml = phase == TEST_PHASE.PHASE_1
                    ? analyzer.getArchivBestandXmlPhase1(crefo)
                    : analyzer.getArchivBestandXmlPhase2(crefo);

            if (sourceXml != null && sourceXml.exists()) {
                File targetXml = new File(targetDir, crefo + ".xml");
                copyFile(sourceXml, targetXml);
            }
        }
    }

    /**
     * Sammelt alle Crefo-Nummern, die fuer eine Phase basierend auf gueltigen Testfaellen benoetigt werden.
     */
    private Set<Long> collectNeededCrefos(TEST_PHASE phase) {
        Set<Long> crefos = new TreeSet<>();
        for (TestCasePhaseAssignment assignment : result.getAllAssignments()) {
            if (assignment.isValidForPhase(phase) && assignment.shouldBeExported()) {
                crefos.add(assignment.getCrefoNr());
            }
        }
        return crefos;
    }

    /**
     * Migriert TestCrefos.properties fuer eine Phase.
     * Filtert Eintraege, um nur Crefos zu enthalten, die von gueltigen Testfaellen verwendet werden.
     */
    private void migrateTestCrefosProperties(TEST_PHASE phase) throws IOException {
        reportProgress("Generiere TestCrefos.properties fuer " + phase.getDirName() + "...");

        File sourceProps = phase == TEST_PHASE.PHASE_1
                ? analyzer.getTestCrefosPropsPhase1()
                : analyzer.getTestCrefosPropsPhase2();

        if (sourceProps == null || !sourceProps.exists()) {
            result.addWarning("TestCrefos.properties nicht gefunden fuer " + phase.getDirName());
            return;
        }

        Set<Long> neededCrefos = collectNeededCrefos(phase);
        File targetDir = structureBuilder.getArchivBestandTargetDir(phase);
        File targetProps = new File(targetDir, "TestCrefos.properties");

        // Filtere und schreibe Properties
        List<String> sourceLines = FileUtils.readLines(sourceProps, StandardCharsets.UTF_8);
        List<String> targetLines = new ArrayList<>();

        for (String line : sourceLines) {
            if (line.isEmpty() || line.trim().startsWith("#")) {
                // Behalte Kommentare und Leerzeilen
                targetLines.add(line);
            } else {
                // Pruefe ob die Crefo dieser Zeile benoetigt wird
                Long crefo = extractCrefoFromPropsLine(line);
                if (crefo != null && neededCrefos.contains(crefo)) {
                    targetLines.add(line);
                }
            }
        }

        FileUtils.writeLines(targetProps, StandardCharsets.UTF_8.name(), targetLines);
        result.incrementFilesCreated();
    }

    /**
     * Extrahiert Crefo-Nummer aus TestCrefos.properties Zeile.
     * Format: 1234567891::[...]
     */
    private Long extractCrefoFromPropsLine(String line) {
        int colonIdx = line.indexOf("::");
        if (colonIdx > 0) {
            try {
                return Long.parseLong(line.substring(0, colonIdx).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Migriert alle Dateien fuer einen Kunden in einer bestimmten Phase.
     */
    private void migrateCustomerFiles(String customerKey, TEST_PHASE phase) throws IOException {
        reportProgress("Migriere Kunde " + customerKey + " " + phase.getDirName() + "...");

        // Kopiere Options.cfg
        copyOptionsCfg(customerKey, phase);

        // Migriere Szenarien
        Set<String> scenarios = calculator.getScenariosForCustomerAndPhase(customerKey, phase);
        for (String scenarioName : scenarios) {
            migrateScenarioFiles(customerKey, scenarioName, phase);
        }
    }

    /**
     * Kopiert Options.cfg fuer einen Kunden in eine Phase.
     */
    private void copyOptionsCfg(String customerKey, TEST_PHASE phase) throws IOException {
        File sourceOptions = analyzer.getOptionsCfgFile(customerKey);
        if (sourceOptions != null && sourceOptions.exists()) {
            File targetDir = structureBuilder.getCustomerTargetDir(customerKey, phase);
            File targetOptions = new File(targetDir, "Options.cfg");
            copyFile(sourceOptions, targetOptions);
        }
    }

    /**
     * Migriert Szenario-Dateien (Relevanz.properties und XMLs).
     */
    private void migrateScenarioFiles(String customerKey, String scenarioName, TEST_PHASE phase) throws IOException {
        File targetDir = structureBuilder.getScenarioTargetDir(customerKey, scenarioName, phase);

        // Hole gueltige Testfaelle fuer dieses Szenario
        List<TestCasePhaseAssignment> validTestCases = calculator.getValidTestCases(customerKey, scenarioName, phase);

        if (validTestCases.isEmpty()) {
            return;
        }

        // Generiere gefilterte Relevanz.properties
        generateRelevanzProperties(targetDir, validTestCases);

        // Kopiere XML-Dateien fuer positive Testfaelle
        for (TestCasePhaseAssignment assignment : validTestCases) {
            if (assignment.shouldBeExported()) {
                copyRefExportXml(assignment, targetDir);
            }
        }
    }

    /**
     * Generiert Relevanz.properties fuer gueltige Testfaelle.
     */
    private void generateRelevanzProperties(File targetDir, List<TestCasePhaseAssignment> testCases) throws IOException {
        File targetProps = new File(targetDir, "Relevanz.properties");
        List<String> lines = new ArrayList<>();

        lines.add("#Testfall-Name=Test-Nummer # Kommentar");

        for (TestCasePhaseAssignment tc : testCases) {
            String line = tc.getTestFallName() + "=" + tc.getCrefoNr();
            if (tc.getTestFallInfo() != null && !tc.getTestFallInfo().isEmpty()) {
                line += " # " + tc.getTestFallInfo();
            }
            lines.add(line);
        }

        FileUtils.writeLines(targetProps, StandardCharsets.UTF_8.name(), lines);
        result.incrementFilesCreated();
    }

    /**
     * Kopiert eine REF-EXPORT XML-Datei fuer einen Testfall.
     */
    private void copyRefExportXml(TestCasePhaseAssignment assignment, File targetDir) throws IOException {
        File sourceXml = assignment.getSourceRefExportXml();
        if (sourceXml != null && sourceXml.exists()) {
            File targetXml = new File(targetDir, sourceXml.getName());
            copyFile(sourceXml, targetXml);
        } else {
            // Protokolliere fehlende XML
            MigrationProblem problem = new MigrationProblem(
                    MigrationProblem.ProblemType.MISSING_REF_EXPORT_XML,
                    assignment.getCustomerKey(),
                    assignment.getScenarioName(),
                    assignment.getTestFallName(),
                    assignment.getCrefoNr(),
                    "REF-EXPORT XML nicht gefunden",
                    assignment.toString()
            );
            result.addProblem(problem);
        }
    }

    /**
     * Kopiert eine Datei an den Zielort.
     */
    private void copyFile(File source, File target) throws IOException {
        if (target.exists()) {
            if (config.isOverwriteExisting()) {
                FileUtils.copyFile(source, target);
                result.incrementFilesCopied();
            } else {
                result.incrementFilesSkipped();
            }
        } else {
            FileUtils.copyFile(source, target);
            result.incrementFilesCopied();
        }
    }

    /**
     * Erstellt ein Backup des Zielverzeichnisses falls es existiert.
     */
    public File createBackup() throws IOException {
        File targetPath = config.getTargetNewPath();
        if (!targetPath.exists()) {
            return null;
        }

        reportProgress("Erstelle Backup...");

        String backupName = targetPath.getName() + "_backup_" + System.currentTimeMillis();
        File backupDir = new File(targetPath.getParentFile(), backupName);

        FileUtils.copyDirectory(targetPath, backupDir);
        result.setBackupDirectory(backupDir);
        result.addInfo("Backup erstellt: " + backupDir.getAbsolutePath());

        return backupDir;
    }

    /**
     * Bereinigt das Zielverzeichnis falls es existiert.
     */
    public void cleanupTarget() throws IOException {
        File targetPath = config.getTargetNewPath();
        if (targetPath.exists()) {
            reportProgress("Bereinige Zielverzeichnis...");
            FileUtils.deleteDirectory(targetPath);
        }
    }
}
