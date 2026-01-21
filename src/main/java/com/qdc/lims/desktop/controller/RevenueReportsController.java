package com.qdc.lims.desktop.controller;

import com.qdc.lims.entity.LabOrder;
import com.qdc.lims.repository.LabOrderRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for Revenue Reports.
 * Rebuilt from scratch to ensure FXML compatibility and stability.
 */
@Component
public class RevenueReportsController {

    @Autowired
    private LabOrderRepository orderRepository;

    @FXML
    private Button closeButton;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private Label totalRevenueLabel;
    @FXML
    private Label totalCountLabel;
    @FXML
    private TableView<LabOrder> reportTable;
    @FXML
    private TableColumn<LabOrder, String> orderIdCol;
    @FXML
    private TableColumn<LabOrder, String> dateCol;
    @FXML
    private TableColumn<LabOrder, String> patientCol;
    @FXML
    private TableColumn<LabOrder, String> amountCol;
    @FXML
    private TableColumn<LabOrder, String> statusCol;

    @FXML
    private CheckBox outstandingOnlyBox;

    @FXML
    public void initialize() {
        System.out.println("RevenueReportsController initialized.");

        // Setup table columns safely
        setupTableColumns();

        // Set default dates
        if (startDatePicker != null && endDatePicker != null) {
            startDatePicker.setValue(LocalDate.now().minusDays(30)); // Default to last 30 days
            endDatePicker.setValue(LocalDate.now());
        }

        // Initial load
        handleGenerateReport();
    }

    private void setupTableColumns() {
        if (orderIdCol != null) {
            orderIdCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        }
        if (dateCol != null) {
            dateCol.setCellValueFactory(data -> {
                LocalDateTime date = data.getValue().getOrderDate();
                return new SimpleStringProperty(
                        date != null ? date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
            });
        }
        if (patientCol != null) {
            patientCol.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().getPatient() != null ? data.getValue().getPatient().getFullName() : "Unknown"));
        }
        if (amountCol != null) {
            amountCol.setCellValueFactory(data -> new SimpleStringProperty(
                    String.format("%.2f",
                            data.getValue().getTotalAmount() != null ? data.getValue().getTotalAmount() : 0.0)));
        }
        if (statusCol != null) {
            statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        }
    }

    @FXML
    private void handleGenerateReport() {
        if (orderRepository == null) {
            System.err.println("OrderRepository is null! Spring injection failed.");
            return;
        }

        try {
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();

            if (start != null && end != null) {
                LocalDateTime startDateTime = start.atStartOfDay();
                LocalDateTime endDateTime = end.atTime(23, 59, 59);

                List<LabOrder> orders = orderRepository.findByOrderDateBetween(startDateTime, endDateTime);

                // Filter for outstanding payments if checkbox is selected
                if (outstandingOnlyBox.isSelected()) {
                    orders = orders.stream()
                            .filter(o -> o.getBalanceDue() > 0)
                            .toList();
                }

                double total = orders.stream()
                        .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0)
                        .sum();

                if (totalRevenueLabel != null) {
                    totalRevenueLabel.setText(String.format("%.2f", total));
                }
                if (totalCountLabel != null) {
                    totalCountLabel.setText(String.valueOf(orders.size()));
                }
                if (reportTable != null) {
                    reportTable.setItems(FXCollections.observableArrayList(orders));
                }
            }
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
