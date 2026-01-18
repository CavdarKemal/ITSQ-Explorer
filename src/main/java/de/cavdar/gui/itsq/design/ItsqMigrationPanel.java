/*
 * Erstellt als JFormDesigner-Aequivalent - Migration Panel
 */

package de.cavdar.gui.itsq.design;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Panel fuer ITSQ-Migration von OLD nach NEW Struktur.
 * Layout:
 * - Oben: Quell-/Zielpfad-Auswahl
 * - Mitte: Vorschau mit Statistiken und Detailtabelle
 * - Unten: Optionen und Aktionsbuttons mit Fortschritt
 *
 * @author kemal
 */
public class ItsqMigrationPanel extends JPanel {

    public ItsqMigrationPanel() {
        initComponents();
    }

    // Getter fuer View-Zugriff
    public JPanel getPanelPaths() {
        return panelPaths;
    }

    public JTextField getTextFieldSource() {
        return textFieldSource;
    }

    public JButton getButtonBrowseSource() {
        return buttonBrowseSource;
    }

    public JTextField getTextFieldTarget() {
        return textFieldTarget;
    }

    public JButton getButtonBrowseTarget() {
        return buttonBrowseTarget;
    }

    public JButton getButtonPreview() {
        return buttonPreview;
    }

    public JPanel getPanelPreview() {
        return panelPreview;
    }

    public JTextArea getTextAreaSummary() {
        return textAreaSummary;
    }

    public JTable getTableDetails() {
        return tableDetails;
    }

    public JScrollPane getScrollPaneTable() {
        return scrollPaneTable;
    }

    public JPanel getPanelOptions() {
        return panelOptions;
    }

    public JCheckBox getCheckBoxBackup() {
        return checkBoxBackup;
    }

    public JCheckBox getCheckBoxOverwrite() {
        return checkBoxOverwrite;
    }

    public JButton getButtonMigrate() {
        return buttonMigrate;
    }

    public JButton getButtonCancel() {
        return buttonCancel;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public JLabel getLabelStatus() {
        return labelStatus;
    }

    private void initComponents() {
        // Panel-Komponenten
        panelPaths = new JPanel();
        labelSource = new JLabel();
        textFieldSource = new JTextField();
        buttonBrowseSource = new JButton();
        labelTarget = new JLabel();
        textFieldTarget = new JTextField();
        buttonBrowseTarget = new JButton();
        buttonPreview = new JButton();

        panelPreview = new JPanel();
        scrollPaneSummary = new JScrollPane();
        textAreaSummary = new JTextArea();
        scrollPaneTable = new JScrollPane();
        tableDetails = new JTable();

        panelOptions = new JPanel();
        checkBoxBackup = new JCheckBox();
        checkBoxOverwrite = new JCheckBox();
        buttonMigrate = new JButton();
        buttonCancel = new JButton();

        panelProgress = new JPanel();
        progressBar = new JProgressBar();
        labelStatus = new JLabel();

        //======== this ========
        setLayout(new BorderLayout(5, 5));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        //======== panelPaths ========
        {
            panelPaths.setBorder(new TitledBorder("Pfade"));
            panelPaths.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);

            //---- labelSource ----
            labelSource.setText("Quelle (OLD):");
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            panelPaths.add(labelSource, gbc);

            //---- textFieldSource ----
            textFieldSource.setColumns(50);
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panelPaths.add(textFieldSource, gbc);

            //---- buttonBrowseSource ----
            buttonBrowseSource.setText("...");
            buttonBrowseSource.setToolTipText("Quellverzeichnis waehlen");
            gbc.gridx = 2;
            gbc.gridy = 0;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            panelPaths.add(buttonBrowseSource, gbc);

            //---- labelTarget ----
            labelTarget.setText("Ziel (NEW):");
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.anchor = GridBagConstraints.WEST;
            panelPaths.add(labelTarget, gbc);

            //---- textFieldTarget ----
            textFieldTarget.setColumns(50);
            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panelPaths.add(textFieldTarget, gbc);

            //---- buttonBrowseTarget ----
            buttonBrowseTarget.setText("...");
            buttonBrowseTarget.setToolTipText("Zielverzeichnis waehlen");
            gbc.gridx = 2;
            gbc.gridy = 1;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            panelPaths.add(buttonBrowseTarget, gbc);

            //---- buttonPreview ----
            buttonPreview.setText("Vorschau");
            buttonPreview.setIcon(loadIcon("/icons/magnifier.png"));
            gbc.gridx = 3;
            gbc.gridy = 0;
            gbc.gridheight = 2;
            gbc.fill = GridBagConstraints.VERTICAL;
            panelPaths.add(buttonPreview, gbc);
        }
        add(panelPaths, BorderLayout.NORTH);

        //======== panelPreview ========
        {
            panelPreview.setBorder(new TitledBorder("Vorschau"));
            panelPreview.setLayout(new BorderLayout(5, 5));

            //======== scrollPaneSummary ========
            {
                textAreaSummary.setEditable(false);
                textAreaSummary.setRows(6);
                textAreaSummary.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                scrollPaneSummary.setViewportView(textAreaSummary);
            }
            panelPreview.add(scrollPaneSummary, BorderLayout.NORTH);

            //======== scrollPaneTable ========
            {
                tableDetails.setModel(new DefaultTableModel(
                        new Object[][]{},
                        new String[]{"Kunde", "Szenario", "Test", "PH1", "PH2", "Status"}
                ));
                tableDetails.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                tableDetails.setFillsViewportHeight(true);
                scrollPaneTable.setViewportView(tableDetails);
            }
            panelPreview.add(scrollPaneTable, BorderLayout.CENTER);
        }
        add(panelPreview, BorderLayout.CENTER);

        //======== panelBottom (options + progress) ========
        JPanel panelBottom = new JPanel(new BorderLayout(5, 5));

        //======== panelOptions ========
        {
            panelOptions.setBorder(new TitledBorder("Optionen"));
            panelOptions.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

            //---- checkBoxBackup ----
            checkBoxBackup.setText("Backup erstellen");
            checkBoxBackup.setSelected(true);
            panelOptions.add(checkBoxBackup);

            //---- checkBoxOverwrite ----
            checkBoxOverwrite.setText("Ueberschreiben");
            checkBoxOverwrite.setSelected(false);
            panelOptions.add(checkBoxOverwrite);

            panelOptions.add(Box.createHorizontalStrut(20));

            //---- buttonMigrate ----
            buttonMigrate.setText("Migration starten");
            buttonMigrate.setIcon(loadIcon("/icons/arrow_right.png"));
            buttonMigrate.setEnabled(false);
            panelOptions.add(buttonMigrate);

            //---- buttonCancel ----
            buttonCancel.setText("Abbrechen");
            buttonCancel.setEnabled(false);
            panelOptions.add(buttonCancel);
        }
        panelBottom.add(panelOptions, BorderLayout.NORTH);

        //======== panelProgress ========
        {
            panelProgress.setLayout(new BorderLayout(5, 5));
            panelProgress.setBorder(new EmptyBorder(5, 5, 5, 5));

            progressBar.setStringPainted(true);
            progressBar.setValue(0);
            panelProgress.add(progressBar, BorderLayout.CENTER);

            labelStatus.setText(" ");
            panelProgress.add(labelStatus, BorderLayout.SOUTH);
        }
        panelBottom.add(panelProgress, BorderLayout.SOUTH);

        add(panelBottom, BorderLayout.SOUTH);
    }

    private ImageIcon loadIcon(String path) {
        try {
            java.net.URL url = getClass().getResource(path);
            if (url != null) {
                return new ImageIcon(url);
            }
        } catch (Exception e) {
            // Ignorieren
        }
        return null;
    }

    // Komponenten-Deklarationen
    private JPanel panelPaths;
    private JLabel labelSource;
    private JTextField textFieldSource;
    private JButton buttonBrowseSource;
    private JLabel labelTarget;
    private JTextField textFieldTarget;
    private JButton buttonBrowseTarget;
    private JButton buttonPreview;

    private JPanel panelPreview;
    private JScrollPane scrollPaneSummary;
    private JTextArea textAreaSummary;
    private JScrollPane scrollPaneTable;
    private JTable tableDetails;

    private JPanel panelOptions;
    private JCheckBox checkBoxBackup;
    private JCheckBox checkBoxOverwrite;
    private JButton buttonMigrate;
    private JButton buttonCancel;

    private JPanel panelProgress;
    private JProgressBar progressBar;
    private JLabel labelStatus;
}
