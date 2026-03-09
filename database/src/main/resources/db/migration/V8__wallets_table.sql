-- V7: Wallet Table

CREATE TABLE IF NOT EXISTS wallets (
    user_id varchar(128) PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    token_balance NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    lifetime_spent NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    lifetime_purchased NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    currency varchar(3) NOT NULL DEFAULT 'GBP',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT balance_non_negative CHECK (token_balance >= 0)
);

-- Index for common queries
CREATE INDEX idx_wallets_balance ON wallets(token_balance);