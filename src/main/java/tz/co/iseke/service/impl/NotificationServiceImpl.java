package tz.co.iseke.service.impl;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import tz.co.iseke.service.NotificationService;

import jakarta.annotation.PostConstruct;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;

    @Value("${twilio.account-sid}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token}")
    private String twilioAuthToken;

    @Value("${twilio.phone-number}")
    private String twilioPhoneNumber;

    @Value("${saccos.name}")
    private String saccosName;

    @Value("${saccos.email}")
    private String saccosEmail;

    @Value("${saccos.phone}")
    private String saccosPhone;

    public NotificationServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void initTwilio() {
        try {
            if (StringUtils.isNotBlank(twilioAccountSid) && StringUtils.isNotBlank(twilioAuthToken)) {
                Twilio.init(twilioAccountSid, twilioAuthToken);
                log.info("Twilio initialized successfully");
            } else {
                log.warn("Twilio credentials not provided. SMS functionality will be disabled.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Twilio: {}", e.getMessage());
        }
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        try {
            if (StringUtils.isBlank(phoneNumber) || StringUtils.isBlank(message)) {
                log.warn("Phone number or message is blank. SMS not sent.");
                return;
            }

            String formattedPhone = formatPhoneNumber(phoneNumber);
            
            Message.creator(
                    new PhoneNumber(formattedPhone),
                    new PhoneNumber(twilioPhoneNumber),
                    message
            ).create();

            log.info("SMS sent successfully to: {}", formattedPhone);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
        }
    }

    @Override
    public void sendEmail(String toEmail, String subject, String message) {
        try {
            if (StringUtils.isBlank(toEmail) || StringUtils.isBlank(subject) || StringUtils.isBlank(message)) {
                log.warn("Email parameters are invalid. Email not sent.");
                return;
            }

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(saccosEmail);
            mailMessage.setTo(toEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);

            mailSender.send(mailMessage);
            log.info("Email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendWelcomeSms(String phoneNumber, String memberName, String memberNumber) {
        String message = String.format(
                "Welcome to %s, %s! Your member number is %s. Thank you for joining us. For support, call %s",
                saccosName, memberName, memberNumber, saccosPhone
        );
        sendSms(phoneNumber, message);
    }

    @Override
    public void sendLoanApprovalNotification(String phoneNumber, String email, String memberName, 
                                           String loanNumber, String amount) {
        String message = String.format(
                "Dear %s, your loan application %s for TZS %s has been approved. Visit our office for disbursement. %s",
                memberName, loanNumber, formatCurrency(amount), saccosName
        );
        
        if (StringUtils.isNotBlank(phoneNumber)) {
            sendSms(phoneNumber, message);
        }
        
        if (StringUtils.isNotBlank(email)) {
            sendEmail(email, "Loan Approval - " + saccosName, message);
        }
    }

    @Override
    public void sendLoanRepaymentReminder(String phoneNumber, String email, String memberName, 
                                        String loanNumber, String dueDate, String amount) {
        String message = String.format(
                "Dear %s, your loan %s payment of TZS %s is due on %s. Please make payment to avoid penalties. %s",
                memberName, loanNumber, formatCurrency(amount), dueDate, saccosName
        );
        
        if (StringUtils.isNotBlank(phoneNumber)) {
            sendSms(phoneNumber, message);
        }
        
        if (StringUtils.isNotBlank(email)) {
            sendEmail(email, "Loan Payment Reminder - " + saccosName, message);
        }
    }

    @Override
    public void sendTransactionNotification(String phoneNumber, String email, String memberName, 
                                          String transactionType, String amount, String balance) {
        String message = String.format(
                "Dear %s, %s of TZS %s completed successfully. Your balance is TZS %s. %s",
                memberName, transactionType, formatCurrency(amount), formatCurrency(balance), saccosName
        );
        
        if (StringUtils.isNotBlank(phoneNumber)) {
            sendSms(phoneNumber, message);
        }
        
        if (StringUtils.isNotBlank(email)) {
            sendEmail(email, "Transaction Notification - " + saccosName, message);
        }
    }

    @Override
    public void sendAccountActivationNotification(String phoneNumber, String email, String memberName, 
                                                 String accountNumber) {
        String message = String.format(
                "Dear %s, your savings account %s has been activated successfully. Start saving today! %s",
                memberName, accountNumber, saccosName
        );
        
        if (StringUtils.isNotBlank(phoneNumber)) {
            sendSms(phoneNumber, message);
        }
        
        if (StringUtils.isNotBlank(email)) {
            sendEmail(email, "Account Activation - " + saccosName, message);
        }
    }

    @Override
    public void sendPasswordResetNotification(String email, String resetToken) {
        String message = String.format(
                "Your password reset token is: %s. This token will expire in 15 minutes. If you didn't request this, please ignore this email. %s",
                resetToken, saccosName
        );
        
        sendEmail(email, "Password Reset - " + saccosName, message);
    }

    private String formatPhoneNumber(String phoneNumber) {
        // Remove all non-numeric characters
        String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
        
        // If starts with 0, replace with +255 (Tanzania country code)
        if (cleanNumber.startsWith("0")) {
            cleanNumber = "+255" + cleanNumber.substring(1);
        }
        // If doesn't start with +, assume it's a Tanzania number
        else if (!cleanNumber.startsWith("+")) {
            cleanNumber = "+255" + cleanNumber;
        }
        // If starts with 255, add +
        else if (cleanNumber.startsWith("255")) {
            cleanNumber = "+" + cleanNumber;
        }
        
        return cleanNumber;
    }

    private String formatCurrency(String amount) {
        try {
            double value = Double.parseDouble(amount);
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
            return formatter.format(value);
        } catch (NumberFormatException e) {
            return amount;
        }
    }
}