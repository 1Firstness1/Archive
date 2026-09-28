package ru.mirea.archive.model;

public enum RequestStatus {
    NEW("Новый"),
    IN_PROGRESS("В обработке"),
    APPROVED("Одобрен"),
    REJECTED("Отклонён"),
    COMPLETED("Выполнен"),
    CANCELLED("Отменён");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RequestStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Статус не может быть пустым.");
        }
        String trimmed = value.trim();
        for (RequestStatus status : values()) {
            if (status.name().equalsIgnoreCase(trimmed) || status.displayName.equalsIgnoreCase(trimmed)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Неизвестный статус: " + value);
    }
}
