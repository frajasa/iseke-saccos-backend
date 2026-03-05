package tz.co.iseke.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import tz.co.iseke.entity.TransactionApproval;
import tz.co.iseke.service.TransactionApprovalService;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ApprovalController {

    private final TransactionApprovalService approvalService;

    @QueryMapping
    public List<TransactionApproval> pendingApprovals() {
        return approvalService.findPendingApprovals();
    }

    @QueryMapping
    public TransactionApproval transactionApproval(@Argument UUID id) {
        return approvalService.findById(id);
    }

    @MutationMapping
    public TransactionApproval approveTransaction(@Argument UUID approvalId) {
        return approvalService.approveTransaction(approvalId);
    }

    @MutationMapping
    public TransactionApproval rejectTransaction(@Argument UUID approvalId, @Argument String reason) {
        return approvalService.rejectTransaction(approvalId, reason);
    }
}
