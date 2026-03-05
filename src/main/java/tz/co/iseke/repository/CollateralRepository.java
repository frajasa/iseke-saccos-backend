package tz.co.iseke.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.Collateral;

import java.util.List;
import java.util.UUID;

@Repository
public interface CollateralRepository extends JpaRepository<Collateral, UUID> {
    List<Collateral> findByLoanAccount_Id(UUID loanId);

    @Query("SELECT c FROM Collateral c WHERE c.loanAccount.id = :loanId AND c.status = 'ACTIVE'")
    List<Collateral> findActiveCollateralByLoanId(@Param("loanId") UUID loanId);
}