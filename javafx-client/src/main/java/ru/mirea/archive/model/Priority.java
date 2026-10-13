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
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Приоритет не может быть пустым.");
        }
        String trimmed = value.trim();

        try {
            int num = Integer.parseInt(trimmed);
            Priority[] all = values();
            if (num >= 1 && num <= all.length) {
                return all[num - 1];
            }
        } catch (NumberFormatException ignored) {
        }

        String normalized = trimmed.replace('ё', 'е').replace('Ё', 'Е');
        for (Priority p : values()) {
            String normName = p.name().replace('ё', 'е').replace('Ё', 'Е');
            String normDisplay = p.displayName.replace('ё', 'е').replace('Ё', 'Е');
            if (normName.equalsIgnoreCase(normalized) || normDisplay.equalsIgnoreCase(normalized)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Неизвестный приоритет: " + value);
    }
}
