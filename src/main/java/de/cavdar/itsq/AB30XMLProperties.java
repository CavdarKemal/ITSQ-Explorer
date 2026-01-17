package de.cavdar.itsq;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class AB30XMLProperties {
    public static final String HEADER = "# CREFO::[{Used-By-Customer;...}],[IKA-AUFTR-CLZ],[{BTLG-CREFO;...}],[BEFR|BILANZ|BEIDES],[ABLEHNUNG_FIRMA_FIRMA|ABLEHNUNG_FIRMA_PRIVPERSON|...],[CTA_STATISTIK],[DSGVO_SPERRE]";
    public static final String VERSION_STR = "# Version::";
    public static final int VERSION = 2;
    List<String> usedByCustomersList = new ArrayList<>();
    private Long crefoNr;
    private Long auftragClz;
    private TreeSet<Long> btlgCrefosList = new TreeSet<>();
    private BILANZEN_TYPE bilanzType;
    private EH_PROD_AUFTR_TYPE ehProdAuftrType;
    private boolean mitCtaStatistik;
    private boolean dsgVoSperre;
    public AB30XMLProperties(Long crefoNr) {
        this(crefoNr, BILANZEN_TYPE.KEINE, EH_PROD_AUFTR_TYPE.KEINE, false, false);
    }
    public AB30XMLProperties(Long crefoNr, BILANZEN_TYPE bilanzType, EH_PROD_AUFTR_TYPE ehProdAuftrType, boolean mitCtaStatistik, boolean dsgVoSperre) {
        this.crefoNr = crefoNr;
        this.bilanzType = bilanzType;
        this.ehProdAuftrType = ehProdAuftrType;
        this.mitCtaStatistik = mitCtaStatistik;
        this.dsgVoSperre = dsgVoSperre;
    }
    public AB30XMLProperties(AB30XMLProperties clone) {
        crefoNr = clone.getCrefoNr();
        auftragClz = clone.getAuftragClz();
        btlgCrefosList.addAll(clone.getBtlgCrefosList());
        bilanzType = clone.getBilanzType();
        mitCtaStatistik = clone.isMitCtaStatistik();
        dsgVoSperre = clone.isMitDsgVoSperre();
    }

    public AB30XMLProperties(String strLine, int version) {
        // "# CREFO::[{Used-By-Customer;...}],[IKA-AUFTR-CLZ],[{BTLG-CREFO;...}],[BEFR|BILANZ|BEIDES],[ABLEHNUNG_FIRMA_FIRMA|ABLEHNUNG_FIRMA_PRIVPERSON|...],[CTA_STATISTIK],[DSGVO_SPERRE]"
        if (strLine == null || strLine.isEmpty()) {
            throw new IllegalArgumentException("\nDie Zeile darf nicht leer oder NULL sein!\nFormat: " + HEADER);
        }
        String[] groups1 = strLine.split("::"); // --> "4110106343"  "[411],[],[]"
        if (groups1.length < 1) {
            throw new IllegalArgumentException("\nDie Zeile '" + strLine + "' hat falsches Format!\nFormat: " + HEADER);
        }
        String strCrefo = groups1[0].trim();
        crefoNr = Long.valueOf(strCrefo); // --> 4110106343

        auftragClz = null;
        bilanzType = BILANZEN_TYPE.KEINE;
        ehProdAuftrType = EH_PROD_AUFTR_TYPE.KEINE;
        mitCtaStatistik = false;
        dsgVoSperre = false;
        btlgCrefosList = new TreeSet<>();

        if (groups1.length > 1) {
            String[] groups2 = groups1[1].trim().split(","); // --> "[411]" "[]"...
            int index = 0;
            if ((version == 2) && groups2.length > index) {
                String[] strUsedByCustomers = groups2[index++].trim().split(";");
                extractAndAssignCustomers(strUsedByCustomers);
            }
            if (groups2.length > index) {
                String strAuftrClz = extractToken(groups2[index++]);
                if (!strAuftrClz.isEmpty()) {
                    try {
                        auftragClz = Long.valueOf(strAuftrClz).longValue(); // --> 411
                    } catch (NumberFormatException ex) {
                        throw new RuntimeException("CLZ '" + strAuftrClz + "' konnte nicht konvertiert werden! Crefo " + crefoNr, ex);
                    }
                }
            }
            if (groups2.length > index) {
                String[] strEntgCrefos = groups2[index++].trim().split(";");
                for (String strTemp : strEntgCrefos) {
                    String strEntgCrefo = extractToken(strTemp);
                    if (!strEntgCrefo.isEmpty()) {
                        if (strEntgCrefo.length() == 10) {
                            Long entgCrefo = Long.valueOf(strEntgCrefo);
                            btlgCrefosList.add(entgCrefo);
                        } else {
                            throw new IllegalArgumentException("\nDie Zeile '" + strLine + "' hat falsches Format für Beteiligten-Crefo!");
                        }
                    }
                }
            }
            if (groups2.length > index) {
                String strBilanzType = extractToken(groups2[index++]);
                if (!strBilanzType.isEmpty()) {
                    bilanzType = BILANZEN_TYPE.valueOf(strBilanzType);
                }
            }
            if (groups2.length > index) {
                String strEhProdAuftrType = extractToken(groups2[index++]);
                if (!strEhProdAuftrType.isEmpty()) {
                    ehProdAuftrType = EH_PROD_AUFTR_TYPE.valueOf(strEhProdAuftrType);
                }
            }
            if (groups2.length > index) {
                String strMitCtaStatistik = extractToken(groups2[index++]);
                if (!strMitCtaStatistik.isEmpty()) {
                    mitCtaStatistik = true;
                }
            }
            if ((version == 2) && groups2.length > index) {
                String strDsgVoSperre = extractToken(groups2[index++]);
                if (!strDsgVoSperre.isEmpty()) {
                    dsgVoSperre = true;
                }
            }
            if ((version == 1) && groups2.length > index) {
                String[] strUsedByCustomers = groups2[index++].trim().split(";");
                extractAndAssignCustomers(strUsedByCustomers);
            }
        }
    }

    /**
     * true, wenn für den angegebenen Typ der Upload eines Produkt-Auftrages für EH erfolgen soll
     */
    public static boolean isUploadable(EH_PROD_AUFTR_TYPE ehProdAuftrType) {
        return ehProdAuftrType != null && ehProdAuftrType.isUploadErforderlich();
    }

    private void extractAndAssignCustomers(String[] strUsedByCustomers) {
        for (String strTemp : strUsedByCustomers) {
            String strUsedByCustomer = extractToken(strTemp);
            if (!strUsedByCustomer.isEmpty()) {
                if (!usedByCustomersList.contains(strUsedByCustomer)) {
                    getUsedByCustomersList().add(strUsedByCustomer);
                }
            }
        }
    }

    private String extractToken(String strToken) {
        return strToken.trim().replace("[", "").replace("]", "");
    }

    public Long getCrefoNr() {
        return crefoNr;
    }

    public void setCrefoNr(Long crefoNr) {
        this.crefoNr = crefoNr;
    }

    public Long getAuftragClz() {
        return auftragClz;
    }

    public void setAuftragClz(Long auftragClz) {
        this.auftragClz = auftragClz;
    }

    public void setEhProdAuftrType(EH_PROD_AUFTR_TYPE ehProdAuftrType) {
        this.ehProdAuftrType = ehProdAuftrType;
    }

    public void setDsgVoSperre(boolean dsgVoSperre) {
        this.dsgVoSperre = dsgVoSperre;
    }

    public TreeSet<Long> getBtlgCrefosList() {
        return btlgCrefosList;
    }

    public void setBtlgCrefosList(TreeSet<Long> btlgCrefosList) {
        this.btlgCrefosList = btlgCrefosList;
    }

    public BILANZEN_TYPE getBilanzType() {
        return bilanzType;
    }

    public void setBilanzType(BILANZEN_TYPE bilanzType) {
        this.bilanzType = bilanzType;
    }

    public EH_PROD_AUFTR_TYPE getEhProduktAuftragType() {
        return ehProdAuftrType;
    }

    public boolean isMitCtaStatistik() {
        return mitCtaStatistik;
    }

    public void setMitCtaStatistik(boolean mitCtaStatistik) {
        this.mitCtaStatistik = mitCtaStatistik;
    }

    public boolean isMitDsgVoSperre() {
        return dsgVoSperre;
    }

    public List<String> getUsedByCustomersList() {
        return usedByCustomersList;
    }

    public void setUsedByCustomersList(List<String> usedByCustomersList) {
        this.usedByCustomersList = usedByCustomersList;
    }

    private String getStrUsedByCustomers() {
        String strUsedByCustomers = "";
        for (String testCustomerKey : getUsedByCustomersList()) {
            strUsedByCustomers += testCustomerKey;
            strUsedByCustomers += ";";
        }
        return strUsedByCustomers.length() > 0 ? (strUsedByCustomers.substring(0, strUsedByCustomers.length() - 1)) : "";
    }

    @Override
    public String toString() {
        // "# CREFO::[{Used-By-Customer;...}],[IKA-AUFTR-CLZ],[{BTLG-CREFO;...}],[BEFR|BILANZ|BEIDES],[ABLEHNUNG_FIRMA_FIRMA|ABLEHNUNG_FIRMA_PRIVPERSON|...],[CTA_STATISTIK],[DSGVO_SPERRE]"
        String strBtlgList = "";
        if (!btlgCrefosList.isEmpty()) {
            strBtlgList = btlgCrefosList.toString().replaceAll(",", ";");
            strBtlgList = strBtlgList.replace("[", "").replace("]", "").replace(" ", "");
        }
        String strAuftrClz = auftragClz != null ? auftragClz.toString() : "";
        String strUsedByCustomers = getStrUsedByCustomers();
        String strInfo = String.format("%d::[%s],[%s],[%s],[%s],[%s],[%s],[%s]",
                crefoNr, strUsedByCustomers, strAuftrClz, strBtlgList, bilanzType, ehProdAuftrType,
                mitCtaStatistik ? "CTA_STATISTIK" : "",
                dsgVoSperre ? "DSGVO_SPERRE" : "");
        return strInfo;
    }

    public enum BILANZEN_TYPE {
        KEINE(""), BEFR("befreiung.xml"), BILANZ("bilanz.xml"), BEIDES("bilanz_befreiung.xml");

        private final String xmlFileName;

        BILANZEN_TYPE(String xmlFileName) {
            this.xmlFileName = xmlFileName;
        }

        public String getXmlFileName() {
            return xmlFileName;
        }
    }

    public enum EH_PROD_AUFTR_TYPE {
        KEINE(false, false, ""),
        INITIAL_BUYER(true, false, ""),
        ABLEHNUNG_FIRMA_FIRMA("EH-Produktauftrag-ABLEHNUNG_FIRMA_FIRMA.xml"),
        ABLEHNUNG_FIRMA_PRIVPERSON("EH-Produktauftrag-ABLEHNUNG_FIRMA_PRIVPERSON.xml"),
        ABLEHNUNG_PRIVPERSON_FIRMA("EH-Produktauftrag-ABLEHNUNG_PRIVPERSON_FIRMA.xml"),
        ABLEHNUNG_PRIVPERSON_PRIVPERSON("EH-Produktauftrag-ABLEHNUNG_PRIVPERSON_PRIVPERSON.xml"),
        ERLEDIGUNG_FIRMA_FIRMA("EH-Produktauftrag-ERLEDIGUNG_FIRMA_FIRMA.xml"),
        ERLEDIGUNG_FIRMA_PRIVPERSON("EH-Produktauftrag-ERLEDIGUNG_FIRMA_PRIVPERSON.xml"),
        ERLEDIGUNG_PRIVPERSON_FIRMA("EH-Produktauftrag-ERLEDIGUNG_PRIVPERSON_FIRMA.xml"),
        ERLEDIGUNG_PRIVPERSON_PRIVPERSON("EH-Produktauftrag-ERLEDIGUNG_PRIVPERSON_PRIVPERSON.xml");

        private final String xmlFileName;
        private final boolean auftragInitialBuyer;
        private final boolean auftragRecherche;
        private final boolean uploadErforderlich;

        /**
         * Convenience-Konstruktor für die Fälle, bei denen ein Recherche-Auftrag aus der angegebenen Datei
         * hochgeladen werden soll
         *
         * @param xmlFileName Name der Datei für den Upload, not null
         */
        EH_PROD_AUFTR_TYPE(String xmlFileName) {
            this(false, true, xmlFileName);
        }

        EH_PROD_AUFTR_TYPE(boolean auftragInitialBuyer, boolean auftragRecherche, String xmlFileName) {
            this.auftragInitialBuyer = auftragInitialBuyer;
            this.auftragRecherche = auftragRecherche;
            this.uploadErforderlich = auftragRecherche || auftragInitialBuyer;
            this.xmlFileName = xmlFileName;
        }

        public String getXmlFileName() {
            return xmlFileName;
        }

        /**
         * true, wenn ein Auftrag (egal welcher Art) hochgeladen werde soll
         */
        public boolean isUploadErforderlich() {
            return uploadErforderlich;
        }

        /**
         * true, wenn eine Meldung als initial Buyer erfolgen soll
         */
        public boolean isAuftragInitialBuyer() {
            return auftragInitialBuyer;
        }

        /**
         * true, wenn ein Recherche-Auftrag übermittelt werden soll
         */
        public boolean isAuftragRecherche() {
            return auftragRecherche;
        }

    }

}
