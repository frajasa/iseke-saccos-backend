package tz.co.iseke.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {
    private EmailProperties email = new EmailProperties();
    private SmsProperties sms = new SmsProperties();

    @Data
    public static class EmailProperties {
        private String from = "springboot@gmail.com";
        private String fromName = "SACCOS MIS";
        private boolean enabled = true;
    }

    @Data
    public static class SmsProperties {
        private boolean enabled = true;
    }
}