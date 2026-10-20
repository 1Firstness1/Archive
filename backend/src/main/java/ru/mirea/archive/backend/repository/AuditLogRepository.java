package ru.mirea.archive.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mirea.archive.backend.model.AuditLog;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByCreatedAtDesc();
    List<AuditLog> findByUsernameContainingIgnoreCaseOrEventContainingIgnoreCaseOrderByCreatedAtDesc(String username, String event);
}
