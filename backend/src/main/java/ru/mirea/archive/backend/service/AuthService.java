package ru.mirea.archive.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.archive.backend.domain.Account;
import ru.mirea.archive.backend.domain.RefreshSession;
import ru.mirea.archive.backend.domain.Role;
import ru.mirea.archive.backend.dto.*;
import ru.mirea.archive.backend.error.BadRequestException;
import ru.mirea.archive.backend.error.ResourceNotFoundException;
import ru.mirea.archive.backend.error.UnauthorizedException;
import ru.mirea.archive.backend.repository.AccountRepository;
import ru.mirea.archive.backend.repository.RefreshSessionRepository;
import ru.mirea.archive.backend.security.JwtTokenProvider;
import ru.mirea.archive.backend.security.LoginAttemptService;
import ru.mirea.archive.backend.security.UserPrincipal;

import java.time.LocalDateTime;

@Service
public class AuthService implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final LoginAttemptService loginAttemptService;
    private final AuditService auditService;

    @Value("${app.init.admin.login:admin}")
    private String initAdminLogin;

    @Value("${app.init.admin.password:admin123}")
    private String initAdminPassword;

    @Value("${app.init.admin.name:Главный Администратор}")
    private String initAdminName;

    @Value("${app.init.admin.email:admin@mirea.ru}")
    private String initAdminEmail;

    public AuthService(
            AccountRepository accountRepository,
            RefreshSessionRepository refreshSessionRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            LoginAttemptService loginAttemptService,
            AuditService auditService
    ) {
        this.accountRepository = accountRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.loginAttemptService = loginAttemptService;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initOrUpdateAccount(initAdminLogin, initAdminPassword, initAdminName, initAdminEmail, Role.ADMIN);
        initOrUpdateAccount("editor", "editor123", "Старший Архивариус", "editor@mirea.ru", Role.EDITOR);
        initOrUpdateAccount("viewer", "viewer123", "Читатель Архива", "viewer@mirea.ru", Role.VIEWER);
    }

    private void initOrUpdateAccount(String username, String rawPassword, String fullName, String email, Role role) {
        accountRepository.findByUsername(username).ifPresentOrElse(
                account -> {
                    if (!passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
                        account.setPasswordHash(passwordEncoder.encode(rawPassword));
                        account.setActive(true);
                        account.setMustChangePassword(false);
                        accountRepository.save(account);
                    }
                },
                () -> {
                    Account account = new Account(username, passwordEncoder.encode(rawPassword), fullName, email, role);
                    account.setActive(true);
                    account.setMustChangePassword(false);
                    accountRepository.save(account);
                    auditService.log("system", "SYSTEM_INIT", "ACCOUNT", account.getUsername(),
                            "Инициализирован пользователь по умолчанию: " + account.getUsername());
                }
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String userAgent, String ipAddress) {
        String username = request.getUsername().trim();
        Account account = accountRepository.findByUsername(username)
                .orElse(null);

        if (account == null) {
            auditService.log(username, "LOGIN_FAILED", "AUTH", null, "Пользователь не найден");
            throw new BadCredentialsException("Неверный логин или пароль");
        }

        if (!account.isActive()) {
            auditService.log(username, "LOGIN_BLOCKED", "AUTH", account.getId().toString(), "Попытка входа в отключенную учетную запись");
            throw new DisabledException("Учетная запись отключена");
        }

        if (loginAttemptService.isLocked(account)) {
            auditService.log(username, "LOGIN_LOCKED", "AUTH", account.getId().toString(), "Попытка входа во временно заблокированную учетную запись");
            throw new LockedException("Учетная запись временно заблокирована из-за превышения числа неудачных попыток");
        }

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            loginAttemptService.loginFailed(username);
            auditService.log(username, "LOGIN_FAILED", "AUTH", account.getId().toString(), "Неверный пароль");
            throw new BadCredentialsException("Неверный логин или пароль");
        }

        loginAttemptService.loginSucceeded(username);

        UserPrincipal principal = new UserPrincipal(account);
        String accessToken = tokenProvider.generateAccessToken(principal);
        String rawRefreshToken = tokenProvider.generateRefreshTokenString();
        String tokenHash = tokenProvider.hashToken(rawRefreshToken);

        LocalDateTime expiresAt = LocalDateTime.now().plusNanos(tokenProvider.getRefreshExpirationMs() * 1_000_000);
        RefreshSession session = new RefreshSession(account, tokenHash, expiresAt, userAgent, ipAddress);
        refreshSessionRepository.save(session);

        auditService.log(username, "LOGIN_SUCCESS", "AUTH", account.getId().toString(), "Успешная авторизация в системе");

        UserSummaryDto userSummary = new UserSummaryDto(
                account.getId(),
                account.getUsername(),
                account.getFullName(),
                account.getEmail(),
                account.getRole(),
                account.isMustChangePassword()
        );

        return new AuthResponse(accessToken, rawRefreshToken, tokenProvider.getAccessExpirationMs(), userSummary);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken, String userAgent, String ipAddress) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadRequestException("Refresh token обязателен");
        }

        String tokenHash = tokenProvider.hashToken(rawRefreshToken.trim());
        RefreshSession session = refreshSessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Недействительная refresh-сессия"));

        if (!session.isValid()) {
            throw new UnauthorizedException("Refresh-сессия истекла или отозвана");
        }

        Account account = session.getAccount();
        if (!account.isActive()) {
            throw new DisabledException("Учетная запись отключена");
        }

        // Ротация refresh-токена: отзываем старый, создаем новый
        session.setRevoked(true);
        refreshSessionRepository.save(session);

        UserPrincipal principal = new UserPrincipal(account);
        String newAccessToken = tokenProvider.generateAccessToken(principal);
        String newRawRefreshToken = tokenProvider.generateRefreshTokenString();
        String newTokenHash = tokenProvider.hashToken(newRawRefreshToken);

        LocalDateTime expiresAt = LocalDateTime.now().plusNanos(tokenProvider.getRefreshExpirationMs() * 1_000_000);
        RefreshSession newSession = new RefreshSession(account, newTokenHash, expiresAt, userAgent, ipAddress);
        refreshSessionRepository.save(newSession);

        auditService.log(account.getUsername(), "TOKEN_REFRESH", "AUTH", account.getId().toString(), "Обновление access токена по refresh сессии");

        UserSummaryDto userSummary = new UserSummaryDto(
                account.getId(),
                account.getUsername(),
                account.getFullName(),
                account.getEmail(),
                account.getRole(),
                account.isMustChangePassword()
        );

        return new AuthResponse(newAccessToken, newRawRefreshToken, tokenProvider.getAccessExpirationMs(), userSummary);
    }

    @Transactional
    public void logout(String rawRefreshToken, String currentUsername) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = tokenProvider.hashToken(rawRefreshToken.trim());
            refreshSessionRepository.revokeByTokenHash(tokenHash);
        }
        auditService.log(currentUsername, "LOGOUT", "AUTH", null, "Выход из системы");
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, String currentUsername) {
        Account account = accountRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + currentUsername));

        if (!passwordEncoder.matches(request.getOldPassword(), account.getPasswordHash())) {
            auditService.log(currentUsername, "PASSWORD_CHANGE_FAILED", "ACCOUNT", account.getId().toString(), "Неверный старый пароль");
            throw new BadRequestException("Текущий пароль указан неверно");
        }

        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        account.setMustChangePassword(false);
        accountRepository.save(account);

        // Отзываем все предыдущие refresh-сессии пользователя для безопасности
        refreshSessionRepository.revokeAllByAccountId(account.getId());

        auditService.log(currentUsername, "PASSWORD_CHANGED", "ACCOUNT", account.getId().toString(), "Пароль успешно изменён");
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getMe(String currentUsername) {
        Account account = accountRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + currentUsername));
        return new UserSummaryDto(
                account.getId(),
                account.getUsername(),
                account.getFullName(),
                account.getEmail(),
                account.getRole(),
                account.isMustChangePassword()
        );
    }
}
