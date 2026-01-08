package de.cavdar.gui.itsq.tree;

import de.cavdar.gui.itsq.model.ItsqRefExports;

/**
 * Tree node for the REF-EXPORTS directory.
 */
public class ItsqRefExportsTreeNode extends ItsqTreeNode {

    public ItsqRefExportsTreeNode(ItsqRefExports refExports) {
        super(refExports);
    }

    public ItsqRefExports getItsqRefExports() {
        return (ItsqRefExports) getItsqItem();
    }
}
