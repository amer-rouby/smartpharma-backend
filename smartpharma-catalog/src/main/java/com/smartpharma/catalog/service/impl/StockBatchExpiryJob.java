package com.smartpharma.catalog.service.impl;

import com.smartpharma.catalog.repository.StockBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// Marks batches past their expiry date as EXPIRED, so stock totals and the
// stock value stop counting them (sales already refuse them). Runs shortly
// after midnight and once at startup, to catch up after the server was down.
@Component
@RequiredArgsConstructor
@Slf4j
public class StockBatchExpiryJob {

    private final StockBatchRepository stockBatchRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${stock.expiry-job.cron:0 5 0 * * *}")
    @Transactional
    public void markExpiredBatches() {
        int marked = stockBatchRepository.markExpired(LocalDate.now());
        if (marked > 0) {
            log.info("Marked {} stock batch(es) past their expiry date as EXPIRED", marked);
        }
    }
}
