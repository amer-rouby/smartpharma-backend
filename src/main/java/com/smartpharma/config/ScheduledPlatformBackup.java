package com.smartpharma.config;

import com.smartpharma.dto.settings.request.BackupRequest;
import com.smartpharma.service.settings.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Whole-database backups shouldn't depend on anyone remembering to click a button -
 * this runs automatically regardless of whether the platform-key endpoints
 * (BackupController) ever get called manually. createdBy is null since no user
 * triggered this.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledPlatformBackup {

    private final BackupService backupService;

    @Scheduled(cron = "0 30 3 * * *") // 03:30 daily - off-peak, staggered from the other hourly jobs
    public void runDailyBackup() {
        try {
            BackupRequest request = BackupRequest.builder()
                    .backupName("scheduled_daily")
                    .backupType("SCHEDULED")
                    .description("Automatic daily whole-database backup")
                    .build();
            backupService.createBackup(request, null);
            log.info("Scheduled daily platform backup completed successfully");
        } catch (Exception e) {
            // Deliberately caught, not rethrown: a failed scheduled backup must not crash
            // the app or block other @Scheduled jobs from running - it needs to be loud
            // in the logs so it gets noticed, and tomorrow's run should still be attempted.
            log.error("Scheduled daily platform backup FAILED - investigate pg_dump availability/permissions", e);
        }
    }
}
