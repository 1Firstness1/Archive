package ru.mirea.archive.model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String fullName;
    private String email;
    private String phone;
    private String department;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(String fullName, String email, String phone, String department) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.department = department;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("| %-4d | %-30s | %-25s | %-18s | %-20s |",
                id,
                truncate(fullName, 30),
                truncate(email, 25),
                truncate(phone != null && !phone.isBlank() ? phone : "-", 18),
                truncate(department != null && !department.isBlank() ? department : "-", 20));
    }

    public static String tableHeader() {
        return String.format("| %-4s | %-30s | %-25s | %-18s | %-20s |",
                "ID", "ФИО", "Email", "Телефон", "Отдел");
    }

    public static String tableSeparator() {
        return "+------+--------------------------------+---------------------------+--------------------+----------------------+";
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "-";
        if (s.length() <= maxLen) return s;
        if (maxLen <= 3) return s.substring(0, maxLen);
        return s.substring(0, maxLen - 3) + "...";
    }
}
