package tz.co.iseke.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.entity.PaymentRequest;
import tz.co.iseke.entity.Transaction;
import tz.co.iseke.enums.PaymentDirection;
import tz.co.iseke.enums.PaymentMethod;
import tz.co.iseke.enums.PaymentProvider;
import tz.co.iseke.inputs.DepositInput;
import tz.co.iseke.inputs.LoanRepaymentInput;
import tz.co.iseke.service.TransactionService;

/**
 * Handles payment completion in an isolated transaction (REQUIRES_NEW).
 * This ensures that if the financial transaction fails and rolls back,
 * it does not affect the caller's transaction that updates payment status.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletionService {

    private final TransactionService transactionService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction completePayment(PaymentRequest request) {
        if (request.getDirection() == PaymentDirection.INBOUND) {
            if ("DEPOSIT".equals(request.getPurpose()) && request.getSavingsAccount() != null) {
                DepositInput depositInput = new DepositInput();
                depositInput.setAccountId(request.getSavingsAccount().getId());
                depositInput.setAmount(request.getAmount());
                depositInput.setPaymentMethod(mapProviderToPaymentMethod(request.getProvider()));
                depositInput.setReferenceNumber(request.getProviderReference());
                depositInput.setDescription("Mobile deposit via " + request.getProvider());
                return transactionService.processDeposit(depositInput);
            } else if ("LOAN_REPAYMENT".equals(request.getPurpose()) && request.getLoanAccount() != null) {
                LoanRepaymentInput repaymentInput = new LoanRepaymentInput();
                repaymentInput.setLoanId(request.getLoanAccount().getId());
                repaymentInput.setAmount(request.getAmount());
                repaymentInput.setPaymentMethod(mapProviderToPaymentMethod(request.getProvider()));
                repaymentInput.setReferenceNumber(request.getProviderReference());
                return transactionService.processLoanRepayment(repaymentInput);
            }
        }
        return null;
    }

    private PaymentMethod mapProviderToPaymentMethod(PaymentProvider provider) {
        return switch (provider) {
            case MPESA -> PaymentMethod.MPESA;
            case TIGOPESA -> PaymentMethod.TIGOPESA;
            case NMB_BANK -> PaymentMethod.NMB_BANK;
            default -> PaymentMethod.MOBILE_MONEY;
        };
    }
}
