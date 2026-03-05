package tz.co.iseke.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import tz.co.iseke.dto.EssDashboard;
import tz.co.iseke.entity.*;
import tz.co.iseke.service.EssService;
import tz.co.iseke.service.PayrollDeductionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class EssResolver {

    private final EssService essService;
    private final PayrollDeductionService payrollDeductionService;

    // --- Member (ESS) Queries ---

    @QueryMapping
    @PreAuthorize("hasRole('MEMBER')")
    public EssDashboard essDashboard() {
        UUID memberId = getCurrentMemberId();
        return essService.getMemberDashboard(memberId);
    }

    @QueryMapping
    @PreAuthorize("hasRole('MEMBER')")
    public List<PayrollDeduction> essPayrollDeductions() {
        UUID memberId = getCurrentMemberId();
        return essService.getPayrollDeductions(memberId);
    }

    @QueryMapping
    @PreAuthorize("hasRole('MEMBER')")
    public List<EssServiceRequest> essServiceRequests() {
        UUID memberId = getCurrentMemberId();
        return essService.getMemberRequests(memberId);
    }

    // --- Member (ESS) Mutations ---

    @MutationMapping
    @PreAuthorize("hasRole('MEMBER')")
    public EssServiceRequest essApplyForLoan(@Argument BigDecimal amount, @Argument int termMonths,
                                               @Argument String purpose, @Argument UUID productId) {
        UUID memberId = getCurrentMemberId();
        return essService.submitLoanApplication(memberId, amount, termMonths, purpose, productId);
    }

    @MutationMapping
    @PreAuthorize("hasRole('MEMBER')")
    public EssServiceRequest essRequestWithdrawal(@Argument UUID accountId, @Argument BigDecimal amount,
                                                    @Argument String reason) {
        UUID memberId = getCurrentMemberId();
        return essService.submitWithdrawalRequest(memberId, accountId, amount, reason);
    }

    // --- Admin Mutations ---

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Employer createEmployer(@Argument String employerCode, @Argument String employerName,
                                    @Argument String contactPerson, @Argument String phoneNumber,
                                    @Argument String email, @Argument String address,
                                    @Argument String tinNumber, @Argument Integer payrollCutoffDay) {
        return essService.createEmployer(employerCode, employerName, contactPerson,
                phoneNumber, email, address, tinNumber, payrollCutoffDay);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public PayrollDeductionBatch processPayrollBatch(@Argument UUID employerId, @Argument String period) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return payrollDeductionService.processPayrollBatch(employerId, period, username);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public PayrollDeduction setupPayrollDeduction(@Argument UUID memberId, @Argument UUID employerId,
                                                    @Argument String deductionType,
                                                    @Argument UUID savingsAccountId,
                                                    @Argument UUID loanAccountId,
                                                    @Argument BigDecimal amount,
                                                    @Argument String description) {
        return payrollDeductionService.setupPayrollDeduction(memberId, employerId, deductionType,
                savingsAccountId, loanAccountId, amount, description);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public EssServiceRequest reviewEssRequest(@Argument UUID requestId, @Argument String status,
                                                @Argument String reviewNotes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return essService.reviewRequestByUsername(requestId, status, reviewNotes, username);
    }

    // --- Admin Queries ---

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<Employer> employers() {
        return essService.getActiveEmployers();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Employer employer(@Argument UUID id) {
        return essService.getEmployer(id);
    }

    // --- Helper ---

    private UUID getCurrentMemberId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return essService.getMemberIdForUsername(username);
    }
}
