package ru.mirea.archive.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.archive.backend.domain.DocumentNote;
import ru.mirea.archive.backend.domain.DocumentRequest;
import ru.mirea.archive.backend.dto.CreateNoteRequest;
import ru.mirea.archive.backend.dto.DocumentNoteDto;
import ru.mirea.archive.backend.dto.UpdateNoteRequest;
import ru.mirea.archive.backend.error.ResourceNotFoundException;
import ru.mirea.archive.backend.repository.DocumentNoteRepository;
import ru.mirea.archive.backend.repository.DocumentRequestRepository;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentNoteService {

    private final DocumentNoteRepository documentNoteRepository;
    private final DocumentRequestRepository documentRequestRepository;
    private final AuditService auditService;

    public DocumentNoteService(
            DocumentNoteRepository documentNoteRepository,
            DocumentRequestRepository documentRequestRepository,
            AuditService auditService
    ) {
        this.documentNoteRepository = documentNoteRepository;
        this.documentRequestRepository = documentRequestRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DocumentNoteDto> getNotes(UUID requestUuid) {
        if (!documentRequestRepository.existsByUuid(requestUuid)) {
            throw new ResourceNotFoundException("Запрос документа не найден: " + requestUuid);
        }
        return documentNoteRepository.findByDocumentRequest_UuidOrderByPinnedDescCreatedAtDesc(requestUuid)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public DocumentNoteDto createNote(UUID requestUuid, CreateNoteRequest request, String currentUsername) {
        DocumentRequest doc = documentRequestRepository.findByUuid(requestUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Запрос документа не найден: " + requestUuid));

        DocumentNote note = new DocumentNote(
                doc,
                currentUsername,
                request.getContent().trim(),
                request.isPinned()
        );
        documentNoteRepository.save(note);

        auditService.log(currentUsername, "CREATE_NOTE", "DOCUMENT_NOTE", note.getId().toString(),
                "Добавлена заметка к запросу " + doc.getRequestNumber());

        return toDto(note);
    }

    @Transactional
    public DocumentNoteDto updateNote(UUID requestUuid, UUID noteId, UpdateNoteRequest request, String currentUsername) {
        DocumentNote note = documentNoteRepository.findByIdAndDocumentRequest_Uuid(noteId, requestUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Заметка не найдена: " + noteId));

        note.setContent(request.getContent().trim());
        if (request.getPinned() != null) {
            note.setPinned(request.getPinned());
        }
        documentNoteRepository.save(note);

        auditService.log(currentUsername, "UPDATE_NOTE", "DOCUMENT_NOTE", note.getId().toString(),
                "Обновлена заметка " + noteId + " к запросу " + requestUuid);

        return toDto(note);
    }

    @Transactional
    public void deleteNote(UUID requestUuid, UUID noteId, String currentUsername) {
        DocumentNote note = documentNoteRepository.findByIdAndDocumentRequest_Uuid(noteId, requestUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Заметка не найдена: " + noteId));

        documentNoteRepository.delete(note);

        auditService.log(currentUsername, "DELETE_NOTE", "DOCUMENT_NOTE", noteId.toString(),
                "Удалена заметка к запросу " + requestUuid);
    }

    @Transactional
    public DocumentNoteDto togglePin(UUID requestUuid, UUID noteId, String currentUsername) {
        DocumentNote note = documentNoteRepository.findByIdAndDocumentRequest_Uuid(noteId, requestUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Заметка не найдена: " + noteId));

        note.setPinned(!note.isPinned());
        documentNoteRepository.save(note);

        auditService.log(currentUsername, "TOGGLE_PIN_NOTE", "DOCUMENT_NOTE", noteId.toString(),
                "Статус закрепления заметки " + noteId + " изменен на: " + note.isPinned());

        return toDto(note);
    }

    private DocumentNoteDto toDto(DocumentNote note) {
        return new DocumentNoteDto(
                note.getId(),
                note.getDocumentRequest().getUuid(),
                note.getAuthorUsername(),
                note.getContent(),
                note.isPinned(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
