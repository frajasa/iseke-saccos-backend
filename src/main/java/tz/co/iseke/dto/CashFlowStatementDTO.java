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
public class CashFlowStatementDTO {
    private LocalDate date;
    private CashFlowSectionDTO operatingActivities;
    private CashFlowSectionDTO investingActivities;
    private CashFlowSectionDTO financingActivities;
    private BigDecimal netCashFlow;
}
