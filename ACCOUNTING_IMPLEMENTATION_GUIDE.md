# Accounting System Implementation Guide

## Overview
This guide documents the double-entry accounting implementation for the Iseke Microfinance System.

## Problem Identified
The system was NOT recording accounting entries for any transactions. While the infrastructure existed (`AccountingService.postToGeneralLedger()`), it was never being called by transaction services.

## Solution Implemented

### 1. Chart of Accounts Setup
**File:** `chart_of_accounts_setup.sql`

This SQL script creates a complete chart of accounts with:
- **Assets** (1000-1999)
  - Cash and Bank accounts (1100-1199)
  - Loans Receivable (1200-1299)

- **Liabilities** (2000-2999)
  - Member Deposits (2100-2199)
  - Interest Payable (2200-2299)

- **Equity** (3000-3999)
  - Retained Earnings
  - Current Year Earnings

- **Income** (4000-4999)
  - Interest Income (4100-4199)
  - Fee Income (4200-4299)

- **Expenses** (5000-5999)
  - Interest Expense (5100-5199)
  - Operating Expenses (5200-5299)

**To run:** Execute this script against your PostgreSQL database.

### 2. Database Schema Updates
**File:** `alter_tables_for_gl_accounts.sql`

Adds GL account references to product tables:
- `savings_products`: Links to liability, cash, interest expense, and fee income accounts
- `loan_products`: Links to loan receivable, cash, interest income, fee income, and penalty income accounts

**To run:** Execute this script AFTER running the chart of accounts setup script.

### 3. Entity Modifications

#### SavingsProduct Entity
**File:** `src/main/java/tz/co/iseke/entity/SavingsProduct.java`

Added fields:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "liability_account_id")
private ChartOfAccounts liabilityAccount;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "cash_account_id")
private ChartOfAccounts cashAccount;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "interest_expense_account_id")
private ChartOfAccounts interestExpenseAccount;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "fee_income_account_id")
private ChartOfAccounts feeIncomeAccount;
```

#### LoanProduct Entity
**File:** `src/main/java/tz/co/iseke/entity/LoanProduct.java`

Added fields:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "loan_receivable_account_id")
private ChartOfAccounts loanReceivableAccount;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "cash_account_id")
private ChartOfAccounts cashAccount;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "interest_income_account_id")
private ChartOfAccounts interestIncomeAccount;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "fee_income_account_id")
private ChartOfAccounts feeIncomeAccount;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "penalty_income_account_id")
private ChartOfAccounts penaltyIncomeAccount;
```

### 4. Service Layer Modifications

#### TransactionService
**File:** `src/main/java/tz/co/iseke/service/TransactionService.java`

**Changes:**
1. Added `AccountingService` dependency
2. Modified `processDeposit()` to create accounting entries
3. Modified `processWithdrawal()` to create accounting entries
4. Modified `processLoanRepayment()` to create accounting entries with principal/interest separation

**Accounting Entries Created:**

##### Deposit Transaction
```
DEBIT:  Cash/Bank Account (Asset)          XXX
CREDIT: Member Savings Account (Liability) XXX
```

##### Withdrawal Transaction
```
DEBIT:  Member Savings Account (Liability) XXX
CREDIT: Cash/Bank Account (Asset)          XXX
```

##### Loan Repayment Transaction
For Principal:
```
DEBIT:  Cash/Bank Account (Asset)       XXX
CREDIT: Loans Receivable (Asset)        XXX
```

For Interest:
```
DEBIT:  Cash/Bank Account (Asset)       XXX
CREDIT: Interest Income (Income)        XXX
```

#### LoanAccountService
**File:** `src/main/java/tz/co/iseke/service/LoanAccountService.java`

**Changes:**
1. Added `AccountingService` dependency
2. Added `TransactionRepository` dependency
3. Modified `disburseLoan()` to:
   - Create a disbursement transaction record
   - Post accounting entries

**Accounting Entries Created:**

##### Loan Disbursement
```
DEBIT:  Loans Receivable (Asset)  XXX
CREDIT: Cash/Bank Account (Asset) XXX
```

#### SavingsAccountService
**File:** `src/main/java/tz/co/iseke/service/SavingsAccountService.java`

**No changes needed** - The `openAccount()` method calls `transactionService.processDeposit()` which now automatically creates accounting entries.

**Accounting Entries Created:**

##### Account Opening (via deposit)
```
DEBIT:  Cash/Bank Account (Asset)          XXX
CREDIT: Member Savings Account (Liability) XXX
```

## Deployment Steps

### Step 1: Run Database Scripts
```bash
# 1. Setup chart of accounts
psql -U your_user -d your_database -f chart_of_accounts_setup.sql

# 2. Alter product tables to add GL account references
psql -U your_user -d your_database -f alter_tables_for_gl_accounts.sql
```

### Step 2: Rebuild and Deploy Application
```bash
# Clean and rebuild
./mvnw clean package

# Run tests (optional)
./mvnw test

# Deploy the application
# (Deploy according to your deployment process)
```

### Step 3: Verify Implementation

After deployment, test each operation:

1. **Create a Savings Account**
   - Open a new savings account with initial deposit
   - Check `general_ledger` table for two entries (debit cash, credit liability)

2. **Make a Deposit**
   - Deposit money into an existing account
   - Verify GL entries created

3. **Make a Withdrawal**
   - Withdraw money from an account
   - Verify GL entries created

4. **Disburse a Loan**
   - Approve and disburse a loan
   - Verify GL entries created (debit loans receivable, credit cash)

5. **Make Loan Repayment**
   - Process a loan repayment
   - Verify two sets of GL entries (one for principal, one for interest)

### Verification Queries

```sql
-- Check if accounting entries are being created
SELECT
    gl.posting_date,
    coa.account_code,
    coa.account_name,
    gl.debit_amount,
    gl.credit_amount,
    gl.description,
    gl.reference
FROM general_ledger gl
JOIN chart_of_accounts coa ON gl.account_id = coa.id
ORDER BY gl.posting_date DESC, gl.created_at DESC
LIMIT 20;

-- Verify trial balance
SELECT
    coa.account_code,
    coa.account_name,
    coa.account_type,
    SUM(gl.debit_amount) as total_debits,
    SUM(gl.credit_amount) as total_credits,
    SUM(gl.debit_amount) - SUM(gl.credit_amount) as balance
FROM general_ledger gl
JOIN chart_of_accounts coa ON gl.account_id = coa.id
GROUP BY coa.account_code, coa.account_name, coa.account_type
ORDER BY coa.account_code;

-- Check that debits equal credits (should always be 0)
SELECT
    SUM(debit_amount) - SUM(credit_amount) as balance_check
FROM general_ledger;
```

## Transaction Type Accounting Summary

| Transaction Type | Debit Account | Credit Account | Notes |
|-----------------|---------------|----------------|-------|
| **Savings Deposit** | Cash/Bank (1101) | Member Savings (2101) | Money received from member |
| **Savings Withdrawal** | Member Savings (2101) | Cash/Bank (1101) | Money paid to member |
| **Account Opening** | Cash/Bank (1101) | Member Savings (2101) | Initial deposit |
| **Loan Disbursement** | Loans Receivable (1201) | Cash/Bank (1101) | Loan given to member |
| **Loan Repayment (Principal)** | Cash/Bank (1101) | Loans Receivable (1201) | Principal portion |
| **Loan Repayment (Interest)** | Cash/Bank (1101) | Interest Income (4101) | Interest portion |

## Important Notes

### Product Configuration Required
After running the scripts, ensure that:
1. Each `SavingsProduct` has GL accounts configured
2. Each `LoanProduct` has GL accounts configured

The `alter_tables_for_gl_accounts.sql` script automatically links existing products to default accounts, but new products must be configured during creation.

### Normal Balance Reference
- **Assets**: Normal balance is DEBIT (increases with debits, decreases with credits)
- **Liabilities**: Normal balance is CREDIT (increases with credits, decreases with debits)
- **Equity**: Normal balance is CREDIT
- **Income**: Normal balance is CREDIT (revenues increase equity)
- **Expenses**: Normal balance is DEBIT (expenses decrease equity)

### Accounting Equation
```
Assets = Liabilities + Equity
```

Every transaction maintains this balance:
- Total Debits = Total Credits (for each transaction)
- Total Assets = Total Liabilities + Total Equity (overall balance sheet)

## Troubleshooting

### Issue: No accounting entries created after transaction
**Possible Causes:**
1. Product doesn't have GL accounts configured
2. AccountingService dependency not injected properly
3. Database transaction rollback

**Solution:**
- Check product configuration in database
- Verify GL account IDs are not null
- Check application logs for errors

### Issue: Trial balance doesn't balance
**Possible Causes:**
1. Manual data entry in general_ledger table
2. Corrupted accounting entries

**Solution:**
- Run the verification query to find imbalanced entries
- Check for transactions with missing or incomplete GL entries

### Issue: Account codes not found during product linking
**Possible Causes:**
1. Chart of accounts script not run
2. Account codes changed after setup

**Solution:**
- Re-run `chart_of_accounts_setup.sql`
- Update account codes in `alter_tables_for_gl_accounts.sql` if you customized them

## Future Enhancements

### Recommended Additions
1. **Fee Handling**: Add accounting entries for withdrawal fees, processing fees
2. **Interest Accrual**: Periodic job to accrue interest on savings and loans
3. **Penalty Tracking**: Separate GL entries for late payment penalties
4. **Reversal Transactions**: Support for transaction reversals with proper accounting
5. **Audit Trail**: Enhanced tracking of who posted each GL entry
6. **Batch Processing**: Support for end-of-day batch posting
7. **Financial Statements**: Auto-generation from GL (Balance Sheet, Income Statement, Cash Flow)

## Support
For questions or issues, please refer to the main project documentation or contact the development team.

---
**Last Updated:** 2025-10-19
**Version:** 1.0
