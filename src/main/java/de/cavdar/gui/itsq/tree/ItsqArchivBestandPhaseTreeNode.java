package de.cavdar.gui.itsq.tree;

import de.cavdar.gui.itsq.model.ItsqArchivBestandPhase;

/**
 * Tree node for a PHASE-x directory under ARCHIV-BESTAND.
 */
public class ItsqArchivBestandPhaseTreeNode extends ItsqTreeNode {

    public ItsqArchivBestandPhaseTreeNode(ItsqArchivBestandPhase phase) {
        super(phase);
    }

    public ItsqArchivBestandPhase getItsqArchivBestandPhase() {
        return (ItsqArchivBestandPhase) getItsqItem();
    }
}
