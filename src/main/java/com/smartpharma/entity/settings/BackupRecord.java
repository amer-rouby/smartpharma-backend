package com.smartpharma.entity.settings;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "backup_records", schema = "smartpharma")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "backup_name", nullable = false)
    private String backupName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "backup_type")
    private String backupType; // FULL, INCREMENTAL

    @Column(name = "status")
    private String status; // PENDING, COMPLETED, FAILED

    @Column(name = "description")
    private String description;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "restored_at")
    private LocalDateTime restoredAt;
}