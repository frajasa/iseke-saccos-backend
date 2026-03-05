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
import tz.co.iseke.repository.LoanAccountRepository;
import tz.co.iseke.repository.LoanRepaymentScheduleRepository;
import tz.co.iseke.service.payment.PaymentOrchestrationService;
import tz.co.iseke.service.payment.ReconciliationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EndOfDayService {

    private final LoanAccountRepository loanAccountRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final InterestAccrualService interestAccrualService;
    private final LoanInterestAccrualService loanInterestAccrualService;
    private final LoanProvisioningService loanProvisioningService;
    private final AccountingService accountingService;
    private final AuditService auditService;
    private final PaymentOrchestrationService paymentOrchestrationService;
    private final ReconciliationService reconciliationService;

    @Scheduled(cron = "0 0 23 * * ?") // Run at 11 PM daily
    public void scheduledEndOfDay() {
        runEndOfDay();
    }

    @Transactional
    public boolean runEndOfDay() {
        log.info("=== Starting End-of-Day Processing ===");
        LocalDate today = LocalDate.now();
        StringBuilder summary = new StringBuilder();

        try {
            // Step 1: Update days in arrears on all active loans
            int loansUpdated = updateDaysInArrears();
            summary.append("Loans arrears updated: ").append(loansUpdated).append("; ");

            // Step 2: Late fees are handled by LoanPenaltyService scheduled at 2 AM
            // Removed duplicate penalty calculation from EOD to prevent double charging

            // Step 3: Run daily savings interest accrual
            int savingsAccrued = interestAccrualService.runDailyAccrual();
            summary.append("Savings interest accrued: ").append(savingsAccrued).append("; ");

            // Step 4: Run daily loan interest accrual
            int loansAccrued = loanInterestAccrualService.runDailyLoanAccrual();
            summary.append("Loan interest accrued: ").append(loansAccrued).append("; ");

            // Step 5: Monthly interest posting (on last day of month)
            if (today.equals(today.withDayOfMonth(today.lengthOfMonth()))) {
                int interestPosted = interestAccrualService.postMonthlyInterest();
                summary.append("Monthly interest posted: ").append(interestPosted).append("; ");

                // Run loan provisioning monthly
                loanProvisioningService.runProvisioning(today, null);
                summary.append("Loan provisioning completed; ");
            }

            // Step 6: Expire stale payment requests
            try {
                paymentOrchestrationService.expireStaleRequests();
                summary.append("Stale payments expired; ");
            } catch (Exception e) {
                log.warn("Payment expiry step failed: {}", e.getMessage());
            }

            // Step 7: Run daily payment reconciliation for previous day
            try {
                reconciliationService.runDailyReconciliation();
                summary.append("Payment reconciliation completed; ");
            } catch (Exception e) {
                log.warn("Payment reconciliation step failed: {}", e.getMessage());
            }

            // Log summary to audit trail
            auditService.logAction("END_OF_DAY", "System", null,
                    null, summary.toString());

            // Step 8: GL balance verification
            try {
                var trialBalance = accountingService.getTrialBalance(today);
                BigDecimal difference = trialBalance.getTotalDebits().subtract(trialBalance.getTotalCredits()).abs();
                if (difference.compareTo(BigDecimal.valueOf(0.01)) > 0) {
                    log.warn("GL IMBALANCE DETECTED: Debits={}, Credits={}, Diff={}",
                            trialBalance.getTotalDebits(), trialBalance.getTotalCredits(), difference);
                    summary.append("WARNING: GL imbalance of ").append(difference).append("; ");
                } else {
                    summary.append("GL balanced; ");
                }
            } catch (Exception e) {
                log.warn("GL balance check failed: {}", e.getMessage());
            }

            log.info("=== End-of-Day Processing Completed: {} ===", summary);
            return true;

        } catch (Exception e) {
            log.error("End-of-Day processing failed: {}", e.getMessage(), e);
            auditService.logAction("END_OF_DAY_FAILED", "System", null,
                    null, "Error: " + e.getMessage());
            return false;
        }
    }

    private int updateDaysInArrears() {
        List<LoanAccount> activeLoans = loanAccountRepository.findAll().stream()
                .filter(l -> List.of(LoanStatus.DISBURSED, LoanStatus.ACTIVE).contains(l.getStatus()))
                .toList();

        int updated = 0;
        LocalDate today = LocalDate.now();

        for (LoanAccount loan : activeLoans) {
            try {
                // Find the earliest overdue schedule
                List<LoanRepaymentSchedule> overdueSchedules = scheduleRepository
                        .findByLoanIdOrderByInstallmentNumber(loan.getId()).stream()
                        .filter(s -> s.getStatus() == ScheduleStatus.PENDING || s.getStatus() == ScheduleStatus.PARTIAL)
                        .filter(s -> s.getDueDate().isBefore(today))
                        .toList();

                if (!overdueSchedules.isEmpty()) {
                    LocalDate earliestOverdue = overdueSchedules.get(0).getDueDate();
                    int daysInArrears = (int) ChronoUnit.DAYS.between(earliestOverdue, today);
                    loan.setDaysInArrears(Math.max(0, daysInArrears));

                    // Mark overdue schedules
                    for (LoanRepaymentSchedule schedule : overdueSchedules) {
                        if (schedule.getStatus() == ScheduleStatus.PENDING) {
                            schedule.setStatus(ScheduleStatus.OVERDUE);
                            scheduleRepository.save(schedule);
                        }
                    }
                } else {
                    loan.setDaysInArrears(0);
                }

                loan.setUpdatedAt(LocalDateTime.now());
                loanAccountRepository.save(loan);
                updated++;
            } catch (Exception e) {
                log.error("Error updating arrears for loan {}: {}", loan.getLoanNumber(), e.getMessage());
            }
        }

        return updated;
    }

    private int applyLateFees(LocalDate today) {
        List<LoanAccount> activeLoans = loanAccountRepository.findAll().stream()
                .filter(l -> List.of(LoanStatus.DISBURSED, LoanStatus.ACTIVE).contains(l.getStatus()))
                .filter(l -> l.getDaysInArrears() > 0)
                .toList();

        int applied = 0;

        for (LoanAccount loan : activeLoans) {
            try {
                BigDecimal penaltyRate = loan.getProduct().getLatePaymentPenaltyRate();
                if (penaltyRate == null || penaltyRate.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                int gracePeriod = loan.getProduct().getGracePeriodDays() != null
                        ? loan.getProduct().getGracePeriodDays() : 0;

                if (loan.getDaysInArrears() <= gracePeriod) {
                    continue;
                }

                // Calculate daily penalty on overdue amount
                BigDecimal overdueAmount = loan.getOutstandingPrincipal();
                BigDecimal dailyPenalty = overdueAmount.multiply(penaltyRate)
                        .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);

                if (dailyPenalty.compareTo(BigDecimal.ZERO) > 0) {
                    loan.setOutstandingPenalties(
                            loan.getOutstandingPenalties().add(dailyPenalty));
                    loan.setUpdatedAt(LocalDateTime.now());
                    loanAccountRepository.save(loan);

                    // Post GL for penalty: DEBIT Penalty Receivable (1204), CREDIT Penalty Income
                    ChartOfAccounts penaltyReceivable = null;
                    try {
                        penaltyReceivable = accountingService.findAccountByCode("1204");
                    } catch (Exception ignored) {}
                    if (penaltyReceivable != null && loan.getProduct().getPenaltyIncomeAccount() != null) {
                        accountingService.postToGeneralLedgerDirect(
                                today,
                                penaltyReceivable,
                                loan.getProduct().getPenaltyIncomeAccount(),
                                dailyPenalty,
                                "Late fee - " + loan.getLoanNumber(),
                                "PEN" + System.currentTimeMillis(),
                                loan.getBranch()
                        );
                    }

                    applied++;
                }
            } catch (Exception e) {
                log.error("Error applying late fee for loan {}: {}", loan.getLoanNumber(), e.getMessage());
            }
        }

        return applied;
    }
}
