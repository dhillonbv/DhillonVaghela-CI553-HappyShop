package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.utility.ProductListFormatter;
import ci553.happyshop.utility.StorageLocation;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * CustomerModel holds the customer-side logic for search, trolley and checkout.
 */
public class CustomerModel {
    public CustomerView cusView;
    public DatabaseRW databaseRW;

    private Product theProduct = null; // product found from search
    private ArrayList<Product> trolley = new ArrayList<>(); // a list of products in trolley

    // UI values for CustomerView
    private String imageName = "imageHolder.jpg";
    private String displayLaSearchResult = "No Product was searched yet";
    private String displayTaTrolley = "";
    private String displayTaReceipt = "";

    // SELECT productID, description, image, unitPrice, inStock quantity
    void search() throws SQLException {
        String productId = cusView.tfId.getText().trim();

        if (!productId.isEmpty()) {
            theProduct = databaseRW.searchByProductId(productId);

            if (theProduct != null && theProduct.getStockQuantity() > 0) {
                double unitPrice = theProduct.getUnitPrice();
                String description = theProduct.getProductDescription();
                int stock = theProduct.getStockQuantity();

                String baseInfo = String.format("Product_Id: %s\n%s,\nPrice: £%.2f", productId, description, unitPrice);
                String quantityInfo = stock < 100 ? String.format("\n%d units left.", stock) : "";
                displayLaSearchResult = baseInfo + quantityInfo;
                System.out.println(displayLaSearchResult);
            } else {
                theProduct = null;
                displayLaSearchResult = "No Product was found with ID " + productId;
                System.out.println("No Product was found with ID " + productId);
            }
        } else {
            theProduct = null;
            displayLaSearchResult = "Please type ProductID";
            System.out.println("Please type ProductID.");
        }

        updateView();
    }

    void addToTrolley() {
        if (theProduct != null) {

            // Merge duplicates by product ID
            addOrMergeProduct(theProduct);

            // Sort for consistent output
            Collections.sort(trolley);

            displayTaTrolley = ProductListFormatter.buildString(trolley);
        } else {
            displayLaSearchResult = "Please search for an available product before adding it to the trolley";
            System.out.println("must search and get an available product before add to trolley");
        }

        // Clear receipt to switch back to trolleyPage
        displayTaReceipt = "";
        updateView();
    }

    void checkOut() throws IOException, SQLException {
        if (trolley.isEmpty()) {
            displayTaTrolley = "Your trolley is empty";
            System.out.println("Your trolley is empty");
            updateView();
            return;
        }

        // Group trolley so each product ID is checked once with the correct quantity
        ArrayList<Product> groupedTrolley = groupProductsById(trolley);

        // Step 1: Validate stock (no DB update here)
        ArrayList<Product> insufficientProducts = validateStockAvailability(groupedTrolley);

        if (!insufficientProducts.isEmpty()) {
            // Build an error message
            StringBuilder errorMsg = new StringBuilder();
            for (Product p : insufficientProducts) {
                errorMsg.append("\u2022 ").append(p.getProductId()).append(", ")
                        .append(p.getProductDescription()).append(" (Only ")
                        .append(p.getStockQuantity()).append(" available, ")
                        .append(p.getOrderedQuantity()).append(" requested)\n");
            }

            theProduct = null;

            // Remove items that cannot be bought
            removeInsufficientProductsFromTrolley(insufficientProducts);

            // Update trolley display after removal
            Collections.sort(trolley);
            displayTaTrolley = ProductListFormatter.buildString(trolley);

            // Keep message in label for now
            displayLaSearchResult = "Checkout failed due to insufficient stock for the following products:\n" + errorMsg;
            System.out.println("Checkout blocked: insufficient stock");

            updateView();
            return;
        }

        // Step 2: Commit purchase (update DB stock)
        // This should normally succeed because we just validated, but we keep it defensive.
        ArrayList<Product> purchaseFailed = databaseRW.purchaseStocks(groupedTrolley);

        if (!purchaseFailed.isEmpty()) {
            // If stock changed since validation, handle it safely
            StringBuilder errorMsg = new StringBuilder();
            for (Product p : purchaseFailed) {
                errorMsg.append("\u2022 ").append(p.getProductId()).append(", ")
                        .append(p.getProductDescription()).append(" (Only ")
                        .append(p.getStockQuantity()).append(" available, ")
                        .append(p.getOrderedQuantity()).append(" requested)\n");
            }

            theProduct = null;

            removeInsufficientProductsFromTrolley(purchaseFailed);

            Collections.sort(trolley);
            displayTaTrolley = ProductListFormatter.buildString(trolley);

            displayLaSearchResult = "Checkout failed because stock changed during checkout:\n" + errorMsg;
            System.out.println("Checkout failed: stock changed after validation");

            updateView();
            return;
        }

        // Create a new order only after stock has been updated successfully
        OrderHub orderHub = OrderHub.getOrderHub();
        Order theOrder = orderHub.newOrder(trolley);

        trolley.clear();
        displayTaTrolley = "";

        displayTaReceipt = String.format(
                "Order_ID: %s\nOrdered_Date_Time: %s\n%s",
                theOrder.getOrderId(),
                theOrder.getOrderedDateTime(),
                ProductListFormatter.buildString(theOrder.getProductList())
        );

        System.out.println(displayTaReceipt);
        updateView();
    }

    /**
     * Groups products by productId to optimise stock checking.
     */
    private ArrayList<Product> groupProductsById(ArrayList<Product> proList) {
        Map<String, Product> grouped = new HashMap<>();

        for (Product p : proList) {
            String id = p.getProductId();

            if (grouped.containsKey(id)) {
                Product existing = grouped.get(id);
                existing.setOrderedQuantity(existing.getOrderedQuantity() + p.getOrderedQuantity());
            } else {
                // Make a shallow copy to avoid modifying the original
                Product copy = new Product(
                        p.getProductId(),
                        p.getProductDescription(),
                        p.getProductImageName(),
                        p.getUnitPrice(),
                        p.getStockQuantity()
                );

                // Keep the requested quantity
                copy.setOrderedQuantity(p.getOrderedQuantity());

                grouped.put(id, copy);
            }
        }

        return new ArrayList<>(grouped.values());
    }

    // -------------------------
    // Feature 2: Stock validation
    // -------------------------

    // Check stock in the database without updating anything
    private ArrayList<Product> validateStockAvailability(ArrayList<Product> groupedTrolley) throws SQLException {
        ArrayList<Product> insufficient = new ArrayList<>();

        for (Product requested : groupedTrolley) {
            if (requested == null) {
                continue;
            }

            String id = requested.getProductId();
            int requestedQty = requested.getOrderedQuantity();

            Product dbProduct = databaseRW.searchByProductId(id);

            if (dbProduct == null) {
                // Treat missing product as unavailable
                Product missing = new Product(
                        id,
                        "Unknown product",
                        requested.getProductImageName(),
                        requested.getUnitPrice(),
                        0
                );
                missing.setOrderedQuantity(requestedQty);
                insufficient.add(missing);
            } else {
                int stock = dbProduct.getStockQuantity();
                if (stock < requestedQty) {
                    // Use DB info for description/stock, but keep the requested quantity
                    dbProduct.setOrderedQuantity(requestedQty);
                    insufficient.add(dbProduct);
                }
            }
        }

        return insufficient;
    }

    // -------------------------
    // Organised trolley helpers
    // -------------------------

    // Add product or merge quantity if it already exists
    private void addOrMergeProduct(Product productToAdd) {
        String id = productToAdd.getProductId();
        int index = findProductIndexById(id);

        if (index == -1) {
            // Add a copy so trolley items are independent
            Product copy = new Product(
                    productToAdd.getProductId(),
                    productToAdd.getProductDescription(),
                    productToAdd.getProductImageName(),
                    productToAdd.getUnitPrice(),
                    productToAdd.getStockQuantity()
            );
            copy.setOrderedQuantity(productToAdd.getOrderedQuantity());
            trolley.add(copy);
        } else {
            // Merge quantity into existing entry
            Product existing = trolley.get(index);
            int newQuantity = existing.getOrderedQuantity() + productToAdd.getOrderedQuantity();
            existing.setOrderedQuantity(newQuantity);
        }
    }

    // Find product in trolley by product id
    private int findProductIndexById(String productId) {
        if (productId == null) {
            return -1;
        }

        for (int i = 0; i < trolley.size(); i++) {
            Product p = trolley.get(i);
            if (p != null && productId.equals(p.getProductId())) {
                return i;
            }
        }
        return -1;
    }

    // Remove products from trolley that do not have enough stock
    private void removeInsufficientProductsFromTrolley(ArrayList<Product> insufficientProducts) {
        if (insufficientProducts == null || insufficientProducts.isEmpty()) {
            return;
        }

        for (Product insufficient : insufficientProducts) {
            if (insufficient == null) {
                continue;
            }

            String badId = insufficient.getProductId();

            // Remove any matching items (loop style, beginner friendly)
            for (int i = trolley.size() - 1; i >= 0; i--) {
                Product inTrolley = trolley.get(i);
                if (inTrolley != null && badId.equals(inTrolley.getProductId())) {
                    trolley.remove(i);
                }
            }
        }
    }

    void cancel() {
        trolley.clear();
        displayTaTrolley = "";
        updateView();
    }

    void closeReceipt() {
        displayTaReceipt = "";
    }

    void updateView() {
        if (cusView == null) {
            return;
        }if (theProduct != null) {
            imageName = theProduct.getProductImageName();
            String relativeImageUrl = StorageLocation.imageFolder + imageName;

            Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath();
            imageName = imageFullPath.toUri().toString();
            System.out.println("Image absolute path: " + imageFullPath);
        } else {
            imageName = "imageHolder.jpg";
        }

        cusView.update(imageName, displayLaSearchResult, displayTaTrolley, displayTaReceipt);
    }

    // For test only
    public ArrayList<Product> getTrolley() {
        return trolley;
    }

    // For unit tests only: adds a product without using the UI flow
    void addProductToTrolleyForTest(Product product) {
        if (product == null) {
            return;
        }
        addOrMergeProduct(product);
        Collections.sort(trolley);
    }
}
