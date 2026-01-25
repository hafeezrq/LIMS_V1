package com.qdc.lims.ui.controller;

import com.qdc.lims.entity.Department;
import com.qdc.lims.repository.DepartmentRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class CategoryManagementController {

    @FXML
    private TableView<Department> categoryTable;
    @FXML
    private TableColumn<Department, String> nameColumn;
    @FXML
    private TableColumn<Department, String> descriptionColumn;

    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionArea;

    private final DepartmentRepository departmentRepository;
    private final ObservableList<Department> categoryList = FXCollections.observableArrayList();

    // Callback to refresh parent controller's combo box
    private Runnable onUpdateCallback;

    @Autowired
    public CategoryManagementController(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @FXML
    public void initialize() {
        setupTable();
        loadCategories();

        // Add selection listener
        categoryTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> showDetails(newVal));
    }

    public void setOnUpdateCallback(Runnable onUpdateCallback) {
        this.onUpdateCallback = onUpdateCallback;
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("code"));

        categoryTable.setItems(categoryList);
    }

    private void loadCategories() {
        categoryList.clear();
        categoryList.addAll(departmentRepository.findAll());
    }

    private void showDetails(Department category) {
        if (category != null) {
            nameField.setText(category.getName());
            descriptionArea.setText(category.getCode());
        } else {
            handleClear();
        }
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText();
        if (name == null || name.trim().isEmpty()) {
            showAlert("Error", "Department name is required.");
            return;
        }

        try {
            Department category = categoryTable.getSelectionModel().getSelectedItem();
            if (category == null) {
                // Check for duplicate name for new items
                Optional<Department> existing = departmentRepository.findByName(name.trim());
                if (existing.isPresent()) {
                    showAlert("Error", "Department with this name already exists.");
                    return;
                }
                category = new Department();
            } else {
                // If editing, check if name changed and if it conflicts
                if (!category.getName().equals(name.trim())) {
                    Optional<Department> existing = departmentRepository.findByName(name.trim());
                    if (existing.isPresent()) {
                        showAlert("Error", "Department with this name already exists.");
                        return;
                    }
                }
            }

            category.setName(name.trim());
            category.setCode(descriptionArea.getText() != null ? descriptionArea.getText().trim() : null);
            category.setActive(true);

            departmentRepository.save(category);
            loadCategories();
            handleClear();

            if (onUpdateCallback != null) {
                onUpdateCallback.run();
            }

        } catch (Exception e) {
            showAlert("Error", "Failed to save department: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        Department selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a department to delete.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Department");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("Delete department: " + selected.getName() + "?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                departmentRepository.delete(selected);
                loadCategories();
                handleClear();

                if (onUpdateCallback != null) {
                    onUpdateCallback.run();
                }
            } catch (Exception e) {
                showAlert("Error", "Cannot delete department (it may be in use).");
            }
        }
    }

    @FXML
    private void handleClear() {
        nameField.clear();
        descriptionArea.clear();
        categoryTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
