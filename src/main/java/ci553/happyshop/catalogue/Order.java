package ci553.happyshop.catalogue;

import ci553.happyshop.orderManagement.OrderState;
import ci553.happyshop.utility.ProductListFormatter;

import java.util.ArrayList;
import java.util.Collections;

/**
 * The Order class represents a customer order, including metadata and a list of ordered products.
 *
 * Responsibilities:
 * - stores information about an order, including order ID, current order state, timestamps, and the list of products.
 * - provides getter methods for order attributes and allows updating the order state.
 * - formats the full order details for writing to a file, including timestamps and item list.
 *
 * This class is mainly used by OrderHub to create and manage order objects during
 * the order lifecycle (ordered → progressing → collected).
 */

public class Order {
    private int orderId;
    private OrderState state;
    private String orderedDateTime = "";
    private String progressingDateTime = "";
    private String collectedDateTime = "";

    // Trolley (kept organised inside Order)
    private ArrayList<Product> productList = new ArrayList<>();

    // Constructor used by OrderHub to create a new order for a customer.
    public Order(int orderId, OrderState state, String orderedDateTime, ArrayList<Product> productList) {
        this.orderId = orderId;
        this.state = state;
        this.orderedDateTime = orderedDateTime;

        // Build an organised trolley (merge duplicates + sort)
        this.productList = buildOrganisedProductList(productList);
    }

    // Getter methods
    public int getOrderId() { return orderId; }
    public OrderState getState() { return state; }
    public String getOrderedDateTime() { return orderedDateTime; }

    // Return a copy to protect internal list
    public ArrayList<Product> getProductList() {
        return new ArrayList<>(productList);
    }

    public void setState(OrderState state) { this.state = state; }

    /**
     * Order details written to file, used by OrderHub
     *  - Order metadata (ID, state, and three timestamps)
     *  - Product details included in the order
     */
    public String orderDetails() {
        return String.format("Order ID: %s \n" +
                        "State: %s \n" +
                        "OrderedDateTime: %s \n" +
                        "ProgressingDateTime: %s \n" +
                        "CollectedDateTime: %s\n" +
                        "Items:\n%s",
                orderId,
                state,
                orderedDateTime,
                progressingDateTime,
                collectedDateTime,
                ProductListFormatter.buildString(productList)
        );
    }

    // Organised trolley logic
    // Merge duplicates and sort for consistent output
    private ArrayList<Product> buildOrganisedProductList(ArrayList<Product> inputList) {
        ArrayList<Product> organised = new ArrayList<>();

        if (inputList == null) {
            return organised;
        }

        // Merge duplicates (by product id)
        for (Product incoming : inputList) {
            if (incoming == null) {
                continue;
            }

            String incomingId = incoming.getProductId();
            int existingIndex = findProductIndexById(organised, incomingId);

            if (existingIndex == -1) {
                organised.add(incoming);
            } else {
                Product existing = organised.get(existingIndex);

                int newQuantity = existing.getOrderedQuantity() + incoming.getOrderedQuantity();
                existing.setOrderedQuantity(newQuantity);
            }
        }

        // Sort for predictable order (uses Product.compareTo by product id)
        Collections.sort(organised);

        return organised;
    }

    // Find product in list by id
    private int findProductIndexById(ArrayList<Product> list, String productId) {
        if (productId == null) {
            return -1;
        }

        for (int i = 0; i < list.size(); i++) {
            Product p = list.get(i);
            if (p != null && productId.equals(p.getProductId())) {
                return i;
            }
        }
        return -1;
    }
}


