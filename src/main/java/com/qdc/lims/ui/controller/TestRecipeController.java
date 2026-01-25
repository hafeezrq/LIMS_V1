package com.qdc.lims.ui.controller;

import com.qdc.lims.entity.InventoryItem;
import com.qdc.lims.entity.TestDefinition;
import com.qdc.lims.entity.TestRecipe;
import com.qdc.lims.repository.InventoryItemRepository;
import com.qdc.lims.repository.TestRecipeRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class TestRecipeController {

    @FXML
    private Label testNameLabel;
    @FXML
    private TableView<TestRecipe> recipeTable;
    @FXML
    private TableColumn<TestRecipe, String> itemNameColumn;
    @FXML
    private TableColumn<TestRecipe, Double> quantityColumn;
    @FXML
    private TableColumn<TestRecipe, String> unitColumn;

    @FXML
    private ComboBox<InventoryItem> inventoryItemComboBox;
    @FXML
    private TextField quantityField;

    private final TestRecipeRepository recipeRepository;
    private final InventoryItemRepository inventoryRepository;

    private TestDefinition currentTest;

    public TestRecipeController(TestRecipeRepository recipeRepository, InventoryItemRepository inventoryRepository) {
        this.recipeRepository = recipeRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @FXML
    public void initialize() {
        // Table Config
        itemNameColumn.setCellValueFactory(
                cell -> new SimpleStringProperty(cell.getValue().getInventoryItem().getItemName()));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        unitColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getInventoryItem().getUnit()));

        // Combo Config
        inventoryItemComboBox.setConverter(new StringConverter<InventoryItem>() {
            @Override
            public String toString(InventoryItem item) {
                return item == null ? null
                        : item.getItemName() + " (" + item.getCurrentStock() + " " + item.getUnit() + ")";
            }

            @Override
            public InventoryItem fromString(String string) {
                return null;
            }
        });

        loadInventoryItems();
    }

    public void setTestDefinition(TestDefinition test) {
        this.currentTest = test;
        if (test != null) {
            testNameLabel.setText("Test: " + test.getTestName());
            loadRecipes();
        }
    }

    private void loadInventoryItems() {
        inventoryItemComboBox.setItems(FXCollections.observableArrayList(inventoryRepository.findAll()));
    }

    private void loadRecipes() {
        if (currentTest == null)
            return;
        recipeTable.setItems(FXCollections.observableArrayList(recipeRepository.findByTestId(currentTest.getId())));
    }

    @FXML
    private void handleAdd() {
        if (currentTest == null)
            return;

        InventoryItem selectedItem = inventoryItemComboBox.getValue();
        if (selectedItem == null) {
            showAlert("Required", "Please select an inventory item.");
            return;
        }

        String qtyStr = quantityField.getText().trim();
        double quantity;
        try {
            quantity = Double.parseDouble(qtyStr);
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Quantity must be a valid number.");
            return;
        }

        if (quantity <= 0) {
            showAlert("Invalid Input", "Quantity must be greater than 0.");
            return;
        }

        TestRecipe recipe = new TestRecipe();
        recipe.setTest(currentTest);
        recipe.setInventoryItem(selectedItem);
        recipe.setQuantity(quantity);

        recipeRepository.save(recipe);

        // Reset form and reload
        quantityField.clear();
        inventoryItemComboBox.getSelectionModel().clearSelection();
        loadRecipes();
    }

    @FXML
    private void handleRemove() {
        TestRecipe selected = recipeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Error", "Please select a recipe item to remove.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Remove");
        alert.setHeaderText("Remove " + selected.getInventoryItem().getItemName() + " from recipe?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            recipeRepository.delete(selected);
            loadRecipes();
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) recipeTable.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
