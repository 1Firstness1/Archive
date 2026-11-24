package ru.mirea.archive.backend.domain;

public enum RequestStatus {
    NEW("Новый"),
    IN_PROGRESS("В обработке"),
    APPROVED("Одобрен"),
    REJECTED("Отклонён"),
    COMPLETED("Завершён"),
    CANCELLED("Отменён");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
