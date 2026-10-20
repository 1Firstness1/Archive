package ru.mirea.archive.model;

import java.time.LocalDateTime;

public class AuditLogRecord {
    private Long id;
    private String username;
    private String event;
    private String entityType;
    private String details;
    private LocalDateTime createdAt;

    public AuditLogRecord() {
    }

    public AuditLogRecord(Long id, String username, String event, String entityType, String details, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.event = event;
        this.entityType = entityType;
        this.details = details;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getWorker() {
        return username;
    }

    public void setWorker(String worker) {
        this.username = worker;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
