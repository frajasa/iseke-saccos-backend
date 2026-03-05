package tz.co.iseke.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.SavingsAccount;
import tz.co.iseke.enums.AccountStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, UUID> {
    Optional<SavingsAccount> findByAccountNumber(String accountNumber);
    List<SavingsAccount> findByMemberId(UUID memberId);
    List<SavingsAccount> findByStatus(AccountStatus status);
    List<SavingsAccount> findByBranchId(UUID branchId);

    @Query("SELECT sa FROM SavingsAccount sa WHERE sa.member.id = :memberId AND sa.status = :status")
    List<SavingsAccount> findByMemberIdAndStatus(@Param("memberId") UUID memberId,
                                                 @Param("status") AccountStatus status);

    @Query("SELECT COALESCE(SUM(sa.balance), 0) FROM SavingsAccount sa WHERE sa.status = 'ACTIVE'")
    java.math.BigDecimal getTotalSavingsBalance();

    @Query("SELECT COUNT(sa) FROM SavingsAccount sa WHERE sa.status = :status")
    Long countByStatus(@Param("status") AccountStatus status);
}