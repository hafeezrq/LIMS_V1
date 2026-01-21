package com.qdc.lims.desktop.controller;

import com.qdc.lims.service.ConfigService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;

@Controller
public class SystemSettingsController {

    @Autowired
    private ConfigService configService;

    // General Info
    @FXML
    private TextField clinicNameField;
    @FXML
    private TextArea clinicAddressArea;
    @FXML
    private TextField clinicPhoneField;
    @FXML
    private TextField clinicEmailField;

    // Reports
    @FXML
    private TextField headerTextField;
    @FXML
    private TextArea footerTextArea;
    @FXML
    private TextField logoPathField;

    // Billing
    @FXML
    private TextField currencySymbolField;
    @FXML
    private TextField taxRateField;

    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        loadSettings();
    }

    private void loadSettings() {
        // Refresh cache to ensure latest data
        configService.refreshCache();

        clinicNameField.setText(configService.get("CLINIC_NAME"));
        clinicAddressArea.setText(configService.get("CLINIC_ADDRESS"));
        clinicPhoneField.setText(configService.get("CLINIC_PHONE"));
        clinicEmailField.setText(configService.get("CLINIC_EMAIL"));

        headerTextField.setText(configService.get("REPORT_HEADER_TEXT"));
        footerTextArea.setText(configService.get("REPORT_FOOTER_TEXT"));
        logoPathField.setText(configService.get("REPORT_LOGO_PATH"));

        currencySymbolField.setText(configService.get("CURRENCY_SYMBOL"));
        taxRateField.setText(configService.get("TAX_RATE_PERCENT"));

        statusLabel.setText("Settings loaded.");
    }

    @FXML
    private void handleSave() {
        try {
            configService.set("CLINIC_NAME", clinicNameField.getText());
            configService.set("CLINIC_ADDRESS", clinicAddressArea.getText());
            configService.set("CLINIC_PHONE", clinicPhoneField.getText());
            configService.set("CLINIC_EMAIL", clinicEmailField.getText());

            configService.set("REPORT_HEADER_TEXT", headerTextField.getText());
            configService.set("REPORT_FOOTER_TEXT", footerTextArea.getText());
            configService.set("REPORT_LOGO_PATH", logoPathField.getText());

            configService.set("CURRENCY_SYMBOL", currencySymbolField.getText());
            configService.set("TAX_RATE_PERCENT", taxRateField.getText());

            statusLabel.setText("Configuration saved successfully!");

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("System configuration updated successfully.");
            alert.showAndWait();

        } catch (Exception e) {
            statusLabel.setText("Error saving settings.");
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Save Failed");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleBrowseLogo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Logo Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        File selectedFile = fileChooser.showOpenDialog(clinicNameField.getScene().getWindow());
        if (selectedFile != null) {
            logoPathField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) clinicNameField.getScene().getWindow();
        stage.close();
    }
}
