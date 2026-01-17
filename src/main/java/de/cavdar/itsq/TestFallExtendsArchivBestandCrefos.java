package de.cavdar.itsq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

public class TestFallExtendsArchivBestandCrefos {

    public static final Logger LOGGER = LoggerFactory.getLogger(TestFallExtendsArchivBestandCrefos.class);
    private AB30MapperUtil ab30MapperUtil;
    private final Map<TestSupportClientKonstanten.TEST_PHASE, Map<String, TestCustomer>> activeCustomersMapMap;
    File testSetRootDir = new File(String.valueOf(TestSupportClientKonstanten.TEST_SET_DIR));
    File ab30RootDir = new File(testSetRootDir, TestSupportClientKonstanten.ARCHIV_BESTAND_ROOT);

    public TestFallExtendsArchivBestandCrefos(Map<TestSupportClientKonstanten.TEST_PHASE, Map<String, TestCustomer>> activeCustomersMapMap) {
        this.activeCustomersMapMap = activeCustomersMapMap;
        ab30MapperUtil = new AB30MapperUtil();
    }

    public void extendTestCrefos() throws Exception {
        Iterator<TestSupportClientKonstanten.TEST_PHASE> phaseIterator = activeCustomersMapMap.keySet().iterator();
        while (phaseIterator.hasNext()) {
            TestSupportClientKonstanten.TEST_PHASE testPhase = phaseIterator.next();
            doForPhase(testPhase);
        }
    }

    public void doForPhase(TestSupportClientKonstanten.TEST_PHASE testPhase) throws Exception {
        File ab30PhaseXmlsDir = new File(ab30RootDir, testPhase.getDirName());
        Map<String, TestCustomer> customerTestInfoMap = activeCustomersMapMap.get(testPhase);

        LOGGER.info("TestFallExtendsArchivBestandCrefos#doForPhase(" + testPhase.getDirName() + ") :: Initialisiere eine neue AB30XMLProperties-Map aus den Testfällen für in customerTestInfoMap befindlichen Testfällen...");
        Map<Long, AB30XMLProperties> ab30CrefoToPropertiesMap = ab30MapperUtil.initAb30CrefoPropertiesMapFromRefExports("", ab30PhaseXmlsDir, customerTestInfoMap);

        LOGGER.info("TestFallExtendsArchivBestandCrefos#doForPhase(" + testPhase.getDirName() + ") :: erweitere die Map um AB30XMLProperties-Einträge für Beteiligten bzw. Entschedidungsträger der TestCrefo, falls nicht vorhanden...");
        ab30CrefoToPropertiesMap = ab30MapperUtil.extendAb30CrefoPropertiesMapWithBtlgs("", ab30PhaseXmlsDir, ab30CrefoToPropertiesMap);

        File testCrefosFile = new File(ab30PhaseXmlsDir, TestSupportClientKonstanten.TEST_CREFOS_PROPS_FILENAME);
        LOGGER.info("TestFallExtendsArchivBestandCrefos#doForPhase(" + testPhase.getDirName() + ") :: Ergänze Attributes von AB30XMLProperties-Map  aus altem 'TestCrefos.properties' - Datei...");
        ab30CrefoToPropertiesMap = ab30MapperUtil.extendAb30CrefoPropertiesWithOldAttributes("", testCrefosFile, ab30CrefoToPropertiesMap);

        LOGGER.info("TestFallExtendsArchivBestandCrefos#doForPhase(" + testPhase.getDirName() + ") :: Erzeuge eine Datei 'ExtendedTestCrefos.properties'...");
        ab30MapperUtil.writeAb30CrefoToPropertiesMapToFile(new File(ab30PhaseXmlsDir, TestSupportClientKonstanten.EXTENDED_CREFOS_PROPS_FILENAME), ab30CrefoToPropertiesMap);

        LOGGER.info("TestFallExtendsArchivBestandCrefos#doForPhase(" + testPhase.getDirName() + ") :: Erzeuge eine Datei \"CrefosToCustomersMap.txt\", in der die Crefos gruppiert nach Kunde aufgelistet werden");
        ab30MapperUtil.writeCrefoToCustomerMappingFile(new File(ab30PhaseXmlsDir, TestSupportClientKonstanten.CREFOS_TO_CUSTOMERS_MAP_FILENAME), ab30CrefoToPropertiesMap);
    }

}