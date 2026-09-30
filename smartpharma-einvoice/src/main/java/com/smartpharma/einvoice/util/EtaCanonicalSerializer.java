package com.smartpharma.einvoice.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

// ETA "Document Serialization Approach" (JSON variant) - the canonical text
// that receipt UUIDs (and batch signatures) are hashed from. Every property
// name is upper-cased and quoted, every simple value is quoted exactly as it
// appears in the document, and every array element is preceded by the array
// name again. See https://sdk.invoicing.eta.gov.eg/document-serialization-approach/
//
// Numbers must keep the exact text that is sent (10.50 stays 10.50), so the
// JsonNode passed in has to carry BigDecimal values - either built directly
// from BigDecimals or parsed with USE_BIG_DECIMAL_FOR_FLOATS.
public final class EtaCanonicalSerializer {

    private EtaCanonicalSerializer() {
    }

    public static String serialize(JsonNode node) {
        StringBuilder out = new StringBuilder();
        write(node, out);
        return out.toString();
    }

    public static String sha256Hex(String canonical) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static void write(JsonNode node, StringBuilder out) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String name = quote(field.getKey().toUpperCase(Locale.ROOT));
                JsonNode value = field.getValue();
                out.append(name);
                if (value.isArray()) {
                    for (JsonNode element : value) {
                        out.append(name);
                        write(element, out);
                    }
                } else {
                    write(value, out);
                }
            }
            return;
        }
        out.append(quote(simpleValue(node)));
    }

    private static String simpleValue(JsonNode node) {
        if (node.isNull() || node.isMissingNode()) {
            return "";
        }
        if (node.isBigDecimal()) {
            return node.decimalValue().toPlainString();
        }
        return node.asText();
    }

    private static String quote(String value) {
        return "\"" + value + "\"";
    }
}
