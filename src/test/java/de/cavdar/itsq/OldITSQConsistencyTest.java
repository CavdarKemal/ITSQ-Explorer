package de.cavdar.itsq;

import org.junit.jupiter.api.DisplayName;

import java.util.List;

/**
 * Konsistenztest fuer ITSQ-Testdaten (OLD Struktur).
 *
 * OLD Struktur:
 * - ARCHIV-BESTAND-PH1, ARCHIV-BESTAND-PH2 (separate Verzeichnisse)
 * - REF-EXPORTS (gemeinsam fuer beide Phasen, c01-c05)
 *
 * Testdaten liegen unter: src/test/resources/ITSQ/OLD/
 */
@DisplayName("ITSQ Konsistenz Tests (OLD)")
public class OldITSQConsistencyTest extends ITSQConsistencyTestBase {

    private static final String BASE_PATH = "/ITSQ/OLD";

    private static final String ARCHIV_BESTAND = "ARCHIV-BESTAND";
    private static final String REF_EXPORTS = "REF-EXPORTS";
    private static final String PHASE_1 = "PH1";
    private static final String PHASE_2 = "PH2";

    private static final String ARCHIV_BESTAND_PH1_ROOT = BASE_PATH + "/" + ARCHIV_BESTAND + "-" + PHASE_1;
    private static final String ARCHIV_BESTAND_PH2_ROOT = BASE_PATH + "/" + ARCHIV_BESTAND + "-" + PHASE_2;

    private static final String REF_EXPORTS_ROOT = BASE_PATH + "/" + REF_EXPORTS;

    // OLD Struktur hat c01-c06 fuer beide Phasen (gemeinsame REF-EXPORTS)
    private static final List<String> CUSTOMERS = List.of("c01", "c02", "c03", "c04", "c05", "c06", "c07");

    @Override
    protected String getArchivBestandPath(TestSupportClientKonstanten.TEST_PHASE phase) {
        return switch (phase) {
            case PHASE_1 -> ARCHIV_BESTAND_PH1_ROOT;
            case PHASE_2 -> ARCHIV_BESTAND_PH2_ROOT;
        };
    }

    @Override
    protected String getRefExportsPath(TestSupportClientKonstanten.TEST_PHASE phase) {
        // OLD Struktur: REF-EXPORTS ist fuer beide Phasen gleich
        return REF_EXPORTS_ROOT;
    }

    @Override
    protected List<String> getCustomersForPhase(TestSupportClientKonstanten.TEST_PHASE phase) {
        // OLD Struktur: Alle Kunden fuer beide Phasen
        return CUSTOMERS;
    }

    @Override
    protected String getStructureName() {
        return "OLD";
    }
}
