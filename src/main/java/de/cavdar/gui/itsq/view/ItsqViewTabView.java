package de.cavdar.gui.itsq.view;

import de.cavdar.gui.itsq.design.ItsqViewTabPanel;
import de.cavdar.gui.itsq.model.ItsqItem;
import de.cavdar.gui.itsq.tree.*;
import de.cavdar.gui.util.TimelineLogger;

import javax.swing.*;
import java.awt.*;

/**
 * View for the ITSQ detail panels with CardLayout.
 * Manages card switching and item selection delegation.
 *
 * @author TemplateGUI
 */
public class ItsqViewTabView extends ItsqViewTabPanel {

    // Card names (must match ItsqViewTabPanel)
    private static final String CARD_ROOT = "cardItsqRoot";
    private static final String CARD_ARCHIV_BESTAND = "cardArchivBestand";
    private static final String CARD_ARCHIV_BESTAND_PHASE = "cardArchivBestandPhase";
    private static final String CARD_REF_EXPORTS = "cardRefExports";
    private static final String CARD_REF_EXPORTS_PHASE = "cardRefExportsPhase";
    private static final String CARD_SCENARIO = "cardScenario";
    private static final String CARD_CUSTOMER = "cardCustomer";
    private static final String CARD_XML = "cardXml";

    public ItsqViewTabView() {
        super();
    }

    /**
     * Shows the appropriate card for the given tree node and passes the item to the view.
     *
     * @param node the selected tree node
     */
    public void showCardForNode(ItsqTreeNode node) {
        if (node == null) {
            return;
        }

        String cardName = determineCardForNode(node);
        ItsqItem item = node.getItsqItem();

        showCard(cardName, item);

        TimelineLogger.debug(ItsqViewTabView.class, "Showing card: {} for item: {}", cardName, item != null ? item.getName() : "null");
    }

    /**
     * Shows the root card (used for initial state).
     */
    public void showRootCard() {
        showCard(CARD_ROOT, null);
    }

    /**
     * Determines which card to show based on the node type.
     */
    private String determineCardForNode(ItsqTreeNode node) {
        if (node instanceof ItsqRootTreeNode) {
            return CARD_ROOT;
        } else if (node instanceof ItsqArchivBestandTreeNode) {
            return CARD_ARCHIV_BESTAND;
        } else if (node instanceof ItsqArchivBestandPhaseTreeNode) {
            return CARD_ARCHIV_BESTAND_PHASE;
        } else if (node instanceof ItsqRefExportsTreeNode) {
            return CARD_REF_EXPORTS;
        } else if (node instanceof ItsqRefExportsPhaseTreeNode) {
            return CARD_REF_EXPORTS_PHASE;
        } else if (node instanceof ItsqCustomerTreeNode) {
            return CARD_CUSTOMER;
        } else if (node instanceof ItsqScenarioTreeNode) {
            return CARD_SCENARIO;
        } else if (node instanceof ItsqXmlTreeNode
                || node instanceof ItsqOptionsTreeNode
                || node instanceof ItsqPropertiesTreeNode) {
            return CARD_XML;
        }
        return CARD_ROOT;
    }

    /**
     * Shows the specified card and passes the item to the view.
     */
    private void showCard(String cardName, ItsqItem item) {
        // Switch card
        CardLayout cardLayout = (CardLayout) getLayout();
        cardLayout.show(this, cardName);

        // Pass selected item to the view
        JPanel panel = getViewPanelForCard(cardName);
        if (panel instanceof ItsqItemSelectable selectable) {
            selectable.setSelectedItem(item);
        }
    }

    /**
     * Gets the view panel for the given card name.
     */
    private JPanel getViewPanelForCard(String cardName) {
        return switch (cardName) {
            case CARD_ROOT -> getPanelRoot();
            case CARD_ARCHIV_BESTAND -> getPanelArchivBestand();
            case CARD_ARCHIV_BESTAND_PHASE -> getPanelArchivBestandPhase();
            case CARD_REF_EXPORTS -> getPanelRefExports();
            case CARD_REF_EXPORTS_PHASE -> getPanelRefExportsPhase();
            case CARD_CUSTOMER -> getPanelCustomer();
            case CARD_SCENARIO -> getPanelScenario();
            case CARD_XML -> getPanelEditor();
            default -> null;
        };
    }
}
