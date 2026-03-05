package tz.co.iseke.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.ChartOfAccounts;
import tz.co.iseke.enums.AccountType;
import tz.co.iseke.enums.ProductStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface ChartOfAccountsRepository extends JpaRepository<ChartOfAccounts, UUID> {
    Optional<ChartOfAccounts> findByAccountCode(String accountCode);
    List<ChartOfAccounts> findByAccountType(AccountType accountType);
    List<ChartOfAccounts> findByParentAccountId(UUID parentAccountId);
    List<ChartOfAccounts> findByStatus(ProductStatus status);

    @Query("SELECT coa FROM ChartOfAccounts coa WHERE coa.parentAccount IS NULL ORDER BY coa.accountCode")
    List<ChartOfAccounts> findRootAccounts();

    @Query("SELECT coa FROM ChartOfAccounts coa WHERE coa.level = :level ORDER BY coa.accountCode")
    List<ChartOfAccounts> findByLevel(@Param("level") Integer level);
}