package de.cavdar.gui.itsq.model;

import java.io.File;

/**
 * Represents a PHASE-x directory under REF-EXPORTS.
 */
public class ItsqRefExportsPhase implements ItsqItem {
    private final File dir;
    private final String phaseName;

    public ItsqRefExportsPhase(File dir) {
        this.dir = dir;
        this.phaseName = dir.getName();
    }

    @Override
    public File getFile() {
        return dir;
    }

    @Override
    public String getName() {
        return phaseName;
    }

    public String getPhaseName() {
        return phaseName;
    }

    @Override
    public String toString() {
        return getName();
    }
}
