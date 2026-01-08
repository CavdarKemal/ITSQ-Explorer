package de.cavdar.gui.itsq.view;

import de.cavdar.gui.itsq.model.ItsqItem;

/**
 * Interface for views that can receive a selected tree item.
 * Views implementing this interface will be notified when
 * the user selects an item in the ITSQ tree.
 *
 * @author TemplateGUI
 * @version 2.0
 */
public interface ItsqItemSelectable {

    /**
     * Called when a tree item is selected.
     *
     * @param item the selected ItsqItem (may be null for root)
     */
    void setSelectedItem(ItsqItem item);
}
