package tz.co.iseke.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "payment.tigopesa")
public class TigopesaProperties {
    private boolean enabled = false;
    private boolean testMode = true;
    private String apiBaseUrl = "https://secure.tigo.com";
    private String clientId;
    private String clientSecret;
    private String account;
    private String pin;
    private String merchantName;
    private String currency = "TZS";
    private String language = "eng";
    private String callbackUrl;
    private String redirectUrl;
}
