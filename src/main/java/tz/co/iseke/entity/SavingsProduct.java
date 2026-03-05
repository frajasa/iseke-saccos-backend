package tz.co.iseke.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.co.iseke.enums.ProductStatus;
import tz.co.iseke.enums.SavingsProductType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "savings_products",schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_code", unique = true, nullable = false, length = 20)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 50)
    private SavingsProductType productType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "interest_calculation_method", length = 50)
    @Builder.Default
    private String interestCalculationMethod = "DAILY_BALANCE";

    @Column(name = "interest_payment_frequency", length = 50)
    @Builder.Default
    private String interestPaymentFrequency = "MONTHLY";

    @Column(name = "minimum_balance", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minimumBalance = BigDecimal.ZERO;

    @Column(name = "maximum_balance", precision = 15, scale = 2)
    private BigDecimal maximumBalance;

    @Column(name = "minimum_opening_balance", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minimumOpeningBalance = BigDecimal.ZERO;

    @Column(name = "withdrawal_limit")
    private Integer withdrawalLimit;

    @Column(name = "withdrawal_fee", precision = 10, scale = 2)
    private BigDecimal withdrawalFee;

    @Column(name = "monthly_fee", precision = 10, scale = 2)
    private BigDecimal monthlyFee;

    @Column(name = "tax_withholding_rate", precision = 5, scale = 4)
    private BigDecimal taxWithholdingRate;

    @Column(name = "dormancy_period_days")
    @Builder.Default
    private Integer dormancyPeriodDays = 365;

    @Column(name = "allows_overdraft")
    @Builder.Default
    private Boolean allowsOverdraft = false;

    @Column(name = "auto_rollover")
    @Builder.Default
    private Boolean autoRollover = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liability_account_id")
    private ChartOfAccounts liabilityAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_account_id")
    private ChartOfAccounts cashAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_expense_account_id")
    private ChartOfAccounts interestExpenseAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_income_account_id")
    private ChartOfAccounts feeIncomeAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_payable_account_id")
    private ChartOfAccounts taxPayableAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "withdrawal_fee_account_id")
    private ChartOfAccounts withdrawalFeeAccount;

    @PostLoad
    private void setDefaults() {
        if (interestCalculationMethod == null) interestCalculationMethod = "DAILY_BALANCE";
        if (interestPaymentFrequency == null) interestPaymentFrequency = "MONTHLY";
        if (minimumBalance == null) minimumBalance = BigDecimal.ZERO;
        if (minimumOpeningBalance == null) minimumOpeningBalance = BigDecimal.ZERO;
        if (dormancyPeriodDays == null) dormancyPeriodDays = 365;
        if (allowsOverdraft == null) allowsOverdraft = false;
        if (autoRollover == null) autoRollover = false;
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}