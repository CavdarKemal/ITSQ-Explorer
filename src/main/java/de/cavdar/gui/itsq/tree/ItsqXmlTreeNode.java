package de.cavdar.gui.itsq.tree;

import de.cavdar.gui.itsq.model.ItsqXmlFile;

/**
 * Tree node for an XML file.
 */
public class ItsqXmlTreeNode extends ItsqTreeNode {

    public ItsqXmlTreeNode(ItsqXmlFile xmlFile) {
        super(xmlFile);
    }

    public ItsqXmlFile getItsqXmlFile() {
        return (ItsqXmlFile) getItsqItem();
    }
}
