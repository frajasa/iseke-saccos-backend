package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.entity.*;
import tz.co.iseke.enums.AccountStatus;
import tz.co.iseke.enums.PaymentMethod;
import tz.co.iseke.enums.TransactionStatus;
import tz.co.iseke.enums.TransactionType;
import tz.co.iseke.repository.SavingsAccountRepository;
import tz.co.iseke.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InterestAccrualService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountingService accountingService;
    private final TaxService taxService;
    private final AuditService auditService;

    @Scheduled(cron = "0 0 1 * * ?") // 1 AM daily
    @Transactional
    public int runDailyAccrual() {
        log.info("Starting daily savings interest accrual");

        List<SavingsAccount> activeAccounts = savingsAccountRepository.findByStatus(AccountStatus.ACTIVE);
        int accrued = 0;

        for (SavingsAccount account : activeAccounts) {
            try {
                SavingsProduct product = account.getProduct();
                if (product.getInterestRate() == null
                        || product.getInterestRate().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                // Only accrue if balance >= minimumBalance
                if (account.getBalance().compareTo(product.getMinimumBalance()) < 0) {
                    continue;
                }

                // dailyInterest = balance * (annualRate / 365)
                BigDecimal dailyRate = product.getInterestRate()
                        .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
                BigDecimal dailyInterest = account.getBalance()
                        .multiply(dailyRate)
                        .setScale(2, RoundingMode.HALF_UP);

                if (dailyInterest.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                // Accumulate accrued interest
                BigDecimal currentAccrued = account.getAccruedInterest() != null
                        ? account.getAccruedInterest() : BigDecimal.ZERO;
                account.setAccruedInterest(currentAccrued.add(dailyInterest));
                account.setUpdatedAt(LocalDateTime.now());
                savingsAccountRepository.save(account);
                accrued++;

            } catch (Exception e) {
                log.error("Error accruing interest for account {}: {}",
                        account.getAccountNumber(), e.getMessage());
            }
        }

        log.info("Daily savings interest accrual completed. {} accounts processed", accrued);
        return accrued;
    }

    @Scheduled(cron = "0 0 3 1 * ?") // 3 AM, 1st of month
    @Transactional
    public int postMonthlyInterest() {
        log.info("Starting monthly interest posting");

        List<SavingsAccount> activeAccounts = savingsAccountRepository.findByStatus(AccountStatus.ACTIVE);
        int posted = 0;

        for (SavingsAccount account : activeAccounts) {
            try {
                BigDecimal accruedInterest = account.getAccruedInterest();
                if (accruedInterest == null || accruedInterest.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                SavingsProduct product = account.getProduct();

                // Calculate withholding tax
                BigDecimal tax = taxService.calculateWithholdingTax(accruedInterest, product);
                BigDecimal netInterest = accruedInterest.subtract(tax);

                // Credit net interest to account
                account.setBalance(account.getBalance().add(netInterest));
                account.setAvailableBalance(account.getAvailableBalance().add(netInterest));
                account.setAccruedInterest(BigDecimal.ZERO);
                account.setLastInterestDate(LocalDate.now());
                account.setUpdatedAt(LocalDateTime.now());
                savingsAccountRepository.save(account);

                // Create interest transaction
                Transaction interestTxn = Transaction.builder()
                        .transactionNumber("INT" + System.currentTimeMillis() + posted)
                        .transactionDate(LocalDate.now())
                        .transactionType(TransactionType.INTEREST)
                        .savingsAccount(account)
                        .member(account.getMember())
                        .amount(netInterest)
                        .balanceBefore(account.getBalance().subtract(netInterest))
                        .balanceAfter(account.getBalance())
                        .description("Monthly interest posting (gross: " + accruedInterest
                                + ", tax: " + tax + ")")
                        .paymentMethod(PaymentMethod.CASH)
                        .branch(account.getBranch())
                        .status(TransactionStatus.COMPLETED)
                        .build();
                transactionRepository.save(interestTxn);

                // Post GL: DEBIT Interest Expense, CREDIT Savings Liability
                if (product.getInterestExpenseAccount() != null
                        && product.getLiabilityAccount() != null) {
                    accountingService.postToGeneralLedger(
                            interestTxn,
                            product.getInterestExpenseAccount(),
                            product.getLiabilityAccount(),
                            netInterest
                    );
                }

                // Post GL for tax: DEBIT Interest Expense, CREDIT Tax Payable
                if (tax.compareTo(BigDecimal.ZERO) > 0) {
                    ChartOfAccounts taxAccount = taxService.getTaxPayableAccount();
                    if (product.getInterestExpenseAccount() != null && taxAccount != null) {
                        accountingService.postToGeneralLedgerDirect(
                                LocalDate.now(),
                                product.getInterestExpenseAccount(),
                                taxAccount,
                                tax,
                                "Withholding tax on interest - " + account.getAccountNumber(),
                                interestTxn.getTransactionNumber(),
                                account.getBranch()
                        );
                    }
                }

                posted++;
            } catch (Exception e) {
                log.error("Error posting interest for account {}: {}",
                        account.getAccountNumber(), e.getMessage());
            }
        }

        log.info("Monthly interest posting completed. {} accounts processed", posted);
        auditService.logAction("MONTHLY_INTEREST_POSTED", "System", null,
                null, "Accounts processed: " + posted);
        return posted;
    }
}
