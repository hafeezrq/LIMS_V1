package com.qdc.lims.ui.controller;

import com.qdc.lims.entity.InventoryItem;
import com.qdc.lims.entity.Supplier;
import com.qdc.lims.repository.InventoryItemRepository;
import com.qdc.lims.repository.SupplierRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Form controller for creating and editing {@link InventoryItem} records.
 */
@Component
@Scope("prototype")
public class InventoryItemFormController {

    @FXML
    private Label titleLabel;
    @FXML
    private TextField itemNameField;
    @FXML
    private ComboBox<String> unitComboBox;
    @FXML
    private TextField minThresholdField;
    @FXML
    private TextField currentStockField;
    @FXML
    private TextField avgCostField;
    @FXML
    private ComboBox<Supplier> supplierComboBox;
    @FXML
    private CheckBox activeCheckBox;

    private final InventoryItemRepository inventoryRepository;
    private final SupplierRepository supplierRepository;

    private InventoryItem item;
    private Runnable onSaveCallback;

    /**
     * Creates the form controller.
     */
    public InventoryItemFormController(InventoryItemRepository inventoryRepository,
            SupplierRepository supplierRepository) {
        this.inventoryRepository = inventoryRepository;
        this.supplierRepository = supplierRepository;
    }

    /**
     * Loads reference data such as units and suppliers.
     */
    @FXML
    public void initialize() {
        unitComboBox.setItems(FXCollections.observableArrayList(
                "pcs", "ml", "liters", "units", "boxes", "tests", "strips"));

        List<Supplier> suppliers = supplierRepository.findAll();
        supplierComboBox.setItems(FXCollections.observableArrayList(suppliers));

        supplierComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Supplier s) {
                return s == null ? null : s.getCompanyName();
            }

            @Override
            public Supplier fromString(String string) {
                return null;
            }
        });
    }

    /**
     * Sets the item to edit (or creates a new one) and updates the form title.
     *
     * @param existingItem item to edit, or {@code null} for a new item
     */
    public void setInventoryItem(InventoryItem existingItem) {
        this.item = existingItem;
        if (item != null) {
            titleLabel.setText("Edit Inventory Item");
            populateFields();
        } else {
            titleLabel.setText("New Inventory Item");
            item = new InventoryItem();
        }
    }

    /**
     * Registers a callback invoked after a successful save.
     */
    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    private void populateFields() {
        itemNameField.setText(item.getItemName());
        unitComboBox.setValue(item.getUnit());
        minThresholdField.setText(item.getMinThreshold() != null ? item.getMinThreshold().toString() : "");
        currentStockField.setText(item.getCurrentStock() != null ? item.getCurrentStock().toString() : "");
        avgCostField.setText(item.getAverageCost() != null ? item.getAverageCost().toString() : "");
        supplierComboBox.setValue(item.getPreferredSupplier());
        activeCheckBox.setSelected(item.isActive());
    }

    /**
     * Validates form input, persists the item, and closes the dialog.
     */
    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        item.setItemName(itemNameField.getText().trim());
        item.setUnit(unitComboBox.getValue());
        item.setMinThreshold(Double.parseDouble(minThresholdField.getText().trim()));
        item.setCurrentStock(Double.parseDouble(currentStockField.getText().trim()));

        String costText = avgCostField.getText().trim();
        item.setAverageCost(costText.isEmpty() ? 0.0 : Double.parseDouble(costText));

        item.setPreferredSupplier(supplierComboBox.getValue());
        item.setActive(activeCheckBox.isSelected());

        try {
            inventoryRepository.save(item);
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            handleCancel();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not save item: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Performs basic validation on required fields and numeric inputs.
     */
    private boolean validateInput() {
        if (itemNameField.getText().trim().isEmpty()) {
            showAlert("Item Name is required.");
            return false;
        }
        if (unitComboBox.getValue() == null || unitComboBox.getValue().trim().isEmpty()) {
            showAlert("Unit is required.");
            return false;
        }

        try {
            Double.parseDouble(minThresholdField.getText().trim());
        } catch (Exception e) {
            showAlert("Min Threshold must be a valid number.");
            return false;
        }

        try {
            Double.parseDouble(currentStockField.getText().trim());
        } catch (Exception e) {
            showAlert("Current Stock must be a valid number.");
            return false;
        }

        return true;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.showAndWait();
    }

    /**
     * Closes the form window without saving.
     */
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) itemNameField.getScene().getWindow();
        stage.close();
    }
}
