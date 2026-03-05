package tz.co.iseke.entity;

import jakarta.persistence.*;
import lombok.*;
import tz.co.iseke.enums.ScheduleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "repayment_schedules",schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepaymentSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_account_id", nullable = false)
    private LoanAccount loanAccount;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalDue;

    @Column(name = "interest_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestDue;

    @Column(name = "fees_due", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal feesDue = BigDecimal.ZERO;

    @Column(name = "penalty_due", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal penaltyDue = BigDecimal.ZERO;

    @Column(name = "total_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDue;

    @Column(name = "principal_paid", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal principalPaid = BigDecimal.ZERO;

    @Column(name = "interest_paid", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal interestPaid = BigDecimal.ZERO;

    @Column(name = "fees_paid", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal feesPaid = BigDecimal.ZERO;

    @Column(name = "penalty_paid", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal penaltyPaid = BigDecimal.ZERO;

    @Column(name = "total_paid", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(name = "outstanding_balance", precision = 15, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.PENDING;

    @Column(name = "days_overdue")
    @Builder.Default
    private Integer daysOverdue = 0;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        // Calculate total due
        this.totalDue = principalDue.add(interestDue).add(feesDue).add(penaltyDue);
        // Calculate total paid
        this.totalPaid = principalPaid.add(interestPaid).add(feesPaid).add(penaltyPaid);
    }

    @PrePersist
    public void prePersist() {
        // Calculate total due
        this.totalDue = principalDue.add(interestDue).add(feesDue).add(penaltyDue);
        // Calculate total paid
        this.totalPaid = principalPaid.add(interestPaid).add(feesPaid).add(penaltyPaid);
    }
}