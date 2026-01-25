package com.qdc.lims.ui.controller;

import com.qdc.lims.entity.SupplierLedger;
import com.qdc.lims.repository.SupplierLedgerRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SupplierPayablesController {

    @Autowired
    private SupplierLedgerRepository supplierLedgerRepository;

    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private ComboBox<String> supplierFilter;
    @FXML
    private Label totalBillLabel;
    @FXML
    private Label totalPaidLabel;
    @FXML
    private Label totalDueLabel;
    @FXML
    private TableView<SupplierPayableSummary> summaryTable;
    @FXML
    private TableColumn<SupplierPayableSummary, String> supplierCol;
    @FXML
    private TableColumn<SupplierPayableSummary, String> billCol;
    @FXML
    private TableColumn<SupplierPayableSummary, String> paidCol;
    @FXML
    private TableColumn<SupplierPayableSummary, String> dueCol;
    @FXML
    private TableColumn<SupplierPayableSummary, String> latestInvoiceCol;
    @FXML
    private TableColumn<SupplierPayableSummary, String> latestDueCol;
    @FXML
    private Button closeButton;

    @FXML
    public void initialize() {
        startDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        endDatePicker.setValue(LocalDate.now());
        setupTable();
        handleGenerate();
    }

    private void setupTable() {
        supplierCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().supplierName));
        billCol.setCellValueFactory(data -> new SimpleStringProperty(formatAmount(data.getValue().totalBill)));
        paidCol.setCellValueFactory(data -> new SimpleStringProperty(formatAmount(data.getValue().totalPaid)));
        dueCol.setCellValueFactory(data -> new SimpleStringProperty(formatAmount(data.getValue().totalDue)));
        latestInvoiceCol.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().latestInvoiceNumber));
        latestDueCol.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().latestDueDate));
    }

    @FXML
    private void handleGenerate() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start == null || end == null) {
            return;
        }

        List<SupplierLedger> ledgers = supplierLedgerRepository.findByTransactionDateBetween(start, end);

        if (supplierFilter.getItems().isEmpty()) {
            supplierFilter.setItems(FXCollections.observableArrayList(
                    ledgers.stream()
                            .map(l -> l.getSupplier() != null ? l.getSupplier().getCompanyName() : "Unknown")
                            .distinct()
                            .sorted()
                            .collect(Collectors.toList())));
            supplierFilter.getItems().add(0, "All Suppliers");
            supplierFilter.setValue("All Suppliers");
        }

        String selectedSupplier = supplierFilter.getValue();
        if (selectedSupplier != null && !"All Suppliers".equals(selectedSupplier)) {
            ledgers = ledgers.stream()
                    .filter(l -> l.getSupplier() != null && selectedSupplier.equals(l.getSupplier().getCompanyName()))
                    .collect(Collectors.toList());
        }

        Map<String, List<SupplierLedger>> bySupplier = ledgers.stream()
                .collect(Collectors.groupingBy(l -> l.getSupplier() != null ? l.getSupplier().getCompanyName() : "Unknown"));

        ObservableList<SupplierPayableSummary> rows = FXCollections.observableArrayList();
        double totalBill = 0.0;
        double totalPaid = 0.0;
        double totalDue = 0.0;

        for (Map.Entry<String, List<SupplierLedger>> entry : bySupplier.entrySet()) {
            double bill = entry.getValue().stream()
                    .mapToDouble(l -> l.getBillAmount() != null ? l.getBillAmount() : 0.0)
                    .sum();
            double paid = entry.getValue().stream()
                    .mapToDouble(l -> l.getPaidAmount() != null ? l.getPaidAmount() : 0.0)
                    .sum();
            double due = Math.max(0.0, bill - paid);

            SupplierLedger latest = entry.getValue().stream()
                    .sorted((a, b) -> {
                        LocalDate aDate = a.getInvoiceDate() != null ? a.getInvoiceDate() : a.getTransactionDate();
                        LocalDate bDate = b.getInvoiceDate() != null ? b.getInvoiceDate() : b.getTransactionDate();
                        if (aDate == null && bDate == null) {
                            return 0;
                        }
                        if (aDate == null) {
                            return 1;
                        }
                        if (bDate == null) {
                            return -1;
                        }
                        return bDate.compareTo(aDate);
                    })
                    .findFirst()
                    .orElse(null);

            String latestInvoice = latest != null && latest.getInvoiceNumber() != null
                    ? latest.getInvoiceNumber()
                    : "-";
            String latestDue = latest != null && latest.getDueDate() != null
                    ? latest.getDueDate().toString()
                    : "-";

            rows.add(new SupplierPayableSummary(entry.getKey(), bill, paid, due, latestInvoice, latestDue));

            totalBill += bill;
            totalPaid += paid;
            totalDue += due;
        }

        summaryTable.setItems(rows);
        totalBillLabel.setText(formatAmount(totalBill));
        totalPaidLabel.setText(formatAmount(totalPaid));
        totalDueLabel.setText(formatAmount(totalDue));
    }

    @FXML
    private void handleClose() {
        ((Stage) closeButton.getScene().getWindow()).close();
    }

    private String formatAmount(double amount) {
        return String.format("$%.2f", amount);
    }

    public static class SupplierPayableSummary {
        private final String supplierName;
        private final double totalBill;
        private final double totalPaid;
        private final double totalDue;
        private final String latestInvoiceNumber;
        private final String latestDueDate;

        public SupplierPayableSummary(String supplierName, double totalBill, double totalPaid, double totalDue,
                String latestInvoiceNumber, String latestDueDate) {
            this.supplierName = supplierName;
            this.totalBill = totalBill;
            this.totalPaid = totalPaid;
            this.totalDue = totalDue;
            this.latestInvoiceNumber = latestInvoiceNumber;
            this.latestDueDate = latestDueDate;
        }
    }
}
