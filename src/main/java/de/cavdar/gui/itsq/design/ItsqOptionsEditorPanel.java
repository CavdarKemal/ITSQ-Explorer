/*
 * Created by JFormDesigner on Sun Jan 18 17:07:54 CET 2026
 */

package de.cavdar.gui.itsq.design;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * @author kemal
 */
public class ItsqOptionsEditorPanel extends JPanel {
    public ItsqOptionsEditorPanel() {
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

    public JPanel getPanelOptionsTable() {
        return panelOptionsTable;
    }

    public JScrollPane getScrollPaneOptionsTable() {
        return scrollPaneOptionsTable;
    }

    public JTable getTableOptions() {
        return tableOptions;
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
        panelOptionsTable = new JPanel();
        scrollPaneOptionsTable = new JScrollPane();
        tableOptions = new JTable();

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

        //======== panelOptionsTable ========
        {
            panelOptionsTable.setBorder(new EtchedBorder());
            panelOptionsTable.setLayout(new BorderLayout());

            //======== scrollPaneOptionsTable ========
            {
                scrollPaneOptionsTable.setViewportView(tableOptions);
            }
            panelOptionsTable.add(scrollPaneOptionsTable, BorderLayout.CENTER);
        }
        add(panelOptionsTable, BorderLayout.CENTER);
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
    private JPanel panelOptionsTable;
    private JScrollPane scrollPaneOptionsTable;
    private JTable tableOptions;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
