package ru.mirea.archive.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.mirea.archive.backend.dto.CreateNoteRequest;
import ru.mirea.archive.backend.dto.DocumentNoteDto;
import ru.mirea.archive.backend.dto.UpdateNoteRequest;
import ru.mirea.archive.backend.security.UserPrincipal;
import ru.mirea.archive.backend.service.DocumentNoteService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/document-requests/{id}/notes")
@Tag(name = "Заметки к запросам", description = "История и управление заметками к архивным запросам")
public class DocumentNoteController {

    private final DocumentNoteService documentNoteService;

    public DocumentNoteController(DocumentNoteService documentNoteService) {
        this.documentNoteService = documentNoteService;
    }

    @GetMapping
    @Operation(summary = "Список заметок к запросу")
    public ResponseEntity<List<DocumentNoteDto>> listNotes(@PathVariable("id") UUID requestId) {
        return ResponseEntity.ok(documentNoteService.getNotes(requestId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    @Operation(summary = "Создание заметки к запросу")
    public ResponseEntity<DocumentNoteDto> createNote(
            @PathVariable("id") UUID requestId,
            @Valid @RequestBody CreateNoteRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentNoteService.createNote(requestId, request, principal.getUsername()));
    }

    @PutMapping("/{noteId}")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    @Operation(summary = "Редактирование заметки")
    public ResponseEntity<DocumentNoteDto> updateNote(
            @PathVariable("id") UUID requestId,
            @PathVariable("noteId") UUID noteId,
            @Valid @RequestBody UpdateNoteRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(documentNoteService.updateNote(requestId, noteId, request, principal.getUsername()));
    }

    @DeleteMapping("/{noteId}")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    @Operation(summary = "Удаление заметки")
    public ResponseEntity<Void> deleteNote(
            @PathVariable("id") UUID requestId,
            @PathVariable("noteId") UUID noteId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        documentNoteService.deleteNote(requestId, noteId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{noteId}/pin")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    @Operation(summary = "Переключение закрепления заметки (pin / unpin)")
    public ResponseEntity<DocumentNoteDto> togglePin(
            @PathVariable("id") UUID requestId,
            @PathVariable("noteId") UUID noteId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(documentNoteService.togglePin(requestId, noteId, principal.getUsername()));
    }
}
