package ru.mirea.archive.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.archive.backend.dto.AnalyticsSummaryDto;
import ru.mirea.archive.backend.service.AnalyticsService;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Аналитика", description = "Аналитическая сводка и агрегированные показатели системы")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Получение аналитической сводки по архиву")
    public ResponseEntity<AnalyticsSummaryDto> getSummary() {
        return ResponseEntity.ok(analyticsService.getSummary());
    }
}
