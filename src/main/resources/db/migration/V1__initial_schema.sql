-- =========================
-- ACCOUNTS
-- =========================
CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    account_name VARCHAR(100) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(255),
    start_balance NUMERIC(19,4) NOT NULL DEFAULT 0,
    current_balance NUMERIC(19,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_accounts_active
    ON accounts (is_active);

CREATE INDEX IF NOT EXISTS idx_accounts_name
    ON accounts (account_name);

-- =========================
-- BUDGETS
-- =========================
CREATE TABLE IF NOT EXISTS budgets (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    monthly_amount NUMERIC(19,4) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_budgets_category
    ON budgets (category);

CREATE INDEX IF NOT EXISTS idx_budgets_active
    ON budgets (is_active);

CREATE INDEX IF NOT EXISTS idx_budgets_period
    ON budgets (period_start, period_end);

-- =========================
-- TRANSACTIONS
-- =========================
CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    source_account_id BIGINT,
    destination_account_id BIGINT,
    recurring_expense_id BIGINT,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    budget_category VARCHAR(50),
    transaction_date DATE NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_transactions_source_account
        FOREIGN KEY (source_account_id)
        REFERENCES accounts (id),

    CONSTRAINT fk_transactions_destination_account
        FOREIGN KEY (destination_account_id)
        REFERENCES accounts (id)
);

CREATE INDEX IF NOT EXISTS idx_transactions_date
    ON transactions (transaction_date);

CREATE INDEX IF NOT EXISTS idx_transactions_category
    ON transactions (budget_category);

CREATE INDEX IF NOT EXISTS idx_transactions_source_account
    ON transactions (source_account_id);

CREATE INDEX IF NOT EXISTS idx_transactions_destination_account
    ON transactions (destination_account_id);

CREATE INDEX IF NOT EXISTS idx_transactions_type
    ON transactions (transaction_type);

CREATE INDEX IF NOT EXISTS idx_transactions_deleted
    ON transactions (is_deleted);

-- =========================
-- RECURRING EXPENSES
-- =========================
CREATE TABLE IF NOT EXISTS recurring_expenses (
    id BIGSERIAL PRIMARY KEY,
    frequency VARCHAR(50) NOT NULL,
    next_scheduled_date DATE NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    budget_category VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    source_account_id BIGINT NOT NULL,
    last_generated_date DATE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_recurring_source_account
        FOREIGN KEY (source_account_id)
        REFERENCES accounts (id)
);

CREATE INDEX IF NOT EXISTS idx_recurring_active
    ON recurring_expenses (is_active);

CREATE INDEX IF NOT EXISTS idx_recurring_next_date
    ON recurring_expenses (next_scheduled_date);

CREATE INDEX IF NOT EXISTS idx_recurring_frequency
    ON recurring_expenses (frequency);