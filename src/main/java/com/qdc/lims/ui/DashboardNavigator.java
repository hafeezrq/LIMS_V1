package com.qdc.lims.ui;

import com.qdc.lims.ui.navigation.DashboardSwitchService;
import com.qdc.lims.ui.navigation.DashboardType;
import com.qdc.lims.entity.User;
import com.qdc.lims.service.BrandingService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Handles navigation between login and dashboards.
 * Supports multiple simultaneous user sessions in different windows.
 * Delegates dashboard switching to DashboardSwitchService.
 */
@Component
public class DashboardNavigator {

    private final ApplicationContext applicationContext;
    private final DashboardSwitchService dashboardSwitchService;
    private final BrandingService brandingService;

    public DashboardNavigator(ApplicationContext applicationContext,
            DashboardSwitchService dashboardSwitchService,
            BrandingService brandingService) {
        this.applicationContext = applicationContext;
        this.dashboardSwitchService = dashboardSwitchService;
        this.brandingService = brandingService;
    }

    /**
     * Navigate user to their default dashboard based on their roles.
     * Goes directly to the highest-priority dashboard - users can switch
     * using the dashboard switcher in the header if they have multiple roles.
     */
    public void navigateBasedOnRole(User user, Stage stage) {
        // Set active stage and login
        SessionManager.setActiveStage(stage);
        SessionManager.login(stage, user);

        // Get the default (highest priority) dashboard for this user
        DashboardType defaultDashboard = dashboardSwitchService.getDefaultDashboard(stage);

        if (defaultDashboard == null) {
            showError("No Access", "User has no roles assigned or no accessible dashboards.");
            return;
        }

        // Go directly to the default dashboard - no dialog needed
        // User can switch dashboards using the switcher in the dashboard header
        dashboardSwitchService.switchToDashboard(defaultDashboard, stage);
    }

    /**
     * Open a specific dashboard by type.
     */
    public void openDashboard(DashboardType dashboardType, Stage stage) {
        dashboardSwitchService.switchToDashboard(dashboardType, stage);
    }

    /**
     * Legacy method for backward compatibility.
     */
    public void openDashboard(String role, Stage stage) {
        DashboardType target = switch (role) {
            case "ADMIN" -> DashboardType.ADMIN;
            case "RECEPTIONIST", "RECEPTION" -> DashboardType.RECEPTION;
            case "LAB_TECH", "LAB", "PATHOLOGIST" -> DashboardType.LAB;
            default -> null;
        };

        if (target != null) {
            dashboardSwitchService.switchToDashboard(target, stage);
        } else {
            showError("Navigation Error", "No dashboard found for role: " + role);
        }
    }

    /**
     * Logout from current window and return to main window.
     */
    public void logout(Stage currentStage) {
        // Logout from this specific window's session
        SessionManager.logout(currentStage);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_window.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Scene mainScene = new Scene(root, 1100, 750);

            brandingService.tagStage(currentStage, brandingService.getApplicationName());
            currentStage.setMaximized(false);
            currentStage.setScene(mainScene);
            currentStage.setWidth(1100);
            currentStage.setHeight(750);
            currentStage.setMinWidth(900);
            currentStage.setMinHeight(600);
            currentStage.centerOnScreen();
            currentStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Switch to a different user (logout + return to main window).
     */
    public void switchUser(Stage currentStage) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Switch User");
        confirmAlert.setHeaderText("Switch to a different user?");
        confirmAlert.setContentText(
                "Your current session will end and you'll be returned to the main window.\n\nDo you want to continue?");

        var result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            logout(currentStage);
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
