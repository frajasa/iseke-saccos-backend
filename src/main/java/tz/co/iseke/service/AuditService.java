package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.entity.AuditLog;
import tz.co.iseke.entity.User;
import tz.co.iseke.repository.AuditLogRepository;
import tz.co.iseke.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, String entityType, UUID entityId,
                          String oldValue, String newValue) {
        try {
            User currentUser = getCurrentUser();

            AuditLog auditLog = AuditLog.builder()
                    .user(currentUser)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to create audit log entry: {}", e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, String entityType, UUID entityId) {
        logAction(action, entityType, entityId, null, null);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findByFilters(String entityType, UUID entityId, UUID userId,
                                         LocalDateTime startDate, LocalDateTime endDate) {
        if (entityType != null && entityId != null) {
            return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
        }
        if (userId != null && startDate != null && endDate != null) {
            return auditLogRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        }
        if (userId != null) {
            return auditLogRepository.findByUserId(userId);
        }
        if (startDate != null && endDate != null) {
            return auditLogRepository.findByTimestampBetween(startDate, endDate);
        }
        return auditLogRepository.findAll();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }
}
