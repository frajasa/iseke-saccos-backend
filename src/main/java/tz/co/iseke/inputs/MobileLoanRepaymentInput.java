package tz.co.iseke.inputs;

import lombok.Data;
import tz.co.iseke.enums.PaymentProvider;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class MobileLoanRepaymentInput {
    private UUID loanId;
    private BigDecimal amount;
    private PaymentProvider provider;
    private String phoneNumber;
}
