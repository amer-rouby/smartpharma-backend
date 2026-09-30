package com.smartpharma.einvoice.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class EtaCanonicalSerializerTest {

    // The default node factory strips trailing zeros (10.50 -> 10.5), which
    // would change the canonical text and therefore the hash.
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));

    // one-doc.json / one-doc-serialized.txt are the official sample pair
    // published by ETA with the serialization spec.
    @Test
    void matchesOfficialEtaSerializationSample() throws IOException {
        JsonNode document = mapper.readTree(resource("eta/one-doc.json"));
        String expected = new String(resource("eta/one-doc-serialized.txt"), StandardCharsets.UTF_8).strip();

        assertThat(EtaCanonicalSerializer.serialize(document)).isEqualTo(expected);
    }

    @Test
    void repeatsArrayNameBeforeEveryElementAndKeepsDecimalText() throws IOException {
        JsonNode node = mapper.readTree("{\"a\":{\"b\":10.50},\"items\":[{\"x\":\"1\"},{\"x\":\"2\"}],\"empty\":[]}");

        assertThat(EtaCanonicalSerializer.serialize(node))
                .isEqualTo("\"A\"\"B\"\"10.50\"\"ITEMS\"\"ITEMS\"\"X\"\"1\"\"ITEMS\"\"X\"\"2\"\"EMPTY\"");
    }

    @Test
    void sha256HexIsLowercase64Chars() {
        assertThat(EtaCanonicalSerializer.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    private byte[] resource(String path) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as(path).isNotNull();
            return in.readAllBytes();
        }
    }
}
