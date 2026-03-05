package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.entity.*;
import tz.co.iseke.enums.AccountStatus;
import tz.co.iseke.enums.LoanStatus;
import tz.co.iseke.enums.ScheduleStatus;
import tz.co.iseke.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreditScoringService {

    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final CreditScoreRepository creditScoreRepository;

    @Transactional
    public CreditScore calculateCreditScore(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        List<String> factors = new ArrayList<>();
        int totalScore = 0;

        // 1. Membership duration (max 15 points)
        long membershipMonths = ChronoUnit.MONTHS.between(member.getMembershipDate(), LocalDate.now());
        int durationScore = (int) Math.min(15, membershipMonths / 6 * 3);
        totalScore += durationScore;
        factors.add("Membership duration: " + membershipMonths + " months (" + durationScore + " pts)");

        // 2. Savings history (max 25 points)
        List<SavingsAccount> savingsAccounts = savingsAccountRepository.findByMemberId(memberId);
        BigDecimal totalSavings = savingsAccounts.stream()
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                .map(SavingsAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int savingsScore = 0;
        if (totalSavings.compareTo(BigDecimal.valueOf(5000000)) >= 0) savingsScore = 25;
        else if (totalSavings.compareTo(BigDecimal.valueOf(2000000)) >= 0) savingsScore = 20;
        else if (totalSavings.compareTo(BigDecimal.valueOf(1000000)) >= 0) savingsScore = 15;
        else if (totalSavings.compareTo(BigDecimal.valueOf(500000)) >= 0) savingsScore = 10;
        else if (totalSavings.compareTo(BigDecimal.valueOf(100000)) >= 0) savingsScore = 5;
        totalScore += savingsScore;
        factors.add("Savings balance: TZS " + totalSavings + " (" + savingsScore + " pts)");

        // 3. Loan repayment history (max 30 points)
        List<LoanAccount> loans = loanAccountRepository.findByMemberId(memberId);
        int repaymentScore = 0;

        long closedLoans = loans.stream().filter(l -> l.getStatus() == LoanStatus.CLOSED).count();
        long writtenOff = loans.stream().filter(l -> l.getStatus() == LoanStatus.WRITTEN_OFF).count();

        if (writtenOff > 0) {
            repaymentScore = 0;
            factors.add("Written-off loans detected: -30 pts");
        } else if (closedLoans > 0) {
            // Check on-time payment ratio
            int totalSchedules = 0;
            int paidOnTime = 0;
            for (LoanAccount loan : loans) {
                List<LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumber(loan.getId());
                for (LoanRepaymentSchedule s : schedules) {
                    if (s.getStatus() == ScheduleStatus.PAID) {
                        totalSchedules++;
                        if (s.getPaymentDate() != null && !s.getPaymentDate().isAfter(s.getDueDate())) {
                            paidOnTime++;
                        }
                    }
                }
            }
            if (totalSchedules > 0) {
                double onTimeRatio = (double) paidOnTime / totalSchedules;
                repaymentScore = (int) (onTimeRatio * 30);
            } else {
                repaymentScore = 15; // No history but no defaults
            }
            factors.add("Repayment history: " + closedLoans + " closed loans (" + repaymentScore + " pts)");
        } else if (loans.isEmpty()) {
            repaymentScore = 10; // No loan history
            factors.add("No loan history: neutral (" + repaymentScore + " pts)");
        }
        totalScore += repaymentScore;

        // 4. Active loan behavior (max 15 points)
        int activeLoanScore = 15;
        for (LoanAccount loan : loans) {
            if (List.of(LoanStatus.DISBURSED, LoanStatus.ACTIVE).contains(loan.getStatus())) {
                if (loan.getDaysInArrears() > 90) activeLoanScore = 0;
                else if (loan.getDaysInArrears() > 30) activeLoanScore = 5;
                else if (loan.getDaysInArrears() > 0) activeLoanScore = 10;
            }
        }
        totalScore += activeLoanScore;
        factors.add("Active loan behavior: (" + activeLoanScore + " pts)");

        // 5. Income-to-debt ratio (max 15 points)
        int debtRatioScore = 15;
        if (member.getMonthlyIncome() != null && member.getMonthlyIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalDebt = loans.stream()
                    .filter(l -> List.of(LoanStatus.DISBURSED, LoanStatus.ACTIVE).contains(l.getStatus()))
                    .map(LoanAccount::getOutstandingPrincipal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal annualIncome = member.getMonthlyIncome().multiply(BigDecimal.valueOf(12));
            if (annualIncome.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ratio = totalDebt.divide(annualIncome, 2, RoundingMode.HALF_UP);
                if (ratio.compareTo(BigDecimal.valueOf(2)) > 0) debtRatioScore = 0;
                else if (ratio.compareTo(BigDecimal.ONE) > 0) debtRatioScore = 5;
                else if (ratio.compareTo(BigDecimal.valueOf(0.5)) > 0) debtRatioScore = 10;
                factors.add("Debt-to-income ratio: " + ratio + " (" + debtRatioScore + " pts)");
            }
        }
        totalScore += debtRatioScore;

        // Determine rating
        String rating;
        if (totalScore >= 80) rating = "A";
        else if (totalScore >= 65) rating = "B";
        else if (totalScore >= 50) rating = "C";
        else if (totalScore >= 35) rating = "D";
        else rating = "E";

        CreditScore creditScore = CreditScore.builder()
                .member(member)
                .score(totalScore)
                .rating(rating)
                .factors(String.join("; ", factors))
                .calculatedAt(LocalDateTime.now())
                .build();

        creditScore = creditScoreRepository.save(creditScore);
        log.info("Credit score calculated for member {}: {} ({})", member.getMemberNumber(), totalScore, rating);
        return creditScore;
    }

    public CreditScore getLatestScore(UUID memberId) {
        return creditScoreRepository.findTopByMemberIdOrderByCalculatedAtDesc(memberId).orElse(null);
    }
}
