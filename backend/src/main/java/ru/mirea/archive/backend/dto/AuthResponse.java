package ru.mirea.archive.backend.dto;

public class AuthResponse {
    private boolean success;
    private String message;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private String token;

    public AuthResponse() {
    }

    public static AuthResponse success(String username, String fullName, String email, String role, String token) {
        AuthResponse res = new AuthResponse();
        res.success = true;
        res.message = "Авторизация успешна";
        res.username = username;
        res.fullName = fullName;
        res.email = email;
        res.role = role;
        res.token = token;
        return res;
    }

    public static AuthResponse error(String message) {
        AuthResponse res = new AuthResponse();
        res.success = false;
        res.message = message;
        return res;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
