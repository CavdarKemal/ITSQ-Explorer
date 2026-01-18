/*
 * Created by JFormDesigner on Sat Jan 03 22:42:47 CET 2026
 */

package de.cavdar.gui.itsq.design;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import de.cavdar.gui.itsq.view.*;

/**
 * @author kemal
 */
public class ItsqViewTabPanel extends JPanel {
    public ItsqViewTabPanel() {
        initComponents();
    }

    // ===== Getters for all panels =====

    public ItsqRootView getPanelRoot() {
        return panelRoot;
    }

    public ItsqArchivBestandView getPanelArchivBestand() {
        return panelArchivBestand;
    }

    public ItsqArchibBestandPhaseView getPanelArchivBestandPhase() {
        return panelArchivBestandPhase;
    }

    public ItsqRefExportsView getPanelRefExports() {
        return panelRefExports;
    }

    public ItsqRefExportsPhaseView getPanelRefExportsPhase() {
        return panelRefExportsPhase;
    }

    public ItsqCustomerView getPanelCustomer() {
        return panelCustomer;
    }

    public ItsqScenarioView getPanelScenario() {
        return panelScenario;
    }

    public ItsqEditorView getPanelEditor() {
        return panelEditor;
    }

    public ItsqOptionsEditorView getPanelOptionsEditor() {
        return panelOptionsEditor;
    }

    public ItsqRefExportPropertiesEditorView getPanelRelevanzPropertiesEditor() {
        return panelRelevanzPropertiesEditor;
    }

    public ItsqTestCrefosPropertiesEditorView getPanelTestCrefosPropertiesEditor() {
        return panelTestCrefosPropertiesEditor;
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        panelRoot = new ItsqRootView();
        panelArchivBestand = new ItsqArchivBestandView();
        panelArchivBestandPhase = new ItsqArchibBestandPhaseView();
        panelRefExports = new ItsqRefExportsView();
        panelRefExportsPhase = new ItsqRefExportsPhaseView();
        panelCustomer = new ItsqCustomerView();
        panelScenario = new ItsqScenarioView();
        panelEditor = new ItsqEditorView();
        panelOptionsEditor = new ItsqOptionsEditorView();
        panelRelevanzPropertiesEditor = new ItsqRefExportPropertiesEditorView();
        panelTestCrefosPropertiesEditor = new ItsqTestCrefosPropertiesEditorView();

        //======== this ========
        setBorder(new EtchedBorder());
        setLayout(new CardLayout());

        //---- panelRoot ----
        panelRoot.setBorder(new EtchedBorder());
        add(panelRoot, "cardItsqRoot");
        add(panelArchivBestand, "cardArchivBestand");
        add(panelArchivBestandPhase, "cardArchivBestandPhase");
        add(panelRefExports, "cardRefExports");
        add(panelRefExportsPhase, "cardRefExportsPhase");
        add(panelCustomer, "cardCustomer");
        add(panelScenario, "cardScenario");
        add(panelEditor, "cardXml");
        add(panelOptionsEditor, "cardOptionsEditor");
        add(panelRelevanzPropertiesEditor, "cardRelevanzPropertiesEditor");
        add(panelTestCrefosPropertiesEditor, "cardTestCrefosPropertiesEditor");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private ItsqRootView panelRoot;
    private ItsqArchivBestandView panelArchivBestand;
    private ItsqArchibBestandPhaseView panelArchivBestandPhase;
    private ItsqRefExportsView panelRefExports;
    private ItsqRefExportsPhaseView panelRefExportsPhase;
    private ItsqCustomerView panelCustomer;
    private ItsqScenarioView panelScenario;
    private ItsqEditorView panelEditor;
    private ItsqOptionsEditorView panelOptionsEditor;
    private ItsqRefExportPropertiesEditorView panelRelevanzPropertiesEditor;
    private ItsqTestCrefosPropertiesEditorView panelTestCrefosPropertiesEditor;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
