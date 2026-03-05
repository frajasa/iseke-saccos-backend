package tz.co.iseke.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.PayrollDeductionBatch;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollDeductionBatchRepository extends JpaRepository<PayrollDeductionBatch, UUID> {
    Optional<PayrollDeductionBatch> findByEmployerIdAndPeriod(UUID employerId, String period);
    List<PayrollDeductionBatch> findByEmployerId(UUID employerId);
}
