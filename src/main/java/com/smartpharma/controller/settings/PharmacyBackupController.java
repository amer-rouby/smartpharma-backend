package com.smartpharma.controller.settings;

import com.smartpharma.dto.settings.request.BackupRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.settings.response.BackupResponse;
import com.smartpharma.service.settings.PharmacyBackupService;
import com.smartpharma.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Self-service export of the CALLING pharmacy's own data - always scoped to
 * SecurityUtils.getCurrentPharmacyId(), never a client-supplied id. This is the
 * pharmacy-facing "backup" feature; the whole-database platform backup/restore lives
 * separately at BackupController (/api/platform/backup, platform-key only). There is
 * deliberately no restore endpoint here - restoring even just one pharmacy's data
 * back into a live multi-tenant database can collide with other pharmacies' current
 * data (shared auto-increment ids across tables), so that stays a manual,
 * platform-operator-reviewed process for now rather than a one-click API call.
 */
@RestController
@RequestMapping("/api/settings/backup")
@RequiredArgsConstructor
@Slf4j
public class PharmacyBackupController {

    private final PharmacyBackupService pharmacyBackupService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BackupResponse>> createBackup(@Valid @RequestBody BackupRequest request) {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Creating pharmacy backup - pharmacyId: {}, userId: {}", pharmacyId, userId);

        BackupResponse backup = pharmacyBackupService.createBackup(pharmacyId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(backup, "Backup created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BackupResponse>>> getBackups() {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();

        List<BackupResponse> backups = pharmacyBackupService.getBackupsForPharmacy(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(backups, "Backups retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BackupResponse>> getBackup(@PathVariable Long id) {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();

        BackupResponse backup = pharmacyBackupService.getBackup(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(backup, "Backup retrieved successfully"));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadBackup(@PathVariable Long id) {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();

        byte[] data = pharmacyBackupService.downloadBackup(id, pharmacyId);
        BackupResponse backup = pharmacyBackupService.getBackup(id, pharmacyId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", backup.getBackupName() + ".json");

        return ResponseEntity.ok().headers(headers).body(data);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBackup(@PathVariable Long id) {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();

        pharmacyBackupService.deleteBackup(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(null, "Backup deleted successfully"));
    }
}
