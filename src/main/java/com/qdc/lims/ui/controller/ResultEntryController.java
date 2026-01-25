package com.qdc.lims.ui.controller;

import com.qdc.lims.ui.SessionManager;
import com.qdc.lims.entity.LabOrder;
import com.qdc.lims.entity.LabResult;
import com.qdc.lims.repository.LabOrderRepository;
import com.qdc.lims.repository.LabResultRepository;
import com.qdc.lims.service.LocaleFormatService;
import com.qdc.lims.service.ResultService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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

    @FXML
    private Button saveButton;

    private final LabOrderRepository orderRepository;
    private final LabResultRepository resultRepository;
    private final ResultService resultService;
    private final LocaleFormatService localeFormatService;
    private LabOrder currentOrder;

    public ResultEntryController(LabOrderRepository orderRepository,
            LabResultRepository resultRepository,
            ResultService resultService,
            LocaleFormatService localeFormatService) {
        this.orderRepository = orderRepository;
        this.resultRepository = resultRepository;
        this.resultService = resultService;
        this.localeFormatService = localeFormatService;
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
        resultsTable.getSelectionModel().setCellSelectionEnabled(true);
        resultsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        testNameColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getTestDefinition().getTestName()));

        // 1. RESULT VALUE COLUMN (With Navigation Fix)
        resultValueColumn.setCellValueFactory(cellData -> {
            LabResult result = cellData.getValue();
            String value = result.getResultValue() != null ? result.getResultValue() : "";
            return new SimpleStringProperty(value);
        });

        resultValueColumn.setCellFactory(column -> new TableCell<LabResult, String>() {
            private TextField textField;

            @Override
            public void startEdit() {
                if (!isEmpty()) {
                    super.startEdit();
                    createTextField();
                    setText(null);
                    setGraphic(textField);
                    textField.selectAll();
                    // Request focus for the text field immediately
                    Platform.runLater(textField::requestFocus);
                }
            }

            @Override
            public void cancelEdit() {
                // Smart Cancel: If text changed, save it instead of reverting
                if (textField != null && !textField.getText().equals(getItem())) {
                    commitEdit(textField.getText());
                } else {
                    super.cancelEdit();
                    setText(getItem());
                    setGraphic(null);
                }
            }

            @Override
            public void updateItem(String item, boolean empty) {
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

                // --- NAVIGATION LOGIC ---
                textField.setOnKeyPressed(event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.ENTER
                            || event.getCode() == javafx.scene.input.KeyCode.TAB) {
                        commitEdit(textField.getText());
                        event.consume();

                        int delta = event.isShiftDown() ? -1 : 1;
                        navigateToRow(delta);
                    } else if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        super.cancelEdit();
                    }
                });

                // Focus Listener: Backup Save
                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused && isEditing()) {
                        commitEdit(textField.getText());
                    }
                });
            }

            private String getString() {
                return getItem() == null ? "" : getItem();
            }

            private void navigateToRow(int delta) {
                int currentRow = getTableRow().getIndex();
                int targetRow = currentRow + delta;
                if (targetRow < 0 || targetRow >= getTableView().getItems().size()) {
                    return;
                }

                Platform.runLater(() -> {
                    getTableView().requestFocus();
                    getTableView().getSelectionModel().clearAndSelect(targetRow, getTableColumn());
                    getTableView().scrollTo(targetRow);
                    getTableView().getFocusModel().focus(targetRow, getTableColumn());
                    Platform.runLater(() -> getTableView().edit(targetRow, getTableColumn()));
                });
            }
        });

        // 2. COMMIT HANDLER (Pure Data Update - NO UI REFRESH HERE)
        resultValueColumn.setOnEditCommit(event -> {
            LabResult result = event.getRowValue();
            String newValue = event.getNewValue();

            // Update Data
            result.setResultValue(newValue);
            autoCalculateStatus(result);

            // --- FIX: VISUAL UPDATE TRICK ---
            // Instead of refreshing the row (which kills the cursor),
            // we force JUST the Status column to redraw by toggling visibility.
            // This is instant and doesn't break focus.
            Platform.runLater(() -> {
                statusColumn.setVisible(false);
                statusColumn.setVisible(true);
            });
        });

        // 3. UNIT COLUMN
        unitColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getTestDefinition().getUnit() != null
                        ? cellData.getValue().getTestDefinition().getUnit()
                        : ""));

        // 4. REFERENCE RANGE COLUMN
        referenceRangeColumn.setCellValueFactory(cellData -> {
            var test = cellData.getValue().getTestDefinition();
            if (test.getMinRange() != null && test.getMaxRange() != null) {
                return new SimpleStringProperty(test.getMinRange() + " - " + test.getMaxRange());
            }
            return new SimpleStringProperty("N/A");
        });

        // 5. STATUS COLUMN
        statusColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().isAbnormal()) {
                return new SimpleStringProperty(cellData.getValue().getRemarks());
            }
            return new SimpleStringProperty("Normal");
        });

        statusColumn.setCellFactory(column -> new TableCell<LabResult, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                // Always clear style first
                setText(null);
                setStyle("");

                if (empty || item == null) {
                    return;
                }

                setText(item);
                if ("HIGH".equals(item) || "LOW".equals(item)) {
                    setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                } else if ("Normal".equals(item)) {
                    setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
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

        // Check if this is editing a completed order
        boolean isEditMode = "COMPLETED".equals(currentOrder.getStatus());

        // Set order info with appropriate label
        if (isEditMode) {
            orderInfoLabel.setText("Order #" + currentOrder.getId() + " - EDITING COMPLETED RESULTS");
            // Change save button text for edit mode
            if (saveButton != null) {
                saveButton.setText("Save Corrections");
            }
        } else {
            orderInfoLabel.setText("Order #" + currentOrder.getId() + " - Status: " + currentOrder.getStatus());
            if (saveButton != null) {
                saveButton.setText("Save Results");
            }
        }

        // Set patient info
        mrnLabel.setText(currentOrder.getPatient().getMrn());
        nameLabel.setText(currentOrder.getPatient().getFullName());
        ageGenderLabel.setText(currentOrder.getPatient().getAge() + " / " + currentOrder.getPatient().getGender());

        orderDateLabel.setText(localeFormatService.formatDateTime(currentOrder.getOrderDate()));

        // Load results
        resultsTable.setItems(FXCollections.observableArrayList(currentOrder.getResults()));

        // Auto-focus the first result cell for immediate data entry
        Platform.runLater(() -> {
            if (!resultsTable.getItems().isEmpty()) {
                resultsTable.getSelectionModel().select(0);
                resultsTable.getFocusModel().focus(0, resultValueColumn);
                resultsTable.edit(0, resultValueColumn);
            }
        });
    }

    private void autoCalculateStatus(LabResult result) {
        String value = result.getResultValue();
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        try {
            java.math.BigDecimal numValue = new java.math.BigDecimal(value.trim());
            var test = result.getTestDefinition();

            if (test.getMinRange() != null && test.getMaxRange() != null) {
                if (numValue.compareTo(test.getMinRange()) < 0) {
                    result.setAbnormal(true);
                    result.setRemarks("LOW");
                } else if (numValue.compareTo(test.getMaxRange()) > 0) {
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

            // Check how many results are entered
            int enteredCount = 0;
            int totalCount = resultsTable.getItems().size();
            for (LabResult result : resultsTable.getItems()) {
                if (result.getResultValue() != null && !result.getResultValue().trim().isEmpty()) {
                    enteredCount++;
                }
            }

            if (enteredCount == 0) {
                showError("No results to save. Please enter at least one result value.");
                return;
            }

            // Check if this is an edit of a completed order (correction scenario)
            boolean isEditingCompletedOrder = "COMPLETED".equals(currentOrder.getStatus());

            // Save all results with values
            // If editing a completed order, just save and show success - no status change
            // needed
            if (isEditingCompletedOrder) {
                String editReason = null;
                if (currentOrder.isReportDelivered()) {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Edit Delivered Report");
                    dialog.setHeaderText("Report was already delivered.");
                    dialog.setContentText("Enter reason for edit:");
                    editReason = dialog.showAndWait().orElse("").trim();
                    if (editReason.isEmpty()) {
                        showError("Edit reason is required for delivered reports.");
                        return;
                    }
                }

                currentOrder.setResults(new ArrayList<>(resultsTable.getItems()));
                try {
                    resultService.saveEditedResults(currentOrder, editReason);
                } catch (ObjectOptimisticLockingFailureException e) {
                    showError("This order was updated by another user. Please refresh and try again.");
                    return;
                }

                if (currentOrder.isReportDelivered()) {
                    showSuccess("Results updated. Reprint required for Reception.");
                } else {
                    showSuccess("Results corrected and saved successfully!");
                }

                // Close window after a short delay
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> handleClose());
                        timer.cancel();
                    }
                }, 1500);
                return;
            }

            // Save all results with values (pending orders)
            for (LabResult result : resultsTable.getItems()) {
                if (result.getResultValue() != null && !result.getResultValue().trim().isEmpty()) {
                    result.setPerformedBy(currentUser);
                    result.setPerformedAt(LocalDateTime.now());
                    try {
                        resultRepository.save(result);
                    } catch (ObjectOptimisticLockingFailureException e) {
                        showError("A result was updated by another user. Please refresh and try again.");
                        return;
                    }
                }
            }

            // For pending orders: Check if all results are entered - if so, auto-complete
            // If not all entered, ask user if they want to mark as completed anyway
            boolean shouldComplete = false;

            if (enteredCount == totalCount) {
                // All results entered - auto-complete
                shouldComplete = true;
            } else {
                // Not all results entered - ask user
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Incomplete Results");
                alert.setHeaderText("Not all results have been entered");
                alert.setContentText("Only " + enteredCount + " of " + totalCount +
                        " tests have results. Do you want to mark this order as completed anyway?");

                ButtonType completeBtn = new ButtonType("Mark Completed", ButtonBar.ButtonData.YES);
                ButtonType saveOnlyBtn = new ButtonType("Save Only", ButtonBar.ButtonData.NO);
                alert.getButtonTypes().setAll(completeBtn, saveOnlyBtn);

                ButtonType response = alert.showAndWait().orElse(saveOnlyBtn);
                shouldComplete = (response == completeBtn);
            }

            if (shouldComplete) {
                // Mark order as completed using ResultService for proper transaction handling
                System.out.println("[ResultEntryController] Auto-completing order after save");
                currentOrder.setResults(new ArrayList<>(resultsTable.getItems()));
                resultService.saveResultsFromForm(currentOrder);

                showSuccess("Results saved and Order #" + currentOrder.getId() + " marked as COMPLETED!");

                // Close window after a short delay
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> handleClose());
                        timer.cancel();
                    }
                }, 1500);
            } else {
                showSuccess("Saved " + enteredCount + " result(s). Order remains pending.");
                loadOrderData();
            }

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
