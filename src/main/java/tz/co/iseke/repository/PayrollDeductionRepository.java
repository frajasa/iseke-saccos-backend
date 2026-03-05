package tz.co.iseke.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.PayrollDeduction;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollDeductionRepository extends JpaRepository<PayrollDeduction, UUID> {
    List<PayrollDeduction> findByMemberIdAndIsActiveTrue(UUID memberId);
    List<PayrollDeduction> findByEmployerIdAndIsActiveTrue(UUID employerId);
}
