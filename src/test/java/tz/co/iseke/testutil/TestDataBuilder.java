package tz.co.iseke.testutil;

import tz.co.iseke.entity.*;
import tz.co.iseke.enums.*;
import tz.co.iseke.inputs.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class TestDataBuilder {

    public static Member aMember() {
        UUID id = UUID.randomUUID();
        return Member.builder()
                .id(id)
                .memberNumber("MEM001")
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .nationalId("19900101-12345-00001")
                .phoneNumber("255712345678")
                .email("john.doe@example.com")
                .membershipDate(LocalDate.now().minusYears(2))
                .monthlyIncome(new BigDecimal("1000000"))
                .status(MemberStatus.ACTIVE)
                .build();
    }

    public static Branch aBranch() {
        return Branch.builder()
                .id(UUID.randomUUID())
                .branchCode("BR001")
                .branchName("Main Branch")
                .status(BranchStatus.ACTIVE)
                .build();
    }

    public static ChartOfAccounts aGlAccount(String code, String name) {
        return ChartOfAccounts.builder()
                .id(UUID.randomUUID())
                .accountCode(code)
                .accountName(name)
                .build();
    }

    public static SavingsProduct aSavingsProduct() {
        return SavingsProduct.builder()
                .id(UUID.randomUUID())
                .productCode("SP001")
                .productName("Regular Savings")
                .productType(SavingsProductType.SAVINGS)
                .interestRate(new BigDecimal("0.05"))
                .minimumBalance(BigDecimal.ZERO)
                .minimumOpeningBalance(new BigDecimal("10000"))
                .withdrawalFee(BigDecimal.ZERO)
                .status(ProductStatus.ACTIVE)
                .liabilityAccount(aGlAccount("2101", "Savings Liability"))
                .cashAccount(aGlAccount("1101", "Cash"))
                .interestExpenseAccount(aGlAccount("5201", "Interest Expense"))
                .feeIncomeAccount(aGlAccount("4201", "Fee Income"))
                .build();
    }

    public static SavingsAccount aSavingsAccount(Member member, SavingsProduct product, Branch branch) {
        return SavingsAccount.builder()
                .id(UUID.randomUUID())
                .accountNumber("SAV001")
                .member(member)
                .product(product)
                .branch(branch)
                .balance(new BigDecimal("500000"))
                .availableBalance(new BigDecimal("500000"))
                .accruedInterest(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .openingDate(LocalDate.now().minusMonths(6))
                .build();
    }

    public static LoanProduct aLoanProduct(InterestMethod method) {
        return LoanProduct.builder()
                .id(UUID.randomUUID())
                .productCode("LP001")
                .productName("Personal Loan")
                .interestRate(new BigDecimal("0.18"))
                .interestMethod(method)
                .repaymentFrequency("MONTHLY")
                .minimumAmount(new BigDecimal("100000"))
                .maximumAmount(new BigDecimal("50000000"))
                .minimumTermMonths(1)
                .maximumTermMonths(60)
                .requiresGuarantors(false)
                .minimumGuarantors(0)
                .requiresCollateral(false)
                .status(ProductStatus.ACTIVE)
                .loanReceivableAccount(aGlAccount("1201", "Loans Receivable"))
                .cashAccount(aGlAccount("1101", "Cash"))
                .interestIncomeAccount(aGlAccount("4101", "Interest Income"))
                .build();
    }

    public static LoanAccount aLoanAccount(Member member, LoanProduct product, Branch branch) {
        return LoanAccount.builder()
                .id(UUID.randomUUID())
                .loanNumber("LN001")
                .member(member)
                .product(product)
                .branch(branch)
                .applicationDate(LocalDate.now().minusMonths(3))
                .disbursementDate(LocalDate.now().minusMonths(2))
                .principalAmount(new BigDecimal("1000000"))
                .interestRate(product.getInterestRate())
                .termMonths(12)
                .repaymentFrequency("MONTHLY")
                .outstandingPrincipal(new BigDecimal("1000000"))
                .outstandingInterest(BigDecimal.ZERO)
                .outstandingPenalties(BigDecimal.ZERO)
                .totalPaid(BigDecimal.ZERO)
                .daysInArrears(0)
                .status(LoanStatus.DISBURSED)
                .build();
    }

    public static LoanRepaymentSchedule aScheduleEntry(LoanAccount loan, int installment,
                                                         BigDecimal principal, BigDecimal interest) {
        return LoanRepaymentSchedule.builder()
                .id(UUID.randomUUID())
                .loan(loan)
                .installmentNumber(installment)
                .dueDate(LocalDate.now().plusMonths(installment))
                .principalDue(principal)
                .interestDue(interest)
                .totalDue(principal.add(interest))
                .principalPaid(BigDecimal.ZERO)
                .interestPaid(BigDecimal.ZERO)
                .feesPaid(BigDecimal.ZERO)
                .penaltiesPaid(BigDecimal.ZERO)
                .totalPaid(BigDecimal.ZERO)
                .status(ScheduleStatus.PENDING)
                .build();
    }

    public static DepositInput aDepositInput(UUID accountId, BigDecimal amount) {
        DepositInput input = new DepositInput();
        input.setAccountId(accountId);
        input.setAmount(amount);
        input.setPaymentMethod(PaymentMethod.CASH);
        input.setDescription("Test deposit");
        return input;
    }

    public static WithdrawInput aWithdrawInput(UUID accountId, BigDecimal amount) {
        WithdrawInput input = new WithdrawInput();
        input.setAccountId(accountId);
        input.setAmount(amount);
        input.setPaymentMethod(PaymentMethod.CASH);
        input.setDescription("Test withdrawal");
        return input;
    }

    public static LoanRepaymentInput aLoanRepaymentInput(UUID loanId, BigDecimal amount) {
        LoanRepaymentInput input = new LoanRepaymentInput();
        input.setLoanId(loanId);
        input.setAmount(amount);
        input.setPaymentMethod(PaymentMethod.CASH);
        return input;
    }

    public static Transaction aTransaction(Member member, SavingsAccount account, BigDecimal amount) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .transactionNumber("TXN" + System.currentTimeMillis())
                .transactionDate(LocalDate.now())
                .transactionTime(LocalDateTime.now())
                .transactionType(TransactionType.DEPOSIT)
                .member(member)
                .savingsAccount(account)
                .amount(amount)
                .balanceBefore(account.getBalance())
                .balanceAfter(account.getBalance().add(amount))
                .paymentMethod(PaymentMethod.CASH)
                .status(TransactionStatus.COMPLETED)
                .build();
    }

    public static PaymentRequest aPaymentRequest(PaymentProvider provider, BigDecimal amount) {
        return PaymentRequest.builder()
                .id(UUID.randomUUID())
                .requestNumber("PAY" + System.currentTimeMillis())
                .provider(provider)
                .direction(PaymentDirection.INBOUND)
                .status(PaymentRequestStatus.SENT)
                .amount(amount)
                .phoneNumber("255712345678")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .initiatedAt(LocalDateTime.now())
                .build();
    }
}
