package com.smartpharma.einvoice.simulator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartpharma.einvoice.util.EtaCanonicalSerializer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A local stand-in for the three ETA eReceipt endpoints SmartPharma calls,
 * answering in the shapes the SDK documents:
 * <ul>
 *   <li>POST /connect/token - client_credentials plus the four POS headers
 *       (ereceiptapi/01-authenticate-pos)</li>
 *   <li>POST /api/v1/receiptsubmissions - 202 with accepted/rejected documents
 *       (ereceiptapi/02-submit-receipt)</li>
 *   <li>GET /api/v1/receiptsubmissions/{uuid}/details - InProgress on the first
 *       look, then Valid/Invalid per receipt (ereceiptapi/06-get-receipt-submission)</li>
 * </ul>
 * Validation is written here independently of EtaReceiptBuilder: structure at
 * submission time, then the "Main Calculations" rules (with ETA's ±0.5
 * tolerance), the per-device previousUUID chain and return references during
 * validation. Only the UUID check reuses the production serializer, since the
 * point there is that the stored text still hashes to its UUID.
 */
public class EtaSimulator implements AutoCloseable {

    public static final String CLIENT_ID = "sim-client";
    public static final String CLIENT_SECRET = "sim-secret";
    public static final String POS_SERIAL = "SIM-POS-1";
    public static final String PRESHARED_KEY = "sim-preshared-key";

    private static final BigDecimal TOLERANCE = new BigDecimal("0.5");
    private static final Pattern DETAILS = Pattern.compile("/api/v1/receiptsubmissions/([^/]+)/details");

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
    private final HttpServer server;

    private final Map<String, String> serialByToken = new HashMap<>();
    private final Map<String, String> chainHeadBySerial = new HashMap<>();
    private final Set<String> knownUuids = new HashSet<>();
    private final Map<String, List<ObjectNode>> outcomesBySubmission = new LinkedHashMap<>();
    private final Set<String> detailsSeenOnce = new HashSet<>();

    private final AtomicInteger failNextSubmissions = new AtomicInteger();
    private volatile int failStatus = 503;
    public final AtomicInteger tokenRequests = new AtomicInteger();
    public final AtomicInteger submissionRequests = new AtomicInteger();

    public EtaSimulator() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/connect/token", this::token);
        server.createContext("/api/v1/receiptsubmissions", this::receipts);
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** The next {@code count} submissions answer with {@code status} (e.g. 503) instead of being processed. */
    public void failNextSubmissions(int count, int status) {
        this.failStatus = status;
        this.failNextSubmissions.set(count);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    // ---- POST /connect/token ------------------------------------------------

    private void token(HttpExchange ex) throws IOException {
        tokenRequests.incrementAndGet();
        Map<String, String> form = parseForm(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        if (!"client_credentials".equals(form.get("grant_type"))) {
            send(ex, 400, "{\"error\":\"unsupported_grant_type\"}");
            return;
        }
        if (!CLIENT_ID.equals(form.get("client_id")) || !CLIENT_SECRET.equals(form.get("client_secret"))) {
            send(ex, 400, "{\"error\":\"invalid_client\"}");
            return;
        }
        String serial = ex.getRequestHeaders().getFirst("posserial");
        if (!POS_SERIAL.equals(serial)) {
            send(ex, 400, "{\"error\":\"invalid_posserial\"}");
            return;
        }
        if (blank(ex.getRequestHeaders().getFirst("pososversion"))) {
            send(ex, 400, "{\"error\":\"invalid_pososversion\"}");
            return;
        }
        if (blank(ex.getRequestHeaders().getFirst("posmodelframework"))) {
            send(ex, 400, "{\"error\":\"invalid_posmodelframework\"}");
            return;
        }
        if (!PRESHARED_KEY.equals(ex.getRequestHeaders().getFirst("presharedkey"))) {
            send(ex, 400, "{\"error\":\"invalid_presharedkey\"}");
            return;
        }
        String token = "tok-" + UUID.randomUUID();
        synchronized (this) {
            serialByToken.put(token, serial);
        }
        send(ex, 200, "{\"access_token\":\"" + token + "\",\"token_type\":\"Bearer\",\"expires_in\":3600,"
                + "\"scope\":\"InvoicingAPI\"}");
    }

    // ---- /api/v1/receiptsubmissions ------------------------------------------

    private void receipts(HttpExchange ex) throws IOException {
        String serial = authorizedSerial(ex);
        if (serial == null) {
            send(ex, 401, "");
            return;
        }
        String path = ex.getRequestURI().getPath();
        Matcher details = DETAILS.matcher(path);
        if ("GET".equals(ex.getRequestMethod()) && details.matches()) {
            details(ex, details.group(1));
        } else if ("POST".equals(ex.getRequestMethod()) && path.equals("/api/v1/receiptsubmissions")) {
            submit(ex, serial);
        } else {
            send(ex, 404, "");
        }
    }

    private synchronized void submit(HttpExchange ex, String serial) throws IOException {
        submissionRequests.incrementAndGet();
        if (failNextSubmissions.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
            send(ex, failStatus, "{\"error\":{\"message\":\"Simulated outage\"}}");
            return;
        }
        JsonNode batch = mapper.readTree(ex.getRequestBody());
        if (!batch.path("receipts").isArray() || !batch.path("signatures").isArray()) {
            send(ex, 400, "{\"error\":{\"code\":\"BadStructure\",\"message\":\"receipts and signatures are required\"}}");
            return;
        }

        String submissionUuid = "SIM" + UUID.randomUUID().toString().replace("-", "").substring(0, 23).toUpperCase();
        ObjectNode response = mapper.createObjectNode();
        response.put("submissionUUID", submissionUuid);
        ArrayNode accepted = response.putArray("acceptedDocuments");
        ArrayNode rejected = response.putArray("rejectedDocuments");
        List<ObjectNode> outcomes = new ArrayList<>();

        for (JsonNode receipt : batch.path("receipts")) {
            String uuid = receipt.path("header").path("uuid").asText();
            String number = receipt.path("header").path("receiptNumber").asText();
            String structural = structuralError(receipt, serial);
            if (structural != null) {
                ObjectNode r = rejected.addObject();
                r.put("uuid", uuid);
                r.put("receiptNumber", number);
                ObjectNode error = r.putObject("error");
                error.put("message", structural);
                error.put("target", uuid);
                continue;
            }
            ObjectNode a = accepted.addObject();
            a.put("uuid", uuid);
            a.put("longId", "LONG" + uuid.substring(0, 12).toUpperCase());
            a.put("receiptNumber", number);
            outcomes.add(validate((ObjectNode) receipt, serial));
        }
        outcomesBySubmission.put(submissionUuid, outcomes);
        send(ex, 202, mapper.writeValueAsString(response));
    }

    private synchronized void details(HttpExchange ex, String submissionUuid) throws IOException {
        List<ObjectNode> outcomes = outcomesBySubmission.get(submissionUuid);
        if (outcomes == null) {
            send(ex, 404, "");
            return;
        }
        ObjectNode body = mapper.createObjectNode();
        body.put("submissionUuid", submissionUuid);
        body.put("receiptsCount", outcomes.size());
        if (detailsSeenOnce.add(submissionUuid)) {
            body.put("status", "InProgress");
            body.putArray("receipts");
        } else {
            boolean anyInvalid = outcomes.stream().anyMatch(o -> "Invalid".equals(o.path("status").asText()));
            body.put("status", anyInvalid ? "Invalid" : "Valid");
            body.put("invalidReceiptCount", outcomes.stream().filter(o -> "Invalid".equals(o.path("status").asText())).count());
            ArrayNode receipts = body.putArray("receipts");
            outcomes.forEach(receipts::add);
        }
        ObjectNode metadata = body.putObject("metadata");
        metadata.put("totalPages", 1);
        metadata.put("totalCount", outcomes.size());
        metadata.put("currentPageNo", 1);
        send(ex, 200, mapper.writeValueAsString(body));
    }

    // ---- validation --------------------------------------------------------

    // Checked synchronously at submission: a document that fails here is in
    // rejectedDocuments and never gets a validation outcome.
    private String structuralError(JsonNode receipt, String serial) {
        JsonNode header = receipt.path("header");
        if (!header.path("uuid").asText().matches("[0-9a-f]{64}")) {
            return "header.uuid must be a SHA256 hex string";
        }
        String type = receipt.path("documentType").path("receiptType").asText();
        if (!type.equals("s") && !type.equals("r")) {
            return "documentType.receiptType must be s or r";
        }
        if (!"1.2".equals(receipt.path("documentType").path("typeVersion").asText())) {
            return "documentType.typeVersion must be 1.2";
        }
        if (!receipt.path("seller").path("rin").asText().matches("\\d{9}")) {
            return "seller.rin length must be 9 digits";
        }
        if (!serial.equals(receipt.path("seller").path("deviceSerialNumber").asText())) {
            return "seller.deviceSerialNumber doesn't match the authenticated POS";
        }
        if (!receipt.path("itemData").isArray() || receipt.path("itemData").isEmpty()) {
            return "itemData is required";
        }
        return null;
    }

    private ObjectNode validate(ObjectNode receipt, String serial) {
        List<String[]> errors = new ArrayList<>(); // {propertyPath, english, arabic}
        JsonNode header = receipt.path("header");
        String uuid = header.path("uuid").asText();

        ObjectNode unhashed = receipt.deepCopy();
        ((ObjectNode) unhashed.get("header")).put("uuid", "");
        if (!EtaCanonicalSerializer.sha256Hex(EtaCanonicalSerializer.serialize(unhashed)).equals(uuid)) {
            errors.add(new String[]{"header.uuid", "UUID doesn't match the receipt content", "الـ UUID لا يطابق محتوى الإيصال"});
        }
        if (!knownUuids.add(uuid)) {
            errors.add(new String[]{"header.uuid", "Duplicate receipt UUID", "رقم الإيصال مكرر"});
        }
        String expectedPrevious = chainHeadBySerial.getOrDefault(serial, "");
        if (!expectedPrevious.equals(header.path("previousUUID").asText())) {
            errors.add(new String[]{"header.previousUUID", "previousUUID isn't the device's last receipt",
                    "previousUUID لا يطابق آخر إيصال للجهاز"});
        }
        chainHeadBySerial.put(serial, uuid);
        if ("r".equals(receipt.path("documentType").path("receiptType").asText())
                && !knownUuids.contains(header.path("referenceUUID").asText())) {
            errors.add(new String[]{"header.referenceUUID", "referenceUUID isn't a known sales receipt",
                    "referenceUUID لا يشير لإيصال بيع معروف"});
        }

        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal netAmount = BigDecimal.ZERO;
        BigDecimal sumTotals = BigDecimal.ZERO;
        BigDecimal sumT1 = BigDecimal.ZERO;
        int i = 0;
        for (JsonNode line : receipt.path("itemData")) {
            String at = "itemData[" + i++ + "]";
            BigDecimal qty = line.path("quantity").decimalValue();
            BigDecimal totalSale = line.path("totalSale").decimalValue();
            BigDecimal netSale = line.path("netSale").decimalValue();
            BigDecimal t1 = BigDecimal.ZERO;
            for (JsonNode tax : line.path("taxableItems")) {
                if ("T1".equals(tax.path("taxType").asText())) {
                    t1 = tax.path("amount").decimalValue();
                    BigDecimal expected = netSale.multiply(tax.path("rate").decimalValue()).movePointLeft(2);
                    if (off(t1, expected)) {
                        errors.add(new String[]{at + ".taxableItems.amount", "T1 amount != netSale x rate",
                                "قيمة الضريبة لا تساوي صافي البيع × النسبة"});
                    }
                }
            }
            if (off(totalSale, qty.multiply(line.path("unitPrice").decimalValue()))) {
                errors.add(new String[]{at + ".totalSale", "totalSale != quantity x unitPrice", "إجمالي البيع لا يساوي الكمية × سعر الوحدة"});
            }
            if (off(line.path("total").decimalValue(), netSale.add(t1))) {
                errors.add(new String[]{at + ".total", "total != netSale + T1", "الإجمالي لا يساوي صافي البيع + الضريبة"});
            }
            totalSales = totalSales.add(totalSale);
            netAmount = netAmount.add(netSale);
            sumTotals = sumTotals.add(line.path("total").decimalValue());
            sumT1 = sumT1.add(t1);
        }
        BigDecimal extra = BigDecimal.ZERO;
        for (JsonNode discount : receipt.path("extraReceiptDiscountData")) {
            extra = extra.add(discount.path("amount").decimalValue());
        }
        if (off(receipt.path("totalSales").decimalValue(), totalSales)) {
            errors.add(new String[]{"totalSales", "totalSales != sum of totalSale", "إجمالي المبيعات لا يساوي مجموع البنود"});
        }
        if (off(receipt.path("netAmount").decimalValue(), netAmount)) {
            errors.add(new String[]{"netAmount", "netAmount != sum of netSale", "صافي المبلغ لا يساوي مجموع صافي البنود"});
        }
        if (off(receipt.path("totalAmount").decimalValue(), sumTotals.subtract(extra))) {
            errors.add(new String[]{"totalAmount", "totalAmount != sum of total - extra discounts",
                    "إجمالي الإيصال لا يساوي مجموع البنود ناقص الخصم"});
        }
        for (JsonNode taxTotal : receipt.path("taxTotals")) {
            if ("T1".equals(taxTotal.path("taxType").asText()) && off(taxTotal.path("amount").decimalValue(), sumT1)) {
                errors.add(new String[]{"taxTotals", "taxTotals T1 != sum of line T1", "إجمالي الضريبة لا يساوي مجموع ضرائب البنود"});
            }
        }

        ObjectNode outcome = mapper.createObjectNode();
        outcome.put("uuid", uuid);
        outcome.put("receiptNumber", header.path("receiptNumber").asText());
        outcome.put("documentType", receipt.path("documentType").path("receiptType").asText());
        outcome.put("status", errors.isEmpty() ? "Valid" : "Invalid");
        outcome.put("longId", "LONG" + uuid.substring(0, 12).toUpperCase());
        ArrayNode errorList = outcome.putArray("errors");
        for (String[] e : errors) {
            ObjectNode step = errorList.addObject();
            step.put("stepId", "20");
            step.put("stepName", "Step 04");
            ObjectNode error = step.putObject("error");
            error.put("propertyPath", "$." + e[0]);
            error.put("errorCode", "SIM");
            error.put("error", e[1]);
            error.put("errorAr", e[2]);
        }
        return outcome;
    }

    private static boolean off(BigDecimal actual, BigDecimal expected) {
        return actual.subtract(expected).abs().compareTo(TOLERANCE) > 0;
    }

    // ---- plumbing -----------------------------------------------------------

    private synchronized String authorizedSerial(HttpExchange ex) {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        return auth != null && auth.startsWith("Bearer ") ? serialByToken.get(auth.substring(7)) : null;
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> form = new HashMap<>();
        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                form.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return form;
    }

    private static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = ex.getResponseBody()) {
                out.write(bytes);
            }
        }
        ex.close();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
