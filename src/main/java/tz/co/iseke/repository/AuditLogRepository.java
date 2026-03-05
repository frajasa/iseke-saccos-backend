package tz.co.iseke.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.AuditLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByUserId(UUID userId);
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    @Query("SELECT al FROM AuditLog al WHERE al.user.id = :userId AND al.timestamp BETWEEN :startDate AND :endDate")
    List<AuditLog> findByUserIdAndDateRange(@Param("userId") UUID userId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    List<AuditLog> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);
}