package ru.mirea.archive.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.mirea.archive.backend.dto.*;
import ru.mirea.archive.backend.security.UserPrincipal;
import ru.mirea.archive.backend.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Аутентификация", description = "Управление доступом, токенами JWT и учетными записями")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Вход в систему по логину и паролю", description = "Выдает access JWT и refresh token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(authService.login(request, userAgent, ipAddress));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновление access токена", description = "Выдает новую пару access и refresh токенов")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken(), userAgent, ipAddress));
    }

    @PostMapping("/logout")
    @Operation(summary = "Выход из системы", description = "Отзывает текущую refresh-сессию")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String username = principal != null ? principal.getUsername() : "anonymous";
        String token = request != null ? request.getRefreshToken() : null;
        authService.logout(token, username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Данные текущего пользователя", description = "Возвращает информацию о текущем авторизованном пользователе")
    public ResponseEntity<UserSummaryDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getMe(principal.getUsername()));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Смена пароля", description = "Позволяет пользователю обновить свой пароль")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        authService.changePassword(request, principal.getUsername());
        return ResponseEntity.ok().build();
    }
}
