package de.cavdar.gui.itsq.view;

import de.cavdar.gui.itsq.design.ItsqRefExportPropertiesEditorPanel;
import de.cavdar.gui.itsq.model.ItsqItem;
import de.cavdar.gui.util.TimelineLogger;
import de.cavdar.itsq.TestCrefo;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * View for editing Relevanz.properties files under REF-EXPORTS.
 * Displays TestCrefo entries in a table with columns:
 * Testname, Crefonummer, Info, Export, REF-Export-Datei
 */
public class ItsqRefExportPropertiesEditorView extends ItsqRefExportPropertiesEditorPanel implements ItsqItemSelectable {

    private ItsqItem selectedItem;
    private TestCrefoTableModel tableModel;
    private DefaultComboBoxModel<String> filterHistoryModel;
    private JLabel statusLabel;

    private boolean modified = false;
    private static final int MAX_FILTER_HISTORY = 20;

    public ItsqRefExportPropertiesEditorView() {
        super();
        initTableModel();
        setupToolbar();
        setupTableSelection();
        setupKeyboardShortcuts();
    }

    /**
     * Initializes the table model and configures the table.
     */
    private void initTableModel() {
        tableModel = new TestCrefoTableModel();
        JTable table = getTableProperties();
        table.setModel(tableModel);
        table.setFont(new Font("Consolas", Font.PLAIN, 13));
        table.setRowHeight(22);

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(100);  // Testname
        table.getColumnModel().getColumn(1).setPreferredWidth(120);  // Crefonummer
        table.getColumnModel().getColumn(2).setPreferredWidth(200);  // Info
        table.getColumnModel().getColumn(3).setPreferredWidth(60);   // Export
        table.getColumnModel().getColumn(4).setPreferredWidth(250);  // REF-Export-Datei

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Double-click to edit
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedEntry();
                }
            }
        });
    }

    /**
     * Sets up the toolbar buttons with their action listeners.
     */
    private void setupToolbar() {
        // Setup filter ComboBox with history model
        filterHistoryModel = new DefaultComboBoxModel<>();
        getComboBoxFilter().setModel(filterHistoryModel);
        getComboBoxFilter().setEditable(true);
        getComboBoxFilter().setToolTipText("Filter nach Testname, Crefonummer oder Info");

        // Get the editor component for key handling
        JTextField editorField = (JTextField) getComboBoxFilter().getEditor().getEditorComponent();
        editorField.addActionListener(e -> {
            filterEntries(getFilterText());
            addToFilterHistory(getFilterText());
        });

        // Live filter
        editorField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filterEntries(getFilterText());
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filterEntries(getFilterText());
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filterEntries(getFilterText());
            }
        });

        // CRUD buttons
        getButtonNew().addActionListener(e -> addNewEntry());
        getButtonEdit().addActionListener(e -> editSelectedEntry());
        getButtonDelete().addActionListener(e -> deleteSelectedEntry());

        // Save button
        getButtonSave().addActionListener(e -> saveFile());

        // File location buttons
        getButtonLocateRefExportFile().addActionListener(e -> locateRefExportFile());
        getButtonDownloadRefExportFile().addActionListener(e -> downloadRefExportFile());
        getButtonLocateArchivBestandFile().addActionListener(e -> locateArchivBestandFile());
        getButtonDownloadArchivBestandFile().addActionListener(e -> downloadArchivBestandFile());

        // Add status label to toolbar
        getToolBarControls().add(Box.createHorizontalGlue());
        statusLabel = new JLabel("");
        getToolBarControls().add(statusLabel);
    }

    /**
     * Sets up table selection listener to update controls panel.
     */
    private void setupTableSelection() {
        getTableProperties().getSelectionModel().addListSelectionListener(this::onTableSelectionChanged);
    }

    /**
     * Called when the table selection changes.
     */
    private void onTableSelectionChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }

        int selectedRow = getTableProperties().getSelectedRow();
        if (selectedRow >= 0) {
            TestCrefo testCrefo = tableModel.getTestCrefoAt(selectedRow);
            updateControlsForSelection(testCrefo);
        } else {
            clearControls();
        }
    }

    /**
     * Updates the controls panel based on the selected TestCrefo.
     */
    private void updateControlsForSelection(TestCrefo testCrefo) {
        if (testCrefo == null) {
            clearControls();
            return;
        }

        // REF-Export file
        File refExportFile = testCrefo.getItsqRexExportXmlFile();
        if (refExportFile != null) {
            getTextFieldRefExportFile().setText(refExportFile.getName());
        } else {
            getTextFieldRefExportFile().setText("(nicht zugewiesen)");
        }

        // ARCHIV-BESTAND file - derived from properties file location
        if (selectedItem != null && selectedItem.getFile() != null) {
            String archivBestandPath = deriveArchivBestandPath(selectedItem.getFile(), testCrefo.getItsqTestCrefoNr());
            getTextFieldArchivBestandFile().setText(archivBestandPath);
        }
    }

    /**
     * Derives the ARCHIV-BESTAND file path from the properties file location.
     */
    private String deriveArchivBestandPath(File propsFile, Long crefoNr) {
        try {
            // Navigate up to find REF-EXPORTS and derive ARCHIV-BESTAND path
            File scenarioDir = propsFile.getParentFile();
            File customerDir = scenarioDir.getParentFile();
            File phaseDir = customerDir.getParentFile();
            File refExportsDir = phaseDir.getParentFile();
            File itsqRoot = refExportsDir.getParentFile();

            String phaseName = phaseDir.getName();
            File archivBestandFile = new File(itsqRoot,
                    "ARCHIV-BESTAND/" + phaseName + "/" + crefoNr + ".xml");

            if (archivBestandFile.exists()) {
                return archivBestandFile.getName();
            } else {
                return crefoNr + ".xml (nicht gefunden)";
            }
        } catch (Exception e) {
            return crefoNr + ".xml";
        }
    }

    /**
     * Clears the controls panel.
     */
    private void clearControls() {
        getTextFieldRefExportFile().setText("");
        getTextFieldArchivBestandFile().setText("");
    }

    /**
     * Sets up keyboard shortcuts.
     */
    private void setupKeyboardShortcuts() {
        JTable table = getTableProperties();

        // Ctrl+F - Focus filter
        KeyStroke findKey = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK);
        table.getInputMap(JComponent.WHEN_FOCUSED).put(findKey, "find");
        table.getActionMap().put("find", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getComboBoxFilter().requestFocusInWindow();
                getComboBoxFilter().getEditor().selectAll();
            }
        });

        // Ctrl+S - Save
        KeyStroke saveKey = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
        table.getInputMap(JComponent.WHEN_FOCUSED).put(saveKey, "save");
        table.getActionMap().put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile();
            }
        });

        // Delete - Delete selected
        KeyStroke deleteKey = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        table.getInputMap(JComponent.WHEN_FOCUSED).put(deleteKey, "delete");
        table.getActionMap().put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedEntry();
            }
        });

        // Enter - Edit selected
        KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        table.getInputMap(JComponent.WHEN_FOCUSED).put(enterKey, "edit");
        table.getActionMap().put("edit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editSelectedEntry();
            }
        });

        // Ctrl+N - New entry
        KeyStroke newKey = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK);
        table.getInputMap(JComponent.WHEN_FOCUSED).put(newKey, "new");
        table.getActionMap().put("new", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewEntry();
            }
        });
    }

    // ===== ItsqItemSelectable Implementation =====

    @Override
    public void setSelectedItem(ItsqItem item) {
        // Check for unsaved changes
        if (modified && selectedItem != null) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Aenderungen in " + selectedItem.getName() + " speichern?",
                    "Ungespeicherte Aenderungen",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                saveFile();
            } else if (result == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        this.selectedItem = item;
        loadPropertiesFile();
    }

    public ItsqItem getSelectedItem() {
        return selectedItem;
    }

    // ===== File Operations =====

    /**
     * Loads the Relevanz.properties file into the table.
     */
    private void loadPropertiesFile() {
        if (selectedItem == null || selectedItem.getFile() == null) {
            tableModel.clear();
            clearControls();
            updateStatus("");
            return;
        }

        File file = selectedItem.getFile();
        if (!file.exists() || !file.isFile()) {
            tableModel.clear();
            updateStatus(file.getName() + " (nicht gefunden)");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            File scenarioDir = file.getParentFile();
            tableModel.loadFromLines(lines, scenarioDir);
            setModified(false);
            updateStatus("Geladen: " + file.getName() + " (" + tableModel.getRowCount() + " Eintraege)");
            TimelineLogger.info(ItsqRefExportPropertiesEditorView.class, "Loaded Relevanz.properties file: {}", file.getAbsolutePath());
        } catch (IOException e) {
            TimelineLogger.error(ItsqRefExportPropertiesEditorView.class, "Failed to load Relevanz.properties file: {}", file.getAbsolutePath(), e);
            tableModel.clear();
            updateStatus(file.getName() + " (Fehler)");
        }
    }

    /**
     * Saves the Relevanz.properties file.
     */
    private void saveFile() {
        if (selectedItem == null || selectedItem.getFile() == null) {
            JOptionPane.showMessageDialog(this, "Keine Datei ausgewaehlt");
            return;
        }

        File file = selectedItem.getFile();
        try {
            List<String> lines = tableModel.toLines();
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
            setModified(false);
            updateStatus("Gespeichert: " + file.getName());
            TimelineLogger.info(ItsqRefExportPropertiesEditorView.class, "Saved Relevanz.properties file: {}", file.getAbsolutePath());
        } catch (IOException e) {
            TimelineLogger.error(ItsqRefExportPropertiesEditorView.class, "Failed to save Relevanz.properties file: {}", file.getAbsolutePath(), e);
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Speichern: " + e.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== CRUD Operations =====

    /**
     * Adds a new TestCrefo entry.
     */
    private void addNewEntry() {
        JTextField testNameField = new JTextField();
        JTextField crefoNrField = new JTextField();
        JTextField infoField = new JTextField();
        JCheckBox exportCheckbox = new JCheckBox("Soll exportiert werden");

        Object[] message = {
                "Testname (z.B. p01, n01, x01):", testNameField,
                "Crefo-Nummer:", crefoNrField,
                "Info:", infoField,
                exportCheckbox
        };

        int result = JOptionPane.showConfirmDialog(this, message, "Neuer Eintrag",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String testName = testNameField.getText().trim();
            String crefoNrStr = crefoNrField.getText().trim();
            String info = infoField.getText().trim();
            boolean shouldExport = exportCheckbox.isSelected();

            if (!testName.isEmpty() && !crefoNrStr.isEmpty()) {
                try {
                    Long crefoNr = Long.parseLong(crefoNrStr);
                    TestCrefo testCrefo = new TestCrefo(testName, crefoNr, info, shouldExport, null);
                    tableModel.addEntry(testCrefo);
                    setModified(true);
                    // Select the new row
                    int newRow = tableModel.getRowCount() - 1;
                    getTableProperties().setRowSelectionInterval(newRow, newRow);
                    getTableProperties().scrollRectToVisible(
                            getTableProperties().getCellRect(newRow, 0, true));
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Ungueltige Crefo-Nummer: " + crefoNrStr,
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Edits the selected TestCrefo entry.
     */
    private void editSelectedEntry() {
        int selectedRow = getTableProperties().getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Bitte einen Eintrag auswaehlen");
            return;
        }

        TestCrefo testCrefo = tableModel.getTestCrefoAt(selectedRow);
        if (testCrefo == null) return;

        JTextField testNameField = new JTextField(testCrefo.getTestFallName());
        JTextField crefoNrField = new JTextField(testCrefo.getItsqTestCrefoNr() != null ? testCrefo.getItsqTestCrefoNr().toString() : "");
        JTextField infoField = new JTextField(testCrefo.getTestFallInfo() != null ? testCrefo.getTestFallInfo() : "");
        JCheckBox exportCheckbox = new JCheckBox("Soll exportiert werden", testCrefo.isShouldBeExported());

        Object[] message = {
                "Testname:", testNameField,
                "Crefo-Nummer:", crefoNrField,
                "Info:", infoField,
                exportCheckbox
        };

        int result = JOptionPane.showConfirmDialog(this, message, "Eintrag bearbeiten",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String testName = testNameField.getText().trim();
            String crefoNrStr = crefoNrField.getText().trim();
            String info = infoField.getText().trim();
            boolean shouldExport = exportCheckbox.isSelected();

            if (!testName.isEmpty() && !crefoNrStr.isEmpty()) {
                try {
                    Long crefoNr = Long.parseLong(crefoNrStr);
                    testCrefo.setTestFallName(testName);
                    testCrefo.setItsqTestCrefoNr(crefoNr);
                    testCrefo.setTestFallInfo(info);
                    testCrefo.setShouldBeExported(shouldExport);
                    tableModel.fireTableRowsUpdated(selectedRow, selectedRow);
                    setModified(true);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Ungueltige Crefo-Nummer: " + crefoNrStr,
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Deletes the selected TestCrefo entry.
     */
    private void deleteSelectedEntry() {
        int selectedRow = getTableProperties().getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Bitte einen Eintrag auswaehlen");
            return;
        }

        TestCrefo testCrefo = tableModel.getTestCrefoAt(selectedRow);
        if (testCrefo == null) return;

        int result = JOptionPane.showConfirmDialog(this,
                "Eintrag '" + testCrefo.getTestFallName() + "' wirklich loeschen?",
                "Loeschen bestaetigen",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            tableModel.removeEntry(selectedRow);
            setModified(true);
        }
    }

    // ===== Filter Operations =====

    /**
     * Gets the current filter text from the combo box.
     */
    private String getFilterText() {
        Object item = getComboBoxFilter().getEditor().getItem();
        return item != null ? item.toString().trim() : "";
    }

    /**
     * Filters the table entries by search text.
     */
    private void filterEntries(String filterText) {
        tableModel.setFilter(filterText);
    }

    /**
     * Adds a filter term to the history.
     */
    private void addToFilterHistory(String filterText) {
        if (filterText == null || filterText.isEmpty()) {
            return;
        }

        filterHistoryModel.removeElement(filterText);
        filterHistoryModel.insertElementAt(filterText, 0);

        while (filterHistoryModel.getSize() > MAX_FILTER_HISTORY) {
            filterHistoryModel.removeElementAt(filterHistoryModel.getSize() - 1);
        }

        getComboBoxFilter().getEditor().setItem(filterText);
    }

    // ===== File Location Actions =====

    private void locateRefExportFile() {
        int selectedRow = getTableProperties().getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Bitte zuerst einen Eintrag auswaehlen");
            return;
        }

        TestCrefo testCrefo = tableModel.getTestCrefoAt(selectedRow);
        if (testCrefo == null) return;

        // Open file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("REF-Export XML Datei auswaehlen");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("XML Dateien", "xml"));

        // Set initial directory to scenario folder if available
        if (selectedItem != null && selectedItem.getFile() != null) {
            fileChooser.setCurrentDirectory(selectedItem.getFile().getParentFile());
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            testCrefo.setItsqRexExportXmlFile(selectedFile);
            getTextFieldRefExportFile().setText(selectedFile.getName());
            tableModel.fireTableRowsUpdated(selectedRow, selectedRow);
            setModified(true);
        }
    }

    private void downloadRefExportFile() {
        int selectedRow = getTableProperties().getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Bitte zuerst einen Eintrag auswaehlen");
            return;
        }
        // TODO: Download REF-EXPORT file from remote source
        JOptionPane.showMessageDialog(this, "Download-Funktion noch nicht implementiert");
    }

    private void locateArchivBestandFile() {
        int selectedRow = getTableProperties().getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Bitte zuerst einen Eintrag auswaehlen");
            return;
        }

        // Open file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("ARCHIV-BESTAND XML Datei auswaehlen");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("XML Dateien", "xml"));

        // Set initial directory to ARCHIV-BESTAND/PHASE-X based on current properties file location
        // Path structure: .../REF-EXPORTS/PHASE-X/customer/scenario/Relevanz.properties
        // Target: .../ARCHIV-BESTAND/PHASE-X/
        if (selectedItem != null && selectedItem.getFile() != null) {
            File archivBestandDir = deriveArchivBestandDirectory(selectedItem.getFile());
            if (archivBestandDir != null && archivBestandDir.exists()) {
                fileChooser.setCurrentDirectory(archivBestandDir);
            }
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            getTextFieldArchivBestandFile().setText(selectedFile.getName());
        }
    }

    /**
     * Derives the ARCHIV-BESTAND directory from the properties file location.
     * Path structure: .../REF-EXPORTS/PHASE-X/customer/scenario/Relevanz.properties
     * Target: .../ARCHIV-BESTAND/PHASE-X/
     */
    private File deriveArchivBestandDirectory(File propsFile) {
        try {
            // Navigate up: scenario -> customer -> PHASE-X -> REF-EXPORTS -> ITSQ root
            File scenarioDir = propsFile.getParentFile();
            File customerDir = scenarioDir.getParentFile();
            File phaseDir = customerDir.getParentFile();
            File refExportsDir = phaseDir.getParentFile();
            File itsqRoot = refExportsDir.getParentFile();

            String phaseName = phaseDir.getName();
            return new File(itsqRoot, "ARCHIV-BESTAND" + File.separator + phaseName);
        } catch (Exception e) {
            return null;
        }
    }

    private void downloadArchivBestandFile() {
        int selectedRow = getTableProperties().getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Bitte zuerst einen Eintrag auswaehlen");
            return;
        }
        // TODO: Download ARCHIV-BESTAND file from remote source
        JOptionPane.showMessageDialog(this, "Download-Funktion noch nicht implementiert");
    }

    // ===== UI Helpers =====

    private void setModified(boolean modified) {
        this.modified = modified;
        if (selectedItem != null) {
            String name = selectedItem.getName() + (modified ? " *" : "");
            updateStatus(modified ? "Geaendert: " + name : "");
        }
    }

    private void updateStatus(String status) {
        if (statusLabel != null) {
            statusLabel.setText(status);
        }
    }

    public boolean isModified() {
        return modified;
    }

    // ===== Inner Classes =====

    /**
     * Table model for Relevanz.properties with TestCrefo entries.
     * Columns: Testname, Crefonummer, Info, Export, REF-Export-Datei
     */
    private static class TestCrefoTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {"Testname", "Crefonummer", "Info", "Export", "REF-Export-Datei"};

        private final List<TestCrefo> allEntries = new ArrayList<>();
        private final List<TestCrefo> filteredEntries = new ArrayList<>();
        private final List<String> comments = new ArrayList<>();
        private String filterText = "";

        @Override
        public int getRowCount() {
            return filteredEntries.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 3) { // Export column
                return Boolean.class;
            }
            return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= filteredEntries.size()) {
                return null;
            }
            TestCrefo entry = filteredEntries.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return entry.getTestFallName();
                case 1:
                    return entry.getItsqTestCrefoNr() != null ? entry.getItsqTestCrefoNr().toString() : "";
                case 2:
                    return entry.getTestFallInfo() != null ? entry.getTestFallInfo() : "";
                case 3:
                    return entry.isShouldBeExported();
                case 4:
                    File refExportFile = entry.getItsqRexExportXmlFile();
                    return refExportFile != null ? refExportFile.getName() : "";
                default:
                    return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false; // Non-editable table
        }

        public TestCrefo getTestCrefoAt(int rowIndex) {
            if (rowIndex >= 0 && rowIndex < filteredEntries.size()) {
                return filteredEntries.get(rowIndex);
            }
            return null;
        }

        /**
         * Loads entries from Relevanz.properties lines.
         * Format: testname=crefonummer  or  testname=crefonummer # comment
         */
        public void loadFromLines(List<String> lines, File scenarioDir) {
            allEntries.clear();
            comments.clear();

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                    comments.add(line);
                } else {
                    int eqIndex = line.indexOf('=');
                    if (eqIndex > 0) {
                        String testName = line.substring(0, eqIndex).trim();
                        String value = line.substring(eqIndex + 1).trim();

                        // Parse value: crefonummer or crefonummer # comment
                        Long crefoNr = null;
                        String info = "";
                        boolean shouldExport = true;

                        // Check for inline comment with #
                        int commentIndex = value.indexOf('#');
                        if (commentIndex > 0) {
                            String crefoStr = value.substring(0, commentIndex).trim();
                            info = value.substring(commentIndex + 1).trim();
                            try {
                                crefoNr = Long.parseLong(crefoStr);
                            } catch (NumberFormatException e) {
                                // Invalid crefo number
                            }
                        } else {
                            try {
                                crefoNr = Long.parseLong(value);
                            } catch (NumberFormatException e) {
                                // Invalid crefo number
                            }
                        }

                        if (crefoNr != null) {
                            // Try to find matching REF-Export XML file
                            File refExportFile = findRefExportFile(scenarioDir, testName, crefoNr);
                            TestCrefo testCrefo = new TestCrefo(testName, crefoNr, info, shouldExport, refExportFile);
                            allEntries.add(testCrefo);
                        }
                    }
                }
            }
            applyFilter();
        }

        /**
         * Finds a matching REF-Export XML file in the scenario directory.
         */
        private File findRefExportFile(File scenarioDir, String testName, Long crefoNr) {
            if (scenarioDir == null || !scenarioDir.isDirectory()) {
                return null;
            }

            // Pattern: {testName}_*_{crefoNr}.xml
            String prefix = testName + "_";
            String suffix = "_" + crefoNr + ".xml";

            File[] files = scenarioDir.listFiles((dir, name) ->
                    name.startsWith(prefix) && name.endsWith(suffix));

            return (files != null && files.length > 0) ? files[0] : null;
        }

        /**
         * Converts entries back to properties file lines.
         * Format: testname=crefonummer # comment
         */
        public List<String> toLines() {
            List<String> lines = new ArrayList<>();
            // Add comments first
            lines.addAll(comments);
            // Add entries
            for (TestCrefo entry : allEntries) {
                String line = entry.getTestFallName() + "=" + entry.getItsqTestCrefoNr();
                if (entry.getTestFallInfo() != null && !entry.getTestFallInfo().isEmpty()) {
                    line += " # " + entry.getTestFallInfo();
                }
                lines.add(line);
            }
            return lines;
        }

        public void setFilter(String text) {
            this.filterText = text != null ? text.toLowerCase() : "";
            applyFilter();
        }

        private void applyFilter() {
            filteredEntries.clear();
            for (TestCrefo entry : allEntries) {
                if (filterText.isEmpty() ||
                        (entry.getTestFallName() != null && entry.getTestFallName().toLowerCase().contains(filterText)) ||
                        (entry.getItsqTestCrefoNr() != null && entry.getItsqTestCrefoNr().toString().contains(filterText)) ||
                        (entry.getTestFallInfo() != null && entry.getTestFallInfo().toLowerCase().contains(filterText))) {
                    filteredEntries.add(entry);
                }
            }
            fireTableDataChanged();
        }

        public void addEntry(TestCrefo entry) {
            allEntries.add(entry);
            applyFilter();
        }

        public void removeEntry(int filteredRowIndex) {
            if (filteredRowIndex >= 0 && filteredRowIndex < filteredEntries.size()) {
                TestCrefo entry = filteredEntries.get(filteredRowIndex);
                allEntries.remove(entry);
                applyFilter();
            }
        }

        public void clear() {
            allEntries.clear();
            filteredEntries.clear();
            comments.clear();
            fireTableDataChanged();
        }
    }
}
