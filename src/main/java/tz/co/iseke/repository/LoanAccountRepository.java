package tz.co.iseke.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.LoanAccount;
import tz.co.iseke.enums.LoanStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface LoanAccountRepository extends JpaRepository<LoanAccount, UUID> {
    Optional<LoanAccount> findByLoanNumber(String loanNumber);
    List<LoanAccount> findByMemberId(UUID memberId);
    List<LoanAccount> findByStatus(LoanStatus status);
    Page<LoanAccount> findByStatus(LoanStatus status, Pageable pageable);
    List<LoanAccount> findByBranchId(UUID branchId);

    @Query("SELECT la FROM LoanAccount la WHERE la.member.id = :memberId AND la.status = :status")
    List<LoanAccount> findByMemberIdAndStatus(@Param("memberId") UUID memberId,
                                              @Param("status") LoanStatus status);

    @Query("SELECT la FROM LoanAccount la WHERE la.status IN ('DISBURSED', 'ACTIVE') AND la.daysInArrears > 0")
    List<LoanAccount> findDelinquentLoans();

    @Query("SELECT la FROM LoanAccount la WHERE la.status IN ('DISBURSED', 'ACTIVE') AND la.daysInArrears >= :days")
    List<LoanAccount> findLoansByDaysInArrears(@Param("days") Integer days);

    @Query("SELECT COALESCE(SUM(la.outstandingPrincipal), 0) FROM LoanAccount la WHERE la.status IN ('DISBURSED', 'ACTIVE')")
    java.math.BigDecimal getTotalOutstandingPrincipal();

    @Query("SELECT COUNT(la) FROM LoanAccount la WHERE la.status = :status")
    Long countByStatus(@Param("status") LoanStatus status);

    @Query("SELECT la FROM LoanAccount la WHERE la.nextPaymentDate BETWEEN :startDate AND :endDate")
    List<LoanAccount> findLoansWithPaymentsDue(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);
}