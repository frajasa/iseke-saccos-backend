package tz.co.iseke.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.co.iseke.entity.*;
import tz.co.iseke.enums.*;
import tz.co.iseke.exception.BusinessException;
import tz.co.iseke.inputs.DepositInput;
import tz.co.iseke.inputs.LoanRepaymentInput;
import tz.co.iseke.inputs.WithdrawInput;
import tz.co.iseke.repository.*;
import tz.co.iseke.testutil.TestDataBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private LoanAccountRepository loanAccountRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private LoanRepaymentScheduleRepository scheduleRepository;
    @Mock private GeneralLedgerRepository generalLedgerRepository;
    @Mock private AccountingService accountingService;
    @Mock private AuditService auditService;

    @InjectMocks
    private TransactionService transactionService;

    private Member member;
    private Branch branch;
    private SavingsProduct product;
    private SavingsAccount account;

    @BeforeEach
    void setUp() {
        member = TestDataBuilder.aMember();
        branch = TestDataBuilder.aBranch();
        product = TestDataBuilder.aSavingsProduct();
        account = TestDataBuilder.aSavingsAccount(member, product, branch);

        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> {
                    Transaction t = inv.getArgument(0);
                    if (t.getId() == null) t.setId(UUID.randomUUID());
                    return t;
                });
        lenient().when(transactionRepository.count()).thenReturn(0L);
    }

    // ===== DEPOSIT TESTS =====

    @Test
    void deposit_success_updatesBalanceAndCreatesTransaction() {
        when(savingsAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        DepositInput input = TestDataBuilder.aDepositInput(account.getId(), new BigDecimal("100000"));
        transactionService.processDeposit(input);

        assertEquals(new BigDecimal("600000"), account.getBalance());
        verify(savingsAccountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void deposit_inactiveAccount_throwsBusinessException() {
        account.setStatus(AccountStatus.INACTIVE);
        when(savingsAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        DepositInput input = TestDataBuilder.aDepositInput(account.getId(), new BigDecimal("100000"));

        assertThrows(BusinessException.class, () -> transactionService.processDeposit(input));
    }

    @Test
    void deposit_dormantAccount_reactivatesToActive() {
        account.setStatus(AccountStatus.DORMANT);
        when(savingsAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        DepositInput input = TestDataBuilder.aDepositInput(account.getId(), new BigDecimal("100000"));
        transactionService.processDeposit(input);

        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertEquals(new BigDecimal("600000"), account.getBalance());
    }

    @Test
    void deposit_withGlAccounts_postsGlEntries() {
        when(savingsAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        DepositInput input = TestDataBuilder.aDepositInput(account.getId(), new BigDecimal("100000"));
        transactionService.processDeposit(input);

        verify(accountingService).postToGeneralLedger(
                any(Transaction.class),
                eq(product.getCashAccount()),
                eq(product.getLiabilityAccount()),
                eq(new BigDecimal("100000"))
        );
    }

    @Test
    void deposit_missingGlAccounts_skipsGlPosting() {
        product.setCashAccount(null);
        when(savingsAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        DepositInput input = TestDataBuilder.aDepositInput(account.getId(), new BigDecimal("100000"));
        transactionService.processDeposit(input);

        verify(accountingService, never()).postToGeneralLedger(any(), any(), any(), any());
    }

    // ===== WITHDRAWAL TESTS =====

    @Test
    void withdrawal_success_deductsBalanceAndCreatesTransaction() {
        when(savingsAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        WithdrawInput input = TestDataBuilder.aWithdrawInput(account.getId(), new BigDecimal("100000"));
        Transaction txn = transactionService.processWithdrawal(input);

        assertEquals(new BigDecimal("400000"), account.getBalance());
        assertNotNull(txn);
    }

    @Test
    void withdrawal_insufficientBalance_throwsBusinessException() {
        when(savingsAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        WithdrawInput input = TestDataBuilder.aWithdrawInput(account.getId(), new BigDecimal("600000"));

        assertThrows(BusinessException.class, () -> transactionService.processWithdrawal(input));
    }

    @Test
    void withdrawal_belowMinimumBalance_throwsBusinessException() {
        product.setMinimumBalance(new BigDecimal("450000"));
        when(savingsAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        WithdrawInput input = TestDataBuilder.aWithdrawInput(account.getId(), new BigDecimal("100000"));

        assertThrows(BusinessException.class, () -> transactionService.processWithdrawal(input));
    }

    @Test
    void withdrawal_inactiveAccount_throwsBusinessException() {
        account.setStatus(AccountStatus.INACTIVE);
        when(savingsAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        WithdrawInput input = TestDataBuilder.aWithdrawInput(account.getId(), new BigDecimal("100000"));

        assertThrows(BusinessException.class, () -> transactionService.processWithdrawal(input));
    }

    @Test
    void withdrawal_exceedsMonthlyLimit_throwsBusinessException() {
        product.setWithdrawalLimit(2);
        when(savingsAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(transactionRepository.countByAccountAndTypeInPeriod(
                eq(account.getId()), eq(TransactionType.WITHDRAWAL), any(), any()))
                .thenReturn(2L);

        WithdrawInput input = TestDataBuilder.aWithdrawInput(account.getId(), new BigDecimal("10000"));

        assertThrows(BusinessException.class, () -> transactionService.processWithdrawal(input));
    }

    @Test
    void withdrawal_withFee_deductsAmountPlusFee() {
        product.setWithdrawalFee(new BigDecimal("1000"));
        when(savingsAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        WithdrawInput input = TestDataBuilder.aWithdrawInput(account.getId(), new BigDecimal("100000"));
        transactionService.processWithdrawal(input);

        // 500000 - 100000 - 1000 = 399000
        assertEquals(new BigDecimal("399000"), account.getBalance());
    }

    @Test
    void withdrawal_feeGlEntriesPosted() {
        product.setWithdrawalFee(new BigDecimal("1000"));
        when(savingsAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        WithdrawInput input = TestDataBuilder.aWithdrawInput(account.getId(), new BigDecimal("100000"));
        transactionService.processWithdrawal(input);

        // Withdrawal GL + Fee GL = 2 calls
        verify(accountingService, times(2)).postToGeneralLedger(any(), any(), any(), any());
    }

    // ===== LOAN REPAYMENT TESTS =====

    @Test
    void loanRepayment_allocatesInterestFirstThenPrincipal() {
        LoanProduct loanProduct = TestDataBuilder.aLoanProduct(InterestMethod.FLAT);
        LoanAccount loan = TestDataBuilder.aLoanAccount(member, loanProduct, branch);

        LoanRepaymentSchedule schedule = TestDataBuilder.aScheduleEntry(
                loan, 1, new BigDecimal("80000"), new BigDecimal("20000"));

        when(loanAccountRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(scheduleRepository.findUnpaidByLoanId(loan.getId()))
                .thenReturn(List.of(schedule));
        when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(loan.getId()))
                .thenReturn(List.of(schedule));

        LoanRepaymentInput input = TestDataBuilder.aLoanRepaymentInput(loan.getId(), new BigDecimal("50000"));
        Transaction txn = transactionService.processLoanRepayment(input);

        // Interest first: 20000 paid, then principal: 30000 paid
        assertEquals(new BigDecimal("20000"), schedule.getInterestPaid());
        assertEquals(new BigDecimal("30000"), schedule.getPrincipalPaid());
        assertNotNull(txn);
    }

    @Test
    void loanRepayment_partialPayment_scheduleMarkedPartial() {
        LoanProduct loanProduct = TestDataBuilder.aLoanProduct(InterestMethod.FLAT);
        LoanAccount loan = TestDataBuilder.aLoanAccount(member, loanProduct, branch);

        LoanRepaymentSchedule schedule = TestDataBuilder.aScheduleEntry(
                loan, 1, new BigDecimal("80000"), new BigDecimal("20000"));

        when(loanAccountRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(scheduleRepository.findUnpaidByLoanId(loan.getId()))
                .thenReturn(List.of(schedule));
        when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(loan.getId()))
                .thenReturn(List.of(schedule));

        LoanRepaymentInput input = TestDataBuilder.aLoanRepaymentInput(loan.getId(), new BigDecimal("50000"));
        transactionService.processLoanRepayment(input);

        assertEquals(ScheduleStatus.PARTIAL, schedule.getStatus());
    }

    @Test
    void loanRepayment_fullPayoff_loanStatusClosed() {
        LoanProduct loanProduct = TestDataBuilder.aLoanProduct(InterestMethod.FLAT);
        LoanAccount loan = TestDataBuilder.aLoanAccount(member, loanProduct, branch);

        LoanRepaymentSchedule schedule = TestDataBuilder.aScheduleEntry(
                loan, 1, new BigDecimal("1000000"), new BigDecimal("15000"));

        when(loanAccountRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(scheduleRepository.findUnpaidByLoanId(loan.getId()))
                .thenReturn(List.of(schedule));

        // After repayment, principal paid = full
        LoanRepaymentSchedule paidSchedule = TestDataBuilder.aScheduleEntry(
                loan, 1, new BigDecimal("1000000"), new BigDecimal("15000"));
        paidSchedule.setPrincipalPaid(new BigDecimal("1000000"));
        paidSchedule.setInterestPaid(new BigDecimal("15000"));
        when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(loan.getId()))
                .thenReturn(List.of(paidSchedule));

        LoanRepaymentInput input = TestDataBuilder.aLoanRepaymentInput(loan.getId(), new BigDecimal("1015000"));
        transactionService.processLoanRepayment(input);

        assertEquals(LoanStatus.CLOSED, loan.getStatus());
    }

    @Test
    void loanRepayment_inactiveLoan_throwsBusinessException() {
        LoanProduct loanProduct = TestDataBuilder.aLoanProduct(InterestMethod.FLAT);
        LoanAccount loan = TestDataBuilder.aLoanAccount(member, loanProduct, branch);
        loan.setStatus(LoanStatus.CLOSED);

        when(loanAccountRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        LoanRepaymentInput input = TestDataBuilder.aLoanRepaymentInput(loan.getId(), new BigDecimal("50000"));

        assertThrows(BusinessException.class, () -> transactionService.processLoanRepayment(input));
    }

    // ===== REVERSAL TESTS =====

    @Test
    void reversal_success_createsReversalAndMarksOriginal() {
        Transaction original = TestDataBuilder.aTransaction(member, account, new BigDecimal("100000"));
        original.setStatus(TransactionStatus.COMPLETED);

        when(transactionRepository.findById(original.getId())).thenReturn(Optional.of(original));
        when(generalLedgerRepository.findByTransactionId(original.getId())).thenReturn(Collections.emptyList());

        Transaction reversal = transactionService.reverseTransaction(original.getId(), "Error correction");

        assertEquals(TransactionStatus.REVERSED, original.getStatus());
        assertNotNull(original.getReversedById());
        assertEquals(new BigDecimal("-100000"), reversal.getAmount());
    }

    @Test
    void reversal_nonCompletedTransaction_throwsBusinessException() {
        Transaction original = TestDataBuilder.aTransaction(member, account, new BigDecimal("100000"));
        original.setStatus(TransactionStatus.PENDING);

        when(transactionRepository.findById(original.getId())).thenReturn(Optional.of(original));

        assertThrows(BusinessException.class,
                () -> transactionService.reverseTransaction(original.getId(), "reason"));
    }

    @Test
    void reversal_alreadyReversed_throwsBusinessException() {
        Transaction original = TestDataBuilder.aTransaction(member, account, new BigDecimal("100000"));
        original.setStatus(TransactionStatus.COMPLETED);
        original.setReversedById(UUID.randomUUID());

        when(transactionRepository.findById(original.getId())).thenReturn(Optional.of(original));

        assertThrows(BusinessException.class,
                () -> transactionService.reverseTransaction(original.getId(), "reason"));
    }
}
