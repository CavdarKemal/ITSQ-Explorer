package de.cavdar.itsq.migration.model;

/**
 * Repraesentiert ein Problem waehrend der Migration mit moeglichen Loesungsoptionen.
 */
public class MigrationProblem {

    public enum ProblemType {
        MISSING_ARCHIV_BESTAND_XML("Fehlende ARCHIV-BESTAND XML"),
        UNEXPECTED_ARCHIV_BESTAND_XML("Unerwartete ARCHIV-BESTAND XML (negativ-Testfall)"),
        MISSING_REF_EXPORT_XML("Fehlende REF-EXPORT XML"),
        INVALID_RELEVANZ_ENTRY("Ungueltige Relevanz.properties Zeile"),
        CUSTOMER_NO_VALID_TESTCASES("Kunde hat keine gueltigen Testfaelle"),
        FILE_COPY_ERROR("Fehler beim Kopieren"),
        OTHER("Sonstiger Fehler");

        private final String displayName;

        ProblemType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Resolution {
        SKIP("Ueberspringen"),
        COPY_ANYWAY("Trotzdem kopieren"),
        ABORT("Abbrechen");

        private final String displayName;

        Resolution(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final ProblemType type;
    private final String customerKey;
    private final String scenarioName;
    private final String testFallName;
    private final Long crefoNr;
    private final String details;
    private final String filePath;
    private Resolution resolution;
    private boolean rememberDecision;

    public MigrationProblem(ProblemType type, String customerKey, String scenarioName,
                            String testFallName, Long crefoNr, String details, String filePath) {
        this.type = type;
        this.customerKey = customerKey;
        this.scenarioName = scenarioName;
        this.testFallName = testFallName;
        this.crefoNr = crefoNr;
        this.details = details;
        this.filePath = filePath;
        this.resolution = null;
        this.rememberDecision = false;
    }

    public ProblemType getType() {
        return type;
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

    public String getDetails() {
        return details;
    }

    public String getFilePath() {
        return filePath;
    }

    public Resolution getResolution() {
        return resolution;
    }

    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
    }

    public boolean isRememberDecision() {
        return rememberDecision;
    }

    public void setRememberDecision(boolean rememberDecision) {
        this.rememberDecision = rememberDecision;
    }

    public String getLocationString() {
        StringBuilder sb = new StringBuilder();
        if (customerKey != null) {
            sb.append(customerKey);
        }
        if (scenarioName != null) {
            if (sb.length() > 0) sb.append("/");
            sb.append(scenarioName);
        }
        if (testFallName != null) {
            if (sb.length() > 0) sb.append("/");
            sb.append(testFallName);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - Crefo: %d - %s",
                type.getDisplayName(), getLocationString(), crefoNr, details);
    }
}
