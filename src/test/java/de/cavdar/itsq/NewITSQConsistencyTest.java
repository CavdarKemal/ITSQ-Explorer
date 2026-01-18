package de.cavdar.itsq;

import org.junit.jupiter.api.DisplayName;

import java.util.List;

/**
 * Konsistenztest fuer ITSQ-Testdaten (NEW Struktur).
 *
 * NEW Struktur:
 * - ARCHIV-BESTAND/PHASE-1, ARCHIV-BESTAND/PHASE-2
 * - REF-EXPORTS/PHASE-1 (c01, c02), REF-EXPORTS/PHASE-2 (c01-c05)
 * - Phasenspezifische REF-EXPORTS!
 *
 * Testdaten liegen unter: src/test/resources/ITSQ/NEW/
 */
@DisplayName("ITSQ Konsistenz Tests (NEW)")
public class NewITSQConsistencyTest extends ITSQConsistencyTestBase {

    private static final String BASE_PATH = "/ITSQ/NEW";

    private static final String ARCHIV_BESTAND = "ARCHIV-BESTAND";
    private static final String REF_EXPORTS = "REF-EXPORTS";
    private static final String PHASE_1 = "PHASE-1";
    private static final String PHASE_2 = "PHASE-2";

    private static final String ARCHIV_BESTAND_ROOT = BASE_PATH + "/" + ARCHIV_BESTAND;
    private static final String REF_EXPORTS_ROOT = BASE_PATH + "/" + REF_EXPORTS;

    // PHASE-1 hat nur c01, c02
    private static final List<String> CUSTOMERS_PHASE1 = List.of("c01", "c02");
    // PHASE-2 hat c01-c05
    private static final List<String> CUSTOMERS_PHASE2 = List.of("c01", "c02", "c03", "c04", "c05");

    @Override
    protected String getArchivBestandPath(TestSupportClientKonstanten.TEST_PHASE phase) {
        return switch (phase) {
            case PHASE_1 -> ARCHIV_BESTAND_ROOT + "/" + PHASE_1;
            case PHASE_2 -> ARCHIV_BESTAND_ROOT + "/" + PHASE_2;
        };
    }

    @Override
    protected String getRefExportsPath(TestSupportClientKonstanten.TEST_PHASE phase) {
        return switch (phase) {
            case PHASE_1 -> REF_EXPORTS_ROOT + "/" + PHASE_1;
            case PHASE_2 -> REF_EXPORTS_ROOT + "/" + PHASE_2;
        };
    }

    @Override
    protected List<String> getCustomersForPhase(TestSupportClientKonstanten.TEST_PHASE phase) {
        return switch (phase) {
            case PHASE_1 -> CUSTOMERS_PHASE1;
            case PHASE_2 -> CUSTOMERS_PHASE2;
        };
    }

    @Override
    protected String getStructureName() {
        return "NEW";
    }
}
