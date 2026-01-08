package de.cavdar.gui.itsq.view;

import de.cavdar.gui.itsq.design.ItsqRootPanel;
import de.cavdar.gui.itsq.model.ItsqItem;

public class ItsqRootView extends ItsqRootPanel implements ItsqItemSelectable {
    private ItsqItem selectedItem;

    public ItsqRootView() {
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
