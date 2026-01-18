package de.cavdar.gui.itsq.view;

import de.cavdar.gui.itsq.design.ItsqTestCrefosPropertiesEditorPanel;
import de.cavdar.gui.itsq.model.ItsqItem;
import de.cavdar.gui.util.TimelineLogger;
import de.cavdar.itsq.AB30XMLProperties;

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
import java.util.stream.Collectors;

/**
 * View for editing TestCrefos.properties files.
 * Displays AB30XMLProperties entries in a table with columns:
 * Crefonummer, Kunden, CLZ, Btlg-List, Bilanz-Typ, Prod-Auft., Statistik, DSGVO-Sperre
 */
public class ItsqTestCrefosPropertiesEditorView extends ItsqTestCrefosPropertiesEditorPanel implements ItsqItemSelectable {

    private ItsqItem selectedItem;
    private AB30XMLPropertiesTableModel tableModel;
    private DefaultComboBoxModel<String> filterHistoryModel;
    private JLabel statusLabel;
    private int fileVersion = AB30XMLProperties.VERSION;

    private boolean modified = false;
    private static final int MAX_FILTER_HISTORY = 20;

    public ItsqTestCrefosPropertiesEditorView() {
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
        tableModel = new AB30XMLPropertiesTableModel();
        JTable table = getTableProperties();
        table.setModel(tableModel);
        table.setFont(new Font("Consolas", Font.PLAIN, 12));
        table.setRowHeight(22);

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(100);  // Crefonummer
        table.getColumnModel().getColumn(1).setPreferredWidth(100);  // Kunden
        table.getColumnModel().getColumn(2).setPreferredWidth(60);   // CLZ
        table.getColumnModel().getColumn(3).setPreferredWidth(120);  // Btlg-List
        table.getColumnModel().getColumn(4).setPreferredWidth(80);   // Bilanz-Typ
        table.getColumnModel().getColumn(5).setPreferredWidth(120);  // Prod-Auft.
        table.getColumnModel().getColumn(6).setPreferredWidth(60);   // Statistik
        table.getColumnModel().getColumn(7).setPreferredWidth(80);   // DSGVO-Sperre

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

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
        getComboBoxFilter().setToolTipText("Filter nach Crefonummer, Kunde oder CLZ");

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
            AB30XMLProperties props = tableModel.getPropertiesAt(selectedRow);
            updateControlsForSelection(props);
        } else {
            clearControls();
        }
    }

    /**
     * Updates the controls panel based on the selected entry.
     */
    private void updateControlsForSelection(AB30XMLProperties props) {
        // Controls panel can be used for additional details if needed
    }

    /**
     * Clears the controls panel.
     */
    private void clearControls() {
        // Clear any controls if present
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
     * Loads the TestCrefos.properties file into the table.
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
            fileVersion = detectVersion(lines);
            tableModel.loadFromLines(lines, fileVersion);
            setModified(false);
            updateStatus("Geladen: " + file.getName() + " (" + tableModel.getRowCount() + " Eintraege)");
            TimelineLogger.info(ItsqTestCrefosPropertiesEditorView.class, "Loaded TestCrefos.properties file: {}", file.getAbsolutePath());
        } catch (IOException e) {
            TimelineLogger.error(ItsqTestCrefosPropertiesEditorView.class, "Failed to load TestCrefos.properties file: {}", file.getAbsolutePath(), e);
            tableModel.clear();
            updateStatus(file.getName() + " (Fehler)");
        }
    }

    /**
     * Detects the version from the file lines.
     */
    private int detectVersion(List<String> lines) {
        for (String line : lines) {
            if (line.startsWith(AB30XMLProperties.VERSION_STR)) {
                try {
                    return Integer.parseInt(line.substring(AB30XMLProperties.VERSION_STR.length()).trim());
                } catch (NumberFormatException e) {
                    return AB30XMLProperties.VERSION;
                }
            }
        }
        return AB30XMLProperties.VERSION;
    }

    /**
     * Saves the TestCrefos.properties file.
     */
    private void saveFile() {
        if (selectedItem == null || selectedItem.getFile() == null) {
            JOptionPane.showMessageDialog(this, "Keine Datei ausgewaehlt");
            return;
        }

        File file = selectedItem.getFile();
        try {
            List<String> lines = tableModel.toLines(fileVersion);
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
            setModified(false);
            updateStatus("Gespeichert: " + file.getName());
            TimelineLogger.info(ItsqTestCrefosPropertiesEditorView.class, "Saved TestCrefos.properties file: {}", file.getAbsolutePath());
        } catch (IOException e) {
            TimelineLogger.error(ItsqTestCrefosPropertiesEditorView.class, "Failed to save TestCrefos.properties file: {}", file.getAbsolutePath(), e);
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Speichern: " + e.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== CRUD Operations =====

    /**
     * Adds a new AB30XMLProperties entry.
     */
    private void addNewEntry() {
        JTextField crefoNrField = new JTextField();
        JTextField clzField = new JTextField();
        JComboBox<AB30XMLProperties.BILANZEN_TYPE> bilanzTypeCombo = new JComboBox<>(AB30XMLProperties.BILANZEN_TYPE.values());
        JComboBox<AB30XMLProperties.EH_PROD_AUFTR_TYPE> prodAuftrCombo = new JComboBox<>(AB30XMLProperties.EH_PROD_AUFTR_TYPE.values());
        JCheckBox ctaStatistikCheck = new JCheckBox("CTA Statistik");
        JCheckBox dsgvoSperreCheck = new JCheckBox("DSGVO Sperre");

        Object[] message = {
                "Crefo-Nummer:", crefoNrField,
                "Auftrags-CLZ:", clzField,
                "Bilanz-Typ:", bilanzTypeCombo,
                "Produktauftrag:", prodAuftrCombo,
                ctaStatistikCheck,
                dsgvoSperreCheck
        };

        int result = JOptionPane.showConfirmDialog(this, message, "Neuer Eintrag",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String crefoNrStr = crefoNrField.getText().trim();

            if (!crefoNrStr.isEmpty()) {
                try {
                    Long crefoNr = Long.parseLong(crefoNrStr);
                    AB30XMLProperties.BILANZEN_TYPE bilanzType = (AB30XMLProperties.BILANZEN_TYPE) bilanzTypeCombo.getSelectedItem();
                    AB30XMLProperties.EH_PROD_AUFTR_TYPE prodAuftrType = (AB30XMLProperties.EH_PROD_AUFTR_TYPE) prodAuftrCombo.getSelectedItem();

                    AB30XMLProperties props = new AB30XMLProperties(crefoNr, bilanzType, prodAuftrType,
                            ctaStatistikCheck.isSelected(), dsgvoSperreCheck.isSelected());

                    String clzStr = clzField.getText().trim();
                    if (!clzStr.isEmpty()) {
                        props.setAuftragClz(Long.parseLong(clzStr));
                    }

                    tableModel.addEntry(props);
                    setModified(true);
                    // Select the new row
                    int newRow = tableModel.getRowCount() - 1;
                    getTableProperties().setRowSelectionInterval(newRow, newRow);
                    getTableProperties().scrollRectToVisible(
                            getTableProperties().getCellRect(newRow, 0, true));
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Ungueltige Nummer: " + e.getMessage(),
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Edits the selected AB30XMLProperties entry.
     */
    private void editSelectedEntry() {
        int selectedRow = getTableProperties().getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Bitte einen Eintrag auswaehlen");
            return;
        }

        AB30XMLProperties props = tableModel.getPropertiesAt(selectedRow);
        if (props == null) return;

        JTextField crefoNrField = new JTextField(props.getCrefoNr() != null ? props.getCrefoNr().toString() : "");
        JTextField clzField = new JTextField(props.getAuftragClz() != null ? props.getAuftragClz().toString() : "");
        JComboBox<AB30XMLProperties.BILANZEN_TYPE> bilanzTypeCombo = new JComboBox<>(AB30XMLProperties.BILANZEN_TYPE.values());
        bilanzTypeCombo.setSelectedItem(props.getBilanzType());
        JComboBox<AB30XMLProperties.EH_PROD_AUFTR_TYPE> prodAuftrCombo = new JComboBox<>(AB30XMLProperties.EH_PROD_AUFTR_TYPE.values());
        prodAuftrCombo.setSelectedItem(props.getEhProduktAuftragType());
        JCheckBox ctaStatistikCheck = new JCheckBox("CTA Statistik", props.isMitCtaStatistik());
        JCheckBox dsgvoSperreCheck = new JCheckBox("DSGVO Sperre", props.isMitDsgVoSperre());

        Object[] message = {
                "Crefo-Nummer:", crefoNrField,
                "Auftrags-CLZ:", clzField,
                "Bilanz-Typ:", bilanzTypeCombo,
                "Produktauftrag:", prodAuftrCombo,
                ctaStatistikCheck,
                dsgvoSperreCheck
        };

        int result = JOptionPane.showConfirmDialog(this, message, "Eintrag bearbeiten",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String crefoNrStr = crefoNrField.getText().trim();

            if (!crefoNrStr.isEmpty()) {
                try {
                    Long crefoNr = Long.parseLong(crefoNrStr);
                    props.setCrefoNr(crefoNr);

                    String clzStr = clzField.getText().trim();
                    props.setAuftragClz(clzStr.isEmpty() ? null : Long.parseLong(clzStr));

                    props.setBilanzType((AB30XMLProperties.BILANZEN_TYPE) bilanzTypeCombo.getSelectedItem());
                    props.setEhProdAuftrType((AB30XMLProperties.EH_PROD_AUFTR_TYPE) prodAuftrCombo.getSelectedItem());
                    props.setMitCtaStatistik(ctaStatistikCheck.isSelected());
                    props.setDsgVoSperre(dsgvoSperreCheck.isSelected());

                    tableModel.fireTableRowsUpdated(selectedRow, selectedRow);
                    setModified(true);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Ungueltige Nummer: " + e.getMessage(),
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Deletes the selected entry.
     */
    private void deleteSelectedEntry() {
        int selectedRow = getTableProperties().getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Bitte einen Eintrag auswaehlen");
            return;
        }

        AB30XMLProperties props = tableModel.getPropertiesAt(selectedRow);
        if (props == null) return;

        int result = JOptionPane.showConfirmDialog(this,
                "Eintrag '" + props.getCrefoNr() + "' wirklich loeschen?",
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
     * Table model for TestCrefos.properties with AB30XMLProperties entries.
     * Columns: Crefonummer, Kunden, CLZ, Btlg-List, Bilanz-Typ, Prod-Auft., Statistik, DSGVO-Sperre
     */
    private static class AB30XMLPropertiesTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {
                "Crefonummer", "Kunden", "CLZ", "Btlg-List", "Bilanz-Typ", "Prod-Auft.", "Statistik", "DSGVO-Sperre"
        };

        private final List<AB30XMLProperties> allEntries = new ArrayList<>();
        private final List<AB30XMLProperties> filteredEntries = new ArrayList<>();
        private final List<String> headerLines = new ArrayList<>();
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
            if (columnIndex == 6 || columnIndex == 7) { // Statistik, DSGVO-Sperre
                return Boolean.class;
            }
            return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= filteredEntries.size()) {
                return null;
            }
            AB30XMLProperties entry = filteredEntries.get(rowIndex);
            switch (columnIndex) {
                case 0: // Crefonummer
                    return entry.getCrefoNr() != null ? entry.getCrefoNr().toString() : "";
                case 1: // Kunden
                    return String.join(", ", entry.getUsedByCustomersList());
                case 2: // CLZ
                    return entry.getAuftragClz() != null ? entry.getAuftragClz().toString() : "";
                case 3: // Btlg-List
                    return entry.getBtlgCrefosList().stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(", "));
                case 4: // Bilanz-Typ
                    return entry.getBilanzType() != null ? entry.getBilanzType().name() : "";
                case 5: // Prod-Auft.
                    return entry.getEhProduktAuftragType() != null ? entry.getEhProduktAuftragType().name() : "";
                case 6: // Statistik
                    return entry.isMitCtaStatistik();
                case 7: // DSGVO-Sperre
                    return entry.isMitDsgVoSperre();
                default:
                    return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false; // Non-editable table
        }

        public AB30XMLProperties getPropertiesAt(int rowIndex) {
            if (rowIndex >= 0 && rowIndex < filteredEntries.size()) {
                return filteredEntries.get(rowIndex);
            }
            return null;
        }

        /**
         * Loads entries from TestCrefos.properties lines.
         * Format: crefonr::[customers],[clz],[btlg-list],[bilanz-type],[eh-prod-auftr],[cta-stat],[dsgvo]
         */
        public void loadFromLines(List<String> lines, int version) {
            allEntries.clear();
            headerLines.clear();

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    headerLines.add(line);
                } else {
                    try {
                        AB30XMLProperties props = new AB30XMLProperties(trimmed, version);
                        allEntries.add(props);
                    } catch (Exception e) {
                        // Skip invalid lines
                        headerLines.add("# INVALID: " + line);
                    }
                }
            }
            applyFilter();
        }

        /**
         * Converts entries back to properties file lines.
         */
        public List<String> toLines(int version) {
            List<String> lines = new ArrayList<>();
            // Add header lines
            lines.addAll(headerLines);
            // Add entries
            for (AB30XMLProperties entry : allEntries) {
                lines.add(entry.toString());
            }
            return lines;
        }

        public void setFilter(String text) {
            this.filterText = text != null ? text.toLowerCase() : "";
            applyFilter();
        }

        private void applyFilter() {
            filteredEntries.clear();
            for (AB30XMLProperties entry : allEntries) {
                if (filterText.isEmpty() ||
                        (entry.getCrefoNr() != null && entry.getCrefoNr().toString().contains(filterText)) ||
                        (entry.getAuftragClz() != null && entry.getAuftragClz().toString().contains(filterText)) ||
                        entry.getUsedByCustomersList().stream().anyMatch(c -> c.toLowerCase().contains(filterText))) {
                    filteredEntries.add(entry);
                }
            }
            fireTableDataChanged();
        }

        public void addEntry(AB30XMLProperties entry) {
            allEntries.add(entry);
            applyFilter();
        }

        public void removeEntry(int filteredRowIndex) {
            if (filteredRowIndex >= 0 && filteredRowIndex < filteredEntries.size()) {
                AB30XMLProperties entry = filteredEntries.get(filteredRowIndex);
                allEntries.remove(entry);
                applyFilter();
            }
        }

        public void clear() {
            allEntries.clear();
            filteredEntries.clear();
            headerLines.clear();
            fireTableDataChanged();
        }
    }
}
