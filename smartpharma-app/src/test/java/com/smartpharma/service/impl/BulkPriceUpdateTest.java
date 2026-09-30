package com.smartpharma.service.impl;

import com.smartpharma.catalog.dto.request.BulkPriceUpdateRequest;
import com.smartpharma.catalog.dto.response.BulkPriceUpdateResponse;
import com.smartpharma.catalog.entity.Product;
import com.smartpharma.catalog.entity.StockBatch;
import com.smartpharma.catalog.repository.ProductRepository;
import com.smartpharma.catalog.service.impl.ProductServiceImpl;
import com.smartpharma.common.exception.LocalizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkPriceUpdateTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl service;

    private Product panadol;
    private Product augmentin;
    private Product cream;

    @BeforeEach
    void setUp() {
        panadol = product(1L, "Panadol", "6221000000010", "Painkillers", "10.00", "7.00");
        augmentin = product(2L, "Augmentin", "1234567890128", "Antibiotics", "18.75", "15.00");
        cream = product(3L, "Cream", null, "Cosmetics", "40.00", "39.00");
        lenient().when(productRepository.findByPharmacyId(4L)).thenReturn(List.of(panadol, augmentin, cream));
    }

    @Test
    void previewComputesButSavesNothing() {
        BulkPriceUpdateResponse preview = service.updatePricesInBulk(percent("10", null, null, false), 4L);

        assertThat(preview.isApplied()).isFalse();
        assertThat(preview.getChangedCount()).isEqualTo(3);
        assertThat(preview.getChanges()).extracting(BulkPriceUpdateResponse.PriceChange::getNewPrice)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("11.00"), new BigDecimal("20.63"), new BigDecimal("44.00"));
        assertThat(panadol.getSellPrice()).isEqualByComparingTo("10.00");
        verify(productRepository, never()).saveAll(any());
    }

    @Test
    void applyRoundsToTheStepAndSavesOnlyWhatChanged() {
        BulkPriceUpdateRequest request = percent("10", null, "0.25", true);

        BulkPriceUpdateResponse result = service.updatePricesInBulk(request, 4L);

        assertThat(result.isApplied()).isTrue();
        assertThat(augmentin.getSellPrice()).isEqualByComparingTo("20.75"); // 20.625 -> nearest 0.25
        assertThat(panadol.getSellPrice()).isEqualByComparingTo("11.00");
        verify(productRepository).saveAll(any());
    }

    @Test
    void percentCanTargetACategoryOrSelectedProducts() {
        BulkPriceUpdateResponse byCategory = service.updatePricesInBulk(percent("-10", "antibiotics", null, false), 4L);
        assertThat(byCategory.getChanges()).singleElement()
                .satisfies(c -> assertThat(c.getProductId()).isEqualTo(2L));

        BulkPriceUpdateRequest selected = percent("-10", null, null, false);
        selected.setProductIds(List.of(1L, 3L));
        BulkPriceUpdateResponse bySelection = service.updatePricesInBulk(selected, 4L);
        assertThat(bySelection.getChanges()).extracting(BulkPriceUpdateResponse.PriceChange::getProductId)
                .containsExactly(1L, 3L);
    }

    @Test
    void priceListMatchesByBarcodeOrIdAndReportsTheRest() {
        BulkPriceUpdateRequest request = BulkPriceUpdateRequest.builder().mode("PRICE_LIST").apply(true)
                .items(List.of(
                        item(null, "1234567890128", "21.00"),
                        item(3L, null, "35.00"),
                        item(null, "6221000000010", "10.00"),   // same as now
                        item(null, "0000000000000", "5.00"),    // unknown barcode
                        item(99L, null, "5.00")))              // not this pharmacy's product
                .build();

        BulkPriceUpdateResponse result = service.updatePricesInBulk(request, 4L);

        assertThat(result.getChangedCount()).isEqualTo(2);
        assertThat(result.getUnchangedCount()).isEqualTo(1);
        assertThat(result.getNotFound()).containsExactly("0000000000000", "#99");
        assertThat(augmentin.getSellPrice()).isEqualByComparingTo("21.00");
        // 35.00 is below the cream's 39.00 buy price - allowed, but flagged.
        assertThat(result.getChanges()).filteredOn(c -> c.getProductId() == 3L).singleElement()
                .satisfies(c -> assertThat(c.isBelowCost()).isTrue());
    }

    @Test
    void missingInputsAreRefusedWithCodes() {
        assertThatThrownBy(() -> service.updatePricesInBulk(BulkPriceUpdateRequest.builder().mode("PERCENT").build(), 4L))
                .isInstanceOf(LocalizedException.class).hasMessageContaining("percentage");
        assertThatThrownBy(() -> service.updatePricesInBulk(BulkPriceUpdateRequest.builder().mode("PRICE_LIST").build(), 4L))
                .isInstanceOf(LocalizedException.class).hasMessageContaining("empty");
    }

    @Test
    void roundsToTheStepAndNeverBelowOnePiaster() {
        Product cheap = product(5L, "Cotton", null, "Supplies", "0.02", "0.01");
        Product mid = product(6L, "Syrup", null, "Supplies", "12.26", "9.00");
        when(productRepository.findByPharmacyId(7L)).thenReturn(List.of(cheap, mid));

        BulkPriceUpdateResponse lowered = service.updatePricesInBulk(percent("-90", null, null, true), 7L);
        assertThat(cheap.getSellPrice()).isEqualByComparingTo("0.01");   // 0.002 floors at 0.01

        BulkPriceUpdateResponse stepped = service.updatePricesInBulk(percent("0", "supplies", "0.50", false), 7L);
        assertThat(stepped.getChanges()).filteredOn(c -> c.getProductId() == 6L).singleElement()
                .satisfies(c -> assertThat(c.getNewPrice()).isEqualByComparingTo("1.00"));
        assertThat(lowered.isApplied()).isTrue();
    }

    private static BulkPriceUpdateRequest percent(String percent, String category, String roundTo, boolean apply) {
        return BulkPriceUpdateRequest.builder().mode("PERCENT").percent(new BigDecimal(percent)).category(category)
                .roundTo(roundTo == null ? null : new BigDecimal(roundTo)).apply(apply).build();
    }

    private static BulkPriceUpdateRequest.PriceListItem item(Long id, String barcode, String price) {
        return BulkPriceUpdateRequest.PriceListItem.builder().productId(id).barcode(barcode)
                .sellPrice(new BigDecimal(price)).build();
    }

    private static Product product(Long id, String name, String barcode, String category, String sell, String buy) {
        Product product = Product.builder().id(id).name(name).barcode(barcode).category(category)
                .sellPrice(new BigDecimal(sell)).buyPrice(new BigDecimal(buy)).build();
        // A batch pointing back at the product, as in the real entity graph -
        // guards against keying anything by Product's recursive hashCode.
        StockBatch batch = StockBatch.builder().product(product).quantityCurrent(5).build();
        product.setStockBatches(new ArrayList<>(List.of(batch)));
        return product;
    }
}
