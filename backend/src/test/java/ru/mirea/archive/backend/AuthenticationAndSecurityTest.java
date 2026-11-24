package ru.mirea.archive.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.archive.backend.domain.Account;
import ru.mirea.archive.backend.domain.Role;
import ru.mirea.archive.backend.dto.AuthResponse;
import ru.mirea.archive.backend.dto.LoginRequest;
import ru.mirea.archive.backend.repository.AccountRepository;
import ru.mirea.archive.backend.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AuthenticationAndSecurityTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("4.1. BCrypt хэширование паролей: необратимость и корректная проверка")
    void testBcryptPasswordHashing() {
        String rawPassword = "SecurePassword123!";
        String encoded = passwordEncoder.encode(rawPassword);

        // Хэш должен начинаться с префикса BCrypt $2a$ или $2b$
        assertThat(encoded).startsWith("$2a$");
        assertThat(encoded).isNotEqualTo(rawPassword);

        // Проверка совпадения
        assertThat(passwordEncoder.matches(rawPassword, encoded)).isTrue();
        assertThat(passwordEncoder.matches("WrongPassword", encoded)).isFalse();
    }

    @Test
    @DisplayName("4.2. Успешный вход пользователя и выдача пары токенов (Access JWT + Refresh Token)")
    void testSuccessfulLogin() {
        Account user = new Account("testuser", passwordEncoder.encode("secretPass"), "Тестовый Пользователь", "test@mirea.ru", Role.EDITOR);
        accountRepository.save(user);

        AuthResponse response = authService.login(new LoginRequest("testuser", "secretPass"), "TestAgent", "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");
        assertThat(response.getUser().getRole()).isEqualTo(Role.EDITOR);
    }

    @Test
    @DisplayName("4.3. Блокировка учетной записи после 5 неудачных попыток входа (Защита от подбора пароля)")
    void testAccountLockoutAfterFailedAttempts() {
        Account user = new Account("locktarget", passwordEncoder.encode("correctPassword"), "Жертва Брутфорса", "target@mirea.ru", Role.VIEWER);
        accountRepository.save(user);

        // 4 неудачные попытки: выбрасывают BadCredentialsException
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("locktarget", "wrongPass"), "TestAgent", "127.0.0.1"))
                    .isInstanceOf(BadCredentialsException.class);
        }

        // 5-я неудачная попытка
        assertThatThrownBy(() -> authService.login(new LoginRequest("locktarget", "wrongPass"), "TestAgent", "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);

        // 6-я попытка (даже с правильным паролем): учётная запись заблокирована!
        assertThatThrownBy(() -> authService.login(new LoginRequest("locktarget", "correctPassword"), "TestAgent", "127.0.0.1"))
                .isInstanceOf(LockedException.class)
                .hasMessageContaining("заблокирована");
    }

    @Test
    @DisplayName("4.4. Запрет входа для отключённых учетных записей")
    void testDisabledAccountLoginRejected() {
        Account disabledUser = new Account("inactive_user", passwordEncoder.encode("password123"), "Отключённый Работник", "disabled@mirea.ru", Role.VIEWER);
        disabledUser.setActive(false);
        accountRepository.save(disabledUser);

        assertThatThrownBy(() -> authService.login(new LoginRequest("inactive_user", "password123"), "TestAgent", "127.0.0.1"))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("отключена");
    }

    @Test
    @DisplayName("4.5. Требование обязательной смены временного пароля при первом входе")
    void testMustChangeTemporaryPasswordFlag() {
        Account userWithTempPass = new Account("newbie", passwordEncoder.encode("tempSecret123"), "Новый Сотрудник", "newbie@mirea.ru", Role.VIEWER);
        userWithTempPass.setMustChangePassword(true);
        accountRepository.save(userWithTempPass);

        AuthResponse response = authService.login(new LoginRequest("newbie", "tempSecret123"), "TestAgent", "127.0.0.1");

        assertThat(response.getUser().isMustChangePassword()).isTrue();
    }
}
