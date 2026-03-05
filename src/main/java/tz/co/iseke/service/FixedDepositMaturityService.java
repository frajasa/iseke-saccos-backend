package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.entity.SavingsAccount;
import tz.co.iseke.entity.SavingsProduct;
import tz.co.iseke.enums.AccountStatus;
import tz.co.iseke.enums.SavingsProductType;
import tz.co.iseke.repository.SavingsAccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FixedDepositMaturityService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final InterestAccrualService interestAccrualService;
    private final AuditService auditService;

    @Scheduled(cron = "0 0 6 * * ?") // 6 AM daily
    @Transactional
    public int processMaturedDeposits() {
        log.info("Starting fixed deposit maturity check");
        LocalDate today = LocalDate.now();
        int processed = 0;

        List<SavingsAccount> maturedDeposits = savingsAccountRepository.findByStatus(AccountStatus.ACTIVE).stream()
                .filter(a -> a.getProduct().getProductType() == SavingsProductType.FIXED_DEPOSIT)
                .filter(a -> a.getMaturityDate() != null && !a.getMaturityDate().isAfter(today))
                .toList();

        for (SavingsAccount account : maturedDeposits) {
            try {
                SavingsProduct product = account.getProduct();

                // Post any remaining accrued interest
                if (account.getAccruedInterest() != null && account.getAccruedInterest().compareTo(BigDecimal.ZERO) > 0) {
                    account.setBalance(account.getBalance().add(account.getAccruedInterest()));
                    account.setAvailableBalance(account.getAvailableBalance().add(account.getAccruedInterest()));
                    account.setAccruedInterest(BigDecimal.ZERO);
                }

                if (Boolean.TRUE.equals(product.getAutoRollover())) {
                    // Auto-rollover: extend maturity date by same term
                    long termDays = java.time.temporal.ChronoUnit.DAYS.between(account.getOpeningDate(), account.getMaturityDate());
                    account.setMaturityDate(today.plusDays(termDays));
                    account.setUpdatedAt(LocalDateTime.now());
                    savingsAccountRepository.save(account);

                    auditService.logAction("FD_AUTO_ROLLOVER", "SavingsAccount", account.getId(),
                            null, "Rolled over for " + termDays + " days, Balance: " + account.getBalance());
                } else {
                    // Mark as matured - set to inactive so member must collect
                    account.setStatus(AccountStatus.INACTIVE);
                    account.setUpdatedAt(LocalDateTime.now());
                    savingsAccountRepository.save(account);

                    auditService.logAction("FD_MATURED", "SavingsAccount", account.getId(),
                            null, "Balance: " + account.getBalance());
                }

                processed++;
            } catch (Exception e) {
                log.error("Error processing matured FD {}: {}", account.getAccountNumber(), e.getMessage());
            }
        }

        log.info("Fixed deposit maturity check completed. {} deposits processed", processed);
        return processed;
    }
}
