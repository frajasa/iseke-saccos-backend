package tz.co.iseke.inputs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.co.iseke.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRepaymentInput {
    private UUID loanId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
}