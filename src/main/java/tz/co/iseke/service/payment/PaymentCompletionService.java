package tz.co.iseke.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.entity.Transaction;
import tz.co.iseke.enums.PaymentDirection;
import tz.co.iseke.enums.PaymentMethod;
import tz.co.iseke.enums.PaymentProvider;
import tz.co.iseke.inputs.DepositInput;
import tz.co.iseke.inputs.LoanRepaymentInput;
import tz.co.iseke.service.TransactionService;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Handles payment completion in an isolated transaction (REQUIRES_NEW).
 * This ensures that if the financial transaction fails and rolls back,
 * it does not affect the caller's transaction that updates payment status.
 * Uses primitive values instead of entity references to avoid lazy-loading issues
 * across transaction boundaries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletionService {

    private final TransactionService transactionService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction completePayment(PaymentDirection direction, String purpose,
                                        UUID savingsAccountId, UUID loanAccountId,
                                        BigDecimal amount, PaymentProvider provider,
                                        String providerReference) {
        if (direction == PaymentDirection.INBOUND) {
            if ("DEPOSIT".equals(purpose) && savingsAccountId != null) {
                DepositInput depositInput = new DepositInput();
                depositInput.setAccountId(savingsAccountId);
                depositInput.setAmount(amount);
                depositInput.setPaymentMethod(mapProviderToPaymentMethod(provider));
                depositInput.setReferenceNumber(providerReference);
                depositInput.setDescription("Mobile deposit via " + provider);
                return transactionService.processDeposit(depositInput);
            } else if ("LOAN_REPAYMENT".equals(purpose) && loanAccountId != null) {
                LoanRepaymentInput repaymentInput = new LoanRepaymentInput();
                repaymentInput.setLoanId(loanAccountId);
                repaymentInput.setAmount(amount);
                repaymentInput.setPaymentMethod(mapProviderToPaymentMethod(provider));
                repaymentInput.setReferenceNumber(providerReference);
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
