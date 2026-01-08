package de.cavdar.gui.itsq.tree;

import de.cavdar.gui.itsq.model.ItsqScenario;

/**
 * Tree node for a scenario directory (Relevanz-xyz).
 */
public class ItsqScenarioTreeNode extends ItsqTreeNode {

    public ItsqScenarioTreeNode(ItsqScenario scenario) {
        super(scenario);
    }

    public ItsqScenario getItsqScenario() {
        return (ItsqScenario) getItsqItem();
    }
}
