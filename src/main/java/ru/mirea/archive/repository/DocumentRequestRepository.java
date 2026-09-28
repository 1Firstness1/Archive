package ru.mirea.archive.repository;

import ru.mirea.archive.exception.DatabaseException;
import ru.mirea.archive.model.DocumentRequest;
import ru.mirea.archive.model.Priority;
import ru.mirea.archive.model.RequestStatus;
import ru.mirea.archive.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DocumentRequestRepository implements CrudRepository<DocumentRequest, Integer> {

    @Override
    public DocumentRequest save(DocumentRequest req) {
        String sql = """
            INSERT INTO document_requests (user_id, document_title, description, category, status, priority)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id, created_at, updated_at
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, req.getUserId());
            ps.setString(2, req.getDocumentTitle());
            ps.setString(3, req.getDescription());
            ps.setString(4, req.getCategory());
            ps.setString(5, req.getStatus().name());
            ps.setString(6, req.getPriority().name());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    req.setId(rs.getInt("id"));
                    req.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    req.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
            return req;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при сохранении запроса: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DocumentRequest> findAll() {
        String sql = """
            SELECT dr.*, u.full_name AS user_name
            FROM document_requests dr
            JOIN users u ON dr.user_id = u.id
            ORDER BY dr.id
            """;
        return executeQuery(sql);
    }

    @Override
    public Optional<DocumentRequest> findById(Integer id) {
        String sql = """
            SELECT dr.*, u.full_name AS user_name
            FROM document_requests dr
            JOIN users u ON dr.user_id = u.id
            WHERE dr.id = ?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске запроса: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public void update(DocumentRequest req) {
        String sql = """
            UPDATE document_requests
            SET user_id = ?, document_title = ?, description = ?, category = ?,
                status = ?, priority = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, req.getUserId());
            ps.setString(2, req.getDocumentTitle());
            ps.setString(3, req.getDescription());
            ps.setString(4, req.getCategory());
            ps.setString(5, req.getStatus().name());
            ps.setString(6, req.getPriority().name());
            ps.setInt(7, req.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при обновлении запроса: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM document_requests WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при удалении запроса: " + e.getMessage(), e);
        }
    }

    public List<DocumentRequest> searchByTitle(String title) {
        String sql = """
            SELECT dr.*, u.full_name AS user_name
            FROM document_requests dr
            JOIN users u ON dr.user_id = u.id
            WHERE LOWER(dr.document_title) LIKE LOWER(?)
            ORDER BY dr.id
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + title + "%");
            return executeQuery(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске по названию: " + e.getMessage(), e);
        }
    }

    public List<DocumentRequest> searchByDescription(String keyword) {
        String sql = """
            SELECT dr.*, u.full_name AS user_name
            FROM document_requests dr
            JOIN users u ON dr.user_id = u.id
            WHERE LOWER(dr.description) LIKE LOWER(?)
            ORDER BY dr.id
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            return executeQuery(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске по описанию: " + e.getMessage(), e);
        }
    }

    public List<DocumentRequest> findByUserId(int userId) {
        String sql = """
            SELECT dr.*, u.full_name AS user_name
            FROM document_requests dr
            JOIN users u ON dr.user_id = u.id
            WHERE dr.user_id = ?
            ORDER BY dr.id
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            return executeQuery(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске по пользователю: " + e.getMessage(), e);
        }
    }

    public List<DocumentRequest> filterByStatus(RequestStatus status) {
        String sql = """
            SELECT dr.*, u.full_name AS user_name
            FROM document_requests dr
            JOIN users u ON dr.user_id = u.id
            WHERE dr.status = ?
            ORDER BY dr.id
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            return executeQuery(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при фильтрации по статусу: " + e.getMessage(), e);
        }
    }

    public List<DocumentRequest> filterByPriority(Priority priority) {
        String sql = """
            SELECT dr.*, u.full_name AS user_name
            FROM document_requests dr
            JOIN users u ON dr.user_id = u.id
            WHERE dr.priority = ?
            ORDER BY dr.id
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, priority.name());
            return executeQuery(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при фильтрации по приоритету: " + e.getMessage(), e);
        }
    }

    public List<DocumentRequest> filterByCategory(String category) {
        String sql = """
            SELECT dr.*, u.full_name AS user_name
            FROM document_requests dr
            JOIN users u ON dr.user_id = u.id
            WHERE LOWER(dr.category) = LOWER(?)
            ORDER BY dr.id
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, category);
            return executeQuery(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при фильтрации по категории: " + e.getMessage(), e);
        }
    }

    public List<DocumentRequest> filterByDateRange(LocalDate from, LocalDate to) {
        String sql = """
            SELECT dr.*, u.full_name AS user_name
            FROM document_requests dr
            JOIN users u ON dr.user_id = u.id
            WHERE dr.created_at >= ? AND dr.created_at < ?
            ORDER BY dr.created_at
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            return executeQuery(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при фильтрации по дате: " + e.getMessage(), e);
        }
    }

    public int count() {
        return countBySql("SELECT COUNT(*) FROM document_requests");
    }

    public int countByStatus(RequestStatus status) {
        String sql = "SELECT COUNT(*) FROM document_requests WHERE status = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при подсчёте по статусу: " + e.getMessage(), e);
        }
    }

    public int countByPriority(Priority priority) {
        String sql = "SELECT COUNT(*) FROM document_requests WHERE priority = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, priority.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при подсчёте по приоритету: " + e.getMessage(), e);
        }
    }

    public List<String> findAllCategories() {
        String sql = "SELECT DISTINCT category FROM document_requests ORDER BY category";
        List<String> categories = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении категорий: " + e.getMessage(), e);
        }
        return categories;
    }

    public int countCategories() {
        return countBySql("SELECT COUNT(DISTINCT category) FROM document_requests");
    }

    private int countBySql(String sql) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при подсчёте: " + e.getMessage(), e);
        }
    }

    private List<DocumentRequest> executeQuery(String sql) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return executeQuery(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при выполнении запроса: " + e.getMessage(), e);
        }
    }

    private List<DocumentRequest> executeQuery(PreparedStatement ps) throws SQLException {
        List<DocumentRequest> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    private DocumentRequest mapRow(ResultSet rs) throws SQLException {
        DocumentRequest req = new DocumentRequest();
        req.setId(rs.getInt("id"));
        req.setUserId(rs.getInt("user_id"));
        req.setDocumentTitle(rs.getString("document_title"));
        req.setDescription(rs.getString("description"));
        req.setCategory(rs.getString("category"));
        req.setStatus(RequestStatus.fromString(rs.getString("status")));
        req.setPriority(Priority.fromString(rs.getString("priority")));
        req.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        req.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

        try {
            req.setUserName(rs.getString("user_name"));
        } catch (SQLException ignored) {
        }
        return req;
    }
}
