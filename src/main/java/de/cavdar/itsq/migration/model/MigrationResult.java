package de.cavdar.itsq.migration.model;

import de.cavdar.itsq.TestSupportClientKonstanten.TEST_PHASE;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Ergebnis einer ITSQ-Migration mit Statistiken und Fehlerverfolgung.
 */
public class MigrationResult {

    public enum Status {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        COMPLETED_WITH_WARNINGS,
        FAILED,
        CANCELLED
    }

    private Status status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private MigrationConfig config;

    // Statistiken
    private int totalCustomers;
    private int customersPhase1;
    private int customersPhase2;

    private int totalScenarios;
    private int scenariosPhase1;
    private int scenariosPhase2;

    private int totalTestCases;
    private int testCasesPhase1;
    private int testCasesPhase2;

    private int filesCreated;
    private int filesCopied;
    private int filesSkipped;

    // Verfolgung
    private final List<MigrationProblem> problems;
    private final List<String> warnings;
    private final List<String> infoMessages;
    private final Map<String, Set<String>> customersPerPhase;
    private final Map<String, List<TestCasePhaseAssignment>> assignmentsByCustomer;
    private File backupDirectory;

    public MigrationResult() {
        this.status = Status.NOT_STARTED;
        this.problems = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.infoMessages = new ArrayList<>();
        this.customersPerPhase = new HashMap<>();
        this.customersPerPhase.put(TEST_PHASE.PHASE_1.getDirName(), new TreeSet<>());
        this.customersPerPhase.put(TEST_PHASE.PHASE_2.getDirName(), new TreeSet<>());
        this.assignmentsByCustomer = new TreeMap<>();
    }

    public void start(MigrationConfig config) {
        this.config = config;
        this.status = Status.IN_PROGRESS;
        this.startTime = LocalDateTime.now();
    }

    public void complete() {
        this.endTime = LocalDateTime.now();
        if (problems.isEmpty() && warnings.isEmpty()) {
            this.status = Status.COMPLETED;
        } else if (problems.isEmpty()) {
            this.status = Status.COMPLETED_WITH_WARNINGS;
        } else {
            // Pruefen ob alle Probleme geloest wurden
            boolean allResolved = problems.stream()
                    .allMatch(p -> p.getResolution() != null &&
                            p.getResolution() != MigrationProblem.Resolution.ABORT);
            this.status = allResolved ? Status.COMPLETED_WITH_WARNINGS : Status.FAILED;
        }
    }

    public void fail(String reason) {
        this.endTime = LocalDateTime.now();
        this.status = Status.FAILED;
        addWarning("Migration fehlgeschlagen: " + reason);
    }

    public void cancel() {
        this.endTime = LocalDateTime.now();
        this.status = Status.CANCELLED;
    }

    // Status-Methoden
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return status == Status.COMPLETED || status == Status.COMPLETED_WITH_WARNINGS;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Duration getDuration() {
        if (startTime == null) return Duration.ZERO;
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return Duration.between(startTime, end);
    }

    public MigrationConfig getConfig() {
        return config;
    }

    // Statistik-Setter
    public void setTotalCustomers(int count) {
        this.totalCustomers = count;
    }

    public void setCustomersPhase1(int count) {
        this.customersPhase1 = count;
    }

    public void setCustomersPhase2(int count) {
        this.customersPhase2 = count;
    }

    public void setTotalScenarios(int count) {
        this.totalScenarios = count;
    }

    public void setScenariosPhase1(int count) {
        this.scenariosPhase1 = count;
    }

    public void setScenariosPhase2(int count) {
        this.scenariosPhase2 = count;
    }

    public void setTotalTestCases(int count) {
        this.totalTestCases = count;
    }

    public void setTestCasesPhase1(int count) {
        this.testCasesPhase1 = count;
    }

    public void setTestCasesPhase2(int count) {
        this.testCasesPhase2 = count;
    }

    public void incrementFilesCreated() {
        this.filesCreated++;
    }

    public void incrementFilesCopied() {
        this.filesCopied++;
    }

    public void incrementFilesSkipped() {
        this.filesSkipped++;
    }

    // Statistik-Getter
    public int getTotalCustomers() {
        return totalCustomers;
    }

    public int getCustomersPhase1() {
        return customersPhase1;
    }

    public int getCustomersPhase2() {
        return customersPhase2;
    }

    public int getTotalScenarios() {
        return totalScenarios;
    }

    public int getScenariosPhase1() {
        return scenariosPhase1;
    }

    public int getScenariosPhase2() {
        return scenariosPhase2;
    }

    public int getTotalTestCases() {
        return totalTestCases;
    }

    public int getTestCasesPhase1() {
        return testCasesPhase1;
    }

    public int getTestCasesPhase2() {
        return testCasesPhase2;
    }

    public int getFilesCreated() {
        return filesCreated;
    }

    public int getFilesCopied() {
        return filesCopied;
    }

    public int getFilesSkipped() {
        return filesSkipped;
    }

    // Probleme und Warnungen
    public void addProblem(MigrationProblem problem) {
        this.problems.add(problem);
    }

    public List<MigrationProblem> getProblems() {
        return Collections.unmodifiableList(problems);
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public void addInfo(String info) {
        this.infoMessages.add(info);
    }

    public List<String> getInfoMessages() {
        return Collections.unmodifiableList(infoMessages);
    }

    // Kundenverfolgung
    public void addCustomerToPhase(String customerKey, TEST_PHASE phase) {
        customersPerPhase.get(phase.getDirName()).add(customerKey);
    }

    public Set<String> getCustomersForPhase(TEST_PHASE phase) {
        return Collections.unmodifiableSet(customersPerPhase.get(phase.getDirName()));
    }

    // Zuordnungsverfolgung
    public void addAssignment(TestCasePhaseAssignment assignment) {
        String customerKey = assignment.getCustomerKey();
        assignmentsByCustomer.computeIfAbsent(customerKey, k -> new ArrayList<>()).add(assignment);
    }

    public List<TestCasePhaseAssignment> getAssignmentsForCustomer(String customerKey) {
        return assignmentsByCustomer.getOrDefault(customerKey, Collections.emptyList());
    }

    public Map<String, List<TestCasePhaseAssignment>> getAssignmentsByCustomer() {
        return Collections.unmodifiableMap(assignmentsByCustomer);
    }

    public List<TestCasePhaseAssignment> getAllAssignments() {
        List<TestCasePhaseAssignment> all = new ArrayList<>();
        assignmentsByCustomer.values().forEach(all::addAll);
        return all;
    }

    // Backup-Methoden
    public File getBackupDirectory() {
        return backupDirectory;
    }

    public void setBackupDirectory(File backupDirectory) {
        this.backupDirectory = backupDirectory;
    }

    // Zusammenfassung
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Migration Summary ===\n");
        sb.append(String.format("Status: %s\n", status));
        if (startTime != null) {
            sb.append(String.format("Dauer: %s\n", formatDuration(getDuration())));
        }
        sb.append("\n--- Statistik ---\n");
        sb.append(String.format("Kunden:     %d -> PHASE-1: %d, PHASE-2: %d\n",
                totalCustomers, customersPhase1, customersPhase2));
        sb.append(String.format("Szenarien:  %d -> PHASE-1: %d, PHASE-2: %d\n",
                totalScenarios, scenariosPhase1, scenariosPhase2));
        sb.append(String.format("Testfaelle: %d -> PHASE-1: %d, PHASE-2: %d\n",
                totalTestCases, testCasesPhase1, testCasesPhase2));
        sb.append(String.format("\nDateien: %d erstellt, %d kopiert, %d uebersprungen\n",
                filesCreated, filesCopied, filesSkipped));
        if (!problems.isEmpty()) {
            sb.append(String.format("\nProbleme: %d\n", problems.size()));
        }
        if (!warnings.isEmpty()) {
            sb.append(String.format("Warnungen: %d\n", warnings.size()));
        }
        return sb.toString();
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + " Sekunden";
        } else if (seconds < 3600) {
            return String.format("%d:%02d Minuten", seconds / 60, seconds % 60);
        } else {
            return String.format("%d:%02d:%02d Stunden", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
        }
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
