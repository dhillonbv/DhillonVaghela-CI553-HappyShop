package ci553.happyshop.client.picker;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * A standalone Order Picker Client that can be run independently without launching the full system.
 * Designed for early-stage testing, though full functionality may require other clients to be active.
 */

public class PickerClient extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Creates the Model, View, and Controller objects for the Picker client.
     * Links them together so they can communicate with each other.
     * Starts the Picker interface.
     *
     * Also registers the PickerModel with the OrderHub to receive order notifications.
     */
    @Override
    public void start(Stage window) {
        PickerModel pickerModel = new PickerModel();
        PickerView pickerView = new PickerView();
        PickerController pickerController = new PickerController();

        pickerView.pickerController = pickerController;
        pickerController.pickerModel = pickerModel;
        pickerModel.pickerView = pickerView;

        pickerModel.registerWithOrderHub();
        pickerView.start(window);
    }
}
