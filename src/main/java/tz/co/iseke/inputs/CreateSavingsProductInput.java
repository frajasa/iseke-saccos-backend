package tz.co.iseke.inputs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.co.iseke.enums.ProductStatus;
import tz.co.iseke.enums.SavingsProductType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSavingsProductInput {
    private String productCode;
    private String productName;
    private SavingsProductType productType;
    private String description;
    private BigDecimal interestRate;
    private String interestCalculationMethod;
    private String interestPaymentFrequency;
    private BigDecimal minimumBalance;
    private BigDecimal maximumBalance;
    private BigDecimal minimumOpeningBalance;
    private Integer withdrawalLimit;
    private BigDecimal withdrawalFee;
    private BigDecimal monthlyFee;
    private BigDecimal taxWithholdingRate;
    private Integer dormancyPeriodDays;
    private Boolean allowsOverdraft;
    private ProductStatus status;
}