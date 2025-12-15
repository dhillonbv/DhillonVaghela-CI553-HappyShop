package ci553.happyshop.catalogue;

import ci553.happyshop.orderManagement.OrderState;
import ci553.happyshop.utility.ProductListFormatter;

import java.util.ArrayList;

/**
 * The Order class represents a customer order, including metadata and a list of ordered products.
 *
 * Responsibilities:
 * - sotres information about an order, including order ID, current order state, timestamps, and the list of products.
 * - Provides getter methods for order attributes and allows updating the order state.
 * - Formats the full order details for writing to a file, including timestamps and item list.
 *
 * An order file example:
 * Order ID: 10
 * State: Ordered
 * OrderedDateTime: 2025-05-03 16:52:24
 * ProgressingDateTime:
 * CollectedDateTime:
 * Items:
 *  0002    DAB Radio          ( 1) £  29.99
 *  0004    Watch              ( 1) £  29.99
 *  0007    USB drive          ( 1) £   6.99
 * --------------------------------------------
 *  Total                               £  66.97
 *
 * This class is mainly used by OrderHub to create and manage order objects during
 * the order lifecycle (ordered → progressing → collected).
 */

public class Order {
    private int orderId;
    private OrderState state;
    private String orderedDateTime="";
    private String progressingDateTime="";
    private String collectedDateTime="";
    private ArrayList<Product> productList = new ArrayList<>(); //Trolley

    // Constructor used by OrderHub to create a new order for a customer.
    // Initializes the order with an ID, state, order date/time, and a list of ordered products.
    public Order(int orderId,OrderState state, String orderedDateTime,ArrayList<Product> productList) {
        this.orderId = orderId;
        this.state = state;
        this.orderedDateTime =orderedDateTime;
        this.productList = new ArrayList<>(productList);
    }

    //a set of getter methods
    public int getOrderId() { return orderId;}
    public OrderState getState() { return state; }
    public String getOrderedDateTime(){ return orderedDateTime; }
    public ArrayList<Product> getProductList() {
        return productList;
    }

    public void setState(OrderState state) { this.state = state; }

    /**
     * order details written to file, used by OrderHub
     *  - Order metadata (ID, state, and three timestamps)
     *  -Product details included in the order
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
}


