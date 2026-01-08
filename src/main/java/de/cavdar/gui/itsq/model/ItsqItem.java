package de.cavdar.gui.itsq.model;

import java.io.File;

/**
 * Base interface for all ITSQ tree items.
 * Each item represents an element in the ITSQ directory structure.
 */
public interface ItsqItem {

    /**
     * Returns the underlying file or directory.
     */
    File getFile();

    /**
     * Returns the display name for this item.
     */
    String getName();

    /**
     * Returns true if this item represents a file (not a directory).
     */
    default boolean isFile() {
        return getFile() != null && getFile().isFile();
    }

    /**
     * Returns true if this item represents a directory.
     */
    default boolean isDirectory() {
        return getFile() != null && getFile().isDirectory();
    }
}
