package ru.mirea.archive.util;

import ru.mirea.archive.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {

    private static final String URL = "jdbc:postgresql://localhost:5432/archive_db";
    private static final String USER = "artem";
    private static final String PASSWORD = "1234";

    private DatabaseManager() {
    }

    public static Connection getConnection() {
        try {
            Properties props = new Properties();
            props.setProperty("user", USER);
            props.setProperty("password", PASSWORD);
            props.setProperty("characterEncoding", "UTF-8");
            props.setProperty("options", "-c client_encoding=UTF8");
            return DriverManager.getConnection(URL, props);
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
