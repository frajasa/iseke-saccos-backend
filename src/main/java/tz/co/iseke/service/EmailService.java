package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import tz.co.iseke.dto.NotificationProperties;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificationProperties notificationProperties;

    @Async
    public void sendSimpleEmail(String to, String subject, String body) {
        if (!notificationProperties.getEmail().isEnabled()) {
            log.info("Email is disabled. Would have sent to {}: {}", to, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(notificationProperties.getEmail().getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (!notificationProperties.getEmail().isEnabled()) {
            log.info("Email is disabled. Would have sent to {}: {}", to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(
                    notificationProperties.getEmail().getFrom(),
                    notificationProperties.getEmail().getFromName()
            );
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("✅ HTML email sent successfully to: {}", to);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("❌ Failed to send HTML email to {}: {}", to, e.getMessage(), e);
        }
    }

    // Specific email methods for different events
    @Async
    public void sendMemberRegistrationEmail(String email, String fullName,
                                            String memberNumber, String branchName) {
        String subject = "Welcome to SACCOS - Registration Successful";
        String body = buildMemberRegistrationHtml(fullName, memberNumber, branchName);
        sendHtmlEmail(email, subject, body);
    }

    @Async
    public void sendTransactionEmail(String email, String fullName, String transactionType,
                                     String amount, String balance, String accountNumber,
                                     String transactionDate, String referenceNumber) {
        String subject = "Transaction Alert - " + transactionType;
        String body = buildTransactionHtml(fullName, transactionType, amount, balance,
                accountNumber, transactionDate, referenceNumber);
        sendHtmlEmail(email, subject, body);
    }

    @Async
    public void sendLoanApplicationEmail(String email, String fullName, String loanNumber,
                                         String amount, String term) {
        String subject = "Loan Application Received - " + loanNumber;
        String body = buildLoanApplicationHtml(fullName, loanNumber, amount, term);
        sendHtmlEmail(email, subject, body);
    }

    @Async
    public void sendLoanApprovalEmail(String email, String fullName, String loanNumber,
                                      String amount, String interestRate, String term) {
        String subject = "Loan Approved - " + loanNumber;
        String body = buildLoanApprovalHtml(fullName, loanNumber, amount, interestRate, term);
        sendHtmlEmail(email, subject, body);
    }

    @Async
    public void sendLoanDisbursementEmail(String email, String fullName, String loanNumber,
                                          String amount, String accountNumber,
                                          String firstPaymentDate) {
        String subject = "Loan Disbursed - " + loanNumber;
        String body = buildLoanDisbursementHtml(fullName, loanNumber, amount,
                accountNumber, firstPaymentDate);
        sendHtmlEmail(email, subject, body);
    }

    @Async
    public void sendLoanRepaymentEmail(String email, String fullName, String loanNumber,
                                       String amountPaid, String outstandingBalance,
                                       String nextPaymentDate) {
        String subject = "Loan Repayment Received - " + loanNumber;
        String body = buildLoanRepaymentHtml(fullName, loanNumber, amountPaid,
                outstandingBalance, nextPaymentDate);
        sendHtmlEmail(email, subject, body);
    }

    @Async
    public void sendMonthlyStatementEmail(String email, String fullName, String memberNumber,
                                          String totalSavings, String totalLoans,
                                          String monthYear) {
        String subject = "Monthly Statement - " + monthYear;
        String body = buildMonthlyStatementHtml(fullName, memberNumber, totalSavings,
                totalLoans, monthYear);
        sendHtmlEmail(email, subject, body);
    }

    // HTML Email Templates
    private String buildMemberRegistrationHtml(String fullName, String memberNumber,
                                               String branchName) {
        return String.format("""
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <style>
                                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                                .header { background: linear-gradient(135deg, #0D5A9E 0%%, #2E8B57 100%%); 
                                         color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                                .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                                .info-box { background: white; padding: 20px; margin: 20px 0; 
                                           border-left: 4px solid #0D5A9E; border-radius: 5px; }
                                .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                                .highlight { color: #0D5A9E; font-weight: bold; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>Welcome to SACCOS!</h1>
                                    <p>Empowering Financial Growth</p>
                                </div>
                                <div class="content">
                                    <p>Dear <strong>%s</strong>,</p>
                                    <p>Congratulations! Your registration with SACCOS has been completed successfully.</p>
                        
                                    <div class="info-box">
                                        <h3>Your Member Details:</h3>
                                        <p><strong>Member Number:</strong> <span class="highlight">%s</span></p>
                                        <p><strong>Branch:</strong> %s</p>
                                        <p><strong>Registration Date:</strong> %s</p>
                                    </div>
                        
                                    <p>You can now:</p>
                                    <ul>
                                        <li>Open savings accounts</li>
                                        <li>Apply for loans</li>
                                        <li>Access all SACCOS services</li>
                                    </ul>
                        
                                    <p>Please visit your branch to complete any additional requirements.</p>
                        
                                    <p>Best regards,<br><strong>SACCOS Team</strong></p>
                                </div>
                                <div class="footer">
                                    <p>This is an automated message from SACCOS MIS. Please do not reply to this email.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """, fullName, memberNumber, branchName,
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
    }

    private String buildTransactionHtml(String fullName, String transactionType, String amount,
                                        String balance, String accountNumber,
                                        String transactionDate, String referenceNumber) {
        String color = transactionType.toUpperCase().contains("DEPOSIT") ? "#2E8B57" : "#0D5A9E";

        return String.format("""
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <style>
                                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                                .header { background: %s; color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }
                                .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                                .transaction-box { background: white; padding: 20px; margin: 20px 0; border-radius: 5px; }
                                .amount { font-size: 24px; color: %s; font-weight: bold; }
                                .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h2>Transaction Alert</h2>
                                </div>
                                <div class="content">
                                    <p>Dear <strong>%s</strong>,</p>
                                    <p>A transaction has been processed on your account.</p>                      
                                    <div class="transaction-box">
                                        <table style="width: 100%%;">
                                            <tr>
                                                <td><strong>Transaction Type:</strong></td>
                                                <td>%s</td>
                                            </tr>
                                            <tr>
                                                <td><strong>Amount:</strong></td>
                                                <td class="amount">TZS %s</td>
                                            </tr>
                                            <tr>
                                                <td><strong>Account Number:</strong></td>
                                                <td>%s</td>
                                            </tr>
                                            <tr>
                                                <td><strong>New Balance:</strong></td>
                                                <td><strong>TZS %s</strong></td>
                                            </tr>
                                            <tr>
                                                <td><strong>Date:</strong></td>
                                                <td>%s</td>
                                            </tr>
                                            <tr>
                                                <td><strong>Reference:</strong></td>
                                                <td>%s</td>
                                            </tr>
                                        </table>
                                    </div>
                        
                                    <p>If you did not authorize this transaction, please contact us immediately.</p>
                        
                                    <p>Best regards,<br><strong>SACCOS Team</strong></p>
                                </div>
                                <div class="footer">
                                    <p>This is an automated message. Please do not reply.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """, color, color, fullName, transactionType, amount, accountNumber,
                balance, transactionDate, referenceNumber);
    }

    private String buildLoanApprovalHtml(String fullName, String loanNumber, String amount,
                                         String interestRate, String term) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #2E8B57 0%%, #0D5A9E 100%%);
                                 color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                        .success-box { background: #d4edda; border: 1px solid #c3e6cb; 
                                      padding: 20px; margin: 20px 0; border-radius: 5px; }
                        .loan-details { background: white; padding: 20px; margin: 20px 0; border-radius: 5px; }
                        .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🎉 Loan Approved!</h1>
                        </div>
                        <div class="content">
                            <p>Dear <strong>%s</strong>,</p>
                
                            <div class="success-box">
                                <h3 style="color: #155724; margin: 0;">Congratulations!</h3>
                                <p style="margin: 10px 0 0 0;">Your loan application has been APPROVED.</p>
                            </div>
                
                            <div class="loan-details">
                                <h3>Loan Details:</h3>
                                <table style="width: 100%%;">
                                    <tr>
                                        <td><strong>Loan Number:</strong></td>
                                        <td>%s</td>
                                    </tr>
                                    <tr>
                                        <td><strong>Approved Amount:</strong></td>
                                        <td><strong style="color: #2E8B57; font-size: 18px;">TZS %s</strong></td>
                                    </tr>
                                    <tr>
                                        <td><strong>Interest Rate:</strong></td>
                                        <td>%s%%</td>
                                    </tr>
                                    <tr>
                                        <td><strong>Loan Term:</strong></td>
                                        <td>%s months</td>
                                    </tr>
                                </table>
                            </div>
                
                            <p><strong>Next Steps:</strong></p>
                            <ol>
                                <li>Visit your branch for loan disbursement</li>
                                <li>Bring your ID and member card</li>
                                <li>Sign the loan agreement</li>
                            </ol>
                
                            <p>Best regards,<br><strong>SACCOS Team</strong></p>
                        </div>
                        <div class="footer">
                            <p>This is an automated message. Please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, fullName, loanNumber, amount, interestRate, term);
    }

    private String buildLoanDisbursementHtml(String fullName, String loanNumber, String amount,
                                             String accountNumber, String firstPaymentDate) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #0D5A9E; color: white; padding: 30px; 
                                 text-align: center; border-radius: 10px 10px 0 0; }
                        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                        .info-box { background: white; padding: 20px; margin: 20px 0; 
                                   border-left: 4px solid #2E8B57; border-radius: 5px; }
                        .warning-box { background: #fff3cd; border: 1px solid #ffc107; 
                                      padding: 15px; margin: 20px 0; border-radius: 5px; }
                        .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>✅ Loan Disbursed</h1>
                        </div>
                        <div class="content">
                            <p>Dear <strong>%s</strong>,</p>
                            <p>Your loan has been successfully disbursed!</p>
                
                            <div class="info-box">
                                <h3>Disbursement Details:</h3>
                                <table style="width: 100%%;">
                                    <tr>
                                        <td><strong>Loan Number:</strong></td>
                                        <td>%s</td>
                                    </tr>
                                    <tr>
                                        <td><strong>Amount Disbursed:</strong></td>
                                        <td><strong style="color: #2E8B57; font-size: 18px;">TZS %s</strong></td>
                                    </tr>
                                    <tr>
                                        <td><strong>Credited to Account:</strong></td>
                                        <td>%s</td>
                                    </tr>
                                    <tr>
                                        <td><strong>First Payment Due:</strong></td>
                                        <td><strong>%s</strong></td>
                                    </tr>
                                </table>
                            </div>
                
                            <div class="warning-box">
                                <strong>⚠️ Important Reminder:</strong>
                                <p style="margin: 10px 0 0 0;">Please ensure timely repayments to maintain a good credit history 
                                and avoid penalties.</p>
                            </div>
                
                            <p>You can check your repayment schedule by logging into your account or visiting your branch.</p>
                
                            <p>Best regards,<br><strong>SACCOS Team</strong></p>
                        </div>
                        <div class="footer">
                            <p>This is an automated message. Please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, fullName, loanNumber, amount, accountNumber, firstPaymentDate);
    }

    private String buildLoanRepaymentHtml(String fullName, String loanNumber, String amountPaid,
                                          String outstandingBalance, String nextPaymentDate) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #2E8B57; color: white; padding: 30px; 
                                 text-align: center; border-radius: 10px 10px 0 0; }
                        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                        .payment-box { background: white; padding: 20px; margin: 20px 0; border-radius: 5px; }
                        .highlight { color: #2E8B57; font-weight: bold; }
                        .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                        table { width: 100%%; border-collapse: collapse; }
                        td { padding: 8px 0; vertical-align: top; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>💰 Payment Received</h1>
                        </div>
                        <div class="content">
                            <p>Dear <strong>%s</strong>,</p>
                            <p>Thank you for your recent loan repayment. Below are the details of your transaction:</p>
                
                            <div class="payment-box">
                                <table>
                                    <tr>
                                        <td><strong>Loan Number:</strong></td>
                                        <td>%s</td>
                                    </tr>
                                    <tr>
                                        <td><strong>Amount Paid:</strong></td>
                                        <td class="highlight">TZS %s</td>
                                    </tr>
                                    <tr>
                                        <td><strong>Outstanding Balance:</strong></td>
                                        <td><strong>TZS %s</strong></td>
                                    </tr>
                                    <tr>
                                        <td><strong>Next Payment Due:</strong></td>
                                        <td>%s</td>
                                    </tr>
                                </table>
                            </div>
                
                            <p>Your continued commitment to timely repayments helps you build a strong credit record and keeps your SACCOS financially healthy.</p>
                
                            <p>To view your full repayment schedule, log in to your SACCOS account or visit your nearest branch.</p>
                
                            <p>Best regards,<br><strong>SACCOS Team</strong></p>
                        </div>
                        <div class="footer">
                            <p>This is an automated message. Please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, fullName, loanNumber, amountPaid, outstandingBalance, nextPaymentDate);
    }
    private String buildMonthlyStatementHtml(String fullName, String memberNumber,
                                             String totalSavings, String totalLoans,
                                             String monthYear) {
        return String.format("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                .container { max-width: 700px; margin: 20px auto; background: #fff; 
                             border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); overflow: hidden; }
                .header { background: linear-gradient(135deg, #0D5A9E 0%%, #2E8B57 100%%); 
                          color: white; padding: 25px 20px; text-align: center; }
                .header h1 { margin: 0; font-size: 24px; }
                .content { padding: 30px; background: #f9f9f9; }
                .content h3 { color: #0D5A9E; margin-bottom: 15px; }
                table { width: 100%%; border-collapse: collapse; background: white; border-radius: 5px; }
                th, td { padding: 12px; text-align: left; }
                th { background: #0D5A9E; color: white; }
                tr:nth-child(even) { background: #f3f3f3; }
                .summary { margin-top: 25px; padding: 20px; background: white; border-left: 4px solid #2E8B57; border-radius: 5px; }
                .summary p { margin: 8px 0; font-size: 15px; }
                .highlight { color: #0D5A9E; font-weight: bold; }
                .footer { text-align: center; padding: 15px; font-size: 12px; color: #777; background: #fff; border-top: 1px solid #eee; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>📊 Monthly Statement</h1>
                    <p>%s</p>
                </div>

                <div class="content">
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Here is your SACCOS monthly financial summary for the period of <strong>%s</strong>.</p>

                    <div class="summary">
                        <p><strong>Member Number:</strong> %s</p>
                        <p><strong>Total Savings:</strong> <span class="highlight">TZS %s</span></p>
                        <p><strong>Total Outstanding Loans:</strong> <span class="highlight">TZS %s</span></p>
                    </div>

                    <p>Keep up your savings and ensure timely loan repayments to maintain a healthy financial record within your SACCOS.</p>

                    <p>Thank you for your continued commitment and trust in SACCOS.</p>

                    <p>Warm regards,<br><strong>SACCOS Team</strong></p>
                </div>

                <div class="footer">
                    <p>This is an automated message from SACCOS MIS. Please do not reply.</p>
                </div>
            </div>
        </body>
        </html>
        """, monthYear, fullName, monthYear, memberNumber, totalSavings, totalLoans);
    }
    private String buildLoanApplicationHtml(String fullName, String loanNumber,
                                            String amount, String term) {
        return String.format("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                .container { max-width: 700px; margin: 20px auto; background: #fff; 
                             border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); overflow: hidden; }
                .header { background: linear-gradient(135deg, #0D5A9E 0%%, #2E8B57 100%%); 
                          color: white; padding: 25px 20px; text-align: center; }
                .header h1 { margin: 0; font-size: 24px; }
                .content { padding: 30px; background: #f9f9f9; }
                .content h3 { color: #0D5A9E; margin-bottom: 15px; }
                table { width: 100%%; border-collapse: collapse; background: white; border-radius: 5px; }
                th, td { padding: 12px; text-align: left; }
                tr:nth-child(even) { background: #f3f3f3; }
                .summary { margin-top: 25px; padding: 20px; background: white; border-left: 4px solid #0D5A9E; border-radius: 5px; }
                .summary p { margin: 8px 0; font-size: 15px; }
                .highlight { color: #0D5A9E; font-weight: bold; }
                .footer { text-align: center; padding: 15px; font-size: 12px; color: #777; background: #fff; border-top: 1px solid #eee; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>📄 Loan Application Received</h1>
                </div>

                <div class="content">
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Thank you for applying for a loan with SACCOS. We’ve received your application and it is currently being reviewed by our credit team.</p>

                    <div class="summary">
                        <h3>Application Details</h3>
                        <table>
                            <tr>
                                <td><strong>Loan Number:</strong></td>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <td><strong>Requested Amount:</strong></td>
                                <td class="highlight">TZS %s</td>
                            </tr>
                            <tr>
                                <td><strong>Loan Term:</strong></td>
                                <td>%s months</td>
                            </tr>
                        </table>
                    </div>

                    <p>Our credit officers will contact you soon for further verification and documentation.</p>

                    <p><strong>Next Steps:</strong></p>
                    <ul>
                        <li>Wait for confirmation from your branch loan officer</li>
                        <li>Ensure your guarantors and collateral (if applicable) are ready</li>
                        <li>Follow up via your branch or member portal for updates</li>
                    </ul>

                    <p>We appreciate your trust in SACCOS for your financial needs.</p>

                    <p>Best regards,<br><strong>SACCOS Credit Team</strong></p>
                </div>

                <div class="footer">
                    <p>This is an automated message from SACCOS MIS. Please do not reply.</p>
                </div>
            </div>
        </body>
        </html>
        """, fullName, loanNumber, amount, term);
    }

}