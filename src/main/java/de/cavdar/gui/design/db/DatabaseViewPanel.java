package de.cavdar.gui.design.db;
import de.cavdar.gui.design.base.BaseViewPanel;

import de.cavdar.gui.util.IconLoader;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

/**
 * GUI panel for DatabaseView - contains only layout and components.
 * No listeners or business logic.
 *
 * @author StandardMDIGUI
 * @version 1.0
 * @since 2024-12-25
 */
public class DatabaseViewPanel extends BaseViewPanel {

    private static final String[] DRIVERS = {
            "org.postgresql.Driver",
            "com.mysql.cj.jdbc.Driver",
            "oracle.jdbc.OracleDriver",
            "com.microsoft.sqlserver.jdbc.SQLServerDriver",
            "org.h2.Driver",
            "org.sqlite.JDBC"
    };

    // Connection panel components
    protected JPanel connectionPanel;
    protected JComboBox<String> cbConnections;
    protected JTextField txtConnectionName;
    protected JTextField txtUrl;
    protected JTextField txtUsername;
    protected JPasswordField txtPassword;
    protected JComboBox<String> cbDriver;
    protected JButton btnConnect;
    protected JButton btnSave;
    protected JButton btnDelete;
    protected JLabel lblStatus;

    // Query panel components
    protected JPanel queryPanel;
    protected JTextArea txtQuery;
    protected JButton btnExecute;
    protected JButton btnClear;

    // SQL History/Favorites components
    protected JComboBox<String> cbSqlHistory;
    protected JButton btnAddFavorite;
    protected JButton btnRemoveFavorite;

    // Result panel components
    protected JPanel resultPanel;
    protected JTable tblResults;
    protected DefaultTableModel tableModel;
    protected JLabel lblRowCount;
    protected JButton btnExportCsv;
    protected JButton btnExportExcel;

    // Tree panel components (for database tables)
    protected JPanel tableTreePanel;
    protected JTree tableTree;
    protected DefaultMutableTreeNode tableRootNode;
    protected DefaultTreeModel tableTreeModel;

    // Split panes
    protected JSplitPane mainSplitPane;
    protected JSplitPane rightSplitPane;

    public DatabaseViewPanel() {
        super();
        initCustomComponents();
    }

    /**
     * Initializes database-specific components.
     */
    protected void initCustomComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Connection panel stays at top
        mainPanel.add(createConnectionPanel(), BorderLayout.NORTH);

        // Create the table tree panel (left side)
        createTableTreePanel();

        // Create query and result panels
        createQueryPanel();
        createResultPanel();

        // Right split pane: SQL query (top) / Results (bottom)
        rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, queryPanel, resultPanel);
        rightSplitPane.setDividerLocation(150);
        rightSplitPane.setResizeWeight(0.3);

        // Main split pane: Table tree (left) / Query+Results (right)
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableTreePanel, rightSplitPane);
        mainSplitPane.setDividerLocation(200);
        mainSplitPane.setResizeWeight(0.2);

        mainPanel.add(mainSplitPane, BorderLayout.CENTER);

        contentPanel.add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createConnectionPanel() {
        connectionPanel = new JPanel(new GridBagLayout());
        connectionPanel.setBorder(BorderFactory.createTitledBorder("Verbindung"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Saved Connections ComboBox
        gbc.gridx = 0;
        gbc.gridy = 0;
        connectionPanel.add(new JLabel("Verbindung:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        cbConnections = new JComboBox<>();
        cbConnections.setName("Verbindungen");
        connectionPanel.add(cbConnections, gbc);

        // Connection Name
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        connectionPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        txtConnectionName = new JTextField(30);
        txtConnectionName.setName("ConnectionName");
        connectionPanel.add(txtConnectionName, gbc);

        // Driver
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        connectionPanel.add(new JLabel("Treiber:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        cbDriver = new JComboBox<>(DRIVERS);
        cbDriver.setName("Treiber");
        cbDriver.setEditable(true);
        connectionPanel.add(cbDriver, gbc);

        // URL
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        connectionPanel.add(new JLabel("JDBC-URL:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        txtUrl = new JTextField("jdbc:postgresql://localhost:5432/postgres", 40);
        txtUrl.setName("JDBC-URL");
        connectionPanel.add(txtUrl, gbc);

        // Username
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        connectionPanel.add(new JLabel("Benutzer:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        txtUsername = new JTextField("postgres", 20);
        txtUsername.setName("Benutzer");
        connectionPanel.add(txtUsername, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        connectionPanel.add(new JLabel("Passwort:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        txtPassword = new JPasswordField(20);
        txtPassword.setName("Passwort");
        connectionPanel.add(txtPassword, gbc);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        btnConnect = new JButton("Verbinden", IconLoader.load("ok.png"));
        btnConnect.setName("Verbinden");
        buttonPanel.add(btnConnect);

        btnSave = new JButton("Speichern", IconLoader.load("save.png"));
        btnSave.setName("Speichern");
        buttonPanel.add(btnSave);

        btnDelete = new JButton("Löschen", IconLoader.load("cancel.png"));
        btnDelete.setName("Löschen");
        buttonPanel.add(btnDelete);

        lblStatus = new JLabel("Nicht verbunden");
        lblStatus.setName("Nicht verbunden");
        lblStatus.setForeground(Color.RED);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(lblStatus);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 3;
        connectionPanel.add(buttonPanel, gbc);

        return connectionPanel;
    }

    private void createTableTreePanel() {
        tableTreePanel = new JPanel(new BorderLayout());
        tableTreePanel.setBorder(BorderFactory.createTitledBorder("Tabellen"));

        tableRootNode = new DefaultMutableTreeNode("Datenbank");
        tableTreeModel = new DefaultTreeModel(tableRootNode);
        tableTree = new JTree(tableTreeModel);
        tableTree.setName("TableTree");
        tableTree.setRootVisible(true);
        tableTree.setShowsRootHandles(true);

        JScrollPane treeScroll = new JScrollPane(tableTree);
        tableTreePanel.add(treeScroll, BorderLayout.CENTER);
    }

    private void createQueryPanel() {
        queryPanel = new JPanel(new BorderLayout(5, 5));
        queryPanel.setBorder(BorderFactory.createTitledBorder("SQL-Abfrage"));

        // History/Favorites panel at top
        JPanel historyPanel = new JPanel(new BorderLayout(5, 0));
        historyPanel.add(new JLabel("History/Favoriten: "), BorderLayout.WEST);

        cbSqlHistory = new JComboBox<>();
        cbSqlHistory.setName("SqlHistory");
        cbSqlHistory.setEditable(false);
        cbSqlHistory.setMaximumRowCount(15);
        historyPanel.add(cbSqlHistory, BorderLayout.CENTER);

        JPanel favoriteButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        btnAddFavorite = new JButton("★");
        btnAddFavorite.setName("AddFavorite");
        btnAddFavorite.setToolTipText("Aktuelle Abfrage zu Favoriten hinzufügen");
        btnAddFavorite.setMargin(new Insets(2, 6, 2, 6));
        favoriteButtonPanel.add(btnAddFavorite);

        btnRemoveFavorite = new JButton("✕");
        btnRemoveFavorite.setName("RemoveFavorite");
        btnRemoveFavorite.setToolTipText("Ausgewählten Eintrag aus History/Favoriten entfernen");
        btnRemoveFavorite.setMargin(new Insets(2, 6, 2, 6));
        favoriteButtonPanel.add(btnRemoveFavorite);

        historyPanel.add(favoriteButtonPanel, BorderLayout.EAST);
        queryPanel.add(historyPanel, BorderLayout.NORTH);

        // Query text area
        txtQuery = new JTextArea(5, 60);
        txtQuery.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txtQuery.setText("SELECT * FROM ");
        queryPanel.add(new JScrollPane(txtQuery), BorderLayout.CENTER);

        // Buttons at bottom
        JPanel queryButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnExecute = new JButton("Ausführen", IconLoader.load("gear_run.png"));
        btnExecute.setName("Ausführen");
        btnExecute.setEnabled(false);
        queryButtonPanel.add(btnExecute);

        btnClear = new JButton("Leeren", IconLoader.load("cancel.png"));
        btnClear.setName("Leeren");
        queryButtonPanel.add(btnClear);

        queryPanel.add(queryButtonPanel, BorderLayout.SOUTH);
    }

    private void createResultPanel() {
        resultPanel = new JPanel(new BorderLayout(5, 5));
        resultPanel.setBorder(BorderFactory.createTitledBorder("Ergebnisse"));

        tableModel = new DefaultTableModel();
        tblResults = new JTable(tableModel);
        tblResults.setName("Ergebnisse");
        tblResults.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane scrollPane = new JScrollPane(tblResults);
        resultPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with row count and export buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());

        lblRowCount = new JLabel("0 Zeilen");
        bottomPanel.add(lblRowCount, BorderLayout.WEST);

        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnExportCsv = new JButton("CSV Export");
        btnExportCsv.setName("ExportCsv");
        btnExportCsv.setToolTipText("Ergebnisse als CSV-Datei exportieren");
        btnExportCsv.setEnabled(false);
        exportPanel.add(btnExportCsv);

        btnExportExcel = new JButton("Excel Export");
        btnExportExcel.setName("ExportExcel");
        btnExportExcel.setToolTipText("Ergebnisse als Excel-Datei exportieren");
        btnExportExcel.setEnabled(false);
        exportPanel.add(btnExportExcel);

        bottomPanel.add(exportPanel, BorderLayout.EAST);
        resultPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    // ===== Getters for View access =====

    public JComboBox<String> getConnectionsComboBox() {
        return cbConnections;
    }

    public JTextField getConnectionNameField() {
        return txtConnectionName;
    }

    public JTextField getUrlField() {
        return txtUrl;
    }

    public JTextField getUsernameField() {
        return txtUsername;
    }

    public JPasswordField getPasswordField() {
        return txtPassword;
    }

    public JComboBox<String> getDriverComboBox() {
        return cbDriver;
    }

    public JButton getConnectButton() {
        return btnConnect;
    }

    public JButton getSaveButton() {
        return btnSave;
    }

    public JButton getDeleteButton() {
        return btnDelete;
    }

    public JLabel getStatusLabel() {
        return lblStatus;
    }

    public JTextArea getQueryArea() {
        return txtQuery;
    }

    public JButton getExecuteButton() {
        return btnExecute;
    }

    public JButton getClearButton() {
        return btnClear;
    }

    public JTable getResultsTable() {
        return tblResults;
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JLabel getRowCountLabel() {
        return lblRowCount;
    }

    // ===== History/Favorites Getters =====

    public JComboBox<String> getSqlHistoryComboBox() {
        return cbSqlHistory;
    }

    public JButton getAddFavoriteButton() {
        return btnAddFavorite;
    }

    public JButton getRemoveFavoriteButton() {
        return btnRemoveFavorite;
    }

    // ===== Export Getters =====

    public JButton getExportCsvButton() {
        return btnExportCsv;
    }

    public JButton getExportExcelButton() {
        return btnExportExcel;
    }

    // ===== Tree Getters =====

    public JTree getTableTree() {
        return tableTree;
    }

    public DefaultMutableTreeNode getTableRootNode() {
        return tableRootNode;
    }

    public DefaultTreeModel getTableTreeModel() {
        return tableTreeModel;
    }

    // ===== SplitPane Getters =====

    public JSplitPane getMainSplitPane() {
        return mainSplitPane;
    }

    public JSplitPane getRightSplitPane() {
        return rightSplitPane;
    }
}
