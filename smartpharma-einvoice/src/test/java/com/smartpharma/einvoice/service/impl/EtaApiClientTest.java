package com.smartpharma.einvoice.service.impl;

import com.smartpharma.einvoice.entity.enums.EtaEnvironment;
import com.smartpharma.einvoice.util.EtaQrCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EtaApiClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // Shape from https://sdk.invoicing.eta.gov.eg/ereceiptapi/02-submit-receipt/
    @Test
    void parsesAcceptedAndRejectedDocuments() throws Exception {
        String body = """
                {"submissionUUID":"TZRKK8MFZCPSTW9XCYWBMKME10",
                 "acceptedDocuments":[{"uuid":"aaa","longId":"LONG1","receiptNumber":"INV-1"}],
                 "rejectedDocuments":[{"uuid":"bbb","receiptNumber":"INV-2",
                   "error":{"message":"Validation failed","propertyPath":"receipt",
                            "details":[{"message":"Issuance date time value is out of the range","propertyPath":"header.dateTimeIssued"}]}}]}
                """;

        EtaApiClient.SubmitResult result = new EtaApiClient().parseSubmission(mapper.readTree(body));

        assertThat(result.failure()).isNull();
        assertThat(result.submissionUuid()).isEqualTo("TZRKK8MFZCPSTW9XCYWBMKME10");
        assertThat(result.accepted()).singleElement().satisfies(a -> {
            assertThat(a.uuid()).isEqualTo("aaa");
            assertThat(a.longId()).isEqualTo("LONG1");
        });
        assertThat(result.rejected()).singleElement().satisfies(r -> {
            assertThat(r.uuid()).isEqualTo("bbb");
            assertThat(r.message()).contains("Validation failed").contains("header.dateTimeIssued");
        });
    }

    // Shape from https://sdk.invoicing.eta.gov.eg/ereceiptapi/06-get-receipt-submission/
    @Test
    void parsesSubmissionDetailsWithArabicErrorsFirst() throws Exception {
        String body = """
                {"submissionUuid":"SUB1","status":"Invalid",
                 "receipts":[
                   {"uuid":"aaa","status":"Valid","errors":[]},
                   {"uuid":"bbb","status":"Invalid","errors":[{"stepId":"20","stepName":"Step 04",
                     "error":{"propertyPath":"$.itemData[*].taxableItems[*].taxType","errorCode":"CV307",
                              "error":"ItemCode [W001] doesn't belong to ParentCode [T3]",
                              "errorAr":"ItemCode [W001] لا ينتمي إلى ParentCode [T3]"}}]}],
                 "metadata":{"totalPages":2,"totalCount":150,"currentPageNo":1}}
                """;

        EtaApiClient.SubmissionDetails details = new EtaApiClient().parseSubmissionDetails(mapper.readTree(body));

        assertThat(details.failure()).isNull();
        assertThat(details.overallStatus()).isEqualTo("Invalid");
        assertThat(details.totalPages()).isEqualTo(2);
        assertThat(details.receipts()).hasSize(2);
        assertThat(details.receipts().get(0).errors()).isNull();
        assertThat(details.receipts().get(1).errors())
                .startsWith("ItemCode [W001] لا ينتمي")
                .contains("doesn't belong")
                .contains("$.itemData[*].taxableItems[*].taxType");
    }

    @Test
    void cipherRoundTripsAndRefusesWithoutKey() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        EtaSecretCipher cipher = new EtaSecretCipher(key);

        String stored = cipher.encrypt("s3cret");
        assertThat(stored).doesNotContain("s3cret");
        assertThat(cipher.decrypt(stored)).isEqualTo("s3cret");
        assertThat(cipher.encrypt("s3cret")).isNotEqualTo(stored);

        EtaSecretCipher missing = new EtaSecretCipher("");
        assertThat(missing.isConfigured()).isFalse();
        assertThatThrownBy(() -> missing.encrypt("x")).hasMessageContaining("ETA_CREDENTIALS_KEY");
    }

    @Test
    void qrContentFollowsEtaTemplate() {
        assertThat(EtaQrCode.content(EtaEnvironment.PROD, "abc", "2026-01-15T12:30:05Z", "50.00", "123456789"))
                .isEqualTo("https://invoicing.eta.gov.eg/receipts/search/abc/share/2026-01-15T12:30:05Z"
                        + "#Total:50.00,IssuerRIN:123456789");
    }
}
