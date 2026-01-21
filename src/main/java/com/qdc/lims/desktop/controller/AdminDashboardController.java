package com.qdc.lims.desktop.controller;

import com.qdc.lims.desktop.DashboardNavigator;
import com.qdc.lims.desktop.SessionManager;
import com.qdc.lims.desktop.navigation.DashboardSwitchService;
import com.qdc.lims.desktop.navigation.DashboardType;
import com.qdc.lims.repository.DoctorRepository;
import com.qdc.lims.repository.LabOrderRepository;
import com.qdc.lims.repository.TestDefinitionRepository;
import com.qdc.lims.repository.UserRepository;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JavaFX controller for the admin dashboard window.
 * This dashboard is for system administration, management, and configuration
 * ONLY.
 * For operational work (patient registration, orders, lab work), admins should
 * switch to Lab or Reception dashboards using the dashboard switcher.
 */
@Controller
public class AdminDashboardController {

    private final ApplicationContext applicationContext;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final TestDefinitionRepository testDefinitionRepository;
    private final LabOrderRepository labOrderRepository;
    private final DashboardNavigator navigator;
    private final DashboardSwitchService dashboardSwitchService;

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label dateTimeLabel;
    @FXML
    private BorderPane mainContainer;
    @FXML
    private Button switchRoleButton;
    @FXML
    private ComboBox<String> dashboardSwitcher;
    @FXML
    private Label userLabel;

    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label todayRevenueLabel;
    @FXML
    private Label activeDoctorsLabel;
    @FXML
    private Label totalTestsLabel;

    public AdminDashboardController(ApplicationContext applicationContext,
            UserRepository userRepository,
            DoctorRepository doctorRepository,
            TestDefinitionRepository testDefinitionRepository,
            LabOrderRepository labOrderRepository,
            DashboardNavigator navigator,
            DashboardSwitchService dashboardSwitchService) {
        this.applicationContext = applicationContext;
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.testDefinitionRepository = testDefinitionRepository;
        this.labOrderRepository = labOrderRepository;
        this.navigator = navigator;
        this.dashboardSwitchService = dashboardSwitchService;
    }

    @FXML
    public void initialize() {
        startClock();

        // Setup dashboard switcher for admin (can access all dashboards)
        dashboardSwitchService.setupDashboardSwitcher(dashboardSwitcher, DashboardType.ADMIN);

        if (SessionManager.getCurrentUser() != null) {
            String fullName = SessionManager.getCurrentUser().getFullName();
            welcomeLabel.setText("Welcome: " + fullName);

            if (userLabel != null) {
                userLabel.setText(fullName);
            }

            // Hide switch role button - use dashboard switcher instead
            if (switchRoleButton != null) {
                switchRoleButton.setVisible(false);
            }
        } else {
            welcomeLabel.setText("Welcome: Admin");
        }
        if (statusLabel != null) {
            statusLabel.setText("System Ready");
        }

        loadDashboardStats();
    }

    private void loadDashboardStats() {
        try {
            if (doctorRepository != null) {
                long activeDoctors = doctorRepository.countByActiveTrue();
                if (activeDoctorsLabel != null)
                    activeDoctorsLabel.setText(String.valueOf(activeDoctors));
            }

            if (testDefinitionRepository != null) {
                long totalTests = testDefinitionRepository.count();
                if (totalTestsLabel != null)
                    totalTestsLabel.setText(String.valueOf(totalTests));
            }

            if (userRepository != null && totalUsersLabel != null) {
                long userCount = userRepository.count();
                totalUsersLabel.setText(String.valueOf(userCount));
            }

            if (todayRevenueLabel != null) {
                todayRevenueLabel.setText("$0.00");
            }

        } catch (Exception e) {
            System.err.println("Error loading stats: " + e.getMessage());
        }
    }

    private void startClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, MMM dd yyyy - HH:mm:ss");
        Thread clockThread = new Thread(() -> {
            while (true) {
                try {
                    String time = LocalDateTime.now().format(formatter);
                    Platform.runLater(() -> {
                        if (dateTimeLabel != null)
                            dateTimeLabel.setText(time);
                    });
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        clockThread.setDaemon(true);
        clockThread.start();
    }

    /**
     * Handle dashboard switcher selection change.
     */
    @FXML
    private void handleDashboardSwitch() {
        String selectedDashboard = dashboardSwitcher.getValue();

        if (selectedDashboard == null || selectedDashboard.isEmpty()
                || selectedDashboard.equals(DashboardType.ADMIN.getDisplayName())) {
            return; // Already on Admin dashboard
        }

        // Use centralized switch service
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        dashboardSwitchService.switchToDashboard(selectedDashboard, stage);
    }

    /**
     * Handle Switch User button - allows quick switching to a different user.
     */
    @FXML
    private void handleSwitchUser() {
        // Delegate to navigator for consistent behavior
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
                    // The tab content is this dashboard - check by traversing up from mainContainer
                    if (isDescendantOf(mainContainer, tab.getContent()) || tab.getContent() == mainContainer) {
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

    // ===========================================================================================
    // ADMINISTRATIVE MENU HANDLERS
    // These are management/configuration functions for system administrators
    // For operational work (patients, orders, lab), admins should switch to
    // Lab/Reception dashboards
    // ===========================================================================================

    // USER MANAGEMENT
    @FXML
    private void handleUserRoles() {
        // Redirect to user management as roles are managed there for now
        handleUserManagement();
    }

    @FXML
    private void handleActivityLogs() {
        showAlert("Feature Coming Soon", "Activity Logs viewer will be implemented soon.");
    }

    // INVENTORY MANAGEMENT
    @FXML
    private void handleInventoryView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/inventory_view.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
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
    private void handleSuppliers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/supplier_management.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Supplier Management");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open supplier management: " + e.getMessage());
        }
    }

    @FXML
    private void handlePurchases() {
        showAlert("Feature Coming Soon", "Purchases & Orders management will be implemented soon.");
    }

    @FXML
    private void handleLowStock() {
        showAlert("Feature Coming Soon", "Low Stock Alerts will be implemented soon.");
    }

    @FXML
    private void handleStockAdjustments() {
        // Redirect to inventory view
        handleInventoryView();
    }

    @FXML
    private void handleViewStock() {
        handleInventoryView();
    }

    // TEST CONFIGURATION
    @FXML
    public void handleTestDefinitions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test_definitions.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Test Definitions Management");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open test definitions: " + e.getMessage());
        }
    }

    @FXML
    private void handleTestRecipes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test_recipes.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Test Recipe Management");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open test recipes: " + e.getMessage());
        }
    }

    @FXML
    private void handleReferenceRanges() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reference_ranges.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Reference Ranges Management");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open reference ranges: " + e.getMessage());
        }
    }

    @FXML
    private void handleTestCategories() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/category_management.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Test Category Management");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open category management: " + e.getMessage());
        }
    }

    @FXML
    private void handleTestPricing() {
        showAlert("Feature Coming Soon", "Test Pricing management will be implemented soon.");
    }

    // DOCTOR MANAGEMENT
    @FXML
    private void handleDoctorPanel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/doctor_panel.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Doctor Management Panel");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open doctor panel: " + e.getMessage());
        }
    }

    @FXML
    private void handleDoctorCommissions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/commission_management.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Commission Management");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open commission management: " + e.getMessage());
        }
    }

    @FXML
    private void handleCommissionRates() {
        showAlert("Feature Coming Soon", "Commission Rates configuration will be implemented soon.");
    }

    @FXML
    private void handleCommissionPayments() {
        showAlert("Feature Coming Soon", "Commission Payments will be implemented soon.");
    }

    @FXML
    private void handleDoctorStats() {
        showAlert("Feature Coming Soon", "Doctor Statistics will be implemented soon.");
    }

    // FINANCE & REPORTS
    @FXML
    private void handleRevenueReports() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/revenue_reports.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Revenue Reports & Analytics");
            stage.setScene(new Scene(root, 1200, 800));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open revenue reports: " + e.getMessage());
        }
    }

    @FXML
    private void handleDailyRevenue() {
        // Redirect to main revenue reports
        handleRevenueReports();
    }

    @FXML
    private void handleMonthlyRevenue() {
        // Redirect to main revenue reports
        handleRevenueReports();
    }

    @FXML
    private void handleCommissionReport() {
        // Redirect to Doctor Commissions
        handleDoctorCommissions();
    }

    @FXML
    private void handlePaymentHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/payment_history.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Payment History & Cash Flow");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open payment history: " + e.getMessage());
        }
    }

    @FXML
    private void handleOutstandingPayments() {
        handleRevenueReports();
    }

    @FXML
    private void handleFinancialQueries() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/financial_queries.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Financial Summary & P&L");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open financial queries: " + e.getMessage());
        }
    }

    @FXML
    private void handleExpenses() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/expenses.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Expense Management");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open expense management: " + e.getMessage());
        }
    }

    // SYSTEM SETTINGS
    @FXML
    private void handleBackupSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/backup_settings.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Backup & Restore");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open backup settings: " + e.getMessage());
        }
    }

    @FXML
    private void handleDatabaseSettings() {
        showAlert("Feature Coming Soon", "Database Settings will be implemented soon.");
    }

    @FXML
    private void handleSystemConfig() {
        openWindow("/fxml/system_settings.fxml", "System Configuration");
    }

    @FXML
    private void handleReportTemplates() {
        showAlert("Feature Coming Soon", "Report Templates editor will be implemented soon.");
    }

    @FXML
    private void handleUserManagement() {
        // SECURITY: Verify admin permission
        if (!isAdminAccessAllowed()) {
            showAccessDenied("User Management");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user_management.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("User Management");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open user management: " + e.getMessage());
        }
    }

    /**
     * Check if current user has admin access.
     */
    private boolean isAdminAccessAllowed() {
        // Check if user has admin role
        boolean isAdmin = SessionManager.isAdmin();
        System.out.println("DEBUG isAdminAccessAllowed: SessionManager.isAdmin() = " + isAdmin);
        System.out.println("DEBUG isAdminAccessAllowed: SessionManager.getCurrentUser() = " +
                (SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getUsername() : "null"));
        if (SessionManager.getCurrentUser() != null && SessionManager.getCurrentUser().getRoles() != null) {
            SessionManager.getCurrentUser().getRoles()
                    .forEach(r -> System.out.println("DEBUG isAdminAccessAllowed:   role = " + r.getName()));
        }
        return isAdmin;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private void showAccessDenied(String feature) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Access Denied");
        alert.setHeaderText("Insufficient Permissions");
        alert.setContentText("You don't have permission to access " + feature + ".");
        alert.show();
    }

    private void openWindow(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open window: " + title + "\n" + e.getMessage());
        }
    }
}
// Checking original content of AdminDashboardController to preserve
// dependencies
