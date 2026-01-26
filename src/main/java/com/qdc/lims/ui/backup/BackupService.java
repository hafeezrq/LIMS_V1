package com.qdc.lims.ui.backup;

import com.qdc.lims.ui.AppPaths;
import net.lingala.zip4j.ZipFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creates and restores encrypted backups.
 */
@Service
public class BackupService {

    private final DataSource dataSource;
    private final BackupSettingsService settings;

    @Value("${spring.datasource.url:}")
    private String jdbcUrl;

    @Value("${spring.datasource.username:}")
    private String jdbcUsername;

    @Value("${spring.datasource.password:}")
    private String jdbcPassword;

    @Value("${qdc.backup.retention-days:0}")
    private int retentionDays;

    public BackupService(DataSource dataSource, BackupSettingsService settings) {
        this.dataSource = dataSource;
        this.settings = settings;
    }

    public Path backupNow() {
        char[] password = settings.getBackupPassword()
                .orElseThrow(() -> new RuntimeException("Backup password is not configured"));

        try {
            Files.createDirectories(AppPaths.backupsDir());

            boolean postgres = isPostgres();
            String suffix = postgres ? ".dump" : ".db";
            Path tempBackup = Files.createTempFile("qdc-lims-backup-", suffix);
            tempBackup.toFile().deleteOnExit();

            if (postgres) {
                runPostgresDump(tempBackup);
            } else {
                runSqliteBackup(tempBackup);
            }

            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            Path outZip = AppPaths.backupsDir().resolve("backup_" + ts + ".zip");

            try (ZipFile zipFile = new ZipFile(outZip.toFile(), password)) {
                zipFile.addFile(tempBackup.toFile());
            }

            // Update daily marker
            settings.setLastBackupDate(LocalDate.now());

            // Optional retention (0 = disabled)
            applyRetention(retentionDays);

            // Cleanup best-effort
            try {
                Files.deleteIfExists(tempBackup);
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
                dbFiles = stream.filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".db") || name.endsWith(".dump");
                })
                        .collect(Collectors.toList());
            }

            if (dbFiles.isEmpty()) {
                throw new IllegalArgumentException("Backup archive does not contain a supported database file");
            }

            Path extracted = dbFiles.get(0);
            if (isPostgres()) {
                runPostgresRestore(extracted);
            } else {
                Path targetDb = AppPaths.databasePath();
                Files.createDirectories(targetDb.getParent());

                // NOTE: replacing the DB while the app is running is risky.
                // We do a best-effort overwrite; the UI should trigger an app restart after
                // this.
                Files.copy(extracted, targetDb, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
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

    private boolean isPostgres() {
        return jdbcUrl != null && jdbcUrl.startsWith("jdbc:postgresql:");
    }

    private void runSqliteBackup(Path outFile) throws Exception {
        // Safe SQLite copy using VACUUM INTO (requires SQLite 3.27+).
        try (var conn = dataSource.getConnection()) {
            if (!conn.getClass().getName().toLowerCase().contains("sqlite")) {
                throw new IllegalStateException("Unsupported DB connection for SQLite backup: " + conn.getClass());
            }
            try (var st = conn.createStatement()) {
                String outPath = outFile.toAbsolutePath().toString().replace("\\", "/");
                st.execute("VACUUM INTO '" + outPath.replace("'", "''") + "'");
            }
        }
    }

    private void runPostgresDump(Path outFile) throws Exception {
        String conn = toPostgresUri(jdbcUrl);
        List<String> command = List.of(
                "pg_dump",
                "--format=custom",
                "--file", outFile.toString(),
                "--dbname", conn,
                "--no-owner",
                "--no-privileges");
        runCommand(command, postgresEnv());
    }

    private void runPostgresRestore(Path dumpFile) throws Exception {
        String conn = toPostgresUri(jdbcUrl);
        List<String> command = List.of(
                "pg_restore",
                "--clean",
                "--if-exists",
                "--no-owner",
                "--no-privileges",
                "--dbname", conn,
                dumpFile.toString());
        runCommand(command, postgresEnv());
    }

    private Map<String, String> postgresEnv() {
        Map<String, String> env = new HashMap<>();
        if (jdbcUsername != null && !jdbcUsername.isBlank()) {
            env.put("PGUSER", jdbcUsername);
        }
        if (jdbcPassword != null && !jdbcPassword.isBlank()) {
            env.put("PGPASSWORD", jdbcPassword);
        }
        return env;
    }

    private void runCommand(List<String> command, Map<String, String> env) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        if (env != null && !env.isEmpty()) {
            pb.environment().putAll(env);
        }

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            String tool = command.isEmpty() ? "command" : command.get(0);
            throw new IllegalStateException(tool + " is not available on this system.", e);
        }
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException(output.toString().trim().isEmpty()
                    ? "Command failed: " + String.join(" ", command)
                    : output.toString().trim());
        }
    }

    private String toPostgresUri(String jdbc) {
        if (jdbc == null || jdbc.isBlank()) {
            throw new IllegalStateException("JDBC URL is not configured");
        }
        try {
            URI uri = new URI(jdbc.substring("jdbc:".length()));
            String host = uri.getHost() != null ? uri.getHost() : "localhost";
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath() != null ? uri.getPath() : "";
            String dbName = path.startsWith("/") ? path.substring(1) : path;
            if (dbName.isBlank()) {
                throw new IllegalStateException("Unable to determine database name from JDBC URL");
            }
            return "postgresql://" + host + ":" + port + "/" + dbName;
        } catch (Exception e) {
            throw new IllegalStateException("Invalid PostgreSQL JDBC URL: " + jdbc, e);
        }
    }
}
