package com.qdc.lims.ui.controller;

import com.qdc.lims.entity.CommissionLedger;
import com.qdc.lims.repository.CommissionLedgerRepository;
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
public class DoctorCommissionLedgerController {

    @Autowired
    private CommissionLedgerRepository commissionRepository;

    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private ComboBox<String> doctorFilter;
    @FXML
    private Label totalDueLabel;
    @FXML
    private Label totalPaidLabel;
    @FXML
    private Label totalCommissionLabel;
    @FXML
    private TableView<DoctorCommissionSummary> summaryTable;
    @FXML
    private TableColumn<DoctorCommissionSummary, String> doctorCol;
    @FXML
    private TableColumn<DoctorCommissionSummary, String> totalBillCol;
    @FXML
    private TableColumn<DoctorCommissionSummary, String> commissionCol;
    @FXML
    private TableColumn<DoctorCommissionSummary, String> paidCol;
    @FXML
    private TableColumn<DoctorCommissionSummary, String> dueCol;
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
        doctorCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().doctorName));
        totalBillCol.setCellValueFactory(data -> new SimpleStringProperty(formatAmount(data.getValue().totalBill)));
        commissionCol
                .setCellValueFactory(data -> new SimpleStringProperty(formatAmount(data.getValue().commissionTotal)));
        paidCol.setCellValueFactory(data -> new SimpleStringProperty(formatAmount(data.getValue().paidTotal)));
        dueCol.setCellValueFactory(data -> new SimpleStringProperty(formatAmount(data.getValue().dueTotal)));
    }

    @FXML
    private void handleGenerate() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start == null || end == null) {
            return;
        }

        List<CommissionLedger> commissions = commissionRepository.findByTransactionDateBetween(start, end);

        List<CommissionLedger> eligibleCommissions = commissions.stream()
                .filter(c -> c.getDoctor() != null)
                .filter(c -> {
                    Double rate = c.getDoctor().getCommissionPercentage();
                    return rate != null && rate > 0.0;
                })
                .collect(Collectors.toList());

        String previousSelection = doctorFilter.getValue();
        List<String> doctorNames = eligibleCommissions.stream()
                .map(c -> c.getDoctor().getName())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        doctorFilter.setItems(FXCollections.observableArrayList(doctorNames));
        doctorFilter.getItems().add(0, "All Doctors");
        if (previousSelection != null && doctorFilter.getItems().contains(previousSelection)) {
            doctorFilter.setValue(previousSelection);
        } else {
            doctorFilter.setValue("All Doctors");
        }

        String selectedDoctor = doctorFilter.getValue();
        if (selectedDoctor != null && !"All Doctors".equals(selectedDoctor)) {
            eligibleCommissions = eligibleCommissions.stream()
                    .filter(c -> selectedDoctor.equals(c.getDoctor().getName()))
                    .collect(Collectors.toList());
        }

        Map<String, List<CommissionLedger>> byDoctor = eligibleCommissions.stream()
                .collect(Collectors.groupingBy(c -> c.getDoctor().getName()));

        ObservableList<DoctorCommissionSummary> rows = FXCollections.observableArrayList();
        double totalDue = 0.0;
        double totalPaid = 0.0;
        double totalCommission = 0.0;

        for (Map.Entry<String, List<CommissionLedger>> entry : byDoctor.entrySet()) {
            double billTotal = entry.getValue().stream()
                    .mapToDouble(this::getBillAmount)
                    .sum();
            double commissionTotal = entry.getValue().stream()
                    .mapToDouble(this::getCommissionAmount)
                    .sum();
            double paid = entry.getValue().stream()
                    .mapToDouble(c -> c.getPaidAmount() != null ? c.getPaidAmount() : 0.0)
                    .sum();
            double due = Math.max(0.0, commissionTotal - paid);

            if (commissionTotal > 0.0) {
                rows.add(new DoctorCommissionSummary(entry.getKey(), billTotal, commissionTotal, paid, due));
            }

            totalDue += due;
            totalPaid += paid;
            totalCommission += commissionTotal;
        }

        summaryTable.setItems(rows);
        totalDueLabel.setText(formatAmount(totalDue));
        totalPaidLabel.setText(formatAmount(totalPaid));
        totalCommissionLabel.setText(formatAmount(totalCommission));
    }

    @FXML
    private void handleClose() {
        ((Stage) closeButton.getScene().getWindow()).close();
    }

    private String formatAmount(double amount) {
        return String.format("$%.2f", amount);
    }

    private double getBillAmount(CommissionLedger commission) {
        if (commission.getLabOrder() == null || commission.getLabOrder().getTotalAmount() == null) {
            return 0.0;
        }
        return commission.getLabOrder().getTotalAmount();
    }

    private double getCommissionAmount(CommissionLedger commission) {
        Double rate = commission.getDoctor() != null ? commission.getDoctor().getCommissionPercentage() : null;
        if (rate == null || rate <= 0.0) {
            return 0.0;
        }
        return getBillAmount(commission) * (rate / 100.0);
    }

    public static class DoctorCommissionSummary {
        private final String doctorName;
        private final double totalBill;
        private final double commissionTotal;
        private final double paidTotal;
        private final double dueTotal;

        public DoctorCommissionSummary(String doctorName, double totalBill, double commissionTotal, double paidTotal,
                double dueTotal) {
            this.doctorName = doctorName;
            this.totalBill = totalBill;
            this.commissionTotal = commissionTotal;
            this.paidTotal = paidTotal;
            this.dueTotal = dueTotal;
        }
    }
}
