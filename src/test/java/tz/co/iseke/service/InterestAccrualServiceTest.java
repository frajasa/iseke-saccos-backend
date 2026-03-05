package tz.co.iseke.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.co.iseke.entity.*;
import tz.co.iseke.enums.AccountStatus;
import tz.co.iseke.enums.TransactionType;
import tz.co.iseke.repository.SavingsAccountRepository;
import tz.co.iseke.repository.TransactionRepository;
import tz.co.iseke.testutil.TestDataBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterestAccrualServiceTest {

    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountingService accountingService;
    @Mock private TaxService taxService;
    @Mock private AuditService auditService;

    @InjectMocks
    private InterestAccrualService interestAccrualService;

    private Member member;
    private Branch branch;
    private SavingsProduct product;
    private SavingsAccount account;

    @BeforeEach
    void setUp() {
        member = TestDataBuilder.aMember();
        branch = TestDataBuilder.aBranch();
        product = TestDataBuilder.aSavingsProduct();
        account = TestDataBuilder.aSavingsAccount(member, product, branch);

        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> {
                    Transaction t = inv.getArgument(0);
                    if (t.getId() == null) t.setId(UUID.randomUUID());
                    return t;
                });
    }

    @Test
    void dailyAccrual_accruesForActiveAccountAboveMinimum() {
        when(savingsAccountRepository.findByStatus(AccountStatus.ACTIVE)).thenReturn(List.of(account));

        int accrued = interestAccrualService.runDailyAccrual();

        assertEquals(1, accrued);
        assertTrue(account.getAccruedInterest().compareTo(BigDecimal.ZERO) > 0);
        verify(savingsAccountRepository).save(account);
    }

    @Test
    void dailyAccrual_skipsAccountsBelowMinimumBalance() {
        product.setMinimumBalance(new BigDecimal("600000"));
        account.setBalance(new BigDecimal("500000")); // below minimum

        when(savingsAccountRepository.findByStatus(AccountStatus.ACTIVE)).thenReturn(List.of(account));

        int accrued = interestAccrualService.runDailyAccrual();

        assertEquals(0, accrued);
        assertEquals(BigDecimal.ZERO, account.getAccruedInterest());
    }

    @Test
    void dailyAccrual_skipsZeroInterestRateProducts() {
        product.setInterestRate(BigDecimal.ZERO);

        when(savingsAccountRepository.findByStatus(AccountStatus.ACTIVE)).thenReturn(List.of(account));

        int accrued = interestAccrualService.runDailyAccrual();

        assertEquals(0, accrued);
    }

    @Test
    void dailyAccrual_correctDailyAmount() {
        // 1,000,000 * (0.05 / 365) = 136.99
        account.setBalance(new BigDecimal("1000000"));
        product.setInterestRate(new BigDecimal("0.05"));

        when(savingsAccountRepository.findByStatus(AccountStatus.ACTIVE)).thenReturn(List.of(account));

        interestAccrualService.runDailyAccrual();

        BigDecimal expected = new BigDecimal("1000000")
                .multiply(new BigDecimal("0.05").divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(expected, account.getAccruedInterest());
        // Should be approximately 136.99
        assertTrue(account.getAccruedInterest().compareTo(new BigDecimal("136")) > 0);
        assertTrue(account.getAccruedInterest().compareTo(new BigDecimal("138")) < 0);
    }

    @Test
    void monthlyPosting_creditsNetInterestMinusTax() {
        BigDecimal accruedInterest = new BigDecimal("5000");
        account.setAccruedInterest(accruedInterest);
        BigDecimal tax = new BigDecimal("500"); // 10% withholding
        BigDecimal netInterest = new BigDecimal("4500");
        BigDecimal balanceBefore = account.getBalance();

        when(savingsAccountRepository.findByStatus(AccountStatus.ACTIVE)).thenReturn(List.of(account));
        when(taxService.calculateWithholdingTax(accruedInterest, product)).thenReturn(tax);

        int posted = interestAccrualService.postMonthlyInterest();

        assertEquals(1, posted);
        assertEquals(balanceBefore.add(netInterest), account.getBalance());
        assertEquals(BigDecimal.ZERO, account.getAccruedInterest());
    }

    @Test
    void monthlyPosting_postsGlEntries() {
        account.setAccruedInterest(new BigDecimal("5000"));

        when(savingsAccountRepository.findByStatus(AccountStatus.ACTIVE)).thenReturn(List.of(account));
        when(taxService.calculateWithholdingTax(any(), any())).thenReturn(new BigDecimal("500"));
        when(taxService.getTaxPayableAccount()).thenReturn(TestDataBuilder.aGlAccount("2301", "Tax Payable"));

        interestAccrualService.postMonthlyInterest();

        // GL posting for net interest + GL posting for tax = postToGeneralLedger + postToGeneralLedgerDirect
        verify(accountingService).postToGeneralLedger(any(Transaction.class),
                eq(product.getInterestExpenseAccount()), eq(product.getLiabilityAccount()),
                eq(new BigDecimal("4500")));
        verify(accountingService).postToGeneralLedgerDirect(any(), eq(product.getInterestExpenseAccount()),
                any(ChartOfAccounts.class), eq(new BigDecimal("500")), anyString(), anyString(), any());
    }

    @Test
    void monthlyPosting_skipsZeroAccruedInterest() {
        account.setAccruedInterest(BigDecimal.ZERO);

        when(savingsAccountRepository.findByStatus(AccountStatus.ACTIVE)).thenReturn(List.of(account));

        int posted = interestAccrualService.postMonthlyInterest();

        assertEquals(0, posted);
        verify(transactionRepository, never()).save(any());
    }
}
