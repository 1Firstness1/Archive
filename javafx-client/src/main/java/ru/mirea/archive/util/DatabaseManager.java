package ru.mirea.archive.util;

import ru.mirea.archive.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        Properties props = new Properties();
        try (InputStream is = DatabaseManager.class.getResourceAsStream("/db.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException ignored) {
        }
        URL = props.getProperty("db.url", "jdbc:postgresql://localhost:5432/archive_db");
        USER = props.getProperty("db.user", "artem");
        PASSWORD = props.getProperty("db.password", "1234");
    }

    private DatabaseManager() {
    }

    public static Connection getConnection() {
        try {
            Properties connProps = new Properties();
            connProps.setProperty("user", USER);
            connProps.setProperty("password", PASSWORD);
            connProps.setProperty("characterEncoding", "UTF-8");
            connProps.setProperty("options", "-c client_encoding=UTF8");
            return DriverManager.getConnection(URL, connProps);
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось подключиться к базе данных: " + e.getMessage(), e);
        }
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}