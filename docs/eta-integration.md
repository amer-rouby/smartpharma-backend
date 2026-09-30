# Egyptian Tax Authority (ETA) e-Receipt Integration

## Status: phase 1 implemented, not yet verified against ETA pre-production

Sales are issued as ETA **e-receipts** (B2C, receipt type `s`, version `1.2`)
and sent to the ETA eReceipt API. Everything is gated behind the
`eInvoiceEnabled` smart feature flag (off by default).

Official references used:

- Receipt structure: https://sdk.invoicing.eta.gov.eg/documents/receipt-v1-2/
- Calculation rules: https://sdk.invoicing.eta.gov.eg/main-calculations/
- UUID, QR, submission windows: https://sdk.invoicing.eta.gov.eg/receiptissuancefaq/
- Serialization for hashing: https://sdk.invoicing.eta.gov.eg/document-serialization-approach/
- POS authentication: https://sdk.invoicing.eta.gov.eg/ereceiptapi/01-authenticate-pos/
- Submission: https://sdk.invoicing.eta.gov.eg/ereceiptapi/02-submit-receipt/
- Environments and test root CA: https://sdk.invoicing.eta.gov.eg/faq/

## How it works

1. A sale commits -> `EtaSaleCompletedListener` (after commit) calls
   `EtaReceiptIssuer`.
2. The issuer locks the pharmacy's single active POS device row, builds the
   receipt (`EtaReceiptBuilder`), chains it to the device's previous receipt
   (`previousUUID`), computes its UUID (SHA-256 of the canonical
   serialization, `EtaCanonicalSerializer`) and stores the exact JSON text in
   `einvoice_submissions.receipt_json`. The UUID is a hash of that text, so it
   is never rebuilt from the sale afterwards.
3. `EtaReceiptSubmitter` sends pending receipts per device, in chain order, in
   batches of at most 500 receipts / 1.5 MB - right after the sale (async) and
   every `eta.submission.interval-ms` (default 2 minutes). ETA accepts a
   receipt up to 24 hours after issue, so an ETA or network outage does not
   block sales.
4. `EtaReceiptStatusPoller` reads back each submission's validation result
   (`GET /api/v1/receiptsubmissions/{uuid}/details`) every
   `eta.status.interval-ms` (default 5 minutes): `Valid` receipts become
   `ACCEPTED`, `Invalid`/`Cancelled` ones `REJECTED` with ETA's reasons
   (Arabic first).
5. The sale details dialog shows the receipt status, UUID and QR code, and
   prints the QR on the invoice.

## Buyer national ID

ETA requires the buyer's national ID and name on receipts of 150,000 EGP or
more. The POS asks for both (optional below the threshold, validated as 14
digits starting with 2 or 3); they're stored on the sale (`buyer_national_id`,
`buyer_name`) and sent in the receipt's `buyer` block. The sales module
publishes `SaleCreatingEvent` synchronously before saving, and with
e-receipts on a sale at or above the threshold without both is refused with
`400 BUYER_ID_REQUIRED` - at the till, before any stock is committed.

## Cancelled and edited sales

- **Deleting a sale** publishes `SaleCancelledEvent`. If the sale has an
  issued receipt that ETA didn't reject, a **return receipt** (type `r`,
  version `1.2`) is issued after commit: a second `einvoice_submissions` row
  for the same sale (`document_type = RETURN`, `original_submission_id` set)
  with `referenceUUID` = the original's UUID and receipt number `R-<original>`.
  It's built from the original's stored JSON, so items, VAT and totals are
  exactly what ETA has, and it joins the device chain like any receipt. ETA
  accepts returns up to 540 days after the sale.
- **Editing a sale** that changes the discount, payment method or customer
  phone publishes `SaleAmendingEvent` synchronously; if an issued, non-rejected
  receipt exists the update is refused with `409 SALE_HAS_ETA_RECEIPT`
  (cancel and sell again instead). Notes can still be edited.
- `EInvoiceReturnReceiptBackfill` (startup) drops the old unique constraint
  on `einvoice_submissions.sale_transaction_id` and marks older rows as `SALE`.

A sale that can't be turned into a valid receipt (missing taxpayer data,
product without an ETA code, ...) is recorded as `ERROR` with the reason; the
chain does not move, and "Retry" issues it once the data is fixed.

## Statuses (`einvoice_submissions.status`)

| Status | Meaning |
|---|---|
| `PENDING` | Issued, waiting to be sent |
| `SUBMITTED` | ETA took it for processing (`long_id` set), validation pending |
| `ACCEPTED` | ETA validated it as `Valid` |
| `REJECTED` | ETA refused it at submission or validated it as `Invalid` - fix the data, then Retry re-issues it with `referenceOldUUID` |
| `ERROR` | Not issued, or not delivered (network, auth, 5xx) - retried automatically up to `eta.submission.max-attempts` |


## Configuration

Server environment variables:

| Variable | Purpose |
|---|---|
| `ETA_CREDENTIALS_KEY` | **Required.** Base64 of 32 random bytes. Encrypts the per-pharmacy client secret and POS pre-shared keys (AES-256-GCM). Changing it makes stored secrets unreadable. Generate with `openssl rand -base64 32`. |
| `ETA_SUBMISSION_INTERVAL_MS` | Retry timer, default `120000` |
| `ETA_SUBMISSION_MAX_ATTEMPTS` | Automatic delivery attempts per receipt, default `50` |
| `ETA_STATUS_INTERVAL_MS` | How often validation results are read back, default `300000` |

Per pharmacy (Settings -> E-Receipt (ETA), ADMIN only):

- Taxpayer: environment (`PREPROD`/`PROD`), 9-digit RIN, registered trade
  name, activity code, branch code (`0` for the main branch), branch address.
- ERP credentials: client ID / secret from the ETA portal (write-only).
- One active POS device: serial, OS version, model framework, pre-shared key
  (as registered with ETA). Its serial is locked once it has issued receipts.
- "Test connection" authenticates the device against ETA.

Per product: ETA code type (`GS1`/`EGS`) and code. When empty, a barcode that
is a valid GTIN is sent as a GS1 code.

## VAT (tax type `T1`)

Each product line gets a VAT subtype from the product, else from the
pharmacy default in the ETA settings. With neither set, the line has no tax
item (a pharmacy that isn't VAT-registered).

| Subtype | Meaning | Rate |
|---|---|---|
| `V009` | General goods | 14% |
| `V010` | Other rate | set on the product / default |
| `V003` | Exempt | 0 |
| `V004` | Not subject to VAT | 0 |

SmartPharma prices are what the customer pays, i.e. VAT-inclusive. The
receipt splits the net unit price out of the shelf price (5 decimals, ETA's
precision) and adds VAT back, so each line total lands on the shelf price
within a fraction of a piaster. `EtaReceiptBuilderTest` checks the ETA
"Main Calculations" identities on 200 random sales.

Which subtype applies to medicines versus cosmetics or supplements is a tax
decision for the pharmacy's accountant, not something the code assumes.

PREPROD uses certificates issued by ETA's own test root CA. Import it into the
JVM truststore used by the backend - never disable TLS verification.

## Deliberate phase-1 limits

Each of these is refused with a clear message instead of being sent wrong:

- EGP only.
- No per-item discounts (SmartPharma only has the sale-level discount, sent as
  `extraReceiptDiscountData`).

Known gaps, not handled yet:

- Only whole-sale returns: SmartPharma has no partial (per-item) returns yet.
- A return receipt ETA rejects is retried automatically if the failure was
  transient, but there's no screen to re-issue it (a cancelled sale isn't
  shown in the sales list).
- Production runs with `ddl-auto=validate`, which doesn't create the ETA
  tables/columns: the schema has to exist before deploying (e.g. start once
  against the production database with `ddl-auto=update`, or run the DDL
  it generates).
- Batch signatures are sent empty: ETA's SDK states batch signature
  validation is not deployed yet.
- `receiptType` `s` / version `1.2` and the UUID procedure follow the docs;
  they have only been unit-tested, not accepted by ETA PREPROD yet.

## API endpoints

Receipts (`ADMIN`/`MANAGER`, `403` when `eInvoiceEnabled` is off):

- `GET /api/e-invoice/{saleId}` - receipt status for a sale.
- `POST /api/e-invoice/{saleId}/submit` - issue if needed, then send pending receipts.
- `POST /api/e-invoice/{saleId}/retry` - re-issue a rejected receipt / resend a failed one.

Settings (`ADMIN`):

- `GET|PUT /api/e-invoice/settings`
- `GET|POST /api/e-invoice/settings/devices`, `PUT /api/e-invoice/settings/devices/{id}`
- `POST /api/e-invoice/settings/devices/{id}/test`
