package com.smartpharma.einvoice.service.impl;

import com.smartpharma.einvoice.entity.enums.EtaEnvironment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

// ETA eReceipt HTTP calls:
//  - POST {identity}/connect/token  (client_credentials + POS headers)
//  - POST {api}/api/v1/receiptsubmissions
//  - GET  {api}/api/v1/receiptsubmissions/{uuid}/details  (validation outcome)
// See https://sdk.invoicing.eta.gov.eg/ereceiptapi/01-authenticate-pos/ and
// https://sdk.invoicing.eta.gov.eg/ereceiptapi/02-submit-receipt/
@Component
@Slf4j
public class EtaApiClient {

    // Refresh a bit early so a token can't expire mid-request.
    private static final long TOKEN_SAFETY_SECONDS = 60;
    private static final int DETAILS_PAGE_SIZE = 100;

    private final RestClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, CachedToken> tokens = new ConcurrentHashMap<>();

    public EtaApiClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    public record PosCredentials(EtaEnvironment environment, String clientId, String clientSecret,
                                 String posSerial, String posOsVersion, String posModelFramework,
                                 String presharedKey) {
    }

    public record Accepted(String uuid, String longId, String receiptNumber) {
    }

    public record Rejected(String uuid, String receiptNumber, String message) {
    }

    // retryable = the batch never got a verdict (network, 5xx, throttling,
    // duplicate-in-10-minutes); otherwise ETA answered and the lists apply.
    public record SubmitResult(String submissionUuid, List<Accepted> accepted, List<Rejected> rejected,
                               String failure, boolean retryable) {
        static SubmitResult failed(String message, boolean retryable) {
            return new SubmitResult(null, List.of(), List.of(), message, retryable);
        }
    }

    public String authenticate(PosCredentials credentials) {
        String cacheKey = cacheKey(credentials);
        CachedToken cached = tokens.get(cacheKey);
        if (cached != null && cached.validAt(Instant.now())) {
            return cached.value();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", credentials.clientId());
        form.add("client_secret", credentials.clientSecret());

        JsonNode body;
        try {
            body = http.post()
                    .uri(credentials.environment().identityUrl() + "/connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("posserial", credentials.posSerial())
                    .header("pososversion", credentials.posOsVersion())
                    .header("posmodelframework", credentials.posModelFramework())
                    .header("presharedkey", credentials.presharedKey())
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw new EtaAuthenticationException(describe(e), e.getStatusCode().is5xxServerError());
        } catch (ResourceAccessException e) {
            throw new EtaAuthenticationException("ETA identity service unreachable: " + e.getMessage(), true);
        }

        String token = body == null ? null : body.path("access_token").asText(null);
        if (token == null) {
            throw new EtaAuthenticationException("ETA identity service returned no access_token", true);
        }
        long expiresIn = body.path("expires_in").asLong(3600);
        tokens.put(cacheKey, new CachedToken(token, Instant.now().plusSeconds(Math.max(0, expiresIn - TOKEN_SAFETY_SECONDS))));
        return token;
    }

    public void forget(PosCredentials credentials) {
        tokens.remove(cacheKey(credentials));
    }

    public SubmitResult submitBatch(PosCredentials credentials, String batchJson) {
        String token;
        try {
            token = authenticate(credentials);
        } catch (EtaAuthenticationException e) {
            return SubmitResult.failed(e.getMessage(), e.isRetryable());
        }

        try {
            JsonNode body = http.post()
                    .uri(credentials.environment().apiUrl() + "/api/v1/receiptsubmissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .body(batchJson)
                    .retrieve()
                    .body(JsonNode.class);
            return parseSubmission(body);
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 401) {
                forget(credentials);
            }
            // 401 (stale token), 422 DuplicateSubmission, 429 throttling and
            // 5xx are worth another try; other 4xx mean the batch itself is bad.
            boolean retryable = status.value() == 401 || status.value() == 422 || status.value() == 429
                    || status.is5xxServerError();
            return SubmitResult.failed(describe(e), retryable);
        } catch (ResourceAccessException e) {
            return SubmitResult.failed("ETA API unreachable: " + e.getMessage(), true);
        }
    }

    // Validation outcome of one receipt from GET receiptsubmissions/{uuid}/details.
    public record ReceiptOutcome(String uuid, String status, String errors) {
    }

    // overallStatus: InProgress, Valid or Invalid. failure set = no answer.
    public record SubmissionDetails(String overallStatus, List<ReceiptOutcome> receipts, int totalPages,
                                    String failure) {
    }

    public SubmissionDetails getSubmissionDetails(PosCredentials credentials, String submissionUuid, int pageNo) {
        String token;
        try {
            token = authenticate(credentials);
        } catch (EtaAuthenticationException e) {
            return new SubmissionDetails(null, List.of(), 0, e.getMessage());
        }
        try {
            JsonNode body = http.get()
                    .uri(credentials.environment().apiUrl() + "/api/v1/receiptsubmissions/{uuid}/details?PageNo={page}&PageSize={size}",
                            submissionUuid, pageNo, DETAILS_PAGE_SIZE)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(JsonNode.class);
            return parseSubmissionDetails(body);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 401) {
                forget(credentials);
            }
            return new SubmissionDetails(null, List.of(), 0, describe(e));
        } catch (ResourceAccessException e) {
            return new SubmissionDetails(null, List.of(), 0, "ETA API unreachable: " + e.getMessage());
        }
    }

    // Shape from https://sdk.invoicing.eta.gov.eg/ereceiptapi/06-get-receipt-submission/
    SubmissionDetails parseSubmissionDetails(JsonNode body) {
        if (body == null) {
            return new SubmissionDetails(null, List.of(), 0, "ETA returned an empty submission details response");
        }
        List<ReceiptOutcome> receipts = new ArrayList<>();
        for (JsonNode receipt : body.path("receipts")) {
            List<String> errors = new ArrayList<>();
            for (JsonNode step : receipt.path("errors")) {
                JsonNode error = step.path("error");
                String en = error.path("error").asText("");
                String ar = error.path("errorAr").asText("");
                String path = error.path("propertyPath").asText("");
                String text = (ar.isBlank() ? en : ar + (en.isBlank() ? "" : " / " + en))
                        + (path.isBlank() ? "" : " [" + path + "]");
                if (!text.isBlank()) {
                    errors.add(text);
                }
            }
            receipts.add(new ReceiptOutcome(receipt.path("uuid").asText(null), receipt.path("status").asText(null),
                    errors.isEmpty() ? null : String.join("; ", errors)));
        }
        return new SubmissionDetails(body.path("status").asText(null), receipts,
                body.path("metadata").path("totalPages").asInt(1), null);
    }

    SubmitResult parseSubmission(JsonNode body) {
        if (body == null) {
            return SubmitResult.failed("ETA returned an empty submission response", true);
        }
        List<Accepted> accepted = new ArrayList<>();
        for (JsonNode doc : body.path("acceptedDocuments")) {
            accepted.add(new Accepted(doc.path("uuid").asText(null), doc.path("longId").asText(null),
                    doc.path("receiptNumber").asText(null)));
        }
        List<Rejected> rejected = new ArrayList<>();
        for (JsonNode doc : body.path("rejectedDocuments")) {
            rejected.add(new Rejected(doc.path("uuid").asText(null), doc.path("receiptNumber").asText(null),
                    errorText(doc.path("error"))));
        }
        return new SubmitResult(body.path("submissionUUID").asText(null), accepted, rejected, null, false);
    }

    // "message (target): detail; detail" from ETA's nested error object.
    static String errorText(JsonNode error) {
        if (error == null || error.isMissingNode() || error.isNull()) {
            return "Rejected by ETA (no error details)";
        }
        StringBuilder text = new StringBuilder(error.path("message").asText("Rejected by ETA"));
        String path = error.path("propertyPath").asText("");
        if (!path.isBlank()) {
            text.append(" [").append(path).append(']');
        }
        List<String> details = new ArrayList<>();
        for (JsonNode detail : error.path("details")) {
            String message = detail.path("message").asText("");
            String detailPath = detail.path("propertyPath").asText("");
            if (!message.isBlank()) {
                details.add(detailPath.isBlank() ? message : message + " [" + detailPath + "]");
            }
        }
        if (!details.isEmpty()) {
            text.append(": ").append(String.join("; ", details));
        }
        return text.toString();
    }

    private String describe(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        String detail = body;
        try {
            JsonNode json = mapper.readTree(body);
            if (json.has("error") && json.get("error").isObject()) {
                detail = errorText(json.get("error"));
            } else if (json.has("error")) {
                detail = json.path("error").asText() + " " + json.path("error_description").asText("");
            }
        } catch (Exception ignored) {
            // not JSON - keep the raw body
        }
        return "ETA HTTP " + e.getStatusCode().value() + ": " + (detail == null || detail.isBlank() ? e.getStatusText() : detail.trim());
    }

    private static String cacheKey(PosCredentials c) {
        return c.environment() + "|" + c.clientId() + "|" + c.posSerial() + "|"
                + Objects.hash(c.clientSecret(), c.presharedKey(), c.posOsVersion(), c.posModelFramework());
    }

    private record CachedToken(String value, Instant refreshAfter) {
        boolean validAt(Instant now) {
            return now.isBefore(refreshAfter);
        }
    }

    public static class EtaAuthenticationException extends RuntimeException {
        private final boolean retryable;

        public EtaAuthenticationException(String message, boolean retryable) {
            super(message);
            this.retryable = retryable;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }
}
