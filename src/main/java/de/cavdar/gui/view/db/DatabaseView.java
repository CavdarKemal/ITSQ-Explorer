package de.cavdar.gui.view.db;
import de.cavdar.gui.view.base.BaseView;

import de.cavdar.gui.design.base.BaseViewPanel;
import de.cavdar.gui.design.db.DatabaseViewPanel;
import de.cavdar.gui.model.base.AppConfig;
import de.cavdar.gui.model.base.ConnectionInfo;
import de.cavdar.gui.util.AppConstants;
import de.cavdar.gui.util.ConnectionManager;

import static de.cavdar.gui.util.AppConstants.*;
import de.cavdar.gui.util.TimelineLogger;


import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Vector;

/**
 * Database view with generic JDBC support and connection management.
 * Uses DatabaseViewPanel for GUI, this class contains only logic.
 * <p>
 * Pattern:
 * - DatabaseViewPanel: GUI only (can be GUI designer generated)
 * - DatabaseView: Logic and event handlers only
 *
 * @author StandardMDIGUI
 * @version 2.0
 * @since 2024-12-25
 */
public class DatabaseView extends BaseView implements ConnectionManager.ConnectionListener {

    private static final int MAX_HISTORY_SIZE = 20;

    private DatabaseViewPanel dbPanel;
    private Connection connection;
    private String preselectedConnection;
    private final AppConfig cfg = AppConfig.getInstance();

    /**
     * Constructs a new DatabaseView.
     */
    public DatabaseView() {
        this(null);
    }

    /**
     * Constructs a new DatabaseView with a preselected connection.
     *
     * @param connectionName the connection name to preselect, or null for default
     */
    public DatabaseView(String connectionName) {
        super("Datenbank");
        setSize(900, 700);
        this.preselectedConnection = connectionName;

        // Load saved connections after panel is created
        loadSavedConnections();

        // Load SQL history/favorites
        loadSqlHistory();

        // Preselect connection if specified
        if (connectionName != null && !connectionName.isEmpty()) {
            dbPanel.getConnectionsComboBox().setSelectedItem(connectionName);
        }

        // Register for connection changes
        ConnectionManager.addListener(this);

        TimelineLogger.debug(DatabaseView.class, "DatabaseView created");
    }

    @Override
    protected BaseViewPanel createPanel() {
        dbPanel = new DatabaseViewPanel();
        return dbPanel;
    }

    @Override
    protected void setupToolbarActions() {
        // Toolbar is empty for DatabaseView, all controls are in the panel
    }

    @Override
    protected void setupListeners() {
        // Connection selection
        dbPanel.getConnectionsComboBox().addActionListener(e -> onConnectionSelected());

        // Connection buttons
        dbPanel.getConnectButton().addActionListener(e -> toggleConnection());
        dbPanel.getSaveButton().addActionListener(e -> saveCurrentConnection());
        dbPanel.getDeleteButton().addActionListener(e -> deleteCurrentConnection());

        // Query buttons
        dbPanel.getExecuteButton().addActionListener(e -> executeQuery());
        dbPanel.getClearButton().addActionListener(e -> clearQuery());

        // SQL History/Favorites
        dbPanel.getSqlHistoryComboBox().addActionListener(e -> onHistorySelected());
        dbPanel.getAddFavoriteButton().addActionListener(e -> addToFavorites());
        dbPanel.getRemoveFavoriteButton().addActionListener(e -> removeFromHistory());

        // Export buttons
        dbPanel.getExportCsvButton().addActionListener(e -> exportToCsv());
        dbPanel.getExportExcelButton().addActionListener(e -> exportToExcel());

        // Table tree selection
        dbPanel.getTableTree().addTreeSelectionListener(e -> onTableSelected());

        // Table tree expansion (lazy load columns)
        dbPanel.getTableTree().addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                onTreeWillExpand(event.getPath());
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                // Nothing to do on collapse
            }
        });
    }

    @Override
    public void onConnectionsChanged() {
        // Reload connections when they change externally
        SwingUtilities.invokeLater(this::loadSavedConnections);
    }

    // ===== Connection Logic =====

    private void onConnectionSelected() {
        String selected = (String) dbPanel.getConnectionsComboBox().getSelectedItem();
        if (selected == null || selected.equals(NEW_CONNECTION)) {
            // Close existing connection when selecting "Neue Verbindung"
            if (connection != null) {
                disconnect();
                TimelineLogger.info(DatabaseView.class, "Closed existing connection for new connection setup");
            }
            // Clear fields for new connection
            dbPanel.getConnectionNameField().setText("");
            dbPanel.getConnectionNameField().setEditable(true);
            dbPanel.getDriverComboBox().setSelectedIndex(0);
            dbPanel.getUrlField().setText("jdbc:postgresql://localhost:5432/postgres");
            dbPanel.getUsernameField().setText("postgres");
            dbPanel.getPasswordField().setText("");
            dbPanel.getDeleteButton().setEnabled(false);
        } else {
            // Load selected connection from ConnectionManager
            ConnectionInfo conn = ConnectionManager.getConnection(selected);
            if (conn != null) {
                dbPanel.getConnectionNameField().setText(conn.getName());
                dbPanel.getConnectionNameField().setEditable(false);
                dbPanel.getDriverComboBox().setSelectedItem(conn.getDriver());
                dbPanel.getUrlField().setText(conn.getUrl());
                dbPanel.getUsernameField().setText(conn.getUsername());
                dbPanel.getPasswordField().setText(conn.getPassword());
                dbPanel.getDeleteButton().setEnabled(true);
            }
        }
    }

    private void loadSavedConnections() {
        JComboBox<String> cb = dbPanel.getConnectionsComboBox();
        String currentSelection = (String) cb.getSelectedItem();

        cb.removeAllItems();
        cb.addItem(NEW_CONNECTION);

        List<ConnectionInfo> connections = ConnectionManager.getConnections();
        for (ConnectionInfo conn : connections) {
            cb.addItem(conn.getName());
        }

        // Restore selection or select first connection
        if (currentSelection != null && !currentSelection.equals(NEW_CONNECTION)) {
            cb.setSelectedItem(currentSelection);
        } else if (connections.size() > 0) {
            cb.setSelectedIndex(1);
        }

        TimelineLogger.debug(DatabaseView.class, "Loaded {} saved connections", connections.size());
    }

    private void saveCurrentConnection() {
        String name = dbPanel.getConnectionNameField().getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bitte geben Sie einen Namen für die Verbindung ein.",
                    "Name erforderlich", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String driver = (String) dbPanel.getDriverComboBox().getSelectedItem();
        String url = dbPanel.getUrlField().getText().trim();
        String username = dbPanel.getUsernameField().getText().trim();
        String password = new String(dbPanel.getPasswordField().getPassword());

        ConnectionInfo conn = new ConnectionInfo(name, driver, url, username, password);
        ConnectionManager.saveConnection(conn);

        // Reload and select
        loadSavedConnections();
        dbPanel.getConnectionsComboBox().setSelectedItem(name);
        dbPanel.getConnectionNameField().setEditable(false);
        dbPanel.getDeleteButton().setEnabled(true);

        JOptionPane.showMessageDialog(this, "Verbindung '" + name + "' wurde gespeichert.",
                "Gespeichert", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteCurrentConnection() {
        String selected = (String) dbPanel.getConnectionsComboBox().getSelectedItem();
        if (selected == null || selected.equals(NEW_CONNECTION)) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Verbindung '" + selected + "' wirklich löschen?",
                "Löschen bestätigen", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            ConnectionManager.deleteConnection(selected);
            loadSavedConnections();
            dbPanel.getConnectionsComboBox().setSelectedItem(NEW_CONNECTION);
        }
    }

    private void toggleConnection() {
        if (connection == null) {
            connect();
        } else {
            disconnect();
        }
    }

    private void connect() {
        String driver = (String) dbPanel.getDriverComboBox().getSelectedItem();
        String url = dbPanel.getUrlField().getText().trim();
        String username = dbPanel.getUsernameField().getText().trim();
        String password = new String(dbPanel.getPasswordField().getPassword());

        executeTask(() -> {
            try {
                TimelineLogger.info(DatabaseView.class, "Connecting to database: {}", url);
                Class.forName(driver);
                connection = DriverManager.getConnection(url, username, password);

                SwingUtilities.invokeLater(() -> {
                    dbPanel.getStatusLabel().setText("Verbunden: " + url);
                    dbPanel.getStatusLabel().setForeground(new Color(0, 128, 0));
                    dbPanel.getConnectButton().setText("Trennen");
                    dbPanel.getExecuteButton().setEnabled(true);
                    loadTables();
                    TimelineLogger.info(DatabaseView.class, "Database connection established");
                });
            } catch (ClassNotFoundException e) {
                TimelineLogger.error(DatabaseView.class, "JDBC driver not found: {}", driver, e);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(DatabaseView.this,
                            "JDBC-Treiber nicht gefunden: " + driver + "\n\n" +
                                    "Bitte stellen Sie sicher, dass die Treiber-JAR im Classpath ist.",
                            "Treiber-Fehler",
                            JOptionPane.ERROR_MESSAGE);
                });
            } catch (SQLException e) {
                TimelineLogger.error(DatabaseView.class, "Database connection failed", e);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(DatabaseView.this,
                            "Verbindungsfehler: " + e.getMessage(),
                            "Verbindungsfehler",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                TimelineLogger.info(DatabaseView.class, "Database connection closed");
            }
        } catch (SQLException e) {
            TimelineLogger.error(DatabaseView.class, "Error closing database connection", e);
        } finally {
            connection = null;
            dbPanel.getStatusLabel().setText("Nicht verbunden");
            dbPanel.getStatusLabel().setForeground(Color.RED);
            dbPanel.getConnectButton().setText("Verbinden");
            dbPanel.getExecuteButton().setEnabled(false);
            clearTables();
        }
    }

    // ===== Table Tree Logic =====

    private void loadTables() {
        DefaultMutableTreeNode root = dbPanel.getTableRootNode();
        root.removeAllChildren();
        root.setUserObject("Datenbank");

        try {
            DatabaseMetaData meta = connection.getMetaData();
            String catalog = connection.getCatalog();
            String schema = connection.getSchema();

            // Create nodes for different table types
            DefaultMutableTreeNode tablesNode = new DefaultMutableTreeNode("Tabellen");
            DefaultMutableTreeNode viewsNode = new DefaultMutableTreeNode("Views");

            // Load tables
            try (ResultSet rs = meta.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    DefaultMutableTreeNode tableNode = new DefaultMutableTreeNode(tableName);
                    // Add dummy node for lazy loading of columns
                    tableNode.add(new DefaultMutableTreeNode(LOADING_NODE));
                    tablesNode.add(tableNode);
                }
            }

            // Load views
            try (ResultSet rs = meta.getTables(catalog, schema, "%", new String[]{"VIEW"})) {
                while (rs.next()) {
                    String viewName = rs.getString("TABLE_NAME");
                    DefaultMutableTreeNode viewNode = new DefaultMutableTreeNode(viewName);
                    // Add dummy node for lazy loading of columns
                    viewNode.add(new DefaultMutableTreeNode(LOADING_NODE));
                    viewsNode.add(viewNode);
                }
            }

            // Only add nodes if they have children
            if (tablesNode.getChildCount() > 0) {
                root.add(tablesNode);
            }
            if (viewsNode.getChildCount() > 0) {
                root.add(viewsNode);
            }

            dbPanel.getTableTreeModel().reload();
            expandTableTree();

            TimelineLogger.info(DatabaseView.class, "Loaded {} tables and {} views",
                    tablesNode.getChildCount(), viewsNode.getChildCount());

        } catch (SQLException e) {
            TimelineLogger.error(DatabaseView.class, "Failed to load tables", e);
            root.add(new DefaultMutableTreeNode("Fehler: " + e.getMessage()));
            dbPanel.getTableTreeModel().reload();
        }
    }

    private void clearTables() {
        DefaultMutableTreeNode root = dbPanel.getTableRootNode();
        root.removeAllChildren();
        root.setUserObject("Datenbank");
        dbPanel.getTableTreeModel().reload();
    }

    private void expandTableTree() {
        JTree tree = dbPanel.getTableTree();
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    /**
     * Called when a tree node is about to be expanded.
     * Loads columns for table/view nodes on demand.
     */
    private void onTreeWillExpand(TreePath path) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (node == null || node.getChildCount() == 0) {
            return;
        }

        // Check if this node has only the dummy "Laden..." child
        DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) node.getFirstChild();
        if (!LOADING_NODE.equals(firstChild.getUserObject())) {
            return; // Already loaded
        }

        // Check if parent is "Tabellen" or "Views"
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        if (parent == null) {
            return;
        }

        String parentName = parent.toString();
        if ("Tabellen".equals(parentName) || "Views".equals(parentName)) {
            String tableName = node.toString();
            loadColumns(node, tableName);
        }
    }

    /**
     * Loads column information for a table or view.
     */
    private void loadColumns(DefaultMutableTreeNode tableNode, String tableName) {
        if (connection == null) {
            return;
        }

        try {
            DatabaseMetaData meta = connection.getMetaData();
            String catalog = connection.getCatalog();
            String schema = connection.getSchema();

            // Remove dummy node
            tableNode.removeAllChildren();

            // Load columns
            try (ResultSet rs = meta.getColumns(catalog, schema, tableName, "%")) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    String typeName = rs.getString("TYPE_NAME");
                    int columnSize = rs.getInt("COLUMN_SIZE");
                    int nullable = rs.getInt("NULLABLE");

                    // Format: column_name (TYPE, size) or column_name (TYPE, size, NULL)
                    StringBuilder columnInfo = new StringBuilder();
                    columnInfo.append(columnName).append(" (").append(typeName);
                    if (columnSize > 0) {
                        columnInfo.append(", ").append(columnSize);
                    }
                    if (nullable == DatabaseMetaData.columnNullable) {
                        columnInfo.append(", NULL");
                    }
                    columnInfo.append(")");

                    tableNode.add(new DefaultMutableTreeNode(columnInfo.toString()));
                }
            }

            // Load primary keys and mark them
            try (ResultSet rs = meta.getPrimaryKeys(catalog, schema, tableName)) {
                while (rs.next()) {
                    String pkColumn = rs.getString("COLUMN_NAME");
                    // Find and update the column node with PK marker
                    for (int i = 0; i < tableNode.getChildCount(); i++) {
                        DefaultMutableTreeNode colNode = (DefaultMutableTreeNode) tableNode.getChildAt(i);
                        String colInfo = colNode.toString();
                        if (colInfo.startsWith(pkColumn + " ")) {
                            colNode.setUserObject("🔑 " + colInfo);
                            break;
                        }
                    }
                }
            }

            dbPanel.getTableTreeModel().nodeStructureChanged(tableNode);
            TimelineLogger.debug(DatabaseView.class, "Loaded {} columns for table {}", tableNode.getChildCount(), tableName);

        } catch (SQLException e) {
            TimelineLogger.error(DatabaseView.class, "Failed to load columns for table: {}", tableName, e);
            tableNode.removeAllChildren();
            tableNode.add(new DefaultMutableTreeNode("Fehler: " + e.getMessage()));
            dbPanel.getTableTreeModel().nodeStructureChanged(tableNode);
        }
    }

    private void onTableSelected() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                dbPanel.getTableTree().getLastSelectedPathComponent();

        if (node == null || node.getParent() == null) {
            return;
        }

        // Check if this is a table or view node (child of "Tabellen" or "Views")
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        String parentName = parent.toString();

        if ("Tabellen".equals(parentName) || "Views".equals(parentName)) {
            String tableName = node.toString();
            dbPanel.getQueryArea().setText("SELECT * FROM " + tableName);
            TimelineLogger.debug(DatabaseView.class, "Selected table/view: {}", tableName);
        }
    }

    // ===== Query Logic =====

    private void executeQuery() {
        String sql = dbPanel.getQueryArea().getText().trim();
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bitte geben Sie eine SQL-Abfrage ein.");
            return;
        }

        executeTask(() -> {
            long startTime = System.currentTimeMillis();
            TimelineLogger.info(DatabaseView.class, "Executing SQL: {}", sql);

            try (Statement stmt = connection.createStatement()) {
                boolean isResultSet = stmt.execute(sql);

                if (isResultSet) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        ResultSetMetaData meta = rs.getMetaData();
                        int columnCount = meta.getColumnCount();

                        Vector<String> columnNames = new Vector<>();
                        for (int i = 1; i <= columnCount; i++) {
                            columnNames.add(meta.getColumnLabel(i));
                        }

                        Vector<Vector<Object>> data = new Vector<>();
                        while (rs.next()) {
                            Vector<Object> row = new Vector<>();
                            for (int i = 1; i <= columnCount; i++) {
                                row.add(rs.getObject(i));
                            }
                            data.add(row);
                        }

                        long duration = System.currentTimeMillis() - startTime;
                        int rowCount = data.size();

                        SwingUtilities.invokeLater(() -> {
                            dbPanel.getTableModel().setDataVector(data, columnNames);
                            dbPanel.getRowCountLabel().setText(rowCount + " Zeilen (" + duration + " ms)");
                            // Enable export buttons when data is available
                            dbPanel.getExportCsvButton().setEnabled(rowCount > 0);
                            dbPanel.getExportExcelButton().setEnabled(rowCount > 0);
                            // Add to history on successful execution
                            addToHistory(sql);
                            TimelineLogger.info(DatabaseView.class, "Query returned {} rows in {} ms", rowCount, duration);
                        });
                    }
                } else {
                    int updateCount = stmt.getUpdateCount();
                    long duration = System.currentTimeMillis() - startTime;

                    SwingUtilities.invokeLater(() -> {
                        dbPanel.getTableModel().setRowCount(0);
                        dbPanel.getTableModel().setColumnCount(0);
                        dbPanel.getRowCountLabel().setText(updateCount + " Zeilen betroffen (" + duration + " ms)");
                        dbPanel.getExportCsvButton().setEnabled(false);
                        dbPanel.getExportExcelButton().setEnabled(false);
                        // Add to history on successful execution
                        addToHistory(sql);
                        JOptionPane.showMessageDialog(DatabaseView.this,
                                updateCount + " Zeilen wurden aktualisiert.",
                                "Ausführung erfolgreich",
                                JOptionPane.INFORMATION_MESSAGE);
                        TimelineLogger.info(DatabaseView.class, "Update affected {} rows in {} ms", updateCount, duration);
                    });
                }
            } catch (SQLException e) {
                TimelineLogger.error(DatabaseView.class, "SQL execution failed", e);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(DatabaseView.this,
                            "SQL-Fehler: " + e.getMessage(),
                            "Fehler",
                            JOptionPane.ERROR_MESSAGE);
                    dbPanel.getRowCountLabel().setText("Fehler: " + e.getMessage());
                    dbPanel.getExportCsvButton().setEnabled(false);
                    dbPanel.getExportExcelButton().setEnabled(false);
                });
            }
        });
    }

    private void clearQuery() {
        dbPanel.getQueryArea().setText("");
        dbPanel.getTableModel().setRowCount(0);
        dbPanel.getTableModel().setColumnCount(0);
        dbPanel.getRowCountLabel().setText("0 Zeilen");
        dbPanel.getExportCsvButton().setEnabled(false);
        dbPanel.getExportExcelButton().setEnabled(false);
    }

    // ===== SQL History/Favorites Logic =====

    private void loadSqlHistory() {
        JComboBox<String> cb = dbPanel.getSqlHistoryComboBox();
        cb.removeAllItems();
        cb.addItem(""); // Empty item for no selection

        // Load favorites first (with star prefix)
        String favoritesData = cfg.getProperty(SQL_FAVORITES_KEY);
        if (!favoritesData.isEmpty()) {
            String[] favorites = favoritesData.split(SQL_SEPARATOR);
            for (String sql : favorites) {
                if (!sql.trim().isEmpty()) {
                    cb.addItem(FAVORITE_PREFIX + sql.trim());
                }
            }
        }

        // Load history
        String historyData = cfg.getProperty(SQL_HISTORY_KEY);
        if (!historyData.isEmpty()) {
            String[] history = historyData.split(SQL_SEPARATOR);
            for (String sql : history) {
                if (!sql.trim().isEmpty()) {
                    cb.addItem(sql.trim());
                }
            }
        }

        TimelineLogger.debug(DatabaseView.class, "Loaded SQL history/favorites");
    }

    private void onHistorySelected() {
        String selected = (String) dbPanel.getSqlHistoryComboBox().getSelectedItem();
        if (selected != null && !selected.isEmpty()) {
            // Remove favorite prefix if present
            if (selected.startsWith(FAVORITE_PREFIX)) {
                selected = selected.substring(FAVORITE_PREFIX.length());
            }
            dbPanel.getQueryArea().setText(selected);
        }
    }

    private void addToFavorites() {
        String sql = dbPanel.getQueryArea().getText().trim();
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Bitte geben Sie zuerst eine SQL-Abfrage ein.",
                    "Keine Abfrage", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Load existing favorites
        String favoritesData = cfg.getProperty(SQL_FAVORITES_KEY);
        LinkedHashSet<String> favorites = new LinkedHashSet<>();
        if (!favoritesData.isEmpty()) {
            for (String fav : favoritesData.split(SQL_SEPARATOR)) {
                if (!fav.trim().isEmpty()) {
                    favorites.add(fav.trim());
                }
            }
        }

        // Add new favorite
        favorites.add(sql);

        // Save favorites
        String newFavorites = String.join(SQL_SEPARATOR, favorites);
        cfg.setProperty(SQL_FAVORITES_KEY, newFavorites);
        cfg.save();

        // Reload history ComboBox
        loadSqlHistory();

        JOptionPane.showMessageDialog(this,
                "Abfrage wurde zu Favoriten hinzugefügt.",
                "Favorit gespeichert", JOptionPane.INFORMATION_MESSAGE);
        TimelineLogger.info(DatabaseView.class, "Added SQL to favorites: {}", sql.substring(0, Math.min(50, sql.length())));
    }

    private void removeFromHistory() {
        String selected = (String) dbPanel.getSqlHistoryComboBox().getSelectedItem();
        if (selected == null || selected.isEmpty()) {
            return;
        }

        boolean isFavorite = selected.startsWith(FAVORITE_PREFIX);
        String sql = isFavorite ? selected.substring(FAVORITE_PREFIX.length()) : selected;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Eintrag wirklich entfernen?\n" + sql.substring(0, Math.min(80, sql.length())) + "...",
                "Entfernen bestätigen", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        if (isFavorite) {
            // Remove from favorites
            String favoritesData = cfg.getProperty(SQL_FAVORITES_KEY);
            List<String> favorites = new ArrayList<>();
            for (String fav : favoritesData.split(SQL_SEPARATOR)) {
                if (!fav.trim().isEmpty() && !fav.trim().equals(sql)) {
                    favorites.add(fav.trim());
                }
            }
            cfg.setProperty(SQL_FAVORITES_KEY, String.join(SQL_SEPARATOR, favorites));
        } else {
            // Remove from history
            String historyData = cfg.getProperty(SQL_HISTORY_KEY);
            List<String> history = new ArrayList<>();
            for (String h : historyData.split(SQL_SEPARATOR)) {
                if (!h.trim().isEmpty() && !h.trim().equals(sql)) {
                    history.add(h.trim());
                }
            }
            cfg.setProperty(SQL_HISTORY_KEY, String.join(SQL_SEPARATOR, history));
        }

        cfg.save();
        loadSqlHistory();
        TimelineLogger.info(DatabaseView.class, "Removed SQL from {}: {}", isFavorite ? "favorites" : "history",
                sql.substring(0, Math.min(50, sql.length())));
    }

    private void addToHistory(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return;
        }
        sql = sql.trim();

        // Load existing history
        String historyData = cfg.getProperty(SQL_HISTORY_KEY);
        LinkedHashSet<String> history = new LinkedHashSet<>();
        history.add(sql); // Add new one first (most recent)

        if (!historyData.isEmpty()) {
            for (String h : historyData.split(SQL_SEPARATOR)) {
                if (!h.trim().isEmpty() && !h.trim().equals(sql)) {
                    history.add(h.trim());
                }
            }
        }

        // Limit history size
        List<String> historyList = new ArrayList<>(history);
        if (historyList.size() > MAX_HISTORY_SIZE) {
            historyList = historyList.subList(0, MAX_HISTORY_SIZE);
        }

        // Save history
        cfg.setProperty(SQL_HISTORY_KEY, String.join(SQL_SEPARATOR, historyList));
        cfg.save();

        // Reload history ComboBox
        loadSqlHistory();
    }

    // ===== Export Logic =====

    private void exportToCsv() {
        DefaultTableModel model = dbPanel.getTableModel();
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Keine Daten zum Exportieren vorhanden.",
                    "Keine Daten", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("CSV exportieren");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV-Dateien (*.csv)", "csv"));
        chooser.setSelectedFile(new File("export.csv"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {

            // Write BOM for Excel compatibility
            writer.print('\ufeff');

            // Write header
            int columnCount = model.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                if (i > 0) writer.print(';');
                writer.print(escapeCsv(model.getColumnName(i)));
            }
            writer.println();

            // Write data
            int rowCount = model.getRowCount();
            for (int row = 0; row < rowCount; row++) {
                for (int col = 0; col < columnCount; col++) {
                    if (col > 0) writer.print(';');
                    Object value = model.getValueAt(row, col);
                    writer.print(escapeCsv(value != null ? value.toString() : ""));
                }
                writer.println();
            }

            JOptionPane.showMessageDialog(this,
                    "Export erfolgreich: " + file.getAbsolutePath() + "\n" + rowCount + " Zeilen exportiert.",
                    "Export abgeschlossen", JOptionPane.INFORMATION_MESSAGE);
            TimelineLogger.info(DatabaseView.class, "Exported {} rows to CSV: {}", rowCount, file.getAbsolutePath());

        } catch (IOException e) {
            TimelineLogger.error(DatabaseView.class, "CSV export failed", e);
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Export: " + e.getMessage(),
                    "Export-Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportToExcel() {
        DefaultTableModel model = dbPanel.getTableModel();
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Keine Daten zum Exportieren vorhanden.",
                    "Keine Daten", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Excel exportieren");
        chooser.setFileFilter(new FileNameExtensionFilter("Excel-Dateien (*.xls)", "xls"));
        chooser.setSelectedFile(new File("export.xls"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".xls")) {
            file = new File(file.getAbsolutePath() + ".xls");
        }

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {

            // Write HTML table format (Excel can open this)
            writer.println("<html>");
            writer.println("<head><meta charset=\"UTF-8\"></head>");
            writer.println("<body>");
            writer.println("<table border=\"1\">");

            // Write header
            writer.println("<tr>");
            int columnCount = model.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                writer.print("<th>");
                writer.print(escapeHtml(model.getColumnName(i)));
                writer.println("</th>");
            }
            writer.println("</tr>");

            // Write data
            int rowCount = model.getRowCount();
            for (int row = 0; row < rowCount; row++) {
                writer.println("<tr>");
                for (int col = 0; col < columnCount; col++) {
                    writer.print("<td>");
                    Object value = model.getValueAt(row, col);
                    writer.print(escapeHtml(value != null ? value.toString() : ""));
                    writer.println("</td>");
                }
                writer.println("</tr>");
            }

            writer.println("</table>");
            writer.println("</body>");
            writer.println("</html>");

            JOptionPane.showMessageDialog(this,
                    "Export erfolgreich: " + file.getAbsolutePath() + "\n" + rowCount + " Zeilen exportiert.",
                    "Export abgeschlossen", JOptionPane.INFORMATION_MESSAGE);
            TimelineLogger.info(DatabaseView.class, "Exported {} rows to Excel (HTML): {}", rowCount, file.getAbsolutePath());

        } catch (IOException e) {
            TimelineLogger.error(DatabaseView.class, "Excel export failed", e);
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Export: " + e.getMessage(),
                    "Export-Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // ===== Getters =====

    public DatabaseViewPanel getDatabasePanel() {
        return dbPanel;
    }

    @Override
    public void dispose() {
        ConnectionManager.removeListener(this);
        disconnect();
        super.dispose();
    }
}
