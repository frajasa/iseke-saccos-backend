package tz.co.iseke.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.SavingsProduct;
import tz.co.iseke.enums.ProductStatus;
import tz.co.iseke.enums.SavingsProductType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavingsProductRepository extends JpaRepository<SavingsProduct, UUID> {
    Optional<SavingsProduct> findByProductCode(String productCode);
    List<SavingsProduct> findByStatus(ProductStatus status);
    List<SavingsProduct> findByProductType(SavingsProductType productType);
}