package tz.co.iseke.entity;

import jakarta.persistence.*;
import lombok.*;
import tz.co.iseke.enums.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_requests", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"member", "savingsAccount", "loanAccount", "transaction"})
public class PaymentRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_number", unique = true, nullable = false, length = 50)
    private String requestNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private PaymentDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PaymentRequestStatus status = PaymentRequestStatus.INITIATED;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "TZS";

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "savings_account_id")
    private SavingsAccount savingsAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_account_id")
    private LoanAccount loanAccount;

    @Column(name = "purpose", length = 200)
    private String purpose;

    @Column(name = "provider_reference", length = 200)
    private String providerReference;

    @Column(name = "provider_conversation_id", length = 200)
    private String providerConversationId;

    @Column(name = "provider_response_code", length = 50)
    private String providerResponseCode;

    @Column(name = "provider_response_message", length = 500)
    private String providerResponseMessage;

    @Column(name = "callback_payload", columnDefinition = "TEXT")
    private String callbackPayload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "initiated_by", length = 100)
    private String initiatedBy;

    @Column(name = "initiated_at", nullable = false)
    @Builder.Default
    private LocalDateTime initiatedAt = LocalDateTime.now();

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "callback_at")
    private LocalDateTime callbackAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (this.initiatedAt == null) {
            this.initiatedAt = LocalDateTime.now();
        }
        if (this.requestNumber == null) {
            this.requestNumber = "PAY" + System.currentTimeMillis();
        }
    }
}
