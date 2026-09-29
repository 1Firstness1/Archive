package ru.mirea.archive.model;

public enum RequestStatus {
    NEW("Новый"),
    IN_PROGRESS("В обработке"),
    APPROVED("Одобрен"),
    REJECTED("Отклонен"),
    COMPLETED("Выполнен"),
    CANCELLED("Отменен");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RequestStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Статус не может быть пустым.");
        }
        String trimmed = value.trim();

        try {
            int num = Integer.parseInt(trimmed);
            RequestStatus[] all = values();
            if (num >= 1 && num <= all.length) {
                return all[num - 1];
            }
        } catch (NumberFormatException ignored) {
        }

        String normalized = trimmed.replace('ё', 'е').replace('Ё', 'Е');
        for (RequestStatus status : values()) {
            String normName = status.name().replace('ё', 'е').replace('Ё', 'Е');
            String normDisplay = status.displayName.replace('ё', 'е').replace('Ё', 'Е');
            if (normName.equalsIgnoreCase(normalized) || normDisplay.equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Неизвестный статус: " + value);
    }
}
