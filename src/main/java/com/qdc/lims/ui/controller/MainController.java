package com.qdc.lims.ui.controller;

import com.qdc.lims.ui.SessionManager;
import com.qdc.lims.ui.navigation.DashboardSwitchService;
import com.qdc.lims.ui.navigation.DashboardType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the main desktop shell. It wires the dashboard switcher to the
 * current window and performs window-scoped logout.
 */
@Controller
public class MainController {

    @Autowired
    private DashboardSwitchService dashboardService;

    @FXML
    private ComboBox<String> dashboardSwitcher;
    @FXML
    private Button logoutButton;

    /**
     * Initializes the dashboard switcher after the scene and window are available.
     */
    @FXML
    public void initialize() {
        if (dashboardSwitcher != null) {
            dashboardSwitcher.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.windowProperty().addListener((obs2, oldWindow, newWindow) -> {
                        if (newWindow instanceof Stage stage) {
                            DashboardType current = dashboardService.getDefaultDashboard(stage);
                            dashboardService.setupDashboardSwitcher(dashboardSwitcher, current, stage);
                        }
                    });
                }
            });
        }
    }

    /**
     * Switches the dashboard for the current window.
     */
    @FXML
    public void onDashboardSwitch() {
        String selected = dashboardSwitcher.getValue();
        if (selected == null) {
            return;
        }

        Stage myStage = (Stage) dashboardSwitcher.getScene().getWindow();
        dashboardService.switchToDashboard(selected, myStage);
    }

    /**
     * Logs out only the current window/session and closes it.
     */
    @FXML
    public void onLogout() {
        Stage myStage = (Stage) logoutButton.getScene().getWindow();
        SessionManager.logout(myStage);
        myStage.close();
    }
}
