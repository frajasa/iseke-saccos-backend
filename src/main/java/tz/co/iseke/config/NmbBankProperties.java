package tz.co.iseke.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "payment.nmb")
public class NmbBankProperties {
    private boolean enabled = false;
    private String apiBaseUrl;
    private String clientId;
    private String clientSecret;
    private String accountNumber;
}
