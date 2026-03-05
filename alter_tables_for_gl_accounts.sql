-- ====================================================================
-- ALTER TABLES TO ADD GL ACCOUNT REFERENCES
-- ====================================================================
-- Run this script to add GL account foreign keys to product tables

-- Add GL account references to savings_products table
ALTER TABLE savings_products
ADD COLUMN IF NOT EXISTS liability_account_id UUID,
ADD COLUMN IF NOT EXISTS cash_account_id UUID,
ADD COLUMN IF NOT EXISTS interest_expense_account_id UUID,
ADD COLUMN IF NOT EXISTS fee_income_account_id UUID;

-- Add foreign key constraints for savings_products
ALTER TABLE savings_products
ADD CONSTRAINT fk_savings_liability_account
    FOREIGN KEY (liability_account_id)
    REFERENCES chart_of_accounts(id),
ADD CONSTRAINT fk_savings_cash_account
    FOREIGN KEY (cash_account_id)
    REFERENCES chart_of_accounts(id),
ADD CONSTRAINT fk_savings_interest_expense_account
    FOREIGN KEY (interest_expense_account_id)
    REFERENCES chart_of_accounts(id),
ADD CONSTRAINT fk_savings_fee_income_account
    FOREIGN KEY (fee_income_account_id)
    REFERENCES chart_of_accounts(id);

-- Add GL account references to loan_products table
ALTER TABLE loan_products
ADD COLUMN IF NOT EXISTS loan_receivable_account_id UUID,
ADD COLUMN IF NOT EXISTS cash_account_id UUID,
ADD COLUMN IF NOT EXISTS interest_income_account_id UUID,
ADD COLUMN IF NOT EXISTS fee_income_account_id UUID,
ADD COLUMN IF NOT EXISTS penalty_income_account_id UUID;

-- Add foreign key constraints for loan_products
ALTER TABLE loan_products
ADD CONSTRAINT fk_loan_receivable_account
    FOREIGN KEY (loan_receivable_account_id)
    REFERENCES chart_of_accounts(id),
ADD CONSTRAINT fk_loan_cash_account
    FOREIGN KEY (cash_account_id)
    REFERENCES chart_of_accounts(id),
ADD CONSTRAINT fk_loan_interest_income_account
    FOREIGN KEY (interest_income_account_id)
    REFERENCES chart_of_accounts(id),
ADD CONSTRAINT fk_loan_fee_income_account
    FOREIGN KEY (fee_income_account_id)
    REFERENCES chart_of_accounts(id),
ADD CONSTRAINT fk_loan_penalty_income_account
    FOREIGN KEY (penalty_income_account_id)
    REFERENCES chart_of_accounts(id);

-- ====================================================================
-- UPDATE EXISTING PRODUCTS WITH DEFAULT GL ACCOUNTS
-- ====================================================================

-- Update savings products to link to default GL accounts
UPDATE savings_products
SET
    liability_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '2101'),
    cash_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '1101'),
    interest_expense_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '5101'),
    fee_income_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '4202')
WHERE product_type = 'SAVINGS';

UPDATE savings_products
SET
    liability_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '2102'),
    cash_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '1101'),
    interest_expense_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '5102'),
    fee_income_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '4202')
WHERE product_type = 'FIXED_DEPOSIT';

UPDATE savings_products
SET
    liability_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '2103'),
    cash_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '1101'),
    interest_expense_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '5101'),
    fee_income_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '4202')
WHERE product_type = 'SHARES';

UPDATE savings_products
SET
    liability_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '2104'),
    cash_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '1101'),
    interest_expense_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '5101'),
    fee_income_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '4202')
WHERE product_type IN ('CURRENT', 'CHECKING');

-- Update loan products to link to default GL accounts
UPDATE loan_products
SET
    loan_receivable_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '1201'),
    cash_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '1101'),
    interest_income_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '4101'),
    fee_income_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '4201'),
    penalty_income_account_id = (SELECT id FROM chart_of_accounts WHERE account_code = '4102');

-- Verify the updates
SELECT
    product_name,
    product_type,
    liability_account_id,
    cash_account_id
FROM savings_products;

SELECT
    product_name,
    loan_receivable_account_id,
    cash_account_id,
    interest_income_account_id
FROM loan_products;
