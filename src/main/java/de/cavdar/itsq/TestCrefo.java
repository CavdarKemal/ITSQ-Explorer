package de.cavdar.itsq;

import java.io.File;

public class TestCrefo {
    private String testFallName;
    private String testFallInfo;
    private Long itsqTestCrefoNr;
    private File itsqRexExportXmlFile;
    private boolean shouldBeExported;
    private boolean activated = true;
    private boolean exported = false;

    public TestCrefo(String testFallName, Long itsqTestCrefoNr, String testFallInfo, boolean shouldBeExported, File itsqRexExportXmlFile) {
        this.testFallName = testFallName;
        this.itsqTestCrefoNr = itsqTestCrefoNr;
        this.testFallInfo = testFallInfo;
        this.shouldBeExported = shouldBeExported;
        this.itsqRexExportXmlFile = itsqRexExportXmlFile;
    }

    public TestCrefo(TestCrefo theClone) {
        setTestFallName(theClone.getTestFallName());
        setItsqTestCrefoNr(theClone.getItsqTestCrefoNr());
        setTestFallInfo(theClone.getTestFallInfo());
        setItsqRexExportXmlFile(theClone.getItsqRexExportXmlFile());
        setActivated(theClone.isActivated());
        setExported(theClone.isExported());
        setShouldBeExported(theClone.isShouldBeExported());
    }

    public Long getItsqTestCrefoNr() {
        return itsqTestCrefoNr;
    }

    public void setItsqTestCrefoNr(Long itsqTestCrefoNr) {
        this.itsqTestCrefoNr = itsqTestCrefoNr;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public boolean isShouldBeExported() {
        return shouldBeExported;
    }

    public void setShouldBeExported(boolean shouldBeExported) {
        this.shouldBeExported = shouldBeExported;
    }

    public boolean isExported() {
        return exported;
    }

    public void setExported(boolean exported) {
        this.exported = exported;
    }

    public String getTestFallInfo() {
        return testFallInfo;
    }

    public void setTestFallInfo(String testFallInfo) {
        this.testFallInfo = testFallInfo;
    }

    public String getTestFallName() {
        return testFallName;
    }

    public void setTestFallName(String testFallName) {
        this.testFallName = testFallName;
    }

    public File getItsqRexExportXmlFile() {
        return itsqRexExportXmlFile;
    }

    public void setItsqRexExportXmlFile(File itsqRexExportXmlFile) {
        this.itsqRexExportXmlFile = itsqRexExportXmlFile;
    }

    @Override
    public String toString() {
        return testFallName + ":" + itsqTestCrefoNr;
    }

    public StringBuilder dump(String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix + testFallName + "\t" + itsqTestCrefoNr);
        stringBuilder.append("\t");
        if (itsqRexExportXmlFile != null) {
            stringBuilder.append(itsqRexExportXmlFile.getName());
        }
        return stringBuilder;
    }
}
