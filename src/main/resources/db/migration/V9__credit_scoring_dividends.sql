-- V9: Credit scoring and dividend management
CREATE TABLE IF NOT EXISTS credit_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id UUID NOT NULL REFERENCES members(id),
    score INTEGER NOT NULL,
    rating VARCHAR(5) NOT NULL,
    factors TEXT,
    calculated_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_credit_scores_member ON credit_scores(member_id);
CREATE INDEX IF NOT EXISTS idx_credit_scores_calculated ON credit_scores(calculated_at DESC);

CREATE TABLE IF NOT EXISTS dividend_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    year INTEGER NOT NULL,
    method VARCHAR(30) NOT NULL,
    rate NUMERIC(10,4),
    total_amount NUMERIC(15,2),
    members_paid INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processed_by VARCHAR(100),
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dividend_runs_year ON dividend_runs(year);
