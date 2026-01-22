package ci553.happyshop.catalogue;

import java.util.ArrayList;

/**
 * Thrown when checkout validation finds one or more items with insufficient stock.
 */
public class InsufficientStockException extends Exception {

    private ArrayList<Product> insufficientProducts;

    public InsufficientStockException(String message, ArrayList<Product> insufficientProducts) {
        super(message);
        this.insufficientProducts = insufficientProducts;
    }

    public ArrayList<Product> getInsufficientProducts() {
        return insufficientProducts;
    }
}
