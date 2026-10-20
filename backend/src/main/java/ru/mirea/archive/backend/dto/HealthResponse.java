package ru.mirea.archive.backend.dto;

import java.time.LocalDateTime;

public class HealthResponse {
    private String status;
    private String service = "archive-backend";
    private String database;
    private String message;
    private LocalDateTime timestamp = LocalDateTime.now();

    public HealthResponse() {
    }

    public HealthResponse(String status, String database, String message) {
        this.status = status;
        this.service = "archive-backend";
        this.database = database;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
