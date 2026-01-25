package com.qdc.lims.ui.controller;

import com.qdc.lims.ui.backup.BackupService;
import com.qdc.lims.ui.backup.BackupSettingsService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Backup/Restore settings window.
 */
@Component("backupSettingsController")
public class BackupSettingsController {

    @FXML
    private PasswordField backupPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label statusLabel;

    private final BackupSettingsService settings;
    private final BackupService backupService;

    public BackupSettingsController(BackupSettingsService settings, BackupService backupService) {
        this.settings = settings;
        this.backupService = backupService;
    }

    @FXML
    private void initialize() {
        statusLabel.setText("");

        // If a password is already configured, show a hint only.
        if (settings.getBackupPassword().isPresent()) {
            statusLabel.setText("Backup password is configured.");
        }
    }

    @FXML
    private void handleSavePassword() {
        String p1 = backupPasswordField.getText();
        String p2 = confirmPasswordField.getText();

        if (p1 == null || p1.isBlank()) {
            showError("Please enter a password.");
            return;
        }
        if (!p1.equals(p2)) {
            showError("Passwords do not match.");
            return;
        }
        if (p1.length() < 8) {
            showError("Password must be at least 8 characters.");
            return;
        }

        settings.setBackupPassword(p1.toCharArray());
        backupPasswordField.clear();
        confirmPasswordField.clear();
        showSuccess("Backup password saved.");
    }

    @FXML
    private void handleBackupNow() {
        try {
            Path zip = backupService.backupNow();
            showSuccess("Backup created: " + zip.getFileName());
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleRestore() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Backup File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Backup ZIP", "*.zip"));

        File file = chooser.showOpenDialog(statusLabel.getScene().getWindow());
        if (file == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Restore");
        confirm.setHeaderText("Restore will replace current database data.");
        confirm.setContentText("This action cannot be undone. Continue?");
        Optional<ButtonType> proceed = confirm.showAndWait();
        if (proceed.isEmpty() || proceed.get() != ButtonType.OK) {
            showError("Restore cancelled.");
            return;
        }

        TextInputDialog passDialog = new TextInputDialog();
        passDialog.setTitle("Restore Backup");
        passDialog.setHeaderText("Enter backup password");
        passDialog.setContentText("Password:");

        var opt = passDialog.showAndWait();
        if (opt.isEmpty() || opt.get().isBlank()) {
            showError("Restore cancelled (no password provided). ");
            return;
        }

        try {
            backupService.restoreBackup(file.toPath(), opt.get().toCharArray());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Restore Complete");
            alert.setHeaderText(null);
            alert.setContentText("Restore completed. The application will now close. Please reopen the app.");
            alert.showAndWait();

            Platform.exit();
        } catch (Exception e) {
            showError(e.getMessage() != null ? e.getMessage() : "Restore failed.");
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        stage.close();
    }

    private void showError(String msg) {
        statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        statusLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        statusLabel.setText(msg);
    }
}
