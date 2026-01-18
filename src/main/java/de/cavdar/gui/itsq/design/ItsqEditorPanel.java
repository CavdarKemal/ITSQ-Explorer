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

    public JButton getButtonSave() {
        return buttonSave;
    }

    public JLabel getLabelFilter() {
        return labelFilter;
    }

    public JComboBox getComboBoxFilter() {
        return comboBoxFilter;
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        toolBarControls = new JToolBar();
        labelFilter = new JLabel();
        comboBoxFilter = new JComboBox();
        buttonSave = new JButton();
        panelEditor = new JPanel();
        scrollPaneEditor = new JScrollPane();
        editorPaneEditor = new JEditorPane();

        //======== this ========
        setLayout(new BorderLayout());

        //======== toolBarControls ========
        {
            toolBarControls.setRollover(true);
            toolBarControls.addSeparator();

            //---- labelFilter ----
            labelFilter.setText("Text-Suche:");
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
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JToolBar toolBarControls;
    private JLabel labelFilter;
    private JComboBox comboBoxFilter;
    private JButton buttonSave;
    private JPanel panelEditor;
    private JScrollPane scrollPaneEditor;
    private JEditorPane editorPaneEditor;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
