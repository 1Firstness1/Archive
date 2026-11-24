package ru.mirea.archive.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateNoteRequest {

    @NotBlank(message = "Текст заметки не может быть пустым")
    @Size(max = 4000, message = "Текст заметки не должен превышать 4000 символов")
    private String content;

    private boolean pinned = false;

    public CreateNoteRequest() {
    }

    public CreateNoteRequest(String content, boolean pinned) {
        this.content = content;
        this.pinned = pinned;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }
}
