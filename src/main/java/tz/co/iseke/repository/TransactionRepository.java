package tz.co.iseke.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.Transaction;
import tz.co.iseke.enums.TransactionType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByTransactionNumber(String transactionNumber);
    List<Transaction> findByMemberId(UUID memberId);
    List<Transaction> findBySavingsAccount_Id(UUID accountId);
    List<Transaction> findByLoanAccount_Id(UUID loanId);

    @Query("SELECT t FROM Transaction t WHERE t.member.id = :memberId AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findMemberTransactionsBetweenDates(@Param("memberId") UUID memberId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.savingsAccount.id = :accountId AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findAccountTransactionsBetweenDates(@Param("accountId") UUID accountId,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.transactionDate = :date")
    List<Transaction> findByTransactionDate(@Param("date") LocalDate date);

    @Query("SELECT t FROM Transaction t WHERE t.branch.id = :branchId AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findBranchTransactionsBetweenDates(@Param("branchId") UUID branchId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.transactionType = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    java.math.BigDecimal sumByTypeAndDateRange(@Param("type") TransactionType type,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.savingsAccount.id = :accountId AND t.transactionType = :type AND t.transactionDate BETWEEN :startDate AND :endDate AND t.status = 'COMPLETED'")
    long countByAccountAndTypeInPeriod(@Param("accountId") UUID accountId,
                                       @Param("type") TransactionType type,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);
}