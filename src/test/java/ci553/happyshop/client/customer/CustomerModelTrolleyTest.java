package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomerModelTrolleyTest {

    @Test
    void addingSameProductTwice_mergesIntoOneEntryWithQuantityTwo() {
        CustomerModel model = new CustomerModel();

        Product p = new Product("0001", "Apples", "0001.jpg", 1.00, 50);

        model.addProductToTrolleyForTest(p);
        model.addProductToTrolleyForTest(p);

        assertEquals(1, model.getTrolley().size());
        assertEquals("0001", model.getTrolley().get(0).getProductId());
        assertEquals(2, model.getTrolley().get(0).getOrderedQuantity());
    }

    @Test
    void addingDifferentProducts_sortsByProductId() {
        CustomerModel model = new CustomerModel();

        Product p2 = new Product("0002", "Radio", "0002.jpg", 10.00, 50);
        Product p1 = new Product("0001", "Apples", "0001.jpg", 1.00, 50);

        model.addProductToTrolleyForTest(p2);
        model.addProductToTrolleyForTest(p1);

        assertEquals(2, model.getTrolley().size());
        assertEquals("0001", model.getTrolley().get(0).getProductId());
        assertEquals("0002", model.getTrolley().get(1).getProductId());
    }

    @Test
    void addingSameProductThreeTimes_increasesQuantityToThree() {
        CustomerModel model = new CustomerModel();

        Product p = new Product("0007", "USB drive", "0007.jpg", 6.99, 50);

        model.addProductToTrolleyForTest(p);
        model.addProductToTrolleyForTest(p);
        model.addProductToTrolleyForTest(p);

        assertEquals(1, model.getTrolley().size());
        assertEquals(3, model.getTrolley().get(0).getOrderedQuantity());
    }
}
