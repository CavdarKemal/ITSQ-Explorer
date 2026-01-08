package de.cavdar.gui.itsq.model;

import java.io.File;

/**
 * Represents the ARCHIV-BESTAND directory.
 */
public class ItsqArchivBestand implements ItsqItem {
    private final File dir;

    public ItsqArchivBestand(File dir) {
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
