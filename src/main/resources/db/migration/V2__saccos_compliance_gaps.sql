-- SACCOS Compliance Gaps - Database Migration
-- Tanzania Registrar of Cooperative Societies minimum standards

-- ================================================
-- 1. User password security fields (Gap 8)
-- ================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_expires_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_failed_login TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_locked_until TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS force_password_change BOOLEAN DEFAULT false;

-- ================================================
-- 2. Transaction reversal fields (Gap 2)
-- ================================================
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS reversal_of_id UUID;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS reversed_by_id UUID;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS reversal_reason TEXT;

-- ================================================
-- 3. Savings product GL account links for fees/tax (Gap 9)
-- ================================================
ALTER TABLE savings_products ADD COLUMN IF NOT EXISTS tax_payable_account_id UUID;
ALTER TABLE savings_products ADD COLUMN IF NOT EXISTS withdrawal_fee_account_id UUID;

-- ================================================
-- 4. New GL accounts for compliance
-- ================================================

-- Withholding Tax Payable (Gap 9)
INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, level, is_control_account, normal_balance, status)
SELECT gen_random_uuid(), '2301', 'Withholding Tax Payable', 'LIABILITY', 'CURRENT_LIABILITIES', 3, false, 'CREDIT', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_code = '2301');

-- Interest Receivable for loan accrual (Gap 1)
INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, level, is_control_account, normal_balance, status)
SELECT gen_random_uuid(), '1203', 'Interest Receivable', 'ASSET', 'CURRENT_ASSETS', 3, false, 'DEBIT', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_code = '1203');

-- Loan Provision Expense (Gap 5)
INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, level, is_control_account, normal_balance, status)
SELECT gen_random_uuid(), '5301', 'Loan Provision Expense', 'EXPENSE', 'OPERATING_EXPENSES', 3, false, 'DEBIT', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_code = '5301');

-- Loan Loss Provision (contra-asset) (Gap 5)
INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, level, is_control_account, normal_balance, status)
SELECT gen_random_uuid(), '1204', 'Loan Loss Provision', 'ASSET', 'CURRENT_ASSETS', 3, false, 'CREDIT', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_code = '1204');
