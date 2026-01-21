package com.qdc.lims.desktop.navigation;

import com.qdc.lims.desktop.SessionManager;
import com.qdc.lims.entity.Role;
import com.qdc.lims.entity.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Centralized service for dashboard navigation and switching.
 * Supports multiple simultaneous user sessions in different windows.
 * Eliminates code duplication across dashboard controllers.
 * 
 * Responsibilities:
 * - Determine which dashboards a user can access based on roles
 * - Handle dashboard switching without logout
 * - Populate dashboard switcher ComboBox
 * - Validate access permissions
 */
@Service
public class DashboardSwitchService {

    private final ApplicationContext applicationContext;

    public DashboardSwitchService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Get list of dashboards a user can access.
     * Admin users can access ALL dashboards.
     */
    public List<DashboardType> getAccessibleDashboards(User user) {
        if (user == null || user.getRoles() == null) {
            return List.of();
        }

        List<DashboardType> accessible = new ArrayList<>();
        Set<Role> userRoles = user.getRoles();
        boolean isAdmin = userRoles.stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        // Admin can access ALL dashboards
        if (isAdmin) {
            accessible.add(DashboardType.ADMIN);
            accessible.add(DashboardType.RECEPTION);
            accessible.add(DashboardType.LAB);
        } else {
            // Non-admin users only see dashboards matching their roles
            for (Role role : userRoles) {
                for (DashboardType dashboard : DashboardType.values()) {
                    if (dashboard.isAllowedForRole(role.getName()) && !accessible.contains(dashboard)) {
                        accessible.add(dashboard);
                    }
                }
            }
        }

        return accessible;
    }

    /**
     * Get list of dashboards the current window's user can access.
     */
    public List<DashboardType> getAccessibleDashboards(Stage stage) {
        User user = SessionManager.getUser(stage);
        return getAccessibleDashboards(user);
    }

    /**
     * Get list of dashboards for the active window's user (backward compatible).
     */
    public List<DashboardType> getAccessibleDashboards() {
        User user = SessionManager.getCurrentUser();
        return getAccessibleDashboards(user);
    }

    /**
     * Populate a ComboBox with available dashboards for a window's user.
     * 
     * @param switcher         The ComboBox to populate
     * @param currentDashboard The currently active dashboard (will be pre-selected)
     * @param stage            The window stage (for getting user session)
     */
    public void setupDashboardSwitcher(ComboBox<String> switcher, DashboardType currentDashboard, Stage stage) {
        if (switcher == null)
            return;

        switcher.getItems().clear();
        List<DashboardType> accessible = getAccessibleDashboards(stage);

        for (DashboardType dashboard : accessible) {
            switcher.getItems().add(dashboard.getDisplayName());
        }

        // Pre-select current dashboard
        if (currentDashboard != null) {
            switcher.setValue(currentDashboard.getDisplayName());
        }

        // Hide switcher if user only has access to one dashboard
        switcher.setVisible(accessible.size() > 1);
        switcher.setManaged(accessible.size() > 1);
    }

    /**
     * Backward compatible version using active window.
     */
    public void setupDashboardSwitcher(ComboBox<String> switcher, DashboardType currentDashboard) {
        if (switcher == null)
            return;

        switcher.getItems().clear();
        List<DashboardType> accessible = getAccessibleDashboards();

        for (DashboardType dashboard : accessible) {
            switcher.getItems().add(dashboard.getDisplayName());
        }

        // Pre-select current dashboard
        if (currentDashboard != null) {
            switcher.setValue(currentDashboard.getDisplayName());
        }

        // Hide switcher if user only has access to one dashboard
        switcher.setVisible(accessible.size() > 1);
        switcher.setManaged(accessible.size() > 1);
    }

    /**
     * Check if a user can access a specific dashboard.
     */
    public boolean canAccess(DashboardType dashboard, User user) {
        return getAccessibleDashboards(user).contains(dashboard);
    }

    /**
     * Check if window's user can access a specific dashboard.
     */
    public boolean canAccess(DashboardType dashboard, Stage stage) {
        User user = SessionManager.getUser(stage);
        return canAccess(dashboard, user);
    }

    /**
     * Check if current user can access a specific dashboard (backward compatible).
     */
    public boolean canAccess(DashboardType dashboard) {
        return getAccessibleDashboards().contains(dashboard);
    }

    /**
     * Switch to a different dashboard.
     * Validates that the user has permission to access the target dashboard.
     * 
     * @param targetDashboard The dashboard to switch to
     * @param currentStage    The current window stage
     * @return true if switch was successful, false otherwise
     */
    public boolean switchToDashboard(DashboardType targetDashboard, Stage currentStage) {
        if (targetDashboard == null) {
            return false;
        }

        // Security check: verify user can access this dashboard
        if (!canAccess(targetDashboard, currentStage)) {
            showError("Access Denied", "You don't have permission to access " + targetDashboard.getDisplayName());
            return false;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(targetDashboard.getFxmlPath()));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            // Update the current role in this window's session
            SessionManager.setActiveStage(currentStage);
            SessionManager.setRole(currentStage, targetDashboard.name());

            currentStage.setTitle(targetDashboard.getWindowTitle());
            Scene scene = new Scene(root);
            currentStage.setScene(scene);

            // Set appropriate window size based on dashboard type
            currentStage.setMaximized(false);
            switch (targetDashboard) {
                case ADMIN -> {
                    currentStage.setWidth(1000);
                    currentStage.setHeight(700);
                    currentStage.setMinWidth(900);
                    currentStage.setMinHeight(600);
                }
                case LAB, RECEPTION -> {
                    currentStage.setWidth(900);
                    currentStage.setHeight(650);
                    currentStage.setMinWidth(800);
                    currentStage.setMinHeight(550);
                }
            }
            currentStage.centerOnScreen();
            currentStage.show();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            showError("Navigation Error", "Failed to load dashboard: " + e.getMessage());
            return false;
        }
    }

    /**
     * Switch to a dashboard by its display name (for ComboBox handler).
     */
    public boolean switchToDashboard(String displayName, Stage currentStage) {
        DashboardType target = DashboardType.fromDisplayName(displayName);
        return switchToDashboard(target, currentStage);
    }

    /**
     * Get the default dashboard for a specific window's user.
     */
    public DashboardType getDefaultDashboard(Stage stage) {
        User user = SessionManager.getUser(stage);
        return getDefaultDashboardForUser(user);
    }

    /**
     * Get the default dashboard for the active window's user (backward compatible).
     */
    public DashboardType getDefaultDashboard() {
        User user = SessionManager.getCurrentUser();
        return getDefaultDashboardForUser(user);
    }

    /**
     * Get the default dashboard for a user based on their highest-priority role.
     */
    private DashboardType getDefaultDashboardForUser(User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return null;
        }

        // Priority: ADMIN > LAB > RECEPTION
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_ADMIN"))
                return DashboardType.ADMIN;
        }
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_LAB") || role.getName().equals("ROLE_PATHOLOGIST")) {
                return DashboardType.LAB;
            }
        }
        for (Role role : user.getRoles()) {
            if (role.getName().equals("ROLE_RECEPTION"))
                return DashboardType.RECEPTION;
        }

        return null;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
