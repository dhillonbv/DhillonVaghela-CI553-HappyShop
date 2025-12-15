package ci553.happyshop.utility;

/**
 * The WindowBounds class represents the dimensions and position of a window.
 * It is used to encapsulate a window's x (horizontal position), y (vertical position),
 * width, and height. This class helps to easily retrieve or manipulate the position
 * and size of a window, which is especially useful when positioning other UI elements
 * (like alert simulators or history windows) relative to the main window (warehouse window).
 *
 * Example usage:
 * 1. Getting the position and size of the warehouse window:
 *    WindowBounds bounds = WarehouseView.getWarehouseWindowBounds();
 *    double x = bounds.x;
 *    double y = bounds.y;
 *    double width = bounds.width;
 *    double height = bounds.height;
 *
 * 2. Using these values to position a new window (e.g., HistoryView, AlertSimulator)
 *    window.setX(bounds.x + bounds.width + 10);  // Positioning the new window to the right of the warehouse window
 *    window.setY(bounds.y);                      // Aligning vertically with the warehouse window
 *
 * This class helps maintain consistency in window placement and simplifies
 * window-related calculations, ensuring the UI elements are well positioned.
 */


public class WindowBounds {
    public double x;
    public double y;
    public double width;
    public double height;

    public WindowBounds(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

}
