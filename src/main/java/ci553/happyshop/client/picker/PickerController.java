package ci553.happyshop.client.picker;

import java.io.IOException;

public class PickerController {
    public PickerModel pickerModel;

    public void doProgressing() throws IOException {
        pickerModel.doProgressing();
    }
    public void doCollected() throws IOException {
        pickerModel.doCollected();
    }
}
