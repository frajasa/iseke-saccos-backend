package tz.co.iseke.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.EssServiceRequest;

import java.util.List;
import java.util.UUID;

@Repository
public interface EssServiceRequestRepository extends JpaRepository<EssServiceRequest, UUID> {
    List<EssServiceRequest> findByMemberId(UUID memberId);
    Page<EssServiceRequest> findByStatus(String status, Pageable pageable);
    Page<EssServiceRequest> findByMemberIdAndStatus(UUID memberId, String status, Pageable pageable);
}
