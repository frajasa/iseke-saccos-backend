package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.inputs.AddCollateralInput;
import tz.co.iseke.inputs.AddGuarantorInput;
import tz.co.iseke.inputs.LoanApplicationInput;
import tz.co.iseke.entity.*;
import tz.co.iseke.enums.*;
import tz.co.iseke.exception.BusinessException;
import tz.co.iseke.exception.ResourceNotFoundException;
import tz.co.iseke.repository.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanAccountService {

    private final LoanAccountRepository loanAccountRepository;
    private final MemberRepository memberRepository;
    private final LoanProductRepository loanProductRepository;
    private final BranchRepository branchRepository;
    private final GuarantorRepository guarantorRepository;
    private final CollateralRepository collateralRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final @Lazy AccountingService accountingService;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;
    private final ChartOfAccountsRepository chartRepository;

    public Page<LoanAccount> findAll(Pageable pageable) {
        return loanAccountRepository.findAll(pageable);
    }

    public Page<LoanAccount> findByStatus(LoanStatus status, Pageable pageable) {
        return loanAccountRepository.findByStatus(status, pageable);
    }

    public LoanAccount findById(UUID id) {
        return loanAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan account not found with id: " + id));
    }

    public LoanAccount findByLoanNumber(String loanNumber) {
        return loanAccountRepository.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with number: " + loanNumber));
    }

    public List<LoanAccount> findByMemberId(UUID memberId) {
        return loanAccountRepository.findByMemberId(memberId);
    }

    public List<LoanRepaymentSchedule> getRepaymentSchedule(UUID loanId) {
        return scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId);
    }

    public Page<LoanRepaymentSchedule> getRepaymentSchedulePaged(UUID loanId, Pageable pageable) {
        return scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId, pageable);
    }

    /**
     * Generate repayment schedules for all existing DISBURSED/ACTIVE loans that don't have one.
     * Marks already-paid installments based on totalPaid on the loan.
     */
    public int generateSchedulesForExistingLoans() {
        List<LoanAccount> loans = loanAccountRepository.findAll().stream()
                .filter(l -> List.of(LoanStatus.DISBURSED, LoanStatus.ACTIVE).contains(l.getStatus()))
                .toList();

        int generated = 0;
        for (LoanAccount loan : loans) {
            List<LoanRepaymentSchedule> existing = scheduleRepository.findByLoanIdOrderByInstallmentNumber(loan.getId());
            if (!existing.isEmpty()) {
                continue; // already has schedule
            }

            // Generate the schedule
            generateRepaymentSchedule(loan);

            // Mark installments as paid based on totalPaid
            BigDecimal totalPaid = loan.getTotalPaid() != null ? loan.getTotalPaid() : BigDecimal.ZERO;
            if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                List<LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumber(loan.getId());
                BigDecimal remaining = totalPaid;
                LocalDate today = LocalDate.now();

                for (LoanRepaymentSchedule schedule : schedules) {
                    if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                        // Mark future unpaid schedules that are past due as OVERDUE
                        if (schedule.getDueDate().isBefore(today)) {
                            schedule.setStatus(ScheduleStatus.OVERDUE);
                            scheduleRepository.save(schedule);
                        }
                        continue;
                    }

                    BigDecimal installmentTotal = schedule.getTotalDue();

                    if (remaining.compareTo(installmentTotal) >= 0) {
                        // Fully paid installment
                        schedule.setInterestPaid(schedule.getInterestDue());
                        schedule.setPrincipalPaid(schedule.getPrincipalDue());
                        schedule.setTotalPaid(installmentTotal);
                        schedule.setStatus(ScheduleStatus.PAID);
                        schedule.setPaymentDate(schedule.getDueDate());
                        remaining = remaining.subtract(installmentTotal);
                    } else {
                        // Partially paid — allocate interest first, then principal
                        BigDecimal interestPayment = remaining.min(schedule.getInterestDue());
                        remaining = remaining.subtract(interestPayment);
                        BigDecimal principalPayment = remaining.min(schedule.getPrincipalDue());
                        remaining = remaining.subtract(principalPayment);

                        schedule.setInterestPaid(interestPayment);
                        schedule.setPrincipalPaid(principalPayment);
                        schedule.setTotalPaid(interestPayment.add(principalPayment));
                        schedule.setStatus(schedule.getDueDate().isBefore(today) ? ScheduleStatus.OVERDUE : ScheduleStatus.PARTIAL);
                    }
                    scheduleRepository.save(schedule);
                }
            }

            generated++;
        }

        return generated;
    }

    public LoanAccount applyForLoan(LoanApplicationInput input) {
        Member member = memberRepository.findById(input.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        LoanProduct product = loanProductRepository.findById(input.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found"));

        // Validate loan amount
        if (input.getRequestedAmount().compareTo(product.getMinimumAmount()) < 0 ||
                input.getRequestedAmount().compareTo(product.getMaximumAmount()) > 0) {
            throw new BusinessException("Loan amount must be between " +
                    product.getMinimumAmount() + " and " + product.getMaximumAmount());
        }

        // Validate term
        if (input.getTermMonths() < product.getMinimumTermMonths() ||
                input.getTermMonths() > product.getMaximumTermMonths()) {
            throw new BusinessException("Loan term must be between " +
                    product.getMinimumTermMonths() + " and " + product.getMaximumTermMonths() + " months");
        }

        Branch branch = null;
        if (input.getBranchId() != null) {
            branch = branchRepository.findById(input.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        }

        String loanNumber = generateLoanNumber();

        LoanAccount loan = LoanAccount.builder()
                .loanNumber(loanNumber)
                .member(member)
                .product(product)
                .branch(branch)
                .applicationDate(LocalDate.now())
                .principalAmount(input.getRequestedAmount())
                .interestRate(product.getInterestRate())
                .termMonths(input.getTermMonths())
                .repaymentFrequency(product.getRepaymentFrequency())
                .loanOfficer(input.getLoanOfficer())
                .purpose(input.getPurpose())
                .status(LoanStatus.APPLIED)
                .build();

        LoanAccount savedLoan = loanAccountRepository.save(loan);
        auditService.logAction("LOAN_APPLIED", "LoanAccount", savedLoan.getId(),
                null, "Amount: " + input.getRequestedAmount());
        return savedLoan;
    }

    public LoanAccount approveLoan(UUID id, BigDecimal approvedAmount) {
        LoanAccount loan = findById(id);

        if (loan.getStatus() != LoanStatus.APPLIED) {
            throw new BusinessException("Only applied loans can be approved");
        }

        if (approvedAmount != null) {
            loan.setPrincipalAmount(approvedAmount);
        }

        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovalDate(LocalDate.now());
        loan.setUpdatedAt(LocalDateTime.now());

        LoanAccount approved = loanAccountRepository.save(loan);
        auditService.logAction("LOAN_APPROVED", "LoanAccount", approved.getId(),
                "Status: APPLIED", "Status: APPROVED");
        return approved;
    }

    public LoanAccount disburseLoan(UUID id, LocalDate disbursementDate) {
        LoanAccount loan = findById(id);

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new BusinessException("Only approved loans can be disbursed");
        }

        // Check guarantors if required
        if (loan.getProduct().getRequiresGuarantors()) {
            List<Guarantor> guarantors = guarantorRepository.findActiveGuarantorsByLoanId(id);
            if (guarantors.size() < loan.getProduct().getMinimumGuarantors()) {
                throw new BusinessException("Loan requires at least " +
                        loan.getProduct().getMinimumGuarantors() + " guarantors");
            }
        }

        // Check collateral if required
        if (loan.getProduct().getRequiresCollateral()) {
            List<Collateral> collaterals = collateralRepository.findActiveCollateralByLoanId(id);
            if (collaterals.isEmpty()) {
                throw new BusinessException("Loan requires collateral");
            }
        }

        LocalDate disbDate = disbursementDate != null ? disbursementDate : LocalDate.now();

        loan.setDisbursementDate(disbDate);
        loan.setStatus(LoanStatus.DISBURSED);
        loan.setOutstandingPrincipal(loan.getPrincipalAmount());
        loan.setUpdatedAt(LocalDateTime.now());

        // Generate repayment schedule
        generateRepaymentSchedule(loan);

        // Calculate maturity date
        loan.setMaturityDate(disbDate.plusMonths(loan.getTermMonths()));
        loan.setNextPaymentDate(getNextPaymentDate(disbDate, loan.getRepaymentFrequency()));

        loan = loanAccountRepository.save(loan);

        // Create disbursement transaction record
        String transactionNumber = generateTransactionNumber();
        Transaction disbursementTransaction = Transaction.builder()
                .transactionNumber(transactionNumber)
                .transactionDate(disbDate)
                .transactionType(TransactionType.LOAN_DISBURSEMENT)
                .loanAccount(loan)
                .member(loan.getMember())
                .amount(loan.getPrincipalAmount())
                .description("Loan disbursement - " + loan.getLoanNumber())
                .branch(loan.getBranch())
                .status(TransactionStatus.COMPLETED)
                .build();

        disbursementTransaction = transactionRepository.save(disbursementTransaction);

        // Post accounting entries for loan disbursement
        // DEBIT: Loans Receivable (Asset) - money lent out
        // CREDIT: Cash/Bank (Asset) - money going out
        if (loan.getProduct().getLoanReceivableAccount() != null &&
            loan.getProduct().getCashAccount() != null) {
            accountingService.postToGeneralLedger(
                disbursementTransaction,
                loan.getProduct().getLoanReceivableAccount(),
                loan.getProduct().getCashAccount(),
                loan.getPrincipalAmount()
            );
        }

        auditService.logAction("LOAN_DISBURSED", "LoanAccount", loan.getId(),
                "Status: APPROVED", "Status: DISBURSED, Amount: " + loan.getPrincipalAmount());

        return loan;
    }

    public Guarantor addGuarantor(AddGuarantorInput input) {
        LoanAccount loan = findById(input.getLoanId());

        Member guarantorMember = null;
        if (input.getGuarantorMemberId() != null) {
            guarantorMember = memberRepository.findById(input.getGuarantorMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Guarantor member not found"));
        }

        Guarantor guarantor = Guarantor.builder()
                .loanAccount(loan)
                .guarantorMember(guarantorMember)
                .guarantorName(input.getGuarantorName())
                .guarantorNationalId(input.getGuarantorNationalId())
                .guarantorPhone(input.getGuarantorPhone())
                .guaranteedAmount(input.getGuaranteedAmount())
                .relationship(input.getRelationship())
                .status(GuarantorStatus.ACTIVE)
                .build();

        return guarantorRepository.save(guarantor);
    }

    public Collateral addCollateral(AddCollateralInput input) {
        LoanAccount loan = findById(input.getLoanId());

        Collateral collateral = Collateral.builder()
                .loanAccount(loan)
                .collateralType(input.getCollateralType())
                .description(input.getDescription())
                .estimatedValue(input.getEstimatedValue())
                .registrationNumber(input.getRegistrationNumber())
                .location(input.getLocation())
                .status(CollateralStatus.ACTIVE)
                .build();

        return collateralRepository.save(collateral);
    }

    public LoanAccount writeOffLoan(UUID loanId, String reason) {
        LoanAccount loan = findById(loanId);

        if (!List.of(LoanStatus.DISBURSED, LoanStatus.ACTIVE).contains(loan.getStatus())) {
            throw new BusinessException("Only active/disbursed loans can be written off");
        }

        BigDecimal outstandingInterest = loan.getOutstandingInterest() != null ? loan.getOutstandingInterest() : BigDecimal.ZERO;
        BigDecimal outstandingPenalties = loan.getOutstandingPenalties() != null ? loan.getOutstandingPenalties() : BigDecimal.ZERO;
        BigDecimal outstandingTotal = loan.getOutstandingPrincipal()
                .add(outstandingInterest)
                .add(outstandingPenalties);

        String currentUser = null;
        try {
            currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception ignored) {}

        // Post GL: DEBIT Loan Loss Expense, CREDIT Loans Receivable
        ChartOfAccounts loanLossExpense = chartRepository.findByAccountCode("5301").orElse(null);
        if (loanLossExpense != null && loan.getProduct().getLoanReceivableAccount() != null) {
            accountingService.postToGeneralLedgerDirect(
                    LocalDate.now(),
                    loanLossExpense,
                    loan.getProduct().getLoanReceivableAccount(),
                    loan.getOutstandingPrincipal(),
                    "Loan write-off principal - " + loan.getLoanNumber(),
                    "WO" + System.currentTimeMillis(),
                    loan.getBranch()
            );
        }

        // Reverse accrued interest: DEBIT Interest Income, CREDIT Interest Receivable
        if (outstandingInterest.compareTo(BigDecimal.ZERO) > 0) {
            ChartOfAccounts interestReceivable = chartRepository.findByAccountCode("1203").orElse(null);
            ChartOfAccounts interestIncome = loan.getProduct().getInterestIncomeAccount();
            if (interestIncome != null && interestReceivable != null) {
                accountingService.postToGeneralLedgerDirect(
                        LocalDate.now(),
                        interestIncome,
                        interestReceivable,
                        loan.getOutstandingInterest(),
                        "Loan write-off interest reversal - " + loan.getLoanNumber(),
                        "WOI" + System.currentTimeMillis(),
                        loan.getBranch()
                );
            }
        }

        loan.setStatus(LoanStatus.WRITTEN_OFF);
        loan.setWriteOffReason(reason);
        loan.setWriteOffDate(LocalDate.now());
        loan.setWrittenOffBy(currentUser);
        loan.setUpdatedAt(LocalDateTime.now());
        loan = loanAccountRepository.save(loan);

        auditService.logAction("LOAN_WRITTEN_OFF", "LoanAccount", loan.getId(),
                "Outstanding: " + outstandingTotal, "Reason: " + reason);

        return loan;
    }

    public LoanAccount refinanceLoan(UUID loanId, Integer newTermMonths, BigDecimal newInterestRate, String reason) {
        LoanAccount loan = findById(loanId);

        if (!List.of(LoanStatus.DISBURSED, LoanStatus.ACTIVE).contains(loan.getStatus())) {
            throw new BusinessException("Only active/disbursed loans can be refinanced");
        }

        String oldTerms = String.format("Term: %d months, Rate: %s", loan.getTermMonths(), loan.getInterestRate());

        // Cancel existing unpaid schedules (not PAID - they were never actually paid)
        List<LoanRepaymentSchedule> existingSchedules = scheduleRepository.findByLoanIdOrderByInstallmentNumber(loan.getId());
        for (LoanRepaymentSchedule schedule : existingSchedules) {
            if (schedule.getStatus() == ScheduleStatus.PENDING || schedule.getStatus() == ScheduleStatus.OVERDUE) {
                schedule.setStatus(ScheduleStatus.CANCELLED);
                scheduleRepository.save(schedule);
            }
        }

        // Update loan terms
        loan.setTermMonths(newTermMonths);
        loan.setInterestRate(newInterestRate);
        loan.setPrincipalAmount(loan.getOutstandingPrincipal());
        loan.setDisbursementDate(LocalDate.now());
        loan.setMaturityDate(LocalDate.now().plusMonths(newTermMonths));
        loan.setNextPaymentDate(getNextPaymentDate(LocalDate.now(), loan.getRepaymentFrequency()));
        loan.setDaysInArrears(0);
        loan.setOutstandingPenalties(BigDecimal.ZERO);
        loan.setUpdatedAt(LocalDateTime.now());

        // Generate new repayment schedule
        generateRepaymentSchedule(loan);

        loan = loanAccountRepository.save(loan);

        String newTerms = String.format("Term: %d months, Rate: %s, Reason: %s", newTermMonths, newInterestRate, reason);
        auditService.logAction("LOAN_REFINANCED", "LoanAccount", loan.getId(), oldTerms, newTerms);

        return loan;
    }

    private void generateRepaymentSchedule(LoanAccount loan) {
        BigDecimal principal = loan.getPrincipalAmount();
        BigDecimal monthlyRate = loan.getInterestRate().divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
        int termMonths = loan.getTermMonths();

        LocalDate paymentDate = getNextPaymentDate(loan.getDisbursementDate(), loan.getRepaymentFrequency());

        if (loan.getProduct().getInterestMethod() == InterestMethod.DECLINING_BALANCE) {
            // Calculate monthly payment using annuity formula
            BigDecimal monthlyPayment = calculateMonthlyPayment(principal, monthlyRate, termMonths);
            BigDecimal remainingPrincipal = principal;

            for (int i = 1; i <= termMonths; i++) {
                BigDecimal interestDue = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal principalDue = monthlyPayment.subtract(interestDue);

                if (i == termMonths) {
                    principalDue = remainingPrincipal; // Adjust last payment for rounding
                }

                LoanRepaymentSchedule schedule = LoanRepaymentSchedule.builder()
                        .loan(loan)
                        .installmentNumber(i)
                        .dueDate(paymentDate)
                        .principalDue(principalDue)
                        .interestDue(interestDue)
                        .totalDue(principalDue.add(interestDue))
                        .status(ScheduleStatus.PENDING)
                        .build();

                scheduleRepository.save(schedule);

                remainingPrincipal = remainingPrincipal.subtract(principalDue);
                paymentDate = getNextPaymentDate(paymentDate, loan.getRepaymentFrequency());
            }
        } else if (loan.getProduct().getInterestMethod() == InterestMethod.REDUCING_BALANCE) {
            // Equal principal payments
            BigDecimal principalDue = principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
            BigDecimal remainingPrincipal = principal;

            for (int i = 1; i <= termMonths; i++) {
                BigDecimal interestDue = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

                if (i == termMonths) {
                    principalDue = remainingPrincipal; // Adjust last payment
                }

                LoanRepaymentSchedule schedule = LoanRepaymentSchedule.builder()
                        .loan(loan)
                        .installmentNumber(i)
                        .dueDate(paymentDate)
                        .principalDue(principalDue)
                        .interestDue(interestDue)
                        .totalDue(principalDue.add(interestDue))
                        .status(ScheduleStatus.PENDING)
                        .build();

                scheduleRepository.save(schedule);

                remainingPrincipal = remainingPrincipal.subtract(principalDue);
                paymentDate = getNextPaymentDate(paymentDate, loan.getRepaymentFrequency());
            }
        } else {
            // Flat rate
            BigDecimal totalInterest = principal.multiply(loan.getInterestRate())
                    .multiply(BigDecimal.valueOf(termMonths))
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

            BigDecimal totalAmount = principal.add(totalInterest);
            BigDecimal monthlyPayment = totalAmount.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
            BigDecimal principalDue = principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
            BigDecimal interestDue = totalInterest.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);

            for (int i = 1; i <= termMonths; i++) {
                LoanRepaymentSchedule schedule = LoanRepaymentSchedule.builder()
                        .loan(loan)
                        .installmentNumber(i)
                        .dueDate(paymentDate)
                        .principalDue(principalDue)
                        .interestDue(interestDue)
                        .totalDue(monthlyPayment)
                        .status(ScheduleStatus.PENDING)
                        .build();

                scheduleRepository.save(schedule);
                paymentDate = getNextPaymentDate(paymentDate, loan.getRepaymentFrequency());
            }
        }
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal rate, int months) {
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }

        BigDecimal onePlusRate = BigDecimal.ONE.add(rate);
        BigDecimal power = onePlusRate.pow(months);
        BigDecimal numerator = principal.multiply(rate).multiply(power);
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private LocalDate getNextPaymentDate(LocalDate fromDate, String frequency) {
        return switch (frequency.toUpperCase()) {
            case "WEEKLY" -> fromDate.plusWeeks(1);
            case "BIWEEKLY" -> fromDate.plusWeeks(2);
            case "MONTHLY" -> fromDate.plusMonths(1);
            case "QUARTERLY" -> fromDate.plusMonths(3);
            default -> fromDate.plusMonths(1);
        };
    }

    private String generateLoanNumber() {
        String datePrefix = LocalDate.now().toString().replace("-", "");
        return String.format("LN%s%s", datePrefix, UUID.randomUUID().toString().substring(0, 6).toUpperCase());
    }

    private String generateTransactionNumber() {
        String datePrefix = LocalDate.now().toString().replace("-", "");
        return String.format("TXN%s%s", datePrefix, UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
