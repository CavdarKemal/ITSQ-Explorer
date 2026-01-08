package de.cavdar.gui.itsq.model;

import java.io.File;

/**
 * Represents a customer directory (c0x) under REF-EXPORTS/PHASE-x.
 */
public class ItsqCustomer implements ItsqItem {
    private final File dir;
    private final String customerId;

    public ItsqCustomer(File dir) {
        this.dir = dir;
        this.customerId = dir.getName();
    }

    @Override
    public File getFile() {
        return dir;
    }

    @Override
    public String getName() {
        return customerId;
    }

    public String getCustomerId() {
        return customerId;
    }

    @Override
    public String toString() {
        return getName();
    }
}
