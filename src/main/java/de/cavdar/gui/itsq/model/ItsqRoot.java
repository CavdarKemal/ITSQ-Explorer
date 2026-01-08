package de.cavdar.gui.itsq.model;

import java.io.File;

/**
 * Represents the ITSQ root directory.
 */
public class ItsqRoot implements ItsqItem {
    private final File rootDir;

    public ItsqRoot(File rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    public File getFile() {
        return rootDir;
    }

    @Override
    public String getName() {
        return "ITSQ: " + rootDir.getName();
    }

    @Override
    public String toString() {
        return getName();
    }
}
