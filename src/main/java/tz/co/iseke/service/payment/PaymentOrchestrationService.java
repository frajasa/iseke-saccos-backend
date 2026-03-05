package tz.co.iseke.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.dto.PaymentDashboard;
import tz.co.iseke.entity.*;
import tz.co.iseke.enums.*;
import tz.co.iseke.exception.BusinessException;
import tz.co.iseke.exception.ResourceNotFoundException;
import tz.co.iseke.inputs.MobileDepositInput;
import tz.co.iseke.inputs.MobileDisbursementInput;
import tz.co.iseke.inputs.MobileLoanRepaymentInput;
import tz.co.iseke.repository.PaymentRequestRepository;
import tz.co.iseke.repository.SavingsAccountRepository;
import tz.co.iseke.repository.LoanAccountRepository;
import tz.co.iseke.service.TransactionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentOrchestrationService {

    private final PaymentGatewayRegistry gatewayRegistry;
    private final PaymentRequestRepository paymentRequestRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final TransactionService transactionService;
    private final PaymentCompletionService paymentCompletionService;

    @Transactional
    public PaymentRequest initiateMobileDeposit(MobileDepositInput input, String initiatedBy) {
        SavingsAccount account = savingsAccountRepository.findById(input.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found"));

        PaymentRequest request = PaymentRequest.builder()
                .provider(input.getProvider())
                .direction(PaymentDirection.INBOUND)
                .amount(input.getAmount())
                .phoneNumber(input.getPhoneNumber())
                .member(account.getMember())
                .savingsAccount(account)
                .purpose("DEPOSIT")
                .initiatedBy(initiatedBy)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        request = paymentRequestRepository.save(request);

        PaymentGateway gateway = gatewayRegistry.getGateway(input.getProvider());
        if (!gateway.isAvailable()) {
            throw new BusinessException("Payment provider " + input.getProvider() + " is currently unavailable");
        }

        try {
            request = gateway.initiateCollection(request);
            request.setStatus(PaymentRequestStatus.SENT);
            request.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to initiate mobile deposit via {}: {}", input.getProvider(), e.getMessage());
            request.setStatus(PaymentRequestStatus.FAILED);
            request.setFailureReason(e.getMessage());
        }

        return paymentRequestRepository.save(request);
    }

    @Transactional
    public PaymentRequest initiateMobileLoanRepayment(MobileLoanRepaymentInput input, String initiatedBy) {
        LoanAccount loanAccount = loanAccountRepository.findById(input.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan account not found"));

        PaymentRequest request = PaymentRequest.builder()
                .provider(input.getProvider())
                .direction(PaymentDirection.INBOUND)
                .amount(input.getAmount())
                .phoneNumber(input.getPhoneNumber())
                .member(loanAccount.getMember())
                .loanAccount(loanAccount)
                .purpose("LOAN_REPAYMENT")
                .initiatedBy(initiatedBy)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        request = paymentRequestRepository.save(request);

        PaymentGateway gateway = gatewayRegistry.getGateway(input.getProvider());
        if (!gateway.isAvailable()) {
            throw new BusinessException("Payment provider " + input.getProvider() + " is currently unavailable");
        }

        try {
            request = gateway.initiateCollection(request);
            request.setStatus(PaymentRequestStatus.SENT);
            request.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to initiate mobile loan repayment via {}: {}", input.getProvider(), e.getMessage());
            request.setStatus(PaymentRequestStatus.FAILED);
            request.setFailureReason(e.getMessage());
        }

        return paymentRequestRepository.save(request);
    }

    @Transactional
    public PaymentRequest initiateMobileDisbursement(MobileDisbursementInput input, String initiatedBy) {
        LoanAccount loanAccount = loanAccountRepository.findById(input.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan account not found"));

        PaymentRequest request = PaymentRequest.builder()
                .provider(input.getProvider())
                .direction(PaymentDirection.OUTBOUND)
                .amount(input.getAmount())
                .phoneNumber(input.getPhoneNumber())
                .bankAccountNumber(input.getBankAccountNumber())
                .member(loanAccount.getMember())
                .loanAccount(loanAccount)
                .purpose("LOAN_DISBURSEMENT")
                .initiatedBy(initiatedBy)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        request = paymentRequestRepository.save(request);

        PaymentGateway gateway = gatewayRegistry.getGateway(input.getProvider());
        if (!gateway.isAvailable()) {
            throw new BusinessException("Payment provider " + input.getProvider() + " is currently unavailable");
        }

        try {
            request = gateway.initiateDisbursement(request);
            request.setStatus(PaymentRequestStatus.SENT);
            request.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to initiate mobile disbursement via {}: {}", input.getProvider(), e.getMessage());
            request.setStatus(PaymentRequestStatus.FAILED);
            request.setFailureReason(e.getMessage());
        }

        return paymentRequestRepository.save(request);
    }

    @Transactional
    public PaymentRequest handleCallback(PaymentProvider provider, String payload) {
        PaymentGateway gateway = gatewayRegistry.getGateway(provider);
        PaymentRequest request = gateway.processCallback(payload);

        if (request == null) {
            log.warn("Could not match callback to a payment request for provider: {}", provider);
            return null;
        }

        request.setCallbackAt(LocalDateTime.now());
        request.setCallbackPayload(payload);

        // Process callback if status is CALLBACK_RECEIVED (normal flow)
        // Also recover EXPIRED payments if a valid callback arrives
        if (request.getStatus() == PaymentRequestStatus.CALLBACK_RECEIVED) {
            attemptCompletion(request);
        }

        return paymentRequestRepository.save(request);
    }

    /**
     * Attempt to complete the payment using an isolated transaction.
     * If the financial transaction fails (e.g., business rule violation),
     * the REQUIRES_NEW transaction in PaymentCompletionService rolls back independently,
     * and we can still save the payment status as FAILED without losing the callback data.
     */
    private void attemptCompletion(PaymentRequest request) {
        try {
            Transaction txn = paymentCompletionService.completePayment(request);
            if (txn != null) {
                request.setTransaction(txn);
            }
            request.setStatus(PaymentRequestStatus.COMPLETED);
            request.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to complete payment {}: {}", request.getRequestNumber(), e.getMessage());
            request.setStatus(PaymentRequestStatus.FAILED);
            request.setFailureReason("Post-processing failed: " + e.getMessage());
        }
    }

    @Transactional
    public PaymentRequest checkPaymentStatus(UUID paymentRequestId) {
        PaymentRequest request = paymentRequestRepository.findById(paymentRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment request not found"));

        if (request.getStatus() == PaymentRequestStatus.SENT && request.getProviderReference() != null) {
            PaymentGateway gateway = gatewayRegistry.getGateway(request.getProvider());
            request = gateway.queryTransactionStatus(request.getProviderReference());
        }

        return request;
    }

    @Transactional
    public PaymentRequest cancelPaymentRequest(UUID paymentRequestId) {
        PaymentRequest request = paymentRequestRepository.findById(paymentRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment request not found"));

        if (request.getStatus() != PaymentRequestStatus.INITIATED && request.getStatus() != PaymentRequestStatus.SENT) {
            throw new BusinessException("Cannot cancel payment request with status: " + request.getStatus());
        }

        request.setStatus(PaymentRequestStatus.CANCELLED);
        return paymentRequestRepository.save(request);
    }

    public PaymentRequest getPaymentRequest(UUID id) {
        return paymentRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment request not found"));
    }

    public Page<PaymentRequest> getPaymentRequests(PaymentProvider provider, PaymentRequestStatus status,
                                                    int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "initiatedAt"));
        if (provider != null && status != null) {
            return paymentRequestRepository.findByProviderAndStatus(provider, status, pageable);
        } else if (provider != null) {
            return paymentRequestRepository.findByProvider(provider, pageable);
        } else if (status != null) {
            return paymentRequestRepository.findByStatus(status, pageable);
        }
        return paymentRequestRepository.findAll(pageable);
    }

    public Page<PaymentRequest> getMemberPaymentRequests(UUID memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "initiatedAt"));
        return paymentRequestRepository.findByMemberId(memberId, pageable);
    }

    public List<PaymentProvider> getAvailableProviders() {
        return gatewayRegistry.getAvailableProviders();
    }

    public PaymentDashboard getPaymentDashboard() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        long completed = paymentRequestRepository.countByProviderAndStatusSince(null, PaymentRequestStatus.COMPLETED, startOfDay);

        return PaymentDashboard.builder()
                .totalPaymentsToday(0)
                .completedPayments((int) completed)
                .failedPayments(0)
                .pendingPayments(0)
                .totalAmountToday(BigDecimal.ZERO)
                .totalCollections(BigDecimal.ZERO)
                .totalDisbursements(BigDecimal.ZERO)
                .build();
    }

    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    @Transactional
    public void expireStaleRequests() {
        List<PaymentRequest> staleRequests = paymentRequestRepository
                .findByStatusAndExpiresAtBefore(PaymentRequestStatus.SENT, LocalDateTime.now());

        for (PaymentRequest request : staleRequests) {
            // If a callback was already received but completion failed (transaction rollback),
            // attempt to complete again instead of expiring
            if (request.getCallbackAt() != null) {
                log.info("Retrying completion for payment {} that received callback but wasn't completed",
                        request.getRequestNumber());
                attemptCompletion(request);
            } else {
                log.info("Expiring stale payment request: {}", request.getRequestNumber());
                request.setStatus(PaymentRequestStatus.EXPIRED);
                request.setFailureReason("Request expired without callback");
            }
            paymentRequestRepository.save(request);
        }

        if (!staleRequests.isEmpty()) {
            log.info("Processed {} stale payment requests", staleRequests.size());
        }
    }

    /**
     * Retry expired payments that had valid callbacks but failed completion.
     * Can be called manually by admin.
     */
    @Transactional
    public int retryExpiredPayments() {
        List<PaymentRequest> expiredRequests = paymentRequestRepository
                .findByStatus(PaymentRequestStatus.EXPIRED, PageRequest.of(0, 100))
                .getContent();

        int recovered = 0;
        for (PaymentRequest request : expiredRequests) {
            if (request.getCallbackAt() != null) {
                log.info("Recovering expired payment: {}", request.getRequestNumber());
                attemptCompletion(request);
                paymentRequestRepository.save(request);
                if (request.getStatus() == PaymentRequestStatus.COMPLETED) {
                    recovered++;
                }
            }
        }

        log.info("Recovered {} out of {} expired payment requests", recovered, expiredRequests.size());
        return recovered;
    }
}
