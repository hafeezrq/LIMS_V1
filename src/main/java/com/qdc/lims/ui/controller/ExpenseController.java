package com.qdc.lims.ui.controller;

import com.qdc.lims.entity.Payment;
import com.qdc.lims.repository.PaymentRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class ExpenseController {

    @Autowired
    private PaymentRepository paymentRepository;

    @FXML
    private Button closeButton;
    @FXML
    private DatePicker expenseDate;
    @FXML
    private ComboBox<String> categoryCombo;
    @FXML
    private TextField descriptionField;
    @FXML
    private TextField amountField;
    @FXML
    private ComboBox<String> paymentMethodCombo;

    @FXML
    private DatePicker filterStartDate;
    @FXML
    private DatePicker filterEndDate;
    @FXML
    private Label totalExpensesLabel;

    @FXML
    private TableView<Payment> expenseTable;
    @FXML
    private TableColumn<Payment, String> idCol;
    @FXML
    private TableColumn<Payment, String> dateCol;
    @FXML
    private TableColumn<Payment, String> categoryCol;
    @FXML
    private TableColumn<Payment, String> descCol;
    @FXML
    private TableColumn<Payment, String> methodCol;
    @FXML
    private TableColumn<Payment, String> amountCol;

    @FXML
    public void initialize() {
        setupTable();
        setupForm();

        // Defaults
        filterStartDate.setValue(LocalDate.now().withDayOfMonth(1));
        filterEndDate.setValue(LocalDate.now());

        handleSearch();
    }

    private void setupForm() {
        expenseDate.setValue(LocalDate.now());
        categoryCombo.setItems(FXCollections.observableArrayList(
                "RENT", "UTILITIES", "SALARY", "MAINTENANCE", "SUPPLIES", "MARKETING", "MISC"));
        paymentMethodCombo.setItems(FXCollections.observableArrayList(
                "CASH", "BANK_TRANSFER", "CHEQUE", "CREDIT_CARD"));
        paymentMethodCombo.setValue("CASH");
    }

    private void setupTable() {
        idCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        dateCol.setCellValueFactory(data -> {
            LocalDateTime dt = data.getValue().getTransactionDate();
            return new SimpleStringProperty(dt != null ? dt.toLocalDate().toString() : "");
        });
        categoryCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        descCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        methodCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentMethod()));
        amountCol.setCellValueFactory(
                data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getAmount())));
    }

    @FXML
    private void handleAddExpense() {
        try {
            if (descriptionField.getText().isEmpty() || amountField.getText().isEmpty()) {
                showAlert("Error", "Description and amount are required.");
                return;
            }

            double amount = Double.parseDouble(amountField.getText());

            Payment payment = new Payment();
            payment.setType("EXPENSE");
            payment.setCategory(categoryCombo.getValue() != null ? categoryCombo.getValue() : "MISC");
            payment.setDescription(descriptionField.getText());
            payment.setAmount(amount);
            payment.setPaymentMethod(paymentMethodCombo.getValue());
            payment.setTransactionDate(expenseDate.getValue().atStartOfDay());

            paymentRepository.save(payment);

            // Clear form
            descriptionField.clear();
            amountField.clear();

            // Refresh
            handleSearch();

        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid amount format.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to save: " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        LocalDate start = filterStartDate.getValue();
        LocalDate end = filterEndDate.getValue();

        if (start != null && end != null) {
            List<Payment> expenses = paymentRepository.findByTypeAndTransactionDateBetween(
                    "EXPENSE", start.atStartOfDay(), end.atTime(23, 59, 59));

            expenseTable.setItems(FXCollections.observableArrayList(expenses));

            double total = expenses.stream().mapToDouble(Payment::getAmount).sum();
            totalExpensesLabel.setText(String.format("%.2f", total));
        }
    }

    @FXML
    private void handleClose() {
        ((Stage) closeButton.getScene().getWindow()).close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}
