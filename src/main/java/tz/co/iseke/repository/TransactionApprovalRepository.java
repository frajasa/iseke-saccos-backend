package tz.co.iseke.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.TransactionApproval;
import tz.co.iseke.enums.ApprovalStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionApprovalRepository extends JpaRepository<TransactionApproval, UUID> {
    List<TransactionApproval> findByStatus(ApprovalStatus status);
    Page<TransactionApproval> findByStatus(ApprovalStatus status, Pageable pageable);
    List<TransactionApproval> findByRequestedBy(String requestedBy);
}
