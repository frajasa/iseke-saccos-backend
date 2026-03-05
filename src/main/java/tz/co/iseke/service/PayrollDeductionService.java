package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.entity.*;
import tz.co.iseke.enums.PaymentMethod;
import tz.co.iseke.exception.BusinessException;
import tz.co.iseke.exception.ResourceNotFoundException;
import tz.co.iseke.inputs.DepositInput;
import tz.co.iseke.inputs.LoanRepaymentInput;
import tz.co.iseke.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollDeductionService {

    private final PayrollDeductionRepository payrollDeductionRepository;
    private final PayrollDeductionBatchRepository payrollDeductionBatchRepository;
    private final EmployerRepository employerRepository;
    private final MemberRepository memberRepository;
    private final TransactionService transactionService;

    @Transactional
    public PayrollDeductionBatch processPayrollBatch(UUID employerId, String period, String processedBy) {
        Employer employer = employerRepository.findById(employerId)
                .orElseThrow(() -> new ResourceNotFoundException("Employer not found"));

        // Check if batch already processed
        if (payrollDeductionBatchRepository.findByEmployerIdAndPeriod(employerId, period).isPresent()) {
            throw new BusinessException("Payroll batch already exists for " + employer.getEmployerName() + " period " + period);
        }

        List<PayrollDeduction> activeDeductions = payrollDeductionRepository
                .findByEmployerIdAndIsActiveTrue(employerId);

        PayrollDeductionBatch batch = PayrollDeductionBatch.builder()
                .employer(employer)
                .period(period)
                .totalDeductions(activeDeductions.size())
                .status("PROCESSING")
                .processedBy(processedBy)
                .build();
        batch = payrollDeductionBatchRepository.save(batch);

        int successful = 0;
        int failed = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PayrollDeduction deduction : activeDeductions) {
            try {
                processIndividualDeduction(deduction);
                successful++;
                totalAmount = totalAmount.add(deduction.getAmount());
            } catch (Exception e) {
                failed++;
                log.error("Failed to process deduction {} for member {}: {}",
                        deduction.getId(), deduction.getMember().getMemberNumber(), e.getMessage());
            }
        }

        batch.setSuccessfulDeductions(successful);
        batch.setFailedDeductions(failed);
        batch.setTotalAmount(totalAmount);
        batch.setStatus("COMPLETED");
        batch.setProcessedAt(LocalDateTime.now());

        return payrollDeductionBatchRepository.save(batch);
    }

    @Transactional
    public void processIndividualDeduction(PayrollDeduction deduction) {
        String deductionType = deduction.getDeductionType();

        if ("SAVINGS".equals(deductionType) || "SHARES".equals(deductionType)) {
            if (deduction.getSavingsAccount() == null) {
                throw new BusinessException("No savings account linked to deduction");
            }
            DepositInput depositInput = DepositInput.builder()
                    .accountId(deduction.getSavingsAccount().getId())
                    .amount(deduction.getAmount())
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .referenceNumber("PAYROLL-" + deduction.getId())
                    .description("Payroll deduction: " + deductionType)
                    .build();
            transactionService.processDeposit(depositInput);
        } else if ("LOAN_REPAYMENT".equals(deductionType)) {
            if (deduction.getLoanAccount() == null) {
                throw new BusinessException("No loan account linked to deduction");
            }
            LoanRepaymentInput repaymentInput = LoanRepaymentInput.builder()
                    .loanId(deduction.getLoanAccount().getId())
                    .amount(deduction.getAmount())
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .referenceNumber("PAYROLL-" + deduction.getId())
                    .build();
            transactionService.processLoanRepayment(repaymentInput);
        } else {
            throw new BusinessException("Unknown deduction type: " + deductionType);
        }

        log.info("Processed payroll deduction {} for member {} amount {}",
                deduction.getDeductionType(), deduction.getMember().getMemberNumber(), deduction.getAmount());
    }

    @Transactional
    public PayrollDeduction setupPayrollDeduction(UUID memberId, UUID employerId, String deductionType,
                                                    UUID savingsAccountId, UUID loanAccountId,
                                                    BigDecimal amount, String description) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        Employer employer = employerRepository.findById(employerId)
                .orElseThrow(() -> new ResourceNotFoundException("Employer not found"));

        PayrollDeduction deduction = PayrollDeduction.builder()
                .member(member)
                .employer(employer)
                .deductionType(deductionType)
                .amount(amount)
                .description(description)
                .startDate(LocalDateTime.now())
                .build();

        if (savingsAccountId != null) {
            deduction.setSavingsAccount(
                    new SavingsAccount() {{ setId(savingsAccountId); }}
            );
        }
        if (loanAccountId != null) {
            deduction.setLoanAccount(
                    new LoanAccount() {{ setId(loanAccountId); }}
            );
        }

        return payrollDeductionRepository.save(deduction);
    }

    public List<PayrollDeductionBatch> getEmployerBatches(UUID employerId) {
        return payrollDeductionBatchRepository.findByEmployerId(employerId);
    }
}
