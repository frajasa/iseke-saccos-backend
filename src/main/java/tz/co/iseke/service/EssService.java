package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.dto.EssDashboard;
import tz.co.iseke.entity.*;
import tz.co.iseke.enums.AccountStatus;
import tz.co.iseke.enums.LoanStatus;
import tz.co.iseke.exception.BusinessException;
import tz.co.iseke.exception.ResourceNotFoundException;
import tz.co.iseke.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EssService {

    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final PayrollDeductionRepository payrollDeductionRepository;
    private final EssServiceRequestRepository essServiceRequestRepository;
    private final EmployerRepository employerRepository;
    private final UserRepository userRepository;

    public EssDashboard getMemberDashboard(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        List<SavingsAccount> savingsAccounts = savingsAccountRepository.findByMemberId(memberId);
        List<LoanAccount> loanAccounts = loanAccountRepository.findByMemberId(memberId);
        List<PayrollDeduction> deductions = payrollDeductionRepository.findByMemberIdAndIsActiveTrue(memberId);
        List<EssServiceRequest> recentRequests = essServiceRequestRepository.findByMemberId(memberId);

        BigDecimal totalSavings = savingsAccounts.stream()
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                .map(SavingsAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLoanOutstanding = loanAccounts.stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE || l.getStatus() == LoanStatus.DISBURSED)
                .map(LoanAccount::getOutstandingPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeLoans = loanAccounts.stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE || l.getStatus() == LoanStatus.DISBURSED)
                .count();

        long activeSavings = savingsAccounts.stream()
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                .count();

        BigDecimal monthlyDeductions = deductions.stream()
                .map(PayrollDeduction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String employerName = member.getEmployerEntity() != null
                ? member.getEmployerEntity().getEmployerName()
                : member.getEmployer();

        return EssDashboard.builder()
                .memberName(member.getFullName())
                .memberNumber(member.getMemberNumber())
                .employerName(employerName)
                .totalSavings(totalSavings)
                .totalLoanOutstanding(totalLoanOutstanding)
                .activeLoans((int) activeLoans)
                .activeSavingsAccounts((int) activeSavings)
                .monthlyDeductions(monthlyDeductions)
                .recentRequests(recentRequests.stream().limit(5).toList())
                .build();
    }

    @Transactional
    public EssServiceRequest submitLoanApplication(UUID memberId, BigDecimal amount, int termMonths,
                                                     String purpose, UUID productId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        EssServiceRequest request = EssServiceRequest.builder()
                .member(member)
                .requestType("LOAN_APPLICATION")
                .amount(amount)
                .description("Loan application: " + purpose)
                .requestData(String.format(
                        "{\"amount\":\"%s\",\"termMonths\":%d,\"purpose\":\"%s\",\"productId\":\"%s\"}",
                        amount.toPlainString(), termMonths,
                        purpose != null ? purpose : "",
                        productId != null ? productId : ""))
                .build();

        return essServiceRequestRepository.save(request);
    }

    @Transactional
    public EssServiceRequest submitWithdrawalRequest(UUID memberId, UUID accountId,
                                                       BigDecimal amount, String reason) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        SavingsAccount account = savingsAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found"));

        if (!account.getMember().getId().equals(memberId)) {
            throw new BusinessException("Account does not belong to this member");
        }

        EssServiceRequest request = EssServiceRequest.builder()
                .member(member)
                .requestType("WITHDRAWAL")
                .amount(amount)
                .description("Withdrawal request: " + (reason != null ? reason : ""))
                .requestData(String.format(
                        "{\"accountId\":\"%s\",\"amount\":\"%s\",\"reason\":\"%s\"}",
                        accountId, amount.toPlainString(),
                        reason != null ? reason : ""))
                .build();

        return essServiceRequestRepository.save(request);
    }

    public List<PayrollDeduction> getPayrollDeductions(UUID memberId) {
        return payrollDeductionRepository.findByMemberIdAndIsActiveTrue(memberId);
    }

    public List<EssServiceRequest> getMemberRequests(UUID memberId) {
        return essServiceRequestRepository.findByMemberId(memberId);
    }

    @Transactional
    public EssServiceRequest reviewRequest(UUID requestId, String status, String reviewNotes, UUID reviewedBy) {
        EssServiceRequest request = essServiceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException("Request is not in PENDING status");
        }

        if (reviewedBy != null) {
            User reviewer = userRepository.findById(reviewedBy)
                    .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found"));
            request.setReviewedByUser(reviewer);
        }

        request.setStatus(status);
        request.setReviewNotes(reviewNotes);
        request.setReviewedAt(java.time.LocalDateTime.now());

        return essServiceRequestRepository.save(request);
    }

    @Transactional
    public EssServiceRequest reviewRequestByUsername(UUID requestId, String status, String reviewNotes, String username) {
        User reviewer = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found"));
        return reviewRequest(requestId, status, reviewNotes, reviewer.getId());
    }

    // Employer management

    @Transactional
    public Employer createEmployer(String employerCode, String employerName, String contactPerson,
                                    String phoneNumber, String email, String address, String tinNumber,
                                    Integer payrollCutoffDay) {
        if (employerRepository.findByEmployerCode(employerCode).isPresent()) {
            throw new BusinessException("Employer with code " + employerCode + " already exists");
        }

        Employer employer = Employer.builder()
                .employerCode(employerCode)
                .employerName(employerName)
                .contactPerson(contactPerson)
                .phoneNumber(phoneNumber)
                .email(email)
                .address(address)
                .tinNumber(tinNumber)
                .payrollCutoffDay(payrollCutoffDay != null ? payrollCutoffDay : 25)
                .build();

        return employerRepository.save(employer);
    }

    public List<Employer> getActiveEmployers() {
        return employerRepository.findByIsActiveTrue();
    }

    public Employer getEmployer(UUID id) {
        return employerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employer not found"));
    }

    public UUID getMemberIdForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getLinkedMemberId() == null) {
            throw new BusinessException("User is not linked to a member record");
        }
        return user.getLinkedMemberId();
    }

    public UUID getMemberIdForUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getLinkedMemberId() == null) {
            throw new BusinessException("User is not linked to a member record");
        }
        return user.getLinkedMemberId();
    }
}
