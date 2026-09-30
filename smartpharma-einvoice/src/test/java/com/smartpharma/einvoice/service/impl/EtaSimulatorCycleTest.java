package com.smartpharma.einvoice.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartpharma.catalog.entity.Product;
import com.smartpharma.einvoice.entity.EInvoiceSubmission;
import com.smartpharma.einvoice.entity.EtaPosDevice;
import com.smartpharma.einvoice.entity.EtaTaxpayerProfile;
import com.smartpharma.einvoice.entity.enums.EtaEnvironment;
import com.smartpharma.einvoice.repository.EInvoiceSubmissionRepository;
import com.smartpharma.einvoice.repository.EtaPosDeviceRepository;
import com.smartpharma.einvoice.simulator.EtaSimulator;
import com.smartpharma.einvoice.util.EtaCanonicalSerializer;
import com.smartpharma.payments.entity.enums.PaymentMethod;
import com.smartpharma.sales.entity.SaleItem;
import com.smartpharma.sales.entity.SaleTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// The whole send/validate cycle over real HTTP against EtaSimulator: real
// EtaApiClient, EtaReceiptSubmitter and EtaReceiptStatusPoller; only the
// database is replaced by an in-memory list behind mocked repositories.
class EtaSimulatorCycleTest {

    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");

    private EtaSimulator eta;
    private EtaApiClient client;
    private EtaApiClient.PosCredentials credentials;
    private final List<EInvoiceSubmission> rows = new ArrayList<>();
    private final EtaPosDevice device = EtaPosDevice.builder().id(1L).serialNumber(EtaSimulator.POS_SERIAL)
            .osVersion("1.0").modelFramework("1").build();
    private EtaReceiptSubmitter submitter;
    private EtaReceiptStatusPoller poller;
    private String chainHead = "";

    @BeforeEach
    void startSimulator() throws Exception {
        eta = new EtaSimulator();
        client = new EtaApiClient(url -> eta.baseUrl());
        credentials = new EtaApiClient.PosCredentials(EtaEnvironment.PREPROD, EtaSimulator.CLIENT_ID,
                EtaSimulator.CLIENT_SECRET, EtaSimulator.POS_SERIAL, "1.0", "1", EtaSimulator.PRESHARED_KEY);

        EInvoiceSubmissionRepository repository = mock(EInvoiceSubmissionRepository.class);
        when(repository.findDeliverable(eq(1L), any(), anyInt())).thenAnswer(inv -> {
            Collection<?> statuses = inv.getArgument(1);
            int max = inv.getArgument(2);
            return rows.stream().filter(r -> statuses.contains(r.getStatus()) && r.getReceiptJson() != null
                    && r.getRetryCount() < max).toList();
        });
        when(repository.findAllById(anyIterable())).thenAnswer(inv -> {
            List<Long> ids = new ArrayList<>();
            ((Iterable<Long>) inv.getArgument(0)).forEach(ids::add);
            return rows.stream().filter(r -> ids.contains(r.getId())).toList();
        });
        when(repository.findSubmittedBySubmissionUuid(anyString())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getStatus() == EInvoiceSubmission.Status.SUBMITTED
                        && inv.getArgument(0).equals(r.getSubmissionUuid())).toList());
        EtaPosDeviceRepository devices = mock(EtaPosDeviceRepository.class);
        when(devices.findById(1L)).thenReturn(Optional.of(device));
        EtaCredentialsResolver resolver = mock(EtaCredentialsResolver.class);
        when(resolver.resolve(device)).thenReturn(credentials);

        TransactionTemplate tx = new TransactionTemplate(new NoTransactions());
        submitter = new EtaReceiptSubmitter(repository, devices, resolver, mock(EtaSecretCipher.class), client, tx, 50);
        poller = new EtaReceiptStatusPoller(repository, devices, resolver, mock(EtaSecretCipher.class), client, tx);
    }

    @AfterEach
    void stopSimulator() {
        eta.close();
    }

    @Test
    void authenticatesWithPosHeadersAndReusesTheToken() {
        String first = client.authenticate(credentials);
        String second = client.authenticate(credentials);

        assertThat(first).startsWith("tok-").isEqualTo(second);
        assertThat(eta.tokenRequests.get()).isEqualTo(1);

        EtaApiClient.PosCredentials wrongSecret = new EtaApiClient.PosCredentials(EtaEnvironment.PREPROD,
                EtaSimulator.CLIENT_ID, "nope", EtaSimulator.POS_SERIAL, "1.0", "1", EtaSimulator.PRESHARED_KEY);
        assertThatThrownBy(() -> client.authenticate(wrongSecret))
                .isInstanceOf(EtaApiClient.EtaAuthenticationException.class)
                .hasMessageContaining("invalid_client");

        EtaApiClient.PosCredentials wrongKey = new EtaApiClient.PosCredentials(EtaEnvironment.PREPROD,
                EtaSimulator.CLIENT_ID, EtaSimulator.CLIENT_SECRET, EtaSimulator.POS_SERIAL, "1.0", "1", "nope");
        assertThatThrownBy(() -> client.authenticate(wrongKey)).hasMessageContaining("invalid_presharedkey");
    }

    @Test
    void salesAndAReturnAreSubmittedThenValidated() {
        EtaReceiptBuilder.BuiltReceipt first = issueSale("INV-1", "2", "11.40", "1.00");
        issueSale("INV-2", "1", "18.75", "0");
        EInvoiceSubmission returned = issue(EtaReceiptBuilder.buildReturn(first.json(), first.uuid(), "R-INV-1",
                chainHead, null, Instant.now()), EInvoiceSubmission.DocumentType.RETURN);

        submitter.deliverDevice(1L);

        assertThat(rows).allSatisfy(r -> {
            assertThat(r.getStatus()).isEqualTo(EInvoiceSubmission.Status.SUBMITTED);
            assertThat(r.getLongId()).startsWith("LONG");
        });
        String submission = rows.get(0).getSubmissionUuid();
        assertThat(rows).extracting(EInvoiceSubmission::getSubmissionUuid).containsOnly(submission);

        poller.poll(1L, submission); // first look: ETA still validating
        assertThat(rows).extracting(EInvoiceSubmission::getStatus).containsOnly(EInvoiceSubmission.Status.SUBMITTED);

        poller.poll(1L, submission);
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.getStatus()).as(r.getReceiptNumber() + ": " + r.getErrorMessage())
                    .isEqualTo(EInvoiceSubmission.Status.ACCEPTED);
            assertThat(r.getErrorMessage()).isNull();
        });
        assertThat(returned.getReceiptNumber()).isEqualTo("R-INV-1");
    }

    @Test
    void aReceiptWithWrongTotalsIsRejectedWithEtasReasonInArabic() throws Exception {
        EtaReceiptBuilder.BuiltReceipt good = build("INV-1", "1", "10.00", "0");
        // Same receipt with its total bumped, re-hashed so only the math is wrong.
        ObjectMapper exact = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
        ObjectNode tampered = (ObjectNode) exact.readTree(good.json());
        tampered.put("totalAmount", new BigDecimal("99.00"));
        ((ObjectNode) tampered.get("header")).put("uuid", "");
        String uuid = EtaCanonicalSerializer.sha256Hex(EtaCanonicalSerializer.serialize(tampered));
        ((ObjectNode) tampered.get("header")).put("uuid", uuid);
        issue(new EtaReceiptBuilder.BuiltReceipt(exact.writeValueAsString(tampered), uuid, "INV-1",
                good.dateTimeIssued(), "99.00"), EInvoiceSubmission.DocumentType.SALE);

        submitter.deliverDevice(1L);
        String submission = rows.get(0).getSubmissionUuid();
        poller.poll(1L, submission);
        poller.poll(1L, submission);

        EInvoiceSubmission row = rows.get(0);
        assertThat(row.getStatus()).isEqualTo(EInvoiceSubmission.Status.REJECTED);
        assertThat(row.getErrorMessage()).startsWith("إجمالي الإيصال لا يساوي").contains("$.totalAmount");
    }

    @Test
    void anOutageIsRetriedAndThenGoesThrough() {
        issueSale("INV-1", "1", "10.00", "0");
        eta.failNextSubmissions(1, 503);

        submitter.deliverDevice(1L);
        EInvoiceSubmission row = rows.get(0);
        assertThat(row.getStatus()).isEqualTo(EInvoiceSubmission.Status.ERROR);
        assertThat(row.getRetryCount()).isEqualTo(1);
        assertThat(row.getErrorMessage()).contains("503");

        submitter.deliverDevice(1L);
        assertThat(row.getStatus()).isEqualTo(EInvoiceSubmission.Status.SUBMITTED);
        assertThat(eta.submissionRequests.get()).isEqualTo(2);
    }

    @Test
    void aBrokenChainIsCaughtByValidation() {
        issueSale("INV-1", "1", "10.00", "0");
        chainHead = "f".repeat(64); // as if a receipt in between had been lost
        issueSale("INV-2", "1", "12.00", "0");

        submitter.deliverDevice(1L);
        String submission = rows.get(0).getSubmissionUuid();
        poller.poll(1L, submission);
        poller.poll(1L, submission);

        assertThat(rows.get(0).getStatus()).isEqualTo(EInvoiceSubmission.Status.ACCEPTED);
        assertThat(rows.get(1).getStatus()).isEqualTo(EInvoiceSubmission.Status.REJECTED);
        assertThat(rows.get(1).getErrorMessage()).contains("previousUUID");
    }

    // --- helpers -------------------------------------------------------------

    private EtaReceiptBuilder.BuiltReceipt issueSale(String number, String qty, String price, String discount) {
        EtaReceiptBuilder.BuiltReceipt built = build(number, qty, price, discount);
        issue(built, EInvoiceSubmission.DocumentType.SALE);
        return built;
    }

    private EtaReceiptBuilder.BuiltReceipt build(String number, String qty, String price, String discount) {
        Product product = Product.builder().id(1L).name("Item " + number).barcode("6221000000010").unitType("BOX").build();
        BigDecimal unit = new BigDecimal(price);
        SaleItem item = SaleItem.builder().product(product).quantity(Integer.parseInt(qty)).unitPrice(unit)
                .totalPrice(unit.multiply(new BigDecimal(qty))).build();
        SaleTransaction sale = SaleTransaction.builder().invoiceNumber(number).discountAmount(new BigDecimal(discount))
                .paymentMethod(PaymentMethod.CASH).transactionDate(LocalDateTime.now(CAIRO).minusMinutes(5))
                .items(new ArrayList<>(List.of(item))).build();
        sale.calculateTotals();
        EtaTaxpayerProfile profile = EtaTaxpayerProfile.builder().environment(EtaEnvironment.PREPROD)
                .rin("123456789").companyTradeName("Sim Pharmacy").branchCode("0").activityCode("4773")
                .governate("Cairo").regionCity("Nasr City").street("Street").buildingNumber("1")
                .defaultTaxSubtype("V009").build();
        return EtaReceiptBuilder.build(sale, profile, EtaSimulator.POS_SERIAL, chainHead, null, "EGP", CAIRO);
    }

    private EInvoiceSubmission issue(EtaReceiptBuilder.BuiltReceipt built, EInvoiceSubmission.DocumentType type) {
        EInvoiceSubmission row = EInvoiceSubmission.builder().id((long) rows.size() + 1).posDevice(device)
                .documentType(type).etaUuid(built.uuid()).previousUuid(chainHead).receiptNumber(built.receiptNumber())
                .dateTimeIssued(built.dateTimeIssued()).receiptJson(built.json())
                .status(EInvoiceSubmission.Status.PENDING).retryCount(0).build();
        rows.add(row);
        chainHead = built.uuid();
        return row;
    }

    // TransactionTemplate needs a manager; the rows live in memory, so there's nothing to commit.
    private static class NoTransactions extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
