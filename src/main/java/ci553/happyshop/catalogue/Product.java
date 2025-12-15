package ci553.happyshop.catalogue;

/**
 * The Product class used to hold the information about a product:
 *
 * Fields:
 * - productId: Unique identifier for the product (eg 0001).
 * - description: Textual description of the product.
 * - unitPrice: Price per single unit of the product.
 * - orderedQuantity: Quantity involved in a customer's order.
 * - stockQuantity: Quantity currently available in stock.
 */

public class Product implements Comparable<Product> {
    private String proId;
    private String proDescription;
    private String proImageName;
    private double unitPrice;
    private int orderedQuantity =1; //The quantity of this product in the customer's order.
    private int stockQuantity;//

    /**
     * Constructor,used by DatabaseRW, make product from searching ResultSet
     * @param id Product ID
     * @param des Description of product
     * @param image image name of product, eg 0001.jpg (0001 is product ID)
     * @param aPrice The price of the product
     * @param stockQuantity The Quantity of the product in stock
     */
    public Product(String id, String des, String image, double aPrice, int stockQuantity) {
        proId = id;
        proDescription = des;
        proImageName = image;
        unitPrice = aPrice;
        this.stockQuantity = stockQuantity;
    }

    // a set of getter methods
    public String getProductId() { return proId;}
    public String getProductDescription() { return proDescription;}
    public String getProductImageName() { return proImageName;}
    public double getUnitPrice() { return unitPrice;}
    public int getOrderedQuantity() { return orderedQuantity;}
    public int getStockQuantity() { return stockQuantity;}

    //a setter method
    public void setOrderedQuantity(int orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
    }

    @Override
    public int compareTo(Product otherProduct) {
        // Compare by product ID or any other attribute you want to sort by
        return this.proId.compareTo(otherProduct.proId); // Sort by proId alphabetically (ascending);
    }

    @Override
    // Creates a formatted string containing ID, price (with 2 decimal places), stock amount, and description
    // Used in the Warehouse search page to display searched product information
    public String toString() {
        String productInfo = String.format("Id: %s, £%.2f/uint, stock: %d \n%s",
                          proId, unitPrice,stockQuantity,proDescription);
        return productInfo;
    }

    /** alternative constructors retained for possible future use.
     *
    public Product(String id, String des, double aPrice, int orderedQuantity, int stockQuantity) {
        proId = id;
        proDescription = des;
        unitPrice = aPrice;
        this.orderedQuantity = orderedQuantity;
        this.stockQuantity = stockQuantity;
    }

    public Product(String id, String des, double aPrice, int orderedQuantity) {
        proId = id;
        proDescription = des;
        unitPrice = aPrice;
        this.orderedQuantity = orderedQuantity;
    }
     */

}

