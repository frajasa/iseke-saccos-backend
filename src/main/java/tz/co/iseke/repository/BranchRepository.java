package tz.co.iseke.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.Branch;
import tz.co.iseke.enums.BranchStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {
    
    Optional<Branch> findByBranchCode(String branchCode);
    
    List<Branch> findByStatus(BranchStatus status);
    
    boolean existsByBranchCode(String branchCode);
    
    List<Branch> findByBranchNameContainingIgnoreCase(String branchName);
}