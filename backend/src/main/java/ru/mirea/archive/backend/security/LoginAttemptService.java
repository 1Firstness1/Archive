package ru.mirea.archive.backend.security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.archive.backend.domain.Account;
import ru.mirea.archive.backend.repository.AccountRepository;

import java.time.LocalDateTime;

@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final int LOCK_MINUTES = 15;

    private final AccountRepository accountRepository;

    public LoginAttemptService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void loginFailed(String username) {
        if (username == null || username.isBlank()) return;
        accountRepository.findByUsername(username.trim()).ifPresent(account -> {
            int attempts = account.getFailedLoginAttempts() + 1;
            account.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_ATTEMPTS) {
                account.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            }
            accountRepository.save(account);
        });
    }

    @Transactional
    public void loginSucceeded(String username) {
        if (username == null || username.isBlank()) return;
        accountRepository.findByUsername(username.trim()).ifPresent(account -> {
            if (account.getFailedLoginAttempts() > 0 || account.getLockedUntil() != null) {
                account.setFailedLoginAttempts(0);
                account.setLockedUntil(null);
                accountRepository.save(account);
            }
        });
    }

    @Transactional(readOnly = true)
    public boolean isLocked(Account account) {
        if (account == null || account.getLockedUntil() == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(account.getLockedUntil());
    }
}
