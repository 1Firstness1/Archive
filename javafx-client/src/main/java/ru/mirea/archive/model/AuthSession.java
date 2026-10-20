package ru.mirea.archive.model;

public class AuthSession {
    private static AuthSession instance;

    private String username;
    private String fullName;
    private String role;
    private String token;

    private AuthSession() {
    }

    public static synchronized AuthSession getInstance() {
        if (instance == null) {
            instance = new AuthSession();
        }
        return instance;
    }

    public void setSession(String username, String fullName, String role, String token) {
        this.username = username;
        this.fullName = fullName != null && !fullName.isBlank() ? fullName : username;
        this.role = role != null ? role : "USER";
        this.token = token;
    }

    public void clear() {
        this.username = null;
        this.fullName = null;
        this.role = null;
        this.token = null;
    }

    public boolean isAuthenticated() {
        return username != null && !username.isBlank();
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }
}
