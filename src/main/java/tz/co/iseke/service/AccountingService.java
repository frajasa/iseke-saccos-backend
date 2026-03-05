package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.dto.JournalEntryResultDTO;
import tz.co.iseke.dto.TrialBalanceDTO;
import tz.co.iseke.dto.TrialBalanceEntryDTO;
import tz.co.iseke.entity.Branch;
import tz.co.iseke.entity.ChartOfAccounts;
import tz.co.iseke.entity.GeneralLedger;
import tz.co.iseke.entity.Transaction;
import tz.co.iseke.exception.BusinessException;
import tz.co.iseke.inputs.JournalEntryInput;
import tz.co.iseke.inputs.JournalEntryLineInput;
import tz.co.iseke.repository.BranchRepository;
import tz.co.iseke.repository.ChartOfAccountsRepository;
import tz.co.iseke.repository.GeneralLedgerRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountingService {

    private final ChartOfAccountsRepository chartRepository;
    private final GeneralLedgerRepository ledgerRepository;
    private final BranchRepository branchRepository;
    private final AuditService auditService;

    public List<ChartOfAccounts> getChartOfAccounts() {
        return chartRepository.findAll();
    }

    public ChartOfAccounts findAccountByCode(String accountCode) {
        return chartRepository.findByAccountCode(accountCode).orElse(null);
    }

    public List<GeneralLedger> getGeneralLedger(UUID accountId, LocalDate startDate, LocalDate endDate) {
        return ledgerRepository.findByAccountIdAndDateRange(accountId, startDate, endDate);
    }

    public TrialBalanceDTO getTrialBalance(LocalDate date) {
        // Use fiscal year start (Jan 1) for cumulative trial balance, not just current month
        LocalDate fiscalYearStart = LocalDate.of(date.getYear(), 1, 1);
        List<GeneralLedger> entries = ledgerRepository.findByDateRange(
                fiscalYearStart, date);

        Map<UUID, TrialBalanceEntryDTO> accountBalances = new HashMap<>();

        for (GeneralLedger entry : entries) {
            UUID accountId = entry.getAccount().getId();
            TrialBalanceEntryDTO balanceEntry = accountBalances.getOrDefault(accountId,
                    TrialBalanceEntryDTO.builder()
                            .account(entry.getAccount())
                            .debitBalance(BigDecimal.ZERO)
                            .creditBalance(BigDecimal.ZERO)
                            .build());

            balanceEntry.setDebitBalance(
                    balanceEntry.getDebitBalance().add(entry.getDebitAmount()));
            balanceEntry.setCreditBalance(
                    balanceEntry.getCreditBalance().add(entry.getCreditAmount()));

            accountBalances.put(accountId, balanceEntry);
        }

        List<TrialBalanceEntryDTO> entryList = new ArrayList<>(accountBalances.values());

        BigDecimal totalDebits = entryList.stream()
                .map(TrialBalanceEntryDTO::getDebitBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = entryList.stream()
                .map(TrialBalanceEntryDTO::getCreditBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return TrialBalanceDTO.builder()
                .date(date)
                .entries(entryList)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .build();
    }

    public void postToGeneralLedger(Transaction transaction,
                                    ChartOfAccounts debitAccount,
                                    ChartOfAccounts creditAccount,
                                    BigDecimal amount) {
        if (debitAccount != null) {
            GeneralLedger debitEntry = GeneralLedger.builder()
                    .postingDate(transaction.getTransactionDate())
                    .account(debitAccount)
                    .transaction(transaction)
                    .debitAmount(amount)
                    .creditAmount(BigDecimal.ZERO)
                    .description(transaction.getDescription())
                    .reference(transaction.getTransactionNumber())
                    .branch(transaction.getBranch())
                    .postedBy(transaction.getProcessedBy())
                    .build();
            ledgerRepository.save(debitEntry);
        }

        if (creditAccount != null) {
            GeneralLedger creditEntry = GeneralLedger.builder()
                    .postingDate(transaction.getTransactionDate())
                    .account(creditAccount)
                    .transaction(transaction)
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(amount)
                    .description(transaction.getDescription())
                    .reference(transaction.getTransactionNumber())
                    .branch(transaction.getBranch())
                    .postedBy(transaction.getProcessedBy())
                    .build();
            ledgerRepository.save(creditEntry);
        }
    }

    public void postToGeneralLedgerDirect(LocalDate postingDate,
                                           ChartOfAccounts debitAccount,
                                           ChartOfAccounts creditAccount,
                                           BigDecimal amount,
                                           String description,
                                           String reference,
                                           Branch branch) {
        if (debitAccount != null) {
            GeneralLedger debitEntry = GeneralLedger.builder()
                    .postingDate(postingDate)
                    .account(debitAccount)
                    .debitAmount(amount)
                    .creditAmount(BigDecimal.ZERO)
                    .description(description)
                    .reference(reference)
                    .branch(branch)
                    .build();
            ledgerRepository.save(debitEntry);
        }

        if (creditAccount != null) {
            GeneralLedger creditEntry = GeneralLedger.builder()
                    .postingDate(postingDate)
                    .account(creditAccount)
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(amount)
                    .description(description)
                    .reference(reference)
                    .branch(branch)
                    .build();
            ledgerRepository.save(creditEntry);
        }
    }

    @Transactional
    public JournalEntryResultDTO postJournalEntry(JournalEntryInput input) {
        // Validate that debits equal credits
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (JournalEntryLineInput line : input.getLines()) {
            BigDecimal debit = line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO;
            BigDecimal credit = line.getCreditAmount() != null ? line.getCreditAmount() : BigDecimal.ZERO;
            totalDebits = totalDebits.add(debit);
            totalCredits = totalCredits.add(credit);
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BusinessException("Journal entry must balance: total debits ("
                    + totalDebits + ") must equal total credits (" + totalCredits + ")");
        }

        if (input.getLines().size() < 2) {
            throw new BusinessException("Journal entry must have at least 2 lines");
        }

        LocalDate postingDate = input.getPostingDate() != null ? input.getPostingDate() : LocalDate.now();
        String reference = input.getReference() != null ? input.getReference()
                : "JE" + System.currentTimeMillis();

        Branch branch = null;
        if (input.getBranchId() != null) {
            branch = branchRepository.findById(input.getBranchId())
                    .orElseThrow(() -> new BusinessException("Branch not found"));
        }

        String currentUser = null;
        try {
            currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception ignored) {}

        int entriesPosted = 0;
        for (JournalEntryLineInput line : input.getLines()) {
            ChartOfAccounts account = chartRepository.findById(line.getAccountId())
                    .orElseThrow(() -> new BusinessException("Account not found: " + line.getAccountId()));

            BigDecimal debit = line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO;
            BigDecimal credit = line.getCreditAmount() != null ? line.getCreditAmount() : BigDecimal.ZERO;

            GeneralLedger entry = GeneralLedger.builder()
                    .postingDate(postingDate)
                    .account(account)
                    .debitAmount(debit)
                    .creditAmount(credit)
                    .description(input.getDescription())
                    .reference(reference)
                    .branch(branch)
                    .postedBy(currentUser)
                    .build();

            ledgerRepository.save(entry);
            entriesPosted++;
        }

        auditService.logAction("JOURNAL_ENTRY_POSTED", "GeneralLedger", null,
                null, "Reference: " + reference + ", Entries: " + entriesPosted);

        return JournalEntryResultDTO.builder()
                .success(true)
                .reference(reference)
                .entriesPosted(entriesPosted)
                .postingDate(postingDate)
                .build();
    }
}
