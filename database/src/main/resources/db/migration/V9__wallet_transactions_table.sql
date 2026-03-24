-- V8 Wallet Transactions table.

-- Transactions table stores all wallet-related financial operations including:
-- - PURCHASE: Token purchases via Stripe payment processing
-- - SPEND: Token deductions when committing to dates
-- - REFUND: Token credits when dates are cancelled
-- - ADJUSTMENT: Manual corrections by administrators

-- Uncommon field explanation
-- description - What the transaction was e.g. Purchase date with X (Activity) / Purchase 10 tokens

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL REFERENCES wallets(wallet_id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    balance_after NUMERIC(10, 2) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    date_id BIGINT REFERENCES date_commitments(date_id) ON DELETE SET NULL,
    related_transaction_id BIGINT REFERENCES transactions(transaction_id) ON DELETE SET NULL,
    stripe_payment_intent_id VARCHAR(255),
    amount_paid_gbp NUMERIC(10, 2),
    idempotency_key VARCHAR(255),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT valid_transaction_type CHECK (type IN ('PURCHASE', 'SPEND', 'REFUND', 'ADJUSTMENT')),
    CONSTRAINT valid_transaction_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

-- Indexes for query optimization
CREATE INDEX idx_transactions_stripe_payment_intent ON transactions(stripe_payment_intent_id);
