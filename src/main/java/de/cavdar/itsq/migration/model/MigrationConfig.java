package de.cavdar.itsq.migration.model;

import java.io.File;

/**
 * Konfiguration fuer die ITSQ-Migration von OLD nach NEW Struktur.
 */
public class MigrationConfig {
    private File sourceOldPath;
    private File targetNewPath;
    private boolean createBackup;
    private boolean overwriteExisting;
    private boolean dryRun;

    public MigrationConfig() {
        this.createBackup = true;
        this.overwriteExisting = false;
        this.dryRun = false;
    }

    public MigrationConfig(File sourceOldPath, File targetNewPath) {
        this();
        this.sourceOldPath = sourceOldPath;
        this.targetNewPath = targetNewPath;
    }

    public File getSourceOldPath() {
        return sourceOldPath;
    }

    public void setSourceOldPath(File sourceOldPath) {
        this.sourceOldPath = sourceOldPath;
    }

    public File getTargetNewPath() {
        return targetNewPath;
    }

    public void setTargetNewPath(File targetNewPath) {
        this.targetNewPath = targetNewPath;
    }

    public boolean isCreateBackup() {
        return createBackup;
    }

    public void setCreateBackup(boolean createBackup) {
        this.createBackup = createBackup;
    }

    public boolean isOverwriteExisting() {
        return overwriteExisting;
    }

    public void setOverwriteExisting(boolean overwriteExisting) {
        this.overwriteExisting = overwriteExisting;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    // OLD-Struktur Pfade
    public File getArchivBestandPh1Dir() {
        return new File(sourceOldPath, "ARCHIV-BESTAND-PH1");
    }

    public File getArchivBestandPh2Dir() {
        return new File(sourceOldPath, "ARCHIV-BESTAND-PH2");
    }

    public File getRefExportsDir() {
        return new File(sourceOldPath, "REF-EXPORTS");
    }

    // NEW-Struktur Pfade
    public File getNewArchivBestandDir() {
        return new File(targetNewPath, "ARCHIV-BESTAND");
    }

    public File getNewArchivBestandPhase1Dir() {
        return new File(getNewArchivBestandDir(), "PHASE-1");
    }

    public File getNewArchivBestandPhase2Dir() {
        return new File(getNewArchivBestandDir(), "PHASE-2");
    }

    public File getNewRefExportsDir() {
        return new File(targetNewPath, "REF-EXPORTS");
    }

    public File getNewRefExportsPhase1Dir() {
        return new File(getNewRefExportsDir(), "PHASE-1");
    }

    public File getNewRefExportsPhase2Dir() {
        return new File(getNewRefExportsDir(), "PHASE-2");
    }

    public boolean isValid() {
        return sourceOldPath != null && sourceOldPath.exists() && sourceOldPath.isDirectory()
                && targetNewPath != null;
    }

    @Override
    public String toString() {
        return "MigrationConfig{" +
                "sourceOldPath=" + sourceOldPath +
                ", targetNewPath=" + targetNewPath +
                ", createBackup=" + createBackup +
                ", overwriteExisting=" + overwriteExisting +
                ", dryRun=" + dryRun +
                '}';
    }
}
