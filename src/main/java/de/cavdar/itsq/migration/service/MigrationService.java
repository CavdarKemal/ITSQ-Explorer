package de.cavdar.itsq.migration.service;

import de.cavdar.itsq.TestSupportClientKonstanten.TEST_PHASE;
import de.cavdar.itsq.migration.model.MigrationConfig;
import de.cavdar.itsq.migration.model.MigrationProblem;
import de.cavdar.itsq.migration.model.MigrationResult;
import de.cavdar.itsq.migration.model.TestCasePhaseAssignment;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Hauptservice der die ITSQ-Migration von OLD nach NEW Struktur orchestriert.
 */
public class MigrationService {

    private MigrationConfig config;
    private MigrationResult result;
    private OldStructureAnalyzer analyzer;
    private PhaseAssignmentCalculator calculator;
    private NewStructureBuilder structureBuilder;
    private FileMigrator fileMigrator;
    private MigrationValidator validator;

    private Consumer<String> progressCallback;
    private Consumer<Integer> progressPercentCallback;
    private Function<MigrationProblem, MigrationProblem.Resolution> problemHandler;
    private volatile boolean cancelled;

    public MigrationService() {
        this.cancelled = false;
    }

    public void setProgressCallback(Consumer<String> callback) {
        this.progressCallback = callback;
    }

    public void setProgressPercentCallback(Consumer<Integer> callback) {
        this.progressPercentCallback = callback;
    }

    public void setProblemHandler(Function<MigrationProblem, MigrationProblem.Resolution> handler) {
        this.problemHandler = handler;
    }

    private void reportProgress(String message) {
        if (progressCallback != null) {
            progressCallback.accept(message);
        }
    }

    private void reportProgressPercent(int percent) {
        if (progressPercentCallback != null) {
            progressPercentCallback.accept(percent);
        }
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Analysiert die OLD-Struktur und berechnet Phasenzuordnungen.
     * Dies ist der Vorschau-Schritt vor der eigentlichen Migration.
     */
    public MigrationResult analyze(MigrationConfig config) throws IOException {
        this.config = config;
        this.result = new MigrationResult();
        this.cancelled = false;

        reportProgress("Starte Analyse...");
        reportProgressPercent(0);

        // Initialisiere Analyzer
        analyzer = new OldStructureAnalyzer(config);
        reportProgress("Analysiere OLD-Struktur...");
        analyzer.analyze();
        reportProgressPercent(30);

        if (cancelled) {
            result.cancel();
            return result;
        }

        // Initialisiere Calculator und berechne Zuordnungen
        calculator = new PhaseAssignmentCalculator(analyzer, result);
        reportProgress("Berechne Phasenzuordnungen...");
        calculator.calculateAssignments();
        reportProgressPercent(60);

        if (cancelled) {
            result.cancel();
            return result;
        }

        // Initialisiere Validator und validiere Quelle
        validator = new MigrationValidator(config, result);
        reportProgress("Validiere Quellstruktur...");
        List<String> sourceErrors = validator.validateSource();
        for (String error : sourceErrors) {
            result.addWarning(error);
        }
        reportProgressPercent(80);

        // Validiere Ziel
        reportProgress("Validiere Zielstruktur...");
        List<String> targetErrors = validator.validateTarget();
        for (String error : targetErrors) {
            result.addWarning(error);
        }
        reportProgressPercent(100);

        reportProgress("Analyse abgeschlossen.");
        return result;
    }

    /**
     * Fuehrt die Migration basierend auf den Analyseergebnissen aus.
     */
    public MigrationResult migrate(MigrationConfig config) throws IOException {
        // Fuehre zuerst Analyse durch falls noch nicht geschehen
        if (result == null || analyzer == null) {
            analyze(config);
        }

        if (cancelled) {
            result.cancel();
            return result;
        }

        result.start(config);
        reportProgress("Starte Migration...");
        reportProgressPercent(0);

        try {
            // Berechne Kundenphasen
            Map<String, Set<TEST_PHASE>> customerPhases = new TreeMap<>();
            for (String customerKey : analyzer.getCustomerKeys()) {
                Set<TEST_PHASE> phases = new TreeSet<>(Comparator.comparing(TEST_PHASE::getDirName));
                if (calculator.hasValidTestCasesForPhase(customerKey, TEST_PHASE.PHASE_1)) {
                    phases.add(TEST_PHASE.PHASE_1);
                }
                if (calculator.hasValidTestCasesForPhase(customerKey, TEST_PHASE.PHASE_2)) {
                    phases.add(TEST_PHASE.PHASE_2);
                }
                if (!phases.isEmpty()) {
                    customerPhases.put(customerKey, phases);
                }
            }

            // Erstelle Backup falls angefordert
            if (config.isCreateBackup() && config.getTargetNewPath().exists()) {
                reportProgress("Erstelle Backup...");
                structureBuilder = new NewStructureBuilder(config, result, calculator);
                fileMigrator = new FileMigrator(config, result, analyzer, calculator, structureBuilder);
                fileMigrator.setProgressCallback(this::reportProgress);
                fileMigrator.createBackup();
            }
            reportProgressPercent(10);

            if (cancelled) {
                result.cancel();
                return result;
            }

            // Initialisiere Structure Builder und erstelle Verzeichnisstruktur
            if (structureBuilder == null) {
                structureBuilder = new NewStructureBuilder(config, result, calculator);
            }
            reportProgress("Erstelle Verzeichnisstruktur...");
            structureBuilder.createStructure(customerPhases);
            reportProgressPercent(30);

            if (cancelled) {
                result.cancel();
                return result;
            }

            // Initialisiere File Migrator und kopiere Dateien
            if (fileMigrator == null) {
                fileMigrator = new FileMigrator(config, result, analyzer, calculator, structureBuilder);
                fileMigrator.setProgressCallback(this::reportProgress);
            }
            reportProgress("Kopiere Dateien...");
            fileMigrator.migrateFiles(customerPhases);
            reportProgressPercent(80);

            if (cancelled) {
                result.cancel();
                return result;
            }

            // Validiere Ergebnis
            reportProgress("Validiere Ergebnis...");
            List<String> validationErrors = validator.validateResult();
            for (String error : validationErrors) {
                result.addWarning(error);
            }
            reportProgressPercent(100);

            result.complete();
            reportProgress("Migration abgeschlossen: " + result.getStatus());

        } catch (Exception e) {
            result.fail(e.getMessage());
            throw e;
        }

        return result;
    }

    /**
     * Gibt das aktuelle Analyseergebnis zurueck.
     */
    public MigrationResult getResult() {
        return result;
    }

    /**
     * Gibt alle Testfall-Zuordnungen zurueck.
     */
    public List<TestCasePhaseAssignment> getAllAssignments() {
        return result != null ? result.getAllAssignments() : Collections.emptyList();
    }

    /**
     * Gibt Zuordnungen gruppiert nach Kunde zurueck.
     */
    public Map<String, List<TestCasePhaseAssignment>> getAssignmentsByCustomer() {
        return result != null ? result.getAssignmentsByCustomer() : Collections.emptyMap();
    }

    /**
     * Gibt Kunden zurueck, die zu einer bestimmten Phase gehoeren.
     */
    public Set<String> getCustomersForPhase(TEST_PHASE phase) {
        return result != null ? result.getCustomersForPhase(phase) : Collections.emptySet();
    }

    /**
     * Gibt Probleme zurueck, die waehrend Analyse/Migration aufgetreten sind.
     */
    public List<MigrationProblem> getProblems() {
        return result != null ? result.getProblems() : Collections.emptyList();
    }

    /**
     * Gibt den Analyzer fuer detaillierte Strukturinformationen zurueck.
     */
    public OldStructureAnalyzer getAnalyzer() {
        return analyzer;
    }

    /**
     * Gibt den Calculator fuer Phasenzuordnungs-Abfragen zurueck.
     */
    public PhaseAssignmentCalculator getCalculator() {
        return calculator;
    }

    /**
     * Generiert eine Vorschau-Zusammenfassung.
     */
    public String getPreviewSummary() {
        if (result == null) {
            return "Keine Analyse durchgefuehrt.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Migrations-Vorschau ===\n\n");

        sb.append(String.format("Kunden:     %d -> PHASE-1: %d, PHASE-2: %d\n",
                result.getTotalCustomers(), result.getCustomersPhase1(), result.getCustomersPhase2()));
        sb.append(String.format("Szenarien:  %d -> PHASE-1: %d, PHASE-2: %d\n",
                result.getTotalScenarios(), result.getScenariosPhase1(), result.getScenariosPhase2()));
        sb.append(String.format("Testfaelle: %d -> PHASE-1: %d, PHASE-2: %d\n",
                result.getTotalTestCases(), result.getTestCasesPhase1(), result.getTestCasesPhase2()));

        if (!result.getProblems().isEmpty()) {
            sb.append(String.format("\nProbleme: %d\n", result.getProblems().size()));
        }

        if (!result.getWarnings().isEmpty()) {
            sb.append(String.format("\nWarnungen:\n"));
            for (String warning : result.getWarnings()) {
                sb.append("  - ").append(warning).append("\n");
            }
        }

        return sb.toString();
    }
}
