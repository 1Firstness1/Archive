package ru.mirea.archive.model;

import java.time.LocalDateTime;

/**
 * Модель работника системы (учетная запись сотрудника архива).
 * Содержит все поля, кроме конфиденциального хэша пароля.
 */
public class Worker {
    private long id;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private LocalDateTime createdAt;

    public Worker() {
    }

    public Worker(long id, String username, String fullName, String email, String role, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
