package de.cavdar.gui.itsq.view;

import de.cavdar.gui.itsq.design.ItsqScenarioPanel;
import de.cavdar.gui.itsq.model.ItsqItem;

public class ItsqScenarioView extends ItsqScenarioPanel implements ItsqItemSelectable {
    private ItsqItem selectedItem;

    public ItsqScenarioView() {
        super();
    }

    @Override
    public void setSelectedItem(ItsqItem item) {
        this.selectedItem = item;
        getLabelTitle().setText("SCENARIO " + selectedItem.getName());
    }

    public ItsqItem getSelectedItem() {
        return selectedItem;
    }
}
