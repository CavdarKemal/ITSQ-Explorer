package de.cavdar.gui.itsq.tree;

import de.cavdar.gui.itsq.model.ItsqItem;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Base class for all ITSQ tree nodes.
 * Extends DefaultMutableTreeNode with typed access to ItsqItem.
 */
public class ItsqTreeNode extends DefaultMutableTreeNode {

    public ItsqTreeNode(ItsqItem item) {
        super(item);
    }

    /**
     * Returns the ItsqItem associated with this node.
     */
    public ItsqItem getItsqItem() {
        return (ItsqItem) getUserObject();
    }

    @Override
    public String toString() {
        ItsqItem item = getItsqItem();
        return item != null ? item.getName() : "";
    }
}
