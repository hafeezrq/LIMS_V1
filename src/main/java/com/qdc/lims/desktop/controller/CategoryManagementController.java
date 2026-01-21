package com.qdc.lims.desktop.controller;

import com.qdc.lims.entity.TestCategory;
import com.qdc.lims.repository.TestCategoryRepository;
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
    private TableView<TestCategory> categoryTable;
    @FXML
    private TableColumn<TestCategory, String> nameColumn;
    @FXML
    private TableColumn<TestCategory, String> descriptionColumn;

    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionArea;

    private final TestCategoryRepository categoryRepository;
    private final ObservableList<TestCategory> categoryList = FXCollections.observableArrayList();

    // Callback to refresh parent controller's combo box
    private Runnable onUpdateCallback;

    @Autowired
    public CategoryManagementController(TestCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
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
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        categoryTable.setItems(categoryList);
    }

    private void loadCategories() {
        categoryList.clear();
        categoryList.addAll(categoryRepository.findAll());
    }

    private void showDetails(TestCategory category) {
        if (category != null) {
            nameField.setText(category.getName());
            descriptionArea.setText(category.getDescription());
        } else {
            handleClear();
        }
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText();
        if (name == null || name.trim().isEmpty()) {
            showAlert("Error", "Category name is required.");
            return;
        }

        try {
            TestCategory category = categoryTable.getSelectionModel().getSelectedItem();
            if (category == null) {
                // Check for duplicate name for new items
                Optional<TestCategory> existing = categoryRepository.findByName(name.trim());
                if (existing.isPresent()) {
                    showAlert("Error", "Category with this name already exists.");
                    return;
                }
                category = new TestCategory();
            } else {
                // If editing, check if name changed and if it conflicts
                if (!category.getName().equals(name.trim())) {
                    Optional<TestCategory> existing = categoryRepository.findByName(name.trim());
                    if (existing.isPresent()) {
                        showAlert("Error", "Category with this name already exists.");
                        return;
                    }
                }
            }

            category.setName(name.trim());
            category.setDescription(descriptionArea.getText());
            category.setActive(true);

            categoryRepository.save(category);
            loadCategories();
            handleClear();

            if (onUpdateCallback != null) {
                onUpdateCallback.run();
            }

        } catch (Exception e) {
            showAlert("Error", "Failed to save category: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        TestCategory selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a category to delete.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Category");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("Delete category: " + selected.getName() + "?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                categoryRepository.delete(selected);
                loadCategories();
                handleClear();

                if (onUpdateCallback != null) {
                    onUpdateCallback.run();
                }
            } catch (Exception e) {
                showAlert("Error", "Cannot delete category (it may be in use).");
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
