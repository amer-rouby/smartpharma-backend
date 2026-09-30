package com.smartpharma.einvoice.service.impl;

import com.smartpharma.einvoice.entity.enums.EtaEnvironment;
import com.smartpharma.einvoice.util.EtaCanonicalSerializer;
import com.smartpharma.einvoice.exception.EtaReceiptException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartpharma.catalog.entity.Product;
import com.smartpharma.einvoice.entity.EtaTaxpayerProfile;
import com.smartpharma.payments.entity.enums.PaymentMethod;
import com.smartpharma.sales.entity.SaleItem;
import com.smartpharma.sales.entity.SaleTransaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EtaReceiptBuilderTest {

    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private final ObjectMapper reader = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));

    @Test
    void buildsReceiptWhoseUuidIsTheHashOfItsOwnTextWithEmptyUuid() throws Exception {
        SaleTransaction sale = sale(new BigDecimal("5.00"), item(1L, "Panadol", "6221000000010", "BOX", 2, "12.50"),
                item(2L, "Vitamin C", "EGS", "EG-123456789-001", "BOTTLE", 1, "30.00"));

        EtaReceiptBuilder.BuiltReceipt built = EtaReceiptBuilder.build(sale, profile(), "POS-1",
                "a".repeat(64), null, "EGP", CAIRO);

        ObjectNode sent = (ObjectNode) reader.readTree(built.json());
        assertThat(sent.path("header").path("uuid").asText()).isEqualTo(built.uuid());
        ((ObjectNode) sent.get("header")).put("uuid", "");
        assertThat(EtaCanonicalSerializer.sha256Hex(EtaCanonicalSerializer.serialize(sent))).isEqualTo(built.uuid());
        assertThat(built.uuid()).matches("[0-9a-f]{64}");
    }

    @Test
    void totalsFollowEtaMainCalculations() throws Exception {
        SaleTransaction sale = sale(new BigDecimal("5.00"), item(1L, "Panadol", "6221000000010", "BOX", 2, "12.50"),
                item(2L, "Vitamin C", "EGS", "EG-123456789-001", "BOTTLE", 1, "30.00"));

        JsonNode r = reader.readTree(EtaReceiptBuilder.build(sale, profile(), "POS-1", "", null, "EGP", CAIRO).json());

        JsonNode first = r.path("itemData").get(0);
        assertThat(first.path("itemType").asText()).isEqualTo("GS1");
        assertThat(first.path("itemCode").asText()).isEqualTo("6221000000010");
        assertThat(first.path("unitType").asText()).isEqualTo("BOX");
        assertThat(first.path("totalSale").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(first.path("netSale").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(first.path("total").decimalValue()).isEqualByComparingTo("25.00");
        JsonNode second = r.path("itemData").get(1);
        assertThat(second.path("itemType").asText()).isEqualTo("EGS");
        assertThat(second.path("unitType").asText()).isEqualTo("BO");

        assertThat(r.path("totalSales").decimalValue()).isEqualByComparingTo("55.00");
        assertThat(r.path("netAmount").decimalValue()).isEqualByComparingTo("55.00");
        assertThat(r.path("extraReceiptDiscountData").get(0).path("amount").decimalValue()).isEqualByComparingTo("5.00");
        assertThat(r.path("totalAmount").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(r.path("paymentMethod").asText()).isEqualTo("C");
        assertThat(r.path("header").path("previousUUID").asText()).isEmpty();
        assertThat(r.path("documentType").path("receiptType").asText()).isEqualTo("S");
        assertThat(r.path("documentType").path("typeVersion").asText()).isEqualTo("1.2");
        // Money keeps two decimals in the text sent - the UUID depends on it.
        assertThat(EtaReceiptBuilder.build(sale, profile(), "POS-1", "", null, "EGP", CAIRO).json())
                .contains("\"unitPrice\":12.50").contains("\"totalAmount\":50.00");
    }

    @Test
    void issueTimeIsConvertedToUtc() throws Exception {
        SaleTransaction sale = sale(BigDecimal.ZERO, item(1L, "Panadol", "6221000000010", "BOX", 1, "10.00"));
        sale.setTransactionDate(LocalDateTime.of(2026, 1, 15, 14, 30, 5));

        EtaReceiptBuilder.BuiltReceipt built = EtaReceiptBuilder.build(sale, profile(), "POS-1", "", null, "EGP", CAIRO);

        // Cairo is UTC+2 in January.
        assertThat(built.dateTimeIssued()).isEqualTo("2026-01-15T12:30:05Z");
        assertThat(reader.readTree(built.json()).has("extraReceiptDiscountData")).isFalse();
    }

    @Test
    void differentPreviousUuidGivesDifferentUuid() {
        SaleTransaction sale = sale(BigDecimal.ZERO, item(1L, "Panadol", "6221000000010", "BOX", 1, "10.00"));

        String a = EtaReceiptBuilder.build(sale, profile(), "POS-1", "", null, "EGP", CAIRO).uuid();
        String b = EtaReceiptBuilder.build(sale, profile(), "POS-1", "b".repeat(64), null, "EGP", CAIRO).uuid();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void refusesWhatCannotBeSentCorrectly() {
        SaleTransaction uncoded = sale(BigDecimal.ZERO, item(1L, "Loose item", "12345", "BOX", 1, "10.00"));
        assertThatThrownBy(() -> EtaReceiptBuilder.build(uncoded, profile(), "POS-1", "", null, "EGP", CAIRO))
                .isInstanceOf(EtaReceiptException.class)
                .hasMessageContaining("Loose item");

        SaleTransaction ok = sale(BigDecimal.ZERO, item(1L, "Panadol", "6221000000010", "BOX", 1, "10.00"));
        assertThatThrownBy(() -> EtaReceiptBuilder.build(ok, profile(), "POS-1", "", null, "USD", CAIRO))
                .hasMessageContaining("EGP");

        EtaTaxpayerProfile badRin = profile();
        badRin.setRin("12345");
        assertThatThrownBy(() -> EtaReceiptBuilder.build(ok, badRin, "POS-1", "", null, "EGP", CAIRO))
                .hasMessageContaining("RIN");

        SaleTransaction large = sale(BigDecimal.ZERO, item(1L, "Device", "6221000000010", "BOX", 1, "150000.00"));
        assertThatThrownBy(() -> EtaReceiptBuilder.build(large, profile(), "POS-1", "", null, "EGP", CAIRO))
                .hasMessageContaining("150,000");
    }

    @Test
    void validatesGtinCheckDigit() {
        assertThat(EtaReceiptBuilder.isValidGtin("6221000000010")).isTrue();
        assertThat(EtaReceiptBuilder.isValidGtin("6221000000014")).isFalse();
        assertThat(EtaReceiptBuilder.isValidGtin("96385074")).isTrue();
        assertThat(EtaReceiptBuilder.isValidGtin("abc")).isFalse();
        assertThat(EtaReceiptBuilder.isValidGtin(null)).isFalse();
    }

    @Test
    void mapsPaymentMethods() {
        assertThat(EtaReceiptBuilder.paymentCode(PaymentMethod.CASH)).isEqualTo("C");
        assertThat(EtaReceiptBuilder.paymentCode(PaymentMethod.MASTERCARD)).isEqualTo("V");
        assertThat(EtaReceiptBuilder.paymentCode(PaymentMethod.INSTAPAY)).isEqualTo("O");
    }

    private static EtaTaxpayerProfile profile() {
        return EtaTaxpayerProfile.builder()
                .environment(EtaEnvironment.PREPROD)
                .rin("123456789")
                .companyTradeName("Test Pharmacy")
                .branchCode("0")
                .activityCode("4772")
                .governate("Cairo")
                .regionCity("Nasr City")
                .street("Abbas El Akkad")
                .buildingNumber("10")
                .build();
    }

    private static SaleTransaction sale(BigDecimal discount, SaleItem... items) {
        SaleTransaction sale = SaleTransaction.builder()
                .invoiceNumber("INV-1001")
                .discountAmount(discount)
                .paymentMethod(PaymentMethod.CASH)
                .transactionDate(LocalDateTime.of(2026, 9, 30, 10, 0))
                .items(new ArrayList<>(List.of(items)))
                .build();
        sale.calculateTotals();
        return sale;
    }

    private static SaleItem item(Long id, String name, String barcode, String unit, int qty, String price) {
        return item(id, name, null, null, unit, qty, price, barcode);
    }

    private static SaleItem item(Long id, String name, String etaType, String etaCode, String unit, int qty, String price) {
        return item(id, name, etaType, etaCode, unit, qty, price, null);
    }

    private static SaleItem item(Long id, String name, String etaType, String etaCode, String unit, int qty,
                                 String price, String barcode) {
        Product product = Product.builder().id(id).name(name).barcode(barcode).unitType(unit)
                .etaItemType(etaType).etaItemCode(etaCode).build();
        BigDecimal unitPrice = new BigDecimal(price);
        return SaleItem.builder().product(product).quantity(qty).unitPrice(unitPrice)
                .totalPrice(unitPrice.multiply(BigDecimal.valueOf(qty))).build();
    }
}
