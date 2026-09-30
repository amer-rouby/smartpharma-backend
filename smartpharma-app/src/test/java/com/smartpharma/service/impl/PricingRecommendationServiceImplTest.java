package com.smartpharma.service.impl;

import com.smartpharma.catalog.entity.Product;
import com.smartpharma.catalog.entity.StockBatch;
import com.smartpharma.catalog.repository.ProductRepository;
import com.smartpharma.catalog.repository.StockBatchRepository;
import com.smartpharma.platform.dto.response.PricingRecommendationDTO;
import com.smartpharma.platform.service.impl.PricingRecommendationServiceImpl;
import com.smartpharma.sales.repository.SaleItemRepository;
import com.smartpharma.settings.entity.SmartFeatureSettings;
import com.smartpharma.settings.service.SmartFeatureSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingRecommendationServiceImplTest {

    @Mock
    private StockBatchRepository stockBatchRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private SaleItemRepository saleItemRepository;
    @Mock
    private SmartFeatureSettingsService smartFeatureSettingsService;

    @InjectMocks
    private PricingRecommendationServiceImpl service;

    private final List<Object[]> soldIn90Days = new ArrayList<>();
    private final List<Object[]> soldIn30Days = new ArrayList<>();

    @BeforeEach
    void setUp() {
        SmartFeatureSettings settings = new SmartFeatureSettings();
        settings.setPricingRecommendationsEnabled(true);
        when(smartFeatureSettingsService.getOrCreate(1L)).thenReturn(settings);
        when(stockBatchRepository.findExpiringBatches(eq(1L), any())).thenReturn(List.of());
        // The service asks for the 90-day window first, then the 30-day one.
        when(saleItemRepository.sumQuantityPerProductByPharmacyIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(soldIn90Days, soldIn30Days);
    }

    @Test
    void flagsStockThatHasntSoldAtAllInTheWindow() {
        Product dead = product(1L, "Old syrup", 12, "8.50", LocalDateTime.now().minusDays(200));
        when(productRepository.findByPharmacyId(1L)).thenReturn(List.of(dead));

        List<PricingRecommendationDTO> result = service.getRecommendations(1L);

        assertThat(result).singleElement().satisfies(r -> {
            assertThat(r.getReason()).isEqualTo("DEAD_STOCK");
            assertThat(r.getPriority()).isEqualTo("MEDIUM");
            assertThat(r.getSuggestedDiscountPercent()).isEqualTo(20);
            assertThat(r.getCurrentStock()).isEqualTo(12);
            assertThat(r.getMessage()).contains("No sales in the last 90 days").contains("102.00");
        });
    }

    @Test
    void aNewArrivalWithNoSalesYetIsNotDead() {
        Product fresh = product(1L, "New cream", 5, "20.00", LocalDateTime.now().minusDays(10));
        when(productRepository.findByPharmacyId(1L)).thenReturn(List.of(fresh));

        assertThat(service.getRecommendations(1L)).isEmpty();
    }

    @Test
    void slowAndNormalMoversKeepTheirPreviousBehaviour() {
        Product slow = product(1L, "Slow tabs", 40, "5.00", LocalDateTime.now().minusDays(200));
        Product normal = product(2L, "Panadol", 40, "5.00", LocalDateTime.now().minusDays(200));
        when(productRepository.findByPharmacyId(1L)).thenReturn(List.of(slow, normal));
        soldIn90Days.add(new Object[]{1L, 30L});   // 10/month on average...
        soldIn90Days.add(new Object[]{2L, 30L});
        soldIn30Days.add(new Object[]{1L, 1L});    // ...but only 1 in the last 30 days
        soldIn30Days.add(new Object[]{2L, 10L});

        List<PricingRecommendationDTO> result = service.getRecommendations(1L);

        assertThat(result).singleElement().satisfies(r -> {
            assertThat(r.getProductId()).isEqualTo(1L);
            assertThat(r.getReason()).isEqualTo("SLOW_MOVING");
        });
    }

    @Test
    void usesTwoGroupedQueriesInsteadOfTwoPerProduct() {
        List<Product> many = new ArrayList<>();
        for (long id = 1; id <= 50; id++) {
            many.add(product(id, "P" + id, 3, "1.00", LocalDateTime.now().minusDays(200)));
        }
        when(productRepository.findByPharmacyId(1L)).thenReturn(many);

        service.getRecommendations(1L);

        verify(saleItemRepository, never()).sumQuantityByProductIdAndPharmacyIdAndDateRange(anyLong(), anyLong(), any(), any());
    }

    private static Product product(Long id, String name, int stock, String buyPrice, LocalDateTime batchCreated) {
        StockBatch batch = StockBatch.builder().quantityCurrent(stock).status(StockBatch.BatchStatus.ACTIVE)
                .createdAt(batchCreated).build();
        Product product = Product.builder().id(id).name(name).buyPrice(new BigDecimal(buyPrice))
                .stockBatches(new ArrayList<>(List.of(batch))).build();
        batch.setProduct(product);
        return product;
    }
}
