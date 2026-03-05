package tz.co.iseke.entity;

import jakarta.persistence.*;
import lombok.*;
import tz.co.iseke.enums.GuarantorStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "guarantors",schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Guarantor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_account_id", nullable = false)
    private LoanAccount loanAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guarantor_member_id")
    private Member guarantorMember;

    @Column(name = "guarantor_name", nullable = false, length = 200)
    private String guarantorName;

    @Column(name = "guarantor_national_id", nullable = false, length = 200)
    private String guarantorNationalId;

    @Column(name = "guarantor_phone", length = 20)
    private String guarantorPhone;

    @Column(name = "guarantor_email", length = 100)
    private String guarantorEmail;

    @Column(name = "guarantor_address", columnDefinition = "TEXT")
    private String guarantorAddress;

    @Column(name = "guaranteed_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal guaranteedAmount;

    @Column(name = "relationship", length = 100)
    private String relationship;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private GuarantorStatus status = GuarantorStatus.ACTIVE;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

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