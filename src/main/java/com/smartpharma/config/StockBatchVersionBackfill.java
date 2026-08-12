package com.smartpharma.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backfills stock_batches.version for rows that predate the @Version column added to
 * StockBatch. A newly-added Hibernate @Version column leaves existing rows NULL, and
 * Hibernate's optimistic-lock check (WHERE ... AND version = ?) never matches a NULL
 * column, which would make every pre-existing batch fail to update forever. There's no
 * Flyway migration in this project (disabled), so this runs as a one-off, idempotent
 * startup step instead - cheap no-op once every row has a version.
 */
@Component
@Slf4j
public class StockBatchVersionBackfill implements ApplicationRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int updated = entityManager
                .createNativeQuery("UPDATE smartpharma.stock_batches SET version = 0 WHERE version IS NULL")
                .executeUpdate();
        if (updated > 0) {
            log.info("Backfilled version=0 for {} pre-existing stock_batches row(s)", updated);
        }
    }
}
