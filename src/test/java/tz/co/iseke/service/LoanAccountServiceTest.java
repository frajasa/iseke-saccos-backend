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
import tz.co.iseke.inputs.LoanApplicationInput;
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
class LoanAccountServiceTest {

    @Mock private LoanAccountRepository loanAccountRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private LoanProductRepository loanProductRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private GuarantorRepository guarantorRepository;
    @Mock private CollateralRepository collateralRepository;
    @Mock private LoanRepaymentScheduleRepository scheduleRepository;
    @Mock private AccountingService accountingService;
    @Mock private TransactionRepository transactionRepository;
    @Mock private AuditService auditService;
    @Mock private ChartOfAccountsRepository chartRepository;

    @InjectMocks
    private LoanAccountService loanAccountService;

    private Member member;
    private Branch branch;
    private LoanProduct flatProduct;

    @BeforeEach
    void setUp() {
        member = TestDataBuilder.aMember();
        branch = TestDataBuilder.aBranch();
        flatProduct = TestDataBuilder.aLoanProduct(InterestMethod.FLAT);

        lenient().when(loanAccountRepository.save(any(LoanAccount.class)))
                .thenAnswer(inv -> {
                    LoanAccount l = inv.getArgument(0);
                    if (l.getId() == null) l.setId(UUID.randomUUID());
                    return l;
                });
        lenient().when(loanAccountRepository.count()).thenReturn(0L);
        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> {
                    Transaction t = inv.getArgument(0);
                    if (t.getId() == null) t.setId(UUID.randomUUID());
                    return t;
                });
        lenient().when(transactionRepository.count()).thenReturn(0L);
        lenient().when(scheduleRepository.save(any(LoanRepaymentSchedule.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ===== APPLICATION TESTS =====

    @Test
    void applyForLoan_success() {
        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(loanProductRepository.findById(flatProduct.getId())).thenReturn(Optional.of(flatProduct));

        LoanApplicationInput input = LoanApplicationInput.builder()
                .memberId(member.getId())
                .productId(flatProduct.getId())
                .requestedAmount(new BigDecimal("500000"))
                .termMonths(12)
                .purpose("Business")
                .build();

        LoanAccount result = loanAccountService.applyForLoan(input);

        assertNotNull(result);
        assertEquals(LoanStatus.APPLIED, result.getStatus());
        assertEquals(new BigDecimal("500000"), result.getPrincipalAmount());
    }

    @Test
    void applyForLoan_amountBelowMin_throwsBusinessException() {
        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(loanProductRepository.findById(flatProduct.getId())).thenReturn(Optional.of(flatProduct));

        LoanApplicationInput input = LoanApplicationInput.builder()
                .memberId(member.getId())
                .productId(flatProduct.getId())
                .requestedAmount(new BigDecimal("50000")) // below 100000 min
                .termMonths(12)
                .build();

        assertThrows(BusinessException.class, () -> loanAccountService.applyForLoan(input));
    }

    @Test
    void applyForLoan_amountAboveMax_throwsBusinessException() {
        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(loanProductRepository.findById(flatProduct.getId())).thenReturn(Optional.of(flatProduct));

        LoanApplicationInput input = LoanApplicationInput.builder()
                .memberId(member.getId())
                .productId(flatProduct.getId())
                .requestedAmount(new BigDecimal("100000000")) // above 50M max
                .termMonths(12)
                .build();

        assertThrows(BusinessException.class, () -> loanAccountService.applyForLoan(input));
    }

    @Test
    void applyForLoan_termBelowMin_throwsBusinessException() {
        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(loanProductRepository.findById(flatProduct.getId())).thenReturn(Optional.of(flatProduct));

        LoanApplicationInput input = LoanApplicationInput.builder()
                .memberId(member.getId())
                .productId(flatProduct.getId())
                .requestedAmount(new BigDecimal("500000"))
                .termMonths(0) // below 1 min
                .build();

        assertThrows(BusinessException.class, () -> loanAccountService.applyForLoan(input));
    }

    @Test
    void applyForLoan_termAboveMax_throwsBusinessException() {
        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(loanProductRepository.findById(flatProduct.getId())).thenReturn(Optional.of(flatProduct));

        LoanApplicationInput input = LoanApplicationInput.builder()
                .memberId(member.getId())
                .productId(flatProduct.getId())
                .requestedAmount(new BigDecimal("500000"))
                .termMonths(120) // above 60 max
                .build();

        assertThrows(BusinessException.class, () -> loanAccountService.applyForLoan(input));
    }

    // ===== APPROVAL TESTS =====

    @Test
    void approveLoan_success() {
        LoanAccount loan = TestDataBuilder.aLoanAccount(member, flatProduct, branch);
        loan.setStatus(LoanStatus.APPLIED);

        when(loanAccountRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        LoanAccount result = loanAccountService.approveLoan(loan.getId(), null);

        assertEquals(LoanStatus.APPROVED, result.getStatus());
        assertNotNull(result.getApprovalDate());
    }

    @Test
    void approveLoan_withDifferentAmount() {
        LoanAccount loan = TestDataBuilder.aLoanAccount(member, flatProduct, branch);
        loan.setStatus(LoanStatus.APPLIED);

        when(loanAccountRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        loanAccountService.approveLoan(loan.getId(), new BigDecimal("800000"));

        assertEquals(new BigDecimal("800000"), loan.getPrincipalAmount());
    }

    @Test
    void approveLoan_nonAppliedStatus_throwsBusinessException() {
        LoanAccount loan = TestDataBuilder.aLoanAccount(member, flatProduct, branch);
        loan.setStatus(LoanStatus.DISBURSED);

        when(loanAccountRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        assertThrows(BusinessException.class, () -> loanAccountService.approveLoan(loan.getId(), null));
    }

    // ===== DISBURSEMENT TESTS =====

    @Test
    void disburseLoan_success_generatesSchedule() {
        LoanAccount loan = TestDataBuilder.aLoanAccount(member, flatProduct, branch);
        loan.setStatus(LoanStatus.APPROVED);
        loan.setPrincipalAmount(new BigDecimal("1200000"));
        loan.setTermMonths(12);
        loan.setInterestRate(new BigDecimal("0.18"));

        when(loanAccountRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        LoanAccount result = loanAccountService.disburseLoan(loan.getId(), LocalDate.now());

        assertEquals(LoanStatus.DISBURSED, result.getStatus());
        assertEquals(new BigDecimal("1200000"), result.getOutstandingPrincipal());
        // Verify schedule entries were saved (12 months)
        verify(scheduleRepository, times(12)).save(any(LoanRepaymentSchedule.class));
    }

    @Test
    void disburseLoan_missingGuarantors_throwsBusinessException() {
        LoanProduct productWithGuarantors = TestDataBuilder.aLoanProduct(InterestMethod.FLAT);
        productWithGuarantors.setRequiresGuarantors(true);
        productWithGuarantors.setMinimumGuarantors(2);

        LoanAccount loan = TestDataBuilder.aLoanAccount(member, productWithGuarantors, branch);
        loan.setStatus(LoanStatus.APPROVED);

        when(loanAccountRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(guarantorRepository.findActiveGuarantorsByLoanId(loan.getId())).thenReturn(Collections.emptyList());

        assertThrows(BusinessException.class, () -> loanAccountService.disburseLoan(loan.getId(), LocalDate.now()));
    }

    @Test
    void disburseLoan_missingCollateral_throwsBusinessException() {
        LoanProduct productWithCollateral = TestDataBuilder.aLoanProduct(InterestMethod.FLAT);
        productWithCollateral.setRequiresCollateral(true);

        LoanAccount loan = TestDataBuilder.aLoanAccount(member, productWithCollateral, branch);
        loan.setStatus(LoanStatus.APPROVED);

        when(loanAccountRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(collateralRepository.findActiveCollateralByLoanId(loan.getId())).thenReturn(Collections.emptyList());

        assertThrows(BusinessException.class, () -> loanAccountService.disburseLoan(loan.getId(), LocalDate.now()));
    }

    // ===== SCHEDULE GENERATION TESTS =====

    @Test
    void flatRateSchedule_equalInstallments() {
        LoanAccount loan = TestDataBuilder.aLoanAccount(member, flatProduct, branch);
        loan.setStatus(LoanStatus.APPROVED);
        loan.setPrincipalAmount(new BigDecimal("1200000"));
        loan.setTermMonths(12);
        loan.setInterestRate(new BigDecimal("0.18"));

        when(loanAccountRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        loanAccountService.disburseLoan(loan.getId(), LocalDate.now());

        ArgumentCaptor<LoanRepaymentSchedule> captor = ArgumentCaptor.forClass(LoanRepaymentSchedule.class);
        verify(scheduleRepository, times(12)).save(captor.capture());

        List<LoanRepaymentSchedule> schedules = captor.getAllValues();
        // Flat rate: equal principal per installment
        BigDecimal expectedPrincipal = new BigDecimal("100000.00");
        assertEquals(0, expectedPrincipal.compareTo(schedules.get(0).getPrincipalDue()));
        // All installments should have same interest
        assertEquals(schedules.get(0).getInterestDue(), schedules.get(5).getInterestDue());
    }

    @Test
    void decliningBalanceSchedule_decreasingInterest() {
        LoanProduct declProduct = TestDataBuilder.aLoanProduct(InterestMethod.DECLINING_BALANCE);
        LoanAccount loan = TestDataBuilder.aLoanAccount(member, declProduct, branch);
        loan.setStatus(LoanStatus.APPROVED);
        loan.setPrincipalAmount(new BigDecimal("1200000"));
        loan.setTermMonths(12);
        loan.setInterestRate(new BigDecimal("0.18"));

        when(loanAccountRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        loanAccountService.disburseLoan(loan.getId(), LocalDate.now());

        ArgumentCaptor<LoanRepaymentSchedule> captor = ArgumentCaptor.forClass(LoanRepaymentSchedule.class);
        verify(scheduleRepository, times(12)).save(captor.capture());

        List<LoanRepaymentSchedule> schedules = captor.getAllValues();
        // Interest should decrease over time (declining balance)
        assertTrue(schedules.get(0).getInterestDue().compareTo(schedules.get(11).getInterestDue()) > 0);
    }

    @Test
    void reducingBalanceSchedule_equalPrincipal() {
        LoanProduct redProduct = TestDataBuilder.aLoanProduct(InterestMethod.REDUCING_BALANCE);
        LoanAccount loan = TestDataBuilder.aLoanAccount(member, redProduct, branch);
        loan.setStatus(LoanStatus.APPROVED);
        loan.setPrincipalAmount(new BigDecimal("1200000"));
        loan.setTermMonths(12);
        loan.setInterestRate(new BigDecimal("0.18"));

        when(loanAccountRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        loanAccountService.disburseLoan(loan.getId(), LocalDate.now());

        ArgumentCaptor<LoanRepaymentSchedule> captor = ArgumentCaptor.forClass(LoanRepaymentSchedule.class);
        verify(scheduleRepository, times(12)).save(captor.capture());

        List<LoanRepaymentSchedule> schedules = captor.getAllValues();
        // Equal principal payments (first 11 should be the same)
        assertEquals(schedules.get(0).getPrincipalDue(), schedules.get(5).getPrincipalDue());
        // Interest should decrease
        assertTrue(schedules.get(0).getInterestDue().compareTo(schedules.get(11).getInterestDue()) > 0);
    }

    // ===== WRITE-OFF & REFINANCE TESTS =====

    @Test
    void writeOffLoan_postsGlEntries() {
        LoanAccount loan = TestDataBuilder.aLoanAccount(member, flatProduct, branch);
        loan.setStatus(LoanStatus.DISBURSED);
        loan.setOutstandingPrincipal(new BigDecimal("500000"));
        loan.setOutstandingInterest(BigDecimal.ZERO);
        loan.setOutstandingPenalties(BigDecimal.ZERO);

        ChartOfAccounts loanLossExpense = TestDataBuilder.aGlAccount("5301", "Loan Loss Expense");

        when(loanAccountRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(chartRepository.findByAccountCode("5301")).thenReturn(Optional.of(loanLossExpense));

        loanAccountService.writeOffLoan(loan.getId(), "Non-performing");

        assertEquals(LoanStatus.WRITTEN_OFF, loan.getStatus());
        verify(accountingService).postToGeneralLedgerDirect(
                any(LocalDate.class), eq(loanLossExpense), eq(flatProduct.getLoanReceivableAccount()),
                eq(new BigDecimal("500000")), anyString(), anyString(), any());
    }

    @Test
    void refinanceLoan_generatesNewSchedule() {
        LoanAccount loan = TestDataBuilder.aLoanAccount(member, flatProduct, branch);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setOutstandingPrincipal(new BigDecimal("600000"));

        when(loanAccountRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(loan.getId()))
                .thenReturn(Collections.emptyList());

        loanAccountService.refinanceLoan(loan.getId(), 6, new BigDecimal("0.15"), "Rate reduction");

        assertEquals(6, loan.getTermMonths());
        assertEquals(new BigDecimal("0.15"), loan.getInterestRate());
        // 6 new schedule entries
        verify(scheduleRepository, times(6)).save(any(LoanRepaymentSchedule.class));
    }
}
