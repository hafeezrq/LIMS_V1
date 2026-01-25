package com.qdc.lims.ui.session;

import com.qdc.lims.entity.User;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple window sessions, allowing different users to be logged in
 * simultaneously in different windows on the same computer.
 * 
 * This replaces the static SessionManager for multi-user support.
 */
@Component
public class WindowSessionManager {

    // Map of Stage -> WindowSession for tracking sessions per window
    private final Map<Stage, WindowSession> sessions = new ConcurrentHashMap<>();

    /**
     * Create or get session for a window.
     */
    public WindowSession getOrCreateSession(Stage stage) {
        return sessions.computeIfAbsent(stage, WindowSession::new);
    }

    /**
     * Get existing session for a window.
     */
    public WindowSession getSession(Stage stage) {
        return sessions.get(stage);
    }

    /**
     * Login a user to a specific window.
     */
    public WindowSession login(Stage stage, User user) {
        WindowSession session = getOrCreateSession(stage);
        session.setUser(user);
        return session;
    }

    /**
     * Logout and remove session for a window.
     */
    public void logout(Stage stage) {
        WindowSession session = sessions.remove(stage);
        if (session != null) {
            session.logout();
        }
    }

    /**
     * Get current user for a window.
     */
    public User getCurrentUser(Stage stage) {
        WindowSession session = sessions.get(stage);
        return session != null ? session.getUser() : null;
    }

    /**
     * Get current role for a window.
     */
    public String getCurrentRole(Stage stage) {
        WindowSession session = sessions.get(stage);
        return session != null ? session.getCurrentRole() : null;
    }

    /**
     * Set current role for a window.
     */
    public void setCurrentRole(Stage stage, String role) {
        WindowSession session = sessions.get(stage);
        if (session != null) {
            session.setCurrentRole(role);
        }
    }

    /**
     * Check if a window has an active session.
     */
    public boolean isLoggedIn(Stage stage) {
        WindowSession session = sessions.get(stage);
        return session != null && session.isLoggedIn();
    }

    /**
     * Get count of active sessions.
     */
    public int getActiveSessionCount() {
        return (int) sessions.values().stream()
                .filter(WindowSession::isLoggedIn)
                .count();
    }

    /**
     * Clean up session when window is closed.
     */
    public void onWindowClosed(Stage stage) {
        sessions.remove(stage);
    }
}
