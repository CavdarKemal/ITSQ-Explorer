package de.cavdar.itsq.migration.service;

import de.cavdar.itsq.TestSupportClientKonstanten.TEST_PHASE;
import de.cavdar.itsq.migration.model.MigrationProblem;
import de.cavdar.itsq.migration.model.MigrationResult;
import de.cavdar.itsq.migration.model.TestCasePhaseAssignment;
import de.cavdar.itsq.migration.model.TestCasePhaseAssignment.AssignmentStatus;
import de.cavdar.itsq.migration.model.TestCasePhaseAssignment.TestCaseType;

import java.io.File;
import java.util.*;

/**
 * Berechnet Phasenzuordnungen fuer Testfaelle basierend auf ARCHIV-BESTAND Verfuegbarkeit.
 *
 * Logik pro Testfall-Typ:
 * - p0x (positiv): ARCHIV-BESTAND/{crefo}.xml MUSS existieren
 * - x0x (loeschsatz): ARCHIV-BESTAND/{crefo}.xml MUSS existieren
 * - n0x (negativ): ARCHIV-BESTAND/{crefo}.xml DARF NICHT existieren
 */
public class PhaseAssignmentCalculator {

    private final OldStructureAnalyzer analyzer;
    private final MigrationResult result;
    private final Map<MigrationProblem.ProblemType, MigrationProblem.Resolution> rememberedDecisions;

    public PhaseAssignmentCalculator(OldStructureAnalyzer analyzer, MigrationResult result) {
        this.analyzer = analyzer;
        this.result = result;
        this.rememberedDecisions = new EnumMap<>(MigrationProblem.ProblemType.class);
    }

    /**
     * Berechnet Phasenzuordnungen fuer alle Testfaelle.
     * @return Map von Kundenschluesseln zu Menge der Phasen, zu denen sie gehoeren
     */
    public Map<String, Set<TEST_PHASE>> calculateAssignments() {
        Map<String, Set<TEST_PHASE>> customerPhases = new TreeMap<>();

        // Zaehler fuer Statistiken
        int phase1TestCases = 0;
        int phase2TestCases = 0;
        Set<String> phase1Scenarios = new HashSet<>();
        Set<String> phase2Scenarios = new HashSet<>();

        for (TestCasePhaseAssignment assignment : analyzer.getAllAssignments()) {
            calculateAssignmentForTestCase(assignment);
            result.addAssignment(assignment);

            // Verfolge Kundenphasen
            String customerKey = assignment.getCustomerKey();
            customerPhases.computeIfAbsent(customerKey, k -> new TreeSet<>(Comparator.comparing(TEST_PHASE::getDirName)));

            if (assignment.isValidForPhase1()) {
                customerPhases.get(customerKey).add(TEST_PHASE.PHASE_1);
                result.addCustomerToPhase(customerKey, TEST_PHASE.PHASE_1);
                phase1TestCases++;
                phase1Scenarios.add(customerKey + "/" + assignment.getScenarioName());
            }
            if (assignment.isValidForPhase2()) {
                customerPhases.get(customerKey).add(TEST_PHASE.PHASE_2);
                result.addCustomerToPhase(customerKey, TEST_PHASE.PHASE_2);
                phase2TestCases++;
                phase2Scenarios.add(customerKey + "/" + assignment.getScenarioName());
            }
        }

        // Aktualisiere Statistiken
        result.setTotalCustomers(analyzer.getTotalCustomers());
        result.setCustomersPhase1((int) customerPhases.values().stream()
                .filter(phases -> phases.contains(TEST_PHASE.PHASE_1)).count());
        result.setCustomersPhase2((int) customerPhases.values().stream()
                .filter(phases -> phases.contains(TEST_PHASE.PHASE_2)).count());

        result.setTotalScenarios(analyzer.getTotalScenarios());
        result.setScenariosPhase1(phase1Scenarios.size());
        result.setScenariosPhase2(phase2Scenarios.size());

        result.setTotalTestCases(analyzer.getTotalTestCases());
        result.setTestCasesPhase1(phase1TestCases);
        result.setTestCasesPhase2(phase2TestCases);

        return customerPhases;
    }

    /**
     * Berechnet Phasenzuordnung fuer einen einzelnen Testfall.
     */
    private void calculateAssignmentForTestCase(TestCasePhaseAssignment assignment) {
        Long crefoNr = assignment.getCrefoNr();
        TestCaseType type = assignment.getTestCaseType();

        if (type == null) {
            assignment.setPhase1Status(AssignmentStatus.ERROR);
            assignment.setPhase1StatusReason("Unbekannter Testfall-Typ: " + assignment.getTestFallName());
            assignment.setPhase2Status(AssignmentStatus.ERROR);
            assignment.setPhase2StatusReason("Unbekannter Testfall-Typ: " + assignment.getTestFallName());
            return;
        }

        // Berechne Phase 1 Zuordnung
        calculatePhaseAssignment(assignment, TEST_PHASE.PHASE_1, type, crefoNr);

        // Berechne Phase 2 Zuordnung
        calculatePhaseAssignment(assignment, TEST_PHASE.PHASE_2, type, crefoNr);
    }

    /**
     * Berechnet Zuordnung fuer eine bestimmte Phase.
     */
    private void calculatePhaseAssignment(TestCasePhaseAssignment assignment, TEST_PHASE phase,
                                          TestCaseType type, Long crefoNr) {
        boolean hasArchivXml;
        File archivXml;

        if (phase == TEST_PHASE.PHASE_1) {
            hasArchivXml = analyzer.hasCrefoInPhase1(crefoNr);
            archivXml = analyzer.getArchivBestandXmlPhase1(crefoNr);
        } else {
            hasArchivXml = analyzer.hasCrefoInPhase2(crefoNr);
            archivXml = analyzer.getArchivBestandXmlPhase2(crefoNr);
        }

        boolean isValid;
        String reason;

        switch (type) {
            case POSITIVE:
            case LOESCHSATZ:
                // Fuer p0x und x0x: ARCHIV-BESTAND XML MUSS existieren
                if (hasArchivXml) {
                    isValid = true;
                    reason = "ARCHIV-BESTAND XML vorhanden";
                } else {
                    isValid = false;
                    reason = "ARCHIV-BESTAND XML fehlt fuer " + phase.getDirName();

                    // Erstelle Problem fuer fehlende XML
                    MigrationProblem problem = new MigrationProblem(
                            MigrationProblem.ProblemType.MISSING_ARCHIV_BESTAND_XML,
                            assignment.getCustomerKey(),
                            assignment.getScenarioName(),
                            assignment.getTestFallName(),
                            crefoNr,
                            reason,
                            getArchivPath(phase, crefoNr)
                    );
                    result.addProblem(problem);
                }
                break;

            case NEGATIVE:
                // Fuer n0x: ARCHIV-BESTAND XML DARF NICHT existieren
                if (!hasArchivXml) {
                    isValid = true;
                    reason = "ARCHIV-BESTAND XML nicht vorhanden (korrekt fuer negativ-Testfall)";
                } else {
                    isValid = false;
                    reason = "ARCHIV-BESTAND XML existiert, obwohl es ein negativ-Testfall ist";

                    // Erstelle Problem fuer unerwartete XML
                    MigrationProblem problem = new MigrationProblem(
                            MigrationProblem.ProblemType.UNEXPECTED_ARCHIV_BESTAND_XML,
                            assignment.getCustomerKey(),
                            assignment.getScenarioName(),
                            assignment.getTestFallName(),
                            crefoNr,
                            reason,
                            getArchivPath(phase, crefoNr)
                    );
                    result.addProblem(problem);
                }
                break;

            default:
                isValid = false;
                reason = "Unbekannter Testfall-Typ";
        }

        if (phase == TEST_PHASE.PHASE_1) {
            assignment.setPhase1Status(isValid ? AssignmentStatus.VALID : AssignmentStatus.INVALID);
            assignment.setPhase1StatusReason(reason);
            if (archivXml != null) {
                assignment.setPhase1ArchivXml(archivXml);
            }
        } else {
            assignment.setPhase2Status(isValid ? AssignmentStatus.VALID : AssignmentStatus.INVALID);
            assignment.setPhase2StatusReason(reason);
            if (archivXml != null) {
                assignment.setPhase2ArchivXml(archivXml);
            }
        }
    }

    private String getArchivPath(TEST_PHASE phase, Long crefoNr) {
        String phaseDir = phase == TEST_PHASE.PHASE_1 ? "ARCHIV-BESTAND-PH1" : "ARCHIV-BESTAND-PH2";
        return phaseDir + "/" + crefoNr + ".xml";
    }

    /**
     * Gibt Kunden zurueck, die mindestens einen gueltigen Testfall fuer die gegebene Phase haben.
     */
    public Set<String> getCustomersForPhase(TEST_PHASE phase) {
        Set<String> customers = new TreeSet<>();
        for (TestCasePhaseAssignment assignment : analyzer.getAllAssignments()) {
            if (assignment.isValidForPhase(phase)) {
                customers.add(assignment.getCustomerKey());
            }
        }
        return customers;
    }

    /**
     * Gibt Szenarien fuer einen Kunden zurueck, die mindestens einen gueltigen Testfall fuer die gegebene Phase haben.
     */
    public Set<String> getScenariosForCustomerAndPhase(String customerKey, TEST_PHASE phase) {
        Set<String> scenarios = new TreeSet<>();
        for (TestCasePhaseAssignment assignment : analyzer.getAssignmentsForCustomer(customerKey)) {
            if (assignment.isValidForPhase(phase)) {
                scenarios.add(assignment.getScenarioName());
            }
        }
        return scenarios;
    }

    /**
     * Gibt gueltige Testfaelle fuer eine Kunde/Szenario/Phase-Kombination zurueck.
     */
    public List<TestCasePhaseAssignment> getValidTestCases(String customerKey, String scenarioName,
                                                           TEST_PHASE phase) {
        List<TestCasePhaseAssignment> validCases = new ArrayList<>();
        for (TestCasePhaseAssignment assignment : analyzer.getAssignmentsForCustomer(customerKey)) {
            if (assignment.getScenarioName().equals(scenarioName) && assignment.isValidForPhase(phase)) {
                validCases.add(assignment);
            }
        }
        return validCases;
    }

    /**
     * Prueft, ob ein Kunde gueltige Testfaelle fuer die gegebene Phase hat.
     */
    public boolean hasValidTestCasesForPhase(String customerKey, TEST_PHASE phase) {
        return analyzer.getAssignmentsForCustomer(customerKey).stream()
                .anyMatch(a -> a.isValidForPhase(phase));
    }

    /**
     * Speichert eine gemerkte Entscheidung fuer einen Problemtyp.
     */
    public void rememberDecision(MigrationProblem.ProblemType type, MigrationProblem.Resolution resolution) {
        rememberedDecisions.put(type, resolution);
    }

    /**
     * Gibt eine gemerkte Entscheidung fuer einen Problemtyp zurueck, falls vorhanden.
     */
    public MigrationProblem.Resolution getRememberedDecision(MigrationProblem.ProblemType type) {
        return rememberedDecisions.get(type);
    }

    /**
     * Loescht alle gemerkten Entscheidungen.
     */
    public void clearRememberedDecisions() {
        rememberedDecisions.clear();
    }
}
