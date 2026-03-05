package tz.co.iseke.entity;

import jakarta.persistence.*;
import lombok.*;
import tz.co.iseke.enums.PaymentProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_reconciliations", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReconciliation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private PaymentProvider provider;

    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;

    @Column(name = "total_internal_count")
    @Builder.Default
    private Integer totalInternalCount = 0;

    @Column(name = "total_provider_count")
    @Builder.Default
    private Integer totalProviderCount = 0;

    @Column(name = "matched_count")
    @Builder.Default
    private Integer matchedCount = 0;

    @Column(name = "mismatched_count")
    @Builder.Default
    private Integer mismatchedCount = 0;

    @Column(name = "missing_internal_count")
    @Builder.Default
    private Integer missingInternalCount = 0;

    @Column(name = "missing_provider_count")
    @Builder.Default
    private Integer missingProviderCount = 0;

    @Column(name = "total_internal_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalInternalAmount = BigDecimal.ZERO;

    @Column(name = "total_provider_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalProviderAmount = BigDecimal.ZERO;

    @Column(name = "amount_difference", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal amountDifference = BigDecimal.ZERO;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reconciled_by", length = 100)
    private String reconciledBy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
