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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * View for displaying and editing XML files.
 * Uses RSyntaxTextArea with syntax highlighting, line numbers, and code folding.
 */
public class ItsqEditorView extends ItsqEditorPanel implements ItsqItemSelectable {

    private ItsqItem selectedItem;

    // XML Editor components
    private RSyntaxTextArea textArea;
    private RTextScrollPane rTextScrollPane;
    private DefaultComboBoxModel<String> filterHistoryModel;
    private JLabel statusLabel;

    private boolean modified = false;
    private static final int MAX_FILTER_HISTORY = 20;

    public ItsqEditorView() {
        super();
        initSyntaxEditor();
        setupToolbar();
        setupKeyboardShortcuts();
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

        // Replace the scrollPane with rTextScrollPane in the parent container
        Container parent = getScrollPaneEditor().getParent();
        if (parent != null) {
            parent.remove(getScrollPaneEditor());
            parent.add(rTextScrollPane, BorderLayout.CENTER);
        }

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
     * Sets up the toolbar buttons from JFormDesigner with their action listeners.
     */
    private void setupToolbar() {
        // Setup filter ComboBox with history model
        filterHistoryModel = new DefaultComboBoxModel<>();
        getComboBoxFilter().setModel(filterHistoryModel);
        getComboBoxFilter().setEditable(true);
        getComboBoxFilter().setToolTipText("Suchbegriff eingeben (F3 = weiter, Shift+F3 = zurueck)");

        // Get the editor component for key handling
        JTextField editorField = (JTextField) getComboBoxFilter().getEditor().getEditorComponent();
        editorField.addActionListener(e -> {
            findNext();
            addToFilterHistory(getFilterText());
        });

        // Save button
        getButtonSave().addActionListener(e -> saveFile());

        // Add status label to toolbar
        getToolBarControls().add(Box.createHorizontalGlue());
        statusLabel = new JLabel("");
        getToolBarControls().add(statusLabel);
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

        // F3 - Find Next
        KeyStroke f3Key = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
        textArea.getInputMap().put(f3Key, "findNext");
        textArea.getActionMap().put("findNext", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                findNext();
            }
        });

        // Shift+F3 - Find Previous
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
     * Loads the selected XML file into the editor.
     */
    private void loadFile() {
        if (selectedItem == null || selectedItem.getFile() == null) {
            textArea.setText("");
            updateStatus("");
            return;
        }

        File file = selectedItem.getFile();
        if (!file.exists() || !file.isFile()) {
            textArea.setText("Datei nicht gefunden: " + file.getAbsolutePath());
            updateStatus(file.getName() + " (nicht gefunden)");
            return;
        }

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            textArea.setText(content);
            textArea.setCaretPosition(0);
            setModified(false);
            updateStatus("Geladen: " + file.getName());
            TimelineLogger.info(ItsqEditorView.class, "Loaded XML file: {}", file.getAbsolutePath());
        } catch (IOException e) {
            TimelineLogger.error(ItsqEditorView.class, "Failed to load file: {}", file.getAbsolutePath(), e);
            textArea.setText("Fehler beim Laden: " + e.getMessage());
            updateStatus(file.getName() + " (Fehler)");
        }
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
            Files.writeString(file.toPath(), textArea.getText(), StandardCharsets.UTF_8);
            setModified(false);
            updateStatus("Gespeichert: " + file.getName());
            TimelineLogger.info(ItsqEditorView.class, "Saved XML file: {}", file.getAbsolutePath());
        } catch (IOException e) {
            TimelineLogger.error(ItsqEditorView.class, "Failed to save file: {}", file.getAbsolutePath(), e);
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Speichern: " + e.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== Search Operations =====

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
     * Finds the next occurrence of the search text.
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
     * Finds the previous occurrence of the search text.
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
            String name = selectedItem.getName() + (modified ? " *" : "");
            updateStatus(modified ? "Geaendert: " + name : "");
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
}
