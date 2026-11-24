package ru.mirea.archive.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateNoteRequest {

    @NotBlank(message = "Текст заметки не может быть пустым")
    @Size(max = 4000, message = "Текст заметки не должен превышать 4000 символов")
    private String content;

    private Boolean pinned;

    public UpdateNoteRequest() {
    }

    public UpdateNoteRequest(String content, Boolean pinned) {
        this.content = content;
        this.pinned = pinned;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }
}
