package tz.co.iseke.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialStatementsDTO {
    private LocalDate date;
    private BalanceSheetDTO balanceSheet;
    private IncomeStatementDTO incomeStatement;
}