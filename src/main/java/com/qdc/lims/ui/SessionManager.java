package com.qdc.lims.ui;

import com.qdc.lims.entity.User;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session manager supporting multiple simultaneous user sessions.
 * Each window (Stage) can have its own logged-in user.
 * 
 * For backward compatibility, also maintains a "current" session concept
 * which tracks the most recently active window.
 */
public class SessionManager {

    // Per-window session storage
    private static final Map<Stage, UserSession> windowSessions = new ConcurrentHashMap<>();

    // Track the currently active window for backward compatibility
    private static Stage activeStage;

    private static volatile Duration sessionTimeout = Duration.ofMinutes(30);

    /**
     * Session data for a single window.
     */
    @Getter
    @Setter
    public static class UserSession {
        private User user;
        private String currentRole;
        private Instant lastAccess;

        public UserSession(User user) {
            setUser(user);
        }

        public void setUser(User user) {
            this.user = user;
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
            touch();
        }

        public boolean hasRole(String roleNameFragment) {
            if (user == null || user.getRoles() == null)
                return false;
            return user.getRoles().stream()
                    .anyMatch(r -> roleMatches(r.getName(), roleNameFragment));
        }

        private boolean roleMatches(String roleName, String query) {
            if (roleName == null || query == null) {
                return false;
            }
            if (query.startsWith("ROLE_")) {
                return roleName.equalsIgnoreCase(query);
            }
            return roleName.equalsIgnoreCase(query) || roleName.equalsIgnoreCase("ROLE_" + query);
        }

        private void touch() {
            lastAccess = Instant.now();
        }

        private boolean isExpired() {
            if (sessionTimeout == null || sessionTimeout.isZero() || sessionTimeout.isNegative()) {
                return false;
            }
            if (lastAccess == null) {
                return false;
            }
            return Duration.between(lastAccess, Instant.now()).compareTo(sessionTimeout) > 0;
        }
    }

    // ========== Per-Window Session Methods ==========

    /**
     * Login user to a specific window.
     */
    public static void login(Stage stage, User user) {
        windowSessions.put(stage, new UserSession(user));
        activeStage = stage;
    }

    /**
     * Logout from a specific window.
     */
    public static void logout(Stage stage) {
        windowSessions.remove(stage);
        if (activeStage == stage) {
            activeStage = null;
        }
    }

    /**
     * Get session for a specific window.
     */
    public static UserSession getSession(Stage stage) {
        return getActiveSession(stage);
    }

    /**
     * Get user for a specific window.
     */
    public static User getUser(Stage stage) {
        UserSession session = getActiveSession(stage);
        return session != null ? session.getUser() : null;
    }

    /**
     * Get current role for a specific window.
     */
    public static String getRole(Stage stage) {
        UserSession session = getActiveSession(stage);
        return session != null ? session.getCurrentRole() : null;
    }

    /**
     * Set current role for a specific window.
     */
    public static void setRole(Stage stage, String role) {
        UserSession session = getActiveSession(stage);
        if (session != null) {
            session.setCurrentRole(role);
        }
    }

    /**
     * Check if window has logged in user.
     */
    public static boolean isLoggedIn(Stage stage) {
        UserSession session = getActiveSession(stage);
        return session != null && session.getUser() != null;
    }

    // ========== Backward Compatible Static Methods ==========
    // These use the "active" stage concept for existing code

    /**
     * Set active stage (called when a window gains focus or is used).
     */
    public static void setActiveStage(Stage stage) {
        activeStage = stage;
    }

    /**
     * Get the currently active user (from active window).
     */
    public static User getCurrentUser() {
        return activeStage != null ? getUser(activeStage) : null;
    }

    /**
     * Set current user (uses active stage).
     * 
     * @deprecated Use login(Stage, User) for multi-window support
     */
    public static void setCurrentUser(User user) {
        if (activeStage != null) {
            login(activeStage, user);
        }
    }

    /**
     * Get current role (from active window).
     */
    public static String getCurrentRole() {
        return activeStage != null ? getRole(activeStage) : null;
    }

    /**
     * Set current role (uses active stage).
     */
    public static void setCurrentRole(String role) {
        if (activeStage != null) {
            setRole(activeStage, role);
        }
    }

    /**
     * Check if logged in (active window).
     */
    public static boolean isLoggedIn() {
        return activeStage != null && isLoggedIn(activeStage);
    }

    /**
     * Logout (from active window).
     */
    public static void logout() {
        if (activeStage != null) {
            logout(activeStage);
        }
    }

    /**
     * Check if user has role (active window).
     */
    public static boolean hasRole(String roleNameFragment) {
        if (activeStage == null)
            return false;
        UserSession session = getActiveSession(activeStage);
        return session != null && session.hasRole(roleNameFragment);
    }

    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public static boolean isLab() {
        return hasRole("LAB") || hasRole("PATHOLOGIST");
    }

    public static boolean isReception() {
        return hasRole("RECEPTION");
    }

    /**
     * Get count of active sessions (logged in windows).
     */
    public static int getActiveSessionCount() {
        windowSessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return (int) windowSessions.values().stream()
                .filter(s -> s.getUser() != null)
                .count();
    }

    /**
     * Clean up when a window closes.
     */
    public static void onWindowClosed(Stage stage) {
        windowSessions.remove(stage);
        if (activeStage == stage) {
            activeStage = null;
        }
    }

    public static void setSessionTimeoutMinutes(long minutes) {
        if (minutes <= 0) {
            sessionTimeout = Duration.ZERO;
            return;
        }
        sessionTimeout = Duration.ofMinutes(minutes);
    }

    private static UserSession getActiveSession(Stage stage) {
        if (stage == null) {
            return null;
        }
        UserSession session = windowSessions.get(stage);
        if (session == null) {
            return null;
        }
        if (session.isExpired()) {
            windowSessions.remove(stage);
            if (activeStage == stage) {
                activeStage = null;
            }
            return null;
        }
        session.touch();
        return session;
    }
}
