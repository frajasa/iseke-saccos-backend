package tz.co.iseke.entity;

import jakarta.persistence.*;
import lombok.*;
import tz.co.iseke.enums.PaymentProvider;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_provider_configs", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProviderConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, unique = true, length = 30)
    private PaymentProvider provider;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = false;

    @Column(name = "api_base_url", length = 500)
    private String apiBaseUrl;

    @Column(name = "api_key", length = 1000)
    private String apiKey;

    @Column(name = "api_secret", length = 1000)
    private String apiSecret;

    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "service_provider_code", length = 50)
    private String serviceProviderCode;

    @Column(name = "callback_base_url", length = 500)
    private String callbackBaseUrl;

    @Column(name = "settlement_gl_account_code", length = 20)
    private String settlementGlAccountCode;

    @Column(name = "extra_config", columnDefinition = "TEXT")
    private String extraConfig;

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
