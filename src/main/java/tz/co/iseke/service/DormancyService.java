package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.entity.SavingsAccount;
import tz.co.iseke.enums.AccountStatus;
import tz.co.iseke.repository.SavingsAccountRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DormancyService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final AuditService auditService;

    @Scheduled(cron = "0 0 7 * * ?") // 7 AM daily
    @Transactional
    public int enforceDormancy() {
        log.info("Starting dormancy enforcement check");
        LocalDate today = LocalDate.now();
        int dormantCount = 0;

        List<SavingsAccount> activeAccounts = savingsAccountRepository.findByStatus(AccountStatus.ACTIVE);

        for (SavingsAccount account : activeAccounts) {
            try {
                int dormancyDays = account.getProduct().getDormancyPeriodDays() != null
                        ? account.getProduct().getDormancyPeriodDays() : 365;

                LocalDate lastActivity = account.getLastActivityDate() != null
                        ? account.getLastActivityDate()
                        : (account.getLastTransactionDate() != null
                                ? account.getLastTransactionDate()
                                : account.getOpeningDate());

                if (lastActivity.plusDays(dormancyDays).isBefore(today)) {
                    account.setStatus(AccountStatus.DORMANT);
                    account.setUpdatedAt(LocalDateTime.now());
                    savingsAccountRepository.save(account);

                    auditService.logAction("ACCOUNT_DORMANT", "SavingsAccount", account.getId(),
                            "Status: ACTIVE", "Status: DORMANT, Last activity: " + lastActivity);
                    dormantCount++;
                }
            } catch (Exception e) {
                log.error("Error checking dormancy for account {}: {}", account.getAccountNumber(), e.getMessage());
            }
        }

        log.info("Dormancy enforcement completed. {} accounts marked dormant", dormantCount);
        return dormantCount;
    }
}
