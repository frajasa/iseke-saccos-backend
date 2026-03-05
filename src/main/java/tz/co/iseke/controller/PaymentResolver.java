package tz.co.iseke.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import tz.co.iseke.dto.PaymentDashboard;
import tz.co.iseke.entity.PaymentRequest;
import tz.co.iseke.enums.PaymentProvider;
import tz.co.iseke.enums.PaymentRequestStatus;
import tz.co.iseke.inputs.MobileDepositInput;
import tz.co.iseke.inputs.MobileDisbursementInput;
import tz.co.iseke.inputs.MobileLoanRepaymentInput;
import tz.co.iseke.service.payment.PaymentOrchestrationService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PaymentResolver {

    private final PaymentOrchestrationService paymentOrchestrationService;

    // Queries

    @QueryMapping
    public PaymentRequest paymentRequest(@Argument UUID id) {
        return paymentOrchestrationService.getPaymentRequest(id);
    }

    @QueryMapping
    public Map<String, Object> paymentRequests(@Argument PaymentProvider provider,
                                                @Argument PaymentRequestStatus status,
                                                @Argument Integer page,
                                                @Argument Integer size) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Page<PaymentRequest> result = paymentOrchestrationService.getPaymentRequests(provider, status, pageNum, pageSize);
        return Map.of(
                "content", result.getContent(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        );
    }

    @QueryMapping
    public Map<String, Object> memberPaymentRequests(@Argument UUID memberId,
                                                      @Argument Integer page,
                                                      @Argument Integer size) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Page<PaymentRequest> result = paymentOrchestrationService.getMemberPaymentRequests(memberId, pageNum, pageSize);
        return Map.of(
                "content", result.getContent(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        );
    }

    @QueryMapping
    public List<PaymentProvider> availablePaymentProviders() {
        return paymentOrchestrationService.getAvailableProviders();
    }

    @QueryMapping
    public PaymentDashboard paymentDashboard() {
        return paymentOrchestrationService.getPaymentDashboard();
    }

    // Mutations

    @MutationMapping
    public PaymentRequest initiateMobileDeposit(@Argument MobileDepositInput input) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return paymentOrchestrationService.initiateMobileDeposit(input, username);
    }

    @MutationMapping
    public PaymentRequest initiateMobileLoanRepayment(@Argument MobileLoanRepaymentInput input) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return paymentOrchestrationService.initiateMobileLoanRepayment(input, username);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'LOAN_OFFICER')")
    public PaymentRequest initiateMobileDisbursement(@Argument MobileDisbursementInput input) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return paymentOrchestrationService.initiateMobileDisbursement(input, username);
    }

    @MutationMapping
    public PaymentRequest checkPaymentStatus(@Argument UUID paymentRequestId) {
        return paymentOrchestrationService.checkPaymentStatus(paymentRequestId);
    }

    @MutationMapping
    public PaymentRequest cancelPaymentRequest(@Argument UUID paymentRequestId) {
        return paymentOrchestrationService.cancelPaymentRequest(paymentRequestId);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public int retryExpiredPayments() {
        return paymentOrchestrationService.retryExpiredPayments();
    }
}
