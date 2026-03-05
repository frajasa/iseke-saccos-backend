package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.inputs.CreateSavingsProductInput;
import tz.co.iseke.inputs.UpdateSavingsProductInput;
import tz.co.iseke.entity.SavingsProduct;
import tz.co.iseke.enums.ProductStatus;
import tz.co.iseke.exception.BusinessException;
import tz.co.iseke.exception.ResourceNotFoundException;
import tz.co.iseke.repository.SavingsProductRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;


@Service
@RequiredArgsConstructor
@Transactional
public class SavingsProductService {

    private final SavingsProductRepository repository;

    public List<SavingsProduct> findAllActive() {
        return repository.findByStatus(ProductStatus.ACTIVE);
    }

    public SavingsProduct findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings product not found with id: " + id));
    }

    public SavingsProduct createProduct(CreateSavingsProductInput input) {
        // Validate product code uniqueness
        repository.findByProductCode(input.getProductCode()).ifPresent(p -> {
            throw new BusinessException("Product code already exists: " + input.getProductCode());
        });

        // Validate interest rate
        if (input.getInterestRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Interest rate cannot be negative");
        }

        // Validate minimum and maximum balance
        if (input.getMaximumBalance() != null &&
                input.getMinimumBalance().compareTo(input.getMaximumBalance()) > 0) {
            throw new BusinessException("Minimum balance cannot be greater than maximum balance");
        }

        SavingsProduct product = SavingsProduct.builder()
                .productCode(input.getProductCode())
                .productName(input.getProductName())
                .productType(input.getProductType())
                .description(input.getDescription())
                .interestRate(input.getInterestRate())
                .interestCalculationMethod(input.getInterestCalculationMethod() != null ?
                        input.getInterestCalculationMethod() : "DAILY_BALANCE")
                .interestPaymentFrequency(input.getInterestPaymentFrequency() != null ?
                        input.getInterestPaymentFrequency() : "MONTHLY")
                .minimumBalance(input.getMinimumBalance())
                .maximumBalance(input.getMaximumBalance())
                .minimumOpeningBalance(input.getMinimumOpeningBalance())
                .withdrawalLimit(input.getWithdrawalLimit())
                .withdrawalFee(input.getWithdrawalFee())
                .monthlyFee(input.getMonthlyFee())
                .taxWithholdingRate(input.getTaxWithholdingRate())
                .dormancyPeriodDays(input.getDormancyPeriodDays() != null ?
                        input.getDormancyPeriodDays() : 365)
                .allowsOverdraft(input.getAllowsOverdraft() != null ?
                        input.getAllowsOverdraft() : false)
                .status(ProductStatus.ACTIVE)
                .build();

        return repository.save(product);
    }

    public SavingsProduct updateProduct(UUID id, UpdateSavingsProductInput input) {

        SavingsProduct product = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings product not found with id: " + id));

        if (input.getProductCode() != null) product.setProductCode(input.getProductCode());
        if (input.getProductName() != null) product.setProductName(input.getProductName());
        if (input.getProductType() != null) product.setProductType(input.getProductType());
        if (input.getDescription() != null) product.setDescription(input.getDescription());
        if (input.getInterestRate() != null) product.setInterestRate(input.getInterestRate());
        if (input.getInterestCalculationMethod() != null)
            product.setInterestCalculationMethod(input.getInterestCalculationMethod());
        if (input.getInterestPaymentFrequency() != null)
            product.setInterestPaymentFrequency(input.getInterestPaymentFrequency());
        if (input.getMinimumBalance() != null) product.setMinimumBalance(input.getMinimumBalance());
        if (input.getMaximumBalance() != null) product.setMaximumBalance(input.getMaximumBalance());
        if (input.getMinimumOpeningBalance() != null)
            product.setMinimumOpeningBalance(input.getMinimumOpeningBalance());
        if (input.getWithdrawalLimit() != null) product.setWithdrawalLimit(input.getWithdrawalLimit());
        if (input.getWithdrawalFee() != null) product.setWithdrawalFee(input.getWithdrawalFee());
        if (input.getMonthlyFee() != null) product.setMonthlyFee(input.getMonthlyFee());
        if (input.getTaxWithholdingRate() != null)
            product.setTaxWithholdingRate(input.getTaxWithholdingRate());
        if (input.getDormancyPeriodDays() != null)
            product.setDormancyPeriodDays(input.getDormancyPeriodDays());
        if (input.getAllowsOverdraft() != null)
            product.setAllowsOverdraft(input.getAllowsOverdraft());
        if (input.getStatus() != null)
            product.setStatus(input.getStatus());

        product.setUpdatedAt(LocalDateTime.now());

        return repository.save(product);
    }

    @Transactional
    public void deactivateProduct(UUID id) {
        SavingsProduct product = findById(id);

        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new BusinessException("Product is already inactive.");
        }
        product.setStatus(ProductStatus.INACTIVE);
        product.setUpdatedAt(LocalDateTime.now());
        repository.save(product);
    }

}
