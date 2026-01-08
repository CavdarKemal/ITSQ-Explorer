package de.cavdar.gui.itsq.tree;

import de.cavdar.gui.itsq.model.ItsqOptionsFile;

/**
 * Tree node for an Options.cfg file.
 */
public class ItsqOptionsTreeNode extends ItsqTreeNode {

    public ItsqOptionsTreeNode(ItsqOptionsFile optionsFile) {
        super(optionsFile);
    }

    public ItsqOptionsFile getItsqOptionsFile() {
        return (ItsqOptionsFile) getItsqItem();
    }
}
