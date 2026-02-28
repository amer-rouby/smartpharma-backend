package com.smartpharma.repository.settings;


import com.smartpharma.entity.settings.BackupRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackupRecordRepository extends JpaRepository<BackupRecord, Long> {

    @Query("SELECT br FROM BackupRecord br ORDER BY br.createdAt DESC")
    List<BackupRecord> findAllRecent();

    @Query("SELECT br FROM BackupRecord br WHERE br.status = 'COMPLETED' ORDER BY br.createdAt DESC")
    List<BackupRecord> findCompletedBackups();

    long count();
}