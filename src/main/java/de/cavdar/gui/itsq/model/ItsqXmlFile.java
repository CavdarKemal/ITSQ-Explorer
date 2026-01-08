package de.cavdar.gui.itsq.model;

import java.io.File;

/**
 * Represents an XML file in the ITSQ structure.
 */
public class ItsqXmlFile implements ItsqItem {
    private final File file;

    public ItsqXmlFile(File file) {
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
