/*
 * Created by JFormDesigner on Sun Jan 04 13:15:18 CET 2026
 */

package de.cavdar.gui.itsq.design;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * @author kemal
 */
public class ItsqEditorPanel extends JPanel {
    public ItsqEditorPanel() {
        initComponents();
    }

    public JScrollPane getScrollPaneEditor() {
        return scrollPaneEditor;
    }

    public JEditorPane getEditorPaneEditor() {
        return editorPaneEditor;
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

    public JButton getButtonSave() {
        return buttonSave;
    }

    public JLabel getLabelFilter() {
        return labelFilter;
    }

    public JComboBox getComboBoxFilter() {
        return comboBoxFilter;
    }

    public JPanel getPanelControls() {
        return panelControls;
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
        panelEditor = new JPanel();
        scrollPaneEditor = new JScrollPane();
        editorPaneEditor = new JEditorPane();
        panelControls = new JPanel();
        labelTitle = new JLabel();

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

        //======== panelEditor ========
        {
            panelEditor.setBorder(new EtchedBorder());
            panelEditor.setLayout(new BorderLayout());

            //======== scrollPaneEditor ========
            {

                //---- editorPaneEditor ----
                editorPaneEditor.setFont(new Font("Courier New", Font.PLAIN, 12));
                editorPaneEditor.setBackground(Color.lightGray);
                scrollPaneEditor.setViewportView(editorPaneEditor);
            }
            panelEditor.add(scrollPaneEditor, BorderLayout.CENTER);
        }
        add(panelEditor, BorderLayout.CENTER);

        //======== panelControls ========
        {
            panelControls.setBorder(new SoftBevelBorder(SoftBevelBorder.LOWERED));
            panelControls.setLayout(new GridBagLayout());
            ((GridBagLayout)panelControls.getLayout()).columnWidths = new int[] {0, 0, 0};
            ((GridBagLayout)panelControls.getLayout()).rowHeights = new int[] {0, 0};
            ((GridBagLayout)panelControls.getLayout()).columnWeights = new double[] {0.0, 1.0, 1.0E-4};
            ((GridBagLayout)panelControls.getLayout()).rowWeights = new double[] {0.0, 1.0E-4};

            //---- labelTitle ----
            labelTitle.setText("Datei:");
            labelTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            panelControls.add(labelTitle, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        }
        add(panelControls, BorderLayout.SOUTH);
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
    private JPanel panelEditor;
    private JScrollPane scrollPaneEditor;
    private JEditorPane editorPaneEditor;
    private JPanel panelControls;
    private JLabel labelTitle;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
