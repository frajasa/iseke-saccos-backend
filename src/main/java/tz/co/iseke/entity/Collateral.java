package tz.co.iseke.entity;

import jakarta.persistence.*;
import lombok.*;
import tz.co.iseke.enums.CollateralStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "collateral",schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collateral {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_account_id", nullable = false)
    private LoanAccount loanAccount;

    @Column(name = "collateral_type", nullable = false, length = 100)
    private String collateralType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "estimated_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "location", columnDefinition = "TEXT")
    private String location;

    @Column(name = "ownership_document", length = 200)
    private String ownershipDocument;

    @Column(name = "serial_number", length = 100)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private CollateralStatus status = CollateralStatus.PLEDGED;

    @Column(name = "valuation_date")
    private LocalDate valuationDate;

    @Column(name = "valuated_by", length = 100)
    private String valuatedBy;

    @Column(name = "insurance_policy", length = 100)
    private String insurancePolicy;

    @Column(name = "insurance_expiry")
    private LocalDate insuranceExpiry;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}