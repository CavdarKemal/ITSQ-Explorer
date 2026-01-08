package de.cavdar.gui.itsq.tree;

import de.cavdar.gui.itsq.model.ItsqArchivBestand;

/**
 * Tree node for the ARCHIV-BESTAND directory.
 */
public class ItsqArchivBestandTreeNode extends ItsqTreeNode {

    public ItsqArchivBestandTreeNode(ItsqArchivBestand archivBestand) {
        super(archivBestand);
    }

    public ItsqArchivBestand getItsqArchivBestand() {
        return (ItsqArchivBestand) getItsqItem();
    }
}
