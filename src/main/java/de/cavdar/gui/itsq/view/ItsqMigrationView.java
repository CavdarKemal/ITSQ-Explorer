package de.cavdar.gui.itsq.view;

import de.cavdar.gui.itsq.design.ItsqMigrationPanel;
import de.cavdar.gui.model.base.AppConfig;
import de.cavdar.gui.util.TimelineLogger;
import de.cavdar.itsq.TestSupportClientKonstanten.TEST_PHASE;
import de.cavdar.itsq.migration.model.MigrationConfig;
import de.cavdar.itsq.migration.model.MigrationProblem;
import de.cavdar.itsq.migration.model.MigrationResult;
import de.cavdar.itsq.migration.model.TestCasePhaseAssignment;
import de.cavdar.itsq.migration.service.MigrationService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * View fuer ITSQ-Migration von OLD nach NEW Struktur.
 * Erweitert ItsqMigrationPanel und fuegt Verhalten hinzu.
 *
 * @author kemal
 */
public class ItsqMigrationView extends ItsqMigrationPanel {

    private static final String SOURCE_PATH_KEY = "migration.source.path";
    private static final String TARGET_PATH_KEY = "migration.target.path";

    private final AppConfig cfg;
    private MigrationService migrationService;
    private MigrationConfig migrationConfig;
    private SwingWorker<MigrationResult, String> currentWorker;

    public ItsqMigrationView(AppConfig config) {
        super();
        this.cfg = config;
        this.migrationService = new MigrationService();

        initFromConfig();
        setupListeners();

        TimelineLogger.debug(ItsqMigrationView.class, "ItsqMigrationView created");
    }

    // ===== Initialisierung =====

    private void initFromConfig() {
        String sourcePath = cfg.getProperty(SOURCE_PATH_KEY);
        if (sourcePath != null && !sourcePath.isEmpty()) {
            getTextFieldSource().setText(sourcePath);
        }

        String targetPath = cfg.getProperty(TARGET_PATH_KEY);
        if (targetPath != null && !targetPath.isEmpty()) {
            getTextFieldTarget().setText(targetPath);
        }
    }

    private void setupListeners() {
        // Quellverzeichnis-Button
        getButtonBrowseSource().addActionListener(e -> browseSourcePath());

        // Zielverzeichnis-Button
        getButtonBrowseTarget().addActionListener(e -> browseTargetPath());

        // Vorschau-Button
        getButtonPreview().addActionListener(e -> runPreview());

        // Migrations-Button
        getButtonMigrate().addActionListener(e -> runMigration());

        // Abbrechen-Button
        getButtonCancel().addActionListener(e -> cancelOperation());
    }

    // ===== Pfad-Auswahl =====

    private void browseSourcePath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("OLD-Quellverzeichnis waehlen");

        String currentPath = getTextFieldSource().getText();
        if (!currentPath.isEmpty()) {
            File current = new File(currentPath);
            if (current.exists()) {
                chooser.setCurrentDirectory(current.getParentFile());
            }
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            getTextFieldSource().setText(selected.getAbsolutePath());
            cfg.setProperty(SOURCE_PATH_KEY, selected.getAbsolutePath());
            cfg.save();
        }
    }

    private void browseTargetPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("NEW-Zielverzeichnis waehlen");

        String currentPath = getTextFieldTarget().getText();
        if (!currentPath.isEmpty()) {
            File current = new File(currentPath);
            if (current.exists()) {
                chooser.setCurrentDirectory(current.getParentFile());
            }
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            getTextFieldTarget().setText(selected.getAbsolutePath());
            cfg.setProperty(TARGET_PATH_KEY, selected.getAbsolutePath());
            cfg.save();
        }
    }

    // ===== Vorschau =====

    private void runPreview() {
        String sourcePath = getTextFieldSource().getText().trim();
        String targetPath = getTextFieldTarget().getText().trim();

        if (sourcePath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Bitte Quellverzeichnis (OLD) angeben.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (targetPath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Bitte Zielverzeichnis (NEW) angeben.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File sourceDir = new File(sourcePath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            JOptionPane.showMessageDialog(this,
                    "Quellverzeichnis existiert nicht: " + sourcePath,
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Erstelle Konfiguration
        migrationConfig = new MigrationConfig(sourceDir, new File(targetPath));
        migrationConfig.setCreateBackup(getCheckBoxBackup().isSelected());
        migrationConfig.setOverwriteExisting(getCheckBoxOverwrite().isSelected());

        // Service zuruecksetzen
        migrationService = new MigrationService();

        // Vorschau im Hintergrund ausfuehren
        setUIEnabled(false);
        getProgressBar().setIndeterminate(true);
        getButtonCancel().setEnabled(true);
        getLabelStatus().setText("Analysiere...");

        currentWorker = new SwingWorker<>() {
            @Override
            protected MigrationResult doInBackground() throws Exception {
                migrationService.setProgressCallback(this::publish);
                migrationService.setProgressPercentCallback(percent ->
                        SwingUtilities.invokeLater(() -> {
                            getProgressBar().setIndeterminate(false);
                            getProgressBar().setValue(percent);
                        }));
                return migrationService.analyze(migrationConfig);
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    getLabelStatus().setText(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                try {
                    MigrationResult result = get();
                    updatePreview(result);
                    getButtonMigrate().setEnabled(true);
                    getLabelStatus().setText("Analyse abgeschlossen.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    getLabelStatus().setText("Abgebrochen.");
                } catch (ExecutionException e) {
                    TimelineLogger.error(ItsqMigrationView.class, "Preview failed", e);
                    JOptionPane.showMessageDialog(ItsqMigrationView.this,
                            "Fehler bei der Analyse:\n" + e.getCause().getMessage(),
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    getLabelStatus().setText("Fehler: " + e.getCause().getMessage());
                } finally {
                    getProgressBar().setIndeterminate(false);
                    getProgressBar().setValue(0);
                    getButtonCancel().setEnabled(false);
                    setUIEnabled(true);
                    currentWorker = null;
                }
            }
        };
        currentWorker.execute();
    }

    private void updatePreview(MigrationResult result) {
        // Aktualisiere Zusammenfassungstext
        getTextAreaSummary().setText(migrationService.getPreviewSummary());

        // Aktualisiere Detailtabelle
        updateDetailsTable(result.getAllAssignments());
    }

    private void updateDetailsTable(List<TestCasePhaseAssignment> assignments) {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Kunde", "Szenario", "Test", "Crefo", "PH1", "PH2", "Status"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (TestCasePhaseAssignment a : assignments) {
            String status;
            if (a.isValidForPhase1() && a.isValidForPhase2()) {
                status = "OK";
            } else if (a.isValidForPhase1() || a.isValidForPhase2()) {
                status = "TEILWEISE";
            } else {
                status = "UNGUELTIG";
            }

            model.addRow(new Object[]{
                    a.getCustomerKey(),
                    a.getScenarioName(),
                    a.getTestFallName(),
                    a.getCrefoNr(),
                    a.getStatusDisplayForPhase(TEST_PHASE.PHASE_1),
                    a.getStatusDisplayForPhase(TEST_PHASE.PHASE_2),
                    status
            });
        }

        getTableDetails().setModel(model);

        // Spaltenbreiten setzen
        getTableDetails().getColumnModel().getColumn(0).setPreferredWidth(60);  // Kunde
        getTableDetails().getColumnModel().getColumn(1).setPreferredWidth(120); // Szenario
        getTableDetails().getColumnModel().getColumn(2).setPreferredWidth(50);  // Test
        getTableDetails().getColumnModel().getColumn(3).setPreferredWidth(100); // Crefo
        getTableDetails().getColumnModel().getColumn(4).setPreferredWidth(50);  // PH1
        getTableDetails().getColumnModel().getColumn(5).setPreferredWidth(50);  // PH2
        getTableDetails().getColumnModel().getColumn(6).setPreferredWidth(80);  // Status
    }

    // ===== Migration =====

    private void runMigration() {
        if (migrationConfig == null) {
            JOptionPane.showMessageDialog(this,
                    "Bitte zuerst eine Vorschau erstellen.",
                    "Hinweis", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Bestaetigung
        int confirm = JOptionPane.showConfirmDialog(this,
                "Migration starten?\n\n" +
                        "Quelle: " + migrationConfig.getSourceOldPath() + "\n" +
                        "Ziel: " + migrationConfig.getTargetNewPath() + "\n\n" +
                        (getCheckBoxBackup().isSelected() ? "Es wird ein Backup erstellt." : "KEIN Backup!"),
                "Migration bestaetigen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Aktualisiere Konfiguration aus Checkboxen
        migrationConfig.setCreateBackup(getCheckBoxBackup().isSelected());
        migrationConfig.setOverwriteExisting(getCheckBoxOverwrite().isSelected());

        // Migration im Hintergrund ausfuehren
        setUIEnabled(false);
        getProgressBar().setIndeterminate(false);
        getProgressBar().setValue(0);
        getButtonCancel().setEnabled(true);
        getLabelStatus().setText("Migration laeuft...");

        currentWorker = new SwingWorker<>() {
            @Override
            protected MigrationResult doInBackground() throws Exception {
                migrationService.setProgressCallback(this::publish);
                migrationService.setProgressPercentCallback(percent ->
                        SwingUtilities.invokeLater(() -> getProgressBar().setValue(percent)));
                return migrationService.migrate(migrationConfig);
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    getLabelStatus().setText(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                try {
                    MigrationResult result = get();
                    showMigrationResult(result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    getLabelStatus().setText("Abgebrochen.");
                } catch (ExecutionException e) {
                    TimelineLogger.error(ItsqMigrationView.class, "Migration failed", e);
                    JOptionPane.showMessageDialog(ItsqMigrationView.this,
                            "Fehler bei der Migration:\n" + e.getCause().getMessage(),
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    getLabelStatus().setText("Fehler: " + e.getCause().getMessage());
                } finally {
                    getButtonCancel().setEnabled(false);
                    setUIEnabled(true);
                    currentWorker = null;
                }
            }
        };
        currentWorker.execute();
    }

    private void showMigrationResult(MigrationResult result) {
        String message;
        int messageType;

        if (result.isSuccess()) {
            message = "Migration erfolgreich abgeschlossen!\n\n" + result.getSummary();
            messageType = JOptionPane.INFORMATION_MESSAGE;
            getLabelStatus().setText("Migration erfolgreich.");
        } else {
            message = "Migration mit Problemen abgeschlossen.\n\n" + result.getSummary();
            messageType = JOptionPane.WARNING_MESSAGE;
            getLabelStatus().setText("Migration mit Warnungen abgeschlossen.");
        }

        // Zeige Probleme falls vorhanden
        List<MigrationProblem> problems = result.getProblems();
        if (!problems.isEmpty()) {
            StringBuilder sb = new StringBuilder(message);
            sb.append("\n\nProbleme:\n");
            int count = 0;
            for (MigrationProblem p : problems) {
                sb.append("- ").append(p.toString()).append("\n");
                count++;
                if (count >= 10) {
                    sb.append("... und ").append(problems.size() - 10).append(" weitere\n");
                    break;
                }
            }
            message = sb.toString();
        }

        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(this, scrollPane,
                "Migrationsergebnis", messageType);

        // Aktualisiere Vorschau mit Endergebnis
        updatePreview(result);
    }

    // ===== Abbrechen =====

    private void cancelOperation() {
        if (currentWorker != null && !currentWorker.isDone()) {
            migrationService.cancel();
            currentWorker.cancel(true);
            getLabelStatus().setText("Abbruch angefordert...");
        }
    }

    // ===== UI-Zustand =====

    private void setUIEnabled(boolean enabled) {
        getTextFieldSource().setEnabled(enabled);
        getTextFieldTarget().setEnabled(enabled);
        getButtonBrowseSource().setEnabled(enabled);
        getButtonBrowseTarget().setEnabled(enabled);
        getButtonPreview().setEnabled(enabled);
        getButtonMigrate().setEnabled(enabled && migrationConfig != null);
        getCheckBoxBackup().setEnabled(enabled);
        getCheckBoxOverwrite().setEnabled(enabled);
    }

    /**
     * Setzt Standard-Pfade fuer Tests.
     */
    public void setDefaultPaths(File sourceDir, File targetDir) {
        if (sourceDir != null) {
            getTextFieldSource().setText(sourceDir.getAbsolutePath());
        }
        if (targetDir != null) {
            getTextFieldTarget().setText(targetDir.getAbsolutePath());
        }
    }

    /**
     * Gibt den aktuellen Migrations-Service zurueck.
     */
    public MigrationService getMigrationService() {
        return migrationService;
    }
}
