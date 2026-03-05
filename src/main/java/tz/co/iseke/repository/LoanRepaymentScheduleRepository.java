package tz.co.iseke.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.LoanRepaymentSchedule;
import tz.co.iseke.enums.ScheduleStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRepaymentScheduleRepository extends JpaRepository<LoanRepaymentSchedule, UUID> {
    List<LoanRepaymentSchedule> findByLoanIdOrderByInstallmentNumber(UUID loanId);

    Page<LoanRepaymentSchedule> findByLoanIdOrderByInstallmentNumber(UUID loanId, Pageable pageable);

    @Query("SELECT lrs FROM LoanRepaymentSchedule lrs WHERE lrs.loan.id = :loanId AND lrs.status = :status")
    List<LoanRepaymentSchedule> findByLoanIdAndStatus(@Param("loanId") UUID loanId,
                                                      @Param("status") ScheduleStatus status);

    @Query("SELECT lrs FROM LoanRepaymentSchedule lrs WHERE lrs.loan.id = :loanId AND lrs.status IN ('PENDING', 'PARTIAL', 'OVERDUE') ORDER BY lrs.installmentNumber")
    List<LoanRepaymentSchedule> findUnpaidByLoanId(@Param("loanId") UUID loanId);

    @Query("SELECT lrs FROM LoanRepaymentSchedule lrs WHERE lrs.dueDate = :date AND lrs.status IN ('PENDING', 'OVERDUE')")
    List<LoanRepaymentSchedule> findPaymentsDueOnDate(@Param("date") LocalDate date);

    @Query("SELECT lrs FROM LoanRepaymentSchedule lrs WHERE lrs.dueDate < :date AND lrs.status = 'PENDING'")
    List<LoanRepaymentSchedule> findOverduePayments(@Param("date") LocalDate date);
}