package com.smartpharma.controller.settings;

import com.smartpharma.dto.settings.request.BackupRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.settings.response.BackupResponse;
import com.smartpharma.service.settings.BackupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings/backup")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class BackupController {

    private final BackupService backupService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BackupResponse>> createBackup(
            @Valid @RequestBody BackupRequest request,
            @RequestParam Long userId) {

        log.info("Creating backup for userId: {}", userId);

        BackupResponse backup = backupService.createBackup(request, userId);
        return ResponseEntity.ok(ApiResponse.success(backup, "Backup created successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<BackupResponse>>> getAllBackups() {
        log.info("Fetching all backups");

        List<BackupResponse> backups = backupService.getAllBackups();
        return ResponseEntity.ok(ApiResponse.success(backups, "Backups retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BackupResponse>> getBackupById(@PathVariable Long id) {
        log.info("Fetching backup with id: {}", id);

        BackupResponse backup = backupService.getBackupById(id);
        return ResponseEntity.ok(ApiResponse.success(backup, "Backup retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBackup(@PathVariable Long id) {
        log.info("Deleting backup with id: {}", id);

        backupService.deleteBackup(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Backup deleted successfully"));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> restoreBackup(@PathVariable Long id) {
        log.info("Restoring backup with id: {}", id);

        backupService.restoreBackup(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Backup restored successfully"));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadBackup(@PathVariable Long id) {
        log.info("Downloading backup with id: {}", id);

        byte[] backupData = backupService.downloadBackup(id);
        BackupResponse backup = backupService.getBackupById(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", backup.getBackupName() + ".sql");

        return ResponseEntity.ok()
                .headers(headers)
                .body(backupData);
    }
}