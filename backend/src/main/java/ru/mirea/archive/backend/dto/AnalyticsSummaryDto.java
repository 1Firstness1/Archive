package ru.mirea.archive.backend.dto;

import java.util.Map;

public class AnalyticsSummaryDto {

    private long totalCount;
    private long activeCount;
    private long archivedCount;
    private long confidentialCount;
    private long nonConfidentialCount;
    private double averagePageCount;
    private Map<String, Long> byStatus;
    private Map<String, Long> byCategory;
    private Map<String, Long> byUser;
    private Map<String, Long> byCalculatedMetric;

    public AnalyticsSummaryDto() {
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(long activeCount) {
        this.activeCount = activeCount;
    }

    public long getArchivedCount() {
        return archivedCount;
    }

    public void setArchivedCount(long archivedCount) {
        this.archivedCount = archivedCount;
    }

    public long getConfidentialCount() {
        return confidentialCount;
    }

    public void setConfidentialCount(long confidentialCount) {
        this.confidentialCount = confidentialCount;
    }

    public long getNonConfidentialCount() {
        return nonConfidentialCount;
    }

    public void setNonConfidentialCount(long nonConfidentialCount) {
        this.nonConfidentialCount = nonConfidentialCount;
    }

    public double getAveragePageCount() {
        return averagePageCount;
    }

    public void setAveragePageCount(double averagePageCount) {
        this.averagePageCount = averagePageCount;
    }

    public Map<String, Long> getByStatus() {
        return byStatus;
    }

    public void setByStatus(Map<String, Long> byStatus) {
        this.byStatus = byStatus;
    }

    public Map<String, Long> getByCategory() {
        return byCategory;
    }

    public void setByCategory(Map<String, Long> byCategory) {
        this.byCategory = byCategory;
    }

    public Map<String, Long> getByUser() {
        return byUser;
    }

    public void setByUser(Map<String, Long> byUser) {
        this.byUser = byUser;
    }

    public Map<String, Long> getByCalculatedMetric() {
        return byCalculatedMetric;
    }

    public void setByCalculatedMetric(Map<String, Long> byCalculatedMetric) {
        this.byCalculatedMetric = byCalculatedMetric;
    }
}
