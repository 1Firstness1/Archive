package ru.mirea.archive.backend.dto;

public class ResetPasswordResponse {
    private String username;
    private String temporaryPassword;
    private String message;

    public ResetPasswordResponse() {
    }

    public ResetPasswordResponse(String username, String temporaryPassword, String message) {
        this.username = username;
        this.temporaryPassword = temporaryPassword;
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }

    public void setTemporaryPassword(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
