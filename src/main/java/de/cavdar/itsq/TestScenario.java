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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestScenario other)) return false;
        return Objects.equals(scenarioName, other.scenarioName)
                && Objects.equals(testCustomer, other.testCustomer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scenarioName, testCustomer);
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
        return new ArrayList<>(testFallNameToTestCrefoMap.values());
    }

    /**
     * Liefert eine unveränderbare View. Modifikationen müssen über
     * addTestCrefo() oder ähnliche Methoden erfolgen.
     */
    public Map<String, TestCrefo> getTestFallNameToTestCrefoMap() {
        return Collections.unmodifiableMap(testFallNameToTestCrefoMap);
    }

    /** Interner Zugriff für Cloning und Klassen-interne Operationen. */
    Map<String, TestCrefo> getTestFallNameToTestCrefoMapMutable() {
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
            // Performance: Vorab-Index nach crefoNr aufbauen — O(N+M) statt O(N·M)
            final Map<Long, List<File>> crefoToXmlFilesIndex = buildCrefoIndexMulti(allXmlFiles);
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
                            TimelineLogger.warn(this.getClass(),
                                    "TestCrefo mit dem Namen '{}' konnte nicht in der Map gefunden werden! Zeile: {}",
                                    testFallName, line);
                        } else {
                            File xmlFile = findInIndex(crefoToXmlFilesIndex, testFallName, crefoNr);
                            testCrefoExtender.fillExtraData(testCrefo, crefoNr, xmlFile);
                        }
                    } catch (Exception ex) {
                        TimelineLogger.error(this.getClass(),
                                "Exception in der Zeile '" + line + "' der Datei '" + thePropsFile.getName() + "'", ex);
                    }
                }
            });
        } catch (IOException ex) {
            TimelineLogger.error(this.getClass(),
                    "Exception beim Lesen der Properties-Datei '" + thePropsFile.getAbsolutePath() + "'", ex);
        }
    }

    protected void initItsqRefExportsData(Collection<File> archivBestandXmlFilesList) {
        try {
            // Performance: Vorab-Index aufbauen — O(N+M) statt O(N·M)
            final Map<Long, File> crefoToXmlIndex = buildCrefoIndex(archivBestandXmlFilesList);
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
                        File refExportFile = crefoToXmlIndex.get(crefoNr);
                        if (!shouldBeExported && (refExportFile != null && refExportFile.exists())) {
                            TimelineLogger.warn(this.getClass(),
                                    "Für die Test-Crefo '{}':{} dürfte es KEINE RefExport-XML existieren!",
                                    testFallName, crefoNr);
                        } else if (shouldBeExported && (refExportFile == null || !refExportFile.exists())) {
                            TimelineLogger.warn(this.getClass(),
                                    "Für die Test-Crefo '{}':{} müsste es EINE RefExport-XML existieren!",
                                    testFallName, crefoNr);
                        }
                        TestCrefo testCrefo = testFallNameToTestCrefoMap.get(testFallName);
                        if (testCrefo == null) {
                            testCrefo = new TestCrefo(testFallName, crefoNr, testFallInfo, shouldBeExported, refExportFile);
                            testFallNameToTestCrefoMap.put(testFallName, testCrefo);
                        }
                    } catch (Exception ex) {
                        TimelineLogger.error(this.getClass(),
                                "Exception in der Zeile '" + line + "' der Datei '" + itsqRefExportsPropsFile.getName() + "'", ex);
                    }
                }
            });
        } catch (IOException ex) {
            TimelineLogger.error(this.getClass(),
                    "Exception beim Lesen der Properties-Datei '" + itsqRefExportsPropsFile.getAbsolutePath() + "'", ex);
        }
    }

    /**
     * Baut einen Index aller XML-Dateien nach crefoNr.
     * Filename muss die crefoNr als Substring enthalten.
     * O(M) — einmaliger Durchlauf statt O(N·M) bei wiederholtem Linear-Scan.
     */
    private Map<Long, File> buildCrefoIndex(Collection<File> allXmlFiles) {
        Map<Long, File> index = new HashMap<>();
        for (File f : allXmlFiles) {
            String name = f.getName();
            // Crefo-Nummer aus Filename extrahieren — wir nehmen den ersten gefundenen passenden Eintrag
            // (entspricht dem alten Verhalten: nur ein File pro crefoNr)
            for (Long crefo : extractCrefoNumbers(name)) {
                index.putIfAbsent(crefo, f);
            }
        }
        return index;
    }

    /**
     * Wie buildCrefoIndex, aber sammelt alle Files pro crefoNr (nicht nur den ersten).
     * Wird gebraucht wenn zusätzlich nach testFallName-Substring gefiltert wird.
     */
    private Map<Long, List<File>> buildCrefoIndexMulti(Collection<File> allXmlFiles) {
        Map<Long, List<File>> index = new HashMap<>();
        for (File f : allXmlFiles) {
            String name = f.getName();
            for (Long crefo : extractCrefoNumbers(name)) {
                index.computeIfAbsent(crefo, k -> new ArrayList<>()).add(f);
            }
        }
        return index;
    }

    /**
     * Extrahiert alle Long-Zahlen aus dem Filename. Wird benötigt weil
     * die alte Implementation .contains(crefoNr+"") nutzte — d.h. die Nummer
     * konnte irgendwo im Namen vorkommen.
     */
    private List<Long> extractCrefoNumbers(String name) {
        List<Long> numbers = new ArrayList<>();
        int i = 0;
        while (i < name.length()) {
            if (Character.isDigit(name.charAt(i))) {
                int start = i;
                while (i < name.length() && Character.isDigit(name.charAt(i))) i++;
                try {
                    numbers.add(Long.parseLong(name.substring(start, i)));
                } catch (NumberFormatException ignored) { }
            } else {
                i++;
            }
        }
        return numbers;
    }

    /**
     * Sucht im Multi-Index nach einem File das BEIDE Substrings (testFallName + crefoNr) enthält.
     */
    private File findInIndex(Map<Long, List<File>> index, String testFallName, long crefoNr) {
        List<File> candidates = index.get(crefoNr);
        if (candidates == null) return null;
        for (File f : candidates) {
            if (f.getName().contains(testFallName)) {
                return f;
            }
        }
        return null;
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
