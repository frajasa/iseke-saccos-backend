package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.entity.ChartOfAccounts;
import tz.co.iseke.entity.LoanAccount;
import tz.co.iseke.enums.LoanStatus;
import tz.co.iseke.repository.ChartOfAccountsRepository;
import tz.co.iseke.repository.LoanAccountRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanInterestAccrualService {

    private final LoanAccountRepository loanAccountRepository;
    private final AccountingService accountingService;
    private final ChartOfAccountsRepository chartRepository;

    @Scheduled(cron = "0 30 1 * * ?") // 1:30 AM daily
    @Transactional
    public int runDailyLoanAccrual() {
        log.info("Starting daily loan interest accrual");

        List<LoanAccount> activeLoans = loanAccountRepository.findAll().stream()
                .filter(l -> List.of(LoanStatus.DISBURSED, LoanStatus.ACTIVE).contains(l.getStatus()))
                .filter(l -> l.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        int accrued = 0;

        // Get Interest Receivable and Interest Income accounts
        ChartOfAccounts interestReceivable = chartRepository.findByAccountCode("1203").orElse(null);

        for (LoanAccount loan : activeLoans) {
            try {
                // Daily interest = outstandingPrincipal * (annualRate / 365)
                BigDecimal dailyRate = loan.getInterestRate()
                        .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
                BigDecimal dailyInterest = loan.getOutstandingPrincipal()
                        .multiply(dailyRate)
                        .setScale(2, RoundingMode.HALF_UP);

                if (dailyInterest.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                // Accrue to outstanding interest
                loan.setOutstandingInterest(
                        loan.getOutstandingInterest().add(dailyInterest));
                loan.setUpdatedAt(LocalDateTime.now());
                loanAccountRepository.save(loan);

                // Post GL: DEBIT Interest Receivable, CREDIT Interest Income
                ChartOfAccounts interestIncome = loan.getProduct().getInterestIncomeAccount();
                if (interestReceivable != null && interestIncome != null) {
                    accountingService.postToGeneralLedgerDirect(
                            LocalDate.now(),
                            interestReceivable,
                            interestIncome,
                            dailyInterest,
                            "Daily loan interest accrual - " + loan.getLoanNumber(),
                            "LACR" + System.currentTimeMillis(),
                            loan.getBranch()
                    );
                }

                accrued++;
            } catch (Exception e) {
                log.error("Error accruing loan interest for loan {}: {}",
                        loan.getLoanNumber(), e.getMessage());
            }
        }

        log.info("Daily loan interest accrual completed. {} loans processed", accrued);
        return accrued;
    }
}
