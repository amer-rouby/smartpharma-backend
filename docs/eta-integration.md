# Egyptian E-Invoice (ETA) Integration

## Status: not implemented

This system has a structural placeholder for submitting sales to the
Egyptian Tax Authority (ETA) e-invoice system, gated behind the
`eInvoiceEnabled` smart feature flag (off by default in
`smart_feature_settings`). No real API call to ETA is made yet.

`NoopEtaProvider` (`service/impl/NoopEtaProvider.java`) is the only
`EtaIntegrationService` implementation today. It never fabricates a
success response - every submission attempt is recorded with
`status = ERROR` and a clear message, either:

- `"ETA credentials not configured"` - `ETA_CLIENT_ID`/`ETA_CLIENT_SECRET`
  are not set, or
- `"ETA credentials are set but no real ETA integration is implemented
  yet"` - credentials are present, but the real ETA SDK/API call still
  needs to be written.

## Required environment variables

None of these have a real default and none are committed to source
control.

| Variable | Purpose | Example |
|---|---|---|
| `ETA_CLIENT_ID` | ETA e-invoicing portal client ID | (from ETA portal) |
| `ETA_CLIENT_SECRET` | ETA e-invoicing portal client secret | (from ETA portal) |
| `ETA_ENVIRONMENT` | `sandbox` or `production` | `sandbox` (default) |

Set them the same way other secrets in this project are supplied (see
`README.md`, e.g. `export ETA_CLIENT_ID=...`) - never hardcode them in
`application.properties` or commit them.

## Adding a real implementation

1. Register with the ETA e-invoicing portal and obtain sandbox
   credentials.
2. Implement `EtaIntegrationService` (see
   `service/EtaIntegrationService.java`) - e.g. `RealEtaProvider` -
   handling ETA's OAuth token exchange, document signing, and submission
   API calls.
3. Mark it `@Primary` (or remove `NoopEtaProvider`'s `@Service`
   annotation) so Spring wires the real implementation instead.
   `EInvoiceService`/`EInvoiceController` require no changes - they only
   depend on the `EtaIntegrationService` interface.
4. Test against the ETA sandbox environment (`ETA_ENVIRONMENT=sandbox`)
   before switching to `production`.

## API endpoints

All endpoints require `ADMIN` or `MANAGER` role and return `403` if
`eInvoiceEnabled` is off for the pharmacy.

- `GET /api/e-invoice/{saleId}` - current submission status for a sale
  (`null` data if never submitted).
- `POST /api/e-invoice/{saleId}/submit` - submit (or re-check) a sale.
- `POST /api/e-invoice/{saleId}/retry` - retry a previously failed
  submission (increments `retryCount`).
