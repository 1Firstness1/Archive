package ru.mirea.archive.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mirea.archive.backend.dto.*;
import ru.mirea.archive.backend.repository.AccountRepository;
import ru.mirea.archive.backend.service.AuthService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final AccountRepository accountRepository;

    public AuthController(AuthService authService, AccountRepository accountRepository) {
        this.authService = authService;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> healthCheck() {
        try {
            long count = accountRepository.count();
            return ResponseEntity.ok(new HealthResponse("UP", "CONNECTED", "База данных доступна (аккаунтов: " + count + ")"));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(new HealthResponse("DOWN", "DISCONNECTED", "Ошибка БД: " + e.getMessage()));
        }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/auth/change-password")
    public ResponseEntity<AuthResponse> changePassword(@RequestBody ChangePasswordRequest request) {
        AuthResponse response = authService.changePassword(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<java.util.List<ru.mirea.archive.backend.model.AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(authService.getAuditLogs());
    }

    @PostMapping("/audit-logs")
    public ResponseEntity<Void> createAuditLog(@RequestBody java.util.Map<String, String> body) {
        String username = body.getOrDefault("username", "anonymous");
        String event = body.getOrDefault("event", "UNKNOWN");
        String entityType = body.getOrDefault("entityType", "GENERAL");
        String details = body.getOrDefault("details", "");
        authService.logEvent(username, event, entityType, details);
        return ResponseEntity.ok().build();
    }

    @GetMapping({"/workers", "/accounts"})
    public ResponseEntity<java.util.List<WorkerDto>> getWorkers() {
        java.util.List<WorkerDto> workers = accountRepository.findAll().stream()
                .map(acc -> new WorkerDto(
                        acc.getId(),
                        acc.getUsername(),
                        acc.getFullName(),
                        acc.getEmail(),
                        acc.getRole(),
                        acc.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(workers);
    }
}
