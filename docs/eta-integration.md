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
4. The sale details dialog shows the receipt status, UUID and QR code, and
   prints the QR on the invoice.

A sale that can't be turned into a valid receipt (missing taxpayer data,
product without an ETA code, ...) is recorded as `ERROR` with the reason; the
chain does not move, and "Retry" issues it once the data is fixed.

## Statuses (`einvoice_submissions.status`)

| Status | Meaning |
|---|---|
| `PENDING` | Issued, waiting to be sent |
| `SUBMITTED` | ETA accepted it into a submission (`long_id` set) |
| `REJECTED` | ETA refused it - fix the data, then Retry re-issues it with `referenceOldUUID` |
| `ERROR` | Not issued, or not delivered (network, auth, 5xx) - retried automatically up to `eta.submission.max-attempts` |

`ACCEPTED` exists in the enum but is not set yet: `SUBMITTED` means ETA took
the receipt for processing; polling the final validation result is phase 2.

## Configuration

Server environment variables:

| Variable | Purpose |
|---|---|
| `ETA_CREDENTIALS_KEY` | **Required.** Base64 of 32 random bytes. Encrypts the per-pharmacy client secret and POS pre-shared keys (AES-256-GCM). Changing it makes stored secrets unreadable. Generate with `openssl rand -base64 32`. |
| `ETA_SUBMISSION_INTERVAL_MS` | Retry timer, default `120000` |
| `ETA_SUBMISSION_MAX_ATTEMPTS` | Automatic delivery attempts per receipt, default `50` |

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
- No buyer national ID capture, so sales of 150,000 EGP or more are refused.
- No per-item discounts (SmartPharma only has the sale-level discount, sent as
  `extraReceiptDiscountData`).

Known gaps, not handled yet:

- Editing or deleting a sale after its receipt was issued does not issue a
  return receipt - the receipt at ETA keeps the original values (phase 3).
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
