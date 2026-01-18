package de.cavdar.itsq.migration.service;

import de.cavdar.itsq.TestSupportClientKonstanten.TEST_PHASE;
import de.cavdar.itsq.migration.model.MigrationConfig;
import de.cavdar.itsq.migration.model.MigrationResult;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Erstellt die NEW-Verzeichnisstruktur fuer die migrierten ITSQ-Daten.
 */
public class NewStructureBuilder {

    private final MigrationConfig config;
    private final MigrationResult result;
    private final PhaseAssignmentCalculator calculator;

    public NewStructureBuilder(MigrationConfig config, MigrationResult result,
                               PhaseAssignmentCalculator calculator) {
        this.config = config;
        this.result = result;
        this.calculator = calculator;
    }

    /**
     * Erstellt die komplette NEW-Verzeichnisstruktur.
     * @param customerPhases Map von Kunden zu ihren gueltigen Phasen
     */
    public void createStructure(Map<String, Set<TEST_PHASE>> customerPhases) throws IOException {
        // Erstelle NEW-Stammverzeichnis falls nicht vorhanden
        File newRoot = config.getTargetNewPath();
        if (!newRoot.exists()) {
            if (!newRoot.mkdirs()) {
                throw new IOException("Konnte Zielverzeichnis nicht erstellen: " + newRoot.getAbsolutePath());
            }
        }

        // Erstelle ARCHIV-BESTAND Struktur
        createArchivBestandStructure();

        // Erstelle REF-EXPORTS Struktur
        createRefExportsStructure(customerPhases);
    }

    /**
     * Erstellt die ARCHIV-BESTAND Verzeichnisstruktur.
     */
    private void createArchivBestandStructure() throws IOException {
        File archivBestandDir = config.getNewArchivBestandDir();
        createDirectory(archivBestandDir);

        // Erstelle PHASE-1 und PHASE-2 Verzeichnisse
        File phase1Dir = config.getNewArchivBestandPhase1Dir();
        File phase2Dir = config.getNewArchivBestandPhase2Dir();

        createDirectory(phase1Dir);
        createDirectory(phase2Dir);

        result.addInfo("ARCHIV-BESTAND Struktur erstellt");
    }

    /**
     * Erstellt die REF-EXPORTS Verzeichnisstruktur.
     * @param customerPhases Map von Kunden zu ihren gueltigen Phasen
     */
    private void createRefExportsStructure(Map<String, Set<TEST_PHASE>> customerPhases) throws IOException {
        File refExportsDir = config.getNewRefExportsDir();
        createDirectory(refExportsDir);

        // Erstelle PHASE-1 und PHASE-2 Verzeichnisse
        File phase1Dir = config.getNewRefExportsPhase1Dir();
        File phase2Dir = config.getNewRefExportsPhase2Dir();

        createDirectory(phase1Dir);
        createDirectory(phase2Dir);

        // Erstelle Kundenverzeichnisse innerhalb jeder Phase
        for (Map.Entry<String, Set<TEST_PHASE>> entry : customerPhases.entrySet()) {
            String customerKey = entry.getKey();
            Set<TEST_PHASE> phases = entry.getValue();

            for (TEST_PHASE phase : phases) {
                createCustomerStructure(customerKey, phase);
            }
        }

        result.addInfo("REF-EXPORTS Struktur erstellt");
    }

    /**
     * Erstellt die Verzeichnisstruktur fuer einen Kunden in einer bestimmten Phase.
     */
    private void createCustomerStructure(String customerKey, TEST_PHASE phase) throws IOException {
        File phaseDir = phase == TEST_PHASE.PHASE_1
                ? config.getNewRefExportsPhase1Dir()
                : config.getNewRefExportsPhase2Dir();

        File customerDir = new File(phaseDir, customerKey);
        createDirectory(customerDir);

        // Erstelle Szenario-Verzeichnisse
        Set<String> scenarios = calculator.getScenariosForCustomerAndPhase(customerKey, phase);
        for (String scenarioName : scenarios) {
            File scenarioDir = new File(customerDir, scenarioName);
            createDirectory(scenarioDir);
        }
    }

    /**
     * Erstellt ein Verzeichnis falls es nicht existiert.
     */
    private void createDirectory(File dir) throws IOException {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Konnte Verzeichnis nicht erstellen: " + dir.getAbsolutePath());
            }
            result.incrementFilesCreated();
        }
    }

    /**
     * Gibt das Zielverzeichnis fuer ARCHIV-BESTAND XMLs fuer eine Phase zurueck.
     */
    public File getArchivBestandTargetDir(TEST_PHASE phase) {
        return phase == TEST_PHASE.PHASE_1
                ? config.getNewArchivBestandPhase1Dir()
                : config.getNewArchivBestandPhase2Dir();
    }

    /**
     * Gibt das Zielverzeichnis fuer ein Kundenszenario in einer Phase zurueck.
     */
    public File getScenarioTargetDir(String customerKey, String scenarioName, TEST_PHASE phase) {
        File phaseDir = phase == TEST_PHASE.PHASE_1
                ? config.getNewRefExportsPhase1Dir()
                : config.getNewRefExportsPhase2Dir();
        return new File(new File(phaseDir, customerKey), scenarioName);
    }

    /**
     * Gibt das Ziel-Kundenverzeichnis fuer eine Phase zurueck.
     */
    public File getCustomerTargetDir(String customerKey, TEST_PHASE phase) {
        File phaseDir = phase == TEST_PHASE.PHASE_1
                ? config.getNewRefExportsPhase1Dir()
                : config.getNewRefExportsPhase2Dir();
        return new File(phaseDir, customerKey);
    }
}
