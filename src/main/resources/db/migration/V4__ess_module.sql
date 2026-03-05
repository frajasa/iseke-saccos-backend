-- V4: Employee Self-Service (ESS) Module
-- Creates tables for employers, payroll deductions, and ESS service requests

-- Employers table
CREATE TABLE IF NOT EXISTS employers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employer_code VARCHAR(20) NOT NULL UNIQUE,
    employer_name VARCHAR(200) NOT NULL,
    contact_person VARCHAR(200),
    phone_number VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    tin_number VARCHAR(50),
    payroll_cutoff_day INTEGER DEFAULT 25,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Add employer-related columns to members table
ALTER TABLE members ADD COLUMN IF NOT EXISTS employer_id UUID REFERENCES employers(id);
ALTER TABLE members ADD COLUMN IF NOT EXISTS employee_number VARCHAR(50);
ALTER TABLE members ADD COLUMN IF NOT EXISTS department VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_members_employer_id ON members(employer_id);

-- Add linked_member_id to users table (for ESS portal)
ALTER TABLE users ADD COLUMN IF NOT EXISTS linked_member_id UUID REFERENCES members(id);

CREATE INDEX IF NOT EXISTS idx_users_linked_member_id ON users(linked_member_id);

-- Payroll Deductions (recurring deduction rules)
CREATE TABLE IF NOT EXISTS payroll_deductions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id UUID NOT NULL REFERENCES members(id),
    employer_id UUID NOT NULL REFERENCES employers(id),
    deduction_type VARCHAR(50) NOT NULL, -- SAVINGS, LOAN_REPAYMENT, SHARES
    savings_account_id UUID REFERENCES savings_accounts(id),
    loan_account_id UUID REFERENCES loan_accounts(id),
    amount NUMERIC(19,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payroll_deductions_member_id ON payroll_deductions(member_id);
CREATE INDEX IF NOT EXISTS idx_payroll_deductions_employer_id ON payroll_deductions(employer_id);

-- Payroll Deduction Batches (monthly processing records)
CREATE TABLE IF NOT EXISTS payroll_deduction_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_number VARCHAR(50) NOT NULL UNIQUE,
    employer_id UUID NOT NULL REFERENCES employers(id),
    period VARCHAR(20) NOT NULL, -- e.g., '2026-03'
    total_deductions INTEGER DEFAULT 0,
    successful_deductions INTEGER DEFAULT 0,
    failed_deductions INTEGER DEFAULT 0,
    total_amount NUMERIC(19,2) DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    processed_by VARCHAR(100),
    processed_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(employer_id, period)
);

-- ESS Service Requests (member self-service requests)
CREATE TABLE IF NOT EXISTS ess_service_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_number VARCHAR(50) NOT NULL UNIQUE,
    member_id UUID NOT NULL REFERENCES members(id),
    request_type VARCHAR(50) NOT NULL, -- LOAN_APPLICATION, WITHDRAWAL, STATEMENT, PROFILE_UPDATE
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    amount NUMERIC(19,2),
    description TEXT,
    request_data TEXT, -- JSON data specific to request type
    reviewed_by UUID REFERENCES users(id),
    review_notes TEXT,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ess_requests_member_id ON ess_service_requests(member_id);
CREATE INDEX IF NOT EXISTS idx_ess_requests_status ON ess_service_requests(status);
