/*
 * Created by JFormDesigner on Sat Jan 03 21:48:07 CET 2026
 */

package de.cavdar.gui.itsq.design;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * @author kemal
 */
public class ItsqTreePanel extends JPanel {
    public ItsqTreePanel() {
        initComponents();
    }

    public JScrollPane getScrollPane1() {
        return scrollPane1;
    }

    public JTree getTreeItsq() {
        return treeItsq;
    }

    public JPanel getPanelControls() {
        return panelControls;
    }

    public JLabel getLabelTitle() {
        return labelTitle;
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        panelControls = new JPanel();
        labelTitle = new JLabel();
        scrollPane1 = new JScrollPane();
        treeItsq = new JTree();

        //======== this ========
        setLayout(new BorderLayout());

        //======== panelControls ========
        {
            panelControls.setBorder(new SoftBevelBorder(SoftBevelBorder.LOWERED));
            panelControls.setLayout(new GridBagLayout());
            ((GridBagLayout)panelControls.getLayout()).columnWidths = new int[] {0, 0};
            ((GridBagLayout)panelControls.getLayout()).rowHeights = new int[] {0, 0};
            ((GridBagLayout)panelControls.getLayout()).columnWeights = new double[] {0.0, 1.0E-4};
            ((GridBagLayout)panelControls.getLayout()).rowWeights = new double[] {0.0, 1.0E-4};

            //---- labelTitle ----
            labelTitle.setText("ITSQ Tree");
            labelTitle.setFont(new Font("Segoe UI", Font.PLAIN, 24));
            panelControls.add(labelTitle, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        }
        add(panelControls, BorderLayout.NORTH);

        //======== scrollPane1 ========
        {

            //---- treeItsq ----
            treeItsq.setBorder(new EtchedBorder());
            scrollPane1.setViewportView(treeItsq);
        }
        add(scrollPane1, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel panelControls;
    private JLabel labelTitle;
    private JScrollPane scrollPane1;
    private JTree treeItsq;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
