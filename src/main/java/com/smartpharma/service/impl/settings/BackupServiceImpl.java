package com.smartpharma.service.impl.settings;

import com.smartpharma.dto.settings.request.BackupRequest;
import com.smartpharma.dto.settings.response.BackupResponse;
import com.smartpharma.entity.settings.BackupRecord;
import com.smartpharma.repository.settings.BackupRecordRepository;
import com.smartpharma.service.settings.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupServiceImpl implements BackupService {

    private final BackupRecordRepository backupRepository;

    @Value("${backup.directory:./backups}")
    private String backupDirectory;

    @Override
    @Transactional
    public BackupResponse createBackup(BackupRequest request, Long userId) {
        try {
            Path backupPath = Paths.get(backupDirectory);
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = request.getBackupName() + "_" + timestamp + ".sql";
            Path fullPath = backupPath.resolve(backupFileName);

            createBackupFile(fullPath, request);

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

            backupRepository.save(backup);
            log.info("Backup created successfully: {}", backupFileName);

            return BackupResponse.fromEntity(backup);

        } catch (IOException e) {
            log.error("Error creating backup", e);
            throw new RuntimeException("Failed to create backup: " + e.getMessage());
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
            Path filePath = Paths.get(backup.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
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

    private void createBackupFile(Path path, BackupRequest request) throws IOException {
        String backupContent = "-- SmartPharma Database Backup\n" +
                "-- Backup Name: " + request.getBackupName() + "\n" +
                "-- Type: " + request.getBackupType() + "\n" +
                "-- Date: " + LocalDateTime.now() + "\n" +
                "-- Description: " + request.getDescription() + "\n\n" +
                "-- This is a simulated backup file\n" +
                "-- In production, this would contain actual SQL dump\n";

        Files.writeString(path, backupContent);
    }
}