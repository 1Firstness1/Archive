package ru.mirea.archive.model;

public enum Priority {
    LOW("Низкий"),
    MEDIUM("Средний"),
    HIGH("Высокий"),
    CRITICAL("Критический");

    private final String displayName;

    Priority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Priority fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Приоритет не может быть пустым.");
        }
        String trimmed = value.trim();
        for (Priority p : values()) {
            if (p.name().equalsIgnoreCase(trimmed) || p.displayName.equalsIgnoreCase(trimmed)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Неизвестный приоритет: " + value);
    }
}
