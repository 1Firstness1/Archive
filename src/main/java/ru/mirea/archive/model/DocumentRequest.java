package ru.mirea.archive.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DocumentRequest {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private int id;
    private int userId;
    private String documentTitle;
    private String description;
    private String category;
    private RequestStatus status;
    private Priority priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String userName;

    public DocumentRequest() {
    }

    public DocumentRequest(int userId, String documentTitle, String description,
                           String category, Priority priority) {
        this.userId = userId;
        this.documentTitle = documentTitle;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.status = RequestStatus.NEW;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return String.format("| %-4d | %-35s | %-16s | %-14s | %-12s | %-16s | %-18s |",
                id,
                truncate(documentTitle, 35),
                truncate(category, 16),
                status != null ? truncate(status.getDisplayName(), 14) : "-",
                priority != null ? truncate(priority.getDisplayName(), 12) : "-",
                createdAt != null ? createdAt.format(FMT) : "-",
                userName != null ? truncate(userName, 18) : String.valueOf(userId));
    }

    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("==============================================================\n");
        sb.append("               ДЕТАЛЬНАЯ ИНФОРМАЦИЯ О ЗАПРОСЕ                 \n");
        sb.append("==============================================================\n");
        sb.append(String.format("  ID:            %d%n", id));
        sb.append(String.format("  Документ:      %s%n", documentTitle));
        sb.append(String.format("  Описание:      %s%n", description != null && !description.isBlank() ? description : "-"));
        sb.append(String.format("  Категория:     %s%n", category));
        sb.append(String.format("  Статус:        %s%n", status != null ? status.getDisplayName() : "-"));
        sb.append(String.format("  Приоритет:     %s%n", priority != null ? priority.getDisplayName() : "-"));
        sb.append(String.format("  Пользователь:  %s (ID: %d)%n", userName != null ? userName : "-", userId));
        sb.append(String.format("  Создан:        %s%n", createdAt != null ? createdAt.format(FMT) : "-"));
        sb.append(String.format("  Обновлён:      %s%n", updatedAt != null ? updatedAt.format(FMT) : "-"));
        sb.append("==============================================================");
        return sb.toString();
    }

    public static String tableHeader() {
        return String.format("| %-4s | %-35s | %-16s | %-14s | %-12s | %-16s | %-18s |",
                "ID", "Документ", "Категория", "Статус", "Приоритет", "Дата создания", "Пользователь");
    }

    public static String tableSeparator() {
        return "+------+-------------------------------------+------------------+----------------+--------------+------------------+--------------------+";
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "-";
        if (s.length() <= maxLen) return s;
        if (maxLen <= 3) return s.substring(0, maxLen);
        return s.substring(0, maxLen - 3) + "...";
    }
}
