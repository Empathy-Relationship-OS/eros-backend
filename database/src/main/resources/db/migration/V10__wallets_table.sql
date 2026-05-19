-- V10: Wallet Table

CREATE TABLE IF NOT EXISTS wallets (
    wallet_id BIGSERIAL PRIMARY KEY,
    user_id varchar(128) NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    token_balance NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    lifetime_spent NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    lifetime_purchased NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    currency varchar(3) NOT NULL DEFAULT 'GBP',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT balance_non_negative CHECK (token_balance >= 0)
);

-- Index for common queries
CREATE INDEX idx_wallets_user_id ON wallets(user_id);