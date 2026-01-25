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

@Controller
public class MainController {

    @Autowired
    private DashboardSwitchService dashboardService;

    @FXML
    private ComboBox<String> dashboardSwitcher;
    @FXML
    private Button logoutButton;

    @FXML
    public void initialize() {
        // Wait for the window to load to get the Stage
        if (dashboardSwitcher != null) {
            dashboardSwitcher.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.windowProperty().addListener((obs2, oldWindow, newWindow) -> {
                        if (newWindow instanceof Stage stage) {
                            // Setup switcher for THIS specific window
                            DashboardType current = dashboardService.getDefaultDashboard(stage);
                            dashboardService.setupDashboardSwitcher(dashboardSwitcher, current, stage);
                        }
                    });
                }
            });
        }
    }

    @FXML
    public void onDashboardSwitch() {
        String selected = dashboardSwitcher.getValue();
        if (selected == null)
            return;

        // 1. Get the specific stage
        Stage myStage = (Stage) dashboardSwitcher.getScene().getWindow();

        // 2. Pass it to the service
        dashboardService.switchToDashboard(selected, myStage);
    }

    @FXML
    public void onLogout() {
        // --- THIS IS THE FIX ---
        // 1. Get the specific stage from the button
        Stage myStage = (Stage) logoutButton.getScene().getWindow();

        // 2. Logout ONLY this window
        SessionManager.logout(myStage);

        // 3. Close this window
        myStage.close();

        // 4. (Optional) Reopen Login Screen
        // openLoginScreen();
    }
}