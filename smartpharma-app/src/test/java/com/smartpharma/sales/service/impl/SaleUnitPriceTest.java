package com.smartpharma.sales.service.impl;

import com.smartpharma.catalog.entity.Product;
import com.smartpharma.common.exception.LocalizedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaleUnitPriceTest {

    private static Product product(String sellPrice) {
        return Product.builder().id(7L).name("Panadol")
                .sellPrice(sellPrice == null ? null : new BigDecimal(sellPrice)).build();
    }

    @Test
    void theProductPriceIsCharged() {
        assertThat(SaleTransactionServiceImpl.resolveUnitPrice(product("25.50"), new BigDecimal("25.5"), false))
                .isEqualByComparingTo("25.50");
    }

    @Test
    void aLiveSaleWithAnotherPriceIsRefused() {
        assertThatThrownBy(() -> SaleTransactionServiceImpl.resolveUnitPrice(product("25.50"), new BigDecimal("0.01"), false))
                .isInstanceOfSatisfying(LocalizedException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getErrorCode()).isEqualTo("SALE_PRICE_CHANGED");
                    assertThat(e.getParams()).containsEntry("price", "25.50");
                });
    }

    @Test
    void anOfflineSaleIsRecordedAtTheCurrentPrice() {
        assertThat(SaleTransactionServiceImpl.resolveUnitPrice(product("25.50"), new BigDecimal("20.00"), true))
                .isEqualByComparingTo("25.50");
    }

    @Test
    void aProductWithoutAPriceCantBeSold() {
        assertThatThrownBy(() -> SaleTransactionServiceImpl.resolveUnitPrice(product("0.00"), null, false))
                .isInstanceOfSatisfying(LocalizedException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("SALE_PRODUCT_NO_PRICE"));
        assertThatThrownBy(() -> SaleTransactionServiceImpl.resolveUnitPrice(product(null), null, true))
                .isInstanceOf(LocalizedException.class);
    }
}
