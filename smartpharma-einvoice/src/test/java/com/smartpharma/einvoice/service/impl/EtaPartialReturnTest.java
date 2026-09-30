package com.smartpharma.einvoice.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpharma.catalog.entity.Product;
import com.smartpharma.einvoice.entity.EtaTaxpayerProfile;
import com.smartpharma.einvoice.entity.enums.EtaEnvironment;
import com.smartpharma.einvoice.exception.EtaReceiptException;
import com.smartpharma.payments.entity.enums.PaymentMethod;
import com.smartpharma.sales.entity.SaleItem;
import com.smartpharma.sales.entity.SaleTransaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EtaPartialReturnTest {

    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    // Product 1: 2 x 11.40 with 14% VAT; product 2: 3 x 5.00, exempt (V003).
    private static EtaReceiptBuilder.BuiltReceipt sale(String discount) {
        Product taxed = Product.builder().id(1L).name("Taxed").barcode("6221000000010").unitType("BOX").build();
        Product exempt = Product.builder().id(2L).name("Exempt").barcode("6221000000027").unitType("BOX")
                .etaTaxSubtype("V003").build();
        List<SaleItem> items = new ArrayList<>(List.of(
                SaleItem.builder().product(taxed).quantity(2).unitPrice(new BigDecimal("11.40"))
                        .totalPrice(new BigDecimal("22.80")).build(),
                SaleItem.builder().product(exempt).quantity(3).unitPrice(new BigDecimal("5.00"))
                        .totalPrice(new BigDecimal("15.00")).build()));
        SaleTransaction sale = SaleTransaction.builder().invoiceNumber("INV-9").discountAmount(new BigDecimal(discount))
                .paymentMethod(PaymentMethod.CASH).transactionDate(LocalDateTime.now(CAIRO).minusHours(1))
                .items(items).build();
        sale.calculateTotals();
        EtaTaxpayerProfile profile = EtaTaxpayerProfile.builder().environment(EtaEnvironment.PREPROD)
                .rin("123456789").companyTradeName("Pharmacy").branchCode("0").activityCode("4773")
                .governate("Cairo").regionCity("Nasr City").street("Street").buildingNumber("1")
                .defaultTaxSubtype("V009").build();
        return EtaReceiptBuilder.build(sale, profile, "POS-1", "", null, "EGP", CAIRO);
    }

    private static JsonNode partial(EtaReceiptBuilder.BuiltReceipt original, Map<String, Integer> quantities,
                                    String discount) throws Exception {
        EtaReceiptBuilder.BuiltReceipt built = EtaReceiptBuilder.buildReturn(original.json(), original.uuid(),
                "R1-INV-9", original.uuid(), null, Instant.now(), quantities, new BigDecimal(discount));
        return MAPPER.readTree(built.json());
    }

    @Test
    void onlyTheReturnedLinesAndQuantitiesAreReversed() throws Exception {
        JsonNode receipt = partial(sale("0"), Map.of("1", 1, "2", 2), "0");

        assertThat(receipt.path("documentType").path("receiptType").asText()).isEqualTo("r");
        assertThat(receipt.path("itemData")).hasSize(2);
        JsonNode taxed = receipt.path("itemData").get(0);
        assertThat(taxed.path("quantity").decimalValue()).isEqualByComparingTo("1");
        assertThat(taxed.path("netSale").decimalValue()).isEqualByComparingTo("10.00");
        assertThat(taxed.path("taxableItems").get(0).path("amount").decimalValue()).isEqualByComparingTo("1.40");
        assertThat(taxed.path("total").decimalValue()).isEqualByComparingTo("11.40");
        JsonNode exempt = receipt.path("itemData").get(1);
        assertThat(exempt.path("quantity").decimalValue()).isEqualByComparingTo("2");
        assertThat(exempt.path("total").decimalValue()).isEqualByComparingTo("10.00");

        assertThat(receipt.path("netAmount").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(receipt.path("totalAmount").decimalValue()).isEqualByComparingTo("21.40");
        assertThat(receipt.path("taxTotals").get(0).path("amount").decimalValue()).isEqualByComparingTo("1.40");
        assertThat(receipt.has("extraReceiptDiscountData")).isFalse();
    }

    @Test
    void theDiscountShareIsDeductedFromTheReturn() throws Exception {
        JsonNode receipt = partial(sale("3.78"), Map.of("2", 3), "1.50");

        assertThat(receipt.path("itemData")).hasSize(1);
        assertThat(receipt.path("extraReceiptDiscountData").get(0).path("amount").decimalValue())
                .isEqualByComparingTo("1.50");
        assertThat(receipt.path("totalAmount").decimalValue()).isEqualByComparingTo("13.50");
        // An exempt (V003) line still carries its 0% tax item, as on the sale.
        assertThat(receipt.path("taxTotals").get(0).path("amount").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    void returningMoreThanWasSoldIsRefused() {
        assertThatThrownBy(() -> partial(sale("0"), Map.of("1", 3), "0"))
                .isInstanceOf(EtaReceiptException.class).hasMessageContaining("more returned");
        assertThatThrownBy(() -> partial(sale("0"), Map.of("7", 1), "0"))
                .isInstanceOf(EtaReceiptException.class);
    }

    @Test
    void theWholeReceiptIsStillReversedAsItIs() throws Exception {
        EtaReceiptBuilder.BuiltReceipt original = sale("1.00");
        EtaReceiptBuilder.BuiltReceipt whole = EtaReceiptBuilder.buildReturn(original.json(), original.uuid(),
                "R-INV-9", original.uuid(), null, Instant.now());

        JsonNode receipt = MAPPER.readTree(whole.json());
        assertThat(receipt.path("itemData")).isEqualTo(MAPPER.readTree(original.json()).path("itemData"));
        assertThat(new BigDecimal(whole.totalAmount())).isEqualByComparingTo(new BigDecimal(original.totalAmount()));
    }
}
