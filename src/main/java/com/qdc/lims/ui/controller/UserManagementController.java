package com.qdc.lims.ui.controller;

import com.qdc.lims.ui.SessionManager;
import com.qdc.lims.entity.Role;
import com.qdc.lims.entity.User;
import com.qdc.lims.repository.RoleRepository;
import com.qdc.lims.repository.UserRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JavaFX controller for user management (Admin only).
 * SECURITY: This controller verifies admin permissions on initialization.
 */
@Component("userManagementController")
public class UserManagementController {

    @FXML
    private TableView<User> userTable;

    @FXML
    private TableColumn<User, String> usernameColumn;

    @FXML
    private TableColumn<User, String> fullNameColumn;

    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TableColumn<User, String> statusColumn;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField fullNameField;

    @FXML
    private CheckBox adminCheckBox;

    @FXML
    private CheckBox receptionCheckBox;

    @FXML
    private CheckBox labCheckBox;

    @FXML
    private CheckBox activeCheckBox;

    @FXML
    private Label messageLabel;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementController(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @FXML
    private void initialize() {
        // SECURITY: Verify admin access before loading
        if (!SessionManager.isAdmin()) {
            showAccessDeniedAndClose();
            return;
        }

        // Setup table columns
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        // Display roles as comma-separated string
        roleColumn.setCellValueFactory(cellData -> {
            String roleNames = cellData.getValue().getRoles().stream()
                    .map(role -> role.getName().replace("ROLE_", ""))
                    .collect(Collectors.joining(", "));
            return new javafx.beans.property.SimpleStringProperty(roleNames);
        });

        statusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().isActive() ? "Active" : "Inactive";
            return new javafx.beans.property.SimpleStringProperty(status);
        });

        statusColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("Active")) {
                        setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                    }
                }
            }
        });

        loadUsers();
        messageLabel.setText("");
    }

    private void loadUsers() {
        List<User> users = userRepository.findAll();
        ObservableList<User> observableUsers = FXCollections.observableArrayList(users);
        userTable.setItems(observableUsers);
    }

    @FXML
    private void handleCreateUser() {
        messageLabel.setText("");

        // Validation
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String fullName = fullNameField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            showError("Please fill in all required fields");
            return;
        }

        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            showError("Username already exists");
            return;
        }

        // Build roles FIRST - need to ensure we have role entities
        List<Role> selectedRoles = new ArrayList<>();
        if (adminCheckBox.isSelected()) {
            roleRepository.findByName("ROLE_ADMIN").ifPresent(selectedRoles::add);
        }
        if (receptionCheckBox.isSelected()) {
            roleRepository.findByName("ROLE_RECEPTION").ifPresent(selectedRoles::add);
        }
        if (labCheckBox.isSelected()) {
            roleRepository.findByName("ROLE_LAB").ifPresent(selectedRoles::add);
        }

        if (selectedRoles.isEmpty()) {
            showError("Please select at least one role");
            return;
        }

        // Create user
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setActive(activeCheckBox.isSelected());
        user.setRoles(new HashSet<>(selectedRoles)); // Set roles directly

        try {
            userRepository.save(user); // Save first to get ID
            showSuccess("User created successfully!");

            // Critical: reload from DB to ensure list is up-to-date with transactions
            loadUsers();
            handleClearForm();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to create user: " + e.getMessage());
        }
    }

    @FXML
    private void handleEditUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showError("Please select a user to edit");
            return;
        }

        // Populate form with selected user data
        usernameField.setText(selectedUser.getUsername());
        usernameField.setDisable(true); // Can't change username
        fullNameField.setText(selectedUser.getFullName());

        // Check which roles the user has
        adminCheckBox.setSelected(selectedUser.hasRole("ROLE_ADMIN"));
        receptionCheckBox.setSelected(selectedUser.hasRole("ROLE_RECEPTION"));
        labCheckBox.setSelected(selectedUser.hasRole("ROLE_LAB"));

        activeCheckBox.setSelected(selectedUser.isActive());

        showInfo("Editing user: " + selectedUser.getUsername() + ". Update and click Create to save changes.");
    }

    @FXML
    private void handleDeleteUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showError("Please select a user to delete");
            return;
        }

        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete user: " + selectedUser.getUsername() + "?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userRepository.delete(selectedUser);
                    showSuccess("User deleted successfully");
                    loadUsers();
                } catch (Exception e) {
                    showError("Failed to delete user: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadUsers();
        handleClearForm();
        showInfo("User list refreshed");
    }

    @FXML
    private void handleClearForm() {
        usernameField.clear();
        usernameField.setDisable(false);
        passwordField.clear();
        fullNameField.clear();
        adminCheckBox.setSelected(false);
        receptionCheckBox.setSelected(false);
        labCheckBox.setSelected(false);
        activeCheckBox.setSelected(true);
        messageLabel.setText("");
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) userTable.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        messageLabel.setText("❌ " + message);
        messageLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
    }

    private void showSuccess(String message) {
        messageLabel.setText("✓ " + message);
        messageLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }

    private void showInfo(String message) {
        messageLabel.setText("ℹ️ " + message);
        messageLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
    }

    /**
     * SECURITY: Show access denied alert and close the window.
     * Called when a non-admin user attempts to open User Management.
     */
    private void showAccessDeniedAndClose() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Access Denied");
        alert.setHeaderText("Admin Access Required");
        alert.setContentText(
                "User Management is restricted to administrators only.\n\n" +
                        "Your current role does not have permission to access this feature.\n\n" +
                        "Please contact your system administrator if you need access.");
        alert.showAndWait();

        // Close the window
        if (userTable != null && userTable.getScene() != null && userTable.getScene().getWindow() != null) {
            Stage stage = (Stage) userTable.getScene().getWindow();
            stage.close();
        }
    }
}
