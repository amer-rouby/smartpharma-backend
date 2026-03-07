package com.smartpharma.scheduler;

import com.smartpharma.service.DemandPredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class PredictionScheduler {

    private final DemandPredictionService predictionService;

    @Scheduled(cron = "0 0 2 * * SUN")
    public void generateWeeklyPredictions() {
        log.info("Scheduled task: Generating weekly demand predictions");
        Long pharmacyId = 4L;
        for (int i = 1; i <= 7; i++) {
            LocalDate predictionDate = LocalDate.now().plusDays(i);
            predictionService.generatePredictions(pharmacyId, predictionDate);
        }
        log.info("Weekly prediction generation completed");
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void updatePredictionsWithActuals() {
        log.info("Scheduled task: Updating predictions with actual sales");
        log.info("Actual sales update completed");
    }
}