package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.dto.DashboardStatsDTO;
import tz.co.iseke.entity.Transaction;
import tz.co.iseke.enums.LoanStatus;
import tz.co.iseke.enums.MemberStatus;
import tz.co.iseke.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final TransactionRepository transactionRepository;

    public DashboardStatsDTO getDashboardStats() {
        // Total active members
        long totalMembers = memberRepository.count();

        // Total savings balance
        BigDecimal totalSavings = savingsAccountRepository.getTotalSavingsBalance();

        // Active loans count (DISBURSED + ACTIVE)
        long activeDisbursed = loanAccountRepository.countByStatus(LoanStatus.DISBURSED);
        long activeStatus = loanAccountRepository.countByStatus(LoanStatus.ACTIVE);
        long activeLoans = activeDisbursed + activeStatus;

        // Loan portfolio (total outstanding principal)
        BigDecimal loanPortfolio = loanAccountRepository.getTotalOutstandingPrincipal();

        // Overdue loans (days in arrears > 30)
        List<?> delinquentLoans = loanAccountRepository.findLoansByDaysInArrears(30);
        long overdueLoans = delinquentLoans.size();

        // Pending loan applications
        long pendingApplications = loanAccountRepository.countByStatus(LoanStatus.APPLIED);

        // Recent transactions (last 10)
        List<Transaction> recentTxns = transactionRepository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "transactionDate", "transactionTime"))
        ).getContent();

        List<DashboardStatsDTO.RecentTransactionDTO> recentTransactions = recentTxns.stream()
                .map(tx -> DashboardStatsDTO.RecentTransactionDTO.builder()
                        .id(tx.getId().toString())
                        .memberName(tx.getMember() != null ? tx.getMember().getFullName() : "System")
                        .transactionType(tx.getTransactionType().name())
                        .amount(tx.getAmount())
                        .date(tx.getTransactionDate().toString())
                        .status(tx.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        return DashboardStatsDTO.builder()
                .totalMembers(totalMembers)
                .totalSavings(totalSavings)
                .activeLoans(activeLoans)
                .loanPortfolio(loanPortfolio)
                .overdueLoans(overdueLoans)
                .pendingApplications(pendingApplications)
                .recentTransactions(recentTransactions)
                .build();
    }
}
