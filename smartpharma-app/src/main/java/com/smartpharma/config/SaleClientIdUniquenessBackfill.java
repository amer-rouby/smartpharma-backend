package com.smartpharma.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The offline POS sends every sale with a device-made clientSaleId and may send the
 * same sale again after a dropped response; SaleTransactionServiceImpl returns the
 * existing sale in that case. That lookup alone can't stop two retries racing each
 * other, so (pharmacy_id, client_sale_id) also needs a unique index - which
 * ddl-auto=update never adds to an existing table.
 *
 * Partial index: sales made before this (or by older clients) have no client_sale_id
 * and aren't constrained. CREATE ... IF NOT EXISTS makes this a no-op after the
 * first run.
 */
@Component
@Order(4)
@Slf4j
public class SaleClientIdUniquenessBackfill implements ApplicationRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        entityManager.createNativeQuery(
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_pharmacy_client_sale " +
                        "ON smartpharma.sales_transactions (pharmacy_id, client_sale_id) " +
                        "WHERE client_sale_id IS NOT NULL"
        ).executeUpdate();
    }
}
