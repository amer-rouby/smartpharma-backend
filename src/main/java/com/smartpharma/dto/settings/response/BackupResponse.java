package com.smartpharma.dto.settings.response;

import com.smartpharma.entity.settings.BackupRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupResponse {

    private Long id;
    private String backupName;
    private String filePath;
    private Long fileSize;
    private String backupType;
    private String status;
    private String description;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime restoredAt;

    public static BackupResponse fromEntity(BackupRecord backup) {
        return BackupResponse.builder()
                .id(backup.getId())
                .backupName(backup.getBackupName())
                .filePath(backup.getFilePath())
                .fileSize(backup.getFileSize())
                .backupType(backup.getBackupType())
                .status(backup.getStatus())
                .description(backup.getDescription())
                .createdBy(backup.getCreatedBy())
                .createdAt(backup.getCreatedAt())
                .restoredAt(backup.getRestoredAt())
                .build();
    }
}