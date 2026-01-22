package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CustomerModelStockValidationTest {

    /**
     * A tiny "fake DB" built using a dynamic proxy.
     * We only implement the behaviour we need for Feature 2 tests.
     * Any other DatabaseRW methods return safe defaults.
     */
    static class FakeDbHandler implements InvocationHandler {
        private final Map<String, Product> stockMap = new HashMap<>();
        private boolean purchaseStocksCalled = false;

        void putStock(String id, String description, int stockQty) {
            Product p = new Product(id, description, id + ".jpg", 1.00, stockQty);
            stockMap.put(id, p);
        }

        boolean wasPurchaseStocksCalled() {
            return purchaseStocksCalled;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            // --- Methods our code actually uses for Feature 2 ---
            if (name.equals("searchByProductId")) {
                String productId = (String) args[0];
                Product inDb = stockMap.get(productId);

                if (inDb == null) {
                    return null;
                }

                // return a copy so tests don't share object state
                Product copy = new Product(
                        inDb.getProductId(),
                        inDb.getProductDescription(),
                        inDb.getProductImageName(),
                        inDb.getUnitPrice(),
                        inDb.getStockQuantity()
                );
                copy.setOrderedQuantity(inDb.getOrderedQuantity());
                return copy;
            }

            if (name.equals("purchaseStocks")) {
                purchaseStocksCalled = true;

                @SuppressWarnings("unchecked")
                ArrayList<Product> groupedTrolley = (ArrayList<Product>) args[0];

                ArrayList<Product> insufficient = new ArrayList<>();

                // basic "commit" simulation
                for (Product requested : groupedTrolley) {
                    Product inDb = stockMap.get(requested.getProductId());

                    if (inDb == null || inDb.getStockQuantity() < requested.getOrderedQuantity()) {
                        Product fail;
                        if (inDb == null) {
                            fail = new Product(requested.getProductId(), "Unknown product", "imageHolder.jpg", 1.00, 0);
                        } else {
                            fail = new Product(
                                    inDb.getProductId(),
                                    inDb.getProductDescription(),
                                    inDb.getProductImageName(),
                                    inDb.getUnitPrice(),
                                    inDb.getStockQuantity()
                            );
                        }
                        fail.setOrderedQuantity(requested.getOrderedQuantity());
                        insufficient.add(fail);
                    } else {
                        int newStock = inDb.getStockQuantity() - requested.getOrderedQuantity();

                        // Product has no setter for stockQuantity, so replace the stored object
                        Product updated = new Product(
                                inDb.getProductId(),
                                inDb.getProductDescription(),
                                inDb.getProductImageName(),
                                inDb.getUnitPrice(),
                                newStock
                        );
                        stockMap.put(inDb.getProductId(), updated);
                    }
                }

                return insufficient;
            }

            // --- Useful defaults for interface methods you won't call in these tests ---
            // If your interface has isProIdAvailable(String), make it behave sensibly:
            if (name.equals("isProIdAvailable")) {
                String id = (String) args[0];
                return !stockMap.containsKey(id);
            }

            // If your interface has searchProduct(String), return empty list:
            if (name.equals("searchProduct")) {
                return new ArrayList<Product>();
            }

            // updateProduct / insertNewProduct / deleteProduct are not needed here:
            if (name.equals("updateProduct") || name.equals("insertNewProduct") || name.equals("deleteProduct")) {
                return defaultReturn(method.getReturnType());
            }

            // --- Fallback: return safe defaults for anything else ---
            return defaultReturn(method.getReturnType());
        }

        private Object defaultReturn(Class<?> returnType) {
            if (returnType.equals(void.class)) {
                return null;
            }
            if (returnType.equals(boolean.class)) {
                return false;
            }
            if (returnType.equals(int.class)) {
                return 0;
            }
            if (returnType.equals(double.class)) {
                return 0.0;
            }
            if (returnType.equals(float.class)) {
                return 0.0f;
            }
            if (returnType.equals(long.class)) {
                return 0L;
            }
            if (returnType.equals(short.class)) {
                return (short) 0;
            }
            if (returnType.equals(byte.class)) {
                return (byte) 0;
            }
            if (returnType.equals(char.class)) {
                return '\0';
            }
            return null;
        }
    }

    private DatabaseRW buildFakeDatabase(FakeDbHandler handler) {
        return (DatabaseRW) Proxy.newProxyInstance(
                DatabaseRW.class.getClassLoader(),
                new Class[]{DatabaseRW.class},
                handler
        );
    }

    @Test
    void checkoutRemovesOnlyInsufficientItems_andKeepsOthers() throws IOException, SQLException {
        CustomerModel model = new CustomerModel();

        FakeDbHandler handler = new FakeDbHandler();
        handler.putStock("0001", "Apples", 1);  // only 1 in stock
        handler.putStock("0002", "Radio", 10);  // plenty

        model.databaseRW = buildFakeDatabase(handler);

        Product p1 = new Product("0001", "Apples", "0001.jpg", 1.00, 1);
        p1.setOrderedQuantity(2); // request 2 but only 1 exists

        Product p2 = new Product("0002", "Radio", "0002.jpg", 10.00, 10);
        p2.setOrderedQuantity(1);

        model.addProductToTrolleyForTest(p1);
        model.addProductToTrolleyForTest(p2);

        model.checkOut();

        // Only the insufficient item should be removed
        assertEquals(1, model.getTrolley().size());
        assertEquals("0002", model.getTrolley().get(0).getProductId());
    }

    @Test
    void checkoutDoesNotCommitPurchase_whenValidationFails() throws IOException, SQLException {
        CustomerModel model = new CustomerModel();

        FakeDbHandler handler = new FakeDbHandler();
        handler.putStock("0007", "USB drive", 0); // no stock

        model.databaseRW = buildFakeDatabase(handler);

        Product p = new Product("0007", "USB drive", "0007.jpg", 6.99, 0);
        p.setOrderedQuantity(1);

        model.addProductToTrolleyForTest(p);

        model.checkOut();

        // Validation should fail before purchaseStocks is called
        assertFalse(handler.wasPurchaseStocksCalled());
        assertTrue(model.getTrolley().isEmpty());
    }
}
