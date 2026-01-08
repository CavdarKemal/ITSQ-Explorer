package de.cavdar.gui.itsq.model;

import java.io.File;

/**
 * Represents the REF-EXPORTS directory.
 */
public class ItsqRefExports implements ItsqItem {
    private final File dir;

    public ItsqRefExports(File dir) {
        this.dir = dir;
    }

    @Override
    public File getFile() {
        return dir;
    }

    @Override
    public String getName() {
        return dir.getName();
    }

    @Override
    public String toString() {
        return getName();
    }
}
