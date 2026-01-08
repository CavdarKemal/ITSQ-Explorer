package de.cavdar.gui.itsq.model;

import java.io.File;

/**
 * Represents a scenario directory (Relevanz-xyz) under a customer folder.
 */
public class ItsqScenario implements ItsqItem {
    private final File dir;
    private final String scenarioName;

    public ItsqScenario(File dir) {
        this.dir = dir;
        this.scenarioName = dir.getName();
    }

    @Override
    public File getFile() {
        return dir;
    }

    @Override
    public String getName() {
        return scenarioName;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    @Override
    public String toString() {
        return getName();
    }
}
