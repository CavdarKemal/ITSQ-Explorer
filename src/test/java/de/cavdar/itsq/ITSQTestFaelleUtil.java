package de.cavdar.itsq;

import de.cavdar.gui.util.TimelineLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ITSQTestFaelleUtil {

    File srcArchivBestandDir;
    File srcRefExportXmlsDir;
    private Map<String, TestCustomer> testCustomerMap;

    public ITSQTestFaelleUtil() throws Exception {
        srcArchivBestandDir = new File(TestSupportClientKonstanten.TEST_SET_DIR, TestSupportClientKonstanten.ARCHIV_BESTAND_ROOT);
        srcRefExportXmlsDir = new File(TestSupportClientKonstanten.TEST_SET_DIR, TestSupportClientKonstanten.REF_EXPORTS_ROOT);
    }

    public static Map<Long, Path> scanSourceDirectory(File srcDir) throws IOException {
        Map<Long, Path> scanResult = new LinkedHashMap<>();
        Path srcPath = Paths.get(srcDir.getAbsolutePath());
        try (Stream<Path> directoryContentStream = Files.list(srcPath)) {
            directoryContentStream
                    .filter(p -> TestSupportClientKonstanten.CRF_XML_PATTERN.matcher(p.getFileName().toString()).matches())
                    .forEach(p -> {
                        Long crefonummer = parseCrefo(p.getFileName().toString().substring(0, 10));
                        if (crefonummer != null) {
                            scanResult.put(crefonummer, p);
                        }
                    });
        }
        return scanResult;
    }

    public static Long parseCrefo(String string) {
        try {
            long crf = Long.parseLong(string);
            if (crf < 1000000000L || crf > 9999999999L) {
                return null;
            }
            return crf;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String getShortPath(File theFile, String pathPrefix) {
        String absolutePath = theFile.getAbsolutePath();
        if (pathPrefix.startsWith("./")) {
            pathPrefix = pathPrefix.substring(2);
        }
        return absolutePath.substring(absolutePath.indexOf(pathPrefix));
    }

    public List<TestCustomer> getTestCustomerList(TestSupportClientKonstanten.TEST_PHASE testPhase) {
        if (testCustomerMap == null) {
            testCustomerMap = getCustomerTestInfoMap(testPhase);
        }
        return new ArrayList<>(testCustomerMap.values());
    }

    public TestCustomer getTestCustomer(String customerKey, TestSupportClientKonstanten.TEST_PHASE testPhase) {
        if (testCustomerMap == null) {
            testCustomerMap = getCustomerTestInfoMap(testPhase);
        }
        return testCustomerMap.get(customerKey);
    }

    public Map<String, TestCustomer> getCustomerTestInfoMap(TestSupportClientKonstanten.TEST_PHASE testPhase) {
        Map<String, TestCustomer> customerTestInfoMap = new TreeMap<>();
        File srcArchivBestandPhaseXDir = new File(srcArchivBestandDir,
                (TestSupportClientKonstanten.TEST_PHASE.PHASE_1.equals(testPhase) ?
                        TestSupportClientKonstanten.TEST_PHASE.PHASE_1.getDirName() :
                        TestSupportClientKonstanten.TEST_PHASE.PHASE_2.getDirName()));
        File srcRefExportXmlsPhaseXDir = new File(srcRefExportXmlsDir,
                (TestSupportClientKonstanten.TEST_PHASE.PHASE_1.equals(testPhase) ?
                        TestSupportClientKonstanten.TEST_PHASE.PHASE_1.getDirName() :
                        TestSupportClientKonstanten.TEST_PHASE.PHASE_2.getDirName()));
        List<File> customerDirsList = Arrays.stream(srcRefExportXmlsPhaseXDir.listFiles((dir, name) -> new File(dir, name).isDirectory())).collect(Collectors.toList());
        customerDirsList.forEach(customerDir -> {
            String customerKey = customerDir.getName().toUpperCase(Locale.ROOT);
            TestCustomer testCustomer = new TestCustomer(customerKey, customerKey);
            testCustomer.setItsqAB30XmlsDir(srcArchivBestandPhaseXDir);
            testCustomer.setItsqRefExportsDir(customerDir);
            customerTestInfoMap.put(customerKey, testCustomer);
            List<File> scenariDirsList = Arrays.stream(customerDir.listFiles((dir, name) -> new File(dir, name).isDirectory())).collect(Collectors.toList());
            for (File scenariDir : scenariDirsList) {
                File[] listFiles = scenariDir.listFiles((dir, name) -> name.endsWith(".xml"));
                List<File> refExportXmlFileList = Arrays.stream(listFiles).collect(Collectors.toList());
                TestScenario testScenario = new TestScenario(testCustomer, scenariDir.getName(), refExportXmlFileList);
                testCustomer.addTestScenario(testScenario);
            }
        });
        return customerTestInfoMap;
    }

    public Map<TestSupportClientKonstanten.TEST_PHASE, Map<String, TestCustomer>> getCustomerTestInfoMapMap() {
        Map<TestSupportClientKonstanten.TEST_PHASE, Map<String, TestCustomer>> customerTestInfoMapMap = new HashMap<>();
        for (TestSupportClientKonstanten.TEST_PHASE testPhase : TestSupportClientKonstanten.TEST_PHASE.values()) {
            TimelineLogger.info(this.getClass(), "ITSQTestFaelleUtil#getCustomerTestInfoMapMap:: TestCustomer-Map für die Phase " + testPhase.name() + " wird gebaut...");
            Map<String, TestCustomer> customerTestInfoMap = getCustomerTestInfoMap(testPhase);
            customerTestInfoMapMap.put(testPhase, customerTestInfoMap);
        }
        TimelineLogger.info(this.getClass(), "ITSQTestFaelleUtil#getCustomerTestInfoMapMap:: TestCustomer-MapMap für beide Phases gebaut");
        return customerTestInfoMapMap;
    }
}
