package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feature 3 tests:
 * - Invalid quantities raise the domain exception and block checkout.
 * - Insufficient stock raises the domain exception, and the trolley is updated.
 * - "Stock changed during checkout" path is handled safely.
 *
 * These tests avoid OrderHub/file creation by making checkout fail before an order is created.
 */
public class CustomerModelDomainExceptionsTest {

    @Test
    void invalidQuantity_blocksCheckout_andKeepsItemInTrolley() throws IOException, SQLException {
        CustomerModel model = new CustomerModel();
        FakeDatabaseRW fakeDb = new FakeDatabaseRW();
        model.databaseRW = fakeDb;   // cusView stays null, updateView() safely returns

        Product p = makeProduct("0001", "Apples", 1.00, 50);
        p.setOrderedQuantity(0); // invalid (zero)

        model.addProductToTrolleyForTest(p);

        // Should be handled inside checkOut() (caught + message set), not thrown to the test
        model.checkOut();

        // Invalid quantity does NOT remove the item; it just blocks checkout
        assertEquals(1, model.getTrolley().size());
        assertEquals(0, model.getTrolley().get(0).getOrderedQuantity());
    }

    @Test
    void insufficientStock_removesItemFromTrolley() throws IOException, SQLException {
        CustomerModel model = new CustomerModel();
        FakeDatabaseRW fakeDb = new FakeDatabaseRW();
        model.databaseRW = fakeDb;

        // DB stock: 2
        fakeDb.putStockProduct("0003", "Toaster", 29.99, 2);

        // Customer requests: 5
        Product requested = makeProduct("0003", "Toaster", 29.99, 999);
        requested.setOrderedQuantity(5);

        model.addProductToTrolleyForTest(requested);

        model.checkOut();

        // Insufficient stock path removes the item from trolley
        assertTrue(model.getTrolley().isEmpty());
    }

    @Test
    void stockChangedDuringCheckout_removesItemFromTrolley() throws IOException, SQLException {
        CustomerModel model = new CustomerModel();
        FakeDatabaseRW fakeDb = new FakeDatabaseRW();
        model.databaseRW = fakeDb;

        // Validation passes (stock is high enough)
        fakeDb.putStockProduct("0007", "USB drive", 6.99, 10);

        // But we simulate "stock changed" during the commit step
        fakeDb.simulateStockChangeOnPurchase = true;

        Product requested = makeProduct("0007", "USB drive", 6.99, 999);
        requested.setOrderedQuantity(1);

        model.addProductToTrolleyForTest(requested);

        model.checkOut();

        // Purchase step fails -> handled as InsufficientStockException -> removed from trolley
        assertTrue(model.getTrolley().isEmpty());
    }

    // Helpers
    private Product makeProduct(String id, String desc, double price, int stockQty) {
        // Image can be anything for unit tests
        return new Product(id, desc, "imageHolder.jpg", price, stockQty);
    }

    /**
     * Fake DB used just for these tests.
     * Implements every DatabaseRW method so IntelliJ stops complaining.
     */
    private static class FakeDatabaseRW implements DatabaseRW {

        private final Map<String, Product> stock = new HashMap<>();
        boolean simulateStockChangeOnPurchase = false;

        void putStockProduct(String id, String desc, double price, int stockQty) {
            stock.put(id, new Product(id, desc, "imageHolder.jpg", price, stockQty));
        }

        @Override
        public Product searchByProductId(String productId) throws SQLException {
            Product p = stock.get(productId);
            if (p == null) {
                return null;
            }
            // Return a fresh copy so tests don’t mutate our “DB” accidentally
            Product copy = new Product(
                    p.getProductId(),
                    p.getProductDescription(),
                    p.getProductImageName(),
                    p.getUnitPrice(),
                    p.getStockQuantity()
            );
            copy.setOrderedQuantity(p.getOrderedQuantity());
            return copy;
        }

        @Override
        public ArrayList<Product> purchaseStocks(ArrayList<Product> groupedTrolley) throws SQLException {
            ArrayList<Product> insufficient = new ArrayList<>();

            for (Product requested : groupedTrolley) {
                Product db = stock.get(requested.getProductId());

                if (db == null) {
                    Product missing = new Product(
                            requested.getProductId(),
                            "Unknown product",
                            "imageHolder.jpg",
                            requested.getUnitPrice(),
                            0
                    );
                    missing.setOrderedQuantity(requested.getOrderedQuantity());
                    insufficient.add(missing);
                    continue;
                }

                int available = db.getStockQuantity();
                int reqQty = requested.getOrderedQuantity();

                // simulate the “race condition” / stock change after validation
                if (simulateStockChangeOnPurchase) {
                    Product failed = new Product(
                            db.getProductId(),
                            db.getProductDescription(),
                            db.getProductImageName(),
                            db.getUnitPrice(),
                            available
                    );
                    failed.setOrderedQuantity(reqQty);
                    insufficient.add(failed);
                    continue;
                }

                if (available < reqQty) {
                    Product failed = new Product(
                            db.getProductId(),
                            db.getProductDescription(),
                            db.getProductImageName(),
                            db.getUnitPrice(),
                            available
                    );
                    failed.setOrderedQuantity(reqQty);
                    insufficient.add(failed);
                } else {
                    // update stock (commit)
                    stock.put(db.getProductId(),
                            new Product(db.getProductId(), db.getProductDescription(), db.getProductImageName(),
                                    db.getUnitPrice(), available - reqQty));
                }
            }

            return insufficient;
        }


        // Unused methods in these tests (still must exist)
        @Override
        public ArrayList<Product> searchProduct(String keyword) throws SQLException {
            return new ArrayList<>();
        }

        @Override
        public void updateProduct(String proId, String description, double unitPrice, String image, int inStockQty)
                throws SQLException {
            // Not needed for these tests
        }

        @Override
        public void deleteProduct(String proId) throws SQLException {
            // Not needed for these tests
        }

        @Override
        public void insertNewProduct(String proId, String description, double unitPrice, String image, int inStockQty)
                throws SQLException {
            // Not needed for these tests
        }

        @Override
        public boolean isProIdAvailable(String proId) throws SQLException {
            return true;
        }
    }
}
