package tz.co.iseke.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDashboard {
    private int totalPaymentsToday;
    private int completedPayments;
    private int failedPayments;
    private int pendingPayments;
    private BigDecimal totalAmountToday;
    private BigDecimal totalCollections;
    private BigDecimal totalDisbursements;
    private int mpesaCount;
    private int tigopesaCount;
    private int nmbCount;
}
