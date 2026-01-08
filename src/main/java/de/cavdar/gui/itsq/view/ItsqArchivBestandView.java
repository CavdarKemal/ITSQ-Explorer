package de.cavdar.gui.itsq.view;

import de.cavdar.gui.itsq.design.ItsqArchivBestandPanel;
import de.cavdar.gui.itsq.model.ItsqItem;

public class ItsqArchivBestandView extends ItsqArchivBestandPanel implements ItsqItemSelectable {
    private ItsqItem selectedItem;

    public ItsqArchivBestandView() {
        super();
    }

    @Override
    public void setSelectedItem(ItsqItem item) {
        this.selectedItem = item;
    }

    public ItsqItem getSelectedItem() {
        return selectedItem;
    }
}
