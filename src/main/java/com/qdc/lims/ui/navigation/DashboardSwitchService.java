package com.qdc.lims.ui.navigation;

import com.qdc.lims.service.BrandingService;
import com.qdc.lims.ui.SessionManager;
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

@Service
public class DashboardSwitchService {

    private final ApplicationContext applicationContext;
    private final BrandingService brandingService;

    public DashboardSwitchService(ApplicationContext applicationContext, BrandingService brandingService) {
        this.applicationContext = applicationContext;
        this.brandingService = brandingService;
    }

    /**
     * KEY FIX: Determines accessible dashboards based on the user of a SPECIFIC
     * window (Stage).
     * This ignores the global 'activeStage' variable that causes your bug.
     */
    public List<DashboardType> getAccessibleDashboards(Stage stage) {
        User user = SessionManager.getUser(stage);
        return getAccessibleDashboards(user);
    }

    public List<DashboardType> getAccessibleDashboards(User user) {
        if (user == null || user.getRoles() == null) {
            return List.of();
        }

        List<DashboardType> accessible = new ArrayList<>();
        Set<Role> userRoles = user.getRoles();
        boolean isAdmin = userRoles.stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        if (isAdmin) {
            // Admin can access everything
            accessible.add(DashboardType.ADMIN);
            accessible.add(DashboardType.RECEPTION);
            accessible.add(DashboardType.LAB);
        } else {
            // Check based on roles
            for (DashboardType dashboard : DashboardType.values()) {
                for (Role role : userRoles) {
                    // Uses your Enum's existing logic
                    if (dashboard.isAllowedForRole(role.getName()) && !accessible.contains(dashboard)) {
                        accessible.add(dashboard);
                    }
                }
            }
        }
        return accessible;
    }

    /**
     * Populates the dashboard switcher ComboBox for a specific window.
     */
    public void setupDashboardSwitcher(ComboBox<String> switcher, DashboardType currentDashboard, Stage stage) {
        if (switcher == null)
            return;

        switcher.getItems().clear();
        List<DashboardType> accessible = getAccessibleDashboards(stage);

        for (DashboardType dashboard : accessible) {
            switcher.getItems().add(dashboard.getDisplayName());
        }

        if (currentDashboard != null) {
            switcher.setValue(currentDashboard.getDisplayName());
        }

        // Only show switcher if user has multiple options
        boolean canSwitch = accessible.size() > 1;
        switcher.setVisible(canSwitch);
        switcher.setManaged(canSwitch);
    }

    /**
     * Switch dashboard for a specific window.
     */
    public boolean switchToDashboard(String displayName, Stage currentStage) {
        DashboardType target = DashboardType.fromDisplayName(displayName);
        return switchToDashboard(target, currentStage);
    }

    public boolean switchToDashboard(DashboardType targetDashboard, Stage currentStage) {
        if (targetDashboard == null)
            return false;

        // Verify permission for this specific window's user
        User user = SessionManager.getUser(currentStage);
        if (!getAccessibleDashboards(user).contains(targetDashboard)) {
            showError("Access Denied", "You do not have permission to access this dashboard.");
            return false;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(targetDashboard.getFxmlPath()));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            // Update the Role in SessionManager for this specific window
            SessionManager.setRole(currentStage, targetDashboard.name());

            // Tag the stage so branding can be applied consistently.
            brandingService.tagStage(currentStage, targetDashboard.getWindowTitle());
            currentStage.setScene(new Scene(root));

            // Resize logic (Customize sizes as needed)
            if (targetDashboard == DashboardType.ADMIN) {
                currentStage.setWidth(1000);
                currentStage.setHeight(700);
            } else {
                currentStage.setWidth(900);
                currentStage.setHeight(650);
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

    public DashboardType getDefaultDashboard(Stage stage) {
        User user = SessionManager.getUser(stage);
        if (user == null || user.getRoles() == null)
            return null;

        // Uses your Enum's logic to find default
        for (Role role : user.getRoles()) {
            DashboardType type = DashboardType.getDefaultForRole(role.getName());
            if (type != null)
                return type;
        }
        return null; // Fallback
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
