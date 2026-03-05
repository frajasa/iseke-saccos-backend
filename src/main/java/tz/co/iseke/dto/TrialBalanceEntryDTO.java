package tz.co.iseke.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.co.iseke.entity.ChartOfAccounts;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrialBalanceEntryDTO {
    private ChartOfAccounts account;
    private BigDecimal debitBalance;
    private BigDecimal creditBalance;
}