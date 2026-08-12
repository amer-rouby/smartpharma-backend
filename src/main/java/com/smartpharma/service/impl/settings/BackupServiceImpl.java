package com.smartpharma.service.impl.settings;

import com.smartpharma.dto.settings.request.BackupRequest;
import com.smartpharma.dto.settings.response.BackupResponse;
import com.smartpharma.entity.settings.BackupRecord;
import com.smartpharma.repository.settings.BackupRecordRepository;
import com.smartpharma.service.settings.BackupService;
import com.smartpharma.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupServiceImpl implements BackupService {

    private static final String DUMP_EXTENSION = ".dump";
    private static final long PROCESS_TIMEOUT_MINUTES = 30;

    private final BackupRecordRepository backupRepository;

    @Value("${backup.directory:./backups}")
    private String backupDirectory;

    @Value("${backup.pg-dump-path:pg_dump}")
    private String pgDumpPath;

    @Value("${backup.pg-restore-path:pg_restore}")
    private String pgRestorePath;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Override
    @Transactional
    public BackupResponse createBackup(BackupRequest request, Long userId) {
        PgConnectionInfo connInfo = parseConnectionInfo(datasourceUrl);

        try {
            Path backupPath = Paths.get(backupDirectory);
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = sanitizeFileName(request.getBackupName()) + "_" + timestamp + DUMP_EXTENSION;
            Path fullPath = backupPath.resolve(backupFileName);

            runPgDump(connInfo, fullPath);

            long fileSize = Files.size(fullPath);

            BackupRecord backup = BackupRecord.builder()
                    .backupName(request.getBackupName())
                    .filePath(fullPath.toString())
                    .fileSize(fileSize)
                    .backupType(request.getBackupType())
                    .status("COMPLETED")
                    .description(request.getDescription())
                    .createdBy(userId)
                    .createdAt(LocalDateTime.now())
                    .build();
            backup = backupRepository.save(backup);

            log.info("Backup created successfully: {} ({} bytes)", backupFileName, fileSize);
            return BackupResponse.fromEntity(backup);

        } catch (Exception e) {
            log.error("Error creating backup", e);
            throw new RuntimeException("Failed to create backup: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BackupResponse> getAllBackups() {
        return backupRepository.findAllRecent().stream()
                .map(BackupResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BackupResponse getBackupById(Long id) {
        BackupRecord backup = backupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Backup not found with id: " + id));
        return BackupResponse.fromEntity(backup);
    }

    @Override
    @Transactional
    public void deleteBackup(Long id) {
        BackupRecord backup = backupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Backup not found with id: " + id));

        try {
            if (backup.getFilePath() != null) {
                Path filePath = Paths.get(backup.getFilePath());
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.error("Error deleting backup file", e);
        }

        backupRepository.delete(backup);
        log.info("Backup deleted: {}", backup.getBackupName());
    }

    @Override
    @Transactional
    public void restoreBackup(Long id) {
        BackupRecord backup = backupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Backup not found with id: " + id));

        if (!"COMPLETED".equals(backup.getStatus())) {
            throw new RuntimeException("Cannot restore backup with status: " + backup.getStatus());
        }
        if (backup.getFilePath() == null || !Files.exists(Paths.get(backup.getFilePath()))) {
            throw new RuntimeException("Backup file is missing on disk, cannot restore");
        }

        Long restoringUserId = SecurityUtils.getCurrentUserId();
        log.warn("DESTRUCTIVE OPERATION: userId={} is restoring backup id={} ({}). This overwrites the ENTIRE " +
                        "database (all pharmacies/tenants), not just the caller's own pharmacy.",
                restoringUserId, backup.getId(), backup.getBackupName());

        PgConnectionInfo connInfo = parseConnectionInfo(datasourceUrl);

        try {
            runPgRestore(connInfo, Paths.get(backup.getFilePath()));
        } catch (Exception e) {
            log.error("Error restoring backup id={}", id, e);
            throw new RuntimeException("Failed to restore backup: " + e.getMessage(), e);
        }

        backup.setRestoredAt(LocalDateTime.now());
        backupRepository.save(backup);

        log.info("Backup restored: {}", backup.getBackupName());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadBackup(Long id) {
        BackupRecord backup = backupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Backup not found with id: " + id));

        try {
            Path filePath = Paths.get(backup.getFilePath());
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Error downloading backup", e);
            throw new RuntimeException("Failed to download backup: " + e.getMessage());
        }
    }

    private void runPgDump(PgConnectionInfo connInfo, Path outputFile) throws IOException, InterruptedException {
        List<String> command = List.of(
                pgDumpPath,
                "-h", connInfo.host(),
                "-p", String.valueOf(connInfo.port()),
                "-U", connInfo.username(),
                "-d", connInfo.database(),
                "-F", "c", // custom format: compressed, restorable with pg_restore, supports selective restore
                "-f", outputFile.toString()
        );
        runProcess(command, connInfo, "pg_dump", "Make sure PostgreSQL client tools are installed and either on " +
                "the PATH or configured via the 'backup.pg-dump-path' property.");
    }

    private void runPgRestore(PgConnectionInfo connInfo, Path inputFile) throws IOException, InterruptedException {
        List<String> command = List.of(
                pgRestorePath,
                "-h", connInfo.host(),
                "-p", String.valueOf(connInfo.port()),
                "-U", connInfo.username(),
                "-d", connInfo.database(),
                "--clean",
                "--if-exists",
                "--no-owner",
                inputFile.toString()
        );
        runProcess(command, connInfo, "pg_restore", "Make sure PostgreSQL client tools are installed and either on " +
                "the PATH or configured via the 'backup.pg-restore-path' property.");
    }

    private void runProcess(List<String> command, PgConnectionInfo connInfo, String toolName, String installHint)
            throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().put("PGPASSWORD", connInfo.password());
        processBuilder.redirectErrorStream(false);

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new IOException(toolName + " could not be started (" + e.getMessage() + "). " + installHint, e);
        }

        String stderr = readStream(process.getErrorStream());

        boolean finished = process.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException(toolName + " timed out after " + PROCESS_TIMEOUT_MINUTES + " minutes");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException(toolName + " exited with code " + exitCode
                    + (stderr.isBlank() ? "" : ": " + stderr.trim()));
        }
    }

    private String readStream(InputStream stream) throws IOException {
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "backup";
        }
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private PgConnectionInfo parseConnectionInfo(String jdbcUrl) {
        // Expected form: jdbc:postgresql://host:port/database[?params]
        String withoutPrefix = jdbcUrl.startsWith("jdbc:") ? jdbcUrl.substring(5) : jdbcUrl;
        try {
            URI uri = new URI(withoutPrefix);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String database = uri.getPath() != null && uri.getPath().startsWith("/")
                    ? uri.getPath().substring(1) : uri.getPath();
            if (host == null || database == null || database.isBlank()) {
                throw new IllegalArgumentException("Could not parse host/database from datasource URL");
            }
            return new PgConnectionInfo(host, port, database, datasourceUsername, datasourcePassword);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Could not parse spring.datasource.url for backup/restore: "
                    + e.getMessage(), e);
        }
    }

    private record PgConnectionInfo(String host, int port, String database, String username, String password) {
    }
}
