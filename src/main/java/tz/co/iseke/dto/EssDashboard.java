package tz.co.iseke.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.co.iseke.entity.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EssDashboard {
    private String memberName;
    private String memberNumber;
    private String employerName;
    private BigDecimal totalSavings;
    private BigDecimal totalLoanOutstanding;
    private int activeLoans;
    private int activeSavingsAccounts;
    private BigDecimal monthlyDeductions;
    private List<EssServiceRequest> recentRequests;
}
