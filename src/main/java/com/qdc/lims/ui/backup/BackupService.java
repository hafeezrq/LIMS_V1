package com.qdc.lims.ui.backup;

import com.qdc.lims.ui.AppPaths;
import net.lingala.zip4j.ZipFile;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates and restores encrypted backups.
 */
@Service
public class BackupService {

    private final DataSource dataSource;
    private final BackupSettingsService settings;

    public BackupService(DataSource dataSource, BackupSettingsService settings) {
        this.dataSource = dataSource;
        this.settings = settings;
    }

    public Path backupNow() {
        char[] password = settings.getBackupPassword()
                .orElseThrow(() -> new RuntimeException("Backup password is not configured"));

        try {
            Files.createDirectories(AppPaths.backupsDir());

            Path tempDb = Files.createTempFile("qdc-lims-backup-", ".db");
            tempDb.toFile().deleteOnExit();

            // Safe SQLite copy using VACUUM INTO (requires SQLite 3.27+).
            try (var conn = dataSource.getConnection()) {
                if (!conn.getClass().getName().toLowerCase().contains("sqlite")) {
                    throw new IllegalStateException("Unsupported DB connection for backup: " + conn.getClass());
                }
                try (var st = conn.createStatement()) {
                    // Use forward slashes in path for SQLite SQL literal compatibility.
                    String outPath = tempDb.toAbsolutePath().toString().replace("\\", "/");
                    st.execute("VACUUM INTO '" + outPath.replace("'", "''") + "'");
                }
            }

            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            Path outZip = AppPaths.backupsDir().resolve("backup_" + ts + ".zip");

            try (ZipFile zipFile = new ZipFile(outZip.toFile(), password)) {
                zipFile.addFile(tempDb.toFile());
            }

            // Update daily marker
            settings.setLastBackupDate(LocalDate.now());

            // Keep last 30 days by default
            applyRetention(30);

            // Cleanup best-effort
            try {
                Files.deleteIfExists(tempDb);
            } catch (IOException ignored) {
            }

            return outZip;
        } catch (Exception e) {
            throw new RuntimeException("Backup failed: " + e.getMessage(), e);
        }
    }

    public void restoreBackup(Path backupZip, char[] password) {
        if (backupZip == null || !Files.exists(backupZip)) {
            throw new IllegalArgumentException("Backup file not found");
        }

        try {
            Path tempDir = Files.createTempDirectory("qdc-lims-restore-");
            tempDir.toFile().deleteOnExit();

            try (ZipFile zipFile = new ZipFile(backupZip.toFile(), password)) {
                zipFile.extractAll(tempDir.toString());
            }

            List<Path> dbFiles;
            try (var stream = Files.list(tempDir)) {
                dbFiles = stream.filter(p -> p.getFileName().toString().endsWith(".db"))
                        .collect(Collectors.toList());
            }

            if (dbFiles.isEmpty()) {
                throw new IllegalArgumentException("Backup archive does not contain a .db file");
            }

            Path extractedDb = dbFiles.get(0);
            Path targetDb = AppPaths.databasePath();
            Files.createDirectories(targetDb.getParent());

            // NOTE: replacing the DB while the app is running is risky.
            // We do a best-effort overwrite; the UI should trigger an app restart after
            // this.
            Files.copy(extractedDb, targetDb, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("Restore failed: " + e.getMessage(), e);
        }
    }

    public void runDailyBackupIfNeeded() {
        if (settings.getBackupPassword().isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate last = settings.getLastBackupDate().orElse(null);
        if (today.equals(last)) {
            return;
        }

        backupNow();
    }

    private void applyRetention(int keepDays) throws IOException {
        if (keepDays <= 0) {
            return;
        }

        // Delete backups older than keepDays based on last-modified time.
        Path dir = AppPaths.backupsDir();
        if (!Files.exists(dir)) {
            return;
        }

        long cutoff = System.currentTimeMillis() - (keepDays * 24L * 60L * 60L * 1000L);
        try (var stream = Files.list(dir)) {
            List<Path> zips = stream
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".zip"))
                    .sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .collect(Collectors.toList());

            for (Path p : zips) {
                if (p.toFile().lastModified() < cutoff) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }
}
