package com.smartpharma.einvoice.service.impl;

import com.smartpharma.einvoice.util.EtaCanonicalSerializer;
import com.smartpharma.einvoice.exception.EtaReceiptException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonGenerator;
import com.smartpharma.catalog.entity.Product;
import com.smartpharma.einvoice.entity.EtaTaxpayerProfile;
import com.smartpharma.payments.entity.enums.PaymentMethod;
import com.smartpharma.sales.entity.SaleItem;
import com.smartpharma.sales.entity.SaleTransaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Builds an ETA sales receipt (document type s, version 1.2) for a sale and
// computes its UUID. Structure and rules follow
// https://sdk.invoicing.eta.gov.eg/documents/receipt-v1-2/ and the "Main
// Calculations" page.
//
// VAT (tax type T1) comes from the product, else the pharmacy default; with
// neither set the line has no tax item. Deliberate limits, each rejected with
// a clear message rather than sent wrong: EGP only, a buyer national ID and
// name from 150,000 EGP (ETA's threshold), no per-item discounts (SmartPharma only
// has a sale-level one, sent as extraReceiptDiscountData).
public final class EtaReceiptBuilder {

    // Lowercase as written in both the receipt v1.2 spec ("must be 's'") and
    // the submission API ("s (for receipt), r (for return receipt)").
    public static final String RECEIPT_TYPE = "s";
    public static final String RETURN_RECEIPT_TYPE = "r";
    public static final String TYPE_VERSION = "1.2";
    // ETA: "Maximum allowed days to issue a return receipt is 540 days".
    static final long MAX_RETURN_DAYS = 540;

    // From this total ETA requires the buyer's ID and name on a receipt.
    public static final BigDecimal BUYER_ID_THRESHOLD = new BigDecimal("150000");
    private static final BigDecimal STANDARD_VAT_RATE = new BigDecimal("14.00");
    private static final BigDecimal TOTAL_TOLERANCE = new BigDecimal("0.05");
    private static final int ETA_SCALE = 5;
    private static final DateTimeFormatter ISSUED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final Pattern RIN = Pattern.compile("\\d{9}");
    private static final Pattern MOBILE = Pattern.compile("\\+?\\d{8,15}");
    private static final Map<String, String> UNIT_TYPES = Map.of(
            "BOX", "BOX",
            "BOTTLE", "BO",
            "PACKET", "PA");

    // Exact decimals: the default node factory would turn 10.50 into 10.5,
    // and the UUID is a hash of the text exactly as sent.
    private static final JsonNodeFactory NODES = JsonNodeFactory.withExactBigDecimals(true);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setNodeFactory(NODES)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
            .disable(SerializationFeature.INDENT_OUTPUT);

    private EtaReceiptBuilder() {
    }

    public record BuiltReceipt(String json, String uuid, String receiptNumber, String dateTimeIssued,
                               String totalAmount) {
    }

    public static BuiltReceipt build(SaleTransaction sale, EtaTaxpayerProfile profile, String deviceSerial,
                                     String previousUuid, String referenceOldUuid, String currency,
                                     ZoneId saleZone) {
        List<String> problems = new ArrayList<>();
        validateProfile(profile, problems);
        if (currency != null && !"EGP".equalsIgnoreCase(currency)) {
            problems.add("pharmacy currency is " + currency + " - only EGP receipts are supported");
        }
        if (sale.getInvoiceNumber() == null || sale.getInvoiceNumber().length() > 50) {
            problems.add("sale invoice number is missing or longer than 50 characters");
        }
        if (sale.getItems() == null || sale.getItems().isEmpty()) {
            problems.add("sale has no items");
        }
        if (!problems.isEmpty()) {
            throw new EtaReceiptException(String.join("; ", problems));
        }

        String dateTimeIssued = sale.getTransactionDate().atZone(saleZone)
                .withZoneSameInstant(ZoneOffset.UTC).format(ISSUED_FORMAT);

        ObjectNode receipt = NODES.objectNode();

        ObjectNode header = receipt.putObject("header");
        header.put("dateTimeIssued", dateTimeIssued);
        header.put("receiptNumber", sale.getInvoiceNumber());
        header.put("uuid", "");
        header.put("previousUUID", previousUuid == null ? "" : previousUuid);
        if (referenceOldUuid != null && !referenceOldUuid.isBlank()) {
            header.put("referenceOldUUID", referenceOldUuid);
        }
        header.put("currency", "EGP");

        ObjectNode documentType = receipt.putObject("documentType");
        documentType.put("receiptType", RECEIPT_TYPE);
        documentType.put("typeVersion", TYPE_VERSION);

        ObjectNode seller = receipt.putObject("seller");
        seller.put("rin", profile.getRin());
        seller.put("companyTradeName", profile.getCompanyTradeName());
        seller.put("branchCode", profile.getBranchCode());
        ObjectNode address = seller.putObject("branchAddress");
        address.put("country", "EG");
        address.put("governate", profile.getGovernate());
        address.put("regionCity", profile.getRegionCity());
        address.put("street", profile.getStreet());
        address.put("buildingNumber", profile.getBuildingNumber());
        if (notBlank(profile.getPostalCode())) {
            address.put("postalCode", profile.getPostalCode());
        }
        seller.put("deviceSerialNumber", deviceSerial);
        seller.put("activityCode", profile.getActivityCode());

        ObjectNode buyer = receipt.putObject("buyer");
        buyer.put("type", "P");
        if (notBlank(sale.getBuyerNationalId())) {
            buyer.put("id", sale.getBuyerNationalId());
        }
        if (notBlank(sale.getBuyerName())) {
            buyer.put("name", truncate(sale.getBuyerName(), 100));
        }
        String phone = sale.getCustomerPhone() == null ? null : sale.getCustomerPhone().replaceAll("[\\s-]", "");
        if (phone != null && MOBILE.matcher(phone).matches()) {
            buyer.put("mobileNumber", phone);
        }

        ArrayNode itemData = receipt.putArray("itemData");
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal netAmount = BigDecimal.ZERO;
        BigDecimal linesTotal = BigDecimal.ZERO;
        BigDecimal vatTotal = BigDecimal.ZERO;
        boolean anyVatLine = false;
        for (SaleItem item : sale.getItems()) {
            Product product = item.getProduct();
            String[] code = itemCode(product, problems);
            Vat vat = vat(product, profile, problems);
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                problems.add("item " + product.getName() + " has a non-positive quantity");
                continue;
            }
            Line calc = line(item.getQuantity(), item.getUnitPrice(), vat);

            ObjectNode line = itemData.addObject();
            line.put("internalCode", String.valueOf(product.getId()));
            line.put("description", truncate(product.getName(), 500));
            line.put("itemType", code[0]);
            line.put("itemCode", code[1]);
            line.put("unitType", UNIT_TYPES.getOrDefault(upper(product.getUnitType()), "EA"));
            line.put("quantity", BigDecimal.valueOf(item.getQuantity()));
            line.put("unitPrice", calc.unitPrice());
            line.put("netSale", calc.netSale());
            line.put("totalSale", calc.netSale());
            line.put("total", calc.total());
            if (vat != null) {
                ObjectNode taxable = line.putArray("taxableItems").addObject();
                taxable.put("taxType", "T1");
                taxable.put("amount", calc.vat());
                taxable.put("subType", vat.subtype());
                taxable.put("rate", vat.rate());
                anyVatLine = true;
            }
            totalSales = totalSales.add(calc.netSale());
            netAmount = netAmount.add(calc.netSale());
            vatTotal = vatTotal.add(calc.vat());
            linesTotal = linesTotal.add(calc.total());
        }

        BigDecimal discount = money(sale.getDiscountAmount() == null ? BigDecimal.ZERO : sale.getDiscountAmount());
        BigDecimal totalAmount = linesTotal.subtract(discount);
        if (discount.signum() < 0 || totalAmount.signum() < 0) {
            problems.add("sale discount " + discount + " is negative or larger than the items total " + linesTotal);
        }
        if (totalAmount.compareTo(BUYER_ID_THRESHOLD) >= 0
                && (!notBlank(sale.getBuyerNationalId()) || !notBlank(sale.getBuyerName()))) {
            problems.add("sales of 150,000 EGP or more need the buyer's national ID and name");
        }
        // Splitting VAT out of tax-inclusive prices rounds each line to 5
        // decimals, so the receipt total can drift a fraction of a piaster
        // from the sale total - more than that means the sale itself is off.
        if (sale.getTotalAmount() != null
                && money(sale.getTotalAmount()).subtract(totalAmount).abs().compareTo(TOTAL_TOLERANCE) > 0) {
            problems.add("sale total " + sale.getTotalAmount() + " doesn't match its items minus discount (" + totalAmount + ")");
        }
        if (!problems.isEmpty()) {
            throw new EtaReceiptException(String.join("; ", problems));
        }

        receipt.put("totalSales", totalSales);
        if (discount.signum() > 0) {
            ObjectNode extra = receipt.putArray("extraReceiptDiscountData").addObject();
            extra.put("amount", discount);
            extra.put("description", "Discount");
        }
        receipt.put("netAmount", netAmount);
        receipt.put("totalAmount", totalAmount);
        if (anyVatLine) {
            ObjectNode taxTotal = receipt.putArray("taxTotals").addObject();
            taxTotal.put("taxType", "T1");
            taxTotal.put("amount", vatTotal);
        }
        receipt.put("paymentMethod", paymentCode(sale.getPaymentMethod()));

        String uuid = EtaCanonicalSerializer.sha256Hex(EtaCanonicalSerializer.serialize(receipt));
        header.put("uuid", uuid);

        return new BuiltReceipt(toJson(receipt), uuid, sale.getInvoiceNumber(), dateTimeIssued,
                totalAmount.toPlainString());
    }

    // Return receipt (type r, v1.2) reversing a whole issued sales receipt -
    // https://sdk.invoicing.eta.gov.eg/documents/return-receipt-v1-2/ has the
    // same structure plus header.referenceUUID. It's built from the original's
    // stored text, not from the (now cancelled) sale, so items, VAT and totals
    // are exactly what ETA already has for the original.
    public static BuiltReceipt buildReturn(String originalJson, String originalUuid, String receiptNumber,
                                           String previousUuid, String referenceOldUuid, Instant issuedAt) {
        ObjectNode original;
        try {
            original = (ObjectNode) MAPPER.readTree(originalJson);
        } catch (JsonProcessingException e) {
            throw new EtaReceiptException("original receipt text can't be read: " + e.getOriginalMessage());
        }
        JsonNode originalHeader = original.path("header");
        String originalIssued = originalHeader.path("dateTimeIssued").asText();
        String dateTimeIssued = ISSUED_FORMAT.format(issuedAt.atOffset(ZoneOffset.UTC));
        if (Duration.between(Instant.parse(originalIssued), issuedAt).toDays() > MAX_RETURN_DAYS) {
            throw new EtaReceiptException("the sale is older than " + MAX_RETURN_DAYS
                    + " days, the latest ETA accepts a return receipt for");
        }
        if (receiptNumber.length() > 50) {
            throw new EtaReceiptException("return receipt number is longer than 50 characters");
        }

        ObjectNode receipt = NODES.objectNode();
        ObjectNode header = receipt.putObject("header");
        header.put("dateTimeIssued", dateTimeIssued);
        header.put("receiptNumber", receiptNumber);
        header.put("uuid", "");
        header.put("previousUUID", previousUuid == null ? "" : previousUuid);
        header.put("referenceUUID", originalUuid);
        if (referenceOldUuid != null && !referenceOldUuid.isBlank()) {
            header.put("referenceOldUUID", referenceOldUuid);
        }
        header.set("currency", originalHeader.path("currency"));

        ObjectNode documentType = receipt.putObject("documentType");
        documentType.put("receiptType", RETURN_RECEIPT_TYPE);
        documentType.put("typeVersion", TYPE_VERSION);

        original.fields().forEachRemaining(field -> {
            if (!field.getKey().equals("header") && !field.getKey().equals("documentType")) {
                receipt.set(field.getKey(), field.getValue());
            }
        });

        String uuid = EtaCanonicalSerializer.sha256Hex(EtaCanonicalSerializer.serialize(receipt));
        header.put("uuid", uuid);
        return new BuiltReceipt(toJson(receipt), uuid, receiptNumber, dateTimeIssued,
                original.path("totalAmount").decimalValue().toPlainString());
    }

    // VAT for one product: its own subtype, else the pharmacy default; null
    // when neither is set (receipt carries no tax line for it).
    record Vat(String subtype, BigDecimal rate) {
    }

    record Line(BigDecimal unitPrice, BigDecimal netSale, BigDecimal vat, BigDecimal total) {
    }

    static Vat vat(Product product, EtaTaxpayerProfile profile, List<String> problems) {
        boolean own = notBlank(product.getEtaTaxSubtype());
        String subtype = own ? product.getEtaTaxSubtype() : profile.getDefaultTaxSubtype();
        if (!notBlank(subtype)) {
            return null;
        }
        return switch (subtype) {
            case "V009" -> new Vat(subtype, STANDARD_VAT_RATE);
            case "V003", "V004" -> new Vat(subtype, BigDecimal.ZERO.setScale(2));
            case "V010" -> {
                BigDecimal rate = own ? product.getEtaTaxRate() : profile.getDefaultTaxRate();
                if (rate == null || rate.signum() <= 0) {
                    problems.add("product " + product.getName() + " uses VAT subtype V010 but has no rate");
                    yield null;
                }
                yield new Vat(subtype, rate.setScale(2, RoundingMode.HALF_UP));
            }
            default -> {
                problems.add("product " + product.getName() + " has unknown VAT subtype " + subtype);
                yield null;
            }
        };
    }

    // SmartPharma prices are what the customer pays, i.e. VAT-inclusive, while
    // ETA wants the net unit price with VAT added on top. So the net price is
    // split out of the shelf price (5 decimals, ETA's precision) and
    // total = netSale + VAT lands back on the shelf price within rounding.
    static Line line(int quantity, BigDecimal shelfPrice, Vat vat) {
        BigDecimal qty = BigDecimal.valueOf(quantity);
        if (vat == null || vat.rate().signum() == 0) {
            BigDecimal unit = money(shelfPrice);
            BigDecimal net = money(unit.multiply(qty));
            BigDecimal zero = BigDecimal.ZERO.setScale(ETA_SCALE);
            return new Line(unit, net, zero, net);
        }
        BigDecimal hundred = BigDecimal.valueOf(100);
        BigDecimal unitNet = shelfPrice.multiply(hundred)
                .divide(hundred.add(vat.rate()), ETA_SCALE, RoundingMode.HALF_UP);
        BigDecimal netSale = unitNet.multiply(qty);
        BigDecimal vatAmount = netSale.multiply(vat.rate()).divide(hundred, ETA_SCALE, RoundingMode.HALF_UP);
        return new Line(unitNet, netSale, vatAmount, netSale.add(vatAmount));
    }

    // ETA payment method codes: C cash, V visa, O others.
    static String paymentCode(PaymentMethod method) {
        if (method == null) {
            return "C";
        }
        return switch (method) {
            case CASH -> "C";
            case VISA, MASTERCARD -> "V";
            default -> "O";
        };
    }

    // [itemType, itemCode] - the ETA code set on the product, else a barcode
    // that is a valid GTIN (sent as GS1).
    static String[] itemCode(Product product, List<String> problems) {
        String type = upper(product.getEtaItemType());
        String code = product.getEtaItemCode();
        if (notBlank(code)) {
            if (!"GS1".equals(type) && !"EGS".equals(type)) {
                problems.add("product " + product.getName() + " has ETA code " + code + " but item type isn't GS1 or EGS");
            }
            return new String[]{type, code.trim()};
        }
        String barcode = product.getBarcode() == null ? null : product.getBarcode().trim();
        if (isValidGtin(barcode)) {
            return new String[]{"GS1", barcode};
        }
        problems.add("product " + product.getName() + " has no ETA item code and no valid GTIN barcode");
        return new String[]{"", ""};
    }

    static boolean isValidGtin(String value) {
        if (value == null || !value.matches("\\d{8}|\\d{12,14}")) {
            return false;
        }
        int sum = 0;
        for (int i = value.length() - 2, weight = 3; i >= 0; i--, weight = 4 - weight) {
            sum += (value.charAt(i) - '0') * weight;
        }
        return (10 - sum % 10) % 10 == value.charAt(value.length() - 1) - '0';
    }

    private static void validateProfile(EtaTaxpayerProfile profile, List<String> problems) {
        if (profile.getRin() == null || !RIN.matcher(profile.getRin()).matches()) {
            problems.add("tax registration number (RIN) must be 9 digits");
        }
        require(profile.getCompanyTradeName(), "company trade name", problems);
        require(profile.getBranchCode(), "branch code", problems);
        require(profile.getActivityCode(), "activity code", problems);
        require(profile.getGovernate(), "governorate", problems);
        require(profile.getRegionCity(), "region/city", problems);
        require(profile.getStreet(), "street", problems);
        require(profile.getBuildingNumber(), "building number", problems);
    }

    private static void require(String value, String label, List<String> problems) {
        if (!notBlank(value)) {
            problems.add(label + " is required");
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private static String toJson(ObjectNode receipt) {
        try {
            return MAPPER.writeValueAsString(receipt);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to write ETA receipt JSON", e);
        }
    }
}
