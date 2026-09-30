package com.smartpharma.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ETA return receipts are stored as a second einvoice_submissions row for the same
 * sale (document_type = RETURN), so sale_transaction_id is no longer unique. But on a
 * database created before returns existed, Hibernate's ddl-auto=update never drops the
 * unique constraint it once added for the old one-to-one mapping, and every return
 * receipt insert would fail on it.
 *
 * This runs once at startup: it drops any unique constraint on exactly
 * (sale_transaction_id) - looked up by column rather than by its generated name - and
 * marks rows written before the document_type column existed as SALE. Both steps are
 * no-ops on a database that is already up to date.
 */
@Component
@Order(3)
@Slf4j
public class EInvoiceReturnReceiptBackfill implements ApplicationRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        @SuppressWarnings("unchecked")
        List<String> uniqueOnSale = entityManager.createNativeQuery(
                        "SELECT c.conname FROM pg_constraint c " +
                                "JOIN pg_class t ON t.oid = c.conrelid " +
                                "JOIN pg_namespace n ON n.oid = t.relnamespace " +
                                "JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY (c.conkey) " +
                                "WHERE n.nspname = 'smartpharma' AND t.relname = 'einvoice_submissions' " +
                                "AND c.contype = 'u' AND array_length(c.conkey, 1) = 1 " +
                                "AND a.attname = 'sale_transaction_id'")
                .getResultList();

        for (String constraint : uniqueOnSale) {
            entityManager.createNativeQuery(
                    "ALTER TABLE smartpharma.einvoice_submissions DROP CONSTRAINT \"" + constraint + "\""
            ).executeUpdate();
            log.info("Dropped unique constraint {} on einvoice_submissions.sale_transaction_id " +
                    "(a sale can now have a sales and a return receipt)", constraint);
        }

        int backfilled = entityManager.createNativeQuery(
                "UPDATE smartpharma.einvoice_submissions SET document_type = 'SALE' WHERE document_type IS NULL"
        ).executeUpdate();
        if (backfilled > 0) {
            log.info("Marked {} existing e-receipt row(s) as document_type = SALE", backfilled);
        }
    }
}
