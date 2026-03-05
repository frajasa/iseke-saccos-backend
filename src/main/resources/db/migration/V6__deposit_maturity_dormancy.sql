-- V6: Fixed deposit maturity and dormancy support
ALTER TABLE savings_products ADD COLUMN IF NOT EXISTS auto_rollover BOOLEAN DEFAULT false;
ALTER TABLE savings_accounts ADD COLUMN IF NOT EXISTS last_activity_date TIMESTAMP;
ALTER TABLE loan_accounts ADD COLUMN IF NOT EXISTS write_off_reason TEXT;
ALTER TABLE loan_accounts ADD COLUMN IF NOT EXISTS write_off_date DATE;
ALTER TABLE loan_accounts ADD COLUMN IF NOT EXISTS written_off_by VARCHAR(100);

-- Backfill last_activity_date from last_transaction_date
UPDATE savings_accounts SET last_activity_date = last_transaction_date WHERE last_activity_date IS NULL AND last_transaction_date IS NOT NULL;
UPDATE savings_accounts SET last_activity_date = opening_date WHERE last_activity_date IS NULL;
