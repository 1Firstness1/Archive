package ru.mirea.archive.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.mirea.archive.backend.domain.Priority;

import java.util.Set;

public class CreateDocumentRequest {

    @NotNull(message = "ID заявителя обязателен")
    private Long userId;

    private String requestNumber;

    @NotBlank(message = "Название документа обязательно")
    @Size(min = 2, max = 300, message = "Название документа должно быть от 2 до 300 символов")
    private String documentTitle;

    @Size(max = 4000, message = "Описание не должно превышать 4000 символов")
    private String description;

    @NotBlank(message = "Категория обязательна")
    @Size(min = 2, max = 100, message = "Категория должна быть от 2 до 100 символов")
    private String category;

    @NotNull(message = "Приоритет обязателен")
    private Priority priority = Priority.MEDIUM;

    @Min(value = 1, message = "Количество страниц должно быть не менее 1")
    @Max(value = 50000, message = "Количество страниц не должно превышать 50000")
    private int pageCount = 1;

    private boolean confidential = false;

    private Set<String> tags;

    public CreateDocumentRequest() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public boolean isConfidential() {
        return confidential;
    }

    public void setConfidential(boolean confidential) {
        this.confidential = confidential;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
}
