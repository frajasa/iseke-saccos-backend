package tz.co.iseke.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.LoanProduct;
import tz.co.iseke.enums.ProductStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, UUID> {
    Optional<LoanProduct> findByProductCode(String productCode);
    List<LoanProduct> findByStatus(ProductStatus status);
}