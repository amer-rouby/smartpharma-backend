package com.smartpharma.sales.service.impl;

import com.smartpharma.catalog.entity.StockBatch;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FefoAllocationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 30);

    private static StockBatch batch(long id, int quantity, LocalDate expiry) {
        return StockBatch.builder().id(id).quantityCurrent(quantity).expiryDate(expiry)
                .status(StockBatch.BatchStatus.ACTIVE).build();
    }

    private static List<String> taken(List<SaleTransactionServiceImpl.BatchTake> takes) {
        return takes.stream().map(t -> t.batch().getId() + "x" + t.quantity()).toList();
    }

    @Test
    void aQuantityOneBatchCoversComesFromTheEarliestExpiry() {
        List<StockBatch> batches = List.of(batch(1, 50, TODAY.plusMonths(6)), batch(2, 10, TODAY.plusMonths(1)));

        assertThat(taken(SaleTransactionServiceImpl.allocateFefo(batches, 5, TODAY))).containsExactly("2x5");
    }

    @Test
    void aQuantityNoSingleBatchCoversIsSpreadAcrossBatches() {
        List<StockBatch> batches = List.of(batch(1, 20, TODAY.plusMonths(6)), batch(2, 10, TODAY.plusMonths(1)));

        assertThat(taken(SaleTransactionServiceImpl.allocateFefo(batches, 25, TODAY))).containsExactly("2x10", "1x15");
    }

    @Test
    void expiredAndEmptyBatchesAreNeverSold() {
        List<StockBatch> batches = List.of(
                batch(1, 100, TODAY.minusDays(1)),
                batch(2, 0, TODAY.plusMonths(1)),
                batch(3, 4, TODAY),
                batch(4, 3, null));

        assertThat(taken(SaleTransactionServiceImpl.allocateFefo(batches, 7, TODAY))).containsExactly("3x4", "4x3");
        assertThat(SaleTransactionServiceImpl.allocateFefo(batches, 8, TODAY)).isNull();
    }
}
