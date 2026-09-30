package com.smartpharma.einvoice.service.impl;

import com.smartpharma.einvoice.service.impl.EtaReceiptSubmitter.Pending;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EtaReceiptSubmitterTest {

    @Test
    void splitsBatchesAt500ReceiptsKeepingOrder() {
        List<Pending> pending = new ArrayList<>();
        for (long i = 0; i < 1201; i++) {
            pending.add(new Pending(i, "uuid-" + i, "{\"n\":" + i + "}"));
        }

        List<List<Pending>> batches = EtaReceiptSubmitter.batches(pending);

        assertThat(batches).extracting(List::size).containsExactly(500, 500, 201);
        assertThat(batches.get(1).get(0)).isSameAs(pending.get(500));
    }

    @Test
    void splitsBatchesBeforeExceeding1_5MB() {
        String big = "{\"x\":\"" + "a".repeat(400_000) + "\"}";
        List<Pending> pending = new ArrayList<>();
        for (long i = 0; i < 5; i++) {
            pending.add(new Pending(i, "uuid-" + i, big));
        }

        assertThat(EtaReceiptSubmitter.batches(pending)).extracting(List::size).containsExactly(3, 2);
    }

    @Test
    void batchJsonSplicesStoredReceiptsVerbatim() {
        List<Pending> batch = List.of(new Pending(1L, "u1", "{\"a\":10.50}"), new Pending(2L, "u2", "{\"b\":1}"));

        assertThat(EtaReceiptSubmitter.batchJson(batch))
                .isEqualTo("{\"receipts\":[{\"a\":10.50},{\"b\":1}],\"signatures\":[]}");
    }
}
