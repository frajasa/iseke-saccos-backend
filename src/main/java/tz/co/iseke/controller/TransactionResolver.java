package tz.co.iseke.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import tz.co.iseke.entity.Transaction;
import tz.co.iseke.inputs.DepositInput;
import tz.co.iseke.inputs.WithdrawInput;
import tz.co.iseke.service.TransactionService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TransactionResolver {

    private final TransactionService transactionService;

    @QueryMapping
    public Transaction transaction(@Argument UUID id) {
        return transactionService.findById(id);
    }

    @QueryMapping
    public List<Transaction> memberTransactions(@Argument UUID memberId,
                                                @Argument LocalDate startDate,
                                                @Argument LocalDate endDate) {
        return transactionService.findMemberTransactions(memberId, startDate, endDate);
    }

    @QueryMapping
    public List<Transaction> accountTransactions(@Argument UUID accountId,
                                                 @Argument LocalDate startDate,
                                                 @Argument LocalDate endDate) {
        return transactionService.findAccountTransactions(accountId, startDate, endDate);
    }

    @QueryMapping
    public List<Transaction> loanTransactions(@Argument UUID loanId) {
        return transactionService.findLoanTransactions(loanId);
    }

    @MutationMapping
    public Transaction deposit(@Argument DepositInput input) {
        return transactionService.processDeposit(input);
    }

    @MutationMapping
    public Transaction withdraw(@Argument WithdrawInput input) {
        return transactionService.processWithdrawal(input);
    }

    @MutationMapping
    public Transaction reverseTransaction(@Argument UUID transactionId, @Argument String reason) {
        return transactionService.reverseTransaction(transactionId, reason);
    }

    @MutationMapping
    public Transaction interBranchTransfer(@Argument UUID fromAccountId, @Argument UUID toAccountId,
                                            @Argument java.math.BigDecimal amount, @Argument String description) {
        return transactionService.processInterBranchTransfer(fromAccountId, toAccountId, amount, description);
    }
}