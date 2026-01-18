/*
 * Created by JFormDesigner on Mon Jan 05 12:07:04 CET 2026
 */

package de.cavdar.gui.itsq.design;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import de.cavdar.gui.itsq.view.*;

/**
 * @author kemal
 */
public class ItsqMainPanel extends JPanel {
    public ItsqMainPanel() {
        initComponents();
    }

    public JPanel getPanelControls() {
        return panelControls;
    }

    public JLabel getLabelTestSet() {
        return labelTestSet;
    }

    public JComboBox getComboBoxTestSet() {
        return comboBoxTestSet;
    }

    public JButton getButtonLoad() {
        return buttonLoad;
    }

    public ItsqTreeView getPanelItsqTree() {
        return panelItsqTree;
    }

    public ItsqViewTabView getPanelItsqView() {
        return panelItsqView;
    }

    public JSplitPane getSplitPaneItsq() {
        return splitPaneItsq;
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        panelControls = new JPanel();
        labelTestSet = new JLabel();
        comboBoxTestSet = new JComboBox();
        buttonLoad = new JButton();
        splitPaneItsq = new JSplitPane();
        panelItsqTree = new ItsqTreeView();
        panelItsqView = new ItsqViewTabView();

        //======== this ========
        setLayout(new BorderLayout());

        //======== panelControls ========
        {
            panelControls.setBorder(new SoftBevelBorder(SoftBevelBorder.LOWERED));
            panelControls.setLayout(new GridBagLayout());
            ((GridBagLayout)panelControls.getLayout()).columnWidths = new int[] {0, 0, 0, 0};
            ((GridBagLayout)panelControls.getLayout()).rowHeights = new int[] {0, 0};
            ((GridBagLayout)panelControls.getLayout()).columnWeights = new double[] {0.0, 1.0, 0.0, 1.0E-4};
            ((GridBagLayout)panelControls.getLayout()).rowWeights = new double[] {1.0, 1.0E-4};

            //---- labelTestSet ----
            labelTestSet.setText("TestSet:");
            panelControls.add(labelTestSet, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(2, 2, 2, 4), 0, 0));
            panelControls.add(comboBoxTestSet, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(2, 2, 2, 4), 0, 0));

            //---- buttonLoad ----
            buttonLoad.setText("Load");
            buttonLoad.setIcon(new ImageIcon(getClass().getResource("/icons/folder_gear.png")));
            panelControls.add(buttonLoad, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(2, 2, 2, 2), 0, 0));
        }
        add(panelControls, BorderLayout.NORTH);

        //======== splitPaneItsq ========
        {
            splitPaneItsq.setDividerLocation(200);

            //---- panelItsqTree ----
            panelItsqTree.setBorder(new EtchedBorder());
            splitPaneItsq.setLeftComponent(panelItsqTree);

            //---- panelItsqView ----
            panelItsqView.setBorder(new EtchedBorder());
            splitPaneItsq.setRightComponent(panelItsqView);
        }
        add(splitPaneItsq, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel panelControls;
    private JLabel labelTestSet;
    private JComboBox comboBoxTestSet;
    private JButton buttonLoad;
    private JSplitPane splitPaneItsq;
    private ItsqTreeView panelItsqTree;
    private ItsqViewTabView panelItsqView;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
