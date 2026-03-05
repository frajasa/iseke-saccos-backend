package tz.co.iseke.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.co.iseke.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loan_accounts",schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "loan_number", unique = true, nullable = false, length = 30)
    private String loanNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate = LocalDate.now();

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "repayment_frequency", nullable = false, length = 50)
    private String repaymentFrequency;

    @Column(name = "outstanding_principal", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal outstandingPrincipal = BigDecimal.ZERO;

    @Column(name = "outstanding_interest", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal outstandingInterest = BigDecimal.ZERO;

    @Column(name = "outstanding_fees", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal outstandingFees = BigDecimal.ZERO;

    @Column(name = "outstanding_penalties", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal outstandingPenalties = BigDecimal.ZERO;

    @Column(name = "total_paid", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(name = "next_payment_date")
    private LocalDate nextPaymentDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private LoanStatus status = LoanStatus.APPLIED;

    @Column(name = "loan_officer", length = 100)
    private String loanOfficer;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "days_in_arrears")
    @Builder.Default
    private Integer daysInArrears = 0;

    @Column(name = "write_off_reason", columnDefinition = "TEXT")
    private String writeOffReason;

    @Column(name = "write_off_date")
    private LocalDate writeOffDate;

    @Column(name = "written_off_by", length = 100)
    private String writtenOffBy;

    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Guarantor> guarantors = new ArrayList<>();

    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Collateral> collateral = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.outstandingInterest == null) this.outstandingInterest = BigDecimal.ZERO;
        if (this.outstandingPenalties == null) this.outstandingPenalties = BigDecimal.ZERO;
        if (this.outstandingFees == null) this.outstandingFees = BigDecimal.ZERO;
        if (this.totalPaid == null) this.totalPaid = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PostLoad
    private void setDefaults() {
        if (this.outstandingInterest == null) this.outstandingInterest = BigDecimal.ZERO;
        if (this.outstandingPenalties == null) this.outstandingPenalties = BigDecimal.ZERO;
        if (this.outstandingFees == null) this.outstandingFees = BigDecimal.ZERO;
        if (this.totalPaid == null) this.totalPaid = BigDecimal.ZERO;
    }
}