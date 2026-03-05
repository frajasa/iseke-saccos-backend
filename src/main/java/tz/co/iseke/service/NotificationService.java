package tz.co.iseke.service;

public interface NotificationService {
    
    /**
     * Send SMS notification
     */
    void sendSms(String phoneNumber, String message);
    
    /**
     * Send email notification
     */
    void sendEmail(String toEmail, String subject, String message);
    
    /**
     * Send welcome SMS to new member
     */
    void sendWelcomeSms(String phoneNumber, String memberName, String memberNumber);
    
    /**
     * Send loan approval notification
     */
    void sendLoanApprovalNotification(String phoneNumber, String email, String memberName, 
                                    String loanNumber, String amount);
    
    /**
     * Send loan repayment reminder
     */
    void sendLoanRepaymentReminder(String phoneNumber, String email, String memberName, 
                                 String loanNumber, String dueDate, String amount);
    
    /**
     * Send transaction notification
     */
    void sendTransactionNotification(String phoneNumber, String email, String memberName, 
                                   String transactionType, String amount, String balance);
    
    /**
     * Send account activation notification
     */
    void sendAccountActivationNotification(String phoneNumber, String email, String memberName, 
                                          String accountNumber);
    
    /**
     * Send password reset notification
     */
    void sendPasswordResetNotification(String email, String resetToken);
}