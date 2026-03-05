-- V3: Payment Gateway Integration
-- Creates tables for payment provider configurations, payment requests, and reconciliation

-- Payment Provider Configurations
CREATE TABLE IF NOT EXISTS payment_provider_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(30) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT false,
    api_base_url VARCHAR(500),
    api_key VARCHAR(1000),
    api_secret VARCHAR(1000),
    public_key TEXT,
    service_provider_code VARCHAR(50),
    callback_base_url VARCHAR(500),
    settlement_gl_account_code VARCHAR(20),
    extra_config TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Payment Requests (core tracking table for all external payment interactions)
CREATE TABLE IF NOT EXISTS payment_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_number VARCHAR(50) NOT NULL UNIQUE,
    provider VARCHAR(30) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    amount NUMERIC(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TZS',
    phone_number VARCHAR(20),
    bank_account_number VARCHAR(50),
    member_id UUID REFERENCES members(id),
    savings_account_id UUID REFERENCES savings_accounts(id),
    loan_account_id UUID REFERENCES loan_accounts(id),
    purpose VARCHAR(200),
    provider_reference VARCHAR(200),
    provider_conversation_id VARCHAR(200),
    provider_response_code VARCHAR(50),
    provider_response_message VARCHAR(500),
    callback_payload TEXT,
    transaction_id UUID REFERENCES transactions(id),
    failure_reason VARCHAR(500),
    retry_count INTEGER DEFAULT 0,
    initiated_by VARCHAR(100),
    initiated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMP,
    callback_at TIMESTAMP,
    completed_at TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payment_requests_provider ON payment_requests(provider);
CREATE INDEX IF NOT EXISTS idx_payment_requests_status ON payment_requests(status);
CREATE INDEX IF NOT EXISTS idx_payment_requests_member_id ON payment_requests(member_id);
CREATE INDEX IF NOT EXISTS idx_payment_requests_provider_ref ON payment_requests(provider_reference);
CREATE INDEX IF NOT EXISTS idx_payment_requests_initiated_at ON payment_requests(initiated_at);

-- Payment Reconciliation Records
CREATE TABLE IF NOT EXISTS payment_reconciliations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(30) NOT NULL,
    reconciliation_date DATE NOT NULL,
    total_internal_count INTEGER DEFAULT 0,
    total_provider_count INTEGER DEFAULT 0,
    matched_count INTEGER DEFAULT 0,
    mismatched_count INTEGER DEFAULT 0,
    missing_internal_count INTEGER DEFAULT 0,
    missing_provider_count INTEGER DEFAULT 0,
    total_internal_amount NUMERIC(19,2) DEFAULT 0,
    total_provider_amount NUMERIC(19,2) DEFAULT 0,
    amount_difference NUMERIC(19,2) DEFAULT 0,
    status VARCHAR(30) DEFAULT 'PENDING',
    notes TEXT,
    reconciled_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(provider, reconciliation_date)
);

-- Insert GL accounts for payment settlement
INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, level, is_control_account, normal_balance, status)
VALUES
    (gen_random_uuid(), '1113', 'M-Pesa Settlement Account', 'ASSET', 'Current Assets', 2, false, 'DEBIT', 'ACTIVE'),
    (gen_random_uuid(), '1114', 'Tigopesa Settlement Account', 'ASSET', 'Current Assets', 2, false, 'DEBIT', 'ACTIVE'),
    (gen_random_uuid(), '1115', 'NMB Settlement Account', 'ASSET', 'Current Assets', 2, false, 'DEBIT', 'ACTIVE'),
    (gen_random_uuid(), '1116', 'Payments in Transit', 'ASSET', 'Current Assets', 2, false, 'DEBIT', 'ACTIVE'),
    (gen_random_uuid(), '5204', 'Payment Gateway Fees', 'EXPENSE', 'Operating Expenses', 2, false, 'DEBIT', 'ACTIVE')
ON CONFLICT (account_code) DO NOTHING;

-- Insert default provider configs (disabled)
INSERT INTO payment_provider_configs (id, provider, display_name, is_enabled, settlement_gl_account_code)
VALUES
    (gen_random_uuid(), 'MPESA', 'Vodacom M-Pesa', false, '1113'),
    (gen_random_uuid(), 'TIGOPESA', 'Airtel Tigopesa', false, '1114'),
    (gen_random_uuid(), 'NMB_BANK', 'NMB Bank', false, '1115')
ON CONFLICT (provider) DO NOTHING;
