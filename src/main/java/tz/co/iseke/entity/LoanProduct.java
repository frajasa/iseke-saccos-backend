package tz.co.iseke.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.co.iseke.enums.InterestMethod;
import tz.co.iseke.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_products",schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_code", unique = true, nullable = false, length = 20)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_method", nullable = false, length = 50)
    private InterestMethod interestMethod;

    @Column(name = "repayment_frequency", length = 50)
    private String repaymentFrequency = "MONTHLY";

    @Column(name = "minimum_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal minimumAmount;

    @Column(name = "maximum_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal maximumAmount;

    @Column(name = "minimum_term_months", nullable = false)
    private Integer minimumTermMonths;

    @Column(name = "maximum_term_months", nullable = false)
    private Integer maximumTermMonths;

    @Column(name = "processing_fee_rate", precision = 5, scale = 4)
    private BigDecimal processingFeeRate;

    @Column(name = "processing_fee_fixed", precision = 10, scale = 2)
    private BigDecimal processingFeeFixed;

    @Column(name = "insurance_fee_rate", precision = 5, scale = 4)
    private BigDecimal insuranceFeeRate;

    @Column(name = "late_payment_penalty_rate", precision = 5, scale = 4)
    private BigDecimal latePaymentPenaltyRate;

    @Column(name = "grace_period_days")
    private Integer gracePeriodDays = 0;

    @Column(name = "requires_guarantors")
    private Boolean requiresGuarantors = true;

    @Column(name = "minimum_guarantors")
    private Integer minimumGuarantors = 1;

    @Column(name = "requires_collateral")
    private Boolean requiresCollateral = false;

    @Column(name = "collateral_percentage", precision = 5, scale = 2)
    private BigDecimal collateralPercentage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_receivable_account_id")
    private ChartOfAccounts loanReceivableAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_account_id")
    private ChartOfAccounts cashAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_income_account_id")
    private ChartOfAccounts interestIncomeAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_income_account_id")
    private ChartOfAccounts feeIncomeAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penalty_income_account_id")
    private ChartOfAccounts penaltyIncomeAccount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}