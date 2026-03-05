package tz.co.iseke.service.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.co.iseke.entity.*;
import tz.co.iseke.enums.*;
import tz.co.iseke.exception.BusinessException;
import tz.co.iseke.inputs.MobileDepositInput;
import tz.co.iseke.inputs.MobileDisbursementInput;
import tz.co.iseke.inputs.MobileLoanRepaymentInput;
import tz.co.iseke.repository.LoanAccountRepository;
import tz.co.iseke.repository.PaymentRequestRepository;
import tz.co.iseke.repository.SavingsAccountRepository;
import tz.co.iseke.service.TransactionService;
import tz.co.iseke.testutil.TestDataBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOrchestrationServiceTest {

    @Mock private PaymentGatewayRegistry gatewayRegistry;
    @Mock private PaymentRequestRepository paymentRequestRepository;
    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private LoanAccountRepository loanAccountRepository;
    @Mock private TransactionService transactionService;
    @Mock private PaymentGateway mockGateway;

    @InjectMocks
    private PaymentOrchestrationService orchestrationService;

    private Member member;
    private Branch branch;
    private SavingsAccount savingsAccount;
    private LoanAccount loanAccount;

    @BeforeEach
    void setUp() {
        member = TestDataBuilder.aMember();
        branch = TestDataBuilder.aBranch();
        SavingsProduct savingsProduct = TestDataBuilder.aSavingsProduct();
        savingsAccount = TestDataBuilder.aSavingsAccount(member, savingsProduct, branch);
        LoanProduct loanProduct = TestDataBuilder.aLoanProduct(InterestMethod.FLAT);
        loanAccount = TestDataBuilder.aLoanAccount(member, loanProduct, branch);

        lenient().when(paymentRequestRepository.save(any(PaymentRequest.class)))
                .thenAnswer(inv -> {
                    PaymentRequest pr = inv.getArgument(0);
                    if (pr.getId() == null) pr.setId(UUID.randomUUID());
                    if (pr.getRequestNumber() == null) pr.setRequestNumber("PAY" + System.currentTimeMillis());
                    return pr;
                });
    }

    // ===== MOBILE DEPOSIT =====

    @Test
    void mobileDeposit_success() {
        MobileDepositInput input = new MobileDepositInput();
        input.setAccountId(savingsAccount.getId());
        input.setAmount(new BigDecimal("100000"));
        input.setProvider(PaymentProvider.MPESA);
        input.setPhoneNumber("255712345678");

        when(savingsAccountRepository.findById(savingsAccount.getId())).thenReturn(Optional.of(savingsAccount));
        when(gatewayRegistry.getGateway(PaymentProvider.MPESA)).thenReturn(mockGateway);
        when(mockGateway.isAvailable()).thenReturn(true);
        when(mockGateway.initiateCollection(any(PaymentRequest.class)))
                .thenAnswer(inv -> {
                    PaymentRequest pr = inv.getArgument(0);
                    pr.setStatus(PaymentRequestStatus.SENT);
                    return pr;
                });

        PaymentRequest result = orchestrationService.initiateMobileDeposit(input, "admin");

        assertNotNull(result);
        assertEquals(PaymentRequestStatus.SENT, result.getStatus());
    }

    @Test
    void mobileDeposit_unavailableProvider_throwsBusinessException() {
        MobileDepositInput input = new MobileDepositInput();
        input.setAccountId(savingsAccount.getId());
        input.setAmount(new BigDecimal("100000"));
        input.setProvider(PaymentProvider.MPESA);
        input.setPhoneNumber("255712345678");

        when(savingsAccountRepository.findById(savingsAccount.getId())).thenReturn(Optional.of(savingsAccount));
        when(gatewayRegistry.getGateway(PaymentProvider.MPESA)).thenReturn(mockGateway);
        when(mockGateway.isAvailable()).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> orchestrationService.initiateMobileDeposit(input, "admin"));
    }

    @Test
    void mobileDeposit_gatewayException_marksFailed() {
        MobileDepositInput input = new MobileDepositInput();
        input.setAccountId(savingsAccount.getId());
        input.setAmount(new BigDecimal("100000"));
        input.setProvider(PaymentProvider.MPESA);
        input.setPhoneNumber("255712345678");

        when(savingsAccountRepository.findById(savingsAccount.getId())).thenReturn(Optional.of(savingsAccount));
        when(gatewayRegistry.getGateway(PaymentProvider.MPESA)).thenReturn(mockGateway);
        when(mockGateway.isAvailable()).thenReturn(true);
        when(mockGateway.initiateCollection(any())).thenThrow(new RuntimeException("Network error"));

        PaymentRequest result = orchestrationService.initiateMobileDeposit(input, "admin");

        assertEquals(PaymentRequestStatus.FAILED, result.getStatus());
    }

    // ===== MOBILE LOAN REPAYMENT =====

    @Test
    void mobileLoanRepayment_success() {
        MobileLoanRepaymentInput input = new MobileLoanRepaymentInput();
        input.setLoanId(loanAccount.getId());
        input.setAmount(new BigDecimal("50000"));
        input.setProvider(PaymentProvider.TIGOPESA);
        input.setPhoneNumber("255712345678");

        when(loanAccountRepository.findById(loanAccount.getId())).thenReturn(Optional.of(loanAccount));
        when(gatewayRegistry.getGateway(PaymentProvider.TIGOPESA)).thenReturn(mockGateway);
        when(mockGateway.isAvailable()).thenReturn(true);
        when(mockGateway.initiateCollection(any(PaymentRequest.class)))
                .thenAnswer(inv -> {
                    PaymentRequest pr = inv.getArgument(0);
                    pr.setStatus(PaymentRequestStatus.SENT);
                    return pr;
                });

        PaymentRequest result = orchestrationService.initiateMobileLoanRepayment(input, "admin");

        assertNotNull(result);
        assertEquals(PaymentRequestStatus.SENT, result.getStatus());
        assertEquals("LOAN_REPAYMENT", result.getPurpose());
    }

    // ===== MOBILE DISBURSEMENT =====

    @Test
    void mobileDisbursement_success() {
        MobileDisbursementInput input = new MobileDisbursementInput();
        input.setLoanId(loanAccount.getId());
        input.setAmount(new BigDecimal("1000000"));
        input.setProvider(PaymentProvider.NMB_BANK);
        input.setPhoneNumber("255712345678");
        input.setBankAccountNumber("1234567890");

        when(loanAccountRepository.findById(loanAccount.getId())).thenReturn(Optional.of(loanAccount));
        when(gatewayRegistry.getGateway(PaymentProvider.NMB_BANK)).thenReturn(mockGateway);
        when(mockGateway.isAvailable()).thenReturn(true);
        when(mockGateway.initiateDisbursement(any(PaymentRequest.class)))
                .thenAnswer(inv -> {
                    PaymentRequest pr = inv.getArgument(0);
                    pr.setStatus(PaymentRequestStatus.SENT);
                    return pr;
                });

        PaymentRequest result = orchestrationService.initiateMobileDisbursement(input, "admin");

        assertNotNull(result);
        assertEquals(PaymentDirection.OUTBOUND, result.getDirection());
    }

    // ===== CALLBACK HANDLING =====

    @Test
    void handleCallback_depositSuccess_createsTransaction() {
        PaymentRequest request = TestDataBuilder.aPaymentRequest(PaymentProvider.MPESA, new BigDecimal("100000"));
        request.setDirection(PaymentDirection.INBOUND);
        request.setPurpose("DEPOSIT");
        request.setSavingsAccount(savingsAccount);
        request.setMember(member);
        request.setStatus(PaymentRequestStatus.CALLBACK_RECEIVED);

        when(gatewayRegistry.getGateway(PaymentProvider.MPESA)).thenReturn(mockGateway);
        when(mockGateway.processCallback(anyString())).thenReturn(request);

        PaymentRequest result = orchestrationService.handleCallback(PaymentProvider.MPESA, "{}");

        assertEquals(PaymentRequestStatus.COMPLETED, result.getStatus());
        verify(transactionService).processDeposit(any());
    }

    @Test
    void handleCallback_repaymentSuccess_createsTransaction() {
        PaymentRequest request = TestDataBuilder.aPaymentRequest(PaymentProvider.TIGOPESA, new BigDecimal("50000"));
        request.setDirection(PaymentDirection.INBOUND);
        request.setPurpose("LOAN_REPAYMENT");
        request.setLoanAccount(loanAccount);
        request.setMember(member);
        request.setStatus(PaymentRequestStatus.CALLBACK_RECEIVED);

        Transaction mockTxn = Transaction.builder().id(UUID.randomUUID()).build();

        when(gatewayRegistry.getGateway(PaymentProvider.TIGOPESA)).thenReturn(mockGateway);
        when(mockGateway.processCallback(anyString())).thenReturn(request);
        when(transactionService.processLoanRepayment(any())).thenReturn(mockTxn);

        PaymentRequest result = orchestrationService.handleCallback(PaymentProvider.TIGOPESA, "{}");

        assertEquals(PaymentRequestStatus.COMPLETED, result.getStatus());
        verify(transactionService).processLoanRepayment(any());
    }

    @Test
    void handleCallback_nullRequest_returnsNull() {
        when(gatewayRegistry.getGateway(PaymentProvider.MPESA)).thenReturn(mockGateway);
        when(mockGateway.processCallback(anyString())).thenReturn(null);

        PaymentRequest result = orchestrationService.handleCallback(PaymentProvider.MPESA, "{}");

        assertNull(result);
    }

    // ===== CANCEL =====

    @Test
    void cancelPayment_success() {
        PaymentRequest request = TestDataBuilder.aPaymentRequest(PaymentProvider.MPESA, new BigDecimal("100000"));
        request.setStatus(PaymentRequestStatus.SENT);

        when(paymentRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        PaymentRequest result = orchestrationService.cancelPaymentRequest(request.getId());

        assertEquals(PaymentRequestStatus.CANCELLED, result.getStatus());
    }

    @Test
    void cancelPayment_alreadyCompleted_throwsBusinessException() {
        PaymentRequest request = TestDataBuilder.aPaymentRequest(PaymentProvider.MPESA, new BigDecimal("100000"));
        request.setStatus(PaymentRequestStatus.COMPLETED);

        when(paymentRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThrows(BusinessException.class,
                () -> orchestrationService.cancelPaymentRequest(request.getId()));
    }

    // ===== EXPIRY =====

    @Test
    void expireStaleRequests_marksExpired() {
        PaymentRequest stale1 = TestDataBuilder.aPaymentRequest(PaymentProvider.MPESA, new BigDecimal("100000"));
        stale1.setStatus(PaymentRequestStatus.SENT);
        stale1.setExpiresAt(LocalDateTime.now().minusMinutes(5));

        PaymentRequest stale2 = TestDataBuilder.aPaymentRequest(PaymentProvider.TIGOPESA, new BigDecimal("50000"));
        stale2.setStatus(PaymentRequestStatus.SENT);
        stale2.setExpiresAt(LocalDateTime.now().minusMinutes(10));

        when(paymentRequestRepository.findByStatusAndExpiresAtBefore(eq(PaymentRequestStatus.SENT), any()))
                .thenReturn(List.of(stale1, stale2));

        orchestrationService.expireStaleRequests();

        assertEquals(PaymentRequestStatus.EXPIRED, stale1.getStatus());
        assertEquals(PaymentRequestStatus.EXPIRED, stale2.getStatus());
        verify(paymentRequestRepository, times(2)).save(any(PaymentRequest.class));
    }
}
