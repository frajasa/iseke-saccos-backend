package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tz.co.iseke.entity.ChartOfAccounts;
import tz.co.iseke.entity.SavingsProduct;
import tz.co.iseke.repository.ChartOfAccountsRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaxService {

    private final ChartOfAccountsRepository chartRepository;

    public BigDecimal calculateWithholdingTax(BigDecimal interestAmount, SavingsProduct product) {
        if (product.getTaxWithholdingRate() == null
                || product.getTaxWithholdingRate().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return interestAmount.multiply(product.getTaxWithholdingRate())
                .setScale(2, RoundingMode.HALF_UP);
    }

    public ChartOfAccounts getTaxPayableAccount() {
        return chartRepository.findByAccountCode("2301")
                .orElse(null);
    }
}
