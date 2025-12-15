package ci553.happyshop.utility;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * This final class is responsible for managing the dynamic positioning of windows on the screen.
 * It calculates the optimal position for each new window based on the size of the screen and previously opened windows,
 * ensuring that windows are spaced out evenly and that new windows do not overlap with existing ones.
 * The class is designed to prevent instantiation and subclassing, and it provides a single static method,
 * `registerWindow`, to register new windows and calculate their position on the screen.
 *
 * Purpose:
 * - To automatically arrange windows on the screen, avoiding overlap by dynamically adjusting their positions.
 * - The windows are arranged row by row with a configurable gap between them, starting from a base position.
 * - When a row is filled, windows will start in the next row, and if necessary, the windows will stack vertically when the screen is filled.
 * - This is useful for applications that create multiple windows and need to ensure they are positioned neatly and without overlap.
 */


// final class:  to prevent subclassing
public final class WinPosManager {
    // Get the screen size
    private static final Rectangle2D SCREEN_SIZE = Screen.getPrimary().getVisualBounds();
    private static final double SCREEN_WIDTH = SCREEN_SIZE.getWidth();
    private static final double SCREEN_HEIGHT = SCREEN_SIZE.getHeight();

    private static final double BASE_X = 10; // Initial X position
    private static final double BASE_Y = 10; // Initial Y position
    private static final double GAP = 10; // Space between windows

    private static double occupiedWidth = 0; // total width of all windows on current row
    private static double occupiedHeight = 0;
    private static double x = BASE_X; //x position for current window
    private static double y = BASE_Y;//y position for current window

    // Private constructor to prevent instantiation
    private WinPosManager() {
        throw new UnsupportedOperationException("final_static class does not have object");
    }

    public static void registerWindow(Stage stage, double width, double height) {
        // Case 1: Fits in current row and within screen height
        if ((occupiedWidth + width < SCREEN_WIDTH - BASE_X) &&
                (occupiedHeight + height < SCREEN_HEIGHT - BASE_Y)) {
            stage.setX(x);
            stage.setY(y);

            occupiedWidth += width + GAP;
            x += width + GAP;
        }

        // Case 2: New row (horizontal overflow, but vertical space available)
        else if ((occupiedHeight + height < SCREEN_HEIGHT - BASE_Y)) {
            // Move to next row
            occupiedWidth = 0;
            x = BASE_X;
            y += height + GAP * 4;

            stage.setX(x);
            stage.setY(y);

            occupiedWidth += width + GAP;
            x += width + GAP;
            occupiedHeight += height + GAP;
        }

        // Case 3: No space — fallback to fixed position (bottom-right stack)
        else {
            stage.setX(SCREEN_WIDTH - width - GAP);
            stage.setY(SCREEN_HEIGHT - height - GAP * 4);
        }
    }


    // Alternative window positioning strategy, (for reference, not used in this version):
    // overflow windows are placed in the last row,
    // continuing horizontally next to the last window.
    public static void registerWindow2(Stage stage, double width, double height) {
        // If the new window exceeds the screen width, move to next row
        if (occupiedWidth + width > SCREEN_WIDTH - BASE_X) {
            occupiedWidth = 0; // Reset occupiedWidth
            x = BASE_X;  //reset x for next row
            y += height + GAP*4; // update y so that move to next row,
        }

        // If the new row exceeds screen height, windows are placed in the last row,
        if (y + height > SCREEN_HEIGHT - BASE_Y) {
            y -= (height + GAP*4); // Stack on top
        }

        // Set window position
        stage.setX(x);
        stage.setY(y);

        // Update the occupied width and x position for the next window
        occupiedWidth += width + GAP;
        x += width + GAP;
    }
}
