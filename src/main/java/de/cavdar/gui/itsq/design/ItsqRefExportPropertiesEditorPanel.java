/*
 * Created by JFormDesigner on Sun Jan 18 16:50:54 CET 2026
 */

package de.cavdar.gui.itsq.design;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * @author kemal
 */
public class ItsqRefExportPropertiesEditorPanel extends JPanel {
    public ItsqRefExportPropertiesEditorPanel() {
        initComponents();
    }

    public JToolBar getToolBarControls() {
        return toolBarControls;
    }

    public JButton getButtonNew() {
        return buttonNew;
    }

    public JButton getButtonEdit() {
        return buttonEdit;
    }

    public JButton getButtonDelete() {
        return buttonDelete;
    }

    public JLabel getLabelFilter() {
        return labelFilter;
    }

    public JComboBox getComboBoxFilter() {
        return comboBoxFilter;
    }

    public JButton getButtonSave() {
        return buttonSave;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    public JPanel getPanelPropertiesTable() {
        return panelPropertiesTable;
    }

    public JScrollPane getScrollPanePropertiesTable() {
        return scrollPanePropertiesTable;
    }

    public JTable getTableProperties() {
        return tableProperties;
    }

    public JPanel getPanelControls() {
        return panelControls;
    }

    public JLabel getLabelRefExportFile() {
        return labelRefExportFile;
    }

    public JTextField getTextFieldRefExportFile() {
        return textFieldRefExportFile;
    }

    public JButton getButtonLocateRefExportFile() {
        return buttonLocateRefExportFile;
    }

    public JButton getButtonDownloadRefExportFile() {
        return buttonDownloadRefExportFile;
    }

    public JLabel getLabelArchivBestandFile() {
        return labelArchivBestandFile;
    }

    public JTextField getTextFieldArchivBestandFile() {
        return textFieldArchivBestandFile;
    }

    public JButton getButtonLocateArchivBestandFile() {
        return buttonLocateArchivBestandFile;
    }

    public JButton getButtonDownloadArchivBestandFile() {
        return buttonDownloadArchivBestandFile;
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        toolBarControls = new JToolBar();
        buttonNew = new JButton();
        buttonEdit = new JButton();
        buttonDelete = new JButton();
        labelFilter = new JLabel();
        comboBoxFilter = new JComboBox();
        buttonSave = new JButton();
        splitPane = new JSplitPane();
        panelPropertiesTable = new JPanel();
        scrollPanePropertiesTable = new JScrollPane();
        tableProperties = new JTable();
        panelControls = new JPanel();
        labelRefExportFile = new JLabel();
        textFieldRefExportFile = new JTextField();
        buttonLocateRefExportFile = new JButton();
        buttonDownloadRefExportFile = new JButton();
        labelArchivBestandFile = new JLabel();
        textFieldArchivBestandFile = new JTextField();
        buttonLocateArchivBestandFile = new JButton();
        buttonDownloadArchivBestandFile = new JButton();

        //======== this ========
        setLayout(new BorderLayout());

        //======== toolBarControls ========
        {
            toolBarControls.setRollover(true);

            //---- buttonNew ----
            buttonNew.setText("Neu");
            buttonNew.setIcon(new ImageIcon(getClass().getResource("/icons/add.png")));
            toolBarControls.add(buttonNew);

            //---- buttonEdit ----
            buttonEdit.setText("\u00c4ndern");
            buttonEdit.setIcon(new ImageIcon(getClass().getResource("/icons/gear_run.png")));
            toolBarControls.add(buttonEdit);

            //---- buttonDelete ----
            buttonDelete.setText("L\u00f6schen");
            buttonDelete.setIcon(new ImageIcon(getClass().getResource("/icons/cancel.png")));
            toolBarControls.add(buttonDelete);
            toolBarControls.addSeparator();

            //---- labelFilter ----
            labelFilter.setText("Filter:");
            toolBarControls.add(labelFilter);
            toolBarControls.addSeparator();
            toolBarControls.add(comboBoxFilter);
            toolBarControls.addSeparator();

            //---- buttonSave ----
            buttonSave.setText("Speichern");
            buttonSave.setIcon(new ImageIcon(getClass().getResource("/icons/save.png")));
            toolBarControls.add(buttonSave);
        }
        add(toolBarControls, BorderLayout.NORTH);

        //======== splitPane ========
        {
            splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
            splitPane.setResizeWeight(1.0);

            //======== panelPropertiesTable ========
            {
                panelPropertiesTable.setBorder(new EtchedBorder());
                panelPropertiesTable.setLayout(new BorderLayout());

                //======== scrollPanePropertiesTable ========
                {
                    scrollPanePropertiesTable.setViewportView(tableProperties);
                }
                panelPropertiesTable.add(scrollPanePropertiesTable, BorderLayout.CENTER);
            }
            splitPane.setTopComponent(panelPropertiesTable);

            //======== panelControls ========
            {
                panelControls.setBorder(new SoftBevelBorder(SoftBevelBorder.LOWERED));
                panelControls.setLayout(new GridBagLayout());
                ((GridBagLayout)panelControls.getLayout()).columnWidths = new int[] {0, 340, 104, 0, 0};
                ((GridBagLayout)panelControls.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                ((GridBagLayout)panelControls.getLayout()).columnWeights = new double[] {0.0, 1.0, 0.0, 0.0, 1.0E-4};
                ((GridBagLayout)panelControls.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

                //---- labelRefExportFile ----
                labelRefExportFile.setText("Ref-Export-Datei:");
                panelControls.add(labelRefExportFile, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(2, 2, 7, 7), 0, 0));

                //---- textFieldRefExportFile ----
                textFieldRefExportFile.setEditable(false);
                panelControls.add(textFieldRefExportFile, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(2, 2, 7, 7), 0, 0));

                //---- buttonLocateRefExportFile ----
                buttonLocateRefExportFile.setText("Selektieren...");
                buttonLocateRefExportFile.setIcon(new ImageIcon(getClass().getResource("/icons/folder_into.png")));
                panelControls.add(buttonLocateRefExportFile, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(2, 2, 7, 7), 0, 0));

                //---- buttonDownloadRefExportFile ----
                buttonDownloadRefExportFile.setText("Downloaden...");
                buttonDownloadRefExportFile.setIcon(new ImageIcon(getClass().getResource("/icons/folder_network.png")));
                panelControls.add(buttonDownloadRefExportFile, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(2, 2, 7, 2), 0, 0));

                //---- labelArchivBestandFile ----
                labelArchivBestandFile.setText("Archiv-Bestand-Datei:");
                panelControls.add(labelArchivBestandFile, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(2, 2, 7, 7), 0, 0));

                //---- textFieldArchivBestandFile ----
                textFieldArchivBestandFile.setEditable(false);
                panelControls.add(textFieldArchivBestandFile, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(2, 2, 7, 7), 0, 0));

                //---- buttonLocateArchivBestandFile ----
                buttonLocateArchivBestandFile.setText("Selektieren...");
                buttonLocateArchivBestandFile.setIcon(new ImageIcon(getClass().getResource("/icons/folder_into.png")));
                panelControls.add(buttonLocateArchivBestandFile, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(2, 2, 7, 7), 0, 0));

                //---- buttonDownloadArchivBestandFile ----
                buttonDownloadArchivBestandFile.setText("Downloaden...");
                buttonDownloadArchivBestandFile.setIcon(new ImageIcon(getClass().getResource("/icons/folder_network.png")));
                panelControls.add(buttonDownloadArchivBestandFile, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(2, 2, 7, 2), 0, 0));
            }
            splitPane.setBottomComponent(panelControls);
        }
        add(splitPane, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JToolBar toolBarControls;
    private JButton buttonNew;
    private JButton buttonEdit;
    private JButton buttonDelete;
    private JLabel labelFilter;
    private JComboBox comboBoxFilter;
    private JButton buttonSave;
    private JSplitPane splitPane;
    private JPanel panelPropertiesTable;
    private JScrollPane scrollPanePropertiesTable;
    private JTable tableProperties;
    private JPanel panelControls;
    private JLabel labelRefExportFile;
    private JTextField textFieldRefExportFile;
    private JButton buttonLocateRefExportFile;
    private JButton buttonDownloadRefExportFile;
    private JLabel labelArchivBestandFile;
    private JTextField textFieldArchivBestandFile;
    private JButton buttonLocateArchivBestandFile;
    private JButton buttonDownloadArchivBestandFile;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
