package ru.mirea.archive.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.archive.backend.domain.ArchiveUser;
import ru.mirea.archive.backend.domain.DocumentRequest;
import ru.mirea.archive.backend.domain.Priority;
import ru.mirea.archive.backend.domain.RequestStatus;
import ru.mirea.archive.backend.dto.CreateDocumentRequest;
import ru.mirea.archive.backend.dto.DocumentRequestDto;
import ru.mirea.archive.backend.dto.UpdateDocumentRequest;
import ru.mirea.archive.backend.error.BadRequestException;
import ru.mirea.archive.backend.error.DuplicateResourceException;
import ru.mirea.archive.backend.error.ResourceNotFoundException;
import ru.mirea.archive.backend.error.VersionConflictException;
import ru.mirea.archive.backend.repository.ArchiveUserRepository;
import ru.mirea.archive.backend.repository.DocumentRequestRepository;
import ru.mirea.archive.backend.repository.DocumentRequestSpecification;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentRequestService {

    public static final int MAX_PAGE_SIZE = 100;
    private static final Random RANDOM = new Random();

    private final DocumentRequestRepository documentRequestRepository;
    private final ArchiveUserRepository archiveUserRepository;
    private final AuditService auditService;

    public DocumentRequestService(
            DocumentRequestRepository documentRequestRepository,
            ArchiveUserRepository archiveUserRepository,
            AuditService auditService
    ) {
        this.documentRequestRepository = documentRequestRepository;
        this.archiveUserRepository = archiveUserRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<DocumentRequestDto> listRequests(
            String search,
            Long userId,
            String category,
            RequestStatus status,
            Priority priority,
            Boolean confidential,
            Boolean archived,
            String tag,
            Integer estimatedDays,
            Integer minEstimatedDays,
            Integer maxEstimatedDays,
            String pageOp,
            Integer pages,
            Integer minPages,
            Integer maxPages,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        int boundedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(Math.max(0, page), boundedSize, sort);

        Specification<DocumentRequest> spec = DocumentRequestSpecification.filterBy(
                search, userId, category, status, priority,
                confidential, archived, tag, estimatedDays,
                minEstimatedDays, maxEstimatedDays,
                pageOp, pages, minPages, maxPages
        );

        return documentRequestRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public DocumentRequestDto getByUuid(UUID uuid) {
        DocumentRequest doc = documentRequestRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Запрос документа не найден: " + uuid));
        return toDto(doc);
    }

    @Transactional
    public DocumentRequestDto create(CreateDocumentRequest request, String currentUsername) {
        ArchiveUser user = archiveUserRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Заявитель архива не найден: " + request.getUserId()));

        String reqNumber = request.getRequestNumber();
        if (reqNumber == null || reqNumber.isBlank()) {
            reqNumber = generateRequestNumber();
        } else {
            reqNumber = reqNumber.trim();
            if (documentRequestRepository.existsByRequestNumber(reqNumber)) {
                throw new DuplicateResourceException("Запрос с номером '" + reqNumber + "' уже существует");
            }
        }

        DocumentRequest doc = new DocumentRequest(
                user,
                reqNumber,
                request.getDocumentTitle().trim(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                request.getCategory().trim(),
                request.getPriority(),
                request.getPageCount(),
                request.isConfidential()
        );

        if (request.getTags() != null) {
            doc.setTags(request.getTags());
        }

        documentRequestRepository.save(doc);

        auditService.log(currentUsername, "CREATE_REQUEST", "DOCUMENT_REQUEST", doc.getUuid().toString(),
                "Создан запрос " + doc.getRequestNumber() + " («" + doc.getDocumentTitle() + "»)");

        return toDto(doc);
    }

    @Transactional
    public DocumentRequestDto update(UUID uuid, UpdateDocumentRequest request, String currentUsername) {
        DocumentRequest doc = documentRequestRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Запрос документа не найден: " + uuid));

        // Проверка версии для оптимистичной блокировки
        if (!Objects.equals(doc.getVersion(), request.getVersion())) {
            throw new VersionConflictException("Конфликт версий: запись была изменена другим пользователем. " +
                    "Текущая версия: " + doc.getVersion() + ", переданная версия: " + request.getVersion());
        }

        ArchiveUser user = archiveUserRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Заявитель архива не найден: " + request.getUserId()));

        if (request.getRequestNumber() != null && !request.getRequestNumber().isBlank()) {
            String newNumber = request.getRequestNumber().trim();
            if (documentRequestRepository.existsByRequestNumberAndUuidNot(newNumber, uuid)) {
                throw new DuplicateResourceException("Запрос с номером '" + newNumber + "' уже существует");
            }
            doc.setRequestNumber(newNumber);
        }

        doc.setUser(user);
        doc.setDocumentTitle(request.getDocumentTitle().trim());
        doc.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        doc.setCategory(request.getCategory().trim());
        doc.setStatus(request.getStatus());
        doc.setPriority(request.getPriority());
        doc.setPageCount(request.getPageCount());
        doc.setConfidential(request.isConfidential());
        doc.setArchived(request.isArchived());

        if (request.getTags() != null) {
            doc.setTags(request.getTags());
        }

        // Пересчет вычисляемого показателя
        doc.recalculateEstimatedDays();

        documentRequestRepository.save(doc);

        auditService.log(currentUsername, "UPDATE_REQUEST", "DOCUMENT_REQUEST", doc.getUuid().toString(),
                "Обновлен запрос " + doc.getRequestNumber() + " (статус: " + doc.getStatus() + ")");

        return toDto(doc);
    }

    @Transactional
    public DocumentRequestDto archive(UUID uuid, String currentUsername) {
        DocumentRequest doc = documentRequestRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Запрос документа не найден: " + uuid));

        if (doc.isArchived()) {
            throw new BadRequestException("Запрос уже находится в архиве");
        }

        doc.setArchived(true);
        documentRequestRepository.save(doc);

        auditService.log(currentUsername, "ARCHIVE_REQUEST", "DOCUMENT_REQUEST", doc.getUuid().toString(),
                "Запрос " + doc.getRequestNumber() + " помещён в архив");

        return toDto(doc);
    }

    @Transactional
    public DocumentRequestDto restore(UUID uuid, String currentUsername) {
        DocumentRequest doc = documentRequestRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Запрос документа не найден: " + uuid));

        if (!doc.isArchived()) {
            throw new BadRequestException("Запрос не находится в архиве");
        }

        doc.setArchived(false);
        documentRequestRepository.save(doc);

        auditService.log(currentUsername, "RESTORE_REQUEST", "DOCUMENT_REQUEST", doc.getUuid().toString(),
                "Запрос " + doc.getRequestNumber() + " восстановлен из архива");

        return toDto(doc);
    }

    @Transactional
    public void permanentDelete(UUID uuid, String currentUsername) {
        DocumentRequest doc = documentRequestRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Запрос документа не найден: " + uuid));

        String reqNum = doc.getRequestNumber();
        documentRequestRepository.delete(doc);

        auditService.log(currentUsername, "PERMANENT_DELETE_REQUEST", "DOCUMENT_REQUEST", uuid.toString(),
                "Окончательно удален запрос " + reqNum);
    }

    public DocumentRequestDto toDto(DocumentRequest doc) {
        DocumentRequestDto dto = new DocumentRequestDto();
        dto.setUuid(doc.getUuid());
        dto.setRequestNumber(doc.getRequestNumber());
        if (doc.getUser() != null) {
            dto.setUserId(doc.getUser().getId());
            dto.setUserName(doc.getUser().getFullName());
            dto.setUserDepartment(doc.getUser().getDepartment());
        }
        dto.setDocumentTitle(doc.getDocumentTitle());
        dto.setDescription(doc.getDescription());
        dto.setCategory(doc.getCategory());
        dto.setStatus(doc.getStatus());
        dto.setPriority(doc.getPriority());
        dto.setPageCount(doc.getPageCount());
        dto.setConfidential(doc.isConfidential());
        dto.setEstimatedDays(doc.getEstimatedDays());
        dto.setArchived(doc.isArchived());
        dto.setVersion(doc.getVersion());
        dto.setTags(Set.copyOf(doc.getTags()));
        dto.setNotesCount(doc.getNotes() != null ? doc.getNotes().size() : 0);
        dto.setCreatedAt(doc.getCreatedAt());
        dto.setUpdatedAt(doc.getUpdatedAt());
        return dto;
    }

    private String generateRequestNumber() {
        int year = LocalDate.now().getYear();
        String num;
        do {
            num = String.format("REQ-%d-%04d", year, RANDOM.nextInt(9000) + 1000);
        } while (documentRequestRepository.existsByRequestNumber(num));
        return num;
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String property = switch (sortBy != null ? sortBy.trim() : "createdAt") {
            case "documentTitle" -> "documentTitle";
            case "requestNumber" -> "requestNumber";
            case "category" -> "category";
            case "status" -> "status";
            case "priority" -> "priority";
            case "pageCount" -> "pageCount";
            case "estimatedDays" -> "estimatedDays";
            case "updatedAt" -> "updatedAt";
            default -> "createdAt";
        };

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}
