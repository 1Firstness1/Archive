package ru.mirea.archive.backend.dto;

public class RegisterRequest {
    private String username;
    private String password;
    private String fullName;
    private String email;

    public RegisterRequest() {
    }

    public RegisterRequest(String username, String password, String fullName, String email) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getLogin() {
        return username;
    }

    public void setLogin(String login) {
        if (this.username == null || this.username.isBlank()) {
            this.username = login;
        }
    }
}
