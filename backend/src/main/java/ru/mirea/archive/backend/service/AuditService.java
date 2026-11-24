package ru.mirea.archive.backend.service;

import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.archive.backend.domain.AuditLog;
import ru.mirea.archive.backend.dto.AuditLogDto;
import ru.mirea.archive.backend.filter.CorrelationIdFilter;
import ru.mirea.archive.backend.repository.AuditLogRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String username, String event, String entityType, String entityId, String details) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        AuditLog auditLog = new AuditLog(
                username != null ? username : "anonymous",
                event,
                entityType,
                entityId,
                correlationId,
                details
        );
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> searchAuditLogs(String search, String event, String entityType, String username, Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("event")), pattern),
                        cb.like(cb.lower(root.get("details")), pattern),
                        cb.like(cb.lower(root.get("correlationId")), pattern)
                ));
            }

            if (event != null && !event.isBlank()) {
                predicates.add(cb.equal(root.get("event"), event.trim()));
            }

            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType.trim()));
            }

            if (username != null && !username.isBlank()) {
                predicates.add(cb.equal(root.get("username"), username.trim()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable).map(this::toDto);
    }

    public AuditLogDto toDto(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getUsername(),
                log.getEvent(),
                log.getEntityType(),
                log.getEntityId(),
                log.getCorrelationId(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}
