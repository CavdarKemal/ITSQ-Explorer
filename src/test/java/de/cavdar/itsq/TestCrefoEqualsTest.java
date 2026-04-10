package de.cavdar.itsq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TestCrefoEqualsTest {

    @Test
    @DisplayName("TestCrefo equals/hashCode – gleiche Identität (testFallName + crefoNr)")
    void testCrefo_equalsByIdentity() {
        TestCrefo a = new TestCrefo("p01_test", 12345L, "info", true, null);
        TestCrefo b = new TestCrefo("p01_test", 12345L, "andere info", false, null);
        TestCrefo c = new TestCrefo("p02_test", 12345L, "info", true, null);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    @DisplayName("TestCrefo – funktioniert in HashSet")
    void testCrefo_worksInHashSet() {
        Set<TestCrefo> set = new HashSet<>();
        set.add(new TestCrefo("p01", 1L, "i", true, null));
        set.add(new TestCrefo("p01", 1L, "andere", false, null)); // Duplikat
        set.add(new TestCrefo("p02", 1L, "i", true, null));

        assertThat(set).hasSize(2);
    }

    @Test
    @DisplayName("TestCrefo – List.contains() funktioniert wertbasiert")
    void testCrefo_listContainsByValue() {
        List<TestCrefo> list = List.of(
                new TestCrefo("p01", 1L, "x", true, null),
                new TestCrefo("p02", 2L, "y", false, null)
        );
        TestCrefo searched = new TestCrefo("p01", 1L, "different", false, null);

        assertThat(list.contains(searched)).isTrue();
    }

    @Test
    @DisplayName("TestCustomer equals/hashCode – Identität via customerKey")
    void testCustomer_equalsByKey() {
        TestCustomer a = new TestCustomer("C01", "Customer A");
        TestCustomer b = new TestCustomer("C01", "Customer B"); // gleicher Key, anderer Name
        TestCustomer c = new TestCustomer("C02", "Customer A");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
    }
}
