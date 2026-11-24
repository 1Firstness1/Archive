package ru.mirea.archive.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.mirea.archive.backend.dto.AdminUserDto;
import ru.mirea.archive.backend.dto.CreateAdminUserRequest;
import ru.mirea.archive.backend.dto.ResetPasswordResponse;
import ru.mirea.archive.backend.dto.UpdateAdminUserRequest;
import ru.mirea.archive.backend.security.UserPrincipal;
import ru.mirea.archive.backend.service.AccountAdminService;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Администрирование пользователей системы", description = "Управление учетными записями работников архива (только для Администратора)")
public class UserAdminController {

    private final AccountAdminService accountAdminService;

    public UserAdminController(AccountAdminService accountAdminService) {
        this.accountAdminService = accountAdminService;
    }

    @GetMapping
    @Operation(summary = "Список учетных записей работников с пагинацией")
    public ResponseEntity<Page<AdminUserDto>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return ResponseEntity.ok(accountAdminService.listAccounts(PageRequest.of(page, Math.min(size, 100), Sort.by(direction, sortBy))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получение данных учетной записи по ID")
    public ResponseEntity<AdminUserDto> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(accountAdminService.getAccountById(id));
    }

    @PostMapping
    @Operation(summary = "Создание учетной записи работника с генерацией временного пароля")
    public ResponseEntity<ResetPasswordResponse> createUser(
            @Valid @RequestBody CreateAdminUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountAdminService.createAccount(request, principal.getUsername()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Изменение имени, логина, роли и статуса активности пользователя")
    public ResponseEntity<AdminUserDto> updateUser(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAdminUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(accountAdminService.updateAccount(id, request, principal.getUsername()));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Сброс пароля пользователя с выдачей временного пароля")
    public ResponseEntity<ResetPasswordResponse> resetPassword(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(accountAdminService.resetPassword(id, principal.getUsername()));
    }
}
