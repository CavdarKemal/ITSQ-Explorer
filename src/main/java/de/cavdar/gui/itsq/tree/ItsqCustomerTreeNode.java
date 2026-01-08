package de.cavdar.gui.itsq.tree;

import de.cavdar.gui.itsq.model.ItsqCustomer;

/**
 * Tree node for a customer directory (c0x).
 */
public class ItsqCustomerTreeNode extends ItsqTreeNode {

    public ItsqCustomerTreeNode(ItsqCustomer customer) {
        super(customer);
    }

    public ItsqCustomer getItsqCustomer() {
        return (ItsqCustomer) getItsqItem();
    }
}
