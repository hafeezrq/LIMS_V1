package com.qdc.lims.ui.navigation;

/**
 * Enum defining all available dashboard types in the system.
 * Centralizes dashboard metadata for consistent navigation.
 */
public enum DashboardType {
    ADMIN("Admin Dashboard", "/fxml/dashboard_admin.fxml", "Admin Dashboard", "ROLE_ADMIN"),
    RECEPTION("Reception Dashboard", "/fxml/dashboard_reception.fxml", "Reception Dashboard", "ROLE_RECEPTION",
            "ROLE_STAFF"),
    LAB("Lab Dashboard", "/fxml/dashboard_lab.fxml", "Lab Dashboard", "ROLE_LAB", "ROLE_PATHOLOGIST", "ROLE_STAFF");

    private final String displayName;
    private final String fxmlPath;
    private final String windowTitle;
    private final String[] allowedRoles;

    DashboardType(String displayName, String fxmlPath, String windowTitle, String... allowedRoles) {
        this.displayName = displayName;
        this.fxmlPath = fxmlPath;
        this.windowTitle = windowTitle;
        this.allowedRoles = allowedRoles;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFxmlPath() {
        return fxmlPath;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public String[] getAllowedRoles() {
        return allowedRoles;
    }

    /**
     * Check if a given role name is allowed to access this dashboard.
     */
    public boolean isAllowedForRole(String roleName) {
        for (String allowed : allowedRoles) {
            if (allowed.equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find a DashboardType by its display name.
     */
    public static DashboardType fromDisplayName(String displayName) {
        for (DashboardType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Get the default dashboard for a given role.
     */
    public static DashboardType getDefaultForRole(String roleName) {
        // Priority: ADMIN > LAB > RECEPTION
        if (ADMIN.isAllowedForRole(roleName))
            return ADMIN;
        if (LAB.isAllowedForRole(roleName))
            return LAB;
        if (RECEPTION.isAllowedForRole(roleName))
            return RECEPTION;
        return null;
    }
}
