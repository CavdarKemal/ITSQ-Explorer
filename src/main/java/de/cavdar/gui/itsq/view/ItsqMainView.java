package de.cavdar.gui.itsq.view;

import de.cavdar.gui.itsq.design.ItsqMainPanel;
import de.cavdar.gui.model.base.AppConfig;
import de.cavdar.gui.util.TimelineLogger;

import javax.swing.*;
import java.io.File;

import static de.cavdar.gui.util.AppConstants.*;

/**
 * Main View for ITSQ Explorer - manages all GUI interactions.
 * Extends ItsqMainPanel (JFormDesigner generated) and adds behavior.
 *
 * @author TemplateGUI
 */
public class ItsqMainView extends ItsqMainPanel {

    private static final String TESTSET_HISTORY_KEY = "itsq.testset.history";
    private static final int MAX_HISTORY = 20;

    private final AppConfig cfg;
    private DefaultComboBoxModel<String> testSetHistoryModel;
    private boolean updatingComboBox = false;
    private boolean initialLoadDone = false;
    private String lastValidSelection = null;

    public ItsqMainView(AppConfig config) {
        super();
        this.cfg = config;

        initTestSetComboBox();
        setupListeners();

        TimelineLogger.debug(ItsqMainView.class, "ItsqMainView created");
    }

    /**
     * Initializes the view and loads data.
     * Call this after the view is visible.
     */
    public void initialize() {
        if (!initialLoadDone) {
            initialLoadDone = true;
            loadItsqDirectory();
        }
    }

    // ===== Setup =====

    private void setupListeners() {
        // Load button
        getButtonLoad().addActionListener(e -> browseItsqPath());

        // ComboBox selection change -> reload tree
        getComboBoxTestSet().addActionListener(e -> onTestSetSelectionChanged());

        // Filter ComboBox -> apply filter
        getComboBoxFilter().addActionListener(e -> applyFilter());
        JTextField filterEditor = (JTextField) getComboBoxFilter().getEditor().getEditorComponent();
        filterEditor.addActionListener(e -> applyFilter());

        // Active Only checkbox -> apply filter
        getCheckBoxActiveOnly().addActionListener(e -> applyFilter());

        // Source filter -> apply filter
        getComboBoxTestSetSource().addActionListener(e -> applyFilter());

        // Phase filter -> apply filter
        getComboBoxPhase().addActionListener(e -> applyFilter());

        // Tree selection callback -> delegate to ViewTabView
        getPanelItsqTree().setSelectionCallback(node -> getPanelItsqView().showCardForNode(node));
    }

    // ===== TestSet ComboBox =====

    @SuppressWarnings("unchecked")
    private void initTestSetComboBox() {
        testSetHistoryModel = new DefaultComboBoxModel<>();
        JComboBox<String> comboBox = getComboBoxTestSet();
        comboBox.setModel(testSetHistoryModel);
        comboBox.setEditable(true);

        updatingComboBox = true;
        try {
            loadTestSetHistory();
        } finally {
            updatingComboBox = false;
        }

        JTextField editor = (JTextField) comboBox.getEditor().getEditorComponent();
        editor.addActionListener(e -> {
            String path = editor.getText().trim();
            if (!path.isEmpty()) {
                loadTestSetPath(path);
            }
        });
    }

    private void loadTestSetHistory() {
        String history = cfg.getProperty(TESTSET_HISTORY_KEY);
        if (history != null && !history.isEmpty()) {
            for (String path : history.split("\\|")) {
                if (!path.isEmpty()) {
                    testSetHistoryModel.addElement(path);
                }
            }
        }
    }

    private void saveTestSetHistory() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < testSetHistoryModel.getSize(); i++) {
            if (i > 0) sb.append("|");
            sb.append(testSetHistoryModel.getElementAt(i));
        }
        cfg.setProperty(TESTSET_HISTORY_KEY, sb.toString());
        cfg.save();
    }

    private void addToTestSetHistory(String path) {
        if (path == null || path.isEmpty()) return;

        updatingComboBox = true;
        try {
            testSetHistoryModel.removeElement(path);
            testSetHistoryModel.insertElementAt(path, 0);
            while (testSetHistoryModel.getSize() > MAX_HISTORY) {
                testSetHistoryModel.removeElementAt(testSetHistoryModel.getSize() - 1);
            }
            testSetHistoryModel.setSelectedItem(path);
            saveTestSetHistory();
        } finally {
            updatingComboBox = false;
        }
    }

    private void onTestSetSelectionChanged() {
        if (updatingComboBox) return;

        Object selected = getComboBoxTestSet().getSelectedItem();
        if (selected != null) {
            String path = selected.toString().trim();
            if (!path.isEmpty()) {
                loadTestSetPath(path);
            }
        }
    }

    private void loadTestSetPath(String path) {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            cfg.setProperty(ITSQ_PATH_KEY, path);
            cfg.save();
            lastValidSelection = path;
            loadItsqDirectory();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Verzeichnis nicht gefunden: " + path,
                    "Fehler", JOptionPane.ERROR_MESSAGE);

            updatingComboBox = true;
            try {
                testSetHistoryModel.removeElement(path);
                saveTestSetHistory();
                if (lastValidSelection != null) {
                    getComboBoxTestSet().setSelectedItem(lastValidSelection);
                } else if (testSetHistoryModel.getSize() > 0) {
                    getComboBoxTestSet().setSelectedIndex(0);
                }
            } finally {
                updatingComboBox = false;
            }
        }
    }

    // ===== Filter =====

    private String getFilterText() {
        Object selected = getComboBoxFilter().getSelectedItem();
        return selected != null ? selected.toString().trim() : "";
    }

    private void applyFilter() {
        if (updatingComboBox || !initialLoadDone) return;
        loadItsqDirectory();
    }

    @SuppressWarnings("unchecked")
    private void addToFilterHistory(String filterText) {
        if (filterText == null || filterText.isEmpty()) return;

        JComboBox<String> filterCombo = getComboBoxFilter();
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) filterCombo.getModel();

        for (int i = 0; i < model.getSize(); i++) {
            if (filterText.equals(model.getElementAt(i))) return;
        }

        model.insertElementAt(filterText, 0);
        if (model.getSize() > 20) {
            model.removeElementAt(model.getSize() - 1);
        }
    }

    // ===== Load Directory =====

    private void loadItsqDirectory() {
        File itsqDir = resolveItsqPath();

        if (!itsqDir.exists() || !itsqDir.isDirectory()) {
            TimelineLogger.warn(ItsqMainView.class, "ITSQ directory not found: {}", itsqDir.getAbsolutePath());
            JOptionPane.showMessageDialog(this,
                    "ITSQ-Verzeichnis nicht gefunden: " + itsqDir.getAbsolutePath(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Collect filter settings
        String filterText = getFilterText();
        boolean activeOnly = getCheckBoxActiveOnly().isSelected();
        String sourceFilter = (String) getComboBoxTestSetSource().getSelectedItem();
        String phaseFilter = (String) getComboBoxPhase().getSelectedItem();

        // Delegate to TreeView
        getPanelItsqTree().reload(itsqDir, filterText, activeOnly, sourceFilter, phaseFilter);

        // Show root card
        getPanelItsqView().showRootCard();

        // Update history
        String path = itsqDir.getAbsolutePath();
        addToTestSetHistory(path);
        lastValidSelection = path;
        addToFilterHistory(filterText);

        TimelineLogger.info(ItsqMainView.class, "Loaded ITSQ: {} ({} files, {} dirs, filter: '{}', source: '{}', phase: '{}')",
                path, getPanelItsqTree().getTotalFiles(), getPanelItsqTree().getTotalDirs(),
                filterText, sourceFilter, phaseFilter);
    }

    private File resolveItsqPath() {
        String configPath = cfg.getProperty(ITSQ_PATH_KEY);
        if (!configPath.isEmpty()) {
            File file = new File(configPath);
            if (file.exists()) return file;
        }

        String cfgFilePath = cfg.getFilePath();
        if (cfgFilePath != null) {
            File configDir = new File(cfgFilePath).getParentFile();
            if (configDir != null) {
                File itsqDir = new File(configDir, DEFAULT_ITSQ_PATH);
                if (itsqDir.exists()) return itsqDir;
            }
        }

        File targetTestfaelle = new File("target/testfaelle");
        if (targetTestfaelle.exists()) return targetTestfaelle;

        return new File(DEFAULT_ITSQ_PATH);
    }

    // ===== Actions =====

    private void browseItsqPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("ITSQ-Verzeichnis waehlen");

        File currentPath = resolveItsqPath();
        if (currentPath.exists()) {
            chooser.setCurrentDirectory(currentPath.getParentFile());
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            cfg.setProperty(ITSQ_PATH_KEY, selected.getAbsolutePath());
            cfg.save();
            loadItsqDirectory();
        }
    }

    // ===== Accessors =====

    public ItsqTreeView getTreeView() {
        return getPanelItsqTree();
    }

    public ItsqViewTabView getViewTabView() {
        return getPanelItsqView();
    }
}
