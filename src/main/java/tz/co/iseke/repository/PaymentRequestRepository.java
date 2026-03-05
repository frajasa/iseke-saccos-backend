package tz.co.iseke.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.PaymentRequest;
import tz.co.iseke.enums.PaymentProvider;
import tz.co.iseke.enums.PaymentRequestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, UUID> {
    Optional<PaymentRequest> findByRequestNumber(String requestNumber);
    Optional<PaymentRequest> findByProviderReference(String providerReference);
    Optional<PaymentRequest> findByProviderConversationId(String conversationId);

    Page<PaymentRequest> findByProvider(PaymentProvider provider, Pageable pageable);
    Page<PaymentRequest> findByStatus(PaymentRequestStatus status, Pageable pageable);
    Page<PaymentRequest> findByProviderAndStatus(PaymentProvider provider, PaymentRequestStatus status, Pageable pageable);
    Page<PaymentRequest> findByMemberId(UUID memberId, Pageable pageable);

    List<PaymentRequest> findByStatusAndExpiresAtBefore(PaymentRequestStatus status, LocalDateTime expiresAt);

    @Query("SELECT p FROM PaymentRequest p WHERE p.provider = :provider AND p.status = :status " +
           "AND p.initiatedAt BETWEEN :startDate AND :endDate")
    List<PaymentRequest> findForReconciliation(
            @Param("provider") PaymentProvider provider,
            @Param("status") PaymentRequestStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.provider = :provider AND p.status = :status " +
           "AND p.initiatedAt >= :since")
    long countByProviderAndStatusSince(
            @Param("provider") PaymentProvider provider,
            @Param("status") PaymentRequestStatus status,
            @Param("since") LocalDateTime since);
}
