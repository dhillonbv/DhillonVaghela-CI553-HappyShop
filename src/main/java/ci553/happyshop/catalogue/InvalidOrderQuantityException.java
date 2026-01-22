package ci553.happyshop.catalogue;

/**
 * Thrown when an order contains an invalid quantity (zero or negative).
 */
public class InvalidOrderQuantityException extends Exception {

    private String productId;
    private int quantity;

    public InvalidOrderQuantityException(String productId, int quantity) {
        super("Invalid quantity for product " + productId + ": " + quantity);
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}
