package tz.co.iseke.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.co.iseke.entity.Member;
import tz.co.iseke.entity.SavingsAccount;
import tz.co.iseke.entity.Transaction;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberStatementDTO {
    private Member member;
    private SavingsAccount account;
    private List<Transaction> transactions;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private String period;
}
