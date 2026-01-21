package com.qdc.lims.desktop.controller;

import com.qdc.lims.desktop.SessionManager;
import com.qdc.lims.entity.LabOrder;
import com.qdc.lims.entity.LabResult;
import com.qdc.lims.repository.LabOrderRepository;
import com.qdc.lims.repository.LabResultRepository;
import com.qdc.lims.service.ResultService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * JavaFX controller for entering test results.
 */
@Component("resultEntryController")
public class ResultEntryController {

    @FXML
    private Label orderInfoLabel;

    @FXML
    private Label mrnLabel;

    @FXML
    private Label nameLabel;

    @FXML
    private Label ageGenderLabel;

    @FXML
    private Label orderDateLabel;

    @FXML
    private TableView<LabResult> resultsTable;

    @FXML
    private TableColumn<LabResult, String> testNameColumn;

    @FXML
    private TableColumn<LabResult, String> resultValueColumn;

    @FXML
    private TableColumn<LabResult, String> unitColumn;

    @FXML
    private TableColumn<LabResult, String> referenceRangeColumn;

    @FXML
    private TableColumn<LabResult, String> statusColumn;

    @FXML
    private Label messageLabel;

    private final LabOrderRepository orderRepository;
    private final LabResultRepository resultRepository;
    private final ResultService resultService;
    private LabOrder currentOrder;

    public ResultEntryController(LabOrderRepository orderRepository,
            LabResultRepository resultRepository,
            ResultService resultService) {
        this.orderRepository = orderRepository;
        this.resultRepository = resultRepository;
        this.resultService = resultService;
    }

    public void setOrder(LabOrder order) {
        this.currentOrder = order;
        loadOrderData();
    }

    @FXML
    private void initialize() {
        setupResultsTable();
        messageLabel.setText("");
    }

    private void setupResultsTable() {
        resultsTable.setEditable(true);

        testNameColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getTestDefinition().getTestName()));

        // Editable result value column - use custom cell that commits on focus loss
        resultValueColumn.setCellValueFactory(cellData -> {
            LabResult result = cellData.getValue();
            String value = result.getResultValue() != null ? result.getResultValue() : "";
            return new SimpleStringProperty(value);
        });

        // Custom cell factory that commits edits when focus is lost
        resultValueColumn.setCellFactory(column -> new TableCell<LabResult, String>() {
            private TextField textField;

            @Override
            public void startEdit() {
                super.startEdit();
                if (textField == null) {
                    createTextField();
                }
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem());
                setGraphic(null);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        if (textField != null) {
                            textField.setText(getString());
                        }
                        setText(null);
                        setGraphic(textField);
                    } else {
                        setText(getString());
                        setGraphic(null);
                    }
                }
            }

            private void createTextField() {
                textField = new TextField(getString());
                textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);

                // Commit on Enter
                textField.setOnKeyPressed(event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                        commitEdit(textField.getText());
                    } else if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                });

                // Commit on focus loss (KEY FIX!)
                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused && isEditing()) {
                        commitEdit(textField.getText());
                    }
                });
            }

            private String getString() {
                return getItem() == null ? "" : getItem();
            }
        });

        resultValueColumn.setEditable(true);

        resultValueColumn.setOnEditCommit(event -> {
            LabResult result = event.getRowValue();
            String newValue = event.getNewValue();
            result.setResultValue(newValue);
            autoCalculateStatus(result);
            resultsTable.refresh();
        });

        unitColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getTestDefinition().getUnit() != null
                        ? cellData.getValue().getTestDefinition().getUnit()
                        : ""));

        referenceRangeColumn.setCellValueFactory(cellData -> {
            var test = cellData.getValue().getTestDefinition();
            if (test.getMinRange() != null && test.getMaxRange() != null) {
                return new SimpleStringProperty(test.getMinRange() + " - " + test.getMaxRange());
            }
            return new SimpleStringProperty("N/A");
        });

        statusColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().isAbnormal()) {
                return new SimpleStringProperty(cellData.getValue().getRemarks());
            }
            return new SimpleStringProperty("Normal");
        });

        // Color code status
        statusColumn.setCellFactory(column -> new TableCell<LabResult, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("HIGH") || item.equals("LOW")) {
                        setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                    } else if (item.equals("Normal")) {
                        setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void loadOrderData() {
        if (currentOrder == null)
            return;

        // Reload from database to get fresh data
        currentOrder = orderRepository.findById(currentOrder.getId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Set order info
        orderInfoLabel.setText("Order #" + currentOrder.getId() + " - Status: " + currentOrder.getStatus());

        // Set patient info
        mrnLabel.setText(currentOrder.getPatient().getMrn());
        nameLabel.setText(currentOrder.getPatient().getFullName());
        ageGenderLabel.setText(currentOrder.getPatient().getAge() + " / " + currentOrder.getPatient().getGender());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        orderDateLabel.setText(currentOrder.getOrderDate().format(formatter));

        // Load results
        resultsTable.setItems(FXCollections.observableArrayList(currentOrder.getResults()));
    }

    private void autoCalculateStatus(LabResult result) {
        String value = result.getResultValue();
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        try {
            double numValue = Double.parseDouble(value.trim());
            var test = result.getTestDefinition();

            if (test.getMinRange() != null && test.getMaxRange() != null) {
                if (numValue < test.getMinRange()) {
                    result.setAbnormal(true);
                    result.setRemarks("LOW");
                } else if (numValue > test.getMaxRange()) {
                    result.setAbnormal(true);
                    result.setRemarks("HIGH");
                } else {
                    result.setAbnormal(false);
                    result.setRemarks("Normal");
                }
            }
        } catch (NumberFormatException e) {
            // Text result, mark as normal
            result.setAbnormal(false);
            result.setRemarks("");
        }
    }

    @FXML
    private void handleSaveResults() {
        try {
            // Commit any pending table edits
            resultsTable.refresh();

            // Get current user for audit
            String currentUser = SessionManager.getCurrentUser() != null
                    ? SessionManager.getCurrentUser().getUsername()
                    : "UNKNOWN";

            // Save all results with values
            int savedCount = 0;
            for (LabResult result : resultsTable.getItems()) {
                if (result.getResultValue() != null && !result.getResultValue().trim().isEmpty()) {
                    result.setPerformedBy(currentUser);
                    result.setPerformedAt(LocalDateTime.now());
                    resultRepository.save(result);
                    savedCount++;
                }
            }

            if (savedCount > 0) {
                showSuccess("Saved " + savedCount + " result(s) successfully!");
            } else {
                showError("No results to save. Please enter at least one result value.");
            }

            // Reload order to refresh data
            loadOrderData();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to save results: " + e.getMessage());
        }
    }

    @FXML
    private void handleMarkCompleted() {
        // Validate all results are entered
        boolean allEntered = true;
        int enteredCount = 0;
        for (LabResult result : resultsTable.getItems()) {
            if (result.getResultValue() != null && !result.getResultValue().trim().isEmpty()) {
                enteredCount++;
            } else {
                allEntered = false;
            }
        }

        if (enteredCount == 0) {
            showError("No results have been entered. Please enter at least one result before completing.");
            return;
        }

        if (!allEntered) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Incomplete Results");
            alert.setHeaderText("Not all results have been entered");
            alert.setContentText("Only " + enteredCount + " of " + resultsTable.getItems().size() +
                    " tests have results. Do you still want to mark this order as completed?");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }

        try {
            System.out.println("[ResultEntryController] Mark completed - calling ResultService.saveResultsFromForm()");

            // Use the ResultService which has proper transaction handling
            // First, update the order with current results from the form
            currentOrder.setResults(new ArrayList<>(resultsTable.getItems()));
            resultService.saveResultsFromForm(currentOrder);

            System.out.println("[ResultEntryController] Order completed successfully!");
            showSuccess("Order #" + currentOrder.getId() + " marked as COMPLETED!");

            // Close window after a short delay (non-blocking)
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> handleClose());
                    timer.cancel();
                }
            }, 1500);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to complete order: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) resultsTable.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        messageLabel.setText("❌ " + message);
        messageLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
    }

    private void showSuccess(String message) {
        messageLabel.setText("✓ " + message);
        messageLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }
}
