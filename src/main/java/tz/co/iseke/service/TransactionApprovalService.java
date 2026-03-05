package tz.co.iseke.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.entity.TransactionApproval;
import tz.co.iseke.enums.ApprovalStatus;
import tz.co.iseke.exception.BusinessException;
import tz.co.iseke.exception.ResourceNotFoundException;
import tz.co.iseke.inputs.DepositInput;
import tz.co.iseke.inputs.WithdrawInput;
import tz.co.iseke.repository.TransactionApprovalRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TransactionApprovalService {

    private final TransactionApprovalRepository approvalRepository;
    private final TransactionService transactionService;
    private final LoanAccountService loanAccountService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Value("${approval.deposit.threshold:5000000}")
    private BigDecimal depositThreshold;

    @Value("${approval.withdrawal.threshold:1000000}")
    private BigDecimal withdrawalThreshold;

    @Value("${approval.disbursement.threshold:10000000}")
    private BigDecimal disbursementThreshold;

    @Value("${approval.transfer.threshold:2000000}")
    private BigDecimal transferThreshold;

    public boolean requiresApproval(String transactionType, BigDecimal amount) {
        return switch (transactionType) {
            case "DEPOSIT" -> amount.compareTo(depositThreshold) > 0;
            case "WITHDRAWAL" -> amount.compareTo(withdrawalThreshold) > 0;
            case "DISBURSEMENT" -> amount.compareTo(disbursementThreshold) > 0;
            case "TRANSFER" -> amount.compareTo(transferThreshold) > 0;
            default -> false;
        };
    }

    public TransactionApproval createApprovalRequest(String transactionType, BigDecimal amount,
                                                      UUID entityId, String requestData) {
        String currentUser = getCurrentUser();

        TransactionApproval approval = TransactionApproval.builder()
                .transactionType(transactionType)
                .entityId(entityId)
                .amount(amount)
                .requestData(requestData)
                .requestedBy(currentUser)
                .requestedAt(LocalDateTime.now())
                .status(ApprovalStatus.PENDING)
                .build();

        approval = approvalRepository.save(approval);

        auditService.logAction("APPROVAL_REQUESTED", "TransactionApproval", approval.getId(),
                null, "Type: " + transactionType + ", Amount: " + amount);

        log.info("Approval request created: {} for {} amount {}", approval.getId(), transactionType, amount);
        return approval;
    }

    public TransactionApproval approveTransaction(UUID approvalId) {
        TransactionApproval approval = findById(approvalId);

        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException("Only pending approvals can be approved");
        }

        String currentUser = getCurrentUser();
        if (currentUser != null && currentUser.equals(approval.getRequestedBy())) {
            throw new BusinessException("Cannot approve your own request");
        }

        approval.setApprovedBy(currentUser);
        approval.setApprovedAt(LocalDateTime.now());
        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setUpdatedAt(LocalDateTime.now());
        approval = approvalRepository.save(approval);

        // Execute the original transaction
        executeApprovedTransaction(approval);

        auditService.logAction("APPROVAL_APPROVED", "TransactionApproval", approval.getId(),
                "Status: PENDING", "Status: APPROVED by " + currentUser);

        return approval;
    }

    public TransactionApproval rejectTransaction(UUID approvalId, String reason) {
        TransactionApproval approval = findById(approvalId);

        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException("Only pending approvals can be rejected");
        }

        String currentUser = getCurrentUser();
        approval.setRejectedBy(currentUser);
        approval.setRejectedAt(LocalDateTime.now());
        approval.setRejectionReason(reason);
        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setUpdatedAt(LocalDateTime.now());
        approval = approvalRepository.save(approval);

        auditService.logAction("APPROVAL_REJECTED", "TransactionApproval", approval.getId(),
                "Status: PENDING", "Status: REJECTED, Reason: " + reason);

        return approval;
    }

    public TransactionApproval findById(UUID id) {
        return approvalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found"));
    }

    public List<TransactionApproval> findPendingApprovals() {
        return approvalRepository.findByStatus(ApprovalStatus.PENDING);
    }

    public Page<TransactionApproval> findPendingApprovals(Pageable pageable) {
        return approvalRepository.findByStatus(ApprovalStatus.PENDING, pageable);
    }

    private void executeApprovedTransaction(TransactionApproval approval) {
        try {
            switch (approval.getTransactionType()) {
                case "DEPOSIT" -> {
                    DepositInput input = objectMapper.readValue(approval.getRequestData(), DepositInput.class);
                    transactionService.processDeposit(input);
                }
                case "WITHDRAWAL" -> {
                    WithdrawInput input = objectMapper.readValue(approval.getRequestData(), WithdrawInput.class);
                    transactionService.processWithdrawal(input);
                }
                case "DISBURSEMENT" -> {
                    loanAccountService.disburseLoan(approval.getEntityId(), null);
                }
                default -> log.warn("Unknown transaction type for approval: {}", approval.getTransactionType());
            }
        } catch (Exception e) {
            log.error("Failed to execute approved transaction {}: {}", approval.getId(), e.getMessage());
            throw new BusinessException("Failed to execute approved transaction: " + e.getMessage());
        }
    }

    private String getCurrentUser() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return null;
        }
    }
}
