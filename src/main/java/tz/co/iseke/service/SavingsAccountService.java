package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.inputs.CreateSavingsAccountInput;
import tz.co.iseke.inputs.DepositInput;
import tz.co.iseke.entity.Branch;
import tz.co.iseke.entity.Member;
import tz.co.iseke.entity.SavingsAccount;
import tz.co.iseke.entity.SavingsProduct;
import tz.co.iseke.enums.AccountStatus;
import tz.co.iseke.enums.PaymentMethod;
import tz.co.iseke.enums.SavingsProductType;
import tz.co.iseke.exception.BusinessException;
import tz.co.iseke.exception.ResourceNotFoundException;
import tz.co.iseke.repository.BranchRepository;
import tz.co.iseke.repository.MemberRepository;
import tz.co.iseke.repository.SavingsAccountRepository;
import tz.co.iseke.repository.SavingsProductRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SavingsAccountService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final MemberRepository memberRepository;
    private final SavingsProductRepository savingsProductRepository;
    private final BranchRepository branchRepository;
    private final TransactionService transactionService;

    public SavingsAccount findById(UUID id) {
        return savingsAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found with id: " + id));
    }

    public SavingsAccount findByAccountNumber(String accountNumber) {
        return savingsAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with number: " + accountNumber));
    }

    public List<SavingsAccount> findByMemberId(UUID memberId) {
        return savingsAccountRepository.findByMemberId(memberId);
    }

    public SavingsAccount openAccount(CreateSavingsAccountInput input) {
        Member member = memberRepository.findById(input.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        SavingsProduct product = savingsProductRepository.findById(input.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings product not found"));

        // Validate minimum opening balance
        if (input.getOpeningDeposit().compareTo(product.getMinimumOpeningBalance()) < 0) {
            throw new BusinessException("Opening deposit must be at least " + product.getMinimumOpeningBalance());
        }

        Branch branch = null;
        if (input.getBranchId() != null) {
            branch = branchRepository.findById(input.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        }

        String accountNumber = generateAccountNumber(product.getProductType());

        SavingsAccount account = SavingsAccount.builder()
                .accountNumber(accountNumber)
                .member(member)
                .product(product)
                .branch(branch)
                .openingDate(LocalDate.now())
                .balance(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .beneficiaryName(input.getBeneficiaryName())
                .beneficiaryRelationship(input.getBeneficiaryRelationship())
                .status(AccountStatus.ACTIVE)
                .build();

        account = savingsAccountRepository.save(account);

        // Record opening deposit transaction
        DepositInput depositInput = DepositInput.builder()
                .accountId(account.getId())
                .amount(input.getOpeningDeposit())
                .paymentMethod(PaymentMethod.CASH)
                .description("Opening deposit")
                .build();

        transactionService.processDeposit(depositInput);

        return account;
    }

    public SavingsAccount closeAccount(UUID id) {
        SavingsAccount account = findById(id);

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Cannot close account with positive balance");
        }

        account.setStatus(AccountStatus.CLOSED);
        account.setUpdatedAt(LocalDateTime.now());

        return savingsAccountRepository.save(account);
    }

    public void updateBalance(UUID accountId, BigDecimal amount, boolean isCredit) {
        SavingsAccount account = findById(accountId);

        BigDecimal newBalance = isCredit
                ? account.getBalance().add(amount)
                : account.getBalance().subtract(amount);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Insufficient balance");
        }

        account.setBalance(newBalance);
        account.setAvailableBalance(newBalance);
        account.setLastTransactionDate(LocalDate.now());
        account.setUpdatedAt(LocalDateTime.now());

        savingsAccountRepository.save(account);
    }

    private String generateAccountNumber(SavingsProductType type) {
        String prefix = switch (type) {
            case SAVINGS -> "SAV";
            case FIXED_DEPOSIT -> "FD";
            case SHARES -> "SHR";
            case CHECKING -> "CHK";
            case CURRENT -> "CUR";
        };

        return String.format("%s%s", prefix, UUID.randomUUID().toString().substring(0, 10).toUpperCase());
    }
}