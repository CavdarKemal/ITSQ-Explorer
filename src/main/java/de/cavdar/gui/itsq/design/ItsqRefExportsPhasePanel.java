/*
 * Created by JFormDesigner on Sat Jan 03 22:20:32 CET 2026
 */

package de.cavdar.gui.itsq.design;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * @author kemal
 */
public class ItsqRefExportsPhasePanel extends JPanel {
    public ItsqRefExportsPhasePanel() {
        initComponents();
    }

    public JPanel getPanelControls() {
        return panelControls;
    }

    public JPanel getPanelView() {
        return panelView;
    }

    public JLabel getLabelTitle() {
        return labelTitle;
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        panelControls = new JPanel();
        labelTitle = new JLabel();
        panelView = new JPanel();

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
            labelTitle.setText("REF-EXPORTS Phase #");
            labelTitle.setFont(new Font("Segoe UI", Font.PLAIN, 24));
            panelControls.add(labelTitle, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        }
        add(panelControls, BorderLayout.NORTH);

        //======== panelView ========
        {
            panelView.setBorder(new EtchedBorder());
            panelView.setLayout(new GridBagLayout());
            ((GridBagLayout)panelView.getLayout()).columnWidths = new int[] {0, 0, 0};
            ((GridBagLayout)panelView.getLayout()).rowHeights = new int[] {0, 27, 0};
            ((GridBagLayout)panelView.getLayout()).columnWeights = new double[] {0.0, 1.0, 1.0E-4};
            ((GridBagLayout)panelView.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0E-4};
        }
        add(panelView, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel panelControls;
    private JLabel labelTitle;
    private JPanel panelView;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
