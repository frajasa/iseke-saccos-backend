package tz.co.iseke.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import tz.co.iseke.dto.JournalEntryResultDTO;
import tz.co.iseke.entity.SavingsAccount;
import tz.co.iseke.inputs.CreateSavingsAccountInput;
import tz.co.iseke.inputs.CreateSavingsProductInput;
import tz.co.iseke.inputs.JournalEntryInput;
import tz.co.iseke.dto.TrialBalanceDTO;
import tz.co.iseke.inputs.UpdateSavingsProductInput;
import tz.co.iseke.entity.ChartOfAccounts;
import tz.co.iseke.entity.GeneralLedger;
import tz.co.iseke.entity.SavingsProduct;
import tz.co.iseke.service.AccountingService;
import tz.co.iseke.service.SavingsAccountService;
import tz.co.iseke.service.SavingsProductService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AccountingResolver {

    private final AccountingService accountingService;
    private final SavingsProductService savingsProductService;
    private final SavingsAccountService savingsAccountService;

    @QueryMapping
    public List<ChartOfAccounts> chartOfAccounts() {
        return accountingService.getChartOfAccounts();
    }

    @QueryMapping
    public List<GeneralLedger> generalLedger(@Argument UUID accountId,
                                             @Argument LocalDate startDate,
                                             @Argument LocalDate endDate) {
        return accountingService.getGeneralLedger(accountId, startDate, endDate);
    }

    @QueryMapping
    public TrialBalanceDTO trialBalance(@Argument LocalDate date) {
        return accountingService.getTrialBalance(date);
    }

    @QueryMapping
    public List<SavingsProduct> activeSavingsProducts() {
        return savingsProductService.findAllActive();
    }

    @QueryMapping
    public SavingsProduct activeSavingsProductById(@Argument UUID id) {
        return savingsProductService.findById(id);
    }

    @MutationMapping
    public SavingsProduct createSavingsProduct(@Argument CreateSavingsProductInput input) {
        return savingsProductService.createProduct(input);
    }

    @MutationMapping
    public SavingsProduct updateSavingsProduct(@Argument UUID id, @Argument UpdateSavingsProductInput input) {
        return savingsProductService.updateProduct(id, input);
    }

    @MutationMapping
    public Boolean deactivateProduct(@Argument UUID id) {
        savingsProductService.deactivateProduct(id);
        return true;
    }
    // Query: Find savings account by ID (matches schema: savingsAccount(id: ID!))
    @QueryMapping
    public SavingsAccount savingsAccount(@Argument UUID id) {
        return savingsAccountService.findById(id);
    }

    // Query: Find savings account by account number (matches schema: savingsAccountByNumber)
    @QueryMapping
    public SavingsAccount savingsAccountByNumber(@Argument String accountNumber) {
        return savingsAccountService.findByAccountNumber(accountNumber);
    }

    // Query: Find all savings accounts by memberId (matches schema: memberSavingsAccounts)
    @QueryMapping
    public List<SavingsAccount> memberSavingsAccounts(@Argument UUID memberId) {
        return savingsAccountService.findByMemberId(memberId);
    }

    // ✅ 3️⃣ Mutation: Open a new savings account
    @MutationMapping
    public SavingsAccount openSavingsAccount(@Argument CreateSavingsAccountInput input) {
        return savingsAccountService.openAccount(input);
    }

    // ✅ 4️⃣ Mutation: Close an existing account
    @MutationMapping
    public SavingsAccount closeSavingsAccount(@Argument UUID id) {
        return savingsAccountService.closeAccount(id);
    }

    // ✅ 5️⃣ Mutation: Update account balance
    @MutationMapping
    public Boolean updateAccountBalance(
            @Argument UUID accountId,
            @Argument BigDecimal amount,
            @Argument boolean isCredit
    ) {
        savingsAccountService.updateBalance(accountId, amount, isCredit);
        return true;
    }

    @MutationMapping
    public JournalEntryResultDTO postJournalEntry(@Argument JournalEntryInput input) {
        return accountingService.postJournalEntry(input);
    }
}