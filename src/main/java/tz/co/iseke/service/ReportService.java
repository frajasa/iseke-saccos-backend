package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.dto.*;
import tz.co.iseke.entity.*;
import tz.co.iseke.enums.AccountType;
import tz.co.iseke.enums.LoanStatus;
import tz.co.iseke.enums.TransactionType;
import tz.co.iseke.exception.ResourceNotFoundException;
import tz.co.iseke.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final LoanAccountRepository loanRepository;
    private final SavingsAccountRepository savingsRepository;
    private final TransactionRepository transactionRepository;
    private final ChartOfAccountsRepository chartRepository;
    private final GeneralLedgerRepository ledgerRepository;
    private final MemberRepository memberRepository;

    public PortfolioSummaryDTO getPortfolioSummary(UUID branchId, LocalDate startDate, LocalDate endDate) {
        List<LoanAccount> loans;

        if (branchId != null) {
            loans = loanRepository.findByBranchId(branchId);
        } else {
            loans = loanRepository.findAll();
        }

        List<LoanAccount> activeLoans = loans.stream()
                .filter(l -> List.of(LoanStatus.DISBURSED, LoanStatus.ACTIVE).contains(l.getStatus()))
                .toList();

        List<LoanAccount> delinquentLoans = activeLoans.stream()
                .filter(l -> l.getDaysInArrears() > 0)
                .toList();

        BigDecimal totalDisbursed = loans.stream()
                .filter(l -> l.getDisbursementDate() != null)
                .map(LoanAccount::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstanding = activeLoans.stream()
                .map(LoanAccount::getOutstandingPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = loans.stream()
                .map(LoanAccount::getTotalPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageLoanSize = activeLoans.isEmpty() ? BigDecimal.ZERO :
                totalOutstanding.divide(BigDecimal.valueOf(activeLoans.size()), 2, java.math.RoundingMode.HALF_UP);

        BigDecimal portfolioAtRisk = delinquentLoans.stream()
                .map(LoanAccount::getOutstandingPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PortfolioSummaryDTO.builder()
                .totalLoans(loans.size())
                .totalDisbursed(totalDisbursed)
                .totalOutstanding(totalOutstanding)
                .totalPaid(totalPaid)
                .activeLoans(activeLoans.size())
                .delinquentLoans(delinquentLoans.size())
                .averageLoanSize(averageLoanSize)
                .portfolioAtRisk(portfolioAtRisk)
                .build();
    }

    public DelinquencyReportDTO getDelinquencyReport(UUID branchId, LocalDate date) {
        List<LoanAccount> loans;

        if (branchId != null) {
            loans = loanRepository.findByBranchId(branchId);
        } else {
            loans = loanRepository.findAll();
        }

        List<LoanAccount> activeLoans = loans.stream()
                .filter(l -> List.of(LoanStatus.DISBURSED, LoanStatus.ACTIVE).contains(l.getStatus()))
                .toList();

        BigDecimal totalOutstanding = activeLoans.stream()
                .map(LoanAccount::getOutstandingPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group by delinquency ranges
        Map<String, List<LoanAccount>> ranges = new HashMap<>();
        ranges.put("Current (0 days)", activeLoans.stream()
                .filter(l -> l.getDaysInArrears() == 0).collect(Collectors.toList()));
        ranges.put("1-30 days", activeLoans.stream()
                .filter(l -> l.getDaysInArrears() >= 1 && l.getDaysInArrears() <= 30).collect(Collectors.toList()));
        ranges.put("31-60 days", activeLoans.stream()
                .filter(l -> l.getDaysInArrears() >= 31 && l.getDaysInArrears() <= 60).collect(Collectors.toList()));
        ranges.put("61-90 days", activeLoans.stream()
                .filter(l -> l.getDaysInArrears() >= 61 && l.getDaysInArrears() <= 90).collect(Collectors.toList()));
        ranges.put("Over 90 days", activeLoans.stream()
                .filter(l -> l.getDaysInArrears() > 90).collect(Collectors.toList()));

        List<DelinquencyRangeDTO> rangeList = ranges.entrySet().stream()
                .map(entry -> {
                    BigDecimal amount = entry.getValue().stream()
                            .map(LoanAccount::getOutstandingPrincipal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal percentage = totalOutstanding.compareTo(BigDecimal.ZERO) > 0 ?
                            amount.divide(totalOutstanding, 4, java.math.RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

                    return DelinquencyRangeDTO.builder()
                            .range(entry.getKey())
                            .numberOfLoans(entry.getValue().size())
                            .outstandingAmount(amount)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalAtRisk = activeLoans.stream()
                .filter(l -> l.getDaysInArrears() > 0)
                .map(LoanAccount::getOutstandingPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DelinquencyReportDTO.builder()
                .date(date)
                .ranges(rangeList)
                .totalOutstanding(totalOutstanding)
                .totalAtRisk(totalAtRisk)
                .build();
    }

    public FinancialStatementsDTO getFinancialStatements(LocalDate date, UUID branchId) {
        BalanceSheetDTO balanceSheet = generateBalanceSheet(date, branchId);
        IncomeStatementDTO incomeStatement = generateIncomeStatement(date, branchId);

        return FinancialStatementsDTO.builder()
                .date(date)
                .balanceSheet(balanceSheet)
                .incomeStatement(incomeStatement)
                .build();
    }

    public CashFlowStatementDTO getCashFlowStatement(LocalDate startDate, LocalDate endDate, UUID branchId) {
        // Operating Activities
        List<CashFlowItemDTO> operatingItems = new ArrayList<>();

        BigDecimal deposits = transactionRepository.sumByTypeAndDateRange(TransactionType.DEPOSIT, startDate, endDate);
        BigDecimal withdrawals = transactionRepository.sumByTypeAndDateRange(TransactionType.WITHDRAWAL, startDate, endDate);
        BigDecimal loanRepayments = transactionRepository.sumByTypeAndDateRange(TransactionType.LOAN_REPAYMENT, startDate, endDate);
        BigDecimal fees = transactionRepository.sumByTypeAndDateRange(TransactionType.FEE, startDate, endDate);
        BigDecimal interest = transactionRepository.sumByTypeAndDateRange(TransactionType.INTEREST, startDate, endDate);

        operatingItems.add(CashFlowItemDTO.builder()
                .description("Member Deposits")
                .amount(deposits)
                .build());
        operatingItems.add(CashFlowItemDTO.builder()
                .description("Member Withdrawals")
                .amount(withdrawals.negate())
                .build());
        operatingItems.add(CashFlowItemDTO.builder()
                .description("Loan Repayments Received")
                .amount(loanRepayments)
                .build());
        operatingItems.add(CashFlowItemDTO.builder()
                .description("Fees Collected")
                .amount(fees)
                .build());
        operatingItems.add(CashFlowItemDTO.builder()
                .description("Interest Paid on Savings")
                .amount(interest.negate())
                .build());

        BigDecimal operatingTotal = operatingItems.stream()
                .map(CashFlowItemDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CashFlowSectionDTO operating = CashFlowSectionDTO.builder()
                .name("Operating Activities")
                .items(operatingItems)
                .total(operatingTotal)
                .build();

        // Investing Activities
        List<CashFlowItemDTO> investingItems = new ArrayList<>();
        BigDecimal disbursements = transactionRepository.sumByTypeAndDateRange(TransactionType.LOAN_DISBURSEMENT, startDate, endDate);

        investingItems.add(CashFlowItemDTO.builder()
                .description("Loans Disbursed")
                .amount(disbursements.negate())
                .build());

        BigDecimal investingTotal = investingItems.stream()
                .map(CashFlowItemDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CashFlowSectionDTO investing = CashFlowSectionDTO.builder()
                .name("Investing Activities")
                .items(investingItems)
                .total(investingTotal)
                .build();

        // Financing Activities
        List<CashFlowItemDTO> financingItems = new ArrayList<>();
        // Share capital / member contributions would go here
        CashFlowSectionDTO financing = CashFlowSectionDTO.builder()
                .name("Financing Activities")
                .items(financingItems)
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal netCashFlow = operatingTotal.add(investingTotal);

        return CashFlowStatementDTO.builder()
                .date(endDate)
                .operatingActivities(operating)
                .investingActivities(investing)
                .financingActivities(financing)
                .netCashFlow(netCashFlow)
                .build();
    }

    public MemberStatementDTO getMemberStatement(UUID memberId, UUID accountId,
                                                  LocalDate startDate, LocalDate endDate) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        SavingsAccount account = null;
        List<Transaction> transactions;

        if (accountId != null) {
            account = savingsRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
            transactions = transactionRepository.findAccountTransactionsBetweenDates(
                    accountId, startDate, endDate);
        } else {
            transactions = transactionRepository.findMemberTransactionsBetweenDates(
                    memberId, startDate, endDate);
        }

        // Calculate opening balance from first transaction or zero
        BigDecimal openingBalance = BigDecimal.ZERO;
        BigDecimal closingBalance = BigDecimal.ZERO;
        if (!transactions.isEmpty()) {
            Transaction first = transactions.get(0);
            openingBalance = first.getBalanceBefore() != null ? first.getBalanceBefore() : BigDecimal.ZERO;
            Transaction last = transactions.get(transactions.size() - 1);
            closingBalance = last.getBalanceAfter() != null ? last.getBalanceAfter() : BigDecimal.ZERO;
        } else if (account != null) {
            openingBalance = account.getBalance();
            closingBalance = account.getBalance();
        }

        String period = startDate.toString() + " to " + endDate.toString();

        return MemberStatementDTO.builder()
                .member(member)
                .account(account)
                .transactions(transactions)
                .openingBalance(openingBalance)
                .closingBalance(closingBalance)
                .period(period)
                .build();
    }

    public DailyTransactionSummaryDTO getDailyTransactionSummary(LocalDate date, UUID branchId) {
        List<Transaction> transactions;
        if (branchId != null) {
            transactions = transactionRepository.findBranchTransactionsBetweenDates(branchId, date, date);
        } else {
            transactions = transactionRepository.findByTransactionDate(date);
        }

        BigDecimal deposits = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEPOSIT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal withdrawals = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.WITHDRAWAL)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal loanDisbursements = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.LOAN_DISBURSEMENT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal loanRepayments = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.LOAN_REPAYMENT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DailyTransactionSummaryDTO.builder()
                .date(date)
                .deposits(deposits)
                .withdrawals(withdrawals)
                .loanDisbursements(loanDisbursements)
                .loanRepayments(loanRepayments)
                .totalCount(transactions.size())
                .build();
    }

    private BalanceSheetDTO generateBalanceSheet(LocalDate date, UUID branchId) {
        List<ChartOfAccounts> assetAccounts = chartRepository.findByAccountType(AccountType.ASSET);
        List<ChartOfAccounts> liabilityAccounts = chartRepository.findByAccountType(AccountType.LIABILITY);
        List<ChartOfAccounts> equityAccounts = chartRepository.findByAccountType(AccountType.EQUITY);

        BigDecimal totalAssets = calculateAccountsBalance(assetAccounts, date, branchId);
        BigDecimal totalLiabilities = calculateAccountsBalance(liabilityAccounts, date, branchId);
        BigDecimal totalEquity = calculateAccountsBalance(equityAccounts, date, branchId);

        List<BalanceSheetItemDTO> details = new ArrayList<>();
        details.add(createBalanceSheetItem("Assets", totalAssets, assetAccounts, date, branchId));
        details.add(createBalanceSheetItem("Liabilities", totalLiabilities, liabilityAccounts, date, branchId));
        details.add(createBalanceSheetItem("Equity", totalEquity, equityAccounts, date, branchId));

        return BalanceSheetDTO.builder()
                .assets(totalAssets)
                .liabilities(totalLiabilities)
                .equity(totalEquity)
                .details(details)
                .build();
    }

    private IncomeStatementDTO generateIncomeStatement(LocalDate date, UUID branchId) {
        LocalDate startDate = date.withDayOfMonth(1);

        List<ChartOfAccounts> incomeAccounts = chartRepository.findByAccountType(AccountType.INCOME);
        List<ChartOfAccounts> expenseAccounts = chartRepository.findByAccountType(AccountType.EXPENSE);

        BigDecimal totalRevenue = calculateAccountsBalance(incomeAccounts, startDate, date, branchId);
        BigDecimal totalExpenses = calculateAccountsBalance(expenseAccounts, startDate, date, branchId);
        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);

        List<IncomeStatementItemDTO> details = new ArrayList<>();
        details.add(createIncomeStatementItem("Revenue", totalRevenue, incomeAccounts, startDate, date, branchId));
        details.add(createIncomeStatementItem("Expenses", totalExpenses, expenseAccounts, startDate, date, branchId));

        return IncomeStatementDTO.builder()
                .revenue(totalRevenue)
                .expenses(totalExpenses)
                .netIncome(netIncome)
                .details(details)
                .build();
    }

    private BigDecimal calculateAccountsBalance(List<ChartOfAccounts> accounts, LocalDate date, UUID branchId) {
        return accounts.stream()
                .map(account -> {
                    BigDecimal balance = ledgerRepository.getAccountBalance(account.getId(), date);
                    return balance != null ? balance : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAccountsBalance(List<ChartOfAccounts> accounts,
                                                LocalDate startDate, LocalDate endDate, UUID branchId) {
        return accounts.stream()
                .map(account -> {
                    List<GeneralLedger> entries = ledgerRepository.findByAccountIdAndDateRange(
                            account.getId(), startDate, endDate);
                    return entries.stream()
                            .map(e -> e.getCreditAmount().subtract(e.getDebitAmount()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BalanceSheetItemDTO createBalanceSheetItem(String category, BigDecimal amount,
                                                       List<ChartOfAccounts> accounts,
                                                       LocalDate date, UUID branchId) {
        List<AccountBalanceDTO> accountBalances = accounts.stream()
                .map(account -> {
                    BigDecimal balance = ledgerRepository.getAccountBalance(account.getId(), date);
                    return AccountBalanceDTO.builder()
                            .accountName(account.getAccountName())
                            .amount(balance != null ? balance : BigDecimal.ZERO)
                            .build();
                })
                .collect(Collectors.toList());

        return BalanceSheetItemDTO.builder()
                .category(category)
                .amount(amount)
                .accounts(accountBalances)
                .build();
    }

    private IncomeStatementItemDTO createIncomeStatementItem(String category, BigDecimal amount,
                                                             List<ChartOfAccounts> accounts,
                                                             LocalDate startDate, LocalDate endDate, UUID branchId) {
        List<AccountBalanceDTO> accountBalances = accounts.stream()
                .map(account -> {
                    List<GeneralLedger> entries = ledgerRepository.findByAccountIdAndDateRange(
                            account.getId(), startDate, endDate);
                    BigDecimal balance = entries.stream()
                            .map(e -> e.getCreditAmount().subtract(e.getDebitAmount()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return AccountBalanceDTO.builder()
                            .accountName(account.getAccountName())
                            .amount(balance)
                            .build();
                })
                .collect(Collectors.toList());

        return IncomeStatementItemDTO.builder()
                .category(category)
                .amount(amount)
                .accounts(accountBalances)
                .build();
    }

    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }
}
