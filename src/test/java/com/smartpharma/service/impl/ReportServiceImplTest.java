package com.smartpharma.service.impl;

import com.smartpharma.dto.request.ReportRequest;
import com.smartpharma.dto.response.ExpiryReportResponse;
import com.smartpharma.repository.ExpenseRepository;
import com.smartpharma.repository.ProductRepository;
import com.smartpharma.repository.SaleTransactionRepository;
import com.smartpharma.repository.StockBatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private SaleTransactionRepository saleRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockBatchRepository stockBatchRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void getExpiryReport_shouldCountExpiredAndUpcomingBatchesCorrectly() {
        ReportRequest request = new ReportRequest();
        request.setPharmacyId(1L);

        LocalDate today = LocalDate.now();

        when(stockBatchRepository.getExpiredProducts(1L, today)).thenReturn(List.of(
                new Object[]{16L, "بنادول إكسترا", "B1", today.minusDays(10), 10},
                new Object[]{17L, "باراسيتامول", "B2", today.minusDays(2), 5}
        ));

        when(stockBatchRepository.getExpiringProducts(1L, today.plusDays(90))).thenReturn(List.of(
                new Object[]{18L, "فولتارين", "B3", today.plusDays(3), 20},
                new Object[]{19L, "أموكسيسيلين", "B4", today.plusDays(25), 10},
                new Object[]{20L, "إيبوبروفين", "B5", today.plusDays(60), 15}
        ));

        ExpiryReportResponse response = reportService.getExpiryReport(request);

        assertEquals(5L, response.getTotalExpiring());
        assertEquals(1L, response.getUrgentExpiring());
        assertEquals(1L, response.getWarningExpiring());
        assertEquals(1L, response.getOkExpiring());
        assertEquals(2L, response.getExpiredCount());
        assertEquals(5, response.getExpiringProducts().size());
    }
}
