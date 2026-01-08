package de.cavdar.gui.itsq.tree;

import de.cavdar.gui.itsq.model.ItsqRoot;

/**
 * Tree node for the ITSQ root directory.
 */
public class ItsqRootTreeNode extends ItsqTreeNode {

    public ItsqRootTreeNode(ItsqRoot root) {
        super(root);
    }

    public ItsqRoot getItsqRoot() {
        return (ItsqRoot) getItsqItem();
    }
}
