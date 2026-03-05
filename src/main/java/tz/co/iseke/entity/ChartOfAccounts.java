package tz.co.iseke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import lombok.Builder;
import tz.co.iseke.enums.AccountType;
import tz.co.iseke.enums.ProductStatus;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "chart_of_accounts",schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartOfAccounts {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_code", unique = true, nullable = false, length = 20)
    private String accountCode;

    @Column(name = "account_name", nullable = false, length = 200)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 50)
    private AccountType accountType;

    @Column(name = "account_category", length = 100)
    private String accountCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    private ChartOfAccounts parentAccount;

    @Column(nullable = false)
    @Builder.Default
    private Integer level = 1;

    @Column(name = "is_control_account")
    @Builder.Default
    private Boolean isControlAccount = false;

    @Column(name = "normal_balance", length = 10)
    private String normalBalance;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PostLoad
    private void setDefaults() {
        if (level == null) level = 1;
        if (isControlAccount == null) isControlAccount = false;
        if (normalBalance == null) normalBalance = "DEBIT";
    }
}