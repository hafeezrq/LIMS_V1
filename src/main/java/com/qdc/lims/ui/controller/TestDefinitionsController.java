package com.qdc.lims.ui.controller;

import com.qdc.lims.entity.Department;
import com.qdc.lims.entity.TestDefinition;
import com.qdc.lims.service.TestDefinitionService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Controller for browsing, searching, and maintaining {@link TestDefinition}
 * master data.
 */
@Component
public class TestDefinitionsController {

    @FXML
    private TextField searchField;
    @FXML
    private TableView<TestDefinition> testTable;
    @FXML
    private TableColumn<TestDefinition, String> colTestName;
    @FXML
    private TableColumn<TestDefinition, String> colShortCode;
    @FXML
    private TableColumn<TestDefinition, String> colDepartment;
    @FXML
    private TableColumn<TestDefinition, Double> colPrice;
    @FXML
    private TableColumn<TestDefinition, Void> colActions;
    @FXML
    private Label statusLabel;

    @Autowired
    private TestDefinitionService testDefinitionService;

    /**
     * Initializes table bindings and loads initial data.
     */
    @FXML
    public void initialize() {
        if (testDefinitionService == null) {
            statusLabel.setText("Error: Service not injected");
            return;
        }
        colDepartment.setCellValueFactory(cellData -> {
            Department dept = cellData.getValue().getDepartment();
            return new SimpleObjectProperty<>(dept != null ? dept.getName() : "");
        });
        setupActionsColumn();
        loadTests();
    }

    /**
     * Loads all tests into the table.
     */
    private void loadTests() {
        try {
            ObservableList<TestDefinition> tests = FXCollections.observableArrayList(testDefinitionService.findAll());
            testTable.setItems(tests);
            statusLabel.setText("Loaded " + tests.size() + " tests.");
        } catch (Exception e) {
            statusLabel.setText("Error loading tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Configures the actions column with edit and delete buttons.
     */
    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setOnAction(event -> handleEdit(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(event -> handleDelete(getTableView().getItems().get(getIndex())));
                deleteBtn.setStyle("-fx-text-fill: red;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
    }

    /**
     * Performs a search using the current query text.
     */
    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        if (query == null || query.isBlank()) {
            loadTests();
            return;
        }
        try {
            ObservableList<TestDefinition> tests = FXCollections
                    .observableArrayList(testDefinitionService.searchTests(query));
            testTable.setItems(tests);
            statusLabel.setText("Found " + tests.size() + " matches.");
        } catch (Exception e) {
            statusLabel.setText("Error searching: " + e.getMessage());
        }
    }

    /**
     * Clears filters and reloads all tests.
     */
    @FXML
    private void handleRefresh() {
        searchField.clear();
        loadTests();
    }

    /**
     * Opens the new-test dialog.
     */
    @FXML
    private void handleNewTest() {
        showTestDialog(new TestDefinition());
    }

    /**
     * Closes the window hosting the controller.
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) testTable.getScene().getWindow();
        stage.close();
    }

    /**
     * Opens the edit dialog for an existing test.
     */
    private void handleEdit(TestDefinition test) {
        showTestDialog(test);
    }

    /**
     * Deletes a test after user confirmation.
     */
    private void handleDelete(TestDefinition test) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Test");
        alert.setHeaderText("Delete " + test.getTestName() + "?");
        alert.setContentText("Are you sure? This cannot be undone.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                testDefinitionService.deleteById(test.getId());
                loadTests();
                statusLabel.setText("Deleted test: " + test.getTestName());
            } catch (Exception e) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setContentText("Could not delete test: " + e.getMessage());
                error.show();
            }
        }
    }

    /**
     * Shows the create/edit dialog and persists the result.
     */
    private void showTestDialog(TestDefinition test) {
        Dialog<TestDefinition> dialog = new Dialog<>();
        dialog.setTitle(test.getId() == null ? "New Test" : "Edit Test");
        dialog.setHeaderText(null);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField name = new TextField(test.getTestName());
        TextField code = new TextField(test.getShortCode());
        ComboBox<com.qdc.lims.entity.Department> deptCombo = new ComboBox<>();
        deptCombo.setItems(FXCollections.observableArrayList(testDefinitionService.findAllDepartments()));
        deptCombo.setValue(test.getDepartment());
        TextField price = new TextField(test.getPrice() != null ? test.getPrice().toString() : "");

        grid.add(new Label("Test Name:"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("Short Code:"), 0, 1);
        grid.add(code, 1, 1);
        grid.add(new Label("Department:"), 0, 2);
        grid.add(deptCombo, 1, 2);
        grid.add(new Label("Price:"), 0, 3);
        grid.add(price, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                test.setTestName(name.getText());
                test.setShortCode(code.getText());
                test.setDepartment(deptCombo.getValue());
                try {
                    if (!price.getText().isEmpty()) {
                        test.setPrice(new java.math.BigDecimal(price.getText()));
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid price input and leave the prior value.
                }
                return test;
            }
            return null;
        });

        Optional<TestDefinition> result = dialog.showAndWait();

        result.ifPresent(t -> {
            try {
                testDefinitionService.save(t);
                loadTests();
                statusLabel.setText("Saved test: " + t.getTestName());
            } catch (Exception e) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setContentText("Could not save test: " + e.getMessage());
                error.show();
            }
        });
    }
}
