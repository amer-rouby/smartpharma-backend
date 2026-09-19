package com.smartpharma.settings.service;

import com.smartpharma.settings.dto.request.BackupRequest;
import com.smartpharma.settings.dto.response.BackupResponse;

import java.util.List;

public interface BackupService {

    BackupResponse createBackup(BackupRequest request, Long userId);

    List<BackupResponse> getAllBackups();

    BackupResponse getBackupById(Long id);

    void deleteBackup(Long id);

    void restoreBackup(Long id);

    byte[] downloadBackup(Long id);
}