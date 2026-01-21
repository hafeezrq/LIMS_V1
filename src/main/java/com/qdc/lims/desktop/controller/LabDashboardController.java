package com.qdc.lims.desktop.controller;

import com.qdc.lims.desktop.DashboardNavigator;
import com.qdc.lims.desktop.SessionManager;
import com.qdc.lims.desktop.navigation.DashboardSwitchService;
import com.qdc.lims.desktop.navigation.DashboardType;
import com.qdc.lims.entity.User;
import com.qdc.lims.repository.LabOrderRepository;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JavaFX controller for the lab technician dashboard window.
 */
@Component("labDashboardController")
public class LabDashboardController {

    @FXML
    private Label userLabel;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label pendingCountLabel;

    @FXML
    private Label completedCountLabel;

    @FXML
    private Button switchRoleButton;

    private final ApplicationContext springContext;
    private final DashboardNavigator navigator;
    private final LabOrderRepository labOrderRepository;
    private final DashboardSwitchService dashboardSwitchService;

    public LabDashboardController(ApplicationContext springContext,
            DashboardNavigator navigator,
            LabOrderRepository labOrderRepository,
            DashboardSwitchService dashboardSwitchService) {
        this.springContext = springContext;
        this.navigator = navigator;
        this.labOrderRepository = labOrderRepository;
        this.dashboardSwitchService = dashboardSwitchService;
    }

    @FXML
    private void initialize() {
        if (SessionManager.getCurrentUser() != null) {
            String fullName = SessionManager.getCurrentUser().getFullName();
            welcomeLabel.setText("Welcome: " + fullName);
            userLabel.setText(fullName);

            // Display active role if label exists
            if (roleLabel != null && SessionManager.getCurrentRole() != null) {
                roleLabel.setText("Active Role: " + SessionManager.getCurrentRole());
            }

            // Load stats
            loadDashboardStats();

            // Hide switch button - not needed in tabbed interface
            if (switchRoleButton != null) {
                switchRoleButton.setVisible(false);
            }
        }
    }

    /**
     * Load dashboard statistics (pending and completed tests).
     */
    private void loadDashboardStats() {
        try {
            // Count pending orders (not COMPLETED or CANCELLED)
            // Implementation depends on repository methods available
            // Assuming generic approach if specific count method not present
            // For better performance, a custom query should be added to repository

            long pendingCount = labOrderRepository.findAll().stream()
                    .filter(order -> "PENDING".equals(order.getStatus()) || "IN_PROGRESS".equals(order.getStatus()))
                    .count();

            long completedCount = labOrderRepository.findAll().stream()
                    .filter(order -> "COMPLETED".equals(order.getStatus()))
                    .count();

            pendingCountLabel.setText(String.valueOf(pendingCount));
            completedCountLabel.setText(String.valueOf(completedCount));

        } catch (Exception e) {
            System.err.println("Error loading lab stats: " + e.getMessage());
        }
    }

    /**
     * Handle Switch User button - allows quick switching to a different user.
     */
    @FXML
    private void handleSwitchUser() {
        navigator.switchUser((Stage) welcomeLabel.getScene().getWindow());
    }

    @FXML
    private void handleLogout() {
        // Find and close the parent tab (if in tabbed interface)
        closeParentTab();
    }

    /**
     * Close the parent tab that contains this dashboard.
     * Used when Logout button is clicked.
     */
    private void closeParentTab() {
        // Get the root of this dashboard (the BorderPane loaded from FXML)
        javafx.scene.Parent dashboardRoot = welcomeLabel.getScene().getRoot();

        // If we're inside a TabPane, the scene root will be the main window's
        // BorderPane
        // We need to find the TabPane and our tab within it
        if (dashboardRoot instanceof javafx.scene.layout.BorderPane mainBorderPane) {
            // Look for the TabPane in the main window
            javafx.scene.Node center = mainBorderPane.getCenter();
            if (center instanceof javafx.scene.control.TabPane tabPane) {
                // Find the tab containing our dashboard content
                for (javafx.scene.control.Tab tab : tabPane.getTabs()) {
                    // The tab content is this dashboard - check by traversing up from welcomeLabel
                    if (isDescendantOf(welcomeLabel, tab.getContent())) {
                        // Confirm logout
                        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Logout");
                        confirm.setHeaderText("Logout from this session?");
                        confirm.setContentText("This will close the current session.");

                        java.util.Optional<javafx.scene.control.ButtonType> result = confirm.showAndWait();
                        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                            tabPane.getTabs().remove(tab);
                        }
                        return;
                    }
                }
            }
        }

        // Fallback: show message that logout failed
        showAlert("Logout", "Unable to close session. Please close the tab manually.");
    }

    /**
     * Check if a node is a descendant of a parent node.
     */
    private boolean isDescendantOf(javafx.scene.Node node, javafx.scene.Node potentialParent) {
        if (potentialParent == null || node == null)
            return false;
        javafx.scene.Parent current = node.getParent();
        while (current != null) {
            if (current == potentialParent)
                return true;
            current = current.getParent();
        }
        return node == potentialParent;
    }

    // Menu action handlers
    @FXML
    private void handleRegisterPatient() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/patient_registration.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Patient Registration");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open patient registration: " + e.getMessage());
        }
    }

    @FXML
    private void handleSearchPatient() {
        showAlert("Feature", "Search Patient feature will be implemented in the full version.");
    }

    @FXML
    private void handleCreateOrder() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create_order.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Create Lab Order");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open order creation: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewOrders() {
        showAlert("Feature", "View Orders feature will be implemented in the full version.");
    }

    @FXML
    private void handleWorklist() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/lab_worklist.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Lab Worklist");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open lab worklist: " + e.getMessage());
        }
    }

    @FXML
    private void handleEnterResults() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/lab_worklist.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Lab Worklist - Enter Results");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open lab worklist: " + e.getMessage());
        }
    }

    @FXML
    private void handleCompletedTests() {
        showAlert("Feature", "Completed Tests feature will be implemented in the full version.");
    }

    @FXML
    private void handleViewStock() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/inventory_view.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Inventory Management");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open inventory view: " + e.getMessage());
        }
    }

    @FXML
    private void handleLowStock() {
        showAlert("Feature", "Low Stock Alert feature will be implemented in the full version.");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
