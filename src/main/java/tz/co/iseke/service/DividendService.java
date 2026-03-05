package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.entity.*;
import tz.co.iseke.enums.AccountStatus;
import tz.co.iseke.enums.SavingsProductType;
import tz.co.iseke.repository.DividendRunRepository;
import tz.co.iseke.repository.SavingsAccountRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DividendService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final DividendRunRepository dividendRunRepository;
    private final AccountingService accountingService;
    private final AuditService auditService;

    @Transactional
    public DividendRun calculateDividends(Integer year, String method, BigDecimal rate) {
        // Find all share accounts
        List<SavingsAccount> shareAccounts = savingsAccountRepository.findByStatus(AccountStatus.ACTIVE).stream()
                .filter(a -> a.getProduct().getProductType() == SavingsProductType.SHARES)
                .filter(a -> a.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        BigDecimal totalPayout = BigDecimal.ZERO;
        int membersPaid = 0;

        for (SavingsAccount account : shareAccounts) {
            BigDecimal dividend = account.getBalance().multiply(rate).setScale(2, RoundingMode.HALF_UP);
            totalPayout = totalPayout.add(dividend);
            membersPaid++;
        }

        DividendRun run = DividendRun.builder()
                .year(year)
                .method(method)
                .rate(rate)
                .totalAmount(totalPayout)
                .membersPaid(membersPaid)
                .status("CALCULATED")
                .build();

        run = dividendRunRepository.save(run);

        auditService.logAction("DIVIDENDS_CALCULATED", "DividendRun", run.getId(),
                null, "Year: " + year + ", Total: " + totalPayout + ", Members: " + membersPaid);

        log.info("Dividends calculated for year {}: TZS {} for {} members", year, totalPayout, membersPaid);
        return run;
    }

    @Transactional
    public DividendRun postDividends(UUID dividendRunId) {
        DividendRun run = dividendRunRepository.findById(dividendRunId)
                .orElseThrow(() -> new RuntimeException("Dividend run not found"));

        if (!"CALCULATED".equals(run.getStatus())) {
            throw new RuntimeException("Dividend run must be in CALCULATED status to post");
        }

        List<SavingsAccount> shareAccounts = savingsAccountRepository.findByStatus(AccountStatus.ACTIVE).stream()
                .filter(a -> a.getProduct().getProductType() == SavingsProductType.SHARES)
                .filter(a -> a.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        BigDecimal totalPosted = BigDecimal.ZERO;

        for (SavingsAccount account : shareAccounts) {
            BigDecimal dividend = account.getBalance().multiply(run.getRate()).setScale(2, RoundingMode.HALF_UP);

            if (dividend.compareTo(BigDecimal.ZERO) > 0) {
                account.setBalance(account.getBalance().add(dividend));
                account.setAvailableBalance(account.getAvailableBalance().add(dividend));
                account.setUpdatedAt(LocalDateTime.now());
                savingsAccountRepository.save(account);
                totalPosted = totalPosted.add(dividend);
            }
        }

        String currentUser = null;
        try {
            currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception ignored) {}

        run.setStatus("POSTED");
        run.setProcessedBy(currentUser);
        run.setProcessedAt(LocalDateTime.now());
        run.setTotalAmount(totalPosted);
        run = dividendRunRepository.save(run);

        auditService.logAction("DIVIDENDS_POSTED", "DividendRun", run.getId(),
                "Status: CALCULATED", "Status: POSTED, Total: " + totalPosted);

        return run;
    }

    public List<DividendRun> findAll() {
        return dividendRunRepository.findAll();
    }
}
