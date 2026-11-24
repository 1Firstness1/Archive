package ru.mirea.archive.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.archive.backend.domain.Account;
import ru.mirea.archive.backend.dto.AdminUserDto;
import ru.mirea.archive.backend.dto.CreateAdminUserRequest;
import ru.mirea.archive.backend.dto.ResetPasswordResponse;
import ru.mirea.archive.backend.dto.UpdateAdminUserRequest;
import ru.mirea.archive.backend.error.BadRequestException;
import ru.mirea.archive.backend.error.DuplicateResourceException;
import ru.mirea.archive.backend.error.ResourceNotFoundException;
import ru.mirea.archive.backend.repository.AccountRepository;
import ru.mirea.archive.backend.repository.RefreshSessionRepository;

import java.security.SecureRandom;

@Service
public class AccountAdminService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AccountAdminService(
            AccountRepository accountRepository,
            RefreshSessionRepository refreshSessionRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.accountRepository = accountRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDto> listAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public AdminUserDto getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Учётная запись не найдена: " + id));
        return toDto(account);
    }

    @Transactional
    public ResetPasswordResponse createAccount(CreateAdminUserRequest request, String currentAdmin) {
        String username = request.getUsername().trim();
        if (accountRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Пользователь с логином '" + username + "' уже существует");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (accountRepository.existsByEmail(request.getEmail().trim())) {
                throw new DuplicateResourceException("Email '" + request.getEmail().trim() + "' уже используется");
            }
        }

        String tempPassword = generateTemporaryPassword(10);
        Account account = new Account(
                username,
                passwordEncoder.encode(tempPassword),
                request.getFullName().trim(),
                request.getEmail() != null ? request.getEmail().trim() : null,
                request.getRole()
        );
        account.setActive(true);
        account.setMustChangePassword(true); // Требуем смены пароля при первом входе

        accountRepository.save(account);

        auditService.log(currentAdmin, "ADMIN_CREATE_USER", "ACCOUNT", account.getId().toString(),
                "Создана учётная запись: " + username + " (роль: " + request.getRole() + ")");

        return new ResetPasswordResponse(
                username,
                tempPassword,
                "Учётная запись успешно создана. Передайте временный пароль пользователю."
        );
    }

    @Transactional
    public AdminUserDto updateAccount(Long id, UpdateAdminUserRequest request, String currentAdmin) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Учётная запись не найдена: " + id));

        String newUsername = request.getUsername().trim();
        if (!account.getUsername().equalsIgnoreCase(newUsername) && accountRepository.existsByUsername(newUsername)) {
            throw new DuplicateResourceException("Логин '" + newUsername + "' уже занят");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (accountRepository.existsByEmailAndIdNot(request.getEmail().trim(), id)) {
                throw new DuplicateResourceException("Email '" + request.getEmail().trim() + "' уже занят другим пользователем");
            }
        }

        // Защита от случайного отключения или понижения роли самого себя
        if (account.getUsername().equalsIgnoreCase(currentAdmin)) {
            if (!request.isActive()) {
                throw new BadRequestException("Администратор не может деактивировать собственную учётную запись");
            }
        }

        account.setUsername(newUsername);
        account.setFullName(request.getFullName().trim());
        account.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        account.setRole(request.getRole());
        account.setActive(request.isActive());

        accountRepository.save(account);

        if (!account.isActive()) {
            // Если аккаунт отключен, отзываем его refresh-сессии
            refreshSessionRepository.revokeAllByAccountId(account.getId());
        }

        auditService.log(currentAdmin, "ADMIN_UPDATE_USER", "ACCOUNT", account.getId().toString(),
                "Обновлена учётная запись: " + newUsername + " (роль: " + request.getRole() + ", активен: " + request.isActive() + ")");

        return toDto(account);
    }

    @Transactional
    public ResetPasswordResponse resetPassword(Long id, String currentAdmin) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Учётная запись не найдена: " + id));

        String tempPassword = generateTemporaryPassword(10);
        account.setPasswordHash(passwordEncoder.encode(tempPassword));
        account.setMustChangePassword(true);
        account.setFailedLoginAttempts(0);
        account.setLockedUntil(null);

        accountRepository.save(account);

        // Отзываем текущие сессии пользователя
        refreshSessionRepository.revokeAllByAccountId(account.getId());

        auditService.log(currentAdmin, "ADMIN_RESET_PASSWORD", "ACCOUNT", account.getId().toString(),
                "Сброшен пароль пользователя: " + account.getUsername());

        return new ResetPasswordResponse(
                account.getUsername(),
                tempPassword,
                "Пароль успешно сброшен. Пользователю необходимо сменить его при первом входе."
        );
    }

    private AdminUserDto toDto(Account account) {
        return new AdminUserDto(
                account.getId(),
                account.getUsername(),
                account.getFullName(),
                account.getEmail(),
                account.getRole(),
                account.isActive(),
                account.isMustChangePassword(),
                account.getFailedLoginAttempts(),
                account.getLockedUntil(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    private String generateTemporaryPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
