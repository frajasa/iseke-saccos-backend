package tz.co.iseke.inputs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.co.iseke.enums.InterestMethod;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLoanProductInput {
    private String productCode;
    private String productName;
    private String description;
    private BigDecimal interestRate;
    private InterestMethod interestMethod;
    private String repaymentFrequency;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private Integer minimumTermMonths;
    private Integer maximumTermMonths;
    private BigDecimal processingFeeRate;
    private BigDecimal processingFeeFixed;
    private BigDecimal insuranceFeeRate;
    private BigDecimal latePaymentPenaltyRate;
    private Integer gracePeriodDays;
    private Boolean requiresGuarantors;
    private Integer minimumGuarantors;
    private Boolean requiresCollateral;
    private BigDecimal collateralPercentage;
}