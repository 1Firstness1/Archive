package ru.mirea.archive.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.archive.backend.domain.DocumentRequest;
import ru.mirea.archive.backend.dto.BulkOperationRequest;
import ru.mirea.archive.backend.dto.BulkOperationResult;
import ru.mirea.archive.backend.repository.DocumentRequestRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class BulkOperationService {

    private final DocumentRequestRepository documentRequestRepository;
    private final AuditService auditService;

    public BulkOperationService(DocumentRequestRepository documentRequestRepository, AuditService auditService) {
        this.documentRequestRepository = documentRequestRepository;
        this.auditService = auditService;
    }

    @Transactional
    public BulkOperationResult execute(BulkOperationRequest request, String currentUsername) {
        BulkOperationResult result = new BulkOperationResult(request.getUuids().size());

        for (UUID uuid : request.getUuids()) {
            try {
                Optional<DocumentRequest> opt = documentRequestRepository.findByUuid(uuid);
                if (opt.isEmpty()) {
                    result.addFailure(uuid, "Запись не найдена в базе данных");
                    continue;
                }

                DocumentRequest doc = opt.get();
                switch (request.getAction()) {
                    case ARCHIVE -> {
                        if (doc.isArchived()) {
                            result.addFailure(uuid, "Запись уже архивирована");
                            continue;
                        }
                        doc.setArchived(true);
                    }
                    case RESTORE -> {
                        if (!doc.isArchived()) {
                            result.addFailure(uuid, "Запись не является архивной");
                            continue;
                        }
                        doc.setArchived(false);
                    }
                    case CHANGE_STATUS -> {
                        if (request.getTargetStatus() == null) {
                            result.addFailure(uuid, "Целевой статус не указан");
                            continue;
                        }
                        doc.setStatus(request.getTargetStatus());
                    }
                    case ADD_TAG -> {
                        if (request.getTag() == null || request.getTag().isBlank()) {
                            result.addFailure(uuid, "Тег для добавления не указан");
                            continue;
                        }
                        doc.getTags().add(request.getTag().trim().toLowerCase());
                    }
                    case REMOVE_TAG -> {
                        if (request.getTag() == null || request.getTag().isBlank()) {
                            result.addFailure(uuid, "Тег для удаления не указан");
                            continue;
                        }
                        doc.getTags().remove(request.getTag().trim().toLowerCase());
                    }
                }

                documentRequestRepository.save(doc);
                result.addSuccess(uuid);

            } catch (Exception e) {
                result.addFailure(uuid, e.getMessage());
            }
        }

        auditService.log(currentUsername, "BULK_OPERATION", "DOCUMENT_REQUEST", null,
                "Массовая операция " + request.getAction() + ": успешно " + result.getSuccessCount() + ", ошибок " + result.getFailureCount());

        return result;
    }
}
