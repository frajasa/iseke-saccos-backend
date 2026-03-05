package tz.co.iseke.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.entity.*;
import tz.co.iseke.enums.*;
import tz.co.iseke.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // Run after ApplicationConfig (default user)
public class DataSeeder implements CommandLineRunner {

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final SavingsProductRepository savingsProductRepository;
    private final LoanProductRepository loanProductRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final TransactionRepository transactionRepository;
    private final ChartOfAccountsRepository chartOfAccountsRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random(42);

    @Override
    @Transactional
    public void run(String... args) {
        if (memberRepository.count() >= 10) {
            log.info("Data already seeded ({} members) - skipping", memberRepository.count());
            return;
        }

        log.info("=== Starting data seeding ===");

        List<Branch> branches = seedBranches();
        seedUsers(branches);
        List<SavingsProduct> savingsProducts = seedSavingsProducts();
        List<LoanProduct> loanProducts = seedLoanProducts();
        List<Member> members = seedMembers(branches);
        List<SavingsAccount> savingsAccounts = seedSavingsAccounts(members, savingsProducts, branches);
        List<LoanAccount> loanAccounts = seedLoanAccounts(members, loanProducts, branches);
        seedTransactions(members, savingsAccounts, loanAccounts, branches);

        log.info("=== Data seeding complete ===");
        log.info("Branches: {}, Members: {}, Savings Accounts: {}, Loan Accounts: {}, Transactions: {}",
                branches.size(), members.size(), savingsAccounts.size(), loanAccounts.size(),
                transactionRepository.count());
    }

    private List<Branch> seedBranches() {
        List<Branch> branches = new ArrayList<>();

        String[][] branchData = {
                {"BR001", "Iseke Main Branch", "Dodoma CBD, Dodoma", "+255262350001", "main@isekesaccos.co.tz", "Francis Sang'wa"},
                {"BR002", "Kondoa Branch", "Kondoa Town, Dodoma", "+255262350002", "kondoa@isekesaccos.co.tz", "Anna Mushi"},
                {"BR003", "Chamwino Branch", "Chamwino, Dodoma", "+255262350003", "chamwino@isekesaccos.co.tz", "Peter Kimaro"},
        };

        for (String[] b : branchData) {
            Branch branch = branchRepository.findByBranchCode(b[0]).orElseGet(() ->
                branchRepository.save(Branch.builder()
                    .branchCode(b[0])
                    .branchName(b[1])
                    .address(b[2])
                    .phoneNumber(b[3])
                    .email(b[4])
                    .managerName(b[5])
                    .openingDate(LocalDate.of(2020, 1, 15))
                    .status(BranchStatus.ACTIVE)
                    .build()));
            branches.add(branch);
        }

        log.info("Seeded {} branches", branches.size());
        return branches;
    }

    private void seedUsers(List<Branch> branches) {
        String[][] userData = {
                {"manager1", "Rose", "Mbeki", "manager1@isekesaccos.co.tz", "MANAGER"},
                {"cashier1", "Amina", "Hassan", "cashier1@isekesaccos.co.tz", "CASHIER"},
                {"cashier2", "Grace", "Mwenda", "cashier2@isekesaccos.co.tz", "CASHIER"},
                {"loanofficer1", "Joseph", "Kilimo", "loanofficer1@isekesaccos.co.tz", "LOAN_OFFICER"},
                {"accountant1", "Mary", "Ngowi", "accountant1@isekesaccos.co.tz", "ACCOUNTANT"},
        };

        for (int i = 0; i < userData.length; i++) {
            String[] u = userData[i];
            if (userRepository.findByUsername(u[0]).isEmpty()) {
                userRepository.save(User.builder()
                        .username(u[0])
                        .passwordHash(passwordEncoder.encode("password123"))
                        .firstName(u[1])
                        .lastName(u[2])
                        .email(u[3])
                        .phoneNumber("+25576200000" + i)
                        .role(UserRole.valueOf(u[4]))
                        .isActive(true)
                        .branch(branches.get(i % branches.size()))
                        .build());
            }
        }
        log.info("Seeded 5 users");
    }

    private List<SavingsProduct> seedSavingsProducts() {
        List<SavingsProduct> products = new ArrayList<>();

        // Look up GL accounts (may not exist if chart_of_accounts_setup.sql wasn't run)
        ChartOfAccounts liabilityAccount = chartOfAccountsRepository.findByAccountCode("2101").orElse(null);
        ChartOfAccounts cashAccount = chartOfAccountsRepository.findByAccountCode("1101").orElse(null);
        ChartOfAccounts interestExpAccount = chartOfAccountsRepository.findByAccountCode("5101").orElse(null);
        ChartOfAccounts feeIncAccount = chartOfAccountsRepository.findByAccountCode("4201").orElse(null);

        products.add(savingsProductRepository.findByProductCode("SAV001").orElseGet(() ->
            savingsProductRepository.save(SavingsProduct.builder()
                .productCode("SAV001")
                .productName("Ordinary Savings")
                .productType(SavingsProductType.SAVINGS)
                .description("Standard savings account for all members")
                .interestRate(new BigDecimal("0.0500"))
                .minimumBalance(new BigDecimal("10000.00"))
                .minimumOpeningBalance(new BigDecimal("20000.00"))
                .maximumBalance(new BigDecimal("100000000.00"))
                .withdrawalLimit(5)
                .withdrawalFee(new BigDecimal("500.00"))
                .taxWithholdingRate(new BigDecimal("0.1000"))
                .liabilityAccount(liabilityAccount)
                .cashAccount(cashAccount)
                .interestExpenseAccount(interestExpAccount)
                .feeIncomeAccount(feeIncAccount)
                .status(ProductStatus.ACTIVE)
                .build())));

        products.add(savingsProductRepository.findByProductCode("FD001").orElseGet(() ->
            savingsProductRepository.save(SavingsProduct.builder()
                .productCode("FD001")
                .productName("Fixed Deposit - 12 Months")
                .productType(SavingsProductType.FIXED_DEPOSIT)
                .description("12-month fixed deposit with higher interest")
                .interestRate(new BigDecimal("0.0900"))
                .minimumBalance(new BigDecimal("100000.00"))
                .minimumOpeningBalance(new BigDecimal("100000.00"))
                .maximumBalance(new BigDecimal("500000000.00"))
                .taxWithholdingRate(new BigDecimal("0.1000"))
                .liabilityAccount(liabilityAccount != null ? chartOfAccountsRepository.findByAccountCode("2102").orElse(liabilityAccount) : null)
                .cashAccount(cashAccount)
                .interestExpenseAccount(interestExpAccount != null ? chartOfAccountsRepository.findByAccountCode("5102").orElse(interestExpAccount) : null)
                .feeIncomeAccount(feeIncAccount)
                .status(ProductStatus.ACTIVE)
                .build())));

        products.add(savingsProductRepository.findByProductCode("SHR001").orElseGet(() ->
            savingsProductRepository.save(SavingsProduct.builder()
                .productCode("SHR001")
                .productName("Member Shares")
                .productType(SavingsProductType.SHARES)
                .description("Share capital contributions")
                .interestRate(BigDecimal.ZERO)
                .minimumBalance(new BigDecimal("50000.00"))
                .minimumOpeningBalance(new BigDecimal("50000.00"))
                .maximumBalance(new BigDecimal("50000000.00"))
                .liabilityAccount(liabilityAccount != null ? chartOfAccountsRepository.findByAccountCode("2103").orElse(liabilityAccount) : null)
                .cashAccount(cashAccount)
                .status(ProductStatus.ACTIVE)
                .build())));

        log.info("Seeded {} savings products", products.size());
        return products;
    }

    private List<LoanProduct> seedLoanProducts() {
        List<LoanProduct> products = new ArrayList<>();

        ChartOfAccounts loanReceivable = chartOfAccountsRepository.findByAccountCode("1201").orElse(null);
        ChartOfAccounts cashAccount = chartOfAccountsRepository.findByAccountCode("1101").orElse(null);
        ChartOfAccounts interestIncome = chartOfAccountsRepository.findByAccountCode("4101").orElse(null);
        ChartOfAccounts feeIncome = chartOfAccountsRepository.findByAccountCode("4201").orElse(null);
        ChartOfAccounts penaltyIncome = chartOfAccountsRepository.findByAccountCode("4102").orElse(null);

        products.add(loanProductRepository.findByProductCode("LN001").orElseGet(() -> loanProductRepository.save(LoanProduct.builder()
                .productCode("LN001")
                .productName("Emergency Loan")
                .description("Short-term emergency loan up to 3x savings")
                .interestRate(new BigDecimal("0.1200"))
                .interestMethod(InterestMethod.REDUCING_BALANCE)
                .minimumAmount(new BigDecimal("50000.00"))
                .maximumAmount(new BigDecimal("5000000.00"))
                .minimumTermMonths(1)
                .maximumTermMonths(12)
                .processingFeeRate(new BigDecimal("0.0200"))
                .latePaymentPenaltyRate(new BigDecimal("0.0200"))
                .gracePeriodDays(7)
                .requiresGuarantors(true)
                .minimumGuarantors(1)
                .loanReceivableAccount(loanReceivable)
                .cashAccount(cashAccount)
                .interestIncomeAccount(interestIncome)
                .feeIncomeAccount(feeIncome)
                .penaltyIncomeAccount(penaltyIncome)
                .status(ProductStatus.ACTIVE)
                .build())));

        products.add(loanProductRepository.findByProductCode("LN002").orElseGet(() -> loanProductRepository.save(LoanProduct.builder()
                .productCode("LN002")
                .productName("Development Loan")
                .description("Medium-term loan for business and personal development")
                .interestRate(new BigDecimal("0.1800"))
                .interestMethod(InterestMethod.REDUCING_BALANCE)
                .minimumAmount(new BigDecimal("500000.00"))
                .maximumAmount(new BigDecimal("50000000.00"))
                .minimumTermMonths(6)
                .maximumTermMonths(36)
                .processingFeeRate(new BigDecimal("0.0300"))
                .latePaymentPenaltyRate(new BigDecimal("0.0150"))
                .gracePeriodDays(14)
                .requiresGuarantors(true)
                .minimumGuarantors(2)
                .loanReceivableAccount(loanReceivable)
                .cashAccount(cashAccount)
                .interestIncomeAccount(interestIncome)
                .feeIncomeAccount(feeIncome)
                .penaltyIncomeAccount(penaltyIncome)
                .status(ProductStatus.ACTIVE)
                .build())));

        products.add(loanProductRepository.findByProductCode("LN003").orElseGet(() -> loanProductRepository.save(LoanProduct.builder()
                .productCode("LN003")
                .productName("Education Loan")
                .description("Special loan for education-related expenses")
                .interestRate(new BigDecimal("0.1000"))
                .interestMethod(InterestMethod.FLAT)
                .minimumAmount(new BigDecimal("200000.00"))
                .maximumAmount(new BigDecimal("10000000.00"))
                .minimumTermMonths(3)
                .maximumTermMonths(24)
                .processingFeeRate(new BigDecimal("0.0100"))
                .latePaymentPenaltyRate(new BigDecimal("0.0100"))
                .gracePeriodDays(30)
                .requiresGuarantors(true)
                .minimumGuarantors(1)
                .requiresCollateral(false)
                .loanReceivableAccount(loanReceivable)
                .cashAccount(cashAccount)
                .interestIncomeAccount(interestIncome)
                .feeIncomeAccount(feeIncome)
                .penaltyIncomeAccount(penaltyIncome)
                .status(ProductStatus.ACTIVE)
                .build())));

        log.info("Seeded {} loan products", products.size());
        return products;
    }

    private List<Member> seedMembers(List<Branch> branches) {
        List<Member> members = new ArrayList<>();

        String[][] memberData = {
                {"Juma", "Hassan", "Mwinyi", "MALE", "MARRIED", "Teacher", "Dodoma Secondary School"},
                {"Fatuma", null, "Abdallah", "FEMALE", "SINGLE", "Nurse", "Dodoma Regional Hospital"},
                {"Baraka", "John", "Kimaro", "MALE", "MARRIED", "Farmer", "Self-Employed"},
                {"Saida", null, "Mwanga", "FEMALE", "MARRIED", "Trader", "Dodoma Central Market"},
                {"Emmanuel", "Peter", "Ngowi", "MALE", "SINGLE", "Engineer", "TANROADS"},
                {"Halima", "Said", "Omar", "FEMALE", "MARRIED", "Accountant", "Dodoma City Council"},
                {"Rajabu", null, "Selemani", "MALE", "MARRIED", "Driver", "DUMA Bus Company"},
                {"Anna", "Joseph", "Mushi", "FEMALE", "DIVORCED", "Tailor", "Self-Employed"},
                {"Michael", null, "Shayo", "MALE", "MARRIED", "Mechanic", "Self-Employed"},
                {"Neema", "Charles", "Urassa", "FEMALE", "SINGLE", "Pharmacist", "Dodoma Pharmacy"},
                {"Said", "Ali", "Bakari", "MALE", "MARRIED", "Carpenter", "Self-Employed"},
                {"Grace", null, "Mwenda", "FEMALE", "MARRIED", "Shopkeeper", "Iseke Mini Market"},
                {"Rashid", "Hamisi", "Juma", "MALE", "SINGLE", "Boda-boda Rider", "Self-Employed"},
                {"Witness", null, "Kiondo", "FEMALE", "MARRIED", "Farmer", "Self-Employed"},
                {"Daniel", "James", "Lema", "MALE", "MARRIED", "Lecturer", "University of Dodoma"},
                {"Amina", null, "Ramadhani", "FEMALE", "SINGLE", "Bank Clerk", "CRDB Bank"},
                {"Omari", "Juma", "Mhina", "MALE", "MARRIED", "Policeman", "Dodoma Central Police"},
                {"Joyce", "Frank", "Massawe", "FEMALE", "MARRIED", "Teacher", "Iseke Primary School"},
                {"Paschal", null, "Mtui", "MALE", "DIVORCED", "Businessman", "Mtui Enterprises"},
                {"Rehema", "Hassan", "Kipingu", "FEMALE", "MARRIED", "Cook", "Dodoma Hotel"},
        };

        for (int i = 0; i < memberData.length; i++) {
            String[] m = memberData[i];
            String memberNum = String.format("ISK%04d", i + 1);
            String nationalId = String.format("19%d0%d0%d%04d%05d", 75 + (i % 25), (i % 12) + 1, (i % 28) + 1, 0, 10000 + i);

            if (memberRepository.existsByMemberNumber(memberNum)) {
                members.add(memberRepository.findByMemberNumber(memberNum).get());
                continue;
            }

            members.add(memberRepository.save(Member.builder()
                    .memberNumber(memberNum)
                    .firstName(m[0])
                    .middleName(m[1])
                    .lastName(m[2])
                    .dateOfBirth(LocalDate.of(1975 + (i % 25), (i % 12) + 1, (i % 28) + 1))
                    .gender(Gender.valueOf(m[3]))
                    .nationalId(nationalId)
                    .phoneNumber(String.format("+25574%07d", 1000000 + i))
                    .email(m[0].toLowerCase() + "." + m[2].toLowerCase() + "@email.com")
                    .address("Ward " + ((i % 5) + 1) + ", " + (i < 10 ? "Dodoma" : i < 15 ? "Kondoa" : "Chamwino"))
                    .occupation(m[5])
                    .employer(m[6])
                    .monthlyIncome(new BigDecimal(300000 + random.nextInt(2000000)))
                    .maritalStatus(MaritalStatus.valueOf(m[4]))
                    .nextOfKinName(m[0] + "'s Relative")
                    .nextOfKinPhone(String.format("+25576%07d", 2000000 + i))
                    .nextOfKinRelationship(i % 2 == 0 ? "Spouse" : "Sibling")
                    .membershipDate(LocalDate.of(2022, (i % 12) + 1, (i % 28) + 1))
                    .status(i < 18 ? MemberStatus.ACTIVE : MemberStatus.INACTIVE)
                    .branch(branches.get(i % branches.size()))
                    .createdBy("admin")
                    .build()));
        }

        log.info("Seeded {} members", members.size());
        return members;
    }

    private List<SavingsAccount> seedSavingsAccounts(List<Member> members, List<SavingsProduct> savingsProducts, List<Branch> branches) {
        List<SavingsAccount> accounts = new ArrayList<>();
        SavingsProduct ordinarySavings = savingsProducts.get(0);
        SavingsProduct fixedDeposit = savingsProducts.get(1);
        SavingsProduct shares = savingsProducts.get(2);

        // Every member gets an ordinary savings account
        for (int i = 0; i < members.size(); i++) {
            Member member = members.get(i);
            String accNum = String.format("SAV%08d", i + 1);
            var existing = savingsAccountRepository.findByAccountNumber(accNum);
            if (existing.isPresent()) { accounts.add(existing.get()); continue; }
            BigDecimal balance = new BigDecimal(50000 + random.nextInt(5000000));

            accounts.add(savingsAccountRepository.save(SavingsAccount.builder()
                    .accountNumber(accNum)
                    .member(member)
                    .product(ordinarySavings)
                    .branch(branches.get(i % branches.size()))
                    .openingDate(member.getMembershipDate().plusDays(1))
                    .balance(balance)
                    .availableBalance(balance)
                    .accruedInterest(new BigDecimal(random.nextInt(10000)))
                    .lastTransactionDate(LocalDate.now().minusDays(random.nextInt(30)))
                    .status(i < 18 ? AccountStatus.ACTIVE : AccountStatus.INACTIVE)
                    .build()));
        }

        // Some members get a shares account
        for (int i = 0; i < 10; i++) {
            Member member = members.get(i);
            String shrNum = String.format("SHR%08d", i + 1);
            var existingShr = savingsAccountRepository.findByAccountNumber(shrNum);
            if (existingShr.isPresent()) { accounts.add(existingShr.get()); continue; }
            BigDecimal shareBalance = new BigDecimal(50000 * (1 + random.nextInt(10)));

            accounts.add(savingsAccountRepository.save(SavingsAccount.builder()
                    .accountNumber(shrNum)
                    .member(member)
                    .product(shares)
                    .branch(branches.get(i % branches.size()))
                    .openingDate(member.getMembershipDate().plusDays(5))
                    .balance(shareBalance)
                    .availableBalance(shareBalance)
                    .status(AccountStatus.ACTIVE)
                    .build()));
        }

        // A few fixed deposits
        for (int i = 0; i < 5; i++) {
            Member member = members.get(i);
            String fdNum = String.format("FD%09d", i + 1);
            var existingFd = savingsAccountRepository.findByAccountNumber(fdNum);
            if (existingFd.isPresent()) { accounts.add(existingFd.get()); continue; }
            BigDecimal fdBalance = new BigDecimal(500000 + random.nextInt(5000000));

            accounts.add(savingsAccountRepository.save(SavingsAccount.builder()
                    .accountNumber(fdNum)
                    .member(member)
                    .product(fixedDeposit)
                    .branch(branches.get(0))
                    .openingDate(LocalDate.now().minusMonths(6).plusDays(i * 10))
                    .balance(fdBalance)
                    .availableBalance(BigDecimal.ZERO) // Fixed deposits not available for withdrawal
                    .maturityDate(LocalDate.now().plusMonths(6).plusDays(i * 10))
                    .status(AccountStatus.ACTIVE)
                    .build()));
        }

        log.info("Seeded {} savings accounts", accounts.size());
        return accounts;
    }

    private List<LoanAccount> seedLoanAccounts(List<Member> members, List<LoanProduct> loanProducts, List<Branch> branches) {
        List<LoanAccount> loans = new ArrayList<>();

        Object[][] loanData = {
                {0, 0, 2000000, 12, "ACTIVE", 15, "Business expansion"},
                {1, 1, 5000000, 24, "ACTIVE", 0, "House construction"},
                {2, 0, 1000000, 6, "ACTIVE", 45, "Medical emergency"},
                {3, 2, 3000000, 18, "ACTIVE", 0, "School fees"},
                {4, 1, 8000000, 36, "ACTIVE", 90, "Commercial farming"},
                {5, 0, 500000, 3, "CLOSED", 0, "Emergency"},
                {6, 1, 10000000, 24, "DISBURSED", 5, "Business startup"},
                {7, 2, 1500000, 12, "ACTIVE", 0, "Children's education"},
                {8, 0, 3000000, 12, "APPROVED", 0, "Motorcycle purchase"},
                {9, 1, 15000000, 36, "APPLIED", 0, "Land purchase"},
                {10, 0, 800000, 6, "ACTIVE", 120, "Personal needs"},
                {11, 2, 2000000, 12, "ACTIVE", 0, "College fees"},
                {12, 0, 400000, 3, "ACTIVE", 200, "Emergency"},
                {13, 1, 6000000, 24, "ACTIVE", 30, "Farming equipment"},
                {14, 0, 1200000, 6, "ACTIVE", 0, "Home repair"},
                {15, 1, 4000000, 18, "ACTIVE", 60, "Business expansion"},
                {16, 2, 5000000, 24, "DISBURSED", 0, "Masters degree"},
                {17, 0, 700000, 6, "ACTIVE", 10, "Medical bills"},
                {18, 1, 3500000, 12, "ACTIVE", 0, "Shop renovation"},
                {19, 0, 900000, 6, "ACTIVE", 0, "Emergency fund"},
        };

        for (int i = 0; i < loanData.length; i++) {
            Object[] ld = loanData[i];
            int memberIdx = (int) ld[0];
            int productIdx = (int) ld[1];
            int principal = (int) ld[2];
            int termMonths = (int) ld[3];
            String statusStr = (String) ld[4];
            int daysInArrears = (int) ld[5];
            String purpose = (String) ld[6];

            String loanNum = String.format("LN%09d", i + 1);
            var existingLoan = loanAccountRepository.findByLoanNumber(loanNum);
            if (existingLoan.isPresent()) { loans.add(existingLoan.get()); continue; }

            LoanStatus status = LoanStatus.valueOf(statusStr);
            BigDecimal principalAmt = new BigDecimal(principal);
            BigDecimal outstanding = status == LoanStatus.CLOSED ? BigDecimal.ZERO :
                    status == LoanStatus.APPLIED || status == LoanStatus.APPROVED ? principalAmt :
                            principalAmt.multiply(new BigDecimal("0." + (30 + random.nextInt(70))));
            BigDecimal totalPaid = principalAmt.subtract(outstanding).max(BigDecimal.ZERO);

            LocalDate appDate = LocalDate.now().minusMonths(termMonths + 1).plusDays(i * 3);
            LocalDate approvalDate = (status != LoanStatus.APPLIED) ? appDate.plusDays(3) : null;
            LocalDate disbDate = (status == LoanStatus.DISBURSED || status == LoanStatus.ACTIVE || status == LoanStatus.CLOSED)
                    ? appDate.plusDays(7) : null;

            loans.add(loanAccountRepository.save(LoanAccount.builder()
                    .loanNumber(loanNum)
                    .member(members.get(memberIdx))
                    .product(loanProducts.get(productIdx))
                    .branch(branches.get(memberIdx % branches.size()))
                    .applicationDate(appDate)
                    .approvalDate(approvalDate)
                    .disbursementDate(disbDate)
                    .principalAmount(principalAmt)
                    .interestRate(loanProducts.get(productIdx).getInterestRate())
                    .termMonths(termMonths)
                    .repaymentFrequency("MONTHLY")
                    .outstandingPrincipal(outstanding)
                    .outstandingInterest(outstanding.multiply(new BigDecimal("0.02")))
                    .totalPaid(totalPaid)
                    .nextPaymentDate(disbDate != null ? disbDate.plusMonths(1) : null)
                    .maturityDate(disbDate != null ? disbDate.plusMonths(termMonths) : null)
                    .status(status)
                    .loanOfficer("Joseph Kilimo")
                    .purpose(purpose)
                    .daysInArrears(daysInArrears)
                    .build()));
        }

        log.info("Seeded {} loan accounts", loans.size());
        return loans;
    }

    private void seedTransactions(List<Member> members, List<SavingsAccount> savingsAccounts,
                                  List<LoanAccount> loanAccounts, List<Branch> branches) {
        List<Transaction> transactions = new ArrayList<>();

        // Create 20 recent transactions across different types
        Object[][] txData = {
                {0, "DEPOSIT", 500000, "CASH", "Monthly savings deposit"},
                {1, "DEPOSIT", 1200000, "BANK_TRANSFER", "Salary savings"},
                {2, "WITHDRAWAL", 150000, "CASH", "Personal expenses"},
                {3, "DEPOSIT", 300000, "MOBILE_MONEY", "Business income deposit"},
                {4, "DEPOSIT", 2000000, "BANK_TRANSFER", "Project payment deposit"},
                {5, "WITHDRAWAL", 500000, "CASH", "School fees payment"},
                {6, "DEPOSIT", 750000, "CASH", "Farming proceeds deposit"},
                {7, "WITHDRAWAL", 200000, "MOBILE_MONEY", "Medical expenses"},
                {8, "DEPOSIT", 400000, "CASH", "Weekly savings"},
                {9, "DEPOSIT", 1500000, "BANK_TRANSFER", "Lump sum deposit"},
                {10, "WITHDRAWAL", 300000, "CASH", "Rent payment"},
                {11, "DEPOSIT", 600000, "MOBILE_MONEY", "Side business income"},
                {0, "LOAN_REPAYMENT", 180000, "CASH", "Monthly loan repayment"},
                {1, "LOAN_REPAYMENT", 350000, "BANK_TRANSFER", "Loan installment payment"},
                {3, "LOAN_REPAYMENT", 200000, "MOBILE_MONEY", "Loan repayment"},
                {12, "DEPOSIT", 100000, "CASH", "Small savings"},
                {13, "DEPOSIT", 800000, "BANK_TRANSFER", "Farm harvest proceeds"},
                {14, "WITHDRAWAL", 450000, "CASH", "Building materials"},
                {15, "DEPOSIT", 250000, "CASH", "Regular savings"},
                {16, "DEPOSIT", 1800000, "BANK_TRANSFER", "Scholarship allowance savings"},
        };

        for (int i = 0; i < txData.length; i++) {
            Object[] td = txData[i];
            String txnNum = "TXN" + String.format("%010d", i + 1);
            if (transactionRepository.findByTransactionNumber(txnNum).isPresent()) { continue; }

            int memberIdx = (int) td[0];
            TransactionType type = TransactionType.valueOf((String) td[1]);
            BigDecimal amount = new BigDecimal((int) td[2]);
            PaymentMethod method = PaymentMethod.valueOf((String) td[3]);
            String desc = (String) td[4];

            Member member = members.get(memberIdx);
            // Use the member's ordinary savings account (first 20 are ordinary savings, one per member)
            SavingsAccount account = savingsAccounts.get(memberIdx);
            LoanAccount loanAccount = null;

            BigDecimal balanceBefore = account.getBalance();
            BigDecimal balanceAfter;

            if (type == TransactionType.DEPOSIT) {
                balanceAfter = balanceBefore.add(amount);
            } else if (type == TransactionType.WITHDRAWAL) {
                balanceAfter = balanceBefore.subtract(amount);
            } else if (type == TransactionType.LOAN_REPAYMENT) {
                balanceAfter = balanceBefore; // Loan repayment doesn't affect savings balance
                // Find a loan for this member
                for (LoanAccount la : loanAccounts) {
                    if (la.getMember().getId().equals(member.getId()) &&
                            (la.getStatus() == LoanStatus.ACTIVE || la.getStatus() == LoanStatus.DISBURSED)) {
                        loanAccount = la;
                        break;
                    }
                }
            } else {
                balanceAfter = balanceBefore;
            }

            LocalDate txDate = LocalDate.now().minusDays(txData.length - i);
            TransactionStatus txStatus = TransactionStatus.COMPLETED;

            Transaction tx = Transaction.builder()
                    .transactionNumber(txnNum)
                    .transactionDate(txDate)
                    .transactionTime(LocalDateTime.of(txDate, java.time.LocalTime.of(9 + (i % 8), (i * 7) % 60)))
                    .transactionType(type)
                    .member(member)
                    .amount(amount)
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .description(desc)
                    .referenceNumber("REF" + String.format("%08d", 10000 + i))
                    .paymentMethod(method)
                    .processedBy("cashier1")
                    .branch(branches.get(memberIdx % branches.size()))
                    .status(txStatus)
                    .savingsAccount(type != TransactionType.LOAN_REPAYMENT ? account : null)
                    .loanAccount(loanAccount)
                    .build();

            transactions.add(transactionRepository.save(tx));
        }

        log.info("Seeded {} transactions", transactions.size());
    }
}
