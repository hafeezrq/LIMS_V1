package com.qdc.lims.ui.session;

import com.qdc.lims.entity.User;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a user session tied to a specific window.
 * Allows multiple users to be logged in simultaneously in different windows.
 */
@Getter
@Setter
public class WindowSession {

    private final Stage stage;
    private User user;
    private String currentRole;

    public WindowSession(Stage stage) {
        this.stage = stage;
    }

    public void setUser(User user) {
        this.user = user;
        // Default to the highest priority role
        if (user != null && user.getRoles() != null && !user.getRoles().isEmpty()) {
            // Priority: ADMIN > LAB > RECEPTION
            if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
                this.currentRole = "ADMIN";
            } else if (user.getRoles().stream()
                    .anyMatch(r -> r.getName().equals("ROLE_LAB") || r.getName().equals("ROLE_PATHOLOGIST"))) {
                this.currentRole = "LAB";
            } else if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_RECEPTION"))) {
                this.currentRole = "RECEPTION";
            } else {
                this.currentRole = user.getRoles().iterator().next().getName().replace("ROLE_", "");
            }
        } else {
            this.currentRole = null;
        }
    }

    public boolean isLoggedIn() {
        return user != null;
    }

    public void logout() {
        this.user = null;
        this.currentRole = null;
    }

    public boolean hasRole(String roleNameFragment) {
        if (user == null || user.getRoles() == null)
            return false;
        return user.getRoles().stream()
                .anyMatch(r -> r.getName().contains(roleNameFragment));
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isLab() {
        return hasRole("LAB") || hasRole("PATHOLOGIST");
    }

    public boolean isReception() {
        return hasRole("RECEPTION");
    }
}
