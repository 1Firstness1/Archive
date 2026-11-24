package ru.mirea.archive.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public class CsvApplyRequest {

    public enum ImportMode {
        CREATE_ONLY,
        UPDATE_EXISTING
    }

    @NotNull(message = "Режим импорта обязателен")
    private ImportMode mode;

    @NotEmpty(message = "Данные для импорта не должны быть пустыми")
    private List<Map<String, String>> rows;

    public CsvApplyRequest() {
    }

    public CsvApplyRequest(ImportMode mode, List<Map<String, String>> rows) {
        this.mode = mode;
        this.rows = rows;
    }

    public ImportMode getMode() {
        return mode;
    }

    public void setMode(ImportMode mode) {
        this.mode = mode;
    }

    public List<Map<String, String>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, String>> rows) {
        this.rows = rows;
    }
}
