package tz.co.iseke.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTransactionSummaryDTO {
    private LocalDate date;
    private BigDecimal deposits;
    private BigDecimal withdrawals;
    private BigDecimal loanDisbursements;
    private BigDecimal loanRepayments;
    private Integer totalCount;
}
