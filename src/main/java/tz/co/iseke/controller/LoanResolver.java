package tz.co.iseke.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import tz.co.iseke.entity.*;
import tz.co.iseke.enums.LoanStatus;
import tz.co.iseke.inputs.*;
import tz.co.iseke.service.LoanAccountService;
import tz.co.iseke.service.LoanProductService;
import tz.co.iseke.service.TransactionService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class LoanResolver {

    private final LoanAccountService loanAccountService;
    private final LoanProductService loanProductService;
    private final TransactionService transactionService;

    @QueryMapping
    public Page<LoanAccount> loanAccounts(@Argument Integer page,
                                          @Argument Integer size,
                                          @Argument LoanStatus status) {
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        if (status != null) {
            return loanAccountService.findByStatus(status, pageable);
        }
        return loanAccountService.findAll(pageable);
    }

    @QueryMapping
    public LoanAccount loanAccount(@Argument UUID id) {
        return loanAccountService.findById(id);
    }

    @QueryMapping
    public LoanAccount loanAccountByNumber(@Argument String loanNumber) {
        return loanAccountService.findByLoanNumber(loanNumber);
    }

    @QueryMapping
    public List<LoanAccount> memberLoanAccounts(@Argument UUID memberId) {
        return loanAccountService.findByMemberId(memberId);
    }

    @QueryMapping
    public List<LoanProduct> loanProducts() {
        return loanProductService.findAllActive();
    }

    @QueryMapping
    public List<Transaction> loanTransactions(@Argument UUID loanId) {
        return transactionService.findLoanTransactions(loanId);
    }

    @QueryMapping
    public Page<LoanRepaymentSchedule> loanRepaymentSchedule(@Argument UUID loanId,
                                                              @Argument Integer page,
                                                              @Argument Integer size) {
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 10;
        return loanAccountService.getRepaymentSchedulePaged(loanId, PageRequest.of(pageNumber, pageSize));
    }

    @MutationMapping
    public LoanAccount applyForLoan(@Argument LoanApplicationInput input) {
        return loanAccountService.applyForLoan(input);
    }

    @MutationMapping
    public LoanAccount approveLoan(@Argument UUID id, @Argument java.math.BigDecimal approvedAmount) {
        return loanAccountService.approveLoan(id, approvedAmount);
    }

    @MutationMapping
    public LoanAccount disburseLoan(@Argument UUID id, @Argument LocalDate disbursementDate) {
        return loanAccountService.disburseLoan(id, disbursementDate);
    }

    @MutationMapping
    public Transaction repayLoan(@Argument LoanRepaymentInput input) {
        return transactionService.processLoanRepayment(input);
    }

    @MutationMapping
    public Guarantor addGuarantor(@Argument AddGuarantorInput input) {
        return loanAccountService.addGuarantor(input);
    }

    @MutationMapping
    public Collateral addCollateral(@Argument AddCollateralInput input) {
        return loanAccountService.addCollateral(input);
    }

    @MutationMapping
    public int generateMissingRepaymentSchedules() {
        return loanAccountService.generateSchedulesForExistingLoans();
    }

    @MutationMapping
    public LoanAccount writeOffLoan(@Argument UUID loanId, @Argument String reason) {
        return loanAccountService.writeOffLoan(loanId, reason);
    }

    @MutationMapping
    public LoanAccount refinanceLoan(@Argument UUID loanId, @Argument Integer newTermMonths,
                                      @Argument java.math.BigDecimal newInterestRate, @Argument String reason) {
        return loanAccountService.refinanceLoan(loanId, newTermMonths, newInterestRate, reason);
    }

    @MutationMapping
    public LoanProduct createLoanProduct(@Argument CreateLoanProductInput input) {
        return loanProductService.createProduct(input);
    }
    @MutationMapping
    public LoanProduct updateLoanProduct(@Argument UUID id,@Argument UpdateLoanProductInput input) {
        return loanProductService.updateProduct(id, input);
    }
}