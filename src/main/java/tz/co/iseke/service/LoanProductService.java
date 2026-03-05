package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.inputs.CreateLoanProductInput;
import tz.co.iseke.inputs.UpdateLoanProductInput;
import tz.co.iseke.entity.LoanProduct;
import tz.co.iseke.enums.ProductStatus;
import tz.co.iseke.exception.BusinessException;
import tz.co.iseke.exception.ResourceNotFoundException;
import tz.co.iseke.repository.LoanProductRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
public class LoanProductService {

    private final LoanProductRepository repository;

    public List<LoanProduct> findAllActive() {
        return repository.findByStatus(ProductStatus.ACTIVE);
    }

    public LoanProduct findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found with id: " + id));
    }

    public LoanProduct createProduct(CreateLoanProductInput input) {
        // Validate product code uniqueness
        repository.findByProductCode(input.getProductCode()).ifPresent(p -> {
            throw new BusinessException("Product code already exists: " + input.getProductCode());
        });

        // Validate interest rate
        if (input.getInterestRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Interest rate must be greater than zero");
        }

        // Validate amounts
        if (input.getMinimumAmount().compareTo(input.getMaximumAmount()) > 0) {
            throw new BusinessException("Minimum amount cannot be greater than maximum amount");
        }

        // Validate term
        if (input.getMinimumTermMonths() > input.getMaximumTermMonths()) {
            throw new BusinessException("Minimum term cannot be greater than maximum term");
        }

        LoanProduct product = LoanProduct.builder()
                .productCode(input.getProductCode())
                .productName(input.getProductName())
                .description(input.getDescription())
                .interestRate(input.getInterestRate())
                .interestMethod(input.getInterestMethod())
                .repaymentFrequency(input.getRepaymentFrequency() != null ?
                        input.getRepaymentFrequency() : "MONTHLY")
                .minimumAmount(input.getMinimumAmount())
                .maximumAmount(input.getMaximumAmount())
                .minimumTermMonths(input.getMinimumTermMonths())
                .maximumTermMonths(input.getMaximumTermMonths())
                .processingFeeRate(input.getProcessingFeeRate())
                .processingFeeFixed(input.getProcessingFeeFixed())
                .insuranceFeeRate(input.getInsuranceFeeRate())
                .latePaymentPenaltyRate(input.getLatePaymentPenaltyRate())
                .gracePeriodDays(input.getGracePeriodDays() != null ?
                        input.getGracePeriodDays() : 0)
                .requiresGuarantors(input.getRequiresGuarantors() != null ?
                        input.getRequiresGuarantors() : true)
                .minimumGuarantors(input.getMinimumGuarantors() != null ?
                        input.getMinimumGuarantors() : 1)
                .requiresCollateral(input.getRequiresCollateral() != null ?
                        input.getRequiresCollateral() : false)
                .collateralPercentage(input.getCollateralPercentage())
                .status(ProductStatus.ACTIVE)
                .build();

        return repository.save(product);
    }
    public LoanProduct updateProduct(UUID id, UpdateLoanProductInput input) {
        LoanProduct product =  repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found with id: " + id));

        if (input.getProductName() != null) product.setProductName(input.getProductName());
        if (input.getDescription() != null) product.setDescription(input.getDescription());
        if (input.getInterestRate() != null) product.setInterestRate(input.getInterestRate());
        if (input.getProcessingFeeRate() != null) product.setProcessingFeeRate(input.getProcessingFeeRate());
        if (input.getLatePaymentPenaltyRate() != null) product.setLatePaymentPenaltyRate(input.getLatePaymentPenaltyRate());
        if (input.getStatus() != null) product.setStatus(input.getStatus());

        product.setUpdatedAt(LocalDateTime.now());

        return repository.save(product);
    }
}
