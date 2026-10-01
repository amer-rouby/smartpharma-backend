-- Schema for: offline POS, active-ingredient alternatives, partial returns.
-- Production runs with ddl-auto=validate, so run this once before deploying.
-- Safe to re-run (IF NOT EXISTS everywhere). The startup backfills
-- (SaleClientIdUniquenessBackfill, IngredientKeyBackfill) fill in data and
-- the partial unique index on their own.

BEGIN;

-- Offline POS: device-generated id that makes a resent sale idempotent.
ALTER TABLE smartpharma.sales_transactions ADD COLUMN IF NOT EXISTS client_sale_id VARCHAR(36);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_pharmacy_client_sale
    ON smartpharma.sales_transactions (pharmacy_id, client_sale_id) WHERE client_sale_id IS NOT NULL;

-- Partial returns: refunds so far (revenue = total_amount - returned_amount).
ALTER TABLE smartpharma.sales_transactions ADD COLUMN IF NOT EXISTS returned_amount NUMERIC(10,2) DEFAULT 0.00;

-- Active ingredient + the derived key products are matched on as alternatives.
ALTER TABLE smartpharma.products ADD COLUMN IF NOT EXISTS active_ingredient VARCHAR(255);
ALTER TABLE smartpharma.products ADD COLUMN IF NOT EXISTS ingredient_key VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_products_ingredient ON smartpharma.products (pharmacy_id, ingredient_key);

CREATE TABLE IF NOT EXISTS smartpharma.sale_returns (
    id                  BIGSERIAL PRIMARY KEY,
    sale_transaction_id BIGINT         NOT NULL REFERENCES smartpharma.sales_transactions (id),
    pharmacy_id         BIGINT         NOT NULL REFERENCES smartpharma.pharmacies (id),
    user_id             BIGINT         NOT NULL REFERENCES smartpharma.users (id),
    return_number       INTEGER        NOT NULL,
    items_total         NUMERIC(10,2)  NOT NULL,
    discount_share      NUMERIC(10,2)  NOT NULL,
    refund_amount       NUMERIC(10,2)  NOT NULL,
    restocked           BOOLEAN        NOT NULL,
    reason              VARCHAR(500),
    created_at          TIMESTAMP(6),
    CONSTRAINT uk_sale_returns_number UNIQUE (sale_transaction_id, return_number)
);
CREATE INDEX IF NOT EXISTS idx_sale_returns_pharmacy ON smartpharma.sale_returns (pharmacy_id);

CREATE TABLE IF NOT EXISTS smartpharma.sale_return_items (
    id             BIGSERIAL PRIMARY KEY,
    sale_return_id BIGINT        NOT NULL REFERENCES smartpharma.sale_returns (id),
    sale_item_id   BIGINT        NOT NULL REFERENCES smartpharma.sale_items (id),
    quantity       INTEGER       NOT NULL,
    unit_price     NUMERIC(10,2) NOT NULL,
    total_price    NUMERIC(10,2) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_sale_return_items_item ON smartpharma.sale_return_items (sale_item_id);

-- ETA return receipt of a partial return (null = cancellation of the whole sale).
ALTER TABLE smartpharma.einvoice_submissions
    ADD COLUMN IF NOT EXISTS sale_return_id BIGINT REFERENCES smartpharma.sale_returns (id);

COMMIT;
