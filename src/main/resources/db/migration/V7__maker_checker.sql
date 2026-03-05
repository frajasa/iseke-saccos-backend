-- V7: Maker-Checker transaction approval workflow
CREATE TABLE IF NOT EXISTS transaction_approvals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    request_data TEXT,
    amount NUMERIC(15,2),
    requested_by VARCHAR(100),
    requested_at TIMESTAMP,
    approved_by VARCHAR(100),
    approved_at TIMESTAMP,
    rejected_by VARCHAR(100),
    rejected_at TIMESTAMP,
    rejection_reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transaction_approvals_status ON transaction_approvals(status);
CREATE INDEX IF NOT EXISTS idx_transaction_approvals_requested_by ON transaction_approvals(requested_by);
