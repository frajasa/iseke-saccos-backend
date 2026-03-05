# Deployment Checklist - Accounting System Implementation

## Pre-Deployment

### Backup
- [ ] Backup your database before making any changes
```bash
pg_dump -U your_user your_database > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Review Files
- [ ] Review `chart_of_accounts_setup.sql`
- [ ] Review `alter_tables_for_gl_accounts.sql`
- [ ] Review modified Java files
- [ ] Review `ACCOUNTING_IMPLEMENTATION_GUIDE.md`

## Deployment Steps

### Step 1: Database Changes
- [ ] Run chart of accounts setup script
```bash
psql -U your_user -d your_database -f chart_of_accounts_setup.sql
```

- [ ] Verify chart of accounts created
```sql
SELECT COUNT(*) FROM chart_of_accounts;
-- Should return approximately 30-35 accounts
```

- [ ] Run product table alterations
```bash
psql -U your_user -d your_database -f alter_tables_for_gl_accounts.sql
```

- [ ] Verify GL account columns added
```sql
\d savings_products
\d loan_products
-- Should show new GL account columns
```

- [ ] Verify existing products are linked to GL accounts
```sql
SELECT product_name, liability_account_id, cash_account_id
FROM savings_products;

SELECT product_name, loan_receivable_account_id, cash_account_id
FROM loan_products;
-- All should have non-null account IDs
```

### Step 2: Application Build
- [ ] Clean previous build
```bash
./mvnw clean
```

- [ ] Compile application
```bash
./mvnw compile
```

- [ ] Run tests (optional but recommended)
```bash
./mvnw test
```

- [ ] Package application
```bash
./mvnw package
```

- [ ] Verify build successful (check target/ directory for .jar file)

### Step 3: Application Deployment
- [ ] Stop current application instance
- [ ] Deploy new build
- [ ] Start application
- [ ] Check application logs for errors
- [ ] Verify application started successfully

## Post-Deployment Testing

### Test 1: Savings Deposit
- [ ] Make a deposit to an existing savings account
- [ ] Verify transaction created successfully
- [ ] Check general_ledger table
```sql
SELECT
    gl.posting_date,
    coa.account_code,
    coa.account_name,
    gl.debit_amount,
    gl.credit_amount,
    gl.description
FROM general_ledger gl
JOIN chart_of_accounts coa ON gl.account_id = coa.id
WHERE gl.reference = 'YOUR_TRANSACTION_NUMBER'
ORDER BY gl.created_at;
```
- [ ] Verify 2 entries created (DR Cash, CR Liability)
- [ ] Verify amounts match

### Test 2: Savings Withdrawal
- [ ] Make a withdrawal from a savings account
- [ ] Verify transaction created successfully
- [ ] Check general_ledger table (same query as above)
- [ ] Verify 2 entries created (DR Liability, CR Cash)
- [ ] Verify amounts match

### Test 3: Account Opening
- [ ] Open a new savings account with initial deposit
- [ ] Verify account created successfully
- [ ] Check general_ledger table
- [ ] Verify 2 entries created for opening deposit
- [ ] Verify amounts match initial deposit

### Test 4: Loan Disbursement
- [ ] Approve and disburse a loan
- [ ] Verify loan status changed to DISBURSED
- [ ] Check general_ledger table
- [ ] Verify 2 entries created (DR Loans Receivable, CR Cash)
- [ ] Verify amounts match loan principal

### Test 5: Loan Repayment
- [ ] Make a loan repayment
- [ ] Verify loan balances updated
- [ ] Check general_ledger table
- [ ] Verify 4 entries created:
  - DR Cash (for principal)
  - CR Loans Receivable (for principal)
  - DR Cash (for interest)
  - CR Interest Income (for interest)
- [ ] Verify principal and interest amounts are correct

## Verification Queries

### Overall Balance Check
```sql
-- This should ALWAYS return 0
SELECT SUM(debit_amount) - SUM(credit_amount) as balance_check
FROM general_ledger;
```
- [ ] Result is 0 ✅

### Trial Balance
```sql
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
```
- [ ] Review trial balance
- [ ] Verify asset accounts have debit balances
- [ ] Verify liability accounts have credit balances
- [ ] Verify income accounts have credit balances

### Recent Transactions
```sql
SELECT
    gl.posting_date,
    t.transaction_number,
    t.transaction_type,
    coa.account_code,
    coa.account_name,
    gl.debit_amount,
    gl.credit_amount,
    gl.description
FROM general_ledger gl
JOIN chart_of_accounts coa ON gl.account_id = coa.id
LEFT JOIN transactions t ON gl.transaction_id = t.id
ORDER BY gl.created_at DESC
LIMIT 20;
```
- [ ] Review recent GL entries
- [ ] Verify all transactions have corresponding GL entries

## Rollback Plan (If Issues Found)

### If Database Issues
```bash
# Restore from backup
psql -U your_user -d your_database < backup_YYYYMMDD_HHMMSS.sql
```

### If Application Issues
- [ ] Stop new application
- [ ] Restore previous application version
- [ ] Start previous application
- [ ] Investigate logs
- [ ] Fix issues
- [ ] Re-deploy

## Sign-off

### Database Administrator
- [ ] Database changes verified
- [ ] Signature: _________________ Date: _________

### Developer
- [ ] Code changes verified
- [ ] Tests passed
- [ ] Signature: _________________ Date: _________

### QA/Tester
- [ ] All test cases passed
- [ ] GL entries verified
- [ ] Signature: _________________ Date: _________

### Project Manager
- [ ] Deployment approved
- [ ] Documentation complete
- [ ] Signature: _________________ Date: _________

## Notes and Issues

Record any issues encountered during deployment:

```
Issue 1:
Description:
Resolution:

Issue 2:
Description:
Resolution:
```

## Success Criteria

Deployment is successful when:
- ✅ All database scripts run without errors
- ✅ Application builds and starts successfully
- ✅ All 5 test cases pass
- ✅ Balance check query returns 0
- ✅ Trial balance shows proper account balances
- ✅ No errors in application logs

---

**Deployment Date:** ______________
**Deployed By:** ______________
**Environment:** ☐ Development  ☐ Staging  ☐ Production
**Status:** ☐ Success  ☐ Failed  ☐ Rolled Back
