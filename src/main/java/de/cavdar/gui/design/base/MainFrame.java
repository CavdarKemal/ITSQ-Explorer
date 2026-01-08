package de.cavdar.gui.design.base;

import de.cavdar.gui.model.base.AppConfig;
import de.cavdar.gui.util.ConnectionManager;
import de.cavdar.gui.util.TestEnvironmentManager;
import de.cavdar.gui.util.TimelineLogger;
import de.cavdar.gui.view.base.BaseView;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Main MDI (Multiple Document Interface) application frame.
 * Simplified layout with two toolbars:
 * - Config toolbar: Configuration selector and all settings
 * - View toolbar: View buttons
 * - Main area: DesktopPanel with MDI internal frames
 *
 * @author TemplateGUI
 * @version 3.0
 */
public class MainFrame extends JFrame implements ConnectionManager.ConnectionListener {

    private final AppConfig cfg = AppConfig.getInstance();
    private DesktopPanel desktopPanel;

    // Toolbars
    private JToolBar configToolbar;
    private JToolBar viewToolbar;

    // Config toolbar components
    private JComboBox<String> configComboBox;
    private JComboBox<String> cbDbConnections;
    private JComboBox<String> cbSources;
    private JComboBox<String> cbTypes;
    private JComboBox<String> cbRevisions;
    private JCheckBox chkDump;
    private JCheckBox chkSftpUpload;
    private JCheckBox chkExportProtokoll;
    private JCheckBox chkUploadSynthetics;
    private JCheckBox chkOnlyTestClz;

    private File configDirectory;
    private String currentConfigName;
    private boolean isReloading = false;

    // View menu and registration
    private JMenu viewMenu;
    private final List<ViewRegistration> registeredViews = new ArrayList<>();
    private final Map<String, JMenu> groupMenus = new LinkedHashMap<>();

    /**
     * Holds registration info for a view type.
     */
    private record ViewRegistration(
            Supplier<BaseView> supplier,
            String menuLabel,
            String toolbarLabel,
            Icon icon,
            KeyStroke shortcut,
            String tooltip,
            String menuGroup
    ) {
    }

    /**
     * Constructs the main MDI frame with toolbar-based layout.
     */
    public MainFrame() {
        setTitle("MDI Application - " + cfg.getProperty("TEST-BASE-PATH"));
        initWindow();

        // Register for connection changes
        ConnectionManager.addListener(this);

        // Create desktop panel
        desktopPanel = new DesktopPanel();

        // Create toolbars
        configToolbar = createConfigToolbar();
        viewToolbar = createViewToolbar();

        // Toolbar panel (stacked vertically)
        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new BoxLayout(toolbarPanel, BoxLayout.Y_AXIS));
        toolbarPanel.add(configToolbar);
        toolbarPanel.add(viewToolbar);

        // Build menu bar
        setJMenuBar(createMenuBar());
        add(toolbarPanel, BorderLayout.NORTH);
        add(desktopPanel, BorderLayout.CENTER);

        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                TimelineLogger.info(MainFrame.class, "Application closing, saving window state");
                ConnectionManager.removeListener(MainFrame.this);
                cfg.setProperty("LAST_WINDOW_WIDTH", String.valueOf(getWidth()));
                cfg.setProperty("LAST_WINDOW_HEIGHT", String.valueOf(getHeight()));
                cfg.setProperty("LAST_WINDOW_X_POS", String.valueOf(getX()));
                cfg.setProperty("LAST_WINDOW_Y_POS", String.valueOf(getY()));
                cfg.save();
                System.exit(0);
            }
        });

        TimelineLogger.info(MainFrame.class, "MainFrame initialized with dual-toolbar layout");
    }

    /**
     * Creates the configuration toolbar with all settings.
     */
    private JToolBar createConfigToolbar() {
        JToolBar toolbar = new JToolBar("Config");
        toolbar.setFloatable(false);

        // === Config file selector ===
        initConfigSelector(toolbar);

        toolbar.addSeparator();

        // === DB Connection ===
        toolbar.add(new JLabel(" DB: "));
        cbDbConnections = new JComboBox<>(ConnectionManager.getConnectionNames());
        String lastConn = ConnectionManager.getLastConnectionName();
        if (!lastConn.isEmpty()) {
            cbDbConnections.setSelectedItem(lastConn);
        }
        cbDbConnections.setMaximumSize(new Dimension(150, 25));
        cbDbConnections.addActionListener(e -> {
            if (isReloading) return;
            String selected = (String) cbDbConnections.getSelectedItem();
            if (selected != null) {
                ConnectionManager.setLastConnectionName(selected);
            }
        });
        toolbar.add(cbDbConnections);

        JButton btnOpenDb = new JButton("\uD83D\uDDC4");
        btnOpenDb.setToolTipText("Datenbank-View oeffnen");
        btnOpenDb.setMargin(new Insets(2, 6, 2, 6));
        btnOpenDb.addActionListener(e -> {
            String conn = (String) cbDbConnections.getSelectedItem();
            desktopPanel.openOrShowDatabaseView(conn);
        });
        toolbar.add(btnOpenDb);

        toolbar.addSeparator();

        // === Test Sources ===
        toolbar.add(new JLabel(" Source: "));
        cbSources = new JComboBox<>(cfg.getArray("TEST-SOURCES"));
        cbSources.setSelectedItem(cfg.getProperty("LAST_TEST_SOURCE"));
        cbSources.setMaximumSize(new Dimension(120, 25));
        cbSources.addActionListener(e -> {
            if (isReloading) return;
            cfg.setProperty("LAST_TEST_SOURCE", (String) cbSources.getSelectedItem());
            cfg.save();
        });
        toolbar.add(cbSources);

        // === Test Types ===
        toolbar.add(new JLabel(" Type: "));
        cbTypes = new JComboBox<>(cfg.getArray("TEST-TYPES"));
        cbTypes.setSelectedItem(cfg.getProperty("LAST_TEST_TYPE"));
        cbTypes.setMaximumSize(new Dimension(150, 25));
        cbTypes.addActionListener(e -> {
            if (isReloading) return;
            cfg.setProperty("LAST_TEST_TYPE", (String) cbTypes.getSelectedItem());
            cfg.save();
        });
        toolbar.add(cbTypes);

        // === ITSQ Revisions ===
        toolbar.add(new JLabel(" Rev: "));
        cbRevisions = new JComboBox<>(cfg.getArray("ITSQ_REVISIONS"));
        cbRevisions.setSelectedItem(cfg.getProperty("LAST_ITSQ_REVISION"));
        cbRevisions.setMaximumSize(new Dimension(120, 25));
        cbRevisions.addActionListener(e -> {
            if (isReloading) return;
            cfg.setProperty("LAST_ITSQ_REVISION", (String) cbRevisions.getSelectedItem());
            cfg.save();
        });
        toolbar.add(cbRevisions);

        toolbar.addSeparator();

        // === Checkboxes ===
        chkDump = createToolbarCheckBox(toolbar, "Dump", "DUMP_IN_REST_CLIENT");
        chkSftpUpload = createToolbarCheckBox(toolbar, "SFTP", "SFTP_UPLOAD_ACTIVE");
        chkExportProtokoll = createToolbarCheckBox(toolbar, "Export", "CHECK-EXPORT-PROTOKOLL-ACTIVE");
        chkUploadSynthetics = createToolbarCheckBox(toolbar, "Synth", "LAST_UPLOAD_SYNTHETICS");
        chkOnlyTestClz = createToolbarCheckBox(toolbar, "TestClz", "LAST_USE_ONLY_TEST_CLZ");

        return toolbar;
    }

    /**
     * Creates the view toolbar for view buttons.
     */
    private JToolBar createViewToolbar() {
        JToolBar toolbar = new JToolBar("Views");
        toolbar.setFloatable(false);
        toolbar.add(new JLabel(" Views: "));
        return toolbar;
    }

    /**
     * Initializes the config selector in the toolbar.
     */
    private void initConfigSelector(JToolBar toolbar) {
        // Determine config directory
        File dockerConfigDir = new File("/app/config");
        if (dockerConfigDir.exists() && dockerConfigDir.isDirectory()) {
            configDirectory = dockerConfigDir;
        } else {
            configDirectory = new File(System.getProperty("user.dir"));
        }

        // Determine current config name
        String configPath = cfg.getFilePath();
        currentConfigName = new File(configPath).getName();

        toolbar.add(new JLabel(" Config: "));
        configComboBox = new JComboBox<>();
        configComboBox.setName("ConfigSelector");
        configComboBox.setMaximumSize(new Dimension(180, 25));
        configComboBox.setToolTipText("Konfigurationsdatei waehlen");

        refreshConfigList();

        // Initialize test environment for current config at startup
        if (!TestEnvironmentManager.switchEnvironment(currentConfigName)) {
            // Environment is locked - try to find an unlocked one
            String envName = currentConfigName.length() >= 3 ? currentConfigName.substring(0, 3).toUpperCase() : currentConfigName;
            boolean foundAlternative = false;

            // Try other configs
            for (int i = 0; i < configComboBox.getItemCount(); i++) {
                String altConfig = configComboBox.getItemAt(i);
                if (!altConfig.equals(currentConfigName)) {
                    if (TestEnvironmentManager.switchEnvironment(altConfig)) {
                        // Found unlocked environment
                        currentConfigName = altConfig;
                        configComboBox.setSelectedItem(altConfig);
                        cfg.loadFrom(new File(configDirectory, altConfig).getAbsolutePath());
                        setTitle("MDI Application - " + cfg.getProperty("TEST-BASE-PATH") + " [" + altConfig + "]");
                        foundAlternative = true;
                        JOptionPane.showMessageDialog(this,
                                "Die Umgebung '" + envName + "' ist bereits von einer anderen Instanz gesperrt.\n" +
                                "Gewechselt zu: " + altConfig.substring(0, 3).toUpperCase(),
                                "Umgebung gesperrt", JOptionPane.WARNING_MESSAGE);
                        break;
                    }
                }
            }

            if (!foundAlternative) {
                JOptionPane.showMessageDialog(this,
                        "Alle Umgebungen sind bereits von anderen Instanzen gesperrt.\n" +
                        "Bitte schliessen Sie eine andere Instanz zuerst.",
                        "Keine freie Umgebung", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }

        configComboBox.addActionListener(e -> {
            if (e.getActionCommand().equals("comboBoxChanged")) {
                String selected = (String) configComboBox.getSelectedItem();
                if (selected != null && !selected.equals(currentConfigName)) {
                    loadSelectedConfig(selected);
                }
            }
        });

        toolbar.add(configComboBox);

        JButton refreshBtn = new JButton("↻");
        refreshBtn.setToolTipText("Config-Liste aktualisieren");
        refreshBtn.setMargin(new Insets(2, 5, 2, 5));
        refreshBtn.addActionListener(e -> refreshConfigList());
        toolbar.add(refreshBtn);
    }

    private JCheckBox createToolbarCheckBox(JToolBar toolbar, String label, String propertyKey) {
        JCheckBox cb = new JCheckBox(label, cfg.getBool(propertyKey));
        cb.setToolTipText(propertyKey);
        cb.addActionListener(e -> {
            if (isReloading) return;
            cfg.setProperty(propertyKey, String.valueOf(cb.isSelected()));
            cfg.save();
        });
        toolbar.add(cb);
        return cb;
    }

    /**
     * Registers a view type for menu and optional toolbar access.
     */
    public void registerView(Supplier<BaseView> viewSupplier) {
        BaseView tempView = viewSupplier.get();

        ViewRegistration reg = new ViewRegistration(
                viewSupplier,
                tempView.getMenuLabel(),
                tempView.getToolbarLabel(),
                tempView.getIcon(),
                tempView.getKeyboardShortcut(),
                tempView.getToolbarTooltip(),
                tempView.getMenuGroup()
        );

        tempView.dispose();
        registeredViews.add(reg);

        addViewToMenu(reg);

        if (reg.toolbarLabel() != null) {
            addViewToToolbar(reg);
        }

        TimelineLogger.info(MainFrame.class, "Registered view: {}", reg.menuLabel());
    }

    private void addViewToMenu(ViewRegistration reg) {
        JMenuItem item = new JMenuItem(reg.menuLabel());
        item.setName(reg.menuLabel());
        if (reg.icon() != null) {
            item.setIcon(reg.icon());
        }
        if (reg.shortcut() != null) {
            item.setAccelerator(reg.shortcut());
        }
        item.addActionListener(e -> desktopPanel.openView(reg.supplier().get()));

        if (reg.menuGroup() != null && !reg.menuGroup().isEmpty()) {
            JMenu groupMenu = groupMenus.computeIfAbsent(reg.menuGroup(), groupName -> {
                JMenu newMenu = new JMenu(groupName);
                int insertPos = Math.max(0, viewMenu.getMenuComponentCount() - 2);
                viewMenu.insert(newMenu, insertPos);
                return newMenu;
            });
            groupMenu.add(item);
        } else {
            int insertPos = Math.max(0, viewMenu.getMenuComponentCount() - 2);
            viewMenu.insert(item, insertPos);
        }
    }

    private void addViewToToolbar(ViewRegistration reg) {
        JButton btn;
        if (reg.icon() != null) {
            btn = new JButton(reg.icon());
            btn.setText(reg.toolbarLabel());
        } else {
            btn = new JButton(reg.toolbarLabel());
        }
        btn.setName(reg.toolbarLabel());
        if (reg.tooltip() != null) {
            btn.setToolTipText(reg.tooltip());
        }
        btn.addActionListener(e -> desktopPanel.openView(reg.supplier().get()));
        viewToolbar.add(btn);
    }

    private void initWindow() {
        try {
            String lafClass = cfg.getProperty("LAST_LOOK_AND_FEEL_CLASS");
            if (!lafClass.isEmpty()) {
                UIManager.setLookAndFeel(lafClass);
                TimelineLogger.debug(MainFrame.class, "Look and Feel set to: {}", lafClass);
            }
        } catch (Exception e) {
            TimelineLogger.warn(MainFrame.class, "Could not set Look and Feel, using default", e);
        }

        int w = cfg.getInt("LAST_WINDOW_WIDTH", 1200);
        int h = cfg.getInt("LAST_WINDOW_HEIGHT", 800);
        int x = cfg.getInt("LAST_WINDOW_X_POS", 100);
        int y = cfg.getInt("LAST_WINDOW_Y_POS", 100);

        Rectangle validBounds = validateWindowBounds(x, y, w, h);

        setBounds(validBounds);
        setResizable(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private Rectangle validateWindowBounds(int x, int y, int width, int height) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle virtualBounds = new Rectangle();

        for (GraphicsDevice gd : ge.getScreenDevices()) {
            for (GraphicsConfiguration gc : gd.getConfigurations()) {
                virtualBounds = virtualBounds.union(gc.getBounds());
            }
        }

        width = Math.max(width, 400);
        height = Math.max(height, 300);

        Rectangle windowBounds = new Rectangle(x, y, width, height);
        if (!virtualBounds.intersects(windowBounds)) {
            x = 100;
            y = 100;
        }

        if (x < virtualBounds.x) x = virtualBounds.x;
        if (y < virtualBounds.y) y = virtualBounds.y;
        if (x + width > virtualBounds.x + virtualBounds.width) {
            x = Math.max(virtualBounds.x, virtualBounds.x + virtualBounds.width - width);
        }
        if (y + height > virtualBounds.y + virtualBounds.height) {
            y = Math.max(virtualBounds.y, virtualBounds.y + virtualBounds.height - height);
        }

        return new Rectangle(x, y, width, height);
    }

    private JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();

        JMenu fileMenu = new JMenu("Datei");
        fileMenu.setName("Datei");
        viewMenu = fileMenu;

        fileMenu.addSeparator();
        JMenuItem itemExit = new JMenuItem("Beenden");
        itemExit.setName("Beenden");
        itemExit.addActionListener(e -> {
            dispatchEvent(new java.awt.event.WindowEvent(this, java.awt.event.WindowEvent.WINDOW_CLOSING));
        });
        fileMenu.add(itemExit);

        mb.add(fileMenu);

        JMenu windowMenu = new JMenu("Fenster");
        windowMenu.setName("Fenster");

        JMenuItem itemCascade = new JMenuItem("Kaskadiert anordnen");
        itemCascade.addActionListener(e -> desktopPanel.layoutCascaded());
        windowMenu.add(itemCascade);

        JMenuItem itemTileHor = new JMenuItem("Nebeneinander anordnen");
        itemTileHor.addActionListener(e -> desktopPanel.layoutTileVertical());
        windowMenu.add(itemTileHor);

        JMenuItem itemTileVer = new JMenuItem("Untereinander anordnen");
        itemTileVer.addActionListener(e -> desktopPanel.layoutTileHorizontal());
        windowMenu.add(itemTileVer);

        mb.add(windowMenu);
        return mb;
    }

    // === Config Management ===

    private void refreshConfigList() {
        String previousSelection = (String) configComboBox.getSelectedItem();
        configComboBox.removeAllItems();

        File[] configFiles = configDirectory.listFiles((dir, name) ->
                name.toLowerCase().endsWith("-config.properties"));

        if (configFiles != null) {
            java.util.Arrays.sort(configFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (File file : configFiles) {
                configComboBox.addItem(file.getName());
            }
        }

        if (currentConfigName != null && containsItem(configComboBox, currentConfigName)) {
            configComboBox.setSelectedItem(currentConfigName);
        } else if (previousSelection != null && containsItem(configComboBox, previousSelection)) {
            configComboBox.setSelectedItem(previousSelection);
        }
    }

    private boolean containsItem(JComboBox<String> combo, String item) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (item.equals(combo.getItemAt(i))) return true;
        }
        return false;
    }

    /**
     * Saves current UI settings to the active config file.
     * Called before switching to a different config file.
     */
    private void saveCurrentSettings() {
        TimelineLogger.info(MainFrame.class, "Saving current settings before config switch");

        // Save DB connection selection
        String dbConnection = (String) cbDbConnections.getSelectedItem();
        if (dbConnection != null) {
            ConnectionManager.setLastConnectionName(dbConnection);
        }

        // Save ComboBox selections
        if (cbSources.getSelectedItem() != null) {
            cfg.setProperty("LAST_TEST_SOURCE", (String) cbSources.getSelectedItem());
        }
        if (cbTypes.getSelectedItem() != null) {
            cfg.setProperty("LAST_TEST_TYPE", (String) cbTypes.getSelectedItem());
        }
        if (cbRevisions.getSelectedItem() != null) {
            cfg.setProperty("LAST_ITSQ_REVISION", (String) cbRevisions.getSelectedItem());
        }

        // Save checkbox states
        cfg.setProperty("DUMP_IN_REST_CLIENT", String.valueOf(chkDump.isSelected()));
        cfg.setProperty("SFTP_UPLOAD_ACTIVE", String.valueOf(chkSftpUpload.isSelected()));
        cfg.setProperty("CHECK-EXPORT-PROTOKOLL-ACTIVE", String.valueOf(chkExportProtokoll.isSelected()));
        cfg.setProperty("LAST_UPLOAD_SYNTHETICS", String.valueOf(chkUploadSynthetics.isSelected()));
        cfg.setProperty("LAST_USE_ONLY_TEST_CLZ", String.valueOf(chkOnlyTestClz.isSelected()));

        cfg.save();
    }

    private void loadSelectedConfig(String configName) {
        // Save current settings to OLD config before switching
        saveCurrentSettings();

        File configFile = new File(configDirectory, configName);
        TimelineLogger.info(MainFrame.class, "Loading configuration: {}", configFile.getAbsolutePath());

        if (cfg.loadFrom(configFile.getAbsolutePath())) {
            // Switch test environment (creates directories, acquires lock, configures logging)
            if (!TestEnvironmentManager.switchEnvironment(configName)) {
                // Environment is locked by another instance
                String envName = configName.length() >= 3 ? configName.substring(0, 3).toUpperCase() : configName;
                JOptionPane.showMessageDialog(this,
                        "Die Umgebung '" + envName + "' ist bereits von einer anderen Instanz gesperrt.\n" +
                        "Bitte schliessen Sie die andere Instanz zuerst.",
                        "Umgebung gesperrt", JOptionPane.WARNING_MESSAGE);
                // Reload old config
                cfg.loadFrom(new File(configDirectory, currentConfigName).getAbsolutePath());
                configComboBox.setSelectedItem(currentConfigName);
                return;
            }

            currentConfigName = configName;
            setTitle("MDI Application - " + cfg.getProperty("TEST-BASE-PATH") + " [" + configName + "]");

            reloadAllSettings();
            TimelineLogger.info(MainFrame.class, "Configuration loaded: {}", configName);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Laden der Konfiguration:\n" + configFile.getAbsolutePath(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            configComboBox.setSelectedItem(currentConfigName);
        }
    }

    /**
     * Reloads all settings when config changes.
     */
    private void reloadAllSettings() {
        TimelineLogger.info(MainFrame.class, "Reloading all settings");
        isReloading = true;
        try {
            // Reload DB connections from new config
            ConnectionManager.reloadConnections();

            // Explicitly reload DB connections ComboBox with value from NEW config
            if (cbDbConnections != null) {
                cbDbConnections.removeAllItems();
                for (String name : ConnectionManager.getConnectionNames()) {
                    cbDbConnections.addItem(name);
                }
                String lastConn = ConnectionManager.getLastConnectionName();
                if (!lastConn.isEmpty()) {
                    cbDbConnections.setSelectedItem(lastConn);
                }
                TimelineLogger.info(MainFrame.class, "DB connection set to: {}", lastConn);
            }

            // Reload comboboxes
            if (cbSources != null) {
                cbSources.removeAllItems();
                for (String s : cfg.getArray("TEST-SOURCES")) cbSources.addItem(s);
                cbSources.setSelectedItem(cfg.getProperty("LAST_TEST_SOURCE"));
            }

            if (cbTypes != null) {
                cbTypes.removeAllItems();
                for (String t : cfg.getArray("TEST-TYPES")) cbTypes.addItem(t);
                cbTypes.setSelectedItem(cfg.getProperty("LAST_TEST_TYPE"));
            }

            if (cbRevisions != null) {
                cbRevisions.removeAllItems();
                for (String r : cfg.getArray("ITSQ_REVISIONS")) cbRevisions.addItem(r);
                cbRevisions.setSelectedItem(cfg.getProperty("LAST_ITSQ_REVISION"));
            }

            // Reload checkboxes
            if (chkDump != null) chkDump.setSelected(cfg.getBool("DUMP_IN_REST_CLIENT"));
            if (chkSftpUpload != null) chkSftpUpload.setSelected(cfg.getBool("SFTP_UPLOAD_ACTIVE"));
            if (chkExportProtokoll != null)
                chkExportProtokoll.setSelected(cfg.getBool("CHECK-EXPORT-PROTOKOLL-ACTIVE"));
            if (chkUploadSynthetics != null) chkUploadSynthetics.setSelected(cfg.getBool("LAST_UPLOAD_SYNTHETICS"));
            if (chkOnlyTestClz != null) chkOnlyTestClz.setSelected(cfg.getBool("LAST_USE_ONLY_TEST_CLZ"));
        } finally {
            isReloading = false;
        }
    }

    @Override
    public void onConnectionsChanged() {
        SwingUtilities.invokeLater(() -> {
            if (cbDbConnections == null) return;
            String current = (String) cbDbConnections.getSelectedItem();
            cbDbConnections.removeAllItems();
            for (String name : ConnectionManager.getConnectionNames()) {
                cbDbConnections.addItem(name);
            }
            if (current != null) {
                cbDbConnections.setSelectedItem(current);
            } else {
                String last = ConnectionManager.getLastConnectionName();
                if (!last.isEmpty()) cbDbConnections.setSelectedItem(last);
            }
        });
    }

    // === Getters ===

    public DesktopPanel getDesktopPanel() {
        return desktopPanel;
    }
}
