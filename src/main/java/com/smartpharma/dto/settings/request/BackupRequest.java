package com.smartpharma.dto.settings.request;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupRequest {

    private String backupName;
    private String backupType;
    private String description;
}