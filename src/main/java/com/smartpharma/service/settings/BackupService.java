package com.smartpharma.service.settings;

import com.smartpharma.dto.settings.request.BackupRequest;
import com.smartpharma.dto.settings.response.BackupResponse;

import java.util.List;

public interface BackupService {

    BackupResponse createBackup(BackupRequest request, Long userId);

    List<BackupResponse> getAllBackups();

    BackupResponse getBackupById(Long id);

    void deleteBackup(Long id);

    void restoreBackup(Long id);

    byte[] downloadBackup(Long id);
}