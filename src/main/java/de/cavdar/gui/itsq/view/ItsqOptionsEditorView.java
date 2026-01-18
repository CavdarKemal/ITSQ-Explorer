package de.cavdar.gui.itsq.view;

import de.cavdar.gui.itsq.design.ItsqOptionsEditorPanel;
import de.cavdar.gui.itsq.model.ItsqItem;
import de.cavdar.gui.util.TimelineLogger;

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
 * View for editing Options.cfg files.
 * Displays name-value pairs in a table with columns: Name, Value
 */
public class ItsqOptionsEditorView extends ItsqOptionsEditorPanel implements ItsqItemSelectable {

    private ItsqItem selectedItem;
    private OptionsTableModel tableModel;
    private DefaultComboBoxModel<String> filterHistoryModel;
    private JLabel statusLabel;

    private boolean modified = false;
    private static final int MAX_FILTER_HISTORY = 20;

    public ItsqOptionsEditorView() {
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
        tableModel = new OptionsTableModel();
        JTable table = getTableOptions();
        table.setModel(tableModel);
        table.setFont(new Font("Consolas", Font.PLAIN, 13));
        table.setRowHeight(22);

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(200);  // Name
        table.getColumnModel().getColumn(1).setPreferredWidth(400);  // Value

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
        getComboBoxFilter().setToolTipText("Filter nach Name oder Wert");

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
     * Sets up table selection listener.
     */
    private void setupTableSelection() {
        getTableOptions().getSelectionModel().addListSelectionListener(this::onTableSelectionChanged);
    }

    /**
     * Called when the table selection changes.
     */
    private void onTableSelectionChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        // Update UI state based on selection if needed
    }

    /**
     * Sets up keyboard shortcuts.
     */
    private void setupKeyboardShortcuts() {
        JTable table = getTableOptions();

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
        loadOptionsFile();
    }

    public ItsqItem getSelectedItem() {
        return selectedItem;
    }

    // ===== File Operations =====

    /**
     * Loads the Options.cfg file into the table.
     */
    private void loadOptionsFile() {
        if (selectedItem == null || selectedItem.getFile() == null) {
            tableModel.clear();
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
            tableModel.loadFromLines(lines);
            setModified(false);
            updateStatus("Geladen: " + file.getName() + " (" + tableModel.getRowCount() + " Eintraege)");
            TimelineLogger.info(ItsqOptionsEditorView.class, "Loaded Options.cfg file: {}", file.getAbsolutePath());
        } catch (IOException e) {
            TimelineLogger.error(ItsqOptionsEditorView.class, "Failed to load Options.cfg file: {}", file.getAbsolutePath(), e);
            tableModel.clear();
            updateStatus(file.getName() + " (Fehler)");
        }
    }

    /**
     * Saves the Options.cfg file.
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
            TimelineLogger.info(ItsqOptionsEditorView.class, "Saved Options.cfg file: {}", file.getAbsolutePath());
        } catch (IOException e) {
            TimelineLogger.error(ItsqOptionsEditorView.class, "Failed to save Options.cfg file: {}", file.getAbsolutePath(), e);
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Speichern: " + e.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== CRUD Operations =====

    /**
     * Adds a new option entry.
     */
    private void addNewEntry() {
        JTextField nameField = new JTextField();
        JTextField valueField = new JTextField();

        Object[] message = {
                "Name:", nameField,
                "Wert:", valueField
        };

        int result = JOptionPane.showConfirmDialog(this, message, "Neue Option",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String value = valueField.getText().trim();

            if (!name.isEmpty()) {
                tableModel.addEntry(name, value);
                setModified(true);
                // Select the new row
                int newRow = tableModel.getRowCount() - 1;
                getTableOptions().setRowSelectionInterval(newRow, newRow);
                getTableOptions().scrollRectToVisible(
                        getTableOptions().getCellRect(newRow, 0, true));
            }
        }
    }

    /**
     * Edits the selected option entry.
     */
    private void editSelectedEntry() {
        int selectedRow = getTableOptions().getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Bitte einen Eintrag auswaehlen");
            return;
        }

        String currentName = (String) tableModel.getValueAt(selectedRow, 0);
        String currentValue = (String) tableModel.getValueAt(selectedRow, 1);

        JTextField nameField = new JTextField(currentName);
        JTextField valueField = new JTextField(currentValue);

        Object[] message = {
                "Name:", nameField,
                "Wert:", valueField
        };

        int result = JOptionPane.showConfirmDialog(this, message, "Option bearbeiten",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String value = valueField.getText().trim();

            if (!name.isEmpty()) {
                tableModel.updateEntry(selectedRow, name, value);
                setModified(true);
            }
        }
    }

    /**
     * Deletes the selected option entry.
     */
    private void deleteSelectedEntry() {
        int selectedRow = getTableOptions().getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Bitte einen Eintrag auswaehlen");
            return;
        }

        String name = (String) tableModel.getValueAt(selectedRow, 0);
        int result = JOptionPane.showConfirmDialog(this,
                "Option '" + name + "' wirklich loeschen?",
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
     * Table model for Options.cfg with name-value pairs.
     * Supports filtering and CRUD operations.
     */
    private static class OptionsTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {"Name", "Wert"};

        private final List<OptionEntry> allEntries = new ArrayList<>();
        private final List<OptionEntry> filteredEntries = new ArrayList<>();
        private final List<String> comments = new ArrayList<>();
        private String filterText = "";

        private static class OptionEntry {
            String name;
            String value;

            OptionEntry(String name, String value) {
                this.name = name;
                this.value = value;
            }
        }

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
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= filteredEntries.size()) {
                return null;
            }
            OptionEntry entry = filteredEntries.get(rowIndex);
            return columnIndex == 0 ? entry.name : entry.value;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false; // Non-editable table (use dialogs for editing)
        }

        /**
         * Loads entries from Options.cfg lines.
         * Supports both = and : as separators.
         */
        public void loadFromLines(List<String> lines) {
            allEntries.clear();
            comments.clear();

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                    comments.add(line);
                } else {
                    int sepIndex = line.indexOf('=');
                    if (sepIndex < 0) {
                        sepIndex = line.indexOf(':');
                    }
                    if (sepIndex > 0) {
                        String name = line.substring(0, sepIndex).trim();
                        String value = line.substring(sepIndex + 1).trim();
                        allEntries.add(new OptionEntry(name, value));
                    } else {
                        // Line without separator - treat as name with empty value
                        allEntries.add(new OptionEntry(trimmed, ""));
                    }
                }
            }
            applyFilter();
        }

        /**
         * Converts entries back to file lines.
         */
        public List<String> toLines() {
            List<String> lines = new ArrayList<>();
            // Add comments first
            lines.addAll(comments);
            // Add entries
            for (OptionEntry entry : allEntries) {
                lines.add(entry.name + "=" + entry.value);
            }
            return lines;
        }

        public void setFilter(String text) {
            this.filterText = text != null ? text.toLowerCase() : "";
            applyFilter();
        }

        private void applyFilter() {
            filteredEntries.clear();
            for (OptionEntry entry : allEntries) {
                if (filterText.isEmpty() ||
                        entry.name.toLowerCase().contains(filterText) ||
                        entry.value.toLowerCase().contains(filterText)) {
                    filteredEntries.add(entry);
                }
            }
            fireTableDataChanged();
        }

        public void addEntry(String name, String value) {
            OptionEntry entry = new OptionEntry(name, value);
            allEntries.add(entry);
            applyFilter();
        }

        public void updateEntry(int filteredRowIndex, String name, String value) {
            if (filteredRowIndex >= 0 && filteredRowIndex < filteredEntries.size()) {
                OptionEntry entry = filteredEntries.get(filteredRowIndex);
                entry.name = name;
                entry.value = value;
                fireTableRowsUpdated(filteredRowIndex, filteredRowIndex);
            }
        }

        public void removeEntry(int filteredRowIndex) {
            if (filteredRowIndex >= 0 && filteredRowIndex < filteredEntries.size()) {
                OptionEntry entry = filteredEntries.get(filteredRowIndex);
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
