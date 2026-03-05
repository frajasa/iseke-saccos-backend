package tz.co.iseke.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.co.iseke.dto.JournalEntryResultDTO;
import tz.co.iseke.dto.TrialBalanceDTO;
import tz.co.iseke.entity.*;
import tz.co.iseke.exception.BusinessException;
import tz.co.iseke.inputs.JournalEntryInput;
import tz.co.iseke.inputs.JournalEntryLineInput;
import tz.co.iseke.repository.BranchRepository;
import tz.co.iseke.repository.ChartOfAccountsRepository;
import tz.co.iseke.repository.GeneralLedgerRepository;
import tz.co.iseke.testutil.TestDataBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountingServiceTest {

    @Mock private ChartOfAccountsRepository chartRepository;
    @Mock private GeneralLedgerRepository ledgerRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private AccountingService accountingService;

    private ChartOfAccounts cashAccount;
    private ChartOfAccounts liabilityAccount;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        cashAccount = TestDataBuilder.aGlAccount("1101", "Cash");
        liabilityAccount = TestDataBuilder.aGlAccount("2101", "Savings Liability");

        Member member = TestDataBuilder.aMember();
        Branch branch = TestDataBuilder.aBranch();
        SavingsProduct product = TestDataBuilder.aSavingsProduct();
        SavingsAccount account = TestDataBuilder.aSavingsAccount(member, product, branch);
        transaction = TestDataBuilder.aTransaction(member, account, new BigDecimal("100000"));

        lenient().when(ledgerRepository.save(any(GeneralLedger.class)))
                .thenAnswer(inv -> {
                    GeneralLedger gl = inv.getArgument(0);
                    if (gl.getId() == null) gl.setId(UUID.randomUUID());
                    return gl;
                });
    }

    @Test
    void postToGeneralLedger_createsDebitAndCreditEntries() {
        accountingService.postToGeneralLedger(transaction, cashAccount, liabilityAccount, new BigDecimal("100000"));

        ArgumentCaptor<GeneralLedger> captor = ArgumentCaptor.forClass(GeneralLedger.class);
        verify(ledgerRepository, times(2)).save(captor.capture());

        List<GeneralLedger> entries = captor.getAllValues();
        GeneralLedger debit = entries.get(0);
        GeneralLedger credit = entries.get(1);

        assertEquals(new BigDecimal("100000"), debit.getDebitAmount());
        assertEquals(BigDecimal.ZERO, debit.getCreditAmount());
        assertEquals(cashAccount, debit.getAccount());

        assertEquals(BigDecimal.ZERO, credit.getDebitAmount());
        assertEquals(new BigDecimal("100000"), credit.getCreditAmount());
        assertEquals(liabilityAccount, credit.getAccount());
    }

    @Test
    void postToGeneralLedger_nullDebitAccount_onlyCreditPosted() {
        accountingService.postToGeneralLedger(transaction, null, liabilityAccount, new BigDecimal("100000"));

        verify(ledgerRepository, times(1)).save(any(GeneralLedger.class));
    }

    @Test
    void postToGeneralLedger_nullCreditAccount_onlyDebitPosted() {
        accountingService.postToGeneralLedger(transaction, cashAccount, null, new BigDecimal("100000"));

        verify(ledgerRepository, times(1)).save(any(GeneralLedger.class));
    }

    @Test
    void postToGeneralLedgerDirect_noTransactionReference() {
        Branch branch = TestDataBuilder.aBranch();

        accountingService.postToGeneralLedgerDirect(
                LocalDate.now(), cashAccount, liabilityAccount,
                new BigDecimal("50000"), "Direct posting", "REF001", branch);

        ArgumentCaptor<GeneralLedger> captor = ArgumentCaptor.forClass(GeneralLedger.class);
        verify(ledgerRepository, times(2)).save(captor.capture());

        List<GeneralLedger> entries = captor.getAllValues();
        assertNull(entries.get(0).getTransaction());
        assertEquals("REF001", entries.get(0).getReference());
    }

    @Test
    void postJournalEntry_balanced_succeeds() {
        UUID cashId = cashAccount.getId();
        UUID liabId = liabilityAccount.getId();

        JournalEntryLineInput line1 = new JournalEntryLineInput();
        line1.setAccountId(cashId);
        line1.setDebitAmount(new BigDecimal("100000"));
        line1.setCreditAmount(BigDecimal.ZERO);

        JournalEntryLineInput line2 = new JournalEntryLineInput();
        line2.setAccountId(liabId);
        line2.setDebitAmount(BigDecimal.ZERO);
        line2.setCreditAmount(new BigDecimal("100000"));

        JournalEntryInput input = new JournalEntryInput();
        input.setDescription("Test journal entry");
        input.setLines(List.of(line1, line2));

        when(chartRepository.findById(cashId)).thenReturn(Optional.of(cashAccount));
        when(chartRepository.findById(liabId)).thenReturn(Optional.of(liabilityAccount));

        JournalEntryResultDTO result = accountingService.postJournalEntry(input);

        assertTrue(result.getSuccess());
        assertEquals(2, result.getEntriesPosted());
    }

    @Test
    void postJournalEntry_unbalanced_throwsBusinessException() {
        JournalEntryLineInput line1 = new JournalEntryLineInput();
        line1.setAccountId(cashAccount.getId());
        line1.setDebitAmount(new BigDecimal("100000"));
        line1.setCreditAmount(BigDecimal.ZERO);

        JournalEntryLineInput line2 = new JournalEntryLineInput();
        line2.setAccountId(liabilityAccount.getId());
        line2.setDebitAmount(BigDecimal.ZERO);
        line2.setCreditAmount(new BigDecimal("50000")); // Not balanced!

        JournalEntryInput input = new JournalEntryInput();
        input.setDescription("Unbalanced entry");
        input.setLines(List.of(line1, line2));

        assertThrows(BusinessException.class, () -> accountingService.postJournalEntry(input));
    }

    @Test
    void postJournalEntry_singleLine_throwsBusinessException() {
        JournalEntryLineInput line1 = new JournalEntryLineInput();
        line1.setAccountId(cashAccount.getId());
        line1.setDebitAmount(BigDecimal.ZERO);
        line1.setCreditAmount(BigDecimal.ZERO);

        JournalEntryInput input = new JournalEntryInput();
        input.setDescription("Single line");
        input.setLines(List.of(line1));

        assertThrows(BusinessException.class, () -> accountingService.postJournalEntry(input));
    }

    @Test
    void trialBalance_aggregatesCorrectly() {
        GeneralLedger debitEntry = GeneralLedger.builder()
                .id(UUID.randomUUID())
                .account(cashAccount)
                .debitAmount(new BigDecimal("100000"))
                .creditAmount(BigDecimal.ZERO)
                .postingDate(LocalDate.now())
                .build();

        GeneralLedger creditEntry = GeneralLedger.builder()
                .id(UUID.randomUUID())
                .account(liabilityAccount)
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("100000"))
                .postingDate(LocalDate.now())
                .build();

        when(ledgerRepository.findByDateRange(any(), any())).thenReturn(List.of(debitEntry, creditEntry));

        TrialBalanceDTO trialBalance = accountingService.getTrialBalance(LocalDate.now());

        assertEquals(0, trialBalance.getTotalDebits().compareTo(trialBalance.getTotalCredits()));
        assertEquals(2, trialBalance.getEntries().size());
    }
}
