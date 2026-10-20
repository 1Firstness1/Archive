package ru.mirea.archive.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.archive.backend.dto.AuthRequest;
import ru.mirea.archive.backend.dto.AuthResponse;
import ru.mirea.archive.backend.dto.ChangePasswordRequest;
import ru.mirea.archive.backend.dto.RegisterRequest;
import ru.mirea.archive.backend.model.Account;
import ru.mirea.archive.backend.repository.AccountRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final ru.mirea.archive.backend.repository.AuditLogRepository auditLogRepository;

    @Value("${app.init.admin.login:admin}")
    private String initAdminLogin;

    @Value("${app.init.admin.password:admin123}")
    private String initAdminPassword;

    @Value("${app.init.admin.name:Администратор Архива}")
    private String initAdminName;

    @Value("${app.init.admin.email:admin@mirea.ru}")
    private String initAdminEmail;

    public AuthService(AccountRepository accountRepository, ru.mirea.archive.backend.repository.AuditLogRepository auditLogRepository) {
        this.accountRepository = accountRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<ru.mirea.archive.backend.model.AuditLog> getAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public void logEvent(String username, String event, String entityType, String details) {
        auditLogRepository.save(new ru.mirea.archive.backend.model.AuditLog(
                username != null ? username : "anonymous",
                event,
                entityType,
                details
        ));
    }

    @Override
    public void run(String... args) {
        // Создаем администратора со стартовыми данными из .env при первом запуске
        if (!accountRepository.existsByUsername(initAdminLogin)) {
            String hash = hashPassword(initAdminPassword);
            Account admin = new Account(
                    initAdminLogin,
                    hash,
                    initAdminName,
                    initAdminEmail,
                    "ADMIN"
            );
            accountRepository.save(admin);
            System.out.println("[Backend] Инициализирован стартовый администратор из .env: " + initAdminLogin);
        }
    }

    public AuthResponse login(AuthRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return AuthResponse.error("Логин не может быть пустым");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return AuthResponse.error("Пароль не может быть пустым");
        }

        Optional<Account> accountOpt = accountRepository.findByUsername(request.getUsername().trim());
        if (accountOpt.isEmpty()) {
            return AuthResponse.error("Пользователь не найден");
        }

        Account account = accountOpt.get();
        String expectedHash = account.getPasswordHash();
        String actualHash = hashPassword(request.getPassword());

        if (!expectedHash.equals(actualHash)) {
            logEvent(request.getUsername(), "LOGIN_FAILED", "AUTH", "Неверный пароль или логин");
            return AuthResponse.error("Неверный пароль");
        }

        String token = UUID.randomUUID().toString();
        logEvent(account.getUsername(), "LOGIN", "AUTH", "Успешная авторизация в системе");
        return AuthResponse.success(
                account.getUsername(),
                account.getFullName(),
                account.getEmail(),
                account.getRole(),
                token
        );
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().length() < 3) {
            return AuthResponse.error("Логин должен содержать не менее 3 символов");
        }
        if (request.getPassword() == null || request.getPassword().length() < 4) {
            return AuthResponse.error("Пароль должен содержать не менее 4 символов");
        }

        String username = request.getUsername().trim();
        if (accountRepository.existsByUsername(username)) {
            return AuthResponse.error("Пользователь с логином '" + username + "' уже существует");
        }

        String email = request.getEmail() != null ? request.getEmail().trim() : "";
        if (!email.isBlank() && accountRepository.existsByEmail(email)) {
            return AuthResponse.error("Email '" + email + "' уже используется");
        }

        String fullName = request.getFullName() != null && !request.getFullName().isBlank()
                ? request.getFullName().trim() : username;

        String hash = hashPassword(request.getPassword());
        Account account = new Account(username, hash, fullName, email, "USER");
        accountRepository.save(account);

        String token = UUID.randomUUID().toString();
        logEvent(account.getUsername(), "REGISTER", "AUTH", "Регистрация нового пользователя: " + account.getFullName());
        return AuthResponse.success(
                account.getUsername(),
                account.getFullName(),
                account.getEmail(),
                account.getRole(),
                token
        );
    }

    @Transactional
    public AuthResponse changePassword(ChangePasswordRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return AuthResponse.error("Укажите имя пользователя");
        }
        if (request.getOldPassword() == null || request.getOldPassword().isBlank()) {
            return AuthResponse.error("Введите старый пароль");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 4) {
            return AuthResponse.error("Новый пароль должен содержать минимум 4 символа");
        }

        Optional<Account> accountOpt = accountRepository.findByUsername(request.getUsername().trim());
        if (accountOpt.isEmpty()) {
            return AuthResponse.error("Пользователь не найден");
        }

        Account account = accountOpt.get();
        String currentHash = hashPassword(request.getOldPassword());
        if (!account.getPasswordHash().equals(currentHash)) {
            logEvent(account.getUsername(), "PASSWORD_CHANGE_FAILED", "AUTH", "Попытка смены пароля: неверный старый пароль");
            return AuthResponse.error("Старый пароль указан неверно");
        }

        account.setPasswordHash(hashPassword(request.getNewPassword()));
        accountRepository.save(account);

        logEvent(account.getUsername(), "PASSWORD_CHANGE", "AUTH", "Пароль успешно обновлен пользователем");

        AuthResponse res = new AuthResponse();
        res.setSuccess(true);
        res.setMessage("Пароль успешно изменён");
        res.setUsername(account.getUsername());
        return res;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
