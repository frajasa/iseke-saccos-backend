package tz.co.iseke.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.co.iseke.entity.*;
import tz.co.iseke.enums.*;
import tz.co.iseke.repository.*;
import tz.co.iseke.testutil.TestDataBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditScoringServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private LoanAccountRepository loanAccountRepository;
    @Mock private LoanRepaymentScheduleRepository scheduleRepository;
    @Mock private CreditScoreRepository creditScoreRepository;

    @InjectMocks
    private CreditScoringService creditScoringService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = TestDataBuilder.aMember();

        lenient().when(creditScoreRepository.save(any(CreditScore.class)))
                .thenAnswer(inv -> {
                    CreditScore cs = inv.getArgument(0);
                    if (cs.getId() == null) cs.setId(UUID.randomUUID());
                    return cs;
                });
    }

    @Test
    void newMember_baselineScore() {
        // Brand new member - 0 months membership, no savings, no loans
        member.setMembershipDate(LocalDate.now());
        member.setMonthlyIncome(null);

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(savingsAccountRepository.findByMemberId(member.getId())).thenReturn(Collections.emptyList());
        when(loanAccountRepository.findByMemberId(member.getId())).thenReturn(Collections.emptyList());

        CreditScore score = creditScoringService.calculateCreditScore(member.getId());

        // Duration: 0, Savings: 0, Repayment: 10 (no history), Active: 15, Debt: 15 => 40
        assertEquals(40, score.getScore());
        assertEquals("D", score.getRating());
    }

    @Test
    void longTermMemberWithHighSavings_highScore() {
        member.setMembershipDate(LocalDate.now().minusYears(3)); // 36 months -> 15 pts (capped)
        member.setMonthlyIncome(new BigDecimal("2000000"));

        SavingsAccount savingsAccount = SavingsAccount.builder()
                .id(UUID.randomUUID())
                .balance(new BigDecimal("6000000")) // > 5M -> 25 pts
                .status(AccountStatus.ACTIVE)
                .build();

        LoanAccount closedLoan = LoanAccount.builder()
                .id(UUID.randomUUID())
                .status(LoanStatus.CLOSED)
                .build();

        LoanRepaymentSchedule paidOnTime = LoanRepaymentSchedule.builder()
                .status(ScheduleStatus.PAID)
                .dueDate(LocalDate.now().minusMonths(1))
                .paymentDate(LocalDate.now().minusMonths(1))
                .build();

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(savingsAccountRepository.findByMemberId(member.getId())).thenReturn(List.of(savingsAccount));
        when(loanAccountRepository.findByMemberId(member.getId())).thenReturn(List.of(closedLoan));
        when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(closedLoan.getId()))
                .thenReturn(List.of(paidOnTime));

        CreditScore score = creditScoringService.calculateCreditScore(member.getId());

        // Duration: 15, Savings: 25, Repayment: 30, Active: 15, Debt: 15 => 100
        assertEquals(100, score.getScore());
        assertEquals("A", score.getRating());
    }

    @Test
    void writtenOffLoan_zeroesRepaymentFactor() {
        member.setMembershipDate(LocalDate.now().minusYears(1));
        member.setMonthlyIncome(null);

        LoanAccount writtenOff = LoanAccount.builder()
                .id(UUID.randomUUID())
                .status(LoanStatus.WRITTEN_OFF)
                .build();

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(savingsAccountRepository.findByMemberId(member.getId())).thenReturn(Collections.emptyList());
        when(loanAccountRepository.findByMemberId(member.getId())).thenReturn(List.of(writtenOff));

        CreditScore score = creditScoringService.calculateCreditScore(member.getId());

        // Repayment factor should be 0 due to write-off
        // Duration: 6, Savings: 0, Repayment: 0, Active: 15, Debt: 15 => 36
        assertEquals(36, score.getScore());
    }

    @Test
    void perfectRepayment_maxRepaymentScore() {
        member.setMembershipDate(LocalDate.now().minusMonths(6));
        member.setMonthlyIncome(null);

        LoanAccount closedLoan = LoanAccount.builder()
                .id(UUID.randomUUID())
                .status(LoanStatus.CLOSED)
                .build();

        // All 6 payments on time
        List<LoanRepaymentSchedule> schedules = List.of(
                buildPaidSchedule(LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(6)),
                buildPaidSchedule(LocalDate.now().minusMonths(5), LocalDate.now().minusMonths(5)),
                buildPaidSchedule(LocalDate.now().minusMonths(4), LocalDate.now().minusMonths(4)),
                buildPaidSchedule(LocalDate.now().minusMonths(3), LocalDate.now().minusMonths(3)),
                buildPaidSchedule(LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(2)),
                buildPaidSchedule(LocalDate.now().minusMonths(1), LocalDate.now().minusMonths(1))
        );

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(savingsAccountRepository.findByMemberId(member.getId())).thenReturn(Collections.emptyList());
        when(loanAccountRepository.findByMemberId(member.getId())).thenReturn(List.of(closedLoan));
        when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(closedLoan.getId())).thenReturn(schedules);

        CreditScore score = creditScoringService.calculateCreditScore(member.getId());

        // Duration: 3, Savings: 0, Repayment: 30 (100%), Active: 15, Debt: 15 => 63
        assertEquals(63, score.getScore());
    }

    @Test
    void latePayments_reducedScore() {
        member.setMembershipDate(LocalDate.now().minusMonths(6));
        member.setMonthlyIncome(null);

        LoanAccount closedLoan = LoanAccount.builder()
                .id(UUID.randomUUID())
                .status(LoanStatus.CLOSED)
                .build();

        // 2 of 4 payments late
        List<LoanRepaymentSchedule> schedules = List.of(
                buildPaidSchedule(LocalDate.now().minusMonths(4), LocalDate.now().minusMonths(4)),
                buildPaidSchedule(LocalDate.now().minusMonths(3), LocalDate.now().minusMonths(2)), // late
                buildPaidSchedule(LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(2)),
                buildPaidSchedule(LocalDate.now().minusMonths(1), LocalDate.now()) // late
        );

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(savingsAccountRepository.findByMemberId(member.getId())).thenReturn(Collections.emptyList());
        when(loanAccountRepository.findByMemberId(member.getId())).thenReturn(List.of(closedLoan));
        when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(closedLoan.getId())).thenReturn(schedules);

        CreditScore score = creditScoringService.calculateCreditScore(member.getId());

        // 50% on-time ratio -> repayment = 15
        // Duration: 3, Savings: 0, Repayment: 15, Active: 15, Debt: 15 => 48
        assertEquals(48, score.getScore());
    }

    @Test
    void ninetydaysArrears_zeroesActiveLoanFactor() {
        member.setMembershipDate(LocalDate.now().minusMonths(6));
        member.setMonthlyIncome(null);

        LoanAccount activeLoan = LoanAccount.builder()
                .id(UUID.randomUUID())
                .status(LoanStatus.ACTIVE)
                .daysInArrears(100)
                .outstandingPrincipal(new BigDecimal("500000"))
                .build();

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(savingsAccountRepository.findByMemberId(member.getId())).thenReturn(Collections.emptyList());
        when(loanAccountRepository.findByMemberId(member.getId())).thenReturn(List.of(activeLoan));

        CreditScore score = creditScoringService.calculateCreditScore(member.getId());

        // Active loan score = 0 due to >90 days arrears
        // Duration: 3, Savings: 0, Repayment: 0 (has loans but none closed/written-off), Active: 0, Debt: 15 => 18
        assertEquals(18, score.getScore());
        assertEquals("E", score.getRating());
    }

    @Test
    void highDebtToIncomeRatio_reducedScore() {
        member.setMembershipDate(LocalDate.now().minusMonths(6));
        member.setMonthlyIncome(new BigDecimal("500000")); // 500K/month -> 6M/year

        LoanAccount activeLoan = LoanAccount.builder()
                .id(UUID.randomUUID())
                .status(LoanStatus.ACTIVE)
                .daysInArrears(0)
                .outstandingPrincipal(new BigDecimal("15000000")) // 15M outstanding
                .build();

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(savingsAccountRepository.findByMemberId(member.getId())).thenReturn(Collections.emptyList());
        when(loanAccountRepository.findByMemberId(member.getId())).thenReturn(List.of(activeLoan));

        CreditScore score = creditScoringService.calculateCreditScore(member.getId());

        // Debt ratio: 15M / 6M = 2.5 -> >2.0 -> debtRatioScore = 0
        // Duration: 3, Savings: 0, Repayment: 0 (has loans but none closed/written-off), Active: 15, Debt: 0 => 18
        assertEquals(18, score.getScore());
    }

    @Test
    void ratingBands_ABCDE() {
        // Just verify the rating thresholds logic
        // A >= 80, B >= 65, C >= 50, D >= 35, E < 35
        member.setMembershipDate(LocalDate.now().minusYears(3));
        member.setMonthlyIncome(new BigDecimal("2000000"));

        // Build a scenario for B rating (65-79)
        SavingsAccount savingsAccount = SavingsAccount.builder()
                .id(UUID.randomUUID())
                .balance(new BigDecimal("2500000")) // 20 pts
                .status(AccountStatus.ACTIVE)
                .build();

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(savingsAccountRepository.findByMemberId(member.getId())).thenReturn(List.of(savingsAccount));
        when(loanAccountRepository.findByMemberId(member.getId())).thenReturn(Collections.emptyList());

        CreditScore score = creditScoringService.calculateCreditScore(member.getId());

        // Duration: 15, Savings: 20, Repayment: 10 (no history), Active: 15, Debt: 15 => 75
        assertEquals(75, score.getScore());
        assertEquals("B", score.getRating());
    }

    private LoanRepaymentSchedule buildPaidSchedule(LocalDate dueDate, LocalDate paymentDate) {
        return LoanRepaymentSchedule.builder()
                .status(ScheduleStatus.PAID)
                .dueDate(dueDate)
                .paymentDate(paymentDate)
                .build();
    }
}
