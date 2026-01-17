package de.cavdar.itsq;

import de.cavdar.gui.util.TimelineLogger;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TestScenario {

    private boolean activated = true;
    private TestCustomer testCustomer;
    private String scenarioName;
    private File itsqRefExportsFile;
    private File itsqRefExportsPropsFile;

    private final Map<String, TestCrefo> testFallNameToTestCrefoMap = new TreeMap<>();

    public TestScenario(TestCustomer testCustomer, String scenarioName) {
        this(testCustomer, scenarioName, new ArrayList<>());
    }

    public TestScenario(TestCustomer testCustomer, String scenarioName, List<File> refExportXmlsList) {
        this.testCustomer = testCustomer;
        this.scenarioName = scenarioName;
        File srcFile = new File(testCustomer.getItsqRefExportsDir(), scenarioName);
        File[] files = srcFile.listFiles(pathname -> pathname.getName().endsWith(".properties"));
        if (files == null) {
            throw new RuntimeException(String.format("Das Test-Scenario '%s' für den Kunden '%s' enthält keine Properties-Dateien!\nDer Pfad ist '%s'!", scenarioName, testCustomer.getCustomerName(), srcFile.getAbsolutePath()));
        }
        if (files.length != 1) {
            throw new RuntimeException(String.format("Das Test-Scenario '%s' für den Kunden '%s' enthält %d Properties-Dateien\nErlaubt ist genau eine Properties-Datei!", scenarioName, testCustomer.getCustomerName(), files.length));
        }

        itsqRefExportsFile = new File(testCustomer.getItsqRefExportsDir(), scenarioName);
        itsqRefExportsPropsFile = new File(itsqRefExportsFile, files[0].getName());
        initItsqRefExportsData(refExportXmlsList);
        extendTestCrefos(itsqRefExportsFile, itsqRefExportsPropsFile, new TestCrefoExtender() {
            @Override
            void fillExtraData(TestCrefo testCrefo, Long itsqCrefoNr, File xmlFile) {
                testCrefo.setItsqRexExportXmlFile(xmlFile);
            }
        });
    }

    public TestScenario(TestScenario toBeCloned) {
        setActivated(toBeCloned.isActivated());
        setScenarioName(toBeCloned.getScenarioName());
        setTestCustomer(toBeCloned.getTestCustomer());

        setItsqRefExportsFile(toBeCloned.getItsqRefExportsFile());
        setItsqRefExportsPropsFile(toBeCloned.getItsqRefExportsPropsFile());

        testFallNameToTestCrefoMap.putAll(toBeCloned.getTestFallNameToTestCrefoMap());
    }

    @Override
    public String toString() {
        return scenarioName + " #" + testFallNameToTestCrefoMap.size();
    }

    public TestCustomer getTestCustomer() {
        return testCustomer;
    }

    public void setTestCustomer(TestCustomer testCustomer) {
        this.testCustomer = testCustomer;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public String getCusomerKey() {
        return testCustomer.getCustomerKey();
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public List<TestCrefo> getTestCrefosAsList() {
        return new ArrayList(getTestFallNameToTestCrefoMap().values());
    }

    public Map<String, TestCrefo> getTestFallNameToTestCrefoMap() {
        return testFallNameToTestCrefoMap;
    }

    /*******************     ItsqRefExports  *************************/
    public File getItsqRefExportsFile() {
        return itsqRefExportsFile;
    }

    public void setItsqRefExportsFile(File itsqRefExportsFile) {
        this.itsqRefExportsFile = itsqRefExportsFile;
    }

    public File getItsqRefExportsPropsFile() {
        return itsqRefExportsPropsFile;
    }

    public void setItsqRefExportsPropsFile(File itsqRefExportsPropsFile) {
        this.itsqRefExportsPropsFile = itsqRefExportsPropsFile;
    }

    /*------------------     ItsqRefExports  ------------------------*/

    private void extendTestCrefos(final File theFile, final File thePropsFile, TestCrefoExtender testCrefoExtender) {
        if (!theFile.exists()) {
            return;
        }
        try {
            final Collection<File> allXmlFiles = FileUtils.listFiles(theFile, new String[]{"xml"}, true);
            final List<String> propsFileContent = FileUtils.readLines(thePropsFile);
            propsFileContent.forEach(line -> {
                if (!line.isBlank() && !line.startsWith("#")) {
                    String[] splitEqual = line.split("=");
                    try {
                        String testFallName = splitEqual[0];
                        final String[] splitHash = splitEqual[1].trim().split("#");
                        long crefoNr = Long.parseLong(splitHash[0].trim());
                        TestCrefo testCrefo = testFallNameToTestCrefoMap.get(testFallName);
                        if (testCrefo == null) {
                            String errorStr = "TestCrefo mit dem Namen: " + testFallName + " konnte nicht in der Map gefunden werden!\n\t" + line;
                            TimelineLogger.info(this.getClass(), errorStr);
                        }
                        if (testCrefo != null) {
                            File xmlFile = findXmlFileForTestfallAndCrefo(allXmlFiles, testFallName, crefoNr);
                            testCrefoExtender.fillExtraData(testCrefo, crefoNr, xmlFile);
                        }
                    } catch (Exception ex) {
                        String errorStr = "Exception in der Zeile '" + line + "' der Datei '" + thePropsFile.getName() + "':\n" + ex.getMessage();
                        TimelineLogger.info(this.getClass(), errorStr, ex);
                    }
                }
            });
        } catch (IOException ex) {
            String errorStr = "Exception beim Lesen der Properties-Datei '" + thePropsFile.getAbsolutePath() + "'!\n" + ex.getMessage();
            TimelineLogger.info(this.getClass(), errorStr, ex);
        }
    }

    protected void initItsqRefExportsData(Collection<File> archivBestandXmlFilesList) {
        try {
            List<String> propsFileContent = FileUtils.readLines(itsqRefExportsPropsFile);
            propsFileContent.forEach(line -> {
                if (!line.isBlank() && !line.startsWith("#")) {
                    String[] splitEqual = line.split("=");
                    try {
                        String testFallName = splitEqual[0].trim();
                        boolean shouldBeExported = !testFallName.startsWith("n");
                        final String[] splitHash = splitEqual[1].trim().split("#");
                        long crefoNr = Long.parseLong(splitHash[0].trim());
                        String testFallInfo = (splitHash.length > 1) ? splitHash[1] : "Norbert's faulheit!";
                        File refExportFile = findXmlFileForCrefo(archivBestandXmlFilesList, crefoNr);
                        if (!shouldBeExported && (refExportFile != null && refExportFile.exists())) {
                            String errorStr = "Für die Test-Crefo '" + testFallName + "':" + crefoNr + " dürfte es KEINE RefExport-XML existieren!";
                            TimelineLogger.info(this.getClass(), errorStr);
                        } else if (shouldBeExported && (refExportFile == null || !refExportFile.exists())) {
                            String errorStr = "Für die Test-Crefo '" + testFallName + "':" + crefoNr + " müsste es EINE RefExport-XML existieren!";
                            TimelineLogger.info(this.getClass(), errorStr);
                        }
                        TestCrefo testCrefo = testFallNameToTestCrefoMap.get(testFallName);
                        if (testCrefo == null) {
                            testCrefo = new TestCrefo(testFallName, crefoNr, testFallInfo, shouldBeExported, refExportFile);
                            testFallNameToTestCrefoMap.put(testFallName, testCrefo);
                        }
                    } catch (Exception ex) {
                        String errorStr = "\n!!! Exception in der Zeile '" + line + "' der Datei '" + itsqRefExportsPropsFile.getName() + "':\n" + ex.getMessage();
                        TimelineLogger.info(this.getClass(), errorStr);
                    }
                }
            });
        } catch (IOException ex) {
            String errorStr = "\n!!! Exception beim Lesen der Properties-Datei '" + itsqRefExportsPropsFile.getAbsolutePath() + "'!\n" + ex.getMessage();
            TimelineLogger.error(this.getClass(), errorStr);
        }
    }

    private File findXmlFileForCrefo(Collection<File> allXmlFiles, long crefoNr) {
        List<File> collect = allXmlFiles.stream().filter(theFile -> theFile.getName().contains(crefoNr + "")).collect(Collectors.toList());
        return collect.isEmpty() ? null : collect.get(0);
    }

    private File findXmlFileForTestfallAndCrefo(Collection<File> allXmlFiles, String testFallName, long crefoNr) {
        List<File> collect = allXmlFiles.stream().filter(theFile -> theFile.getName().contains(testFallName) && theFile.getName().contains(crefoNr + "")).collect(Collectors.toList());
        return collect.isEmpty() ? null : collect.get(0);
    }

    public StringBuilder dump(String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix + "\tScenario: " + scenarioName);
        stringBuilder.append(prefix + "\t\tTest-Crefos");
        stringBuilder.append(prefix + "\t\t\ttestFallName\titsqTestCrefoNr\tpseudoCrefoNr\titsqPhase2XmlFile\titsqRexExportXmlFile\tpseudoRefExportXmlFile\tcollectedXmlFile\trestoredXmlFile");
        testFallNameToTestCrefoMap.entrySet().forEach(testCrefoEntry -> {
            TestCrefo testCrefo = testCrefoEntry.getValue();
            stringBuilder.append(testCrefo.dump(prefix + "\t\t\t"));
        });
        return stringBuilder;
    }

    private abstract class TestCrefoExtender {
        abstract void fillExtraData(TestCrefo testCrefo, Long crefoNrFromPropsFile, File xmlFile);
    }
}
