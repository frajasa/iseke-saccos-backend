package tz.co.iseke.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import tz.co.iseke.enums.AccountType;
import tz.co.iseke.enums.ProductStatus;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "chart_of_accounts",schema = "public")
@Data
@EqualsAndHashCode(callSuper = false)
public class AccountGL {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "account_code", unique = true, nullable = false)
    private String accountCode;

    @NotBlank
    @Column(name = "account_name", nullable = false)
    private String accountName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "account_category")
    private String accountCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AccountGL parentAccount;

    @OneToMany(mappedBy = "parentAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<AccountGL> childAccounts;

    @NotNull
    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @NotNull
    @Column(name = "is_control_account", nullable = false)
    private Boolean isControlAccount = false;

    @NotBlank
    @Column(name = "normal_balance", nullable = false)
    private String normalBalance; // "DEBIT" or "CREDIT"

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}