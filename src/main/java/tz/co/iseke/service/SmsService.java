package tz.co.iseke.service;


import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tz.co.iseke.dto.NotificationProperties;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    @Value("${twilio.phone-number}")
    private String twilioPhoneNumber;

    private final NotificationProperties notificationProperties;

    @Async
    public void sendSms(String toPhoneNumber, String messageBody) {
        if (!notificationProperties.getSms().isEnabled()) {
            log.info("SMS is disabled. Would have sent to {}: {}", toPhoneNumber, messageBody);
            return;
        }

        try {
            // Validate phone number format
            String formattedNumber = formatPhoneNumber(toPhoneNumber);

            Message message = Message.creator(
                    new PhoneNumber(formattedNumber),
                    new PhoneNumber(twilioPhoneNumber),
                    messageBody
            ).create();

            log.info("SMS sent successfully. SID: {}, To: {}", message.getSid(), formattedNumber);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", toPhoneNumber, e.getMessage(), e);
        }
    }

    private String formatPhoneNumber(String phoneNumber) {
        // Remove any spaces, dashes, or parentheses
        String cleaned = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");

        // If number starts with 0 (local format), replace with +255 for Tanzania
        if (cleaned.startsWith("0")) {
            cleaned = "+255" + cleaned.substring(1);
        } else if (!cleaned.startsWith("+")) {
            // If no country code, assume Tanzania
            cleaned = "+255" + cleaned;
        }

        return cleaned;
    }

    // Specific SMS methods for different events
    @Async
    public void sendMemberRegistrationSms(String phoneNumber, String memberNumber, String fullName) {
        String message = String.format(
                "Welcome to SACCOS! Your member number is %s. Thank you for joining us, %s. " +
                        "Visit our office or call for more information.",
                memberNumber, fullName
        );
        sendSms(phoneNumber, message);
    }

    @Async
    public void sendTransactionSms(String phoneNumber, String transactionType,
                                   String amount, String balance, String accountNumber) {
        String message = String.format(
                "SACCOS Transaction Alert:\n%s of TZS %s\nAccount: %s\nNew Balance: TZS %s",
                transactionType, amount, accountNumber, balance
        );
        sendSms(phoneNumber, message);
    }

    @Async
    public void sendLoanApprovalSms(String phoneNumber, String loanNumber, String amount) {
        String message = String.format(
                "Congratulations! Your loan application %s for TZS %s has been APPROVED. " +
                        "Visit our office for disbursement details.",
                loanNumber, amount
        );
        sendSms(phoneNumber, message);
    }

    @Async
    public void sendLoanDisbursementSms(String phoneNumber, String loanNumber,
                                        String amount, String accountNumber) {
        String message = String.format(
                "Loan Disbursement Alert:\nLoan %s of TZS %s has been disbursed to account %s. " +
                        "First repayment details will be sent shortly.",
                loanNumber, amount, accountNumber
        );
        sendSms(phoneNumber, message);
    }

    @Async
    public void sendLoanRepaymentSms(String phoneNumber, String loanNumber,
                                     String amount, String outstandingBalance) {
        String message = String.format(
                "Loan Repayment Received:\nLoan %s\nAmount Paid: TZS %s\nOutstanding: TZS %s\n" +
                        "Thank you for your payment!",
                loanNumber, amount, outstandingBalance
        );
        sendSms(phoneNumber, message);
    }

    @Async
    public void sendLoanReminderSms(String phoneNumber, String loanNumber,
                                    String dueAmount, String dueDate) {
        String message = String.format(
                "Payment Reminder:\nLoan %s\nAmount Due: TZS %s\nDue Date: %s\n" +
                        "Please make your payment on time to avoid penalties.",
                loanNumber, dueAmount, dueDate
        );
        sendSms(phoneNumber, message);
    }
}