package ci553.happyshop.client.warehouse;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.storageAccess.DerbyRW;
import ci553.happyshop.storageAccess.ImageFileManager;
import ci553.happyshop.utility.StorageLocation;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class WarehouseModel {
    public WarehouseView view;
    public DatabaseRW databaseRW; //Interface type, not specific implementation
                         //Benefits: Flexibility: Easily change the database implementation.

    private ArrayList<Product> productList = new ArrayList<>(); // search results fetched from the database
    private Product theSelectedPro; // the product selected from the ListView before the user edits or deletes
    private String theNewProId;

    //information used to update editProduct child in WarehouseView
    String displayIdEdit="";
    String displayPriceEdit="";
    String displayStockEdit="";
    String displayDescriptionEdit="";
    String displayImageUrlEdit ="WarehouseImageHolder.jpg";

    public HistoryWindow historyWindow;
    public AlertSimulator alertSimulator;
    private String displayInputErrorMsg =""; //error message showing in the alertSimulator
    private ArrayList<String> displayManageHistory = new ArrayList<>();// Manage Product history
                                                               //shows in the HistoryWindow
    private enum ManageProductType{
        Edited,
        Deleted,
        New
    }

    private enum UpdateForAction{
        //actions in Search Page
        BtnSearch,  //actually its updating the Observable ProductList
        BtnEdit,
        BtnDelete,

        //actions in Editing an existing product page
        BtnChangeStockBy, // Refers to both "+" and "−" buttons for changing stock
        BtnSummitEdit,
        BtnCancelEdit,

        // actions in Adding a new product to stock page
        BtnCancelNew,
        BtnSummitNew,

        //show user input error message in alertSimulator
        ShowInputErrorMsg
    }

    void doSearch() throws SQLException {
        String keyword = view.tfSearchKeyword.getText().trim();
        if (!keyword.equals("")) {
            productList = databaseRW.searchProduct(keyword);
        }
        else{
            productList.clear();
            System.out.println("please type product ID or name to search");
        }
        updateView(UpdateForAction.BtnSearch);
    }

    void doDelete() throws SQLException, IOException {
        System.out.println("delete gets called in model");
        Product pro  = view.obrLvProducts.getSelectionModel().getSelectedItem();
        if (pro != null ) {
            theSelectedPro = pro;
            productList.remove(theSelectedPro); //remove the product from product List

            //update databse: delete the product from database
            databaseRW.deleteProduct(theSelectedPro.getProductId());

            //delete the image from imageFolder "images/"
            String imageName = theSelectedPro.getProductImageName(); //eg 0011.jpg;
            ImageFileManager.deleteImageFile(StorageLocation.imageFolder, imageName);

            updateView(UpdateForAction.BtnDelete);
            theSelectedPro = null;
        }
        else{
            System.out.println("No product was selected");
        }
    }

    void doEdit() {
        System.out.println("Edit gets called in model");
        Product pro = view.obrLvProducts.getSelectionModel().getSelectedItem();
        if (pro != null) {
            theSelectedPro = pro;
            displayIdEdit = theSelectedPro.getProductId();
            displayPriceEdit = String.format("%.2f", theSelectedPro.getUnitPrice());
            displayStockEdit = String.valueOf (theSelectedPro.getStockQuantity());
            displayDescriptionEdit = theSelectedPro.getProductDescription();

            String relativeImageUri = StorageLocation.imageFolder + theSelectedPro.getProductImageName();
            Path imageFullPath = Paths.get(relativeImageUri).toAbsolutePath();
            displayImageUrlEdit = imageFullPath.toUri().toString();//build the full path Uri

            System.out.println("get new pro image name: " + displayImageUrlEdit);
            updateView(UpdateForAction.BtnEdit);
        }
        else{
            System.out.println("No product was selected");
        }

    }

    void doCancel(){
       if(view.theProFormMode.equals("EDIT")){
           updateView(UpdateForAction.BtnCancelEdit);
           theSelectedPro = null;
       }
       if(view.theProFormMode.equals("NEW")){
           updateView(UpdateForAction.BtnCancelNew);
           theNewProId = null;
       }
    }
    void doSummit() throws SQLException, IOException {
        if(view.theProFormMode.equals("EDIT")){
            doSubmitEdit();
        }
        if(view.theProFormMode.equals("NEW")){
            doSubmitNew();
        }
    }

    private void doSubmitEdit() throws IOException, SQLException {
        System.out.println("ok edit is called");
        if(theSelectedPro!=null) {
            String id=theSelectedPro.getProductId();
            System.out.println("theSelectedPro " + id); //debug purpose
            String imageName = theSelectedPro.getProductImageName();

            String textPrice =view.tfPriceEdit.getText().trim();
            String textStock =view.tfStockEdit.getText().trim();
            String description = view.taDescriptionEdit.getText().trim();

            if(view.isUserSelectedImageEdit == true){  //if the user changed image
                ImageFileManager.deleteImageFile(StorageLocation.imageFolder, imageName); //delete the old image
                //copy the user selected image to project image folder
                //we use productId as image name, but we need to get its extension from the user selected image
                String newImageNameWithExtension = ImageFileManager.copyFileToDestination(view.userSelectedImageUriEdit, StorageLocation.imageFolder,id);
                imageName = newImageNameWithExtension;
            }

            if(validateInputEditChild(textPrice,textStock,description)==false){
                updateView(UpdateForAction.ShowInputErrorMsg);
            }
            else{
                double price = Double.parseDouble(textPrice);
                int stock= Integer.parseInt(textStock);
                //update datbase
                databaseRW.updateProduct(id,description,price,imageName,stock);

                updateView(UpdateForAction.BtnSummitEdit);
                theSelectedPro=null;
            }
        }
        else{
            System.out.println("No Product Selected");
        }
    }

    void doChangeStockBy(String addOrSub) throws SQLException {
        int oldStock = Integer.parseInt(view.tfStockEdit.getText().trim());
        int newStock =oldStock;
        String TextChangeBy = view.tfChangeByEdit.getText().trim();
        if(!TextChangeBy.isEmpty()){
            if(validateInputChangeStockBy(TextChangeBy)==false){
                updateView(UpdateForAction.ShowInputErrorMsg);
            } else{
                int changeBy = Integer.parseInt(TextChangeBy);
                switch(addOrSub){
                    case "add":
                        newStock = oldStock + changeBy;
                        break;
                    case "sub":
                        newStock = oldStock - changeBy;
                        break;
                }
                displayStockEdit = String.valueOf (newStock);
                updateView(UpdateForAction.BtnChangeStockBy);
            }
        }
    }

    private  boolean validateInputChangeStockBy(String txChangeBy) throws SQLException {
        StringBuilder errorMessage = new StringBuilder();
        // Validate Stock changBy Quantity (must be an integer)
        try {
            int changeBy = Integer.parseInt(txChangeBy);
        } catch (NumberFormatException e) {
            errorMessage.append("Invalid stock quantity format.\n");
        }
        // Show Alert if there are errors
        if (errorMessage.length() > 0) {
            displayInputErrorMsg = errorMessage.toString();
            return false;
        }
        return true;
    }

    private void doSubmitNew() throws SQLException, IOException {
        System.out.println("Adding new Pro in model");

        //all info(input from user) about the new product
        theNewProId = view.tfIdNewPro.getText().trim();
        String textPrice = view.tfPriceNewPro.getText().trim();
        String textStock = view.tfStockNewPro.getText().trim();
        String description = view.taDescriptionNewPro.getText().trim();
        String iPath = view.imageUriNewPro; //image Path from the imageChooser in View class

        //validate input
        if (validateInputNewProChild(theNewProId, textPrice, textStock, description, iPath) ==false) {
            updateView(UpdateForAction.ShowInputErrorMsg);
        } else {
            //copy the user selected image to project image folder and using productId as image name
            //and get the image extension from the source image, we write this name to database
            String imageNameWithExtension = ImageFileManager.copyFileToDestination(view.imageUriNewPro, StorageLocation.imageFolder,theNewProId);
            double price = Double.parseDouble(textPrice);
            int stock = Integer.parseInt(textStock);

            //insertNewProduct to databse (String id, String des,double price,String image,int stock)
            //a record in databse looks like ('0001', '40 inch TV', 269.00,'0001TV.jpg',100)"
            databaseRW.insertNewProduct(theNewProId,description,price,imageNameWithExtension,stock);
            updateView(UpdateForAction.BtnSummitNew);
            theNewProId = null;
        }
    }

    private  boolean validateInputEditChild(String txPrice, String txStock,
                                         String description) throws SQLException {

        StringBuilder errorMessage = new StringBuilder();

        // Validate Price (must be a positive number, and two digitals )
        try {
            double price = Double.parseDouble(txPrice);

            // Validate: Ensure at most two decimal places
            if (!txPrice.matches("^[0-9]+(\\.[0-9]{0,2})?$")) {
                errorMessage.append("\u2022 Price can have at most two decimal places.\n");
            }

            if (price <= 0) {
                errorMessage.append("\u2022 Price must be a positive number.\n");
            }

        } catch (NumberFormatException e) {
            errorMessage.append("\u2022 Invalid price format.\n");
        }

        // Validate if there is unperformed stock changeBy:
        if(!view.tfChangeByEdit.getText().trim().isEmpty()){
            errorMessage.append("\u2022 Change stock by not applied.\n");
        }

        // Validate Stock Quantity (must be a non-negative integer)
        try {
            int stock = Integer.parseInt(txStock);
            if (stock < 0) {
                errorMessage.append("\u2022 Stock quantity cannot be negative.\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("\u2022 Invalid stock quantity format.\n");
        }

        // Validate Description
        if (description.isEmpty())
            errorMessage.append("\u2022 Product description cannot be empty.");

        // Show Alert if there are errors
        if (errorMessage.length() > 0) {
            displayInputErrorMsg = errorMessage.toString();
            return false;
        }
        return true;
    }

    private  boolean validateInputNewProChild(String id, String txPrice, String txStock,
                                   String description, String imageUri) throws SQLException {

        StringBuilder errorMessage = new StringBuilder();
        // Validate Id (must be exactly 4 digits)
        if (id == null || !id.matches("\\d{4}"))
            errorMessage.append("\u2022 Product ID must be exactly 4 digits.\n");

        //check Id is unique
        if(!databaseRW.isProIdAvailable(id))
            errorMessage.append("\u2022 Product ID " + id + " is not available.\n");

        // Validate Price (must be a positive number, and two digitals )
        try {
            double price = Double.parseDouble(txPrice);

            // Validate: Ensure at most two decimal places
            if (!txPrice.matches("^[0-9]+(\\.[0-9]{0,2})?$")) {
                errorMessage.append("\u2022 Price can have at most two decimal places.\n");
            }

            if (price <= 0) {
                errorMessage.append("\u2022 Price must be a positive number.\n");
            }

        } catch (NumberFormatException e) {
            errorMessage.append("\u2022 Invalid price format.\n");
        }


        // Validate Stock Quantity (must be a non-negative integer)
        try {
            int stock = Integer.parseInt(txStock);
            if (stock < 0) {
                errorMessage.append("\u2022 Stock quantity cannot be negative.\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("\u2022 Invalid stock quantity format.\n");
        }

        // Validate Description
        if (description.isEmpty())
            errorMessage.append("\u2022 Product description cannot be empty.\n");

        // Validate Image Path
        if (imageUri == null )
            errorMessage.append("\u2022 An image must be selected.");

        // Show Alert if there are errors
        if (errorMessage.length() > 0) {
            displayInputErrorMsg = errorMessage.toString();
            return false;
        }
        return true;
    }


    private void updateView(UpdateForAction updateFor){
        switch (updateFor) {
            case UpdateForAction.BtnSearch:
                view.updateObservableProductList(productList);
                break;
            case UpdateForAction.BtnEdit:
                view.updateEditProductChild(displayIdEdit,displayPriceEdit,displayStockEdit,displayDescriptionEdit,displayImageUrlEdit);
                break;
            case UpdateForAction.BtnDelete:
                view.updateObservableProductList(productList); //update search page in view
                showManageStockHistory(ManageProductType.Deleted);
                view.resetEditChild();
                alertSimulator.closeAlertSimulatorWindow();//close AlertSimulatorWindow if exists
                break;

            case UpdateForAction.BtnChangeStockBy:
                view.updateBtnAddSub(displayStockEdit);
                alertSimulator.closeAlertSimulatorWindow();//close AlertSimulatorWindow if exists
                break;

            case UpdateForAction.BtnCancelEdit:
                view.resetEditChild();
                alertSimulator.closeAlertSimulatorWindow();//close AlertSimulatorWindow if exists
                break;

            case UpdateForAction.BtnSummitEdit:
                showManageStockHistory(ManageProductType.Edited);
                view.resetEditChild();
                alertSimulator.closeAlertSimulatorWindow();//close AlertSimulatorWindow if exists
                break;

            case UpdateForAction.BtnCancelNew:
                view.resetNewProChild();
                alertSimulator.closeAlertSimulatorWindow();//close AlertSimulatorWindow if exists
                break;

            case UpdateForAction.BtnSummitNew:
                showManageStockHistory(ManageProductType.New );
                view.resetNewProChild();
                alertSimulator.closeAlertSimulatorWindow();//close AlertSimulatorWindow if exists
                break;

            case UpdateForAction.ShowInputErrorMsg:
                alertSimulator.showErrorMsg(displayInputErrorMsg);
        }
    }

    private void showManageStockHistory(ManageProductType type){
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String record="";
        switch (type) {
            case ManageProductType.Edited:
                record = theSelectedPro.getProductId() + " edited successfully, " + dateTime;
                break;
            case ManageProductType.Deleted:
                record = theSelectedPro.getProductId() + " deleted successfully, " + dateTime;
                break;
            case ManageProductType.New :
                record = theNewProId + " added to database successfully, " + dateTime;
        }
        if(!record.equals(""))
            displayManageHistory.add(record);
        historyWindow.showManageHistory(displayManageHistory);
    }

}
