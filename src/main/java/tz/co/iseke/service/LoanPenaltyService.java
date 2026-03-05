package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.entity.ChartOfAccounts;
import tz.co.iseke.entity.LoanAccount;
import tz.co.iseke.entity.LoanRepaymentSchedule;
import tz.co.iseke.enums.LoanStatus;
import tz.co.iseke.enums.ScheduleStatus;
import tz.co.iseke.repository.ChartOfAccountsRepository;
import tz.co.iseke.repository.LoanAccountRepository;
import tz.co.iseke.repository.LoanRepaymentScheduleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanPenaltyService {

    private final LoanAccountRepository loanAccountRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final AccountingService accountingService;
    private final ChartOfAccountsRepository chartRepository;

    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    @Transactional
    public int calculateDailyPenalties() {
        log.info("Starting daily loan penalty calculation");
        LocalDate today = LocalDate.now();
        int penalized = 0;

        List<LoanAccount> activeLoans = loanAccountRepository.findAll().stream()
                .filter(l -> List.of(LoanStatus.DISBURSED, LoanStatus.ACTIVE).contains(l.getStatus()))
                .toList();

        ChartOfAccounts penaltyReceivable = chartRepository.findByAccountCode("1204").orElse(null);

        for (LoanAccount loan : activeLoans) {
            try {
                List<LoanRepaymentSchedule> overdueSchedules = scheduleRepository
                        .findByLoanIdOrderByInstallmentNumber(loan.getId()).stream()
                        .filter(s -> s.getStatus() == ScheduleStatus.PENDING || s.getStatus() == ScheduleStatus.PARTIAL)
                        .filter(s -> s.getDueDate().isBefore(today))
                        .toList();

                if (overdueSchedules.isEmpty()) {
                    loan.setDaysInArrears(0);
                    loanAccountRepository.save(loan);
                    continue;
                }

                // Update days in arrears
                LocalDate earliestOverdue = overdueSchedules.get(0).getDueDate();
                int daysOverdue = (int) ChronoUnit.DAYS.between(earliestOverdue, today);
                loan.setDaysInArrears(Math.max(0, daysOverdue));

                // Mark overdue schedules
                for (LoanRepaymentSchedule schedule : overdueSchedules) {
                    if (schedule.getStatus() == ScheduleStatus.PENDING) {
                        schedule.setStatus(ScheduleStatus.OVERDUE);
                        scheduleRepository.save(schedule);
                    }
                }

                // Calculate penalty if past grace period
                BigDecimal penaltyRate = loan.getProduct().getLatePaymentPenaltyRate();
                int gracePeriod = loan.getProduct().getGracePeriodDays() != null
                        ? loan.getProduct().getGracePeriodDays() : 0;

                if (penaltyRate != null && penaltyRate.compareTo(BigDecimal.ZERO) > 0
                        && daysOverdue > gracePeriod) {
                    BigDecimal dailyPenalty = loan.getOutstandingPrincipal()
                            .multiply(penaltyRate)
                            .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);

                    if (dailyPenalty.compareTo(BigDecimal.ZERO) > 0) {
                        loan.setOutstandingPenalties(loan.getOutstandingPenalties().add(dailyPenalty));

                        // Post GL: DEBIT Penalty Receivable, CREDIT Penalty Income
                        ChartOfAccounts penaltyIncome = loan.getProduct().getPenaltyIncomeAccount();
                        if (penaltyReceivable != null && penaltyIncome != null) {
                            accountingService.postToGeneralLedgerDirect(
                                    today, penaltyReceivable, penaltyIncome, dailyPenalty,
                                    "Daily penalty - " + loan.getLoanNumber(),
                                    "PEN" + System.currentTimeMillis(),
                                    loan.getBranch()
                            );
                        }
                    }
                }

                loan.setUpdatedAt(LocalDateTime.now());
                loanAccountRepository.save(loan);
                penalized++;
            } catch (Exception e) {
                log.error("Error calculating penalty for loan {}: {}", loan.getLoanNumber(), e.getMessage());
            }
        }

        log.info("Daily penalty calculation completed. {} loans processed", penalized);
        return penalized;
    }
}
