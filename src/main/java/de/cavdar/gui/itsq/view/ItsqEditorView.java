package de.cavdar.gui.itsq.view;

import de.cavdar.gui.itsq.design.ItsqEditorPanel;
import de.cavdar.gui.itsq.model.ItsqItem;
import de.cavdar.gui.util.TimelineLogger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * View for displaying and editing text files.
 * Supports two modes:
 * - XML mode: RSyntaxTextArea with syntax highlighting, line numbers, code folding
 * - Properties mode: Table editor for .cfg and .properties files with Name/Value columns
 */
public class ItsqEditorView extends ItsqEditorPanel implements ItsqItemSelectable {

    private static final String CARD_XML = "xml";
    private static final String CARD_PROPERTIES = "properties";

    private ItsqItem selectedItem;
    private String currentMode = CARD_XML;

    // XML Editor components
    private RSyntaxTextArea textArea;
    private RTextScrollPane rTextScrollPane;
    private DefaultComboBoxModel<String> filterHistoryModel;
    private JLabel statusLabel;

    // Properties Table components
    private JTable propertiesTable;
    private PropertiesTableModel propertiesTableModel;

    // CardLayout for switching between modes
    private JPanel cardPanel;
    private CardLayout cardLayout;

    private boolean modified = false;
    private static final int MAX_FILTER_HISTORY = 20;

    public ItsqEditorView() {
        super();
        initCardLayout();
        initSyntaxEditor();
        initPropertiesEditor();
        setupToolbar();
        setupKeyboardShortcuts();
    }

    /**
     * Initializes the CardLayout for switching between XML and Properties mode.
     */
    private void initCardLayout() {
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // Replace the scrollPane with cardPanel in the parent container
        Container parent = getScrollPaneEditor().getParent();
        if (parent != null) {
            parent.remove(getScrollPaneEditor());
            parent.add(cardPanel, BorderLayout.CENTER);
        }
    }

    /**
     * Initializes RSyntaxTextArea for XML editing.
     */
    private void initSyntaxEditor() {
        // Create RSyntaxTextArea
        textArea = new RSyntaxTextArea(20, 60);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setAutoIndentEnabled(true);
        textArea.setBracketMatchingEnabled(true);
        textArea.setCloseCurlyBraces(true);
        textArea.setMarkOccurrences(true);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 13));

        // Create scroll pane with line numbers
        rTextScrollPane = new RTextScrollPane(textArea);
        rTextScrollPane.setLineNumbersEnabled(true);
        rTextScrollPane.setFoldIndicatorEnabled(true);

        // Add to card panel
        cardPanel.add(rTextScrollPane, CARD_XML);

        // Track modifications
        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                setModified(true);
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                setModified(true);
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                setModified(true);
            }
        });
    }

    /**
     * Initializes the Properties table editor for .cfg and .properties files.
     */
    private void initPropertiesEditor() {
        // Create table model and table
        propertiesTableModel = new PropertiesTableModel();
        propertiesTable = new JTable(propertiesTableModel);
        propertiesTable.setFont(new Font("Consolas", Font.PLAIN, 13));
        propertiesTable.setRowHeight(22);
        propertiesTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        propertiesTable.getColumnModel().getColumn(1).setPreferredWidth(400);
        propertiesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Double-click to edit
        propertiesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedProperty();
                }
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(propertiesTable);

        // Add to card panel
        cardPanel.add(tableScrollPane, CARD_PROPERTIES);
    }

    /**
     * Filters properties table by search text.
     */
    private void filterProperties(String filterText) {
        propertiesTableModel.setFilter(filterText);
    }

    /**
     * Sets up the toolbar buttons from JFormDesigner with their action listeners.
     */
    private void setupToolbar() {
        // Setup filter ComboBox with history model
        filterHistoryModel = new DefaultComboBoxModel<>();
        getComboBoxFilter().setModel(filterHistoryModel);
        getComboBoxFilter().setEditable(true);
        getComboBoxFilter().setToolTipText("Filter eingeben (XML: Strg+F zum Suchen, Properties: Filter nach Name/Wert)");

        // Get the editor component for key handling
        JTextField editorField = (JTextField) getComboBoxFilter().getEditor().getEditorComponent();
        editorField.addActionListener(e -> {
            if (CARD_XML.equals(currentMode)) {
                findNext();
                addToFilterHistory(getFilterText());
            } else {
                filterProperties(getFilterText());
                addToFilterHistory(getFilterText());
            }
        });

        // Live filter for properties mode
        editorField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                if (CARD_PROPERTIES.equals(currentMode)) {
                    filterProperties(getFilterText());
                }
            }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                if (CARD_PROPERTIES.equals(currentMode)) {
                    filterProperties(getFilterText());
                }
            }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                if (CARD_PROPERTIES.equals(currentMode)) {
                    filterProperties(getFilterText());
                }
            }
        });

        // CRUD buttons - only enabled in Properties mode
        getButtonNew().addActionListener(e -> addNewProperty());
        getButtonEdit().addActionListener(e -> {
            if (CARD_PROPERTIES.equals(currentMode)) {
                editSelectedProperty();
            }
        });
        getButtonDelete().addActionListener(e -> {
            if (CARD_PROPERTIES.equals(currentMode)) {
                deleteSelectedProperty();
            }
        });

        // Save button
        getButtonSave().addActionListener(e -> saveFile());

        // Add status label to toolbar
        getToolBarControls().add(Box.createHorizontalGlue());
        statusLabel = new JLabel("");
        getToolBarControls().add(statusLabel);
    }

    /**
     * Updates the toolbar button states based on the current mode.
     */
    private void updateToolbarForMode() {
        boolean isPropertiesMode = CARD_PROPERTIES.equals(currentMode);
        getButtonNew().setEnabled(isPropertiesMode);
        getButtonEdit().setEnabled(isPropertiesMode);
        getButtonDelete().setEnabled(isPropertiesMode);

        // Update filter label tooltip
        if (isPropertiesMode) {
            getLabelFilter().setToolTipText("Filter nach Name oder Wert");
        } else {
            getLabelFilter().setToolTipText("Suchbegriff (F3 = weiter, Shift+F3 = zurueck)");
        }
    }

    /**
     * Sets up keyboard shortcuts.
     */
    private void setupKeyboardShortcuts() {
        // Ctrl+F - Focus filter/search
        KeyStroke findKey = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK);
        textArea.getInputMap().put(findKey, "find");
        textArea.getActionMap().put("find", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getComboBoxFilter().requestFocusInWindow();
                getComboBoxFilter().getEditor().selectAll();
            }
        });

        // F3 - Find Next (XML mode only)
        KeyStroke f3Key = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
        textArea.getInputMap().put(f3Key, "findNext");
        textArea.getActionMap().put("findNext", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                findNext();
            }
        });

        // Shift+F3 - Find Previous (XML mode only)
        KeyStroke shiftF3Key = KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK);
        textArea.getInputMap().put(shiftF3Key, "findPrev");
        textArea.getActionMap().put("findPrev", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                findPrevious();
            }
        });

        // Ctrl+S - Save
        KeyStroke saveKey = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
        textArea.getInputMap().put(saveKey, "save");
        textArea.getActionMap().put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile();
            }
        });

        // Ctrl+G - Go to Line
        KeyStroke gotoKey = KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK);
        textArea.getInputMap().put(gotoKey, "gotoLine");
        textArea.getActionMap().put("gotoLine", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showGoToLineDialog();
            }
        });
    }

    // ===== ItsqItemSelectable Implementation =====

    @Override
    public void setSelectedItem(ItsqItem item) {
        // Check for unsaved changes
        if (modified && selectedItem != null) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Änderungen in " + selectedItem.getName() + " speichern?",
                    "Ungespeicherte Änderungen",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                saveFile();
            } else if (result == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        this.selectedItem = item;
        loadFile();
    }

    public ItsqItem getSelectedItem() {
        return selectedItem;
    }

    // ===== File Operations =====

    /**
     * Determines the editor mode based on file extension.
     */
    private String determineMode(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".cfg") || name.endsWith(".properties")) {
            return CARD_PROPERTIES;
        }
        return CARD_XML;
    }

    /**
     * Switches to the specified editor mode.
     */
    private void switchMode(String mode) {
        currentMode = mode;
        cardLayout.show(cardPanel, mode);
        updateToolbarForMode();
    }

    /**
     * Loads the selected file into the appropriate editor.
     */
    private void loadFile() {
        if (selectedItem == null || selectedItem.getFile() == null) {
            textArea.setText("");
            propertiesTableModel.clear();
            updateTitle(null);
            return;
        }

        File file = selectedItem.getFile();
        if (!file.exists() || !file.isFile()) {
            textArea.setText("Datei nicht gefunden: " + file.getAbsolutePath());
            updateTitle(file.getName() + " (nicht gefunden)");
            return;
        }

        String mode = determineMode(file);
        switchMode(mode);

        if (CARD_PROPERTIES.equals(mode)) {
            loadPropertiesFile(file);
        } else {
            loadXmlFile(file);
        }
    }

    /**
     * Loads an XML file into the syntax editor.
     */
    private void loadXmlFile(File file) {
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            textArea.setText(content);
            textArea.setCaretPosition(0);
            setModified(false);
            updateTitle(file.getName());
            updateStatus("Geladen: " + file.getName());
            TimelineLogger.info(ItsqEditorView.class, "Loaded XML file: {}", file.getAbsolutePath());
        } catch (IOException e) {
            TimelineLogger.error(ItsqEditorView.class, "Failed to load file: {}", file.getAbsolutePath(), e);
            textArea.setText("Fehler beim Laden: " + e.getMessage());
            updateTitle(file.getName() + " (Fehler)");
        }
    }

    /**
     * Loads a properties/cfg file into the table editor.
     */
    private void loadPropertiesFile(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            propertiesTableModel.loadFromLines(lines);
            setModified(false);
            updateTitle(file.getName());
            updateStatus("Geladen: " + file.getName() + " (" + propertiesTableModel.getRowCount() + " Eintraege)");
            TimelineLogger.info(ItsqEditorView.class, "Loaded properties file: {}", file.getAbsolutePath());
        } catch (IOException e) {
            TimelineLogger.error(ItsqEditorView.class, "Failed to load properties file: {}", file.getAbsolutePath(), e);
            propertiesTableModel.clear();
            updateTitle(file.getName() + " (Fehler)");
        }
    }

    /**
     * Reloads the current file.
     */
    private void reloadFile() {
        if (modified) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Änderungen verwerfen und neu laden?",
                    "Neu laden",
                    JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }
        loadFile();
    }

    /**
     * Saves the current content to file.
     */
    private void saveFile() {
        if (selectedItem == null || selectedItem.getFile() == null) {
            JOptionPane.showMessageDialog(this, "Keine Datei ausgewaehlt");
            return;
        }

        File file = selectedItem.getFile();
        try {
            if (CARD_PROPERTIES.equals(currentMode)) {
                savePropertiesFile(file);
            } else {
                saveXmlFile(file);
            }
        } catch (IOException e) {
            TimelineLogger.error(ItsqEditorView.class, "Failed to save file: {}", file.getAbsolutePath(), e);
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Speichern: " + e.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Saves XML content to file.
     */
    private void saveXmlFile(File file) throws IOException {
        Files.writeString(file.toPath(), textArea.getText(), StandardCharsets.UTF_8);
        setModified(false);
        updateStatus("Gespeichert: " + file.getName());
        TimelineLogger.info(ItsqEditorView.class, "Saved XML file: {}", file.getAbsolutePath());
    }

    /**
     * Saves properties content to file.
     */
    private void savePropertiesFile(File file) throws IOException {
        List<String> lines = propertiesTableModel.toLines();
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        setModified(false);
        updateStatus("Gespeichert: " + file.getName());
        TimelineLogger.info(ItsqEditorView.class, "Saved properties file: {}", file.getAbsolutePath());
    }

    // ===== Properties CRUD Operations =====

    /**
     * Adds a new property entry.
     */
    private void addNewProperty() {
        JTextField nameField = new JTextField();
        JTextField valueField = new JTextField();
        Object[] message = {
            "Name:", nameField,
            "Wert:", valueField
        };

        int result = JOptionPane.showConfirmDialog(this, message, "Neuer Eintrag",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String value = valueField.getText();
            if (!name.isEmpty()) {
                propertiesTableModel.addEntry(name, value);
                setModified(true);
                // Select the new row
                int newRow = propertiesTableModel.getRowCount() - 1;
                propertiesTable.setRowSelectionInterval(newRow, newRow);
                propertiesTable.scrollRectToVisible(propertiesTable.getCellRect(newRow, 0, true));
            }
        }
    }

    /**
     * Edits the selected property entry.
     */
    private void editSelectedProperty() {
        int selectedRow = propertiesTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Bitte einen Eintrag auswaehlen");
            return;
        }

        String currentName = (String) propertiesTableModel.getValueAt(selectedRow, 0);
        String currentValue = (String) propertiesTableModel.getValueAt(selectedRow, 1);

        JTextField nameField = new JTextField(currentName);
        JTextField valueField = new JTextField(currentValue);
        Object[] message = {
            "Name:", nameField,
            "Wert:", valueField
        };

        int result = JOptionPane.showConfirmDialog(this, message, "Eintrag bearbeiten",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String newName = nameField.getText().trim();
            String newValue = valueField.getText();
            if (!newName.isEmpty()) {
                propertiesTableModel.updateEntry(selectedRow, newName, newValue);
                setModified(true);
            }
        }
    }

    /**
     * Deletes the selected property entry.
     */
    private void deleteSelectedProperty() {
        int selectedRow = propertiesTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Bitte einen Eintrag auswaehlen");
            return;
        }

        String name = (String) propertiesTableModel.getValueAt(selectedRow, 0);
        int result = JOptionPane.showConfirmDialog(this,
                "Eintrag '" + name + "' wirklich loeschen?",
                "Loeschen bestaetigen",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            propertiesTableModel.removeEntry(selectedRow);
            setModified(true);
        }
    }

    // ===== Search/Filter Operations =====

    /**
     * Gets the current filter/search text from the combo box.
     */
    private String getFilterText() {
        Object item = getComboBoxFilter().getEditor().getItem();
        return item != null ? item.toString().trim() : "";
    }

    /**
     * Adds a filter term to the history (at the top, avoiding duplicates).
     */
    private void addToFilterHistory(String filterText) {
        if (filterText == null || filterText.isEmpty()) {
            return;
        }

        // Remove if already exists (to move to top)
        filterHistoryModel.removeElement(filterText);

        // Add at the beginning
        filterHistoryModel.insertElementAt(filterText, 0);

        // Limit history size
        while (filterHistoryModel.getSize() > MAX_FILTER_HISTORY) {
            filterHistoryModel.removeElementAt(filterHistoryModel.getSize() - 1);
        }

        // Keep the text in the editor
        getComboBoxFilter().getEditor().setItem(filterText);
    }

    /**
     * Finds the next occurrence of the search text (XML mode).
     */
    private void findNext() {
        String searchText = getFilterText();
        if (searchText.isEmpty()) {
            return;
        }

        SearchContext context = new SearchContext(searchText);
        context.setMatchCase(false);
        context.setWholeWord(false);
        context.setSearchForward(true);

        boolean found = SearchEngine.find(textArea, context).wasFound();
        if (!found) {
            // Wrap around
            textArea.setCaretPosition(0);
            found = SearchEngine.find(textArea, context).wasFound();
        }

        if (!found) {
            updateStatus("Nicht gefunden: " + searchText);
        } else {
            addToFilterHistory(searchText);
            updateStatus("");
        }
    }

    /**
     * Finds the previous occurrence of the search text (XML mode).
     */
    private void findPrevious() {
        String searchText = getFilterText();
        if (searchText.isEmpty()) {
            return;
        }

        SearchContext context = new SearchContext(searchText);
        context.setMatchCase(false);
        context.setWholeWord(false);
        context.setSearchForward(false);

        boolean found = SearchEngine.find(textArea, context).wasFound();
        if (!found) {
            // Wrap around
            textArea.setCaretPosition(textArea.getText().length());
            found = SearchEngine.find(textArea, context).wasFound();
        }

        if (!found) {
            updateStatus("Nicht gefunden: " + searchText);
        } else {
            addToFilterHistory(searchText);
            updateStatus("");
        }
    }

    /**
     * Shows dialog to go to a specific line.
     */
    private void showGoToLineDialog() {
        String input = JOptionPane.showInputDialog(this,
                "Zeilennummer (1-" + textArea.getLineCount() + "):",
                "Gehe zu Zeile",
                JOptionPane.PLAIN_MESSAGE);
        if (input != null && !input.isEmpty()) {
            try {
                int line = Integer.parseInt(input) - 1;
                if (line >= 0 && line < textArea.getLineCount()) {
                    textArea.setCaretPosition(textArea.getLineStartOffset(line));
                    textArea.requestFocusInWindow();
                }
            } catch (NumberFormatException | javax.swing.text.BadLocationException e) {
                // Ignore invalid input
            }
        }
    }

    // ===== UI Helpers =====

    private void setModified(boolean modified) {
        this.modified = modified;
        if (selectedItem != null) {
            updateTitle(selectedItem.getName() + (modified ? " *" : ""));
        }
    }

    private void updateTitle(String title) {
        // Update the label in panelControls
        Component[] components = getPanelControls().getComponents();
        for (Component c : components) {
            if (c instanceof JLabel label) {
                label.setText(title != null ? title : "Keine Datei");
                break;
            }
        }
    }

    private void updateStatus(String status) {
        if (statusLabel != null) {
            statusLabel.setText(status);
        }
    }

    // ===== Public Accessors =====

    public RSyntaxTextArea getTextArea() {
        return textArea;
    }

    public boolean isModified() {
        return modified;
    }

    // ===== Inner Classes =====

    /**
     * Table model for properties/cfg files with Name/Value columns.
     * Supports filtering, CRUD operations, and preserves comments/empty lines.
     */
    private static class PropertiesTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {"Name", "Wert"};

        // Original data (including comments and empty lines for preservation)
        private final List<PropertyEntry> allEntries = new ArrayList<>();
        // Filtered data for display
        private final List<PropertyEntry> filteredEntries = new ArrayList<>();
        private String filterText = "";

        /**
         * Represents a single line in the properties file.
         */
        private static class PropertyEntry {
            String key;
            String value;
            boolean isComment;  // true for comments and empty lines
            String originalLine; // preserved for comments

            PropertyEntry(String key, String value) {
                this.key = key;
                this.value = value;
                this.isComment = false;
            }

            PropertyEntry(String originalLine, boolean isComment) {
                this.originalLine = originalLine;
                this.isComment = isComment;
                this.key = "";
                this.value = "";
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
            PropertyEntry entry = filteredEntries.get(rowIndex);
            return columnIndex == 0 ? entry.key : entry.value;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false; // Edit via dialog
        }

        /**
         * Loads properties from file lines.
         */
        public void loadFromLines(List<String> lines) {
            allEntries.clear();
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                    // Comment or empty line
                    allEntries.add(new PropertyEntry(line, true));
                } else {
                    // Key=Value line
                    int eqIndex = line.indexOf('=');
                    if (eqIndex < 0) {
                        eqIndex = line.indexOf(':');
                    }
                    if (eqIndex > 0) {
                        String key = line.substring(0, eqIndex).trim();
                        String value = line.substring(eqIndex + 1);
                        allEntries.add(new PropertyEntry(key, value));
                    } else {
                        // Line without separator - treat as key with empty value
                        allEntries.add(new PropertyEntry(trimmed, ""));
                    }
                }
            }
            applyFilter();
        }

        /**
         * Converts the model back to lines for saving.
         */
        public List<String> toLines() {
            List<String> lines = new ArrayList<>();
            for (PropertyEntry entry : allEntries) {
                if (entry.isComment) {
                    lines.add(entry.originalLine);
                } else {
                    lines.add(entry.key + "=" + entry.value);
                }
            }
            return lines;
        }

        /**
         * Sets the filter text and updates the view.
         */
        public void setFilter(String text) {
            this.filterText = text != null ? text.toLowerCase() : "";
            applyFilter();
        }

        /**
         * Applies the current filter to show only matching entries.
         */
        private void applyFilter() {
            filteredEntries.clear();
            for (PropertyEntry entry : allEntries) {
                if (!entry.isComment) {
                    if (filterText.isEmpty() ||
                        entry.key.toLowerCase().contains(filterText) ||
                        entry.value.toLowerCase().contains(filterText)) {
                        filteredEntries.add(entry);
                    }
                }
            }
            fireTableDataChanged();
        }

        /**
         * Adds a new property entry.
         */
        public void addEntry(String key, String value) {
            PropertyEntry entry = new PropertyEntry(key, value);
            allEntries.add(entry);
            applyFilter();
        }

        /**
         * Updates an entry at the given row index.
         */
        public void updateEntry(int filteredRowIndex, String newKey, String newValue) {
            if (filteredRowIndex >= 0 && filteredRowIndex < filteredEntries.size()) {
                PropertyEntry entry = filteredEntries.get(filteredRowIndex);
                entry.key = newKey;
                entry.value = newValue;
                fireTableRowsUpdated(filteredRowIndex, filteredRowIndex);
            }
        }

        /**
         * Removes an entry at the given row index.
         */
        public void removeEntry(int filteredRowIndex) {
            if (filteredRowIndex >= 0 && filteredRowIndex < filteredEntries.size()) {
                PropertyEntry entry = filteredEntries.get(filteredRowIndex);
                allEntries.remove(entry);
                applyFilter();
            }
        }

        /**
         * Clears all entries.
         */
        public void clear() {
            allEntries.clear();
            filteredEntries.clear();
            fireTableDataChanged();
        }
    }
}
