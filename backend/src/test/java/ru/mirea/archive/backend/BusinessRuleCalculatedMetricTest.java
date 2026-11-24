package ru.mirea.archive.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mirea.archive.backend.domain.DocumentRequest;
import ru.mirea.archive.backend.domain.Priority;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessRuleCalculatedMetricTest {

    @Test
    @DisplayName("2.1. Базовый расчет срока обработки по приоритетам (при объеме до 20 страниц, открытый)")
    void testBasePriorityDays() {
        // Базовые дни: CRITICAL=1, HIGH=2, MEDIUM=4, LOW=6
        assertThat(DocumentRequest.calculateEstimatedDays(Priority.CRITICAL, 10, false)).isEqualTo(1);
        assertThat(DocumentRequest.calculateEstimatedDays(Priority.HIGH, 15, false)).isEqualTo(2);
        assertThat(DocumentRequest.calculateEstimatedDays(Priority.MEDIUM, 19, false)).isEqualTo(4);
        assertThat(DocumentRequest.calculateEstimatedDays(Priority.LOW, 5, false)).isEqualTo(6);
    }

    @Test
    @DisplayName("2.2. Увеличение срока обработки в зависимости от объема страниц (+1 день на каждые 20 страниц)")
    void testPageVolumeCalculation() {
        // Приоритет MEDIUM: база = 4
        // 20 страниц: 4 + 20/20 = 5 дней
        assertThat(DocumentRequest.calculateEstimatedDays(Priority.MEDIUM, 20, false)).isEqualTo(5);
        // 45 страниц: 4 + 45/20 = 4 + 2 = 6 дней
        assertThat(DocumentRequest.calculateEstimatedDays(Priority.MEDIUM, 45, false)).isEqualTo(6);
        // 120 страниц: 4 + 120/20 = 4 + 6 = 10 дней
        assertThat(DocumentRequest.calculateEstimatedDays(Priority.MEDIUM, 120, false)).isEqualTo(10);
    }

    @Test
    @DisplayName("2.3. Влияние грифа секретности (+1 день на согласование режима ДСП)")
    void testConfidentialityAddition() {
        // Приоритет HIGH (2 дня) + 15 страниц (0 дн) + конфиденциально (1 день) = 3 дня
        assertThat(DocumentRequest.calculateEstimatedDays(Priority.HIGH, 15, true)).isEqualTo(3);
        // Приоритет CRITICAL (1 день) + 55 страниц (2 дн) + конфиденциально (1 день) = 4 дня
        assertThat(DocumentRequest.calculateEstimatedDays(Priority.CRITICAL, 55, true)).isEqualTo(4);
    }

    @Test
    @DisplayName("2.4. Граничные значения: 0 и отрицательные страницы")
    void testEdgeCases() {
        // Минимальный срок не может быть меньше 1 дня
        assertThat(DocumentRequest.calculateEstimatedDays(Priority.CRITICAL, 0, false)).isEqualTo(1);
        assertThat(DocumentRequest.calculateEstimatedDays(Priority.CRITICAL, -5, false)).isEqualTo(1);
        assertThat(DocumentRequest.calculateEstimatedDays(null, 10, false)).isEqualTo(4); // default MEDIUM
    }
}
