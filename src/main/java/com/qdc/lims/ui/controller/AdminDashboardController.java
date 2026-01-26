package com.qdc.lims.ui.controller;

import com.qdc.lims.ui.DashboardNavigator;
import com.qdc.lims.ui.SessionManager;
import com.qdc.lims.ui.navigation.DashboardSwitchService;
import com.qdc.lims.ui.navigation.DashboardType;
import com.qdc.lims.ui.util.LogoutUtil;
import com.qdc.lims.repository.LabOrderRepository;
import com.qdc.lims.service.AdminDashboardStatsService;
import com.qdc.lims.service.BrandingService;
import com.qdc.lims.service.LocaleFormatService;
import com.qdc.lims.entity.User;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

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
    private final LabOrderRepository labOrderRepository;
    private final DashboardNavigator navigator;
    private final DashboardSwitchService dashboardSwitchService;
    private final AdminDashboardStatsService statsService;
    private final BrandingService brandingService;
    private final LocaleFormatService localeFormatService;

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
    private Label footerBrandLabel;

    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label todayRevenueLabel;
    @FXML
    private Label activeDoctorsLabel;
    @FXML
    private Label totalTestsLabel;

    public AdminDashboardController(ApplicationContext applicationContext,
            LabOrderRepository labOrderRepository,
            DashboardNavigator navigator,
            DashboardSwitchService dashboardSwitchService,
            AdminDashboardStatsService statsService,
            BrandingService brandingService,
            LocaleFormatService localeFormatService) {
        this.applicationContext = applicationContext;
        this.labOrderRepository = labOrderRepository;
        this.navigator = navigator;
        this.dashboardSwitchService = dashboardSwitchService;
        this.statsService = statsService;
        this.brandingService = brandingService;
        this.localeFormatService = localeFormatService;
    }

    @FXML
    public void initialize() {
        startClock();

        // --- FIX STARTS HERE ---
        // We use a listener to wait until the Scene and Window (Stage) are ready
        // This fixes the "method not applicable" error by passing the 3rd argument
        // (Stage)
        if (dashboardSwitcher != null) {
            dashboardSwitcher.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.windowProperty().addListener((obs2, oldWindow, newWindow) -> {
                        if (newWindow instanceof Stage stage) {
                            // 1. Calculate default dashboard for this SPECIFIC window
                            DashboardType current = DashboardType.ADMIN;
                            // 2. Pass the stage to the setup method
                            dashboardSwitchService.setupDashboardSwitcher(dashboardSwitcher, current, stage);
                            brandingService.tagStage(stage, DashboardType.ADMIN.getWindowTitle());
                        }
                    });
                }
            });
        }
        // --- FIX ENDS HERE ---

        // Note: We cannot rely on SessionManager.getCurrentUser() here safely in all
        // cases,
        // but for text labels in initialize, it's usually acceptable.
        // ideally, you would update these labels inside the listener above using
        // SessionManager.getUser(stage)
        if (SessionManager.getCurrentUser() != null) {
            String fullName = SessionManager.getCurrentUser().getFullName();
            welcomeLabel.setText("Welcome: " + fullName);

            if (userLabel != null) {
                userLabel.setText(fullName);
            }

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
        applyBranding();
    }

    private void loadDashboardStats() {
        try {
            if (activeDoctorsLabel != null) {
                activeDoctorsLabel.setText(String.valueOf(statsService.getActiveDoctorsCount()));
            }

            if (totalTestsLabel != null) {
                totalTestsLabel.setText(String.valueOf(statsService.getTotalTestsCount()));
            }

            if (totalUsersLabel != null) {
                totalUsersLabel.setText(String.valueOf(statsService.getTotalUsersCount()));
            }

            if (todayRevenueLabel != null) {
                todayRevenueLabel.setText(statsService.getTodayRevenueLabel());
            }

        } catch (Exception e) {
            System.err.println("Error loading stats: " + e.getMessage());
        }
    }

    private void startClock() {
        Thread clockThread = new Thread(() -> {
            while (true) {
                try {
                    String time = localeFormatService.formatDateTime(LocalDateTime.now());
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
        LogoutUtil.confirmAndCloseParentTab(mainContainer);
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
        openAdminWindow("/fxml/inventory_view.fxml", "Inventory Management");
    }

    @FXML
    private void handleSuppliers() {
        openAdminWindow("/fxml/supplier_management.fxml", "Supplier Management");
    }

    @FXML
    private void handleSupplierPayables() {
        openAdminWindow("/fxml/supplier_payables.fxml", "Supplier Payables");
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
        openAdminWindow("/fxml/test_definitions.fxml", "Test Definitions Management");
    }

    @FXML
    private void handleTestRecipes() {
        openAdminWindow("/fxml/test_recipes.fxml", "Test Recipe Management");
    }

    @FXML
    private void handleReferenceRanges() {
        openAdminWindow("/fxml/reference_ranges.fxml", "Reference Ranges Management");
    }

    @FXML
    private void handleTestCategories() {
        openAdminWindow("/fxml/category_management.fxml", "Department Management");
    }

    @FXML
    private void handleTestPricing() {
        showAlert("Feature Coming Soon", "Test Pricing management will be implemented soon.");
    }

    // DOCTOR MANAGEMENT
    @FXML
    private void handleDoctorPanel() {
        openAdminWindow("/fxml/doctor_panel.fxml", "Doctor Management Panel");
    }

    @FXML
    private void handleDoctorCommissions() {
        openAdminWindow("/fxml/commission_management.fxml", "Commission Management");
    }

    @FXML
    private void handleDoctorCommissionLedger() {
        openAdminWindow("/fxml/doctor_commission_ledger.fxml", "Doctor Commission Ledger");
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
        openAdminWindow("/fxml/revenue_reports.fxml", "Revenue Reports & Analytics", 1200, 800);
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
        openAdminWindow("/fxml/payment_history.fxml", "Payment History & Cash Flow");
    }

    @FXML
    private void handleOutstandingPayments() {
        handleRevenueReports();
    }

    @FXML
    private void handleFinancialQueries() {
        openAdminWindow("/fxml/financial_queries.fxml", "Financial Summary & P&L");
    }

    @FXML
    private void handleExpenses() {
        openAdminWindow("/fxml/expenses.fxml", "Expense Management");
    }

    // SYSTEM SETTINGS
    @FXML
    private void handleBackupSettings() {
        openAdminWindow("/fxml/backup_settings.fxml", "Backup & Restore");
    }

    @FXML
    private void handleDatabaseSettings() {
        showAlert("Feature Coming Soon", "Database Settings will be implemented soon.");
    }

    @FXML
    private void handleSystemConfig() {
        openAdminWindow("/fxml/system_settings.fxml", "System Configuration");
    }

    @FXML
    private void handleReportTemplates() {
        showAlert("Feature Coming Soon", "Report Templates editor will be implemented soon.");
    }

    @FXML
    private void handleUserManagement() {
        openAdminWindow("/fxml/user_management.fxml", "User Management");
    }

    /**
     * Check if current user has admin access.
     */
    /**
     * Check if current window's user has admin access.
     * UPDATED to take Stage as a parameter.
     */
    private boolean isAdminAccessAllowed(Stage stage) {
        // --- FIX: Check the user of the passed stage, not the global active stage ---
        User user = SessionManager.getUser(stage);

        if (user == null || user.getRoles() == null) {
            System.out.println("DEBUG: User is null for this stage");
            return false;
        }

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        System.out.println("DEBUG isAdminAccessAllowed: " + isAdmin);
        return isAdmin;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAccessDenied(String feature) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Access Denied");
        alert.setHeaderText("Insufficient Permissions");
        alert.setContentText("You don't have permission to access " + feature + ".");
        alert.showAndWait();
    }

    private void openAdminWindow(String fxmlPath, String title) {
        openAdminWindow(fxmlPath, title, null, null);
    }

    private void openAdminWindow(String fxmlPath, String title, Integer width, Integer height) {
        if (!ensureAdminAccess(title)) {
            return;
        }
        openWindow(fxmlPath, title, width, height);
    }

    private void openWindow(String fxmlPath, String title, Integer width, Integer height) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            brandingService.tagStage(stage, title);
            if (width != null && height != null) {
                stage.setScene(new Scene(root, width, height));
            } else {
                stage.setScene(new Scene(root));
            }
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open window: " + title + "\n" + e.getMessage());
        }
    }

    private boolean ensureAdminAccess(String feature) {
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        if (!isAdminAccessAllowed(stage)) {
            showAccessDenied(feature);
            return false;
        }
        return true;
    }

    private void applyBranding() {
        if (footerBrandLabel != null) {
            footerBrandLabel.setText(brandingService.getCopyrightLine());
        }
    }
}
