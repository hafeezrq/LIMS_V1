package com.qdc.lims.desktop.controller;

import com.qdc.lims.dto.OrderRequest;
import com.qdc.lims.entity.Doctor;
import com.qdc.lims.entity.LabOrder;
import com.qdc.lims.entity.Patient;
import com.qdc.lims.entity.TestDefinition;
import com.qdc.lims.repository.DoctorRepository;
import com.qdc.lims.repository.PatientRepository;
import com.qdc.lims.repository.TestDefinitionRepository;
import com.qdc.lims.service.OrderService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX controller for creating lab orders.
 */
@Component("createOrderController")
public class CreateOrderController {

    @FXML
    private TextField patientSearchField;

    @FXML
    private VBox patientInfoBox;

    @FXML
    private Label patientNameLabel;

    @FXML
    private Label patientDetailsLabel;

    @FXML
    private ComboBox<Doctor> doctorComboBox;

    @FXML
    private ListView<TestDefinition> testsListView;

    @FXML
    private Label selectedTestsCountLabel;

    @FXML
    private Label totalAmountLabel;

    @FXML
    private TextField discountField;

    @FXML
    private TextField cashPaidField;

    @FXML
    private Label balanceLabel;

    @FXML
    private Label messageLabel;

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TestDefinitionRepository testRepository;
    private final OrderService orderService;

    private Patient selectedPatient;
    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    public CreateOrderController(PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            TestDefinitionRepository testRepository,
            OrderService orderService) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.testRepository = testRepository;
        this.orderService = orderService;
    }

    @FXML
    private void initialize() {
        // Load doctors
        loadDoctors();

        // Load tests with checkboxes
        loadTests();

        // Add listeners for billing calculation
        discountField.textProperty().addListener((obs, old, newVal) -> calculateBalance());
        cashPaidField.textProperty().addListener((obs, old, newVal) -> calculateBalance());

        messageLabel.setText("");
    }

    /**
     * Pre-select a patient (used when coming from registration)
     */
    public void setPreselectedPatient(Patient patient) {
        if (patient != null) {
            selectedPatient = patient;
            patientSearchField.setText(patient.getMrn());

            // Display patient info
            patientNameLabel.setText(patient.getFullName() + " (MRN: " + patient.getMrn() + ")");
            patientDetailsLabel.setText(
                    "Age: " + patient.getAge() + " | " +
                            "Gender: " + patient.getGender() + " | " +
                            "Mobile: " + (patient.getMobileNumber() != null ? patient.getMobileNumber() : "N/A") + " | "
                            +
                            "City: " + (patient.getCity() != null ? patient.getCity() : "N/A"));

            patientInfoBox.setVisible(true);
            patientInfoBox.setManaged(true);
        }
    }

    private void loadDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();

        // Add a "None" option
        Doctor noneDoctor = new Doctor();
        noneDoctor.setId(null);
        noneDoctor.setName("-- None / Self --");

        ObservableList<Doctor> doctorList = FXCollections.observableArrayList(noneDoctor);
        doctorList.addAll(doctors);

        doctorComboBox.setItems(doctorList);
        doctorComboBox.setValue(noneDoctor);

        // Custom display - only show doctor name (commission is confidential)
        doctorComboBox.setConverter(new StringConverter<Doctor>() {
            @Override
            public String toString(Doctor doctor) {
                if (doctor == null)
                    return "";
                return doctor.getName();
            }

            @Override
            public Doctor fromString(String string) {
                return null;
            }
        });
    }

    private void loadTests() {
        List<TestDefinition> tests = testRepository.findAll();
        ObservableList<TestDefinition> observableTests = FXCollections.observableArrayList(tests);

        testsListView.setItems(observableTests);
        testsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Format list cells
        testsListView.setCellFactory(lv -> new ListCell<TestDefinition>() {
            @Override
            protected void updateItem(TestDefinition item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTestName() + " - Rs. " + df.format(item.getPrice()));
                }
            }
        });

        // Selection listener
        testsListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            updateTotalAmount();
        });
    }

    @FXML
    private void handleSearchPatient() {
        String searchTerm = patientSearchField.getText().trim();

        if (searchTerm.isEmpty()) {
            showError("Please enter patient MRN or name");
            return;
        }

        Patient patient = null;

        // Try to find by MRN first (exact match)
        if (searchTerm.contains("-") || searchTerm.length() == 6) {
            patient = patientRepository.findByMrn(searchTerm).orElse(null);
        }

        // If not found by MRN, search by name
        if (patient == null) {
            List<Patient> matchingPatients = patientRepository.findAll().stream()
                    .filter(p -> p.getFullName() != null &&
                            p.getFullName().toLowerCase().contains(searchTerm.toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());

            if (matchingPatients.isEmpty()) {
                showError("No patient found with MRN or name: " + searchTerm);
                patientInfoBox.setVisible(false);
                patientInfoBox.setManaged(false);
                selectedPatient = null;
                return;
            } else if (matchingPatients.size() == 1) {
                patient = matchingPatients.get(0);
            } else {
                // Multiple matches - show in dialog
                patient = showPatientSelectionDialog(matchingPatients);
                if (patient == null) {
                    showError("Please select a patient from the list");
                    return;
                }
            }
        }

        // Display patient info
        selectedPatient = patient;
        patientNameLabel.setText(patient.getFullName() + " (MRN: " + patient.getMrn() + ")");
        patientDetailsLabel.setText(
                "Age: " + patient.getAge() + " | " +
                        "Gender: " + patient.getGender() + " | " +
                        "Mobile: " + (patient.getMobileNumber() != null ? patient.getMobileNumber() : "N/A") + " | " +
                        "City: " + (patient.getCity() != null ? patient.getCity() : "N/A"));

        patientInfoBox.setVisible(true);
        patientInfoBox.setManaged(true);
        messageLabel.setText("");
    }

    private void updateTotalAmount() {
        ObservableList<TestDefinition> selectedTests = testsListView.getSelectionModel().getSelectedItems();

        double total = 0.0;
        for (TestDefinition test : selectedTests) {
            total += test.getPrice();
        }

        selectedTestsCountLabel.setText(String.valueOf(selectedTests.size()));
        totalAmountLabel.setText("Rs. " + df.format(total));

        calculateBalance();
    }

    private void calculateBalance() {
        ObservableList<TestDefinition> selectedTests = testsListView.getSelectionModel().getSelectedItems();

        double total = 0.0;
        for (TestDefinition test : selectedTests) {
            total += test.getPrice();
        }

        double discount = parseDouble(discountField.getText());
        double cashPaid = parseDouble(cashPaidField.getText());

        double balance = total - discount - cashPaid;
        balanceLabel.setText("Rs. " + df.format(balance));

        if (balance < 0) {
            balanceLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        } else if (balance == 0) {
            balanceLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #95a5a6;");
        } else {
            balanceLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        }
    }

    @FXML
    private void handleCreateOrder() {
        messageLabel.setText("");

        // Validation
        if (selectedPatient == null) {
            showError("Please select a patient first");
            return;
        }

        ObservableList<TestDefinition> selectedTests = testsListView.getSelectionModel().getSelectedItems();
        if (selectedTests.isEmpty()) {
            showError("Please select at least one test");
            return;
        }

        // Get doctor (nullable)
        Doctor selectedDoctor = doctorComboBox.getValue();
        Long doctorId = (selectedDoctor != null && selectedDoctor.getId() != null)
                ? selectedDoctor.getId()
                : null;

        // Get test IDs
        List<Long> testIds = new ArrayList<>();
        for (TestDefinition test : selectedTests) {
            testIds.add(test.getId());
        }

        // Create order request
        double discount = parseDouble(discountField.getText());
        double cashPaid = parseDouble(cashPaidField.getText());

        OrderRequest request = new OrderRequest(
                selectedPatient.getId(),
                doctorId,
                testIds,
                discount,
                cashPaid);

        // Create order
        try {
            LabOrder order = orderService.createOrder(request);

            // Show print receipt dialog
            showPrintReceiptDialog(order);

        } catch (Exception e) {
            showError("Failed to create order: " + e.getMessage());
        }
    }

    /**
     * Show print receipt dialog after order creation.
     */
    private void showPrintReceiptDialog(LabOrder order) {
        javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Order Created Successfully!");
        dialog.setHeaderText("Order #" + order.getId() + " created for " + order.getPatient().getFullName());

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(15);
        content.setPadding(new javafx.geometry.Insets(20));
        content.setAlignment(javafx.geometry.Pos.CENTER);

        // Order summary
        javafx.scene.layout.VBox summaryBox = new javafx.scene.layout.VBox(5);
        summaryBox.setStyle(
                "-fx-background-color: #e8f5e9; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #27ae60; -fx-border-radius: 8;");
        summaryBox.getChildren().addAll(
                new Label("Order Created Successfully!"),
                new javafx.scene.control.Separator(),
                new Label("MRN: " + order.getPatient().getMrn()),
                new Label("Tests: " + order.getResults().size()),
                new Label("Total: Rs. " + df.format(order.getTotalAmount())),
                new Label("Discount: Rs. "
                        + df.format(order.getDiscountAmount() != null ? order.getDiscountAmount() : 0)),
                new Label("Paid: Rs. " + df.format(order.getPaidAmount())),
                new Label("Balance Due: Rs. " + df.format(order.getBalanceDue())));
        summaryBox.getChildren().get(0).setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #27ae60;");

        // Print buttons
        Label printLabel = new Label("Print Receipt:");
        printLabel.setStyle("-fx-font-weight: bold;");

        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);

        Button printNormalBtn = new Button("🖨 Normal Print");
        printNormalBtn.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10 15; -fx-cursor: hand;");
        printNormalBtn.setOnAction(e -> {
            printReceipt(order, "NORMAL");
            dialog.close();
            closeCreateOrderWindow();
        });

        Button printThermalBtn = new Button("🧾 Thermal Print");
        printThermalBtn.setStyle(
                "-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10 15; -fx-cursor: hand;");
        printThermalBtn.setOnAction(e -> {
            printReceipt(order, "THERMAL");
            dialog.close();
            closeCreateOrderWindow();
        });

        Button skipBtn = new Button("Skip Print");
        skipBtn.setStyle(
                "-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 10 15; -fx-cursor: hand;");
        skipBtn.setOnAction(e -> {
            dialog.close();
            closeCreateOrderWindow();
        });

        buttonBox.getChildren().addAll(printNormalBtn, printThermalBtn, skipBtn);

        content.getChildren().addAll(summaryBox, printLabel, buttonBox);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Hide the default close button since we have our own buttons
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

        dialog.showAndWait();
    }

    /**
     * Close the Create Order window after successful order creation.
     */
    private void closeCreateOrderWindow() {
        Stage stage = (Stage) patientSearchField.getScene().getWindow();
        stage.close();
    }

    /**
     * Print receipt using system print dialog.
     * Supports both NORMAL (A4/Letter) and THERMAL (58mm/80mm) formats.
     */
    private void printReceipt(LabOrder order, String printerType) {
        // Build receipt content based on printer type
        String receiptText;
        String fontStyle;

        if ("THERMAL".equals(printerType)) {
            receiptText = buildThermalReceipt(order);
            fontStyle = "-fx-font-family: 'Courier New'; -fx-font-size: 9;";
        } else {
            receiptText = buildNormalReceipt(order);
            fontStyle = "-fx-font-family: 'Courier New'; -fx-font-size: 11;";
        }

        // Create printable content
        javafx.scene.text.TextFlow textFlow = new javafx.scene.text.TextFlow();
        javafx.scene.text.Text text = new javafx.scene.text.Text(receiptText);
        text.setStyle(fontStyle);
        textFlow.getChildren().add(text);

        // Set preferred width for thermal (narrow) vs normal (wider)
        if ("THERMAL".equals(printerType)) {
            textFlow.setPrefWidth(200); // ~58-80mm thermal paper width
        }

        // Use system print dialog
        javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(patientSearchField.getScene().getWindow())) {
            boolean success = job.printPage(textFlow);
            if (success) {
                job.endJob();
                showSuccess("Receipt sent to printer");
            } else {
                showError("Failed to print receipt");
            }
        }
    }

    /**
     * Build receipt content for normal printers (A4/Letter size).
     */
    private String buildNormalReceipt(LabOrder order) {
        StringBuilder sb = new StringBuilder();
        sb.append("========== QDC LABORATORY ==========\n");
        sb.append("          RECEIPT / INVOICE\n");
        sb.append("=====================================\n\n");
        sb.append("Receipt #: ").append(order.getId()).append("\n");
        sb.append("Date: ")
                .append(order.getOrderDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")))
                .append("\n\n");
        sb.append("--- PATIENT DETAILS ---\n");
        sb.append("Name: ").append(order.getPatient().getFullName()).append("\n");
        sb.append("MRN: ").append(order.getPatient().getMrn()).append("\n");
        sb.append("Age/Gender: ").append(order.getPatient().getAge()).append(" / ")
                .append(order.getPatient().getGender()).append("\n\n");
        sb.append("--- TESTS ORDERED ---\n");

        for (var result : order.getResults()) {
            sb.append("* ").append(result.getTestDefinition().getTestName());
            sb.append(" - Rs. ").append(df.format(result.getTestDefinition().getPrice())).append("\n");
        }

        sb.append("\n--- BILLING ---\n");
        sb.append("Subtotal:  Rs. ").append(df.format(order.getTotalAmount())).append("\n");
        if (order.getDiscountAmount() != null && order.getDiscountAmount() > 0) {
            sb.append("Discount:  Rs. ").append(df.format(order.getDiscountAmount())).append("\n");
        }
        sb.append("Paid:      Rs. ").append(df.format(order.getPaidAmount())).append("\n");
        sb.append("Balance:   Rs. ").append(df.format(order.getBalanceDue())).append("\n\n");
        sb.append("=====================================\n");
        sb.append("    Thank you for choosing QDC!\n");
        sb.append("    Results typically ready in 24hrs\n");
        sb.append("=====================================\n");

        return sb.toString();
    }

    /**
     * Build receipt content optimized for thermal printers (58mm/80mm paper).
     * Compact format with shorter lines and condensed layout.
     */
    private String buildThermalReceipt(LabOrder order) {
        StringBuilder sb = new StringBuilder();

        // Header - centered, compact
        sb.append("--------------------------------\n");
        sb.append("       QDC LABORATORY\n");
        sb.append("         CASH RECEIPT\n");
        sb.append("--------------------------------\n");
        sb.append("Rcpt#: ").append(order.getId()).append("\n");
        sb.append("Date: ")
                .append(order.getOrderDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")))
                .append("\n");
        sb.append("--------------------------------\n");

        // Patient - abbreviated
        sb.append("Patient: ").append(truncate(order.getPatient().getFullName(), 20)).append("\n");
        sb.append("MRN: ").append(order.getPatient().getMrn()).append("\n");
        sb.append("Age: ").append(order.getPatient().getAge())
                .append(" | ").append(order.getPatient().getGender()).append("\n");
        sb.append("--------------------------------\n");

        // Tests - compact format
        sb.append("TESTS:\n");
        for (var result : order.getResults()) {
            String testName = truncate(result.getTestDefinition().getTestName(), 18);
            String price = df.format(result.getTestDefinition().getPrice());
            sb.append(String.format("%-18s %8s\n", testName, price));
        }
        sb.append("--------------------------------\n");

        // Billing - right-aligned amounts
        sb.append(String.format("%-12s Rs.%10s\n", "Total:", df.format(order.getTotalAmount())));
        if (order.getDiscountAmount() != null && order.getDiscountAmount() > 0) {
            sb.append(String.format("%-12s Rs.%10s\n", "Discount:", df.format(order.getDiscountAmount())));
        }
        sb.append(String.format("%-12s Rs.%10s\n", "Paid:", df.format(order.getPaidAmount())));
        sb.append(String.format("%-12s Rs.%10s\n", "Balance:", df.format(order.getBalanceDue())));
        sb.append("--------------------------------\n");

        // Footer - compact
        sb.append("   Thank you for choosing QDC!\n");
        sb.append("  Results ready in 24 hours\n");
        sb.append("--------------------------------\n");
        sb.append("\n\n"); // Extra space for tear-off

        return sb.toString();
    }

    /**
     * Truncate string to specified length for thermal receipt formatting.
     */
    private String truncate(String str, int maxLength) {
        if (str == null)
            return "";
        return str.length() <= maxLength ? str : str.substring(0, maxLength - 2) + "..";
    }

    @FXML
    private void handleClear() {
        patientSearchField.clear();
        patientInfoBox.setVisible(false);
        patientInfoBox.setManaged(false);
        selectedPatient = null;

        doctorComboBox.getSelectionModel().selectFirst();
        testsListView.getSelectionModel().clearSelection();

        discountField.setText("0");
        cashPaidField.setText("0");

        selectedTestsCountLabel.setText("0");
        totalAmountLabel.setText("Rs. 0.00");
        balanceLabel.setText("Rs. 0.00");

        messageLabel.setText("");
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) patientSearchField.getScene().getWindow();
        stage.close();
    }

    private double parseDouble(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void showError(String message) {
        messageLabel.setText("❌ " + message);
        messageLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
    }

    private void showSuccess(String message) {
        messageLabel.setText("✓ " + message);
        messageLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }

    private Patient showPatientSelectionDialog(List<Patient> patients) {
        // Create ListView for better display
        ListView<Patient> listView = new ListView<>();
        listView.getItems().addAll(patients);
        listView.setCellFactory(lv -> new ListCell<Patient>() {
            @Override
            protected void updateItem(Patient patient, boolean empty) {
                super.updateItem(patient, empty);
                if (empty || patient == null) {
                    setText(null);
                } else {
                    setText(String.format("%s (MRN: %s) - Age: %d, Gender: %s",
                            patient.getFullName(),
                            patient.getMrn(),
                            patient.getAge(),
                            patient.getGender()));
                }
            }
        });

        Dialog<Patient> dialog = new Dialog<>();
        dialog.setTitle("Multiple Patients Found");
        dialog.setHeaderText(patients.size() + " patients match your search. Please select one:");
        dialog.getDialogPane().setContent(listView);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }
}
