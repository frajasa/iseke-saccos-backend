-- ====================================================================
-- CHART OF ACCOUNTS SETUP SCRIPT FOR ISEKE MICROFINANCE SYSTEM
-- ====================================================================
-- This script creates standard chart of accounts for microfinance operations
-- Run this script against your database to populate chart_of_accounts table

-- Clear existing data (optional - comment out if you want to keep existing accounts)
-- TRUNCATE TABLE chart_of_accounts CASCADE;

-- ====================================================================
-- ASSETS (Account Type: ASSET, Normal Balance: DEBIT)
-- ====================================================================

-- 1000 - CURRENT ASSETS
INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '1000', 'Current Assets', 'ASSET', 'ASSETS', NULL, 1, true, 'DEBIT', 'ACTIVE');

-- 1100 - Cash and Bank Accounts
INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '1100', 'Cash and Bank', 'ASSET', 'CURRENT_ASSETS',
    (SELECT id FROM chart_of_accounts WHERE account_code = '1000'), 2, true, 'DEBIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '1101', 'Cash on Hand', 'ASSET', 'CURRENT_ASSETS',
    (SELECT id FROM chart_of_accounts WHERE account_code = '1100'), 3, false, 'DEBIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '1102', 'Cash in Vault', 'ASSET', 'CURRENT_ASSETS',
    (SELECT id FROM chart_of_accounts WHERE account_code = '1100'), 3, false, 'DEBIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '1110', 'Bank Accounts', 'ASSET', 'CURRENT_ASSETS',
    (SELECT id FROM chart_of_accounts WHERE account_code = '1100'), 3, false, 'DEBIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '1111', 'Bank Account - Operating', 'ASSET', 'CURRENT_ASSETS',
    (SELECT id FROM chart_of_accounts WHERE account_code = '1110'), 4, false, 'DEBIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '1112', 'Bank Account - Mobile Money', 'ASSET', 'CURRENT_ASSETS',
    (SELECT id FROM chart_of_accounts WHERE account_code = '1110'), 4, false, 'DEBIT', 'ACTIVE');

-- 1200 - Loans Receivable
INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '1200', 'Loans Receivable', 'ASSET', 'CURRENT_ASSETS',
    (SELECT id FROM chart_of_accounts WHERE account_code = '1000'), 2, true, 'DEBIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '1201', 'Loans to Members', 'ASSET', 'CURRENT_ASSETS',
    (SELECT id FROM chart_of_accounts WHERE account_code = '1200'), 3, false, 'DEBIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '1202', 'Loans - Principal Outstanding', 'ASSET', 'CURRENT_ASSETS',
    (SELECT id FROM chart_of_accounts WHERE account_code = '1200'), 3, false, 'DEBIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '1203', 'Interest Receivable', 'ASSET', 'CURRENT_ASSETS',
    (SELECT id FROM chart_of_accounts WHERE account_code = '1200'), 3, false, 'DEBIT', 'ACTIVE');

-- ====================================================================
-- LIABILITIES (Account Type: LIABILITY, Normal Balance: CREDIT)
-- ====================================================================

-- 2000 - CURRENT LIABILITIES
INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '2000', 'Current Liabilities', 'LIABILITY', 'LIABILITIES', NULL, 1, true, 'CREDIT', 'ACTIVE');

-- 2100 - Member Deposits
INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '2100', 'Member Deposits', 'LIABILITY', 'CURRENT_LIABILITIES',
    (SELECT id FROM chart_of_accounts WHERE account_code = '2000'), 2, true, 'CREDIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '2101', 'Savings Accounts', 'LIABILITY', 'CURRENT_LIABILITIES',
    (SELECT id FROM chart_of_accounts WHERE account_code = '2100'), 3, false, 'CREDIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '2102', 'Fixed Deposit Accounts', 'LIABILITY', 'CURRENT_LIABILITIES',
    (SELECT id FROM chart_of_accounts WHERE account_code = '2100'), 3, false, 'CREDIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '2103', 'Share Capital Accounts', 'LIABILITY', 'CURRENT_LIABILITIES',
    (SELECT id FROM chart_of_accounts WHERE account_code = '2100'), 3, false, 'CREDIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '2104', 'Current Accounts', 'LIABILITY', 'CURRENT_LIABILITIES',
    (SELECT id FROM chart_of_accounts WHERE account_code = '2100'), 3, false, 'CREDIT', 'ACTIVE');

-- 2200 - Interest Payable
INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '2200', 'Interest Payable', 'LIABILITY', 'CURRENT_LIABILITIES',
    (SELECT id FROM chart_of_accounts WHERE account_code = '2000'), 2, true, 'CREDIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '2201', 'Interest Payable on Savings', 'LIABILITY', 'CURRENT_LIABILITIES',
    (SELECT id FROM chart_of_accounts WHERE account_code = '2200'), 3, false, 'CREDIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '2202', 'Interest Payable on Fixed Deposits', 'LIABILITY', 'CURRENT_LIABILITIES',
    (SELECT id FROM chart_of_accounts WHERE account_code = '2200'), 3, false, 'CREDIT', 'ACTIVE');

-- ====================================================================
-- EQUITY (Account Type: EQUITY, Normal Balance: CREDIT)
-- ====================================================================

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '3000', 'Equity', 'EQUITY', 'EQUITY', NULL, 1, true, 'CREDIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '3100', 'Retained Earnings', 'EQUITY', 'EQUITY',
    (SELECT id FROM chart_of_accounts WHERE account_code = '3000'), 2, false, 'CREDIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '3200', 'Current Year Earnings', 'EQUITY', 'EQUITY',
    (SELECT id FROM chart_of_accounts WHERE account_code = '3000'), 2, false, 'CREDIT', 'ACTIVE');

-- ====================================================================
-- INCOME (Account Type: INCOME, Normal Balance: CREDIT)
-- ====================================================================

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '4000', 'Income', 'INCOME', 'INCOME', NULL, 1, true, 'CREDIT', 'ACTIVE');

-- 4100 - Interest Income
INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '4100', 'Interest Income', 'INCOME', 'OPERATING_INCOME',
    (SELECT id FROM chart_of_accounts WHERE account_code = '4000'), 2, true, 'CREDIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '4101', 'Interest on Loans', 'INCOME', 'OPERATING_INCOME',
    (SELECT id FROM chart_of_accounts WHERE account_code = '4100'), 3, false, 'CREDIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '4102', 'Penalty Income', 'INCOME', 'OPERATING_INCOME',
    (SELECT id FROM chart_of_accounts WHERE account_code = '4100'), 3, false, 'CREDIT', 'ACTIVE');

-- 4200 - Fee Income
INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '4200', 'Fee Income', 'INCOME', 'OPERATING_INCOME',
    (SELECT id FROM chart_of_accounts WHERE account_code = '4000'), 2, true, 'CREDIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '4201', 'Loan Processing Fees', 'INCOME', 'OPERATING_INCOME',
    (SELECT id FROM chart_of_accounts WHERE account_code = '4200'), 3, false, 'CREDIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '4202', 'Withdrawal Fees', 'INCOME', 'OPERATING_INCOME',
    (SELECT id FROM chart_of_accounts WHERE account_code = '4200'), 3, false, 'CREDIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '4203', 'Account Maintenance Fees', 'INCOME', 'OPERATING_INCOME',
    (SELECT id FROM chart_of_accounts WHERE account_code = '4200'), 3, false, 'CREDIT', 'ACTIVE');

-- ====================================================================
-- EXPENSES (Account Type: EXPENSE, Normal Balance: DEBIT)
-- ====================================================================

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '5000', 'Expenses', 'EXPENSE', 'EXPENSES', NULL, 1, true, 'DEBIT', 'ACTIVE');

-- 5100 - Interest Expense
INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '5100', 'Interest Expense', 'EXPENSE', 'OPERATING_EXPENSES',
    (SELECT id FROM chart_of_accounts WHERE account_code = '5000'), 2, true, 'DEBIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '5101', 'Interest on Savings', 'EXPENSE', 'OPERATING_EXPENSES',
    (SELECT id FROM chart_of_accounts WHERE account_code = '5100'), 3, false, 'DEBIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '5102', 'Interest on Fixed Deposits', 'EXPENSE', 'OPERATING_EXPENSES',
    (SELECT id FROM chart_of_accounts WHERE account_code = '5100'), 3, false, 'DEBIT', 'ACTIVE');

-- 5200 - Operating Expenses
INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '5200', 'Operating Expenses', 'EXPENSE', 'OPERATING_EXPENSES',
    (SELECT id FROM chart_of_accounts WHERE account_code = '5000'), 2, true, 'DEBIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '5201', 'Salaries and Wages', 'EXPENSE', 'OPERATING_EXPENSES',
    (SELECT id FROM chart_of_accounts WHERE account_code = '5200'), 3, false, 'DEBIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '5202', 'Rent Expense', 'EXPENSE', 'OPERATING_EXPENSES',
    (SELECT id FROM chart_of_accounts WHERE account_code = '5200'), 3, false, 'DEBIT', 'ACTIVE');

INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, account_category, parent_account_id, level, is_control_account, normal_balance, status)
VALUES (uuid_generate_v4(), '5203', 'Utilities Expense', 'EXPENSE', 'OPERATING_EXPENSES',
    (SELECT id FROM chart_of_accounts WHERE account_code = '5200'), 3, false, 'DEBIT', 'ACTIVE');

-- ====================================================================
-- END OF CHART OF ACCOUNTS SETUP
-- ====================================================================

-- Verify the setup
SELECT
    account_code,
    account_name,
    account_type,
    level,
    is_control_account,
    normal_balance,
    status
FROM chart_of_accounts
ORDER BY account_code;
