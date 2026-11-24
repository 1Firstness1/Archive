package ru.mirea.archive.backend.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BulkOperationResult {

    public static class Failure {
        private UUID uuid;
        private String reason;

        public Failure() {
        }

        public Failure(UUID uuid, String reason) {
            this.uuid = uuid;
            this.reason = reason;
        }

        public UUID getUuid() {
            return uuid;
        }

        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    private int totalRequested;
    private int successCount;
    private int failureCount;
    private List<UUID> successUuids = new ArrayList<>();
    private List<Failure> failures = new ArrayList<>();

    public BulkOperationResult() {
    }

    public BulkOperationResult(int totalRequested) {
        this.totalRequested = totalRequested;
    }

    public void addSuccess(UUID uuid) {
        this.successUuids.add(uuid);
        this.successCount++;
    }

    public void addFailure(UUID uuid, String reason) {
        this.failures.add(new Failure(uuid, reason));
        this.failureCount++;
    }

    public int getTotalRequested() {
        return totalRequested;
    }

    public void setTotalRequested(int totalRequested) {
        this.totalRequested = totalRequested;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<UUID> getSuccessUuids() {
        return successUuids;
    }

    public void setSuccessUuids(List<UUID> successUuids) {
        this.successUuids = successUuids;
    }

    public List<Failure> getFailures() {
        return failures;
    }

    public void setFailures(List<Failure> failures) {
        this.failures = failures;
    }
}
