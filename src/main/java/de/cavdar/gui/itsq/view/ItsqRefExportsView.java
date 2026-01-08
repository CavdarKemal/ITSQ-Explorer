package de.cavdar.gui.itsq.view;

import de.cavdar.gui.itsq.design.ItsqRefExportsPanel;
import de.cavdar.gui.itsq.model.ItsqItem;

public class ItsqRefExportsView extends ItsqRefExportsPanel implements ItsqItemSelectable {
    private ItsqItem selectedItem;

    public ItsqRefExportsView() {
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
