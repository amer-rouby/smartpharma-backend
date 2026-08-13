package com.smartpharma.service.settings;

import com.smartpharma.dto.settings.request.BackupRequest;
import com.smartpharma.dto.settings.response.BackupResponse;

import java.util.List;

/**
 * Self-service, per-pharmacy data export. Unlike BackupService (whole-database,
 * pg_dump-based, platform-operator only), this is a JSON export of just the calling
 * pharmacy's own rows - pg_dump can't filter by tenant, so this queries each
 * pharmacy-scoped table directly instead. Safe by construction: it only ever reads
 * and writes a file, it never touches the database, so unlike a restore there's no
 * cross-tenant risk here.
 */
public interface PharmacyBackupService {

    BackupResponse createBackup(Long pharmacyId, Long userId, BackupRequest request);

    List<BackupResponse> getBackupsForPharmacy(Long pharmacyId);

    BackupResponse getBackup(Long id, Long pharmacyId);

    byte[] downloadBackup(Long id, Long pharmacyId);

    void deleteBackup(Long id, Long pharmacyId);
}
