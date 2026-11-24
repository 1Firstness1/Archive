package ru.mirea.archive.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.mirea.archive.backend.dto.ArchiveUserDto;
import ru.mirea.archive.backend.dto.CreateArchiveUserRequest;
import ru.mirea.archive.backend.dto.UpdateArchiveUserRequest;
import ru.mirea.archive.backend.security.UserPrincipal;
import ru.mirea.archive.backend.service.ArchiveUserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Справочник заявителей архива", description = "Справочник пользователей/клиентов архива ({справочник})")
public class ArchiveUserController {

    private final ArchiveUserService archiveUserService;

    public ArchiveUserController(ArchiveUserService archiveUserService) {
        this.archiveUserService = archiveUserService;
    }

    @GetMapping
    @Operation(summary = "Получение списка справочника заявителей")
    public ResponseEntity<List<ArchiveUserDto>> list(
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return ResponseEntity.ok(archiveUserService.listAll(includeInactive));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получение записи справочника по ID")
    public ResponseEntity<ArchiveUserDto> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(archiveUserService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создание новой записи в справочнике (только для Администратора)")
    public ResponseEntity<ArchiveUserDto> create(
            @Valid @RequestBody CreateArchiveUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(archiveUserService.create(request, principal.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Редактирование записи справочника (только для Администратора)")
    public ResponseEntity<ArchiveUserDto> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateArchiveUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(archiveUserService.update(id, request, principal.getUsername()));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Включение или отключение записи справочника (только для Администратора)")
    public ResponseEntity<ArchiveUserDto> toggleActive(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(archiveUserService.toggleActive(id, principal.getUsername()));
    }
}
