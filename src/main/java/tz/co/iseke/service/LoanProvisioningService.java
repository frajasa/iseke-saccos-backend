package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.dto.LoanClassificationDTO;
import tz.co.iseke.dto.LoanProvisionReportDTO;
import tz.co.iseke.entity.ChartOfAccounts;
import tz.co.iseke.entity.LoanAccount;
import tz.co.iseke.enums.LoanStatus;
import tz.co.iseke.repository.ChartOfAccountsRepository;
import tz.co.iseke.repository.LoanAccountRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanProvisioningService {

    private final LoanAccountRepository loanAccountRepository;
    private final AccountingService accountingService;
    private final ChartOfAccountsRepository chartRepository;
    private final AuditService auditService;

    // Tanzania SACCOS provisioning rates
    private static final Map<String, BigDecimal> PROVISION_RATES = new LinkedHashMap<>();
    static {
        PROVISION_RATES.put("Current", BigDecimal.ZERO);
        PROVISION_RATES.put("Watch", new BigDecimal("0.05"));
        PROVISION_RATES.put("Substandard", new BigDecimal("0.25"));
        PROVISION_RATES.put("Doubtful", new BigDecimal("0.50"));
        PROVISION_RATES.put("Loss", BigDecimal.ONE);
    }

    public String classifyLoan(int daysInArrears) {
        if (daysInArrears <= 0) return "Current";
        if (daysInArrears <= 30) return "Watch";
        if (daysInArrears <= 90) return "Substandard";
        if (daysInArrears <= 180) return "Doubtful";
        return "Loss";
    }

    @Transactional
    public LoanProvisionReportDTO runProvisioning(LocalDate date, UUID branchId) {
        log.info("Starting loan provisioning calculation");

        List<LoanAccount> activeLoans = loanAccountRepository.findAll().stream()
                .filter(l -> List.of(LoanStatus.DISBURSED, LoanStatus.ACTIVE).contains(l.getStatus()))
                .filter(l -> branchId == null || (l.getBranch() != null && l.getBranch().getId().equals(branchId)))
                .toList();

        Map<String, List<LoanAccount>> classifiedLoans = activeLoans.stream()
                .collect(Collectors.groupingBy(l -> classifyLoan(l.getDaysInArrears())));

        List<LoanClassificationDTO> classifications = new ArrayList<>();
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal totalProvision = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : PROVISION_RATES.entrySet()) {
            String classification = entry.getKey();
            BigDecimal rate = entry.getValue();
            List<LoanAccount> loans = classifiedLoans.getOrDefault(classification, List.of());

            BigDecimal outstanding = loans.stream()
                    .map(LoanAccount::getOutstandingPrincipal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal provision = outstanding.multiply(rate).setScale(2, RoundingMode.HALF_UP);

            totalOutstanding = totalOutstanding.add(outstanding);
            totalProvision = totalProvision.add(provision);

            classifications.add(LoanClassificationDTO.builder()
                    .classification(classification)
                    .count(loans.size())
                    .outstandingAmount(outstanding)
                    .provisionRate(rate)
                    .provisionAmount(provision)
                    .build());
        }

        // Post GL entries for provision
        if (totalProvision.compareTo(BigDecimal.ZERO) > 0) {
            ChartOfAccounts provisionExpense = chartRepository.findByAccountCode("5301").orElse(null);
            // 1205 = Loan Loss Provision (contra-asset), not 1204 which is Penalty Receivable
            ChartOfAccounts loanLossProvision = chartRepository.findByAccountCode("1205").orElse(null);

            if (provisionExpense != null && loanLossProvision != null) {
                accountingService.postToGeneralLedgerDirect(
                        date != null ? date : LocalDate.now(),
                        provisionExpense,
                        loanLossProvision,
                        totalProvision,
                        "Loan loss provisioning",
                        "PROV" + System.currentTimeMillis(),
                        null
                );
            }
        }

        auditService.logAction("LOAN_PROVISIONING", "System", null,
                null, "Total provision: " + totalProvision);

        log.info("Loan provisioning completed. Total provision: {}", totalProvision);

        return LoanProvisionReportDTO.builder()
                .date(date != null ? date : LocalDate.now())
                .classifications(classifications)
                .totalOutstanding(totalOutstanding)
                .totalProvision(totalProvision)
                .build();
    }
}
