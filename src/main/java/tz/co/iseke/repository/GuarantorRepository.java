package tz.co.iseke.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.Guarantor;

import java.util.List;
import java.util.UUID;

@Repository
public interface GuarantorRepository extends JpaRepository<Guarantor, UUID> {
    List<Guarantor> findByLoanAccount_Id(UUID loanAccountId);
    List<Guarantor> findByGuarantorMemberId(UUID guarantorMemberId);

    @Query("SELECT g FROM Guarantor g WHERE g.loanAccount.id = :loanId AND g.status = 'ACTIVE'")
    List<Guarantor> findActiveGuarantorsByLoanId(@Param("loanId") UUID loanId);
}