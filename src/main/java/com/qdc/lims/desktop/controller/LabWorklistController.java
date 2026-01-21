package com.qdc.lims.desktop.controller;

import com.qdc.lims.entity.LabOrder;
import com.qdc.lims.repository.LabOrderRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JavaFX controller for lab worklist.
 */
@Component("labWorklistController")
public class LabWorklistController {

    @FXML
    private RadioButton pendingRadio;
    
    @FXML
    private RadioButton completedRadio;
    
    @FXML
    private RadioButton allRadio;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private Label pendingCountLabel;
    
    @FXML
    private Label completedTodayLabel;
    
    @FXML
    private Label totalOrdersLabel;
    
    @FXML
    private TableView<LabOrder> ordersTable;
    
    @FXML
    private TableColumn<LabOrder, Long> orderIdColumn;
    
    @FXML
    private TableColumn<LabOrder, String> mrnColumn;
    
    @FXML
    private TableColumn<LabOrder, String> patientNameColumn;
    
    @FXML
    private TableColumn<LabOrder, String> ageGenderColumn;
    
    @FXML
    private TableColumn<LabOrder, Integer> testCountColumn;
    
    @FXML
    private TableColumn<LabOrder, String> orderDateColumn;
    
    @FXML
    private TableColumn<LabOrder, String> statusColumn;
    
    @FXML
    private TableColumn<LabOrder, Void> actionColumn;
    
    private final LabOrderRepository orderRepository;
    private final ApplicationContext springContext;
    private List<LabOrder> allOrders;
    
    public LabWorklistController(LabOrderRepository orderRepository, ApplicationContext springContext) {
        this.orderRepository = orderRepository;
        this.springContext = springContext;
    }
    
    @FXML
    private void initialize() {
        setupTableColumns();
        loadOrders();
        updateStats();
    }
    
    private void setupTableColumns() {
        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        mrnColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getPatient().getMrn()
            )
        );
        
        patientNameColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getPatient().getFullName()
            )
        );
        
        ageGenderColumn.setCellValueFactory(cellData -> {
            var patient = cellData.getValue().getPatient();
            return new javafx.beans.property.SimpleStringProperty(
                patient.getAge() + " / " + patient.getGender()
            );
        });
        
        testCountColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(
                cellData.getValue().getResults().size()
            ).asObject()
        );
        
        orderDateColumn.setCellValueFactory(cellData -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getOrderDate().format(formatter)
            );
        });
        
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Color-code status
        statusColumn.setCellFactory(column -> new TableCell<LabOrder, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("PENDING")) {
                        setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
                    } else if (item.equals("COMPLETED")) {
                        setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // Action buttons in table
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button enterResultsBtn = new Button("Enter Results");
            
            {
                enterResultsBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 5 10;");
                enterResultsBtn.setOnAction(event -> {
                    LabOrder order = getTableView().getItems().get(getIndex());
                    openResultEntryForm(order);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    LabOrder order = getTableView().getItems().get(getIndex());
                    if (order.getStatus().equals("PENDING")) {
                        setGraphic(enterResultsBtn);
                    } else {
                        Label completedLabel = new Label("✓ Done");
                        completedLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        setGraphic(completedLabel);
                    }
                }
            }
        });
    }
    
    private void loadOrders() {
        allOrders = orderRepository.findAll();
        applyFilter();
    }
    
    private void applyFilter() {
        List<LabOrder> filteredOrders = allOrders;
        
        if (pendingRadio.isSelected()) {
            filteredOrders = allOrders.stream()
                .filter(order -> order.getStatus().equals("PENDING"))
                .collect(Collectors.toList());
        } else if (completedRadio.isSelected()) {
            filteredOrders = allOrders.stream()
                .filter(order -> order.getStatus().equals("COMPLETED"))
                .collect(Collectors.toList());
        }
        
        ObservableList<LabOrder> observableOrders = FXCollections.observableArrayList(filteredOrders);
        ordersTable.setItems(observableOrders);
    }
    
    private void updateStats() {
        long pending = allOrders.stream()
            .filter(order -> order.getStatus().equals("PENDING"))
            .count();
        
        long completedToday = allOrders.stream()
            .filter(order -> order.getStatus().equals("COMPLETED") &&
                           order.getOrderDate().toLocalDate().equals(LocalDate.now()))
            .count();
        
        pendingCountLabel.setText(String.valueOf(pending));
        completedTodayLabel.setText(String.valueOf(completedToday));
        totalOrdersLabel.setText(String.valueOf(allOrders.size()));
    }
    
    @FXML
    private void handleFilterChange() {
        applyFilter();
    }
    
    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim().toLowerCase();
        
        if (searchTerm.isEmpty()) {
            applyFilter();
            return;
        }
        
        List<LabOrder> searchResults = allOrders.stream()
            .filter(order -> 
                order.getPatient().getMrn().toLowerCase().contains(searchTerm) ||
                order.getPatient().getFullName().toLowerCase().contains(searchTerm)
            )
            .collect(Collectors.toList());
        
        ObservableList<LabOrder> observableOrders = FXCollections.observableArrayList(searchResults);
        ordersTable.setItems(observableOrders);
    }
    
    @FXML
    private void handleRefresh() {
        searchField.clear();
        pendingRadio.setSelected(true);
        loadOrders();
        updateStats();
    }
    
    @FXML
    private void handleEnterResults() {
        LabOrder selectedOrder = ordersTable.getSelectionModel().getSelectedItem();
        if (selectedOrder == null) {
            showAlert("Please select an order to enter results");
            return;
        }
        
        if (selectedOrder.getStatus().equals("COMPLETED")) {
            showAlert("This order is already completed");
            return;
        }
        
        openResultEntryForm(selectedOrder);
    }
    
    @FXML
    private void handleViewDetails() {
        LabOrder selectedOrder = ordersTable.getSelectionModel().getSelectedItem();
        if (selectedOrder == null) {
            showAlert("Please select an order to view");
            return;
        }
        
        openResultEntryForm(selectedOrder);
    }
    
    private void openResultEntryForm(LabOrder order) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/result_entry.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();
            
            ResultEntryController controller = loader.getController();
            controller.setOrder(order);
            
            Stage stage = new Stage();
            stage.setTitle("Enter Results - Order #" + order.getId());
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> {
                handleRefresh(); // Refresh when result entry closes
            });
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to open result entry: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleClose() {
        Stage stage = (Stage) ordersTable.getScene().getWindow();
        stage.close();
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
