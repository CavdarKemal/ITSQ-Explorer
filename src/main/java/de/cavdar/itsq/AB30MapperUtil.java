package de.cavdar.itsq;

import de.cavdar.gui.util.TimelineLogger;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class AB30MapperUtil {
    protected static final String TAG_STEUERUNGSDATEN = "STEUERUNGSDATEN";
    protected static final String TAG_FIRMENBETEILIGTER = "FIRMENBETEILIGTER";
    protected static final String TAG_VERFAHRENSBETEILIGTER = "VERFAHRENSBETEILIGTER";

    public Map<Long, AB30XMLProperties> extendAb30CrefoPropertiesWithOldAttributes(String strInfoPrefix, File testCrefosFile, Map<Long, AB30XMLProperties> ab30CrefoToPropertiesMap) throws IOException {
        Map<Long, AB30XMLProperties> oldAb30CrefoToPropertiesMap = initAb30CrefoPropertiesMap(testCrefosFile);
        Iterator<Long> iterator = oldAb30CrefoToPropertiesMap.keySet().iterator();
        while (iterator.hasNext()) {
            Long crefoNr = iterator.next();
            AB30XMLProperties oldAb30XMLProperties = oldAb30CrefoToPropertiesMap.get(crefoNr);
            AB30XMLProperties newAb30XMLProperties = ab30CrefoToPropertiesMap.get(crefoNr);
            if (newAb30XMLProperties != null) {
                TimelineLogger.info(this.getClass(), strInfoPrefix + " textendAb30CrefoPropertiesWithOldAttributes():: Ergänze Attributes von AB30XMLProperties '" + newAb30XMLProperties + "' aus altem 'TestCrefos.properties' - Datei...");
                Long auftragClz = oldAb30XMLProperties.getAuftragClz();
                if (auftragClz != null) {
                    newAb30XMLProperties.setAuftragClz(auftragClz);
                }
                AB30XMLProperties.BILANZEN_TYPE bilanzType = oldAb30XMLProperties.getBilanzType();
                if (bilanzType != null) {
                    newAb30XMLProperties.setBilanzType(bilanzType);
                }
                AB30XMLProperties.EH_PROD_AUFTR_TYPE ehProduktAuftragType = oldAb30XMLProperties.getEhProduktAuftragType();
                if (ehProduktAuftragType != null) {
                    newAb30XMLProperties.setEhProdAuftrType(ehProduktAuftragType);
                }
                boolean mitCtaStatistik = oldAb30XMLProperties.isMitCtaStatistik();
                newAb30XMLProperties.setMitCtaStatistik(mitCtaStatistik);
                boolean mitDsgVoSperre = oldAb30XMLProperties.isMitDsgVoSperre();
                newAb30XMLProperties.setDsgVoSperre(mitDsgVoSperre);
                TimelineLogger.info(this.getClass(), strInfoPrefix + " extendAb30CrefoPropertiesWithOldAttributes():: Neue AB30XMLProperties:" + newAb30XMLProperties);
            } else {
                TimelineLogger.info(this.getClass(), strInfoPrefix + " extendAb30CrefoPropertiesWithOldAttributes():: AB30XMLProperties " + oldAb30XMLProperties + " aus altem 'TestCrefos.properties' - Datei ist nicht mehr im Testpaket!");
                File xmlToRename = new File(testCrefosFile.getParentFile(), oldAb30XMLProperties.getCrefoNr() + ".xml");
                if (xmlToRename.exists()) {
                    xmlToRename.renameTo(new File(xmlToRename.getParentFile(), xmlToRename.getName() + ".deleted"));
                }
            }
        }
        return ab30CrefoToPropertiesMap;
    }

    public Map<Long, AB30XMLProperties> extendAb30CrefoPropertiesMapWithBtlgs(String strInfoPrefix, File archivBestandsPhaseFile, Map<Long, AB30XMLProperties> ab30CrefoToPropertiesMap) throws Exception {
        Map<Long, AB30XMLProperties> ab30CrefoToPropertiesResult = new TreeMap<>(ab30CrefoToPropertiesMap);
        Iterator<Long> iterator = ab30CrefoToPropertiesMap.keySet().iterator();
        while (iterator.hasNext()) {
            Long crefoNr = iterator.next();
            AB30XMLProperties ab30XMLProperties = ab30CrefoToPropertiesMap.get(crefoNr);
            if (ab30XMLProperties == null) {
                throw new IllegalStateException("Für die Test-Crefo " + crefoNr + " existiert kein AB30XMLProperties-Eintrag in der Map!");
            }
            // behandle die Beteiligten...
            handleBTLGsFromCrefoXMLFile(strInfoPrefix, archivBestandsPhaseFile, ab30XMLProperties, ab30CrefoToPropertiesResult);
            TimelineLogger.info(this.getClass(), strInfoPrefix + "Mapping für Test-Crefo " + crefoNr + " wird angelegt.");
            ab30CrefoToPropertiesResult.put(crefoNr, ab30XMLProperties);
        }
        return ab30CrefoToPropertiesResult;
    }

    public Map<Long, AB30XMLProperties> initAb30CrefoPropertiesMapFromRefExports(String strInfoPrefix, File archivBestandsPhaseFile, Map<String, TestCustomer> customerTestInfoMap) throws Exception {
        Map<Long, AB30XMLProperties> ab30CrefoToPropertiesMap = new TreeMap<>();
        for (Map.Entry<String, TestCustomer> testCustomerEntry : customerTestInfoMap.entrySet()) {
            TestCustomer testCustomer = testCustomerEntry.getValue();
            List<TestCrefo> allTestCrefos = testCustomer.getAllTestCrefos(false, false);  // nur die aktive und positive+negative Testfälle!
            for (TestCrefo testCrefo : allTestCrefos) {
                File crefoXmlFile = new File(archivBestandsPhaseFile, testCrefo.getItsqTestCrefoNr() + ".xml");
                // prüfe, ob die XML-Datei für die Test-Crefo existiert
                if (!crefoXmlFile.exists()) {
                    // die XML-Datei für die Test-Crefo existiert nicht,
                    throw new RuntimeException("Die XML-Datei für die Test-Crefo " + testCrefo + " existiert nicht!");
                }
                AB30XMLProperties ab30XMLProperties = ab30CrefoToPropertiesMap.get(testCrefo.getItsqTestCrefoNr());
                if (ab30XMLProperties != null) {
                    final List<String> usedByCustomersList = ab30XMLProperties.getUsedByCustomersList();
                    if (!usedByCustomersList.contains(testCustomer.getCustomerKey())) {
                        usedByCustomersList.add(testCustomer.getCustomerKey());
                    }
                } else {
                    ab30XMLProperties = new AB30XMLProperties(testCrefo.getItsqTestCrefoNr());
                    ab30XMLProperties.getUsedByCustomersList().add(testCustomer.getCustomerKey());
                }
                ab30CrefoToPropertiesMap.put(testCrefo.getItsqTestCrefoNr(), ab30XMLProperties);
            }
        }
        return ab30CrefoToPropertiesMap;
    }

    public Map<Long, AB30XMLProperties> initAb30CrefoPropertiesMap(File propsFile) throws IOException {
        Map<Long, AB30XMLProperties> ab30CrefoToPropertiesMap = new HashMap<>();
        if (!propsFile.exists()) {
            return ab30CrefoToPropertiesMap;
        }
        List<String> strLines = FileUtils.readLines(propsFile);
        int version = 1;
        for (String strLine : strLines) {
            if (!strLine.isBlank()) {
                if (strLine.startsWith(AB30XMLProperties.VERSION_STR)) {
                    String[] split = strLine.split("::");
                    if (split.length > 1) {
                        version = Integer.valueOf(split[1].trim());
                    }
                }
                if (!strLine.startsWith("#")) {
                    AB30XMLProperties ab30XMLProperties = new AB30XMLProperties(strLine, version);
                    ab30CrefoToPropertiesMap.put(ab30XMLProperties.getCrefoNr(), ab30XMLProperties);
                }
            }
        }
        return ab30CrefoToPropertiesMap;
    }

    public void writeCrefoToCustomerMappingFile(File newFile, Map<Long, AB30XMLProperties> ab30CrefoToPropertiesMap) throws IOException {
        if (newFile.exists()) {
            newFile.renameTo(new File(newFile.getParentFile(), newFile.getName() + ".old"));
            newFile.delete();
        }
        Map<String, List<Long>> customerToCrefoListMap = new HashMap<>();
        for (Long creoNummer : ab30CrefoToPropertiesMap.keySet()) {
            AB30XMLProperties ab30XMLProperties = ab30CrefoToPropertiesMap.get(creoNummer);
            List<String> customersList = ab30XMLProperties.getUsedByCustomersList();
            customersList.stream().forEach(customerKey -> {
                if (customerToCrefoListMap.containsKey(customerKey)) {
                    customerToCrefoListMap.get(customerKey).add(creoNummer);
                } else {
                    customerToCrefoListMap.put(customerKey, new ArrayList<>());
                }
            });
        }
        List<String> strLines = new ArrayList<>();
        for (String customerKey : customerToCrefoListMap.keySet()) {
            strLines.add(customerKey);
            List<Long> crefosList = customerToCrefoListMap.get(customerKey);
            strLines.add("\t" + crefosList);
        }
        FileUtils.writeLines(newFile, strLines);
    }

    public void writeAb30CrefoToPropertiesMapToFile(File newPorpsFile, Map<Long, AB30XMLProperties> ab30CrefoToPropertiesMap) throws IOException {
        if (newPorpsFile.exists()) {
            newPorpsFile.renameTo(new File(newPorpsFile.getParentFile(), TestSupportClientKonstanten.EXTENDED_CREFOS_PROPS_FILENAME + ".old"));
            newPorpsFile.delete();
        }
        List<String> strLines = new ArrayList<>();
        strLines.add(AB30XMLProperties.HEADER);
        strLines.add(AB30XMLProperties.VERSION_STR + " " + AB30XMLProperties.VERSION);
        for (Map.Entry<Long, AB30XMLProperties> ab30XMLPropertiesEntry : ab30CrefoToPropertiesMap.entrySet()) {
            AB30XMLProperties ab30XMLProperties = ab30XMLPropertiesEntry.getValue();
            strLines.add(ab30XMLProperties.toString());
        }
        strLines.sort(Comparator.naturalOrder());
        FileUtils.writeLines(newPorpsFile, strLines);
    }

    public void handleBTLGsFromCrefoXMLFile(String strInfoPrefix, File ab30XmlsDir, AB30XMLProperties ab30XMLPropertiesCrefo, Map<Long, AB30XMLProperties> ab30CrefoToPropertiesMap) throws Exception {
        Long testCrefo = ab30XMLPropertiesCrefo.getCrefoNr();
        TimelineLogger.info(this.getClass(), strInfoPrefix + " handleBTLGsFromCrefoXMLFile(): Suche Beteiligten für Testfall-Crefo " + testCrefo + " in der XML-Datei...");

        // prüfe, ob die XML-Datei für die Test-Crefo existiert
        File crefoXmlFile = new File(ab30XmlsDir, testCrefo + ".xml");

        // Alle XML-Tags "crefonummer" aus der XML-Datei ermitteln...
        Map<String, List<Long>> crefoListsMap = parseCrefosFromXmlContent(crefoXmlFile);

        List<Long> btlgCrefosList = crefoListsMap.get(TAG_FIRMENBETEILIGTER);
        for (Long btlgCrefo : btlgCrefosList) {
            handleBtlgOrEntg(strInfoPrefix, "Beteiligten", btlgCrefo, ab30XmlsDir, ab30XMLPropertiesCrefo, ab30CrefoToPropertiesMap);
        }
        List<Long> verfBtlgCrefosList = crefoListsMap.get(TAG_VERFAHRENSBETEILIGTER);
        for (Long verfBtlgCrefo : verfBtlgCrefosList) {
            handleBtlgOrEntg(strInfoPrefix, "Verfahrens-Beteiligten", verfBtlgCrefo, ab30XmlsDir, ab30XMLPropertiesCrefo, ab30CrefoToPropertiesMap);
        }
    }

    public void handleBtlgOrEntg(String strInfoPrefix, String strBtlgEntg, Long btlgEntgCrefo, File ab30XmlsDir, AB30XMLProperties ab30XMLPropertiesCrefo, Map<Long, AB30XMLProperties> ab30CrefoToPropertiesMap) throws Exception {
        Long testCrefo = ab30XMLPropertiesCrefo.getCrefoNr();
        if (btlgEntgCrefo.equals(testCrefo)) {
            TimelineLogger.info(this.getClass(), strInfoPrefix + strBtlgEntg + " " + btlgEntgCrefo + " und Haupt-Crefo sind identisch, braucht nicht nochmal hochgeladen zu werden.");
            return;
        }
        TreeSet<Long> btlgCrefosList = ab30XMLPropertiesCrefo.getBtlgCrefosList();
        // prüfe, ob die XML-Datei für den Beteiligten der Test-Crefo existiert...
        File btlgCrefoXmlFile = new File(ab30XmlsDir, btlgEntgCrefo + ".xml");
        if (!btlgCrefoXmlFile.exists()) {
            TimelineLogger.info(this.getClass(), strInfoPrefix + "XML-Datei " + btlgCrefoXmlFile + " für die btlgEntgCrefo-Crefo " + strBtlgEntg + " existiert nicht!");
        }
        // wenn diese BTLG-Crefo noch nicht in der Beteiligten-Liste  existiert, dann hinzufügen
        if (!btlgCrefosList.contains(btlgEntgCrefo)) {
            btlgCrefosList.add(btlgEntgCrefo);
            TimelineLogger.info(this.getClass(), strInfoPrefix + " Nehme " + strBtlgEntg + " " + btlgEntgCrefo + " in die Beteiligten-Liste auf.");
        }
        // wenn diese BTLG-Crefo noch nicht in der neuen Map existiert, dann hinzufügen (sonst wird kein "replacement" durchgeführt!)
        if (!ab30CrefoToPropertiesMap.containsKey(btlgEntgCrefo)) {
            // existiert sie denn in ab30CrefoToPropertiesMap?
            AB30XMLProperties ab30XMLPropertiesBTLG = ab30CrefoToPropertiesMap.get(btlgEntgCrefo);
            if (ab30XMLPropertiesBTLG == null) {
                ab30XMLPropertiesBTLG = new AB30XMLProperties(btlgEntgCrefo);
            }
            TimelineLogger.info(this.getClass(), strInfoPrefix + " Nehme " + strBtlgEntg + " " + btlgEntgCrefo + " in die Mapping-Zeile auf.");
            ab30CrefoToPropertiesMap.put(btlgEntgCrefo, ab30XMLPropertiesBTLG);
        } else {
            // wenn diese BTLG-Crefo noch nicht in der Beteiligten-Liste  existiert, dann hinzufügen
            if (!btlgCrefosList.contains(btlgEntgCrefo)) {
                btlgCrefosList.add(btlgEntgCrefo);
                TimelineLogger.info(this.getClass(), strInfoPrefix + " Nehme " + strBtlgEntg + " " + btlgEntgCrefo + " in die Beteiligten-Liste auf.");
            }
            TimelineLogger.info(this.getClass(), strInfoPrefix + strBtlgEntg + " " + btlgEntgCrefo + " wurde schon als Mapping-Zeile aufgenommen!");
        }
    }

    protected Map<String, List<Long>> parseCrefosFromXmlContent(File ab30CrefoXmlFile) throws XPathExpressionException {
        Map<String, List<Long>> crefoListsMap = new HashMap<>();
        crefoListsMap.put(TAG_STEUERUNGSDATEN, new ArrayList<>());
        crefoListsMap.put(TAG_FIRMENBETEILIGTER, new ArrayList<>());
        crefoListsMap.put(TAG_VERFAHRENSBETEILIGTER, new ArrayList<>());
        XPath xPath = XPathFactory.newInstance().newXPath();
        InputSource xml = new InputSource(ab30CrefoXmlFile.getAbsolutePath());
        NodeList result = (NodeList) xPath.evaluate("//*[local-name() = 'crefonummer'] | //*[local-name() = 'crefonummer-beteiligter']", xml, XPathConstants.NODESET);
        for (int i = 0; i < result.getLength(); i++) {
            Node node = result.item(i);
            String key = node.getParentNode().getNodeName().toUpperCase(Locale.ROOT);
            if (key.startsWith("ARC:")) {
                key = key.substring(4);
            }
            List<Long> crefosList = crefoListsMap.get(key);
            if (crefosList == null) {
                crefosList = new ArrayList<>();
            }
            crefosList.add(Long.valueOf(node.getTextContent()));
            crefoListsMap.put(key, crefosList);
        }
        return crefoListsMap;
    }

}
