package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.inputs.DepositInput;
import tz.co.iseke.inputs.LoanRepaymentInput;
import tz.co.iseke.inputs.WithdrawInput;
import tz.co.iseke.entity.ChartOfAccounts;
import tz.co.iseke.entity.LoanAccount;
import tz.co.iseke.entity.LoanRepaymentSchedule;
import tz.co.iseke.entity.SavingsAccount;
import tz.co.iseke.entity.Transaction;
import tz.co.iseke.enums.*;
import tz.co.iseke.exception.BusinessException;
import tz.co.iseke.exception.ResourceNotFoundException;
import tz.co.iseke.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final MemberRepository memberRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final GeneralLedgerRepository generalLedgerRepository;
    private final @Lazy AccountingService accountingService;
    private final AuditService auditService;

    public Transaction findById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    public List<Transaction> findMemberTransactions(UUID memberId, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return transactionRepository.findMemberTransactionsBetweenDates(memberId, startDate, endDate);
        }
        return transactionRepository.findByMemberId(memberId);
    }

    public List<Transaction> findAccountTransactions(UUID accountId, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return transactionRepository.findAccountTransactionsBetweenDates(accountId, startDate, endDate);
        }
        return transactionRepository.findBySavingsAccount_Id(accountId);
    }

    public List<Transaction> findLoanTransactions(UUID loanId) {
        return transactionRepository.findByLoanAccount_Id(loanId);
    }

    public Transaction processDeposit(DepositInput input) {
        SavingsAccount account = savingsAccountRepository.findById(input.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // Reactivate dormant account on deposit
        if (account.getStatus() == AccountStatus.DORMANT) {
            account.setStatus(AccountStatus.ACTIVE);
            auditService.logAction("ACCOUNT_REACTIVATED", "SavingsAccount", account.getId(),
                    "Status: DORMANT", "Status: ACTIVE (deposit reactivation)");
        } else if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Account is not active");
        }

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(input.getAmount());

        // Update account balance (add to existing availableBalance to preserve holds)
        account.setBalance(balanceAfter);
        account.setAvailableBalance(account.getAvailableBalance().add(input.getAmount()));
        account.setLastTransactionDate(LocalDate.now());
        account.setLastActivityDate(LocalDate.now());
        account.setUpdatedAt(LocalDateTime.now());
        savingsAccountRepository.save(account);

        // Create transaction
        String transactionNumber = generateTransactionNumber();

        Transaction transaction = Transaction.builder()
                .transactionNumber(transactionNumber)
                .transactionDate(LocalDate.now())
                .transactionType(TransactionType.DEPOSIT)
                .savingsAccount(account)
                .member(account.getMember())
                .amount(input.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(input.getDescription())
                .referenceNumber(input.getReferenceNumber())
                .paymentMethod(input.getPaymentMethod())
                .branch(account.getBranch())
                .status(TransactionStatus.COMPLETED)
                .build();

        transaction = transactionRepository.save(transaction);

        // Post accounting entries
        if (account.getProduct().getCashAccount() != null &&
            account.getProduct().getLiabilityAccount() != null) {
            accountingService.postToGeneralLedger(
                transaction,
                account.getProduct().getCashAccount(),
                account.getProduct().getLiabilityAccount(),
                input.getAmount()
            );
        }

        auditService.logAction("DEPOSIT", "Transaction", transaction.getId(),
                null, "Amount: " + input.getAmount());

        return transaction;
    }

    public Transaction processWithdrawal(WithdrawInput input) {
        SavingsAccount account = savingsAccountRepository.findById(input.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Account is not active");
        }

        // Check withdrawal limit
        Integer withdrawalLimit = account.getProduct().getWithdrawalLimit();
        if (withdrawalLimit != null && withdrawalLimit > 0) {
            LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
            LocalDate monthEnd = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
            long withdrawalsThisMonth = transactionRepository.countByAccountAndTypeInPeriod(
                    account.getId(), TransactionType.WITHDRAWAL, monthStart, monthEnd);
            if (withdrawalsThisMonth >= withdrawalLimit) {
                throw new BusinessException("Monthly withdrawal limit of " + withdrawalLimit + " reached");
            }
        }

        BigDecimal withdrawalAmount = input.getAmount();
        BigDecimal feeAmount = BigDecimal.ZERO;

        // Calculate withdrawal fee
        if (account.getProduct().getWithdrawalFee() != null
                && account.getProduct().getWithdrawalFee().compareTo(BigDecimal.ZERO) > 0) {
            feeAmount = account.getProduct().getWithdrawalFee();
        }

        BigDecimal totalDeduction = withdrawalAmount.add(feeAmount);
        BigDecimal balanceBefore = account.getBalance();

        if (balanceBefore.compareTo(totalDeduction) < 0) {
            throw new BusinessException("Insufficient balance (including withdrawal fee of " + feeAmount + ")");
        }

        BigDecimal balanceAfter = balanceBefore.subtract(totalDeduction);

        // Check minimum balance
        if (balanceAfter.compareTo(account.getProduct().getMinimumBalance()) < 0) {
            throw new BusinessException("Withdrawal would violate minimum balance requirement");
        }

        // Update account balance (subtract from existing availableBalance to preserve holds)
        account.setBalance(balanceAfter);
        account.setAvailableBalance(account.getAvailableBalance().subtract(totalDeduction));
        account.setLastTransactionDate(LocalDate.now());
        account.setLastActivityDate(LocalDate.now());
        account.setUpdatedAt(LocalDateTime.now());
        savingsAccountRepository.save(account);

        // Create withdrawal transaction
        String transactionNumber = generateTransactionNumber();

        Transaction transaction = Transaction.builder()
                .transactionNumber(transactionNumber)
                .transactionDate(LocalDate.now())
                .transactionType(TransactionType.WITHDRAWAL)
                .savingsAccount(account)
                .member(account.getMember())
                .amount(withdrawalAmount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(input.getDescription())
                .paymentMethod(input.getPaymentMethod())
                .branch(account.getBranch())
                .status(TransactionStatus.COMPLETED)
                .build();

        transaction = transactionRepository.save(transaction);

        // Post accounting entries for withdrawal
        if (account.getProduct().getLiabilityAccount() != null &&
            account.getProduct().getCashAccount() != null) {
            accountingService.postToGeneralLedger(
                transaction,
                account.getProduct().getLiabilityAccount(),
                account.getProduct().getCashAccount(),
                withdrawalAmount
            );
        }

        // Post withdrawal fee GL entries
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            Transaction feeTransaction = Transaction.builder()
                    .transactionNumber(generateTransactionNumber())
                    .transactionDate(LocalDate.now())
                    .transactionType(TransactionType.FEE)
                    .savingsAccount(account)
                    .member(account.getMember())
                    .amount(feeAmount)
                    .description("Withdrawal fee")
                    .paymentMethod(input.getPaymentMethod())
                    .branch(account.getBranch())
                    .status(TransactionStatus.COMPLETED)
                    .build();
            feeTransaction = transactionRepository.save(feeTransaction);

            if (account.getProduct().getLiabilityAccount() != null &&
                account.getProduct().getFeeIncomeAccount() != null) {
                accountingService.postToGeneralLedger(
                    feeTransaction,
                    account.getProduct().getLiabilityAccount(),
                    account.getProduct().getFeeIncomeAccount(),
                    feeAmount
                );
            }
        }

        auditService.logAction("WITHDRAWAL", "Transaction", transaction.getId(),
                null, "Amount: " + withdrawalAmount + ", Fee: " + feeAmount);

        return transaction;
    }

    public Transaction reverseTransaction(UUID transactionId, String reason) {
        Transaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (original.getStatus() != TransactionStatus.COMPLETED) {
            throw new BusinessException("Only COMPLETED transactions can be reversed");
        }

        if (original.getReversedById() != null) {
            throw new BusinessException("Transaction has already been reversed");
        }

        // Create reversal transaction
        Transaction reversal = Transaction.builder()
                .transactionNumber(generateTransactionNumber())
                .transactionDate(LocalDate.now())
                .transactionType(original.getTransactionType())
                .savingsAccount(original.getSavingsAccount())
                .loanAccount(original.getLoanAccount())
                .member(original.getMember())
                .amount(original.getAmount().negate())
                .description("Reversal of " + original.getTransactionNumber() + ": " + reason)
                .paymentMethod(original.getPaymentMethod())
                .branch(original.getBranch())
                .status(TransactionStatus.COMPLETED)
                .reversalOfId(original.getId())
                .reversalReason(reason)
                .build();

        // Reverse savings account balance
        if (original.getSavingsAccount() != null) {
            SavingsAccount account = original.getSavingsAccount();
            BigDecimal balanceBefore = account.getBalance();
            if (original.getTransactionType() == TransactionType.DEPOSIT
                    || original.getTransactionType() == TransactionType.INTEREST) {
                account.setBalance(account.getBalance().subtract(original.getAmount()));
                account.setAvailableBalance(account.getAvailableBalance().subtract(original.getAmount()));
            } else if (original.getTransactionType() == TransactionType.WITHDRAWAL
                    || original.getTransactionType() == TransactionType.FEE
                    || original.getTransactionType() == TransactionType.TRANSFER) {
                account.setBalance(account.getBalance().add(original.getAmount()));
                account.setAvailableBalance(account.getAvailableBalance().add(original.getAmount()));
            }
            reversal.setBalanceBefore(balanceBefore);
            reversal.setBalanceAfter(account.getBalance());
            account.setUpdatedAt(LocalDateTime.now());
            savingsAccountRepository.save(account);
        }

        // Reverse loan account balance
        if (original.getLoanAccount() != null) {
            LoanAccount loan = original.getLoanAccount();
            if (original.getTransactionType() == TransactionType.LOAN_REPAYMENT) {
                loan.setOutstandingPrincipal(loan.getOutstandingPrincipal().add(original.getAmount()));
                loan.setTotalPaid(loan.getTotalPaid().subtract(original.getAmount()));
                if (loan.getStatus() == LoanStatus.CLOSED) {
                    loan.setStatus(LoanStatus.ACTIVE);
                }
            } else if (original.getTransactionType() == TransactionType.LOAN_DISBURSEMENT) {
                loan.setOutstandingPrincipal(BigDecimal.ZERO);
                loan.setStatus(LoanStatus.APPROVED);
            }
            loan.setUpdatedAt(LocalDateTime.now());
            loanAccountRepository.save(loan);
        }

        reversal = transactionRepository.save(reversal);

        // Mark original as reversed
        original.setStatus(TransactionStatus.REVERSED);
        original.setReversedById(reversal.getId());
        original.setReversalReason(reason);
        transactionRepository.save(original);

        // Reverse GL entries - collect paired debit/credit accounts and swap them
        var originalGLEntries = generalLedgerRepository.findByTransactionId(original.getId());
        // GL entries come in pairs (debit+credit). Reverse by swapping debit and credit accounts.
        ChartOfAccounts debitAccount = null;
        ChartOfAccounts creditAccount = null;
        BigDecimal glAmount = BigDecimal.ZERO;
        for (var glEntry : originalGLEntries) {
            if (glEntry.getDebitAmount().compareTo(BigDecimal.ZERO) > 0) {
                debitAccount = glEntry.getAccount();
                glAmount = glEntry.getDebitAmount();
            }
            if (glEntry.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
                creditAccount = glEntry.getAccount();
            }
            // When we have both sides, post the reversal (swap debit/credit)
            if (debitAccount != null && creditAccount != null) {
                accountingService.postToGeneralLedger(reversal, creditAccount, debitAccount, glAmount);
                debitAccount = null;
                creditAccount = null;
            }
        }

        auditService.logAction("TRANSACTION_REVERSED", "Transaction", original.getId(),
                "Status: COMPLETED", "Status: REVERSED, Reason: " + reason);

        return reversal;
    }

    public Transaction processLoanRepayment(LoanRepaymentInput input) {
        LoanAccount loan = loanAccountRepository.findById(input.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (!List.of(LoanStatus.DISBURSED, LoanStatus.ACTIVE).contains(loan.getStatus())) {
            throw new BusinessException("Loan is not active");
        }

        BigDecimal amount = input.getAmount();
        BigDecimal remainingAmount = amount;
        BigDecimal totalPrincipalPaid = BigDecimal.ZERO;
        BigDecimal totalInterestPaid = BigDecimal.ZERO;

        // Apply payment to schedule (interest first, then principal)
        List<LoanRepaymentSchedule> unpaidSchedules = scheduleRepository
                .findUnpaidByLoanId(loan.getId());

        for (LoanRepaymentSchedule schedule : unpaidSchedules) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) break;

            // Pay interest first
            BigDecimal interestOwed = schedule.getInterestDue().subtract(schedule.getInterestPaid());
            BigDecimal interestPayment = remainingAmount.min(interestOwed);
            schedule.setInterestPaid(schedule.getInterestPaid().add(interestPayment));
            totalInterestPaid = totalInterestPaid.add(interestPayment);
            remainingAmount = remainingAmount.subtract(interestPayment);

            // Then principal
            if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal principalOwed = schedule.getPrincipalDue().subtract(schedule.getPrincipalPaid());
                BigDecimal principalPayment = remainingAmount.min(principalOwed);
                schedule.setPrincipalPaid(schedule.getPrincipalPaid().add(principalPayment));
                totalPrincipalPaid = totalPrincipalPaid.add(principalPayment);
                remainingAmount = remainingAmount.subtract(principalPayment);
            }

            // Update total paid and status
            schedule.setTotalPaid(schedule.getPrincipalPaid()
                    .add(schedule.getInterestPaid())
                    .add(schedule.getFeesPaid())
                    .add(schedule.getPenaltiesPaid()));

            if (schedule.getTotalPaid().compareTo(schedule.getTotalDue()) >= 0) {
                schedule.setStatus(ScheduleStatus.PAID);
                schedule.setPaymentDate(LocalDate.now());
            } else if (schedule.getTotalPaid().compareTo(BigDecimal.ZERO) > 0) {
                schedule.setStatus(ScheduleStatus.PARTIAL);
            }

            scheduleRepository.save(schedule);
        }

        // Update loan balances using current payment amounts (not cumulative)
        loan.setTotalPaid(loan.getTotalPaid().add(amount));

        loan.setOutstandingPrincipal(loan.getOutstandingPrincipal().subtract(totalPrincipalPaid));
        loan.setOutstandingInterest(loan.getOutstandingInterest().subtract(totalInterestPaid));

        // Close loan if fully repaid (principal, interest, and penalties)
        if (loan.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) <= 0
                && loan.getOutstandingInterest().compareTo(BigDecimal.ZERO) <= 0
                && loan.getOutstandingPenalties().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(LoanStatus.CLOSED);
        }

        loan.setUpdatedAt(LocalDateTime.now());
        loanAccountRepository.save(loan);

        // Create transaction
        String transactionNumber = generateTransactionNumber();

        Transaction transaction = Transaction.builder()
                .transactionNumber(transactionNumber)
                .transactionDate(LocalDate.now())
                .transactionType(TransactionType.LOAN_REPAYMENT)
                .loanAccount(loan)
                .member(loan.getMember())
                .amount(amount)
                .description("Loan repayment")
                .referenceNumber(input.getReferenceNumber())
                .paymentMethod(input.getPaymentMethod())
                .branch(loan.getBranch())
                .status(TransactionStatus.COMPLETED)
                .build();

        transactionRepository.save(transaction);

        // Post accounting entries - Principal repayment
        if (totalPrincipalPaid.compareTo(BigDecimal.ZERO) > 0 &&
                loan.getProduct().getCashAccount() != null &&
                loan.getProduct().getLoanReceivableAccount() != null) {
            accountingService.postToGeneralLedger(
                    transaction,
                    loan.getProduct().getCashAccount(),
                    loan.getProduct().getLoanReceivableAccount(),
                    totalPrincipalPaid
            );
        }

        // Interest repayment
        if (totalInterestPaid.compareTo(BigDecimal.ZERO) > 0 &&
                loan.getProduct().getCashAccount() != null &&
                loan.getProduct().getInterestIncomeAccount() != null) {
            accountingService.postToGeneralLedger(
                    transaction,
                    loan.getProduct().getCashAccount(),
                    loan.getProduct().getInterestIncomeAccount(),
                    totalInterestPaid
            );
        }

        auditService.logAction("LOAN_REPAYMENT", "Transaction", transaction.getId(),
                null, "Amount: " + amount);

        return transaction;
    }

    public Transaction processInterBranchTransfer(UUID fromAccountId, UUID toAccountId,
                                                     BigDecimal amount, String description) {
        SavingsAccount fromAccount = savingsAccountRepository.findById(fromAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
        SavingsAccount toAccount = savingsAccountRepository.findById(toAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Source account is not active");
        }
        if (toAccount.getStatus() != AccountStatus.ACTIVE && toAccount.getStatus() != AccountStatus.DORMANT) {
            throw new BusinessException("Destination account is not active");
        }

        if (fromAccount.getBranch() != null && toAccount.getBranch() != null
                && fromAccount.getBranch().getId().equals(toAccount.getBranch().getId())) {
            throw new BusinessException("Inter-branch transfer requires accounts in different branches");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient balance for transfer");
        }

        // Debit source account
        BigDecimal fromBalanceBefore = fromAccount.getBalance();
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        fromAccount.setAvailableBalance(fromAccount.getAvailableBalance().subtract(amount));
        fromAccount.setLastTransactionDate(LocalDate.now());
        fromAccount.setLastActivityDate(LocalDate.now());
        fromAccount.setUpdatedAt(LocalDateTime.now());
        savingsAccountRepository.save(fromAccount);

        // Credit destination account
        BigDecimal toBalanceBefore = toAccount.getBalance();
        toAccount.setBalance(toAccount.getBalance().add(amount));
        toAccount.setAvailableBalance(toAccount.getAvailableBalance().add(amount));
        toAccount.setLastTransactionDate(LocalDate.now());
        toAccount.setLastActivityDate(LocalDate.now());
        if (toAccount.getStatus() == AccountStatus.DORMANT) {
            toAccount.setStatus(AccountStatus.ACTIVE);
        }
        toAccount.setUpdatedAt(LocalDateTime.now());
        savingsAccountRepository.save(toAccount);

        String txnDesc = description != null ? description : "Inter-branch transfer";

        // Debit transaction (source branch)
        Transaction debitTxn = Transaction.builder()
                .transactionNumber(generateTransactionNumber())
                .transactionDate(LocalDate.now())
                .transactionType(TransactionType.TRANSFER)
                .savingsAccount(fromAccount)
                .member(fromAccount.getMember())
                .amount(amount)
                .balanceBefore(fromBalanceBefore)
                .balanceAfter(fromAccount.getBalance())
                .description(txnDesc + " (OUT to " + toAccount.getAccountNumber() + ")")
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .branch(fromAccount.getBranch())
                .status(TransactionStatus.COMPLETED)
                .build();
        debitTxn = transactionRepository.save(debitTxn);

        // Credit transaction (destination branch)
        Transaction creditTxn = Transaction.builder()
                .transactionNumber(generateTransactionNumber())
                .transactionDate(LocalDate.now())
                .transactionType(TransactionType.TRANSFER)
                .savingsAccount(toAccount)
                .member(toAccount.getMember())
                .amount(amount)
                .balanceBefore(toBalanceBefore)
                .balanceAfter(toAccount.getBalance())
                .description(txnDesc + " (IN from " + fromAccount.getAccountNumber() + ")")
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .branch(toAccount.getBranch())
                .status(TransactionStatus.COMPLETED)
                .build();
        transactionRepository.save(creditTxn);

        // GL entries for source branch
        if (fromAccount.getProduct().getLiabilityAccount() != null && fromAccount.getProduct().getCashAccount() != null) {
            accountingService.postToGeneralLedger(debitTxn,
                    fromAccount.getProduct().getLiabilityAccount(),
                    fromAccount.getProduct().getCashAccount(), amount);
        }

        // GL entries for destination branch
        if (toAccount.getProduct().getCashAccount() != null && toAccount.getProduct().getLiabilityAccount() != null) {
            accountingService.postToGeneralLedger(creditTxn,
                    toAccount.getProduct().getCashAccount(),
                    toAccount.getProduct().getLiabilityAccount(), amount);
        }

        auditService.logAction("INTER_BRANCH_TRANSFER", "Transaction", debitTxn.getId(),
                null, "Amount: " + amount + ", From: " + fromAccount.getAccountNumber() + ", To: " + toAccount.getAccountNumber());

        return debitTxn;
    }

    private BigDecimal calculateTotalInterest(LoanAccount loan) {
        return scheduleRepository.findByLoanIdOrderByInstallmentNumber(loan.getId())
                .stream()
                .map(LoanRepaymentSchedule::getInterestDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateTransactionNumber() {
        String datePrefix = LocalDate.now().toString().replace("-", "");
        return String.format("TXN%s%s", datePrefix, UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
