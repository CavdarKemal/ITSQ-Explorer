package de.cavdar.gui.itsq.tree;

import de.cavdar.gui.itsq.model.ItsqPropertiesFile;

/**
 * Tree node for a .properties file.
 */
public class ItsqPropertiesTreeNode extends ItsqTreeNode {

    public ItsqPropertiesTreeNode(ItsqPropertiesFile propertiesFile) {
        super(propertiesFile);
    }

    public ItsqPropertiesFile getItsqPropertiesFile() {
        return (ItsqPropertiesFile) getItsqItem();
    }
}
