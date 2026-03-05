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
public class IncomeStatementDTO {
    private BigDecimal revenue;
    private BigDecimal expenses;
    private BigDecimal netIncome;
    private List<IncomeStatementItemDTO> details;
}