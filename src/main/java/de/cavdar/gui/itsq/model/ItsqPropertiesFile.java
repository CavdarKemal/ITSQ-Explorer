package de.cavdar.gui.itsq.model;

import java.io.File;

/**
 * Represents a .properties file in the ITSQ structure.
 */
public class ItsqPropertiesFile implements ItsqItem {
    private final File file;

    public ItsqPropertiesFile(File file) {
        this.file = file;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String toString() {
        return getName();
    }
}
