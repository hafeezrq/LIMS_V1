package com.qdc.lims.ui;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Computes OS-appropriate, user-writable locations for application data.
 *
 * The app stores the SQLite database and backups outside the install directory,
 * so upgrades won't delete user data.
 */
public final class AppPaths {

    private static final String APP_DIR_NAME = "QDC LIMS";

    private AppPaths() {
    }

    public static Path appDataDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String userHome = System.getProperty("user.home", ".");

        if (os.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support", APP_DIR_NAME);
        }

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Paths.get(appData, APP_DIR_NAME);
            }
            return Paths.get(userHome, "AppData", "Roaming", APP_DIR_NAME);
        }

        // Linux / others
        return Paths.get(userHome, ".local", "share", "qdc-lims");
    }

    public static Path databasePath() {
        return appDataDir().resolve("data").resolve("qdc-lims.db");
    }

    public static Path backupsDir() {
        return appDataDir().resolve("Backups");
    }

    // Checking available methods in AppPaths
}
