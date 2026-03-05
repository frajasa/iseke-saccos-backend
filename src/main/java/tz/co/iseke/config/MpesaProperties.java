package tz.co.iseke.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "payment.mpesa")
public class MpesaProperties {
    private boolean enabled = false;
    private String apiBaseUrl;
    private String apiKey;
    private String publicKey;
    private String serviceProviderCode;
}
