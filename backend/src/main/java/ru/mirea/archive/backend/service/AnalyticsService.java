package ru.mirea.archive.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.archive.backend.domain.DocumentRequest;
import ru.mirea.archive.backend.dto.AnalyticsSummaryDto;
import ru.mirea.archive.backend.repository.DocumentRequestRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final DocumentRequestRepository documentRequestRepository;

    public AnalyticsService(DocumentRequestRepository documentRequestRepository) {
        this.documentRequestRepository = documentRequestRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryDto getSummary() {
        List<DocumentRequest> all = documentRequestRepository.findAll();

        long total = all.size();
        long archived = all.stream().filter(DocumentRequest::isArchived).count();
        long active = total - archived;
        long confidential = all.stream().filter(DocumentRequest::isConfidential).count();
        long nonConfidential = total - confidential;

        double avgPages = all.stream()
                .mapToInt(DocumentRequest::getPageCount)
                .average()
                .orElse(0.0);
        // Округляем до одного знака после запятой
        avgPages = Math.round(avgPages * 10.0) / 10.0;

        // Группировка по статусу
        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getStatus().getDisplayName(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        // Группировка по категории
        Map<String, Long> byCategory = all.stream()
                .collect(Collectors.groupingBy(
                        DocumentRequest::getCategory,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        // Группировка по справочнику (заявителям архива)
        Map<String, Long> byUser = all.stream()
                .filter(d -> d.getUser() != null)
                .collect(Collectors.groupingBy(
                        d -> d.getUser().getFullName(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        // Группировка по вычисляемому показателю (estimatedDays)
        Map<String, Long> byCalculatedMetric = all.stream()
                .collect(Collectors.groupingBy(
                        d -> {
                            int days = d.getEstimatedDays();
                            if (days <= 2) return "1-2 дня (Экспресс)";
                            if (days <= 4) return "3-4 дня (Стандарт)";
                            return "5+ дней (Продлённый)";
                        },
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        AnalyticsSummaryDto summary = new AnalyticsSummaryDto();
        summary.setTotalCount(total);
        summary.setActiveCount(active);
        summary.setArchivedCount(archived);
        summary.setConfidentialCount(confidential);
        summary.setNonConfidentialCount(nonConfidential);
        summary.setAveragePageCount(avgPages);
        summary.setByStatus(byStatus);
        summary.setByCategory(byCategory);
        summary.setByUser(byUser);
        summary.setByCalculatedMetric(byCalculatedMetric);

        return summary;
    }
}
