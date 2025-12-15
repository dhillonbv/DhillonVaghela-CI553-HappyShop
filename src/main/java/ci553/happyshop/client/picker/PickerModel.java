package ci553.happyshop.client.picker;

import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.orderManagement.OrderState;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * PickerModel represents the logic order picker.
 * PickerModel handles two main responsibilities:
 * 1. Observing OrderHub.
 * 2. Notifying PickerView to Updates user interface.
 *
 * 1. Observing OrderHub.
 * PickerModel is an observer of  OrderHub, receiving orderMap from OrderHub.
 * When a picker claims a task, PickerModel:
 * - Retrieves the first unlocked order from the orderMap.
 * - Locks the selected order to prevent other pickers from accessing it.
 * - Notifies OrderHub to update the orderMap, and begin preparation of the order.
 *
 * Once the order is collected by the customer, PickerModel:
 * - Unlocks the order.
 * - Notifies OrderHub to update the orderMap.
 * - Begins the next task if available.
 *
 * All changes in order state are centralized through OrderHub to ensure synchronization.
 * No picker directly changes the display before OrderHub updates the shared orderMap;
 * instead, each PickerModel waits for OrderHub's notification to refresh its state.
 *
 * Imagine the interaction flow:
 * PickerModel: "Hey OrderHub, I found an order that needs to be prepared. Please update the orderMap."
 * OrderHub: "Got it. I'll update the orderMap first."
 * OrderHub (after updating): "Attention all pickers: the orderMap has changed. Please refresh your views."
 *
 * This ensures that all PickerModels stay in sync by only updating their local state
 * in response to centralized changes made by the OrderHub.
 */

public class PickerModel {
    public PickerView pickerView;
    private OrderHub orderHub = OrderHub.getOrderHub();

    //two elements that need to be passed to PickerView for updating.
    private String displayTaOrderMap="";
    private String displayTaOrderDetail ="";

    // TreeMap (orderID,state) holding order IDs and their corresponding states.
    private static TreeMap<Integer, OrderState> orderMap = new TreeMap<>();
    private static TreeSet<Integer> lockedOrderIds = new TreeSet<>(); // Track locked orders by orderId

    private int theOrderId=0; //Order ID assigned to a picker;
                              // 0 means no order is currently assigned.
    private OrderState theOrderState;

    /**
     * Attempts to find an unlocked order for this picker and mark it as progressing.
     * The order will be locked to prevent other pickers from accessing it.
     * Only the first unlocked order found will be processed.
     */
    public void doProgressing() throws IOException {
        for (Map.Entry<Integer, OrderState> entry : orderMap.entrySet()) {
            int orderId = entry.getKey();
            if (!isOrderLocked(orderId)) { // Find the first unlocked order
                lockOrder(orderId);// Lock the order to prevent other pickers from taking it
                theOrderId = orderId; // Save the assigned orderId to this picker and update its state
                theOrderState = OrderState.Progressing;
                notifyOrderHub();// Notify the OrderHub about the state change
                updatePickerView(); // Refresh picker view
                return; // Exit after handling one order
            }
        }
    }

    // Lock an order
    private boolean lockOrder(int orderId) {
        if (lockedOrderIds.contains(orderId)) {
            return false; // Order is already locked
        } else {
            lockedOrderIds.add(orderId);
            return true; // Successfully locked the order
        }
    }

    // Unlock an order
    private void unlockOrder(int orderId) {
        lockedOrderIds.remove(orderId);
    }

    // Check if an order is locked
    private boolean isOrderLocked(int orderId) {
        return lockedOrderIds.contains(orderId);
    }

    public void doCollected() throws IOException {
        if(theOrderId!=0 && isOrderLocked(theOrderId)){
            theOrderState = OrderState.Collected;
            notifyOrderHub(); // Notify the OrderHub about the state change
            displayTaOrderDetail = "";
            updatePickerView(); // update picker view
            theOrderId=0;  //reset to no order is with the picker
            unlockOrder(theOrderId);//remove the order from locked orderId set
        }
    }

    // Registers this PickerModel instance with the OrderHub
    //so it can receive updates about orderMap changes.
    public void registerWithOrderHub(){
        OrderHub orderHub = OrderHub.getOrderHub();
        orderHub.registerPickerModel(this);
    }

    //Notifies the OrderHub of a change in the order state.
    //If the order is moving to the 'Progressing' state, asks OrderHub to read the order detail
    // from the file system for displaying in the pickerView.
    private void notifyOrderHub() throws IOException {
        orderHub.changeOrderStateMoveFile(theOrderId, theOrderState);
        if (theOrderState == OrderState.Progressing) {
            // Read order file, ie. order details
            displayTaOrderDetail = orderHub.getOrderDetailForPicker(theOrderId);
        }
    }

    // Sets the order map with new data and refreshes the display.
    // This method is called by OrderHub to set orderMap for picker.
    public void setOrderMap(TreeMap<Integer,OrderState> om) {
        orderMap.clear();
        orderMap.putAll(om);
        displayTaOrderMap= buildOrderMapString();
        updatePickerView();
    }

    //Builds a formatted string representing the current order map.
    //Each line contains the order ID followed by its state, aligned with spacing.
    private String buildOrderMapString() {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<Integer, OrderState> entry : orderMap.entrySet()) {
            int orderId = entry.getKey();
            OrderState orderState = entry.getValue();
            sb.append(orderId).append(" ".repeat(8)).append(orderState).append("\n");
        }
        return sb.toString();
    }

    private void updatePickerView()
    {
        pickerView.update(displayTaOrderMap,displayTaOrderDetail);
    }
}
