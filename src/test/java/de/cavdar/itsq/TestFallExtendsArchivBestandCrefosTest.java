package de.cavdar.itsq;

import java.util.Map;
import org.junit.Test;

public class TestFallExtendsArchivBestandCrefosTest {
    @Test
    public void testIt() throws Exception {
        ITSQTestFaelleUtil testFaelleUtil = new ITSQTestFaelleUtil();
        Map<TestSupportClientKonstanten.TEST_PHASE, Map<String, TestCustomer>> customerTestInfoMapMap = testFaelleUtil.getCustomerTestInfoMapMap();
        TestFallExtendsArchivBestandCrefos cut = new TestFallExtendsArchivBestandCrefos(customerTestInfoMapMap);
        cut.extendTestCrefos();
    }
}
