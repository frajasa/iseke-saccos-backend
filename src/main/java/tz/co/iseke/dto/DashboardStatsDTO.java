package tz.co.iseke.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalMembers;
    private BigDecimal totalSavings;
    private long activeLoans;
    private BigDecimal loanPortfolio;
    private long overdueLoans;
    private long pendingApplications;
    private List<RecentTransactionDTO> recentTransactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentTransactionDTO {
        private String id;
        private String memberName;
        private String transactionType;
        private BigDecimal amount;
        private String date;
        private String status;
    }
}
