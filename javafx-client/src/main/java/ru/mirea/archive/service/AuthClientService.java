package ru.mirea.archive.service;

import ru.mirea.archive.model.AuthSession;
import ru.mirea.archive.util.DatabaseManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

public class AuthClientService {

    private final String baseUrl;
    private final HttpClient httpClient;

    public AuthClientService() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/db.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException ignored) {
        }
        this.baseUrl = props.getProperty("backend.url", "http://localhost:8080").replaceAll("/+$", "");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final String username;
        private final String fullName;
        private final String role;
        private final String token;

        public AuthResult(boolean success, String message, String username, String fullName, String role, String token) {
            this.success = success;
            this.message = message;
            this.username = username;
            this.fullName = fullName;
            this.role = role;
            this.token = token;
        }

        public static AuthResult success(String message, String username, String fullName, String role, String token) {
            return new AuthResult(true, message, username, fullName, role, token);
        }

        public static AuthResult error(String message) {
            return new AuthResult(false, message, null, null, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
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

    public static class SystemHealth {
        private final boolean backendAvailable;
        private final boolean databaseAvailable;
        private final boolean fullyOnline;
        private final String message;
        private final String backendDetails;
        private final String databaseDetails;

        public SystemHealth(boolean backendAvailable, boolean databaseAvailable, String message, String backendDetails, String databaseDetails) {
            this.backendAvailable = backendAvailable;
            this.databaseAvailable = databaseAvailable;
            this.fullyOnline = backendAvailable && databaseAvailable;
            this.message = message;
            this.backendDetails = backendDetails;
            this.databaseDetails = databaseDetails;
        }

        public boolean isBackendAvailable() {
            return backendAvailable;
        }

        public boolean isDatabaseAvailable() {
            return databaseAvailable;
        }

        public boolean isFullyOnline() {
            return fullyOnline;
        }

        public String getMessage() {
            return message;
        }

        public String getBackendDetails() {
            return backendDetails;
        }

        public String getDatabaseDetails() {
            return databaseDetails;
        }
    }

    public SystemHealth checkSystemHealth() {
        boolean backendOk = false;
        boolean backendDbOk = false;
        String backendDetails = "Не запущен (порт 8080)";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/health"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (response.statusCode() == 200 && body.contains("\"status\":\"UP\"")) {
                backendOk = true;
                backendDetails = "Работает (Spring Boot REST API)";
                if (body.contains("\"database\":\"CONNECTED\"")) {
                    backendDbOk = true;
                }
            } else if (response.statusCode() == 503 || body.contains("\"database\":\"DISCONNECTED\"")) {
                backendOk = true;
                backendDetails = "Запущен, но база данных отключена";
            } else {
                backendDetails = "Отвечает посторонний сервис (код: " + response.statusCode() + ")";
            }
        } catch (Exception e) {
            backendDetails = "Не отвечает (" + e.getClass().getSimpleName() + ")";
        }

        boolean directDbOk = DatabaseManager.testConnection();
        boolean dbOk = directDbOk || backendDbOk;
        String dbDetails = dbOk ? "Подключена (PostgreSQL)" : "Недоступна (порт 5432)";

        String msg;
        if (backendOk && dbOk) {
            msg = "Все компоненты (Backend и PostgreSQL) работают штатно.";
        } else if (!backendOk && !dbOk) {
            msg = "Бэкенд и база данных не запущены. Запустите: cd backend && docker compose up -d";
        } else if (!backendOk) {
            msg = "Бэкенд REST API не запущен (порт 8080). Запустите: cd backend && docker compose up -d";
        } else {
            msg = "Бэкенд работает, но база данных PostgreSQL недоступна.";
        }

        return new SystemHealth(backendOk, dbOk, msg, backendDetails, dbDetails);
    }

    public boolean isBackendAvailable() {
        return checkSystemHealth().isFullyOnline();
    }

    public AuthResult login(String username, String password) {
        String jsonBody = "{\"username\":" + escapeJson(username) + ",\"password\":" + escapeJson(password) + "}";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/login"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (response.statusCode() == 200 && extractBoolean(body, "success")) {
                String user = extractString(body, "username");
                String name = extractString(body, "fullName");
                String role = extractString(body, "role");
                String token = extractString(body, "token");
                AuthSession.getInstance().setSession(user, name, role, token);
                return AuthResult.success("Авторизация успешна", user, name, role, token);
            } else {
                String msg = extractString(body, "message");
                if (msg == null || msg.isBlank()) {
                    msg = "Неверный логин или пароль (Код: " + response.statusCode() + ")";
                }
                return AuthResult.error(msg);
            }
        } catch (Exception e) {
            return AuthResult.error("Не удалось связаться с сервером: " + e.getMessage());
        }
    }

    public AuthResult register(String username, String password, String fullName, String email) {
        String jsonBody = "{\"username\":" + escapeJson(username)
                + ",\"password\":" + escapeJson(password)
                + ",\"fullName\":" + escapeJson(fullName)
                + ",\"email\":" + escapeJson(email) + "}";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/register"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (response.statusCode() == 200 && extractBoolean(body, "success")) {
                String user = extractString(body, "username");
                String name = extractString(body, "fullName");
                String role = extractString(body, "role");
                String token = extractString(body, "token");
                AuthSession.getInstance().setSession(user, name, role, token);
                return AuthResult.success("Регистрация успешна", user, name, role, token);
            } else {
                String msg = extractString(body, "message");
                if (msg == null || msg.isBlank()) {
                    msg = "Ошибка регистрации (Код: " + response.statusCode() + ")";
                }
                return AuthResult.error(msg);
            }
        } catch (Exception e) {
            return AuthResult.error("Не удалось связаться с сервером: " + e.getMessage());
        }
    }

    public AuthResult changePassword(String username, String oldPassword, String newPassword) {
        String jsonBody = "{\"username\":" + escapeJson(username)
                + ",\"oldPassword\":" + escapeJson(oldPassword)
                + ",\"newPassword\":" + escapeJson(newPassword) + "}";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/change-password"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (response.statusCode() == 200 && extractBoolean(body, "success")) {
                return AuthResult.success("Пароль успешно изменён", username, null, null, null);
            } else {
                String msg = extractString(body, "message");
                if (msg == null || msg.isBlank()) {
                    msg = "Не удалось сменить пароль";
                }
                return AuthResult.error(msg);
            }
        } catch (Exception e) {
            return AuthResult.error("Не удалось связаться с сервером: " + e.getMessage());
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    private static String escapeJson(String str) {
        if (str == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    public java.util.List<ru.mirea.archive.model.Worker> fetchWorkers() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/workers"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseWorkersJson(response.body());
            }
        } catch (Exception ignored) {
        }
        return fetchWorkersFromDb();
    }

    private java.util.List<ru.mirea.archive.model.Worker> fetchWorkersFromDb() {
        java.util.List<ru.mirea.archive.model.Worker> list = new java.util.ArrayList<>();
        String sql = "SELECT id, username, full_name, email, role, created_at FROM app_accounts ORDER BY id ASC";
        try (java.sql.Connection conn = ru.mirea.archive.util.DatabaseManager.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.sql.Timestamp ts = rs.getTimestamp("created_at");
                java.time.LocalDateTime ldt = ts != null ? ts.toLocalDateTime() : java.time.LocalDateTime.now();
                list.add(new ru.mirea.archive.model.Worker(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("role"),
                        ldt
                ));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private java.util.List<ru.mirea.archive.model.Worker> parseWorkersJson(String json) {
        java.util.List<ru.mirea.archive.model.Worker> list = new java.util.ArrayList<>();
        if (json == null || !json.startsWith("[")) return list;
        int idx = 0;
        while ((idx = json.indexOf('{', idx)) != -1) {
            int end = json.indexOf('}', idx);
            if (end == -1) break;
            String obj = json.substring(idx, end + 1);
            Long id = extractLong(obj, "id");
            String username = extractString(obj, "username");
            String fullName = extractString(obj, "fullName");
            String email = extractString(obj, "email");
            String role = extractString(obj, "role");
            String tsStr = extractString(obj, "createdAt");
            java.time.LocalDateTime ts = java.time.LocalDateTime.now();
            if (tsStr != null && !tsStr.isBlank()) {
                try {
                    ts = java.time.LocalDateTime.parse(tsStr.substring(0, Math.min(19, tsStr.length())));
                } catch (Exception ignored) {}
            }
            list.add(new ru.mirea.archive.model.Worker(
                    id != null ? id : 0L,
                    username != null ? username : "",
                    fullName != null ? fullName : "",
                    email != null ? email : "",
                    role != null ? role : "USER",
                    ts
            ));
            idx = end + 1;
        }
        return list;
    }

    public java.util.List<ru.mirea.archive.model.AuditLogRecord> fetchAuditLogs() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/audit-logs"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseAuditLogsJson(response.body());
            }
        } catch (Exception ignored) {
        }
        return fetchAuditLogsFromDb();
    }

    private java.util.List<ru.mirea.archive.model.AuditLogRecord> fetchAuditLogsFromDb() {
        java.util.List<ru.mirea.archive.model.AuditLogRecord> list = new java.util.ArrayList<>();
        String sql = "SELECT id, username, event, entity_type, details, created_at FROM audit_logs ORDER BY created_at DESC";
        try (java.sql.Connection conn = ru.mirea.archive.util.DatabaseManager.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.sql.Timestamp ts = rs.getTimestamp("created_at");
                java.time.LocalDateTime ldt = ts != null ? ts.toLocalDateTime() : java.time.LocalDateTime.now();
                list.add(new ru.mirea.archive.model.AuditLogRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("event"),
                        rs.getString("entity_type"),
                        rs.getString("details"),
                        ldt
                ));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public void logAuditEvent(String username, String event, String entityType, String details) {
        try {
            String json = "{\"username\":" + escapeJson(username)
                    + ",\"event\":" + escapeJson(event)
                    + ",\"entityType\":" + escapeJson(entityType)
                    + ",\"details\":" + escapeJson(details) + "}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/audit-logs"))
                    .timeout(Duration.ofSeconds(2))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            httpClient.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            String sql = "INSERT INTO audit_logs (username, event, entity_type, details, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
            try (java.sql.Connection conn = ru.mirea.archive.util.DatabaseManager.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username != null ? username : "anonymous");
                ps.setString(2, event != null ? event : "UNKNOWN");
                ps.setString(3, entityType != null ? entityType : "GENERAL");
                ps.setString(4, details != null ? details : "");
                ps.executeUpdate();
            } catch (Exception ignored) {
            }
        }
    }

    private java.util.List<ru.mirea.archive.model.AuditLogRecord> parseAuditLogsJson(String json) {
        java.util.List<ru.mirea.archive.model.AuditLogRecord> list = new java.util.ArrayList<>();
        if (json == null || !json.startsWith("[")) return list;
        int idx = 0;
        while ((idx = json.indexOf('{', idx)) != -1) {
            int end = json.indexOf('}', idx);
            if (end == -1) break;
            String obj = json.substring(idx, end + 1);
            Long id = extractLong(obj, "id");
            String user = extractString(obj, "username");
            String event = extractString(obj, "event");
            String entityType = extractString(obj, "entityType");
            String details = extractString(obj, "details");
            String tsStr = extractString(obj, "createdAt");
            java.time.LocalDateTime ts = java.time.LocalDateTime.now();
            if (tsStr != null && !tsStr.isBlank()) {
                try {
                    ts = java.time.LocalDateTime.parse(tsStr.substring(0, Math.min(19, tsStr.length())));
                } catch (Exception ignored) {}
            }
            list.add(new ru.mirea.archive.model.AuditLogRecord(id, user, event, entityType, details, ts));
            idx = end + 1;
        }
        return list;
    }

    private static Long extractLong(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Long.parseLong(json.substring(start, end));
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    private static boolean extractBoolean(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return false;
        start += search.length();
        return json.startsWith("true", start);
    }
}
