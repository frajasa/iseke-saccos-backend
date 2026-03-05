package tz.co.iseke.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String phoneNumber;
    private String email;
    private String subject;
    private String message;
    private String memberName;
    private String memberNumber;
    private NotificationType type;
    
    public enum NotificationType {
        SMS_ONLY,
        EMAIL_ONLY,
        BOTH
    }
}