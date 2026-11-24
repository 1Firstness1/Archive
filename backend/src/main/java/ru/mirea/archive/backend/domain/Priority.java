package ru.mirea.archive.backend.domain;

public enum Priority {
    LOW("Низкий"),
    MEDIUM("Обычный"),
    HIGH("Высокий"),
    CRITICAL("Критический");

    private final String displayName;

    Priority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
