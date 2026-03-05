package tz.co.iseke.inputs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.co.iseke.enums.ProductStatus;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLoanProductInput {
    private String productName;
    private String description;
    private BigDecimal interestRate;
    private BigDecimal processingFeeRate;
    private BigDecimal latePaymentPenaltyRate;
    private ProductStatus status;
}