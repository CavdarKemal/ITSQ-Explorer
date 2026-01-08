package de.cavdar.gui.itsq.tree;

import de.cavdar.gui.itsq.model.ItsqRefExportsPhase;

/**
 * Tree node for a PHASE-x directory under REF-EXPORTS.
 */
public class ItsqRefExportsPhaseTreeNode extends ItsqTreeNode {

    public ItsqRefExportsPhaseTreeNode(ItsqRefExportsPhase phase) {
        super(phase);
    }

    public ItsqRefExportsPhase getItsqRefExportsPhase() {
        return (ItsqRefExportsPhase) getItsqItem();
    }
}
