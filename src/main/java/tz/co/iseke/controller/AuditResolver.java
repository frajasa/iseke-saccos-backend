package tz.co.iseke.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import tz.co.iseke.entity.AuditLog;
import tz.co.iseke.service.AuditService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AuditResolver {

    private final AuditService auditService;

    @QueryMapping
    public List<AuditLog> auditLogs(@Argument String entityType,
                                     @Argument UUID entityId,
                                     @Argument UUID userId,
                                     @Argument LocalDateTime startDate,
                                     @Argument LocalDateTime endDate) {
        return auditService.findByFilters(entityType, entityId, userId, startDate, endDate);
    }
}
