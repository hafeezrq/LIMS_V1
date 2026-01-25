package com.qdc.lims.ui.backup;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Runs an automatic daily backup on startup.
 */
@Component
public class BackupScheduler implements CommandLineRunner {

    private final BackupService backupService;

    public BackupScheduler(BackupService backupService) {
        this.backupService = backupService;
    }

    @Override
    public void run(String... args) {
        // Run in a background thread so startup isn't delayed.
        new Thread(() -> {
            try {
                backupService.runDailyBackupIfNeeded();
            } catch (Exception ignored) {
                // Avoid failing the app because of backup issues.
            }
        }, "daily-backup").start();
    }
}
