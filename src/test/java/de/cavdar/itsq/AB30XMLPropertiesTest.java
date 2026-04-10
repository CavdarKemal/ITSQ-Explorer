package de.cavdar.itsq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AB30XMLPropertiesTest {

    @Test
    @DisplayName("Copy-Constructor – kopiert ALLE Felder (auch ehProdAuftrType + usedByCustomersList)")
    void copyConstructor_allFieldsCopied() {
        AB30XMLProperties original = new AB30XMLProperties(
                123456L,
                AB30XMLProperties.BILANZEN_TYPE.BEFR,
                AB30XMLProperties.EH_PROD_AUFTR_TYPE.ABLEHNUNG_FIRMA_FIRMA,
                true,
                false
        );
        original.setAuftragClz(789L);
        original.getBtlgCrefosList().add(111L);
        original.getBtlgCrefosList().add(222L);
        original.usedByCustomersList.add("CustomerA");
        original.usedByCustomersList.add("CustomerB");

        AB30XMLProperties copy = new AB30XMLProperties(original);

        assertThat(copy.getCrefoNr()).isEqualTo(123456L);
        assertThat(copy.getAuftragClz()).isEqualTo(789L);
        assertThat(copy.getBtlgCrefosList()).containsExactly(111L, 222L);
        assertThat(copy.getBilanzType()).isEqualTo(AB30XMLProperties.BILANZEN_TYPE.BEFR);
        assertThat(copy.isMitCtaStatistik()).isTrue();
        assertThat(copy.isMitDsgVoSperre()).isFalse();

        // Bug 1: ehProdAuftrType wurde nicht kopiert (war immer KEINE)
        assertThat(copy.getEhProduktAuftragType())
                .as("ehProdAuftrType muss vom Original kopiert werden")
                .isEqualTo(AB30XMLProperties.EH_PROD_AUFTR_TYPE.ABLEHNUNG_FIRMA_FIRMA);

        // Bug 2: usedByCustomersList wurde nicht kopiert (war immer leer)
        assertThat(copy.getUsedByCustomersList())
                .as("usedByCustomersList muss vom Original kopiert werden")
                .containsExactly("CustomerA", "CustomerB");
    }

    @Test
    @DisplayName("toString – usedByCustomers werden mit Semikolon getrennt (kein Trailing-Separator)")
    void toString_usedByCustomersJoinedCorrectly() {
        AB30XMLProperties props = new AB30XMLProperties(123L);
        props.usedByCustomersList.add("C1");
        props.usedByCustomersList.add("C2");
        props.usedByCustomersList.add("C3");

        String s = props.toString();
        assertThat(s).contains("[C1;C2;C3]");
        // Kein Trailing-Separator
        assertThat(s).doesNotContain("C3;]");
    }

    @Test
    @DisplayName("toString – leere Customer-Liste crasht nicht")
    void toString_emptyCustomerList() {
        AB30XMLProperties props = new AB30XMLProperties(123L);
        String s = props.toString();
        assertThat(s).isNotNull();
    }

    @Test
    @DisplayName("Copy-Constructor – Defensive Copy: Änderungen an der Kopie beeinflussen Original nicht")
    void copyConstructor_defensiveCopy() {
        AB30XMLProperties original = new AB30XMLProperties(
                111L,
                AB30XMLProperties.BILANZEN_TYPE.BILANZ,
                AB30XMLProperties.EH_PROD_AUFTR_TYPE.KEINE,
                false,
                false
        );
        original.usedByCustomersList.add("X");
        original.getBtlgCrefosList().add(999L);

        AB30XMLProperties copy = new AB30XMLProperties(original);

        // Modify copy
        copy.usedByCustomersList.add("Y");
        copy.getBtlgCrefosList().add(888L);

        // Original muss unverändert sein
        assertThat(original.getUsedByCustomersList()).containsExactly("X");
        assertThat(original.getBtlgCrefosList()).containsExactly(999L);
    }
}
