package com.qdc.lims.desktop.controller;

import com.qdc.lims.desktop.SessionManager;
import com.qdc.lims.desktop.navigation.DashboardSwitchService;
import com.qdc.lims.desktop.navigation.DashboardType;
import com.qdc.lims.entity.LabOrder;
import com.qdc.lims.entity.LabResult;
import com.qdc.lims.entity.Patient;
import com.qdc.lims.repository.LabOrderRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for the Reception Dashboard.
 * Handles patient registration, order creation, and report delivery workflow.
 * 
 * Key responsibilities:
 * - Patient registration and order creation navigation
 * - Display of orders ready for pickup vs pending in lab
 * - Report delivery with payment enforcement
 * - Receipt reprinting
 */
@Component("receptionDashboardController")
public class ReceptionDashboardController {

    private final ApplicationContext applicationContext;
    private final LabOrderRepository labOrderRepository;
    private final DashboardSwitchService dashboardSwitchService;
    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    // FXML Components - Header
    @FXML
    private BorderPane mainContainer;
    @FXML
    private Label userLabel;
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label dateTimeLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ComboBox<String> dashboardSwitcher;

    // FXML Components - Quick Actions Panel
    @FXML
    private Label readyCountLabel;
    @FXML
    private Label pendingCountLabel;
    @FXML
    private VBox readyPanel;
    @FXML
    private VBox pendingPanel;

    // FXML Components - Orders Table
    @FXML
    private TabPane ordersTabPane;
    @FXML
    private TextField searchField;

    // Ready Orders Table
    @FXML
    private TableView<LabOrder> readyOrdersTable;
    @FXML
    private TableColumn<LabOrder, String> readyOrderIdCol;
    @FXML
    private TableColumn<LabOrder, String> readyMrnCol;
    @FXML
    private TableColumn<LabOrder, String> readyPatientCol;
    @FXML
    private TableColumn<LabOrder, String> readyDateCol;
    @FXML
    private TableColumn<LabOrder, String> readyBalanceCol;
    @FXML
    private TableColumn<LabOrder, Void> readyActionCol;

    // Pending Orders Table
    @FXML
    private TableView<LabOrder> pendingOrdersTable;
    @FXML
    private TableColumn<LabOrder, String> pendingOrderIdCol;
    @FXML
    private TableColumn<LabOrder, String> pendingMrnCol;
    @FXML
    private TableColumn<LabOrder, String> pendingPatientCol;
    @FXML
    private TableColumn<LabOrder, String> pendingDateCol;
    @FXML
    private TableColumn<LabOrder, String> pendingStatusCol;

    // Data
    private ObservableList<LabOrder> readyOrders = FXCollections.observableArrayList();
    private ObservableList<LabOrder> pendingOrders = FXCollections.observableArrayList();

    public ReceptionDashboardController(ApplicationContext applicationContext,
            LabOrderRepository labOrderRepository,
            DashboardSwitchService dashboardSwitchService) {
        this.applicationContext = applicationContext;
        this.labOrderRepository = labOrderRepository;
        this.dashboardSwitchService = dashboardSwitchService;
    }

    @FXML
    public void initialize() {
        // Setup user info
        if (SessionManager.getCurrentUser() != null) {
            String fullName = SessionManager.getCurrentUser().getFullName();
            welcomeLabel.setText("Welcome, " + fullName);
            userLabel.setText(fullName);
        }

        // Setup dashboard switcher
        dashboardSwitchService.setupDashboardSwitcher(dashboardSwitcher, DashboardType.RECEPTION);

        // Start clock
        startClock();

        // Setup tables
        setupReadyOrdersTable();
        setupPendingOrdersTable();

        // Load data (counts only, tables hidden initially)
        loadOrders();

        // Hide the TabPane initially - only show when Ready or Pending button is
        // clicked
        ordersTabPane.setVisible(false);
        ordersTabPane.setManaged(false);

        // Setup double-click handlers for tables
        readyOrdersTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && readyOrdersTable.getSelectionModel().getSelectedItem() != null) {
                handleDeliverReport();
            }
        });
    }

    private void startClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, MMM dd yyyy - HH:mm");
        Thread clockThread = new Thread(() -> {
            while (true) {
                try {
                    String time = LocalDateTime.now().format(formatter);
                    Platform.runLater(() -> {
                        if (dateTimeLabel != null) {
                            dateTimeLabel.setText(time);
                        }
                    });
                    Thread.sleep(60000); // Update every minute
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        clockThread.setDaemon(true);
        clockThread.start();

        // Initial time
        dateTimeLabel.setText(LocalDateTime.now().format(formatter));
    }

    private void setupReadyOrdersTable() {
        readyOrderIdCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));

        readyMrnCol.setCellValueFactory(data -> {
            Patient p = data.getValue().getPatient();
            return new SimpleStringProperty(p != null ? p.getMrn() : "-");
        });

        readyPatientCol.setCellValueFactory(data -> {
            Patient p = data.getValue().getPatient();
            return new SimpleStringProperty(p != null ? p.getFullName() : "-");
        });

        readyDateCol.setCellValueFactory(data -> {
            LocalDateTime dt = data.getValue().getOrderDate();
            return new SimpleStringProperty(
                    dt != null ? dt.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")) : "-");
        });

        readyBalanceCol.setCellValueFactory(data -> {
            Double balance = data.getValue().getBalanceDue();
            if (balance == null || balance <= 0) {
                return new SimpleStringProperty("PAID");
            }
            return new SimpleStringProperty("Rs. " + df.format(balance));
        });

        // Style the balance column - red if unpaid
        readyBalanceCol.setCellFactory(col -> new TableCell<LabOrder, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("PAID")) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Action column with Deliver button
        readyActionCol.setCellFactory(col -> new TableCell<LabOrder, Void>() {
            private final Button deliverBtn = new Button("Deliver");
            {
                deliverBtn.setStyle(
                        "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 3 10;");
                deliverBtn.setOnAction(e -> {
                    LabOrder order = getTableView().getItems().get(getIndex());
                    deliverReport(order);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deliverBtn);
            }
        });

        readyOrdersTable.setItems(readyOrders);
    }

    private void setupPendingOrdersTable() {
        pendingOrderIdCol
                .setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));

        pendingMrnCol.setCellValueFactory(data -> {
            Patient p = data.getValue().getPatient();
            return new SimpleStringProperty(p != null ? p.getMrn() : "-");
        });

        pendingPatientCol.setCellValueFactory(data -> {
            Patient p = data.getValue().getPatient();
            return new SimpleStringProperty(p != null ? p.getFullName() : "-");
        });

        pendingDateCol.setCellValueFactory(data -> {
            LocalDateTime dt = data.getValue().getOrderDate();
            return new SimpleStringProperty(
                    dt != null ? dt.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")) : "-");
        });

        pendingStatusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        pendingOrdersTable.setItems(pendingOrders);
    }

    private void loadOrders() {
        try {
            // Get orders from last 30 days
            LocalDateTime startDate = LocalDateTime.now().minusDays(30);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);

            List<LabOrder> allOrders = labOrderRepository.findByOrderDateBetween(startDate, endDate);

            // Ready orders: COMPLETED status and not delivered
            List<LabOrder> ready = allOrders.stream()
                    .filter(o -> "COMPLETED".equals(o.getStatus()) && !o.isReportDelivered())
                    .collect(Collectors.toList());

            // Pending orders: Not completed yet
            List<LabOrder> pending = allOrders.stream()
                    .filter(o -> !"COMPLETED".equals(o.getStatus()) && !"CANCELLED".equals(o.getStatus()))
                    .collect(Collectors.toList());

            readyOrders.setAll(ready);
            pendingOrders.setAll(pending);

            // Update counts
            readyCountLabel.setText(String.valueOf(ready.size()));
            pendingCountLabel.setText(String.valueOf(pending.size()));

            statusLabel
                    .setText("Last refreshed: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        } catch (Exception e) {
            showError("Failed to load orders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== Navigation Handlers ==========

    @FXML
    private void handleRegisterPatient() {
        openWindow("/fxml/patient_registration.fxml", "Patient Registration", 600, 500);
    }

    @FXML
    private void handleCreateOrder() {
        openWindow("/fxml/create_order.fxml", "Create Lab Order", 1000, 750);
    }

    @FXML
    private void handleSearchOrder() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Search Order");
        dialog.setHeaderText("Search by MRN");
        dialog.setContentText("Enter MRN:");

        dialog.showAndWait().ifPresent(mrn -> {
            if (!mrn.trim().isEmpty()) {
                searchField.setText(mrn.trim());
                handleSearchInTable();
            }
        });
    }

    @FXML
    private void handleShowReadyOrders() {
        // Show the TabPane and select Ready tab
        ordersTabPane.setVisible(true);
        ordersTabPane.setManaged(true);
        ordersTabPane.getSelectionModel().select(0);
    }

    @FXML
    private void handleShowPendingOrders() {
        // Show the TabPane and select Pending tab
        ordersTabPane.setVisible(true);
        ordersTabPane.setManaged(true);
        ordersTabPane.getSelectionModel().select(1);
    }

    @FXML
    private void handleSearchInTable() {
        String searchTerm = searchField.getText().trim().toLowerCase();

        if (searchTerm.isEmpty()) {
            loadOrders();
            return;
        }

        // Filter ready orders
        List<LabOrder> filteredReady = readyOrders.stream()
                .filter(o -> matchesSearch(o, searchTerm))
                .collect(Collectors.toList());

        // Filter pending orders
        List<LabOrder> filteredPending = pendingOrders.stream()
                .filter(o -> matchesSearch(o, searchTerm))
                .collect(Collectors.toList());

        readyOrdersTable.setItems(FXCollections.observableArrayList(filteredReady));
        pendingOrdersTable.setItems(FXCollections.observableArrayList(filteredPending));
    }

    private boolean matchesSearch(LabOrder order, String searchTerm) {
        if (order.getPatient() != null) {
            if (order.getPatient().getMrn() != null &&
                    order.getPatient().getMrn().toLowerCase().contains(searchTerm)) {
                return true;
            }
            if (order.getPatient().getFullName() != null &&
                    order.getPatient().getFullName().toLowerCase().contains(searchTerm)) {
                return true;
            }
        }
        return String.valueOf(order.getId()).contains(searchTerm);
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        loadOrders();
    }

    // ========== Report Delivery ==========

    @FXML
    private void handleDeliverReport() {
        LabOrder selectedOrder = readyOrdersTable.getSelectionModel().getSelectedItem();
        if (selectedOrder == null) {
            showAlert("Selection Required", "Please select an order from the Ready for Pickup table.");
            return;
        }
        deliverReport(selectedOrder);
    }

    private void deliverReport(LabOrder order) {
        // Check if balance is due
        Double balance = order.getBalanceDue();
        if (balance != null && balance > 0) {
            // Show payment dialog first
            boolean paid = showPaymentDialog(order);
            if (!paid) {
                return; // User cancelled or payment not completed
            }
            // Reload order to get updated balance
            order = labOrderRepository.findById(order.getId()).orElse(order);
        }

        // Show report delivery dialog
        showReportDeliveryDialog(order);
    }

    private boolean showPaymentDialog(LabOrder order) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Payment Required");
        dialog.setHeaderText("Outstanding Balance: Rs. " + df.format(order.getBalanceDue()));

        // Create content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        content.getChildren().add(new Label("Patient: " + order.getPatient().getFullName()));
        content.getChildren().add(new Label("Order #: " + order.getId()));
        content.getChildren().add(new Separator());

        Label amountLabel = new Label("Amount to Collect: Rs. " + df.format(order.getBalanceDue()));
        amountLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        content.getChildren().add(amountLabel);

        TextField paymentField = new TextField(df.format(order.getBalanceDue()));
        paymentField.setPromptText("Enter amount received");

        HBox paymentRow = new HBox(10);
        paymentRow.setAlignment(Pos.CENTER_LEFT);
        paymentRow.getChildren().addAll(new Label("Payment Received: Rs."), paymentField);
        content.getChildren().add(paymentRow);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                double payment = Double.parseDouble(paymentField.getText().replace(",", ""));
                if (payment > 0) {
                    // Update order payment
                    Double currentPaid = order.getPaidAmount() != null ? order.getPaidAmount() : 0;
                    order.setPaidAmount(currentPaid + payment);
                    order.calculateBalance();
                    labOrderRepository.save(order);

                    showAlert("Payment Recorded", "Payment of Rs. " + df.format(payment) + " has been recorded.");
                    return true;
                }
            } catch (NumberFormatException e) {
                showError("Invalid payment amount");
            }
        }
        return false;
    }

    private void showReportDeliveryDialog(LabOrder order) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Report Delivery");
        dialog.setHeaderText("Deliver Report for Order #" + order.getId());
        dialog.initModality(Modality.APPLICATION_MODAL);

        // Create report content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);

        // Patient info
        Patient patient = order.getPatient();
        content.getChildren().add(new Label("Patient: " + patient.getFullName()));
        content.getChildren().add(new Label("MRN: " + patient.getMrn()));
        content.getChildren().add(new Label("Age/Gender: " + patient.getAge() + " / " + patient.getGender()));
        content.getChildren().add(new Separator());

        // Test results
        content.getChildren().add(new Label("Test Results:"));

        if (order.getResults() != null && !order.getResults().isEmpty()) {
            for (LabResult result : order.getResults()) {
                String testName = result.getTestDefinition() != null ? result.getTestDefinition().getTestName()
                        : "Unknown Test";
                String value = result.getResultValue() != null ? result.getResultValue() : "Pending";
                String abnormal = result.isAbnormal() ? " (ABNORMAL)" : "";

                Label resultLabel = new Label("  - " + testName + ": " + value + abnormal);
                if (result.isAbnormal()) {
                    resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
                content.getChildren().add(resultLabel);
            }
        } else {
            content.getChildren().add(new Label("  No results entered yet."));
        }

        content.getChildren().add(new Separator());

        // Payment status
        String paymentStatus = (order.getBalanceDue() == null || order.getBalanceDue() <= 0) ? "FULLY PAID"
                : "Balance Due: Rs. " + df.format(order.getBalanceDue());
        Label paymentLabel = new Label("Payment Status: " + paymentStatus);
        paymentLabel.setStyle((order.getBalanceDue() == null || order.getBalanceDue() <= 0)
                ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;"
                : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        content.getChildren().add(paymentLabel);

        dialog.getDialogPane().setContent(content);

        // Buttons
        ButtonType printAndDeliverBtn = new ButtonType("Print & Mark Delivered", ButtonBar.ButtonData.OK_DONE);
        ButtonType markDeliveredBtn = new ButtonType("Mark Delivered Only", ButtonBar.ButtonData.APPLY);
        dialog.getDialogPane().getButtonTypes().addAll(printAndDeliverBtn, markDeliveredBtn, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent()) {
            if (result.get() == printAndDeliverBtn) {
                printReport(order);
                markAsDelivered(order);
            } else if (result.get() == markDeliveredBtn) {
                markAsDelivered(order);
            }
        }
    }

    private void markAsDelivered(LabOrder order) {
        try {
            order.setReportDelivered(true);
            order.setDeliveryDate(LocalDateTime.now());
            labOrderRepository.save(order);

            showAlert("Report Delivered", "Report for Order #" + order.getId() + " has been marked as delivered.");
            loadOrders(); // Refresh tables
        } catch (Exception e) {
            showError("Failed to mark as delivered: " + e.getMessage());
        }
    }

    private void printReport(LabOrder order) {
        // Create printable report content
        TextFlow reportContent = createReportContent(order);

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(mainContainer.getScene().getWindow())) {
            boolean success = job.printPage(reportContent);
            if (success) {
                job.endJob();
            } else {
                showError("Failed to print report");
            }
        }
    }

    private TextFlow createReportContent(LabOrder order) {
        TextFlow flow = new TextFlow();
        Patient patient = order.getPatient();

        Text header = new Text("QDC LABORATORY - TEST REPORT\n\n");
        header.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Text patientInfo = new Text(
                "Patient: " + patient.getFullName() + "\n" +
                        "MRN: " + patient.getMrn() + "\n" +
                        "Age/Gender: " + patient.getAge() + " / " + patient.getGender() + "\n" +
                        "Order Date: " + order.getOrderDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"))
                        + "\n\n");

        StringBuilder results = new StringBuilder("TEST RESULTS\n" + "-".repeat(40) + "\n");
        if (order.getResults() != null) {
            for (LabResult result : order.getResults()) {
                String testName = result.getTestDefinition() != null ? result.getTestDefinition().getTestName()
                        : "Test";
                String value = result.getResultValue() != null ? result.getResultValue() : "-";
                String flag = result.isAbnormal() ? " *ABNORMAL*" : "";
                results.append(testName).append(": ").append(value).append(flag).append("\n");
            }
        }
        results.append("-".repeat(40)).append("\n\n");

        Text resultsText = new Text(results.toString());

        Text footer = new Text(
                "Report Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"))
                        + "\n" +
                        "Thank you for choosing QDC Laboratory");
        footer.setStyle("-fx-font-size: 10;");

        flow.getChildren().addAll(header, patientInfo, resultsText, footer);
        return flow;
    }

    // ========== Receipt Reprinting ==========

    @FXML
    private void handleReprintReceipt() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reprint Receipt");
        dialog.setHeaderText("Enter Order Number");
        dialog.setContentText("Order #:");

        dialog.showAndWait().ifPresent(orderIdStr -> {
            try {
                Long orderId = Long.parseLong(orderIdStr.trim());
                Optional<LabOrder> orderOpt = labOrderRepository.findById(orderId);

                if (orderOpt.isPresent()) {
                    printReceipt(orderOpt.get());
                } else {
                    showError("Order #" + orderId + " not found");
                }
            } catch (NumberFormatException e) {
                showError("Invalid order number");
            }
        });
    }

    private void printReceipt(LabOrder order) {
        TextFlow receiptContent = createReceiptContent(order);

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(mainContainer.getScene().getWindow())) {
            boolean success = job.printPage(receiptContent);
            if (success) {
                job.endJob();
            } else {
                showError("Failed to print receipt");
            }
        }
    }

    private TextFlow createReceiptContent(LabOrder order) {
        TextFlow flow = new TextFlow();
        Patient patient = order.getPatient();

        Text header = new Text("QDC LABORATORY - RECEIPT\n\n");
        header.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Text orderInfo = new Text(
                "Order #: " + order.getId() + "\n" +
                        "Date: " + order.getOrderDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"))
                        + "\n\n" +
                        "Patient: " + patient.getFullName() + "\n" +
                        "MRN: " + patient.getMrn() + "\n\n");

        StringBuilder tests = new StringBuilder("TESTS ORDERED\n" + "-".repeat(30) + "\n");
        if (order.getResults() != null) {
            for (LabResult result : order.getResults()) {
                if (result.getTestDefinition() != null) {
                    tests.append(result.getTestDefinition().getTestName())
                            .append(" - Rs. ")
                            .append(df.format(result.getTestDefinition().getPrice()))
                            .append("\n");
                }
            }
        }
        tests.append("-".repeat(30)).append("\n\n");

        Text testsText = new Text(tests.toString());

        Double total = order.getTotalAmount() != null ? order.getTotalAmount() : 0;
        Double discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : 0;
        Double paid = order.getPaidAmount() != null ? order.getPaidAmount() : 0;
        Double balance = order.getBalanceDue() != null ? order.getBalanceDue() : 0;

        Text billing = new Text(
                "Total Amount: Rs. " + df.format(total) + "\n" +
                        "Discount: Rs. " + df.format(discount) + "\n" +
                        "Paid: Rs. " + df.format(paid) + "\n" +
                        "Balance Due: Rs. " + df.format(balance) + "\n\n");
        billing.setStyle("-fx-font-weight: bold;");

        Text footer = new Text("Thank you for choosing QDC Laboratory");
        footer.setStyle("-fx-font-size: 10;");

        flow.getChildren().addAll(header, orderInfo, testsText, billing, footer);
        return flow;
    }

    // ========== Dashboard Switching & Logout ==========

    @FXML
    private void handleDashboardSwitch() {
        String selected = dashboardSwitcher.getValue();
        if (selected == null || selected.isEmpty() ||
                selected.equals(DashboardType.RECEPTION.getDisplayName())) {
            return; // Already on Reception dashboard
        }

        Stage stage = (Stage) mainContainer.getScene().getWindow();
        dashboardSwitchService.switchToDashboard(selected, stage);
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Confirm Logout");
        confirm.setContentText("Are you sure you want to logout?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    Stage stage = (Stage) mainContainer.getScene().getWindow();
                    // Clear session
                    SessionManager.logout(stage);

                    // Load main_window (the tabbed interface) instead of old launcher
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_window.fxml"));
                    loader.setControllerFactory(applicationContext::getBean);
                    Parent root = loader.load();

                    stage.setTitle("QDC LIMS");
                    stage.setScene(new Scene(root, 1100, 750));
                    stage.setWidth(1100);
                    stage.setHeight(750);
                    stage.centerOnScreen();
                } catch (Exception e) {
                    showError("Logout failed: " + e.getMessage());
                }
            }
        });
    }

    // ========== Utility Methods ==========

    private void openWindow(String fxmlPath, String title, int width, int height) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, width, height));
            stage.initModality(Modality.NONE);

            // Refresh orders when the window closes
            stage.setOnHidden(e -> loadOrders());

            stage.show();
        } catch (Exception e) {
            showError("Failed to open " + title + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
