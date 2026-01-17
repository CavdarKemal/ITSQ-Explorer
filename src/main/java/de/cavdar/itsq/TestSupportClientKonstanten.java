package de.cavdar.itsq;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

public interface TestSupportClientKonstanten {
    String TEST_CREFOS_PROPS_FILENAME = "TestCrefos.properties";
    String CREFOS_TO_CUSTOMERS_MAP_FILENAME = "CrefosToCustomersMap.txt";
    String EXTENDED_CREFOS_PROPS_FILENAME = "ExtendedTestCrefos.properties";
    File TEST_SET_DIR = new File(System.getProperty("user.dir"), "ITSQ");
    String ARCHIV_BESTAND_ROOT = "ARCHIV-BESTAND";
    String PHASE_1 = "PHASE-1";
    String PHASE_2 = "PHASE-2";
    String ARCHIV_BESTAND_PHASE_1 = ARCHIV_BESTAND_ROOT + "/" + PHASE_1;
    String ARCHIV_BESTAND_PHASE_2 = ARCHIV_BESTAND_ROOT + "/" + PHASE_2;
    String REF_EXPORTS_ROOT = "REF-EXPORTS";
    String REF_EXPORTS_PHASE_1 = REF_EXPORTS_ROOT + "/" + PHASE_1;
    String REF_EXPORTS_PHASE_2 = REF_EXPORTS_ROOT + "/" + PHASE_2;
    Pattern CRF_XML_PATTERN = Pattern.compile("\\d{10}.xml");
    // <arc:vorraetig-bis>2099-03-12+01:00</arc:vorraetig-bis>
    Pattern DATE_PATTERN = Pattern.compile("[1-2]{1}[0-9]{3}-[0-1]{1}[0-9]{1}-[0-2]{1}[0-9]{1}\\+[0-9]{2}:[0-9]{2}");
    // <arc:letzte-archivaenderung>2020-03-19T09:53:24.852+01:00</arc:letzte-archivaenderung>
    Pattern DATE_TIME_PATTERN = Pattern.compile("[1-2]{1}[0-9]{3}-[0-1]{1}[0-9]{1}-[0-2]{1}[0-9]{1}T[0-9]{2}:[0-9]{2}:[0-9]{2}(.[0-9]{3})?\\+[0-9]{2}:[0-9]{2}");
    SimpleDateFormat DATE_FORMAT_YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat DATE_FORMAT_YYYY_MM_DD_HH_MM_SS = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");

    enum TEST_PHASE {
        PHASE_1("PHASE-1"),
        PHASE_2("PHASE-2");
        private final String dirName;

        TEST_PHASE(String dirName) {
            this.dirName = dirName;
        }

        public String getDirName() {
            return dirName;
        }
    }
}
