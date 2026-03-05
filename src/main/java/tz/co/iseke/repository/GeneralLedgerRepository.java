package tz.co.iseke.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.GeneralLedger;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
@Repository
public interface GeneralLedgerRepository extends JpaRepository<GeneralLedger, UUID> {
    List<GeneralLedger> findByAccountId(UUID accountId);
    List<GeneralLedger> findByTransactionId(UUID transactionId);

    @Query("SELECT gl FROM GeneralLedger gl WHERE gl.account.id = :accountId AND gl.postingDate BETWEEN :startDate AND :endDate ORDER BY gl.postingDate")
    List<GeneralLedger> findByAccountIdAndDateRange(@Param("accountId") UUID accountId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    @Query("SELECT gl FROM GeneralLedger gl WHERE gl.postingDate = :date")
    List<GeneralLedger> findByPostingDate(@Param("date") LocalDate date);

    @Query("SELECT gl FROM GeneralLedger gl WHERE gl.postingDate BETWEEN :startDate AND :endDate")
    List<GeneralLedger> findByDateRange(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(gl.debitAmount), 0) - COALESCE(SUM(gl.creditAmount), 0) FROM GeneralLedger gl WHERE gl.account.id = :accountId AND gl.postingDate <= :date")
    java.math.BigDecimal getAccountBalance(@Param("accountId") UUID accountId, @Param("date") LocalDate date);
}