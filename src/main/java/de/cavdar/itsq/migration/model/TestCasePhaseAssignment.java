package de.cavdar.itsq.migration.model;

import de.cavdar.itsq.TestSupportClientKonstanten.TEST_PHASE;

import java.io.File;

/**
 * Repraesentiert die Phasenzuordnung fuer einen einzelnen Testfall.
 * Bestimmt, ob ein Testfall fuer PHASE-1, PHASE-2 oder beide gueltig ist.
 */
public class TestCasePhaseAssignment {

    public enum TestCaseType {
        POSITIVE("p"),      // pXX - soll exportiert werden
        LOESCHSATZ("x"),    // xXX - Loeschsatz, soll exportiert werden
        NEGATIVE("n");      // nXX - soll NICHT exportiert werden

        private final String prefix;

        TestCaseType(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }

        public static TestCaseType fromTestFallName(String testFallName) {
            if (testFallName == null || testFallName.isEmpty()) {
                return null;
            }
            char firstChar = testFallName.charAt(0);
            if (firstChar == 'p') return POSITIVE;
            if (firstChar == 'x') return LOESCHSATZ;
            if (firstChar == 'n') return NEGATIVE;
            return null;
        }
    }

    public enum AssignmentStatus {
        VALID,              // Testfall ist gueltig fuer diese Phase
        INVALID,            // Testfall ist NICHT gueltig fuer diese Phase
        SKIPPED,            // Benutzer hat diesen Testfall uebersprungen
        ERROR               // Fehler bei der Auswertung
    }

    private final String customerKey;
    private final String scenarioName;
    private final String testFallName;
    private final Long crefoNr;
    private final String testFallInfo;
    private final TestCaseType testCaseType;

    // Phase 1 Zuordnung
    private AssignmentStatus phase1Status;
    private String phase1StatusReason;
    private File phase1ArchivXml;
    private File phase1RefExportXml;

    // Phase 2 Zuordnung
    private AssignmentStatus phase2Status;
    private String phase2StatusReason;
    private File phase2ArchivXml;
    private File phase2RefExportXml;

    // Quelldateien aus OLD-Struktur
    private File sourceRefExportXml;

    public TestCasePhaseAssignment(String customerKey, String scenarioName, String testFallName,
                                   Long crefoNr, String testFallInfo) {
        this.customerKey = customerKey;
        this.scenarioName = scenarioName;
        this.testFallName = testFallName;
        this.crefoNr = crefoNr;
        this.testFallInfo = testFallInfo;
        this.testCaseType = TestCaseType.fromTestFallName(testFallName);
        this.phase1Status = AssignmentStatus.INVALID;
        this.phase2Status = AssignmentStatus.INVALID;
    }

    public String getCustomerKey() {
        return customerKey;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public String getTestFallName() {
        return testFallName;
    }

    public Long getCrefoNr() {
        return crefoNr;
    }

    public String getTestFallInfo() {
        return testFallInfo;
    }

    public TestCaseType getTestCaseType() {
        return testCaseType;
    }

    public boolean shouldBeExported() {
        return testCaseType == TestCaseType.POSITIVE || testCaseType == TestCaseType.LOESCHSATZ;
    }

    // Phase 1 Getter/Setter
    public AssignmentStatus getPhase1Status() {
        return phase1Status;
    }

    public void setPhase1Status(AssignmentStatus status) {
        this.phase1Status = status;
    }

    public String getPhase1StatusReason() {
        return phase1StatusReason;
    }

    public void setPhase1StatusReason(String reason) {
        this.phase1StatusReason = reason;
    }

    public File getPhase1ArchivXml() {
        return phase1ArchivXml;
    }

    public void setPhase1ArchivXml(File file) {
        this.phase1ArchivXml = file;
    }

    public File getPhase1RefExportXml() {
        return phase1RefExportXml;
    }

    public void setPhase1RefExportXml(File file) {
        this.phase1RefExportXml = file;
    }

    public boolean isValidForPhase1() {
        return phase1Status == AssignmentStatus.VALID;
    }

    // Phase 2 Getter/Setter
    public AssignmentStatus getPhase2Status() {
        return phase2Status;
    }

    public void setPhase2Status(AssignmentStatus status) {
        this.phase2Status = status;
    }

    public String getPhase2StatusReason() {
        return phase2StatusReason;
    }

    public void setPhase2StatusReason(String reason) {
        this.phase2StatusReason = reason;
    }

    public File getPhase2ArchivXml() {
        return phase2ArchivXml;
    }

    public void setPhase2ArchivXml(File file) {
        this.phase2ArchivXml = file;
    }

    public File getPhase2RefExportXml() {
        return phase2RefExportXml;
    }

    public void setPhase2RefExportXml(File file) {
        this.phase2RefExportXml = file;
    }

    public boolean isValidForPhase2() {
        return phase2Status == AssignmentStatus.VALID;
    }

    // Quelldateien
    public File getSourceRefExportXml() {
        return sourceRefExportXml;
    }

    public void setSourceRefExportXml(File file) {
        this.sourceRefExportXml = file;
    }

    // Hilfsmethoden
    public boolean isValidForPhase(TEST_PHASE phase) {
        if (phase == TEST_PHASE.PHASE_1) {
            return isValidForPhase1();
        } else {
            return isValidForPhase2();
        }
    }

    public AssignmentStatus getStatusForPhase(TEST_PHASE phase) {
        if (phase == TEST_PHASE.PHASE_1) {
            return phase1Status;
        } else {
            return phase2Status;
        }
    }

    public String getStatusDisplayForPhase(TEST_PHASE phase) {
        AssignmentStatus status = getStatusForPhase(phase);
        if (status == AssignmentStatus.VALID) {
            return "JA";
        } else if (status == AssignmentStatus.INVALID) {
            return "NEIN";
        } else if (status == AssignmentStatus.SKIPPED) {
            return "SKIP";
        } else {
            return "ERR";
        }
    }

    public boolean hasAnyValidAssignment() {
        return isValidForPhase1() || isValidForPhase2();
    }

    @Override
    public String toString() {
        return String.format("%s/%s/%s (Crefo: %d) - PH1: %s, PH2: %s",
                customerKey, scenarioName, testFallName, crefoNr,
                getStatusDisplayForPhase(TEST_PHASE.PHASE_1),
                getStatusDisplayForPhase(TEST_PHASE.PHASE_2));
    }
}
