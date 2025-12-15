package ci553.happyshop.client.orderTracker;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * A standalone OrderTracker client that can be run independently without launching the full system.
 * Designed for early-stage testing, though full functionality may require other clients to be active.
 *
 * This client is simple and does not follow the MVC pattern, as it only registers with the OrderHub
 * to receive order status notifications. All logic is handled internally within the OrderTracker.
 */

public class OrderTrackerClient extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage window) {
        OrderTracker orderTracker = new OrderTracker();
        orderTracker.registerWithOrderHub();
    }
}
