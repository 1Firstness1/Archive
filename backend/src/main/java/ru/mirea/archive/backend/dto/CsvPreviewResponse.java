package ru.mirea.archive.backend.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvPreviewResponse {

    public static class RowError {
        private int rowNumber;
        private String field;
        private String message;

        public RowError() {
        }

        public RowError(int rowNumber, String field, String message) {
            this.rowNumber = rowNumber;
            this.field = field;
            this.message = message;
        }

        public int getRowNumber() {
            return rowNumber;
        }

        public void setRowNumber(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    private int totalRows;
    private int validRows;
    private int invalidRows;
    private List<Map<String, String>> previewData = new ArrayList<>();
    private List<RowError> errors = new ArrayList<>();

    public CsvPreviewResponse() {
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getValidRows() {
        return validRows;
    }

    public void setValidRows(int validRows) {
        this.validRows = validRows;
    }

    public int getInvalidRows() {
        return invalidRows;
    }

    public void setInvalidRows(int invalidRows) {
        this.invalidRows = invalidRows;
    }

    public List<Map<String, String>> getPreviewData() {
        return previewData;
    }

    public void setPreviewData(List<Map<String, String>> previewData) {
        this.previewData = previewData;
    }

    public List<RowError> getErrors() {
        return errors;
    }

    public void setErrors(List<RowError> errors) {
        this.errors = errors;
    }
}
