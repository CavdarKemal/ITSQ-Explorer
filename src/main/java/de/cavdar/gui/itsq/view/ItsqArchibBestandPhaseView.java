package de.cavdar.gui.itsq.view;

import de.cavdar.gui.itsq.design.ItsqArchivBestandPhasePanel;
import de.cavdar.gui.itsq.model.ItsqItem;

public class ItsqArchibBestandPhaseView extends ItsqArchivBestandPhasePanel implements ItsqItemSelectable {
    private ItsqItem selectedItem;

    public ItsqArchibBestandPhaseView() {
        super();
    }

    @Override
    public void setSelectedItem(ItsqItem item) {
        this.selectedItem = item;
        getLabelTitle().setText("ARCHIV-BESTAND " + selectedItem.getName());
    }

    public ItsqItem getSelectedItem() {
        return selectedItem;
    }
}
